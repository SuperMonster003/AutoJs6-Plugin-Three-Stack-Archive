package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.io.File
import java.io.IOException
import java.io.InputStream
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
        password = ArchiveOptionMode.UNSUPPORTED,
        filenameEncryption = ArchiveOptionMode.UNSUPPORTED,
        splitVolumes = ArchiveOptionMode.UNSUPPORTED,
        compressionLevels = (0..9).toList(),
        limitations = setOf(
            ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT,
            ArchiveFormatLimitation.PASSWORD_UNAVAILABLE,
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE,
            ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE,
            ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE,
        ),
    )

    override fun openReader(source: File, options: ArchiveReaderOptions): ArchiveReader {
        val charsetName = options.filenameCharsetName ?: try {
            ZipArchiveAccess.detectCharset(source).name()
        } catch (error: Exception) {
            throw mapOpenFailure(error)
        }
        val archive = try {
            ZipArchiveAccess.open(source, charsetName)
        } catch (error: Exception) {
            throw mapOpenFailure(error)
        }
        return try {
            ZipArchiveReader(
                archive = archive,
                options = ArchiveReaderOptions(filenameCharsetName = charsetName),
            )
        } catch (error: Exception) {
            runCatching { archive.close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw mapOpenFailure(error)
        }
    }

    override fun createWriter(session: IExplorerActionHostSession): ArchiveWriter =
        ZipArchiveCreator(session)

    private fun mapOpenFailure(error: Throwable): ArchiveBackendException {
        if (error is ArchiveBackendException) return error
        val unsupportedMethod = error is ZipException &&
            error.message.orEmpty().contains("compression method", ignoreCase = true)
        return ArchiveBackendException(
            failure = if (unsupportedMethod) {
                ArchiveBackendFailure.UNSUPPORTED_METHOD
            } else {
                ArchiveBackendFailure.MALFORMED
            },
            message = if (unsupportedMethod) {
                "ZIP contains an unsupported compression method"
            } else {
                "ZIP directory metadata cannot be read"
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
        val canReadData = entry.canExtract && !entry.isEncrypted
        val limitations = buildSet {
            if (entry.isDirectory) add(ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA)
            if (entry.isEncrypted) add(ArchiveEntryLimitation.ENCRYPTED)
            if (!entry.isDirectory && !entry.isEncrypted && !entry.canExtract) {
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
            capabilities = ArchiveEntryCapabilities(
                canOpen = !entry.isDirectory && canReadData,
                canExtract = entry.isDirectory || canReadData,
                canDelete = false,
                canRename = false,
                limitations = limitations,
            ),
            compressedSize = entry.compressedSize,
            size = entry.size,
            crc = entry.crc.takeIf { it >= 0L },
            time = entry.time.takeIf { it >= 0L },
            backendToken = entry,
        )
    }

    override fun openEntry(entry: ArchiveReaderEntry): InputStream {
        val zipEntry = entry.backendToken as? ZipEntryMetadata
            ?: throw IllegalArgumentException("Archive entry belongs to a different backend")
        val expected = entries.getOrNull(entry.ordinal)
        require(expected === entry) { "Archive entry does not belong to this reader" }
        require(entry.capabilities.canExtract) { "Archive entry data cannot be read" }
        return try {
            archive.getInputStream(zipEntry)
        } catch (error: IOException) {
            throw error
        } catch (error: RuntimeException) {
            throw IOException("ZIP entry data cannot be opened", error)
        }
    }

    override fun close() = archive.close()
}
