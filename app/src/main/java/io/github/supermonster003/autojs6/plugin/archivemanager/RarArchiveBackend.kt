package io.github.supermonster003.autojs6.plugin.archivemanager

import com.github.junrar.Archive as JunrarArchive
import com.github.junrar.ArchiveOptions as JunrarArchiveOptions
import com.github.junrar.RarFormat
import com.github.junrar.crypt.Rar5Crypt
import com.github.junrar.exception.CrcErrorException
import com.github.junrar.exception.MissingNextVolumeException
import com.github.junrar.exception.MissingPreviousVolumeException
import com.github.junrar.exception.UnsupportedDictionarySizeException
import com.github.junrar.exception.UnsupportedRarEncryptedException
import com.github.junrar.exception.UnsupportedRarMethodException
import com.github.junrar.exception.UnsupportedRarVersionException
import com.github.junrar.exception.WrongPasswordException
import com.github.junrar.io.SeekableReadOnlyByteChannel
import com.github.junrar.rarfile.EndArcHeader
import com.github.junrar.rarfile.FileHeader
import com.github.junrar.rarfile.rar5.Rar5BlockType
import com.github.junrar.rarfile.rar5.Rar5MainHeader
import com.github.junrar.volume.Volume
import com.github.junrar.volume.VolumeHelper
import com.github.junrar.volume.VolumeManager
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.util.Arrays
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Read-only RAR support. Under the UnRAR license, this code may not be used to develop a RAR
 * (WinRAR) compatible archiver; RAR creation and mutation are intentionally not implemented.
 */
internal object RarArchiveBackend : ArchiveBackend {
    override val format = ArchiveFormat.RAR

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
        password = ArchiveOptionMode.OPTIONAL,
        filenameEncryption = ArchiveOptionMode.UNSUPPORTED,
        splitVolumes = ArchiveOptionMode.OPTIONAL,
        compressionLevels = emptyList(),
        limitations = setOf(
            ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT,
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE,
            ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE,
        ),
        canReadSplitVolumes = true,
    )

    override fun openReader(source: ArchiveReadSource, options: ArchiveReaderOptions): ArchiveReader {
        if (!hasSignature(source)) {
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.INVALID_SIGNATURE,
                stage = ArchiveFailureStage.FORMAT_DETECTION,
                message = "RAR signature is not present",
            )
        }

        val password = options.passwordChars()
        val resolvedOptions = ArchiveReaderOptions(password = password)
        var archive: JunrarArchive? = null
        try {
            archive = openArchive(source, password)
            requirePlausibleDirectory(archive)
            if (password != null) validatePasswordChecks(archive.fileHeaders, password)
            val entries = archive.fileHeaders.mapIndexed { ordinal, header ->
                header.toReaderEntry(
                    ordinal = ordinal,
                    rarFormat = archive.format,
                    passwordProvided = password != null,
                    nextVolumeAvailable = archive.nextVolumeAvailable(source),
                )
            }
            return RarArchiveReader(
                archive = archive,
                entries = entries,
                options = resolvedOptions,
            )
        } catch (error: Throwable) {
            resolvedOptions.clearPassword()
            archive?.let { opened ->
                runCatching { opened.close() }.exceptionOrNull()?.let(error::addSuppressed)
            }
            if (error !is Exception && error !is LinkageError) throw error
            throw mapOpenFailure(error, password != null)
        } finally {
            password?.fill('\u0000')
        }
    }

    private fun openArchive(source: ArchiveReadSource, password: CharArray?): JunrarArchive {
        val options = JunrarArchiveOptions.builder()
            .maxDictionarySize(DECODER_DICTIONARY_LIMIT_BYTES)
            .apply { if (password != null) password(password) }
            .build()
        return try {
            JunrarArchive(
                SourceVolumeManager(source),
                options,
            )
        } finally {
            options.close()
        }
    }

    private fun FileHeader.toReaderEntry(
        ordinal: Int,
        rarFormat: RarFormat,
        passwordProvided: Boolean,
        nextVolumeAvailable: Boolean,
    ): ArchiveReaderEntry {
        val missingVolume = isSplitBefore || isSplitAfter && !nextVolumeAvailable
        val redirected = redirection != null
        val unknownSize = isUnpSizeUnknown
        val unsupportedType = redirected || unknownSize
        val limitations = buildSet {
            if (isDirectory) add(ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA)
            if (isEncrypted) add(ArchiveEntryLimitation.ENCRYPTED)
            if (missingVolume) add(ArchiveEntryLimitation.MISSING_VOLUME)
            if (unsupportedType) add(ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE)
            add(ArchiveEntryLimitation.MUTATION_UNAVAILABLE)
        }
        val extractable = !missingVolume &&
            !unsupportedType &&
            (isDirectory || !isEncrypted || passwordProvided)
        val method = unpMethod.toInt() and UNSIGNED_BYTE_MASK
        val stored = if (rarFormat == RarFormat.RAR50) method == RAR5_STORED_METHOD else {
            method == LEGACY_STORED_METHOD
        }
        return ArchiveReaderEntry(
            ordinal = ordinal,
            name = fileName ?: throw IOException("RAR entry has no name"),
            isDirectory = isDirectory,
            compressionMethod = if (stored) {
                ArchiveCompressionMethod.STORED
            } else {
                ArchiveCompressionMethod.OTHER
            },
            compressionMethodId = buildString {
                append(rarFormat.name)
                append(":v")
                append(unpVersion.toInt() and UNSIGNED_BYTE_MASK)
                append(":m")
                append(method.toString(RADIX_HEXADECIMAL).uppercase(Locale.ROOT))
            },
            isEncrypted = isEncrypted,
            encryptionMethod = encryptionMethod(rarFormat),
            capabilities = ArchiveEntryCapabilities(
                canOpen = !isDirectory && extractable,
                canExtract = extractable,
                canDelete = false,
                canRename = false,
                limitations = limitations,
            ),
            compressedSize = fullPackSize,
            size = if (unknownSize) UNKNOWN_UNCOMPRESSED_SIZE else fullUnpackSize,
            // A split header carries the CRC of its current segment, not the reconstructed file.
            crc = if (!isSplitBefore && !isSplitAfter && hasFileCrc() && !isUseHashKey) {
                Integer.toUnsignedLong(fileCRC)
            } else {
                null
            },
            time = lastModifiedTime?.toMillis(),
            backendToken = this,
        )
    }

    private fun requirePlausibleDirectory(archive: JunrarArchive) {
        val headers = archive.headers
        val hasMainHeader = if (archive.format == RarFormat.RAR50) {
            headers.any { header ->
                header is Rar5MainHeader && header.rar5Type == Rar5BlockType.MAIN
            }
        } else {
            archive.mainHeader != null
        }
        if (!hasMainHeader) throw IOException("RAR main header is missing")

        if (archive.fileHeaders.isEmpty() && archive.format != RarFormat.RAR14) {
            val hasEndHeader = if (archive.format == RarFormat.RAR50) {
                headers.any { header ->
                    header is Rar5MainHeader && header.rar5Type == Rar5BlockType.ENDARC
                }
            } else {
                headers.any { it is EndArcHeader }
            }
            if (!hasEndHeader) throw IOException("RAR directory ended before its end header")
        }
    }

    private fun validatePasswordChecks(headers: List<FileHeader>, password: CharArray) {
        val header = headers.firstOrNull { candidate ->
            candidate.isEncrypted && candidate.isRar5Family && candidate.isUsePswCheck
        } ?: return
        val encodedPassword = Rar5Crypt.passwordToUtf8(password)
        val derived = try {
            Rar5Crypt.deriveKey(encodedPassword, header.salt16, header.lg2Count)
        } finally {
            encodedPassword.fill(0)
        }
        try {
            if (!Arrays.equals(derived.pswCheck, header.pswCheck)) {
                throw WrongPasswordException("RAR5 password check failed")
            }
        } finally {
            derived.aesKey.fill(0)
            derived.hashKey.fill(0)
            derived.pswCheck.fill(0)
        }
    }

    private fun FileHeader.encryptionMethod(rarFormat: RarFormat): ArchiveEncryptionMethod? {
        if (!isEncrypted) return null
        val version = unpVersion.toInt() and UNSIGNED_BYTE_MASK
        return if (rarFormat == RarFormat.RAR50 || version >= RAR3_AES_UNPACK_VERSION) {
            ArchiveEncryptionMethod.AES
        } else {
            ArchiveEncryptionMethod.OTHER
        }
    }

    private fun JunrarArchive.nextVolumeAvailable(source: ArchiveReadSource): Boolean {
        val volumeSet = source.volumeSet ?: return false
        val nextName = VolumeHelper.nextVolumeName(
            source.displayName,
            usesOldVolumeNumbering(),
        ) ?: return false
        return volumeSet.volumes.any { volume ->
            collisionKey(volume.displayName) == collisionKey(nextName)
        }
    }

    private fun JunrarArchive.usesOldVolumeNumbering(): Boolean {
        val header = mainHeader
        return header != null && (!header.isNewNumbering || isOldFormat)
    }

    private fun hasSignature(source: ArchiveReadSource): Boolean {
        val prefix = ByteArray(MAX_SIGNATURE_SIZE)
        val length = source.openInputStream().use { input ->
            var total = 0
            while (total < prefix.size) {
                val read = input.read(prefix, total, prefix.size - total)
                if (read < 0) break
                if (read == 0) continue
                total += read
            }
            total
        }
        return prefix.startsWith(RAR14_SIGNATURE, length) ||
            prefix.startsWith(RAR15_SIGNATURE, length) ||
            prefix.startsWith(RAR50_SIGNATURE, length)
    }

    private fun ByteArray.startsWith(signature: ByteArray, available: Int): Boolean {
        if (available < signature.size) return false
        return signature.indices.all { index -> this[index] == signature[index] }
    }

    private const val DECODER_DICTIONARY_LIMIT_BYTES = 256L * 1_024L * 1_024L
    private const val MAX_SIGNATURE_SIZE = 8
    private const val UNSIGNED_BYTE_MASK = 0xFF
    private const val RAR5_STORED_METHOD = 0
    private const val LEGACY_STORED_METHOD = 0x30
    private const val RAR3_AES_UNPACK_VERSION = 29
    private const val RADIX_HEXADECIMAL = 16
    private const val UNKNOWN_UNCOMPRESSED_SIZE = -1L

    private val RAR14_SIGNATURE = byteArrayOf(0x52, 0x45, 0x7E, 0x5E)
    private val RAR15_SIGNATURE = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00)
    private val RAR50_SIGNATURE = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)
}

private class RarArchiveReader(
    private val archive: JunrarArchive,
    override val entries: List<ArchiveReaderEntry>,
    override val options: ArchiveReaderOptions,
) : ArchiveReader {
    override val format = ArchiveFormat.RAR
    override val formatCapabilities = RarArchiveBackend.capabilities

    private var closed = false
    private var activeStream: RarEntryInputStream? = null

    @Synchronized
    override fun openEntry(entry: ArchiveReaderEntry): InputStream {
        check(!closed) { "RAR reader is closed" }
        val header = entry.backendToken as? FileHeader
            ?: throw IllegalArgumentException("Archive entry belongs to a different backend")
        require(entries.getOrNull(entry.ordinal) === entry) {
            "Archive entry does not belong to this reader"
        }
        check(activeStream == null) { "Only one RAR entry stream may be open at a time" }
        if (entry.isDirectory) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNKNOWN_SELECTION,
                message = "RAR directory entries do not expose a data stream",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        if (ArchiveEntryLimitation.MISSING_VOLUME in entry.capabilities.limitations) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.MISSING_VOLUME,
                message = "Additional RAR volumes are unavailable",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        if (entry.isEncrypted && !options.hasPassword) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.PASSWORD_REQUIRED,
                message = "RAR entry requires a password",
                format = format,
                stage = ArchiveFailureStage.PASSWORD,
            )
        }
        if (!entry.capabilities.canExtract) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNSUPPORTED_METHOD,
                message = "RAR entry data cannot be read by this backend",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }

        lateinit var stream: RarEntryInputStream
        stream = RarEntryInputStream(
            archive = archive,
            header = header,
            passwordProvided = options.hasPassword,
            threadName = "rar-entry-${entry.ordinal}",
            onClosed = { release(stream) },
        )
        activeStream = stream
        return stream
    }

    @Synchronized
    private fun release(stream: RarEntryInputStream) {
        if (activeStream === stream) activeStream = null
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        try {
            activeStream?.close()
            activeStream = null
            archive.close()
        } finally {
            options.clearPassword()
        }
    }
}

private class RarEntryInputStream(
    private val archive: JunrarArchive,
    private val header: FileHeader,
    private val passwordProvided: Boolean,
    private val threadName: String,
    private val onClosed: () -> Unit,
) : InputStream() {
    private val input = PipedInputStream(PIPE_BUFFER_SIZE)
    private val output = PipedOutputStream(input)
    private val failure = AtomicReference<IOException?>()
    private val completed = AtomicBoolean(false)
    private var worker: Thread? = null
    private var closed = false

    override fun read(): Int = readMapped { input.read() }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        readMapped { input.read(buffer, offset, length) }

    override fun available(): Int = input.available()

    private inline fun readMapped(block: () -> Int): Int {
        ensureStarted()
        val result = try {
            block()
        } catch (error: IOException) {
            failure.get()?.let {
                complete()
                throw it
            }
            throw error
        }
        if (result < 0) {
            complete()
            failure.get()?.let { throw it }
        }
        return result
    }

    @Synchronized
    private fun ensureStarted() {
        if (closed) throw IOException("RAR entry stream is closed")
        if (worker != null) return
        worker = Thread(
            {
                try {
                    archive.extractFile(header, output)
                } catch (error: Throwable) {
                    failure.compareAndSet(
                        null,
                        mapRarEntryFailure(error, header.isEncrypted, passwordProvided),
                    )
                } finally {
                    runCatching { output.close() }
                }
            },
            threadName,
        ).apply {
            isDaemon = true
            start()
        }
    }

    override fun close() {
        val running = synchronized(this) {
            if (closed) return
            closed = true
            runCatching { input.close() }
            runCatching { output.close() }
            worker?.also(Thread::interrupt)
        }
        if (running != null && running !== Thread.currentThread()) {
            try {
                running.join(WORKER_CLOSE_WAIT_MILLIS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        complete()
    }

    private fun complete() {
        if (completed.compareAndSet(false, true)) onClosed()
    }

    private companion object {
        const val PIPE_BUFFER_SIZE = 32 * 1_024
        const val WORKER_CLOSE_WAIT_MILLIS = 2_000L
    }
}

private class SourceVolumeManager(
    private val primarySource: ArchiveReadSource,
) : VolumeManager {
    override fun nextVolume(archive: JunrarArchive, lastVolume: Volume?): Volume? {
        if (lastVolume == null) {
            return SourceVolume(
                archive = archive,
                source = primarySource,
                displayName = primarySource.displayName,
            )
        }
        val previous = lastVolume as? SourceVolume ?: return null
        val header = archive.mainHeader
        val oldNumbering = header != null && (!header.isNewNumbering || archive.isOldFormat)
        val nextName = VolumeHelper.nextVolumeName(previous.displayName, oldNumbering) ?: return null
        val nextSource = primarySource.volumeSet?.openSource(nextName) ?: return null
        return SourceVolume(archive, nextSource, nextName)
    }
}

private class SourceVolume(
    private val archive: JunrarArchive,
    private val source: ArchiveReadSource,
    val displayName: String,
) : Volume {
    override fun getChannel(): SeekableReadOnlyByteChannel =
        RarSeekableChannel(source.openSeekableChannel())

    override fun getLength(): Long = source.identity().length

    override fun getArchive(): JunrarArchive = archive
}

private class RarSeekableChannel(
    private val channel: SeekableByteChannel,
) : SeekableReadOnlyByteChannel {
    override fun getPosition(): Long = channel.position()

    override fun setPosition(pos: Long) {
        channel.position(pos)
    }

    override fun read(): Int {
        val buffer = ByteBuffer.allocate(1)
        val count = channel.read(buffer)
        return if (count < 0) -1 else buffer.array()[0].toInt() and 0xFF
    }

    override fun read(buffer: ByteArray, off: Int, count: Int): Int =
        channel.read(ByteBuffer.wrap(buffer, off, count))

    override fun readFully(buffer: ByteArray, count: Int): Int {
        require(count in 0..buffer.size) { "RAR read length is outside the destination buffer" }
        var total = 0
        while (total < count) {
            val read = read(buffer, total, count - total)
            if (read < 0) throw EOFException("RAR volume ended before the requested data")
            if (read == 0) continue
            total += read
        }
        return total
    }

    override fun close() {
        channel.close()
    }
}

private fun mapOpenFailure(error: Throwable, passwordProvided: Boolean): ArchiveBackendException {
    if (error is ArchiveBackendException) return error
    val failure = when {
        error.findRarCause<WrongPasswordException>() != null -> if (passwordProvided) {
            ArchiveBackendFailure.WRONG_PASSWORD
        } else {
            ArchiveBackendFailure.PASSWORD_REQUIRED
        }
        error.hasMissingRarVolume() -> ArchiveBackendFailure.MISSING_VOLUME
        error.hasUnsupportedRarFeature() -> ArchiveBackendFailure.UNSUPPORTED_METHOD
        else -> ArchiveBackendFailure.MALFORMED
    }
    return ArchiveBackendException(
        format = ArchiveFormat.RAR,
        failure = failure,
        stage = when (failure) {
            ArchiveBackendFailure.INVALID_SIGNATURE -> ArchiveFailureStage.FORMAT_DETECTION
            ArchiveBackendFailure.PASSWORD_REQUIRED,
            ArchiveBackendFailure.WRONG_PASSWORD,
            -> ArchiveFailureStage.PASSWORD
            ArchiveBackendFailure.INVALID_OPTIONS,
            ArchiveBackendFailure.MALFORMED,
            ArchiveBackendFailure.MISSING_VOLUME,
            ArchiveBackendFailure.UNSUPPORTED_METHOD,
            -> ArchiveFailureStage.INDEX
        },
        message = when (failure) {
            ArchiveBackendFailure.INVALID_SIGNATURE -> "RAR signature is not present"
            ArchiveBackendFailure.INVALID_OPTIONS -> "RAR reader options are invalid"
            ArchiveBackendFailure.UNSUPPORTED_METHOD -> "RAR feature is not supported"
            ArchiveBackendFailure.MISSING_VOLUME -> "RAR volumes are missing or unavailable"
            ArchiveBackendFailure.PASSWORD_REQUIRED -> "RAR password is required"
            ArchiveBackendFailure.WRONG_PASSWORD -> "RAR password is incorrect"
            ArchiveBackendFailure.MALFORMED -> "RAR directory metadata cannot be read"
        },
        cause = error,
    )
}

private fun mapRarEntryFailure(
    error: Throwable,
    encrypted: Boolean,
    passwordProvided: Boolean,
): IOException {
    if (error is ArchiveException) return error
    if (error.findRarCause<ArchiveVolumeChangedException>() != null) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.SOURCE_CHANGED,
            message = "Archive volumes changed or became unavailable",
            cause = error,
            format = ArchiveFormat.RAR,
            stage = ArchiveFailureStage.INPUT,
        )
    }
    if (error.findRarCause<WrongPasswordException>() != null) {
        return ArchiveExtractionException(
            code = if (passwordProvided) {
                ArchiveFailureCode.WRONG_PASSWORD
            } else {
                ArchiveFailureCode.PASSWORD_REQUIRED
            },
            message = if (passwordProvided) {
                "RAR password is incorrect"
            } else {
                "RAR entry requires a password"
            },
            cause = error,
            format = ArchiveFormat.RAR,
            stage = ArchiveFailureStage.PASSWORD,
        )
    }
    if (error.hasMissingRarVolume()) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.MISSING_VOLUME,
            message = "Additional RAR volumes are unavailable",
            cause = error,
            format = ArchiveFormat.RAR,
            stage = ArchiveFailureStage.ENTRY_DATA,
        )
    }
    if (error.hasUnsupportedRarFeature()) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.UNSUPPORTED_METHOD,
            message = "RAR compression or encryption method is not supported",
            cause = error,
            format = ArchiveFormat.RAR,
            stage = ArchiveFailureStage.ENTRY_DATA,
        )
    }
    if (encrypted && passwordProvided && error.findRarCause<CrcErrorException>() != null) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.WRONG_PASSWORD,
            message = "RAR password is incorrect or encrypted data is damaged",
            cause = error,
            format = ArchiveFormat.RAR,
            stage = ArchiveFailureStage.PASSWORD,
        )
    }
    return IOException("RAR entry data cannot be read", error)
}

private inline fun <reified T : Throwable> Throwable.findRarCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

private fun Throwable.hasMissingRarVolume(): Boolean =
    findRarCause<MissingNextVolumeException>() != null ||
        findRarCause<MissingPreviousVolumeException>() != null

private fun Throwable.hasUnsupportedRarFeature(): Boolean =
    this is LinkageError ||
        findRarCause<UnsupportedDictionarySizeException>() != null ||
        findRarCause<UnsupportedRarEncryptedException>() != null ||
        findRarCause<UnsupportedRarMethodException>() != null ||
        findRarCause<UnsupportedRarVersionException>() != null
