package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import net.lingala.zip4j.ZipFile
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * Fully reads a newly written archive before its host transaction is committed.
 *
 * Source fingerprints are captured on the exact byte stream supplied to the encoder. Verification
 * therefore detects both container corruption and an internally consistent archive containing the
 * wrong data without reopening mutable host source files.
 */
internal class CreatedArchiveVerifier(
    private val cacheDirectory: File,
    private val engine: ArchiveEngine = ArchiveEngine.DEFAULT,
) {

    init {
        require(cacheDirectory.isDirectory) {
            "Archive output verification requires an existing private cache directory"
        }
    }

    fun verifyPendingOutput(
        descriptor: ParcelFileDescriptor,
        format: ArchiveFormat,
        manifest: ArchiveSourceManifest,
        counters: ArchiveCreationCounters,
        password: CharArray?,
        checkCancelled: () -> Unit,
    ) {
        checkCancelled()
        val duplicate = ParcelFileDescriptor.dup(descriptor.fileDescriptor)
        var staged = ArchiveCacheStager.stage(
            source = duplicate,
            cacheDirectory = cacheDirectory,
            reportedSize = SIZE_UNKNOWN,
        )
        try {
            try {
                verifyArchive(
                    source = staged.source,
                    format = format,
                    manifest = manifest,
                    counters = counters,
                    password = password,
                    checkCancelled = checkCancelled,
                )
            } catch (_: ArchiveLocalFileRequiredException) {
                val cached = ArchiveCacheStager.materialize(staged, cacheDirectory)
                staged.close()
                staged = cached
                verifyArchive(
                    source = staged.source,
                    format = format,
                    manifest = manifest,
                    counters = counters,
                    password = password,
                    checkCancelled = checkCancelled,
                )
            }
        } finally {
            staged.close()
        }
        checkCancelled()
    }

    fun verifySplitZip(
        terminalFile: File,
        volumes: List<StagedZipVolume>,
        manifest: ArchiveSourceManifest,
        counters: ArchiveCreationCounters,
        password: CharArray?,
        checkCancelled: () -> Unit,
    ) {
        require(volumes.isNotEmpty() && volumes.last().terminal)
        validateFingerprintTable(manifest, counters)
        val identities = volumes.map { volume ->
            require(volume.file.isFile) { "ZIP staging volume is missing" }
            LocalFileIdentity(
                canonicalFile = volume.file.canonicalFile,
                length = volume.file.length(),
                lastModified = volume.file.lastModified(),
            )
        }
        require(identities.last().canonicalFile == terminalFile.canonicalFile) {
            "ZIP staging terminal volume is inconsistent"
        }

        val retainedPassword = password?.takeIf(CharArray::isNotEmpty)?.clone()
        try {
            val archive = if (retainedPassword == null) {
                ZipFile(terminalFile)
            } else {
                ZipFile(terminalFile, retainedPassword)
            }
            archive.use { zip ->
                checkCancelled()
                val archiveVolumes = if (zip.isSplitArchive) {
                    zip.splitZipFiles.toList()
                } else {
                    listOf(terminalFile)
                }
                if (archiveVolumes.map(File::getCanonicalFile) !=
                    identities.map(LocalFileIdentity::canonicalFile)
                ) {
                    throw IOException("ZIP staging volume order changed after creation")
                }
                if ((volumes.size > 1) != zip.isSplitArchive) {
                    throw IOException("ZIP staging split metadata does not match its volumes")
                }

                val headers = zip.fileHeaders
                if (headers.size != manifest.entries.size) {
                    throw IOException("Created ZIP entry count does not match the source manifest")
                }
                val expectedEntries = CreatedManifestIndex(manifest, counters)
                headers.forEach { header ->
                    checkCancelled()
                    val fingerprint = expectedEntries.validate(
                        name = header.fileName,
                        isDirectory = header.isDirectory,
                        declaredSize = header.uncompressedSize,
                    )
                    if (fingerprint != null) {
                        zip.getInputStream(header).use { input ->
                            verifyEntryData(
                                input = input,
                                fingerprint = fingerprint,
                                declaredCrc = header.crc.takeIf {
                                    !header.isEncrypted || it != 0L
                                },
                                checkCancelled = checkCancelled,
                            )
                        }
                    }
                }
                expectedEntries.requireComplete()
            }
        } finally {
            retainedPassword?.fill('\u0000')
        }

        identities.forEach { expected ->
            checkCancelled()
            val file = expected.canonicalFile
            if (
                !file.isFile ||
                file.length() != expected.length ||
                file.lastModified() != expected.lastModified
            ) {
                throw IOException("ZIP staging volume changed while it was being verified")
            }
        }
    }

    private fun verifyArchive(
        source: ArchiveReadSource,
        format: ArchiveFormat,
        manifest: ArchiveSourceManifest,
        counters: ArchiveCreationCounters,
        password: CharArray?,
        checkCancelled: () -> Unit,
    ) {
        validateFingerprintTable(manifest, counters)
        val identity = source.identity()
        if (!source.isRegularFile || identity.length < 0L) {
            throw IOException("Created archive output is not a regular file")
        }
        if (format.isTarFamily) {
            verifyTarArchive(source, format, manifest, counters, checkCancelled)
        } else {
            verifyRandomAccessArchive(
                source = source,
                format = format,
                manifest = manifest,
                counters = counters,
                password = password,
                checkCancelled = checkCancelled,
            )
        }
        if (source.identity() != identity) {
            throw IOException("Created archive output changed while it was being verified")
        }
    }

    private fun verifyRandomAccessArchive(
        source: ArchiveReadSource,
        format: ArchiveFormat,
        manifest: ArchiveSourceManifest,
        counters: ArchiveCreationCounters,
        password: CharArray?,
        checkCancelled: () -> Unit,
    ) {
        val retainedPassword = password?.takeIf(CharArray::isNotEmpty)?.clone()
        val options = ArchiveReaderOptions(password = retainedPassword)
        retainedPassword?.fill('\u0000')
        try {
            engine.openReader(source, format, options).use { reader ->
                if (reader.format != format) {
                    throw IOException("Created archive format does not match the requested format")
                }
                if (reader.entries.size != manifest.entries.size) {
                    throw IOException("Created archive entry count does not match the source manifest")
                }
                val expectedEntries = CreatedManifestIndex(manifest, counters)
                reader.entries.forEachIndexed { index, entry ->
                    checkCancelled()
                    if (entry.ordinal != index) {
                        throw IOException("Created archive entry order is inconsistent")
                    }
                    val fingerprint = expectedEntries.validate(
                        name = entry.name,
                        isDirectory = entry.isDirectory,
                        declaredSize = entry.size,
                    )
                    if (fingerprint != null) {
                        if (!entry.capabilities.canOpen) {
                            throw IOException("Created archive entry cannot be read back")
                        }
                        reader.openEntry(entry).use { input ->
                            verifyEntryData(
                                input = input,
                                fingerprint = fingerprint,
                                declaredCrc = entry.crc,
                                checkCancelled = checkCancelled,
                            )
                        }
                    }
                }
                expectedEntries.requireComplete()
            }
        } finally {
            options.clearPassword()
        }
    }

    private fun verifyTarArchive(
        source: ArchiveReadSource,
        format: ArchiveFormat,
        manifest: ArchiveSourceManifest,
        counters: ArchiveCreationCounters,
        checkCancelled: () -> Unit,
    ) {
        val container = format.tarContainer()
        if (!container.hasOuterSignature(source)) {
            throw IOException("Created ${format.displayName} signature is not present")
        }
        container.requirePlausibleStructure(source)
        TarArchiveAccess.open(
            source = source,
            container = container,
            checkCancelled = checkCancelled,
        ).use { input ->
            val expectedEntries = CreatedManifestIndex(manifest, counters)
            var ordinal = 0
            while (true) {
                checkCancelled()
                val tarEntry = input.nextEntry ?: break
                if (ordinal >= manifest.entries.size) {
                    throw IOException("Created ${format.displayName} contains an unexpected entry")
                }
                TarArchiveAccess.requireValidChecksum(tarEntry)
                val entry = TarArchiveAccess.toReaderEntry(input, tarEntry, ordinal, container)
                val fingerprint = expectedEntries.validate(
                    name = entry.name,
                    isDirectory = entry.isDirectory,
                    declaredSize = entry.size,
                )
                if (fingerprint != null) {
                    if (!entry.capabilities.canOpen) {
                        throw IOException("Created ${format.displayName} entry cannot be read back")
                    }
                    verifyEntryData(
                        input = input,
                        fingerprint = fingerprint,
                        declaredCrc = entry.crc,
                        checkCancelled = checkCancelled,
                    )
                }
                ordinal++
            }
            if (ordinal != manifest.entries.size) {
                throw IOException("Created ${format.displayName} entry count is incomplete")
            }
            expectedEntries.requireComplete()
        }
    }

    private fun verifyEntryData(
        input: InputStream,
        fingerprint: ArchiveCreationSourceFingerprint,
        declaredCrc: Long?,
        checkCancelled: () -> Unit,
    ) {
        val sha256 = MessageDigest.getInstance(SHA_256)
        val crc32 = CRC32()
        var bytes = 0L
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            checkCancelled()
            val read = buffered.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            if (bytes > fingerprint.bytes - read.toLong()) {
                throw IOException("Created archive entry expands beyond its written source size")
            }
            sha256.update(buffer, 0, read)
            crc32.update(buffer, 0, read)
            bytes += read
        }
        if (bytes != fingerprint.bytes) {
            throw IOException("Created archive entry is shorter than its written source data")
        }
        if (!MessageDigest.isEqual(sha256.digest(), fingerprint.sha256())) {
            throw IOException("Created archive entry content does not match the written source data")
        }
        if (declaredCrc != null && declaredCrc >= 0L && crc32.value != declaredCrc) {
            throw IOException("Created archive entry CRC does not match its data")
        }
    }

    private fun validateFingerprintTable(
        manifest: ArchiveSourceManifest,
        counters: ArchiveCreationCounters,
    ) {
        if (counters.manifestEntryCount != manifest.entries.size) {
            throw IOException("Created archive fingerprint table does not match the source manifest")
        }
        if (
            counters.files != manifest.fileCount ||
            counters.directories != manifest.directoryCount
        ) {
            throw IOException("Created archive counters do not match the source manifest")
        }
    }

    private fun normalizeCreatedEntryName(name: String, isDirectory: Boolean): String {
        if (name.isEmpty() || '\u0000' in name || '\\' in name) {
            throw IOException("Created archive entry name is invalid")
        }
        val normalized = if (isDirectory) name.trimEnd('/') else name
        if (
            normalized.isEmpty() ||
            (!isDirectory && name.endsWith('/')) ||
            normalized.startsWith('/') ||
            normalized.split('/').any { it.isEmpty() || it == "." || it == ".." }
        ) {
            throw IOException("Created archive entry path is invalid")
        }
        return normalized
    }

    private inner class CreatedManifestIndex(
        private val manifest: ArchiveSourceManifest,
        private val counters: ArchiveCreationCounters,
    ) {
        private val entryIndexByPath = manifest.entries
            .mapIndexed { index, entry -> entry.archivePath to index }
            .toMap()
        private val seen = BooleanArray(manifest.entries.size)

        init {
            require(entryIndexByPath.size == manifest.entries.size) {
                "Archive source manifest contains duplicate paths"
            }
        }

        fun validate(
            name: String,
            isDirectory: Boolean,
            declaredSize: Long,
        ): ArchiveCreationSourceFingerprint? {
            val normalizedName = normalizeCreatedEntryName(name, isDirectory)
            val entryIndex = entryIndexByPath[normalizedName]
                ?: throw IOException("Created archive contains an unexpected entry")
            if (seen[entryIndex]) {
                throw IOException("Created archive contains a duplicate entry")
            }
            seen[entryIndex] = true
            val expected = manifest.entries[entryIndex]
            if (isDirectory != expected.isDirectory) {
                throw IOException("Created archive entry type does not match the source manifest")
            }
            val fingerprint = counters.sourceFingerprintAt(entryIndex)
            if (isDirectory) {
                if (fingerprint != null || declaredSize != 0L) {
                    throw IOException("Created archive directory contains unexpected data")
                }
                return null
            }
            val requiredFingerprint = fingerprint
                ?: throw IOException("Created archive source fingerprint is missing")
            if (declaredSize != requiredFingerprint.bytes) {
                throw IOException(
                    "Created archive entry size does not match the written source bytes",
                )
            }
            if (expected.size >= 0L && expected.size != requiredFingerprint.bytes) {
                throw IOException("Archive source size changed while it was being written")
            }
            return requiredFingerprint
        }

        fun requireComplete() {
            if (seen.any { !it }) {
                throw IOException("Created archive is missing one or more source entries")
            }
        }
    }

    private data class LocalFileIdentity(
        val canonicalFile: File,
        val length: Long,
        val lastModified: Long,
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1_024
        const val SHA_256 = "SHA-256"
        const val SIZE_UNKNOWN = -1L
    }
}
