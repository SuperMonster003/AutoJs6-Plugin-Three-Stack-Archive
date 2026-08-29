package io.github.supermonster003.autojs6.plugin.archivemanager

import org.apache.commons.compress.MemoryLimitException
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.tukaani.xz.LZMA2InputStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAInputStream
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException

internal object SevenZArchiveBackend : ArchiveMutationBackend {
    override val format = ArchiveFormat.SEVEN_Z

    override val capabilities = FormatCapabilities(
        canDetect = true,
        canList = true,
        canPreview = true,
        canOpen = true,
        canExtract = true,
        canCreate = true,
        canAdd = true,
        canDelete = true,
        canRename = true,
        password = ArchiveOptionMode.OPTIONAL,
        filenameEncryption = ArchiveOptionMode.UNSUPPORTED,
        splitVolumes = ArchiveOptionMode.UNSUPPORTED,
        compressionLevels = (0..9).toList(),
        limitations = setOf(
            ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT,
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE,
            ArchiveFormatLimitation.SPLIT_CREATION_UNAVAILABLE,
            ArchiveFormatLimitation.SOLID_CREATION_UNAVAILABLE,
            ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE,
        ),
        canReadSplitVolumes = true,
    )

    override val mutationCapabilities = SEVEN_Z_MUTATION_CAPABILITIES

    override fun openReader(source: ArchiveReadSource, options: ArchiveReaderOptions): ArchiveReader {
        if (!hasSignature(source)) {
            throw ArchiveBackendException(
                format = format,
                failure = ArchiveBackendFailure.INVALID_SIGNATURE,
                stage = ArchiveFailureStage.FORMAT_DETECTION,
                message = "7Z signature is not present",
            )
        }

        val password = options.passwordChars()
        val resolvedOptions = ArchiveReaderOptions(password = password)
        var archive: SevenZFile? = null
        try {
            archive = openArchive(source, password)
            val entries = readEntries(
                archive = archive,
                passwordProvided = password != null,
                multiVolumeArchive = source.isMultiVolumeArchive,
            )
            return SevenZArchiveReader(
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

    override fun createWriter(
        session: IExplorerActionHostSession,
        cacheDirectory: File?,
    ): ArchiveWriter =
        SevenZArchiveCreator(session, cacheDirectory)

    override fun mutationAvailability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
        sevenZMutationAvailability(snapshot)

    override fun createMutationProvider(
        session: ExplorerActionHostSessionClient,
        cacheDirectory: File,
        engine: ArchiveEngine,
    ): ArchiveMutationProvider = SevenZArchiveMutationProvider(session, cacheDirectory, engine)

    private fun openArchive(source: ArchiveReadSource, password: CharArray?): SevenZFile {
        val channel = source.openSeekableChannel()
        val encodedPassword = password?.toSevenZPasswordBytes()
        return try {
            SevenZFile.builder()
                .setSeekableByteChannel(channel)
                .setDefaultName(source.displayName)
                .setMaxMemoryLimitKiB(DECODER_MEMORY_LIMIT_KIB)
                .apply { if (encodedPassword != null) setPassword(encodedPassword) }
                .get()
        } catch (error: Throwable) {
            runCatching { channel.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        } finally {
            encodedPassword?.fill(0)
        }
    }

    private fun CharArray.toSevenZPasswordBytes(): ByteArray = ByteArray(size * 2).also { bytes ->
        forEachIndexed { index, character ->
            val value = character.code
            bytes[index * 2] = value.toByte()
            bytes[index * 2 + 1] = (value ushr 8).toByte()
        }
    }

    private fun readEntries(
        archive: SevenZFile,
        passwordProvided: Boolean,
        multiVolumeArchive: Boolean,
    ): List<ArchiveReaderEntry> {
        val metadataEntries = archive.entries.toList()
        val methodEntries = metadataEntries.mapIndexed { ordinal, metadata ->
            val reached = try {
                archive.nextEntry
            } catch (error: Exception) {
                if (error.findCause<PasswordRequiredException>() != null) throw error
                return@mapIndexed SevenZDiscoveredEntry(
                    metadata,
                    ordinal,
                    SevenZMethodInfo.unavailable(error),
                )
            } catch (error: LinkageError) {
                return@mapIndexed SevenZDiscoveredEntry(
                    metadata,
                    ordinal,
                    SevenZMethodInfo.unavailable(error),
                )
            }
            if (reached == null) throw IOException("7Z stream map ended before its directory")
            if (reached !== metadata) throw IOException("7Z directory and stream order differ")
            val methodInfo = try {
                SevenZMethodInfo.from(reached)
            } catch (error: Exception) {
                SevenZMethodInfo.unavailable(error)
            } catch (error: LinkageError) {
                SevenZMethodInfo.unavailable(error)
            }
            SevenZDiscoveredEntry(metadata, ordinal, methodInfo)
        }
        val solidArchive = archive.hasSolidCompression()
        val encryptedArchive = passwordProvided || methodEntries.any { it.methodInfo.encrypted }
        return methodEntries.map { discovered ->
            discovered.metadata.toReaderEntry(
                ordinal = discovered.ordinal,
                methodInfo = discovered.methodInfo,
                passwordProvided = passwordProvided,
                solidArchive = solidArchive,
                encryptedArchive = encryptedArchive,
                multiVolumeArchive = multiVolumeArchive,
            )
        }
    }

    private fun SevenZArchiveEntry.toReaderEntry(
        ordinal: Int,
        methodInfo: SevenZMethodInfo,
        passwordProvided: Boolean,
        solidArchive: Boolean,
        encryptedArchive: Boolean,
        multiVolumeArchive: Boolean,
    ): ArchiveReaderEntry {
        val unsupportedType = isAntiItem
        val mutationCapable = !solidArchive &&
            !encryptedArchive &&
            !multiVolumeArchive &&
            !unsupportedType &&
            methodInfo.readable &&
            methodInfo.mutationDecoderWithinBudget
        val limitations = buildSet {
            if (isDirectory) add(ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA)
            if (methodInfo.encrypted) add(ArchiveEntryLimitation.ENCRYPTED)
            if (solidArchive) add(ArchiveEntryLimitation.SOLID_COMPRESSION)
            if (!methodInfo.mutationDecoderWithinBudget) {
                add(ArchiveEntryLimitation.MUTATION_RESOURCE_BUDGET_EXCEEDED)
            }
            if (!methodInfo.readable) add(ArchiveEntryLimitation.UNSUPPORTED_COMPRESSION_METHOD)
            if (unsupportedType) add(ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE)
            if (!mutationCapable) add(ArchiveEntryLimitation.MUTATION_UNAVAILABLE)
        }
        val readableFile = !isDirectory &&
            !unsupportedType &&
            methodInfo.readable &&
            (!methodInfo.encrypted || passwordProvided)
        return ArchiveReaderEntry(
            ordinal = ordinal,
            name = name ?: throw IOException("7Z entry has no name"),
            isDirectory = isDirectory,
            compressionMethod = methodInfo.compressionMethod,
            compressionMethodId = methodInfo.methodId,
            isEncrypted = methodInfo.encrypted,
            encryptionMethod = ArchiveEncryptionMethod.AES.takeIf { methodInfo.encrypted },
            capabilities = ArchiveEntryCapabilities(
                canOpen = readableFile,
                canExtract = isDirectory || readableFile,
                canDelete = mutationCapable,
                canRename = mutationCapable,
                limitations = limitations,
            ),
            compressedSize = if (hasStream()) UNKNOWN_COMPRESSED_SIZE else 0L,
            size = size,
            crc = crcValue.takeIf { getHasCrc() },
            time = if (getHasLastModifiedDate()) lastModifiedDate.time else null,
            backendToken = this,
        )
    }

    private fun mapOpenFailure(error: Throwable, passwordProvided: Boolean): ArchiveBackendException {
        error.findCause<CancellationException>()?.let { throw it }
        if (error is ArchiveBackendException) return error
        val failure = when {
            error.findCause<PasswordRequiredException>() != null ->
                ArchiveBackendFailure.PASSWORD_REQUIRED
            error.findCause<MemoryLimitException>() != null ->
                ArchiveBackendFailure.UNSUPPORTED_METHOD
            error is LinkageError -> ArchiveBackendFailure.UNSUPPORTED_METHOD
            passwordProvided && error.looksLikePasswordFailure() ->
                ArchiveBackendFailure.WRONG_PASSWORD
            else -> ArchiveBackendFailure.MALFORMED
        }
        return ArchiveBackendException(
            format = format,
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
                ArchiveBackendFailure.INVALID_SIGNATURE -> "7Z signature is not present"
                ArchiveBackendFailure.INVALID_OPTIONS -> "7Z reader options are invalid"
                ArchiveBackendFailure.UNSUPPORTED_METHOD ->
                    "7Z decoder memory requirement is not supported"
                ArchiveBackendFailure.MISSING_VOLUME -> "7Z volumes are missing or unavailable"
                ArchiveBackendFailure.PASSWORD_REQUIRED -> "7Z password is required"
                ArchiveBackendFailure.WRONG_PASSWORD -> "7Z password is incorrect"
                ArchiveBackendFailure.MALFORMED -> "7Z directory metadata cannot be read"
            },
            cause = error,
        )
    }

    private fun hasSignature(source: ArchiveReadSource): Boolean {
        val signature = ByteArray(SIGNATURE_SIZE)
        val length = source.openInputStream().use { input ->
            var total = 0
            while (total < signature.size) {
                val read = input.read(signature, total, signature.size - total)
                if (read < 0) break
                if (read == 0) continue
                total += read
            }
            total
        }
        return SevenZFile.matches(signature, length)
    }

    private const val DECODER_MEMORY_LIMIT_KIB = 256 * 1_024
    private const val SIGNATURE_SIZE = 6
    private const val UNKNOWN_COMPRESSED_SIZE = -1L
}

private class SevenZArchiveReader(
    private val archive: SevenZFile,
    override val entries: List<ArchiveReaderEntry>,
    override val options: ArchiveReaderOptions,
) : ArchiveReader {
    override val format = ArchiveFormat.SEVEN_Z
    override val formatCapabilities = SevenZArchiveBackend.capabilities

    override fun openEntry(entry: ArchiveReaderEntry): InputStream {
        val sevenZEntry = entry.backendToken as? SevenZArchiveEntry
            ?: throw IllegalArgumentException("Archive entry belongs to a different backend")
        require(entries.getOrNull(entry.ordinal) === entry) {
            "Archive entry does not belong to this reader"
        }
        if (entry.isDirectory) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNKNOWN_SELECTION,
                message = "7Z directory entries do not expose a data stream",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        if (entry.isEncrypted && !options.hasPassword) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.PASSWORD_REQUIRED,
                message = "7Z entry requires a password",
                format = format,
                stage = ArchiveFailureStage.PASSWORD,
            )
        }
        if (!entry.capabilities.canExtract) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNSUPPORTED_METHOD,
                message = "7Z entry data cannot be read by this backend",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        val input = try {
            archive.getInputStream(sevenZEntry)
        } catch (error: Throwable) {
            throw mapEntryFailure(error, entry.isEncrypted, options.hasPassword)
        }
        return SevenZEntryInputStream(
            delegate = input,
            encrypted = entry.isEncrypted,
            passwordProvided = options.hasPassword,
        )
    }

    override fun close() {
        try {
            archive.close()
        } finally {
            options.clearPassword()
        }
    }
}

private class SevenZEntryInputStream(
    delegate: InputStream,
    private val encrypted: Boolean,
    private val passwordProvided: Boolean,
) : FilterInputStream(delegate) {
    override fun read(): Int = mapRead { super.read() }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        mapRead { super.read(buffer, offset, length) }

    override fun skip(byteCount: Long): Long = mapRead { super.skip(byteCount) }

    override fun close() = mapRead { super.close() }

    private inline fun <T> mapRead(block: () -> T): T = try {
        block()
    } catch (error: Throwable) {
        throw mapEntryFailure(error, encrypted, passwordProvided)
    }
}

private data class SevenZMethodInfo(
    val compressionMethod: ArchiveCompressionMethod,
    val methodId: String,
    val encrypted: Boolean,
    val readable: Boolean,
    val mutationDecoderWithinBudget: Boolean,
) {
    companion object {
        fun from(entry: SevenZArchiveEntry): SevenZMethodInfo {
            if (!entry.hasStream()) {
                return SevenZMethodInfo(
                    compressionMethod = ArchiveCompressionMethod.STORED,
                    methodId = if (entry.isDirectory) "DIRECTORY" else "EMPTY_STREAM",
                    encrypted = false,
                    readable = true,
                    mutationDecoderWithinBudget = true,
                )
            }
            val configurations = entry.contentMethods?.toList().orEmpty()
            val methods = configurations.map(SevenZMethodConfiguration::getMethod)
            val compressionMethod = methods.firstNotNullOfOrNull { method ->
                when (method) {
                    SevenZMethod.COPY -> ArchiveCompressionMethod.STORED
                    SevenZMethod.DEFLATE -> ArchiveCompressionMethod.DEFLATED
                    SevenZMethod.DEFLATE64 -> ArchiveCompressionMethod.DEFLATE64
                    SevenZMethod.LZMA -> ArchiveCompressionMethod.LZMA
                    SevenZMethod.LZMA2 -> ArchiveCompressionMethod.LZMA2
                    SevenZMethod.BZIP2 -> ArchiveCompressionMethod.BZIP2
                    else -> null
                }
            } ?: ArchiveCompressionMethod.OTHER
            return SevenZMethodInfo(
                compressionMethod = compressionMethod,
                methodId = methods.joinToString(METHOD_SEPARATOR, transform = SevenZMethod::name)
                    .ifEmpty { "UNKNOWN" },
                encrypted = SevenZMethod.AES256SHA256 in methods,
                readable = true,
                mutationDecoderWithinBudget = configurations.all { configuration ->
                    configuration.mutationDecoderMemoryUsageKiB()?.let { memoryUsageKiB ->
                        memoryUsageKiB <= SEVEN_Z_MUTATION_MAX_DECODER_MEMORY_KIB
                    } == true
                },
            )
        }

        fun unavailable(error: Throwable): SevenZMethodInfo = SevenZMethodInfo(
            compressionMethod = ArchiveCompressionMethod.OTHER,
            methodId = error.findMethodFailureName(),
            encrypted = false,
            readable = false,
            mutationDecoderWithinBudget = false,
        )

        private const val METHOD_SEPARATOR = "+"
    }
}

private data class SevenZDiscoveredEntry(
    val metadata: SevenZArchiveEntry,
    val ordinal: Int,
    val methodInfo: SevenZMethodInfo,
)

private fun SevenZMethodConfiguration.mutationDecoderMemoryUsageKiB(): Int? {
    return try {
        when (method) {
            SevenZMethod.LZMA2 -> {
                val dictionarySize = (options as? Number)?.toLong() ?: return null
                if (dictionarySize !in 1L..Int.MAX_VALUE.toLong()) return null
                LZMA2InputStream.getMemoryUsage(dictionarySize.toInt())
            }
            SevenZMethod.LZMA -> {
                val lzmaOptions = options as? LZMA2Options ?: return null
                val properties =
                    ((lzmaOptions.pb * 5 + lzmaOptions.lp) * 9 + lzmaOptions.lc).toByte()
                LZMAInputStream.getMemoryUsage(lzmaOptions.dictSize, properties)
            }
            else -> 0
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun mapEntryFailure(
    error: Throwable,
    encrypted: Boolean,
    passwordProvided: Boolean,
): IOException {
    error.findCause<CancellationException>()?.let { throw it }
    if (error is ArchiveException) return error
    if (error.findCause<PasswordRequiredException>() != null) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.PASSWORD_REQUIRED,
            message = "7Z entry requires a password",
            cause = error,
            format = ArchiveFormat.SEVEN_Z,
            stage = ArchiveFailureStage.PASSWORD,
        )
    }
    if (error.findCause<MemoryLimitException>() != null) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.UNSUPPORTED_METHOD,
            message = "7Z decoder memory limit exceeded",
            cause = error,
            format = ArchiveFormat.SEVEN_Z,
            stage = ArchiveFailureStage.ENTRY_DATA,
        )
    }
    if (encrypted && passwordProvided && error.looksLikePasswordFailure()) {
        return ArchiveExtractionException(
            code = ArchiveFailureCode.WRONG_PASSWORD,
            message = "7Z password is incorrect or encrypted data is damaged",
            cause = error,
            format = ArchiveFormat.SEVEN_Z,
            stage = ArchiveFailureStage.PASSWORD,
        )
    }
    return IOException("7Z entry data cannot be read", error)
}

private inline fun <reified T : Throwable> Throwable.findCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

private fun Throwable.looksLikePasswordFailure(): Boolean =
    generateSequence(this) { it.cause }
        .any { cause ->
            cause.javaClass.simpleName == "CorruptedInputException" ||
                cause.message.orEmpty().contains("password", ignoreCase = true) ||
                cause.message.orEmpty().contains("checksum", ignoreCase = true) ||
                cause.message.orEmpty().contains("crc", ignoreCase = true)
        }

private fun Throwable.findMethodFailureName(): String {
    val type = generateSequence(this) { it.cause }.last().javaClass.simpleName
        .takeIf(String::isNotBlank)
        ?: "UNSUPPORTED"
    return "UNSUPPORTED:$type"
}
