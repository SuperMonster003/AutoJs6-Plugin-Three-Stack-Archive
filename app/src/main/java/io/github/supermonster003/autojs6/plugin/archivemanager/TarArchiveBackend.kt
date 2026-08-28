@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import com.github.luben.zstd.ZstdInputStream
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.tukaani.xz.MemoryLimitException
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

internal object TarArchiveBackend : TarArchiveBackendBase(
    format = ArchiveFormat.TAR,
    container = TarContainer.PLAIN,
    canMutate = true,
), ArchiveMutationBackend {
    override val mutationCapabilities = TAR_MUTATION_CAPABILITIES

    override fun mutationAvailability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
        tarMutationAvailability(snapshot)

    override fun createMutationProvider(
        session: ExplorerActionHostSessionClient,
        cacheDirectory: File,
        engine: ArchiveEngine,
    ): ArchiveMutationProvider = TarArchiveMutationProvider(session, cacheDirectory, engine)
}

internal object TarGzipArchiveBackend : TarArchiveBackendBase(
    format = ArchiveFormat.TAR_GZIP,
    container = TarContainer.GZIP,
)

internal object TarXzArchiveBackend : TarArchiveBackendBase(
    format = ArchiveFormat.TAR_XZ,
    container = TarContainer.XZ,
)

internal object TarBzip2ArchiveBackend : TarArchiveBackendBase(
    format = ArchiveFormat.TAR_BZIP2,
    container = TarContainer.BZIP2,
)

internal object TarZstdArchiveBackend : TarArchiveBackendBase(
    format = ArchiveFormat.TAR_ZSTD,
    container = TarContainer.ZSTD,
)

internal abstract class TarArchiveBackendBase(
    override val format: ArchiveFormat,
    private val container: TarContainer,
    private val canMutate: Boolean = false,
) : ArchiveBackend {
    init {
        require(format.isTarFamily)
    }

    override val capabilities = tarCapabilities(container, canMutate)

    override fun createWriter(
        session: IExplorerActionHostSession,
        cacheDirectory: File?,
    ): ArchiveWriter =
        TarArchiveCreator(format, capabilities, session, cacheDirectory)

    override fun openReader(source: ArchiveReadSource, options: ArchiveReaderOptions): ArchiveReader {
        if (!container.hasOuterSignature(source)) {
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.INVALID_SIGNATURE,
                stage = ArchiveFailureStage.FORMAT_DETECTION,
                message = "${format.displayName} signature is not present",
            )
        }
        val resolvedOptions = ArchiveReaderOptions()
        return try {
            container.requirePlausibleStructure(source)
            TarArchiveReader(
                source = source,
                entries = TarArchiveAccess.readEntries(source, container, canMutate),
                options = resolvedOptions,
                format = format,
                formatCapabilities = capabilities,
                container = container,
                canMutate = canMutate,
            )
        } catch (error: TarPayloadSignatureException) {
            resolvedOptions.clearPassword()
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.INVALID_SIGNATURE,
                stage = ArchiveFailureStage.FORMAT_DETECTION,
                message = "${format.displayName} payload is not a TAR archive",
                cause = error,
            )
        } catch (error: MemoryLimitException) {
            resolvedOptions.clearPassword()
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.UNSUPPORTED_METHOD,
                stage = ArchiveFailureStage.INDEX,
                message = "XZ dictionary exceeds the decoder memory limit",
                cause = error,
            )
        } catch (error: ArchiveBackendException) {
            resolvedOptions.clearPassword()
            throw error
        } catch (error: LinkageError) {
            resolvedOptions.clearPassword()
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.UNSUPPORTED_METHOD,
                stage = ArchiveFailureStage.INDEX,
                message = "${format.displayName} decoder is unavailable on this runtime",
                cause = error,
            )
        } catch (error: Exception) {
            resolvedOptions.clearPassword()
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.MALFORMED,
                stage = ArchiveFailureStage.INDEX,
                message = "${format.displayName} directory metadata cannot be read",
                cause = error,
            )
        }
    }
}

private fun tarCapabilities(
    container: TarContainer,
    canMutate: Boolean,
) = FormatCapabilities(
    canDetect = true,
    canList = true,
    canPreview = true,
    canOpen = true,
    canExtract = true,
    canCreate = true,
    canAdd = canMutate,
    canDelete = canMutate,
    canRename = canMutate,
    password = ArchiveOptionMode.UNSUPPORTED,
    filenameEncryption = ArchiveOptionMode.UNSUPPORTED,
    splitVolumes = ArchiveOptionMode.UNSUPPORTED,
    compressionLevels = when (container) {
        TarContainer.PLAIN -> listOf(0)
        TarContainer.GZIP -> (0..9).toList()
        TarContainer.XZ,
        TarContainer.BZIP2,
        TarContainer.ZSTD,
        -> (1..9).toList()
    },
    limitations = setOf(
        ArchiveFormatLimitation.PASSWORD_UNAVAILABLE,
        ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE,
        ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE,
        ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE,
    ),
)

private class TarArchiveReader(
    private val source: ArchiveReadSource,
    override val entries: List<ArchiveReaderEntry>,
    override val options: ArchiveReaderOptions,
    override val format: ArchiveFormat,
    override val formatCapabilities: FormatCapabilities,
    private val container: TarContainer,
    private val canMutate: Boolean,
) : ArchiveReader {

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

        val input = TarArchiveAccess.open(
            source = source,
            container = container,
            verifyContainerIntegrity = false,
        )
        try {
            var ordinal = 0
            while (true) {
                val liveTarEntry = input.nextEntry
                    ?: changed("TAR entry no longer exists")
                TarArchiveAccess.requireValidChecksum(liveTarEntry)
                val liveEntry = TarArchiveAccess.toReaderEntry(
                    input = input,
                    entry = liveTarEntry,
                    ordinal = ordinal,
                    container = container,
                    canMutate = canMutate,
                )
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

internal enum class TarContainer {
    PLAIN {
        override fun hasOuterSignature(source: ArchiveReadSource): Boolean =
            TarArchiveAccess.hasRawTarSignature(source)

        override fun openPayload(input: InputStream): InputStream = input
    },
    GZIP {
        override fun hasOuterSignature(source: ArchiveReadSource): Boolean =
            sourceSignatureMatches(source, GzipCompressorInputStream::matches)

        override fun openPayload(input: InputStream): InputStream =
            GzipCompressorInputStream(input, true)
    },
    XZ {
        override fun hasOuterSignature(source: ArchiveReadSource): Boolean =
            sourceSignatureMatches(source, XZCompressorInputStream::matches)

        override fun openPayload(input: InputStream): InputStream =
            XZCompressorInputStream(input, true, XZ_MEMORY_LIMIT_KIB)
    },
    BZIP2 {
        override fun hasOuterSignature(source: ArchiveReadSource): Boolean =
            sourceSignatureMatches(source, BZip2CompressorInputStream::matches)

        override fun openPayload(input: InputStream): InputStream =
            BZip2CompressorInputStream(input, true)
    },
    ZSTD {
        override fun hasOuterSignature(source: ArchiveReadSource): Boolean =
            sourceSignatureMatches(source, ::matchesZstdSignature)

        override fun openPayload(input: InputStream): InputStream =
            ZstdInputStream(input)
                .setContinuous(true)
                .setLongMax(ZSTD_WINDOW_LOG_MAX)

        override fun requirePlausibleStructure(source: ArchiveReadSource) {
            val signature = ByteArray(ZSTD_MAGIC_SIZE)
            val bytesRead = source.openInputStream().use { input -> input.readPrefix(signature) }
            val minimumSize = when {
                matchesStandardZstdSignature(signature, bytesRead) -> ZSTD_MINIMUM_FRAME_SIZE
                matchesSkippableZstdSignature(signature, bytesRead) -> ZSTD_SKIPPABLE_HEADER_SIZE
                else -> return
            }
            if (source.identity().length < minimumSize) {
                throw IOException("Zstandard frame is truncated")
            }
        }
    },
    ;

    abstract fun hasOuterSignature(source: ArchiveReadSource): Boolean

    abstract fun openPayload(input: InputStream): InputStream

    open fun requirePlausibleStructure(source: ArchiveReadSource) = Unit

    companion object {
        private const val SIGNATURE_BUFFER_SIZE = 12
        private const val XZ_MEMORY_LIMIT_KIB = 256 * 1_024
        private const val ZSTD_WINDOW_LOG_MAX = 28

        private fun matchesZstdSignature(signature: ByteArray, length: Int): Boolean =
            matchesStandardZstdSignature(signature, length) ||
                matchesSkippableZstdSignature(signature, length)

        private fun matchesStandardZstdSignature(
            signature: ByteArray,
            length: Int,
        ): Boolean {
            if (length < ZSTD_MAGIC_SIZE) return false
            val first = signature[0].toInt() and 0xFF
            val second = signature[1].toInt() and 0xFF
            val third = signature[2].toInt() and 0xFF
            val fourth = signature[3].toInt() and 0xFF
            return first == 0x28 && second == 0xB5 && third == 0x2F && fourth == 0xFD
        }

        private fun matchesSkippableZstdSignature(
            signature: ByteArray,
            length: Int,
        ): Boolean {
            if (length < ZSTD_MAGIC_SIZE) return false
            val first = signature[0].toInt() and 0xFF
            val second = signature[1].toInt() and 0xFF
            val third = signature[2].toInt() and 0xFF
            val fourth = signature[3].toInt() and 0xFF
            return first in 0x50..0x5F && second == 0x2A && third == 0x4D && fourth == 0x18
        }

        private fun sourceSignatureMatches(
            source: ArchiveReadSource,
            matcher: (ByteArray, Int) -> Boolean,
        ): Boolean {
            val signature = ByteArray(SIGNATURE_BUFFER_SIZE)
            val bytesRead = source.openInputStream().use { input -> input.readPrefix(signature) }
            return matcher(signature, bytesRead)
        }

        private const val ZSTD_MAGIC_SIZE = 4
        private const val ZSTD_MINIMUM_FRAME_SIZE = 9L
        private const val ZSTD_SKIPPABLE_HEADER_SIZE = 8L
    }
}

internal object TarArchiveAccess {
    private const val RECORD_SIZE = 512
    private const val END_MARKER_SIZE = RECORD_SIZE * 2

    fun hasRawTarSignature(source: ArchiveReadSource): Boolean {
        val length = source.identity().length
        if (length < END_MARKER_SIZE || length % RECORD_SIZE != 0L) return false

        return source.openInputStream().use { input ->
            hasTarSignature(BufferedInputStream(input))
        }
    }

    fun readEntries(
        source: ArchiveReadSource,
        container: TarContainer = TarContainer.PLAIN,
        canMutate: Boolean = false,
    ): List<ArchiveReaderEntry> = open(source, container).use { input ->
        buildList {
            var ordinal = 0
            while (true) {
                val entry = input.nextEntry ?: break
                requireValidChecksum(entry)
                add(toReaderEntry(input, entry, ordinal, container, canMutate))
                ordinal++
            }
        }
    }

    fun open(
        source: ArchiveReadSource,
        container: TarContainer = TarContainer.PLAIN,
        verifyContainerIntegrity: Boolean = true,
    ): TarArchiveInputStream {
        val sourceInput = BufferedInputStream(source.openInputStream())
        var payload: InputStream? = null
        try {
            payload = container.openPayload(sourceInput)
            val bufferedPayload = if (payload is BufferedInputStream) {
                payload
            } else {
                BufferedInputStream(payload)
            }
            payload = bufferedPayload
            if (!hasTarSignature(bufferedPayload)) {
                throw TarPayloadSignatureException()
            }
            return ContainerTarArchiveInputStream(
                payload = bufferedPayload,
                verifyContainerIntegrity =
                    verifyContainerIntegrity && container != TarContainer.PLAIN,
            )
        } catch (error: Throwable) {
            runCatching { (payload ?: sourceInput).close() }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            throw error
        }
    }

    fun requireValidChecksum(entry: TarArchiveEntry) {
        if (!entry.isCheckSumOK) throw IOException("TAR entry header checksum is invalid")
    }

    fun toReaderEntry(
        input: TarArchiveInputStream,
        entry: TarArchiveEntry,
        ordinal: Int,
        container: TarContainer = TarContainer.PLAIN,
        canMutate: Boolean = false,
    ): ArchiveReaderEntry {
        val entryType = entryType(entry)
        val canReadFile = entryType == TarEntryType.REGULAR_FILE &&
            input.canReadEntryData(entry)
        val size = when {
            entry.isDirectory -> entry.size
            entry.isSparse -> entry.realSize
            else -> entry.size
        }
        val compressedSize = when {
            entry.isDirectory -> 0L
            container != TarContainer.PLAIN -> UNKNOWN_COMPRESSED_SIZE
            entry.isSparse -> entry.size
            else -> size
        }
        val capabilities = when {
            entry.isDirectory -> ArchiveEntryCapabilities(
                canOpen = false,
                canExtract = true,
                canDelete = canMutate,
                canRename = canMutate,
                limitations = buildSet {
                    add(ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA)
                    if (!canMutate) add(ArchiveEntryLimitation.MUTATION_UNAVAILABLE)
                },
            )
            canReadFile -> ArchiveEntryCapabilities(
                canOpen = true,
                canExtract = true,
                canDelete = canMutate,
                canRename = canMutate,
                limitations = if (canMutate) {
                    emptySet()
                } else {
                    setOf(ArchiveEntryLimitation.MUTATION_UNAVAILABLE)
                },
            )
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

    private fun hasTarSignature(input: BufferedInputStream): Boolean {
        input.mark(END_MARKER_SIZE)
        val prefix = ByteArray(END_MARKER_SIZE)
        val bytesRead = input.readPrefix(prefix)
        input.reset()
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

    private const val UNKNOWN_COMPRESSED_SIZE = -1L

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

private class TarPayloadSignatureException : IOException("Compressed payload is not TAR")

private class ContainerTarArchiveInputStream(
    private val payload: InputStream,
    private val verifyContainerIntegrity: Boolean,
) : TarArchiveInputStream(payload) {
    private var closed = false

    override fun close() {
        if (closed) return
        closed = true

        var failure: Throwable? = null
        if (verifyContainerIntegrity) {
            try {
                val buffer = ByteArray(DRAIN_BUFFER_SIZE)
                while (true) {
                    val read = payload.read(buffer)
                    if (read < 0) break
                }
            } catch (error: Throwable) {
                failure = error
            }
        }
        try {
            super.close()
        } catch (error: Throwable) {
            if (failure == null) {
                failure = error
            } else {
                failure.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }

    private companion object {
        const val DRAIN_BUFFER_SIZE = 32 * 1_024
    }
}

private fun InputStream.readPrefix(destination: ByteArray): Int {
    var total = 0
    while (total < destination.size) {
        val read = read(destination, total, destination.size - total)
        if (read < 0) break
        if (read == 0) continue
        total += read
    }
    return total
}
