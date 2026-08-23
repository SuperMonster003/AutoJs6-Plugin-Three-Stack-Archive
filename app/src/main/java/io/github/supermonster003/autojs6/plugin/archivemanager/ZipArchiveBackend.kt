package io.github.supermonster003.autojs6.plugin.archivemanager

import net.lingala.zip4j.exception.ZipException as Zip4jException
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import java.util.zip.ZipException

internal object ZipArchiveBackend : ArchiveBackend {
    override val format = ArchiveFormat.ZIP

    override val capabilities = FormatCapabilities(
        canDetect = true,
        canList = true,
        canPreview = true,
        canOpen = true,
        canExtract = true,
        canCreate = true,
        canAdd = false,
        canDelete = false,
        canRename = false,
        password = ArchiveOptionMode.OPTIONAL,
        filenameEncryption = ArchiveOptionMode.UNSUPPORTED,
        splitVolumes = ArchiveOptionMode.UNSUPPORTED,
        compressionLevels = (0..9).toList(),
        filenameCharsetNames = ZipArchiveAccess.supportedFilenameCharsetNames(),
        limitations = setOf(
            ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT,
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE,
            ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE,
            ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE,
        ),
    )

    override fun openReader(source: File, options: ArchiveReaderOptions): ArchiveReader {
        val charsetName = options.filenameCharsetName ?: try {
            ZipArchiveAccess.detectCharset(source).name()
        } catch (error: Exception) {
            throw mapOpenFailure(source, error)
        }
        val password = options.passwordChars()
        val archive = try {
            ZipArchiveAccess.open(source, charsetName, password)
        } catch (error: Exception) {
            throw mapOpenFailure(source, error)
        } finally {
            password?.fill('\u0000')
        }
        val resolvedOptions = options.resolved(charsetName)
        return try {
            ZipArchiveReader(
                archive = archive,
                options = resolvedOptions,
            )
        } catch (error: Exception) {
            resolvedOptions.clearPassword()
            runCatching { archive.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw mapOpenFailure(source, error)
        }
    }

    override fun createWriter(session: IExplorerActionHostSession): ArchiveWriter =
        ZipArchiveCreator(session)

    private fun mapOpenFailure(source: File, error: Throwable): ArchiveBackendException {
        if (error is ArchiveBackendException) return error
        val invalidOptions = error is UnsupportedCharsetException || error is IllegalCharsetNameException
        val zip4jError = generateSequence(error) { it.cause }
            .filterIsInstance<Zip4jException>()
            .firstOrNull()
        val wrongPassword = zip4jError?.type == Zip4jException.Type.WRONG_PASSWORD
        val unsupportedMethod = zip4jError?.type in setOf(
            Zip4jException.Type.UNKNOWN_COMPRESSION_METHOD,
            Zip4jException.Type.UNSUPPORTED_ENCRYPTION,
        ) || error is ZipException &&
            error.message.orEmpty().contains("compression method", ignoreCase = true)
        val invalidSignature = !invalidOptions && !ZipArchiveAccess.hasZipSignature(source)
        val failure = when {
            invalidOptions -> ArchiveBackendFailure.INVALID_OPTIONS
            invalidSignature -> ArchiveBackendFailure.INVALID_SIGNATURE
            wrongPassword -> ArchiveBackendFailure.WRONG_PASSWORD
            unsupportedMethod -> ArchiveBackendFailure.UNSUPPORTED_METHOD
            else -> ArchiveBackendFailure.MALFORMED
        }
        return ArchiveBackendException(
            format = format,
            failure = failure,
            stage = when (failure) {
                ArchiveBackendFailure.INVALID_SIGNATURE -> ArchiveFailureStage.FORMAT_DETECTION
                ArchiveBackendFailure.PASSWORD_REQUIRED,
                ArchiveBackendFailure.WRONG_PASSWORD -> ArchiveFailureStage.PASSWORD
                ArchiveBackendFailure.INVALID_OPTIONS,
                ArchiveBackendFailure.MALFORMED,
                ArchiveBackendFailure.UNSUPPORTED_METHOD,
                -> ArchiveFailureStage.INDEX
            },
            message = when (failure) {
                ArchiveBackendFailure.INVALID_SIGNATURE -> "ZIP signature is not present"
                ArchiveBackendFailure.INVALID_OPTIONS -> "ZIP filename encoding is not supported"
                ArchiveBackendFailure.WRONG_PASSWORD -> "ZIP password is incorrect"
                ArchiveBackendFailure.UNSUPPORTED_METHOD ->
                    "ZIP contains an unsupported compression method"
                ArchiveBackendFailure.PASSWORD_REQUIRED -> "ZIP password is required"
                ArchiveBackendFailure.MALFORMED -> "ZIP directory metadata cannot be read"
            },
            cause = error,
        )
    }
}

private class ZipArchiveReader(
    private val archive: OpenZipArchive,
    override val options: ArchiveReaderOptions,
) : ArchiveReader {
    override val format = ArchiveFormat.ZIP
    override val formatCapabilities = ZipArchiveBackend.capabilities

    override val entries: List<ArchiveReaderEntry> = archive.entries.mapIndexed { ordinal, entry ->
        val canReadData = entry.canExtract && (!entry.isEncrypted || options.hasPassword)
        val limitations = buildSet {
            if (entry.isDirectory) add(ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA)
            if (entry.isEncrypted) add(ArchiveEntryLimitation.ENCRYPTED)
            if (!entry.isDirectory && !entry.canExtract) {
                add(ArchiveEntryLimitation.UNSUPPORTED_COMPRESSION_METHOD)
            }
            add(ArchiveEntryLimitation.MUTATION_UNAVAILABLE)
        }
        ArchiveReaderEntry(
            ordinal = ordinal,
            name = entry.name,
            isDirectory = entry.isDirectory,
            compressionMethod = when (entry.method) {
                0 -> ArchiveCompressionMethod.STORED
                8 -> ArchiveCompressionMethod.DEFLATED
                else -> ArchiveCompressionMethod.OTHER
            },
            compressionMethodId = entry.method.toString(),
            isEncrypted = entry.isEncrypted,
            encryptionMethod = entry.encryptionMethod,
            capabilities = ArchiveEntryCapabilities(
                canOpen = !entry.isDirectory && canReadData,
                canExtract = entry.isDirectory || canReadData,
                canDelete = false,
                canRename = false,
                limitations = limitations,
            ),
            compressedSize = entry.compressedSize,
            size = entry.size,
            crc = entry.crc,
            time = entry.time.takeIf { it >= 0L },
            backendToken = entry,
        )
    }

    override fun openEntry(entry: ArchiveReaderEntry): InputStream {
        val zipEntry = entry.backendToken as? ZipEntryMetadata
            ?: throw IllegalArgumentException("Archive entry belongs to a different backend")
        val expected = entries.getOrNull(entry.ordinal)
        require(expected === entry) { "Archive entry does not belong to this reader" }
        if (entry.isEncrypted && !options.hasPassword) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.PASSWORD_REQUIRED,
                message = "ZIP entry requires a password",
                format = format,
                stage = ArchiveFailureStage.PASSWORD,
            )
        }
        if (!entry.capabilities.canExtract) {
            throw ArchiveExtractionException(
                code = ArchiveFailureCode.UNSUPPORTED_METHOD,
                message = "ZIP entry data cannot be read",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        return try {
            archive.getInputStream(zipEntry)
        } catch (error: IOException) {
            throw error
        } catch (error: RuntimeException) {
            throw IOException("ZIP entry data cannot be opened", error)
        }
    }

    override fun close() {
        try {
            archive.close()
        } finally {
            options.clearPassword()
        }
    }
}
