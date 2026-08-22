@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

internal object TarArchiveBackend : ArchiveBackend {
    override val format = ArchiveFormat.TAR

    override val capabilities = FormatCapabilities(
        canDetect = true,
        canList = true,
        canPreview = true,
        canOpen = true,
        canExtract = true,
        canCreate = false,
        canAdd = false,
        canDelete = false,
        canRename = false,
        password = ArchiveOptionMode.UNSUPPORTED,
        filenameEncryption = ArchiveOptionMode.UNSUPPORTED,
        splitVolumes = ArchiveOptionMode.UNSUPPORTED,
        compressionLevels = emptyList(),
        limitations = setOf(
            ArchiveFormatLimitation.PASSWORD_UNAVAILABLE,
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE,
            ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE,
            ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE,
        ),
    )

    override fun openReader(source: File, options: ArchiveReaderOptions): ArchiveReader {
        if (!TarArchiveAccess.hasTarSignature(source)) {
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.INVALID_SIGNATURE,
                stage = ArchiveFailureStage.FORMAT_DETECTION,
                message = "TAR signature is not present",
            )
        }
        val resolvedOptions = ArchiveReaderOptions()
        return try {
            TarArchiveReader(
                source = source,
                entries = TarArchiveAccess.readEntries(source),
                options = resolvedOptions,
            )
        } catch (error: ArchiveBackendException) {
            resolvedOptions.clearPassword()
            throw error
        } catch (error: Exception) {
            resolvedOptions.clearPassword()
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.MALFORMED,
                stage = ArchiveFailureStage.INDEX,
                message = "TAR directory metadata cannot be read",
                cause = error,
            )
        }
    }
}

private class TarArchiveReader(
    private val source: File,
    override val entries: List<ArchiveReaderEntry>,
    override val options: ArchiveReaderOptions,
) : ArchiveReader {
    override val format = ArchiveFormat.TAR
    override val formatCapabilities = TarArchiveBackend.capabilities

    override fun openEntry(entry: ArchiveReaderEntry): InputStream {
        val token = entry.backendToken as? TarEntryToken
            ?: throw IllegalArgumentException("Archive entry belongs to a different backend")
        require(entries.getOrNull(entry.ordinal) === entry) {
            "Archive entry does not belong to this reader"
        }
        if (entry.isDirectory) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNKNOWN_SELECTION,
                message = "TAR directory entries do not expose a data stream",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        if (!entry.capabilities.canExtract) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNSUPPORTED_METHOD,
                message = "TAR entry type cannot be extracted safely",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }

        val input = TarArchiveAccess.open(source)
        try {
            var ordinal = 0
            while (true) {
                val liveTarEntry = input.nextEntry
                    ?: changed("TAR entry no longer exists")
                TarArchiveAccess.requireValidChecksum(liveTarEntry)
                val liveEntry = TarArchiveAccess.toReaderEntry(input, liveTarEntry, ordinal)
                if (ordinal == token.ordinal) {
                    if (!sameMetadata(liveEntry, entry)) {
                        changed("TAR entry metadata changed")
                    }
                    if (!liveEntry.capabilities.canExtract) {
                        changed("TAR entry readability changed")
                    }
                    return input
                }
                ordinal++
            }
        } catch (error: Throwable) {
            runCatching { input.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
    }

    override fun close() {
        options.clearPassword()
    }

    private fun sameMetadata(
        liveEntry: ArchiveReaderEntry,
        expectedEntry: ArchiveReaderEntry,
    ): Boolean = liveEntry.name == expectedEntry.name &&
        liveEntry.isDirectory == expectedEntry.isDirectory &&
        liveEntry.compressionMethod == expectedEntry.compressionMethod &&
        liveEntry.compressionMethodId == expectedEntry.compressionMethodId &&
        liveEntry.isEncrypted == expectedEntry.isEncrypted &&
        liveEntry.encryptionMethod == expectedEntry.encryptionMethod &&
        liveEntry.capabilities == expectedEntry.capabilities &&
        liveEntry.compressedSize == expectedEntry.compressedSize &&
        liveEntry.size == expectedEntry.size &&
        liveEntry.crc == expectedEntry.crc &&
        liveEntry.time == expectedEntry.time

    private fun changed(message: String): Nothing = throw ArchiveExtractionException(
        code = ArchiveFailureCode.SOURCE_CHANGED,
        message = message,
        format = format,
        stage = ArchiveFailureStage.ENTRY_DATA,
    )
}

private data class TarEntryToken(
    val ordinal: Int,
)

private enum class TarEntryType(
    val methodId: String,
) {
    REGULAR_FILE("TAR"),
    DIRECTORY("DIRECTORY"),
    SYMBOLIC_LINK("SYMBOLIC_LINK"),
    HARD_LINK("HARD_LINK"),
    SPARSE_FILE("SPARSE_FILE"),
    FIFO("FIFO"),
    CHARACTER_DEVICE("CHARACTER_DEVICE"),
    BLOCK_DEVICE("BLOCK_DEVICE"),
    OTHER("SPECIAL_ENTRY"),
    ;
}

internal object TarArchiveAccess {
    private const val RECORD_SIZE = 512
    private const val END_MARKER_SIZE = RECORD_SIZE * 2

    fun hasTarSignature(source: File): Boolean {
        val length = source.length()
        if (length < END_MARKER_SIZE || length % RECORD_SIZE != 0L) return false

        val prefix = ByteArray(END_MARKER_SIZE)
        val bytesRead = FileInputStream(source).use { input ->
            var total = 0
            while (total < prefix.size) {
                val read = input.read(prefix, total, prefix.size - total)
                if (read < 0) break
                if (read == 0) continue
                total += read
            }
            total
        }
        if (bytesRead < RECORD_SIZE) return false

        val firstRecordIsEmpty = prefix.copyOfRange(0, RECORD_SIZE).all { it == 0.toByte() }
        if (firstRecordIsEmpty) {
            return bytesRead == END_MARKER_SIZE && prefix.all { it == 0.toByte() }
        }
        if (TarArchiveInputStream.matches(prefix, RECORD_SIZE)) return true

        return runCatching {
            TarArchiveEntry(prefix.copyOf(RECORD_SIZE)).let { entry ->
                entry.isCheckSumOK && entry.name.isNotBlank()
            }
        }.getOrDefault(false)
    }

    fun readEntries(source: File): List<ArchiveReaderEntry> = open(source).use { input ->
        buildList {
            var ordinal = 0
            while (true) {
                val entry = input.nextEntry ?: break
                requireValidChecksum(entry)
                add(toReaderEntry(input, entry, ordinal))
                ordinal++
            }
        }
    }

    fun open(source: File): TarArchiveInputStream = TarArchiveInputStream(
        BufferedInputStream(FileInputStream(source)),
    )

    fun requireValidChecksum(entry: TarArchiveEntry) {
        if (!entry.isCheckSumOK) throw IOException("TAR entry header checksum is invalid")
    }

    fun toReaderEntry(
        input: TarArchiveInputStream,
        entry: TarArchiveEntry,
        ordinal: Int,
    ): ArchiveReaderEntry {
        val entryType = entryType(entry)
        val canReadFile = entryType == TarEntryType.REGULAR_FILE &&
            input.canReadEntryData(entry)
        val size = when {
            entry.isDirectory -> entry.size
            entry.isSparse -> entry.realSize
            else -> entry.size
        }
        val compressedSize = if (entry.isSparse) entry.size else size
        val capabilities = when {
            entry.isDirectory -> ArchiveEntryCapabilities.DIRECTORY
            canReadFile -> ArchiveEntryCapabilities.READABLE_FILE
            else -> ArchiveEntryCapabilities(
                canOpen = false,
                canExtract = false,
                canDelete = false,
                canRename = false,
                limitations = setOf(
                    ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE,
                    ArchiveEntryLimitation.MUTATION_UNAVAILABLE,
                ),
            )
        }
        return ArchiveReaderEntry(
            ordinal = ordinal,
            name = entry.name,
            isDirectory = entry.isDirectory,
            compressionMethod = ArchiveCompressionMethod.STORED,
            compressionMethodId = entryType.methodId,
            isEncrypted = false,
            encryptionMethod = null,
            capabilities = capabilities,
            compressedSize = compressedSize,
            size = size,
            crc = null,
            time = entry.modTime.time,
            backendToken = TarEntryToken(ordinal),
        )
    }

    private fun entryType(entry: TarArchiveEntry): TarEntryType = when {
        entry.isDirectory -> TarEntryType.DIRECTORY
        entry.isSparse -> TarEntryType.SPARSE_FILE
        entry.isSymbolicLink -> TarEntryType.SYMBOLIC_LINK
        entry.isLink -> TarEntryType.HARD_LINK
        entry.isFIFO -> TarEntryType.FIFO
        entry.isCharacterDevice -> TarEntryType.CHARACTER_DEVICE
        entry.isBlockDevice -> TarEntryType.BLOCK_DEVICE
        entry.isFile -> TarEntryType.REGULAR_FILE
        else -> TarEntryType.OTHER
    }
}
