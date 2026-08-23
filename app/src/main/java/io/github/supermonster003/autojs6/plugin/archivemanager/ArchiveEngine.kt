package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale

enum class ArchiveFormat(
    val id: String,
    val displayName: String,
    val primaryExtension: String,
    val primaryMimeType: String,
    val extensions: Set<String>,
    val mimeTypes: Set<String>,
) {
    ZIP(
        id = "zip",
        displayName = "ZIP",
        primaryExtension = "zip",
        primaryMimeType = "application/zip",
        extensions = setOf("zip", "jar", "aar", "war"),
        mimeTypes = setOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/java-archive",
        ),
    ),
    TAR(
        id = "tar",
        displayName = "TAR",
        primaryExtension = "tar",
        primaryMimeType = "application/x-tar",
        extensions = setOf("tar"),
        mimeTypes = setOf(
            "application/x-tar",
            "application/tar",
        ),
    ),
    TAR_GZIP(
        id = "tar-gzip",
        displayName = "TAR.GZ",
        primaryExtension = "tar.gz",
        primaryMimeType = "application/x-compressed-tar",
        extensions = setOf("tar.gz", "tgz"),
        mimeTypes = setOf("application/x-compressed-tar"),
    ),
    TAR_XZ(
        id = "tar-xz",
        displayName = "TAR.XZ",
        primaryExtension = "tar.xz",
        primaryMimeType = "application/x-xz-compressed-tar",
        extensions = setOf("tar.xz", "txz"),
        mimeTypes = setOf("application/x-xz-compressed-tar"),
    ),
    TAR_BZIP2(
        id = "tar-bzip2",
        displayName = "TAR.BZ2",
        primaryExtension = "tar.bz2",
        primaryMimeType = "application/x-bzip2-compressed-tar",
        extensions = setOf("tar.bz2", "tbz2"),
        mimeTypes = setOf(
            "application/x-bzip-compressed-tar",
            "application/x-bzip2-compressed-tar",
        ),
    ),
    TAR_ZSTD(
        id = "tar-zstd",
        displayName = "TAR.ZST",
        primaryExtension = "tar.zst",
        primaryMimeType = "application/x-zstd-compressed-tar",
        extensions = setOf("tar.zst", "tzst"),
        mimeTypes = setOf("application/x-zstd-compressed-tar"),
    ),
    ;

    /** Leaf extensions accepted by the host catalog protocol. */
    val catalogExtensions: Set<String>
        get() = extensions.mapTo(linkedSetOf()) { it.substringAfterLast('.') }

    val isTarFamily: Boolean
        get() = this == TAR ||
            this == TAR_GZIP ||
            this == TAR_XZ ||
            this == TAR_BZIP2 ||
            this == TAR_ZSTD

    fun matchesFileName(displayName: String): Boolean {
        val normalized = displayName.lowercase(Locale.ROOT)
        return extensions.any { extension -> normalized.endsWith(".$extension") }
    }

    internal fun baseNameWithoutArchiveExtension(displayName: String): String? {
        val normalized = displayName.lowercase(Locale.ROOT)
        val extension = extensions
            .filter { normalized.endsWith(".$it") }
            .maxByOrNull(String::length)
            ?: return null
        return displayName.dropLast(extension.length + 1)
    }

    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(primaryExtension in extensions)
        require(primaryMimeType in mimeTypes)
        require(
            extensions.isNotEmpty() &&
                extensions.all { it.isNotBlank() && it == it.lowercase(Locale.ROOT) },
        )
        require(
            mimeTypes.isNotEmpty() &&
                mimeTypes.all { '/' in it && it == it.lowercase(Locale.ROOT) },
        )
    }
}

enum class ArchiveOperation {
    DETECT,
    LIST,
    PREVIEW,
    OPEN,
    EXTRACT,
    CREATE,
    ADD,
    DELETE,
    RENAME,
}

enum class ArchiveOptionMode {
    UNSUPPORTED,
    OPTIONAL,
    REQUIRED,
}

enum class ArchiveFormatLimitation {
    ENTRY_METHOD_DEPENDENT,
    PASSWORD_UNAVAILABLE,
    FILENAME_ENCRYPTION_UNAVAILABLE,
    SPLIT_VOLUMES_UNAVAILABLE,
    MUTATION_REQUIRES_REWRITE,
}

data class FormatCapabilities(
    val canDetect: Boolean,
    val canList: Boolean,
    val canPreview: Boolean,
    val canOpen: Boolean,
    val canExtract: Boolean,
    val canCreate: Boolean,
    val canAdd: Boolean,
    val canDelete: Boolean,
    val canRename: Boolean,
    val password: ArchiveOptionMode,
    val filenameEncryption: ArchiveOptionMode,
    val splitVolumes: ArchiveOptionMode,
    val compressionLevels: List<Int>,
    /** Supported manual filename-decoding overrides. An empty list means no override UI. */
    val filenameCharsetNames: List<String> = emptyList(),
    val limitations: Set<ArchiveFormatLimitation>,
) {
    init {
        require(!canPreview || canList)
        require(!canOpen || canList)
        require(!canExtract || canList)
        require(compressionLevels.all { it >= 0 })
        require(compressionLevels.distinct().size == compressionLevels.size)
        require(filenameCharsetNames.none(String::isBlank))
        require(
            filenameCharsetNames.distinctBy { it.uppercase(Locale.ROOT) }.size ==
                filenameCharsetNames.size,
        )
        require(canCreate || compressionLevels.isEmpty())
    }

    fun supports(operation: ArchiveOperation): Boolean = when (operation) {
        ArchiveOperation.DETECT -> canDetect
        ArchiveOperation.LIST -> canList
        ArchiveOperation.PREVIEW -> canPreview
        ArchiveOperation.OPEN -> canOpen
        ArchiveOperation.EXTRACT -> canExtract
        ArchiveOperation.CREATE -> canCreate
        ArchiveOperation.ADD -> canAdd
        ArchiveOperation.DELETE -> canDelete
        ArchiveOperation.RENAME -> canRename
    }
}

enum class ArchiveEntryLimitation {
    DIRECTORY_HAS_NO_DATA,
    ENCRYPTED,
    UNSUPPORTED_COMPRESSION_METHOD,
    UNSUPPORTED_ENTRY_TYPE,
    MUTATION_UNAVAILABLE,
}

data class ArchiveEntryCapabilities(
    val canOpen: Boolean,
    val canExtract: Boolean,
    val canDelete: Boolean,
    val canRename: Boolean,
    val limitations: Set<ArchiveEntryLimitation>,
) {
    companion object {
        @JvmField
        val READABLE_FILE = ArchiveEntryCapabilities(
            canOpen = true,
            canExtract = true,
            canDelete = false,
            canRename = false,
            limitations = setOf(ArchiveEntryLimitation.MUTATION_UNAVAILABLE),
        )

        @JvmField
        val DIRECTORY = ArchiveEntryCapabilities(
            canOpen = false,
            canExtract = true,
            canDelete = false,
            canRename = false,
            limitations = setOf(
                ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA,
                ArchiveEntryLimitation.MUTATION_UNAVAILABLE,
            ),
        )
    }
}

class ArchiveReaderOptions(
    /** Filename charset selected for a legacy archive whose entry names do not declare Unicode. */
    val filenameCharsetName: String? = null,
    /** Transient password for this open archive. It is deliberately omitted from [toString]. */
    password: CharArray? = null,
) {
    @Volatile
    private var passwordValue = password?.takeIf(CharArray::isNotEmpty)?.clone()

    val hasPassword: Boolean
        get() = passwordValue != null

    @Synchronized
    internal fun passwordChars(): CharArray? = passwordValue?.clone()

    @Synchronized
    internal fun resolved(filenameCharsetName: String): ArchiveReaderOptions =
        ArchiveReaderOptions(filenameCharsetName, passwordValue)

    @Synchronized
    internal fun retainedCopy(): ArchiveReaderOptions =
        ArchiveReaderOptions(filenameCharsetName, passwordValue)

    @Synchronized
    internal fun clearPassword() {
        passwordValue?.fill('\u0000')
        passwordValue = null
    }

    override fun toString(): String =
        "ArchiveReaderOptions(filenameCharsetName=$filenameCharsetName, hasPassword=$hasPassword)"
}

internal data class ArchiveReaderEntry(
    val ordinal: Int,
    val name: String,
    val isDirectory: Boolean,
    val compressionMethod: ArchiveCompressionMethod,
    /** Backend-defined stable method identifier. It is interpreted together with the archive format. */
    val compressionMethodId: String,
    val isEncrypted: Boolean,
    val encryptionMethod: ArchiveEncryptionMethod?,
    val capabilities: ArchiveEntryCapabilities,
    val compressedSize: Long,
    val size: Long,
    val crc: Long?,
    val time: Long?,
    internal val backendToken: Any,
)

internal interface ArchiveReader : Closeable {
    val format: ArchiveFormat
    val formatCapabilities: FormatCapabilities
    val options: ArchiveReaderOptions
    val entries: List<ArchiveReaderEntry>

    fun entryAt(ordinal: Int): ArchiveReaderEntry? = entries.getOrNull(ordinal)

    fun openEntry(entry: ArchiveReaderEntry): InputStream
}

internal interface ArchiveWriter {
    val format: ArchiveFormat
    val formatCapabilities: FormatCapabilities

    fun create(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): ArchiveCreationResult
}

internal enum class ArchiveBackendFailure {
    INVALID_SIGNATURE,
    MALFORMED,
    INVALID_OPTIONS,
    UNSUPPORTED_METHOD,
    WRONG_PASSWORD,
}

internal class ArchiveBackendException(
    val format: ArchiveFormat?,
    val failure: ArchiveBackendFailure,
    val stage: ArchiveFailureStage,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

internal interface ArchiveBackend {
    val format: ArchiveFormat
    val capabilities: FormatCapabilities

    fun openReader(source: File, options: ArchiveReaderOptions): ArchiveReader

    fun createWriter(session: IExplorerActionHostSession): ArchiveWriter? = null
}

data class DetectedArchiveFormat(
    val format: ArchiveFormat,
    val capabilities: FormatCapabilities,
    /** Structural detection means that the backend successfully opened the directory metadata. */
    val structurallyVerified: Boolean,
)

internal class ArchiveEngine private constructor(
    private val backends: List<ArchiveBackend>,
) {
    init {
        require(backends.isNotEmpty())
        require(backends.map(ArchiveBackend::format).distinct().size == backends.size)
    }

    val formats: List<ArchiveFormat> = backends.map(ArchiveBackend::format)

    val readableFormats: List<ArchiveFormat> = backends
        .filter { it.capabilities.canList }
        .map(ArchiveBackend::format)

    val creatableFormats: List<ArchiveFormat> = backends
        .filter { it.capabilities.canCreate }
        .map(ArchiveBackend::format)

    fun capabilities(format: ArchiveFormat): FormatCapabilities = backend(format).capabilities

    fun probe(source: File): DetectedArchiveFormat {
        if (!source.isFile) {
            throw ArchiveValidationException(
                ArchiveFailureCode.SOURCE_NOT_FILE,
                "Archive source is not a regular file",
            )
        }
        try {
            openReader(source).use { reader ->
                return DetectedArchiveFormat(
                    format = reader.format,
                    capabilities = reader.formatCapabilities,
                    structurallyVerified = true,
                )
            }
        } catch (error: ArchiveBackendException) {
            throw ArchiveValidationException(
                code = when (error.failure) {
                    ArchiveBackendFailure.INVALID_SIGNATURE -> ArchiveFailureCode.INVALID_SIGNATURE
                    ArchiveBackendFailure.INVALID_OPTIONS ->
                        ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET
                    ArchiveBackendFailure.UNSUPPORTED_METHOD -> ArchiveFailureCode.UNSUPPORTED_METHOD
                    ArchiveBackendFailure.WRONG_PASSWORD -> ArchiveFailureCode.WRONG_PASSWORD
                    ArchiveBackendFailure.MALFORMED -> ArchiveFailureCode.MALFORMED_ARCHIVE
                },
                message = error.message ?: "Archive format probe failed",
                cause = error,
                format = error.format,
                stage = error.stage,
            )
        } catch (error: ArchiveException) {
            throw error
        } catch (error: Exception) {
            throw ArchiveValidationException(
                ArchiveFailureCode.INVALID_SIGNATURE,
                "Archive format is not recognized by an installed backend",
                error,
            )
        }
    }

    fun openReader(
        source: File,
        format: ArchiveFormat? = null,
        options: ArchiveReaderOptions = ArchiveReaderOptions(),
    ): ArchiveReader {
        if (format != null) return backend(format).openReader(source, options)
        if (backends.size == 1) return backends.single().openReader(source, options)

        var formatFailure: ArchiveBackendException? = null
        var unexpectedFailure: Throwable? = null
        backends.forEach { candidate ->
            try {
                return candidate.openReader(source, options)
            } catch (error: ArchiveBackendException) {
                if (
                    error.failure != ArchiveBackendFailure.INVALID_SIGNATURE &&
                    formatFailure == null
                ) {
                    formatFailure = error
                }
            } catch (error: IOException) {
                if (unexpectedFailure == null) unexpectedFailure = error
            } catch (error: IllegalArgumentException) {
                if (unexpectedFailure == null) unexpectedFailure = error
            }
        }
        formatFailure?.let { throw it }
        unexpectedFailure?.let { error ->
            throw ArchiveBackendException(
                format = null,
                failure = ArchiveBackendFailure.MALFORMED,
                stage = ArchiveFailureStage.FORMAT_DETECTION,
                message = "Archive format detection failed",
                cause = error,
            )
        }
        throw ArchiveBackendException(
            format = null,
            failure = ArchiveBackendFailure.INVALID_SIGNATURE,
            stage = ArchiveFailureStage.FORMAT_DETECTION,
            message = "Archive format is not recognized by an installed backend",
        )
    }

    fun createWriter(
        format: ArchiveFormat,
        session: IExplorerActionHostSession,
    ): ArchiveWriter {
        val backend = backend(format)
        check(backend.capabilities.canCreate) { "${format.displayName} creation is not supported" }
        return backend.createWriter(session)
            ?: error("${format.displayName} backend does not provide an archive writer")
    }

    private fun backend(format: ArchiveFormat): ArchiveBackend = backends.firstOrNull {
        it.format == format
    } ?: throw IllegalArgumentException("No backend is registered for ${format.displayName}")

    companion object {
        @JvmField
        val DEFAULT = ArchiveEngine(
            listOf(
                ZipArchiveBackend,
                TarArchiveBackend,
                TarGzipArchiveBackend,
                TarXzArchiveBackend,
                TarBzip2ArchiveBackend,
                TarZstdArchiveBackend,
            ),
        )
    }
}
