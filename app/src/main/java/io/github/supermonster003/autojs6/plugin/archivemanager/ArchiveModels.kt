package io.github.supermonster003.autojs6.plugin.archivemanager

import java.io.IOException

data class ArchiveSecurityLimits(
    val maxEntries: Int = 100_000,
    val maxPathLength: Int = 16_384,
    val maxDepth: Int = 256,
    val maxSingleUncompressedBytes: Long = Long.MAX_VALUE,
    val maxTotalUncompressedBytes: Long = Long.MAX_VALUE,
    val maxCompressionRatio: Long = Long.MAX_VALUE,
) {
    init {
        require(maxEntries > 0)
        require(maxPathLength > 0)
        require(maxDepth > 0)
        require(maxSingleUncompressedBytes >= 0L)
        require(maxTotalUncompressedBytes >= maxSingleUncompressedBytes)
        require(maxCompressionRatio > 0L)
    }

    companion object {
        @JvmField
        val DEFAULT = ArchiveSecurityLimits()
    }
}

enum class ArchiveCompressionMethod {
    STORED,
    DEFLATED,
    OTHER,
}

enum class ArchiveEncryptionMethod {
    ZIP_CRYPTO,
    AES,
    OTHER,
}

data class ArchiveEntry(
    /** Portable separator-normalized path without a trailing slash; Unicode spelling is preserved. */
    val path: String,
    /** Exact backend directory name used to reopen this entry. */
    val sourceName: String,
    val displayName: String,
    val isDirectory: Boolean,
    val compressionMethod: ArchiveCompressionMethod,
    /** Backend-defined method identifier interpreted together with [ArchiveSnapshot.format]. */
    val compressionMethodId: String = compressionMethod.name,
    val isEncrypted: Boolean = false,
    val encryptionMethod: ArchiveEncryptionMethod? = null,
    val capabilities: ArchiveEntryCapabilities = if (isDirectory) {
        ArchiveEntryCapabilities.DIRECTORY
    } else {
        ArchiveEntryCapabilities.READABLE_FILE
    },
    val compressedSize: Long,
    /** Uncompressed size declared by the archive directory and verified while extracting. */
    val uncompressedSize: Long,
    val crc32: Long?,
    val modifiedTimeMillis: Long?,
    val ordinal: Int,
) {
    val canOpen: Boolean
        get() = capabilities.canOpen

    val canExtract: Boolean
        get() = capabilities.canExtract
}

data class ArchiveSnapshot(
    val sourceLength: Long,
    val sourceLastModifiedMillis: Long,
    val entries: List<ArchiveEntry>,
    val totalUncompressedBytes: Long,
    val limits: ArchiveSecurityLimits,
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val readerOptions: ArchiveReaderOptions = ArchiveReaderOptions(),
)

data class ArchiveNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    /** Null for an implicit directory that has no explicit archive entry. */
    val entry: ArchiveEntry?,
    val childCount: Int,
    val descendantFileCount: Int,
    val uncompressedSize: Long,
)

data class ResolvedArchiveSelection(
    val requestedPaths: Set<String>,
    /** Directories relative to the newly created extraction root, parents first. */
    val directories: List<String>,
    val files: List<ArchiveEntry>,
) {
    val totalUncompressedBytes: Long = files.sumOf(ArchiveEntry::uncompressedSize)
    val totalEntries: Int = directories.size + files.size
}

enum class ExtractionPhase {
    PREPARING,
    EXTRACTING,
    CLEANING_UP,
    COMPLETED,
}

data class ExtractionProgress(
    val phase: ExtractionPhase,
    val currentPath: String?,
    val completedEntries: Int,
    val totalEntries: Int,
    val bytesWritten: Long,
    val totalBytes: Long,
)

fun interface ArchiveProgressListener {
    fun onProgress(progress: ExtractionProgress)

    companion object {
        @JvmField
        val NONE = ArchiveProgressListener { }
    }
}

data class ArchiveOutputLocation(
    /** A writer-defined stable identifier. SAF writers use the document URI string. */
    val identifier: String,
    val displayName: String,
)

data class ExtractionResult(
    val root: ArchiveOutputLocation,
    val filesExtracted: Int,
    val directoriesCreated: Int,
    val bytesWritten: Long,
)

internal data class ArchiveCreationOptions(
    val outputDisplayName: String,
    val compressionLevel: Int,
    val password: CharArray? = null,
)

internal data class ArchiveCreationProgress(
    val currentEntry: String,
    val completedFiles: Long,
    val completedDirectories: Long,
    val sourceBytesRead: Long,
)

internal data class ArchiveCreationResult(
    val outputDisplayName: String,
    val outputDisplayPath: String,
    val filesCompressed: Long,
    val directoriesAdded: Long,
    val sourceBytesRead: Long,
)

internal fun interface ArchiveCreationProgressListener {
    fun onProgress(progress: ArchiveCreationProgress)
}

enum class ArchiveFailureCode {
    SOURCE_NOT_FILE,
    SOURCE_CHANGED,
    CACHE_SPACE_UNAVAILABLE,
    INVALID_SIGNATURE,
    MALFORMED_ARCHIVE,
    UNSUPPORTED_FILENAME_CHARSET,
    UNSUPPORTED_METHOD,
    PASSWORD_REQUIRED,
    WRONG_PASSWORD,
    ENTRY_LIMIT_EXCEEDED,
    INVALID_PATH,
    PATH_LIMIT_EXCEEDED,
    DEPTH_LIMIT_EXCEEDED,
    DUPLICATE_PATH,
    FILE_DIRECTORY_CONFLICT,
    SINGLE_SIZE_LIMIT_EXCEEDED,
    TOTAL_SIZE_LIMIT_EXCEEDED,
    COMPRESSION_RATIO_LIMIT_EXCEEDED,
    SIZE_MISMATCH,
    CRC_MISMATCH,
    EMPTY_SELECTION,
    UNKNOWN_SELECTION,
    INVALID_DESTINATION_NAME,
    OUTPUT_FAILURE,
}

enum class ArchiveFailureStage {
    INPUT,
    FORMAT_DETECTION,
    INDEX,
    PASSWORD,
    ENTRY_DATA,
    OUTPUT,
    CLEANUP,
}

internal val ArchiveFailureCode.defaultStage: ArchiveFailureStage
    get() = when (this) {
        ArchiveFailureCode.SOURCE_NOT_FILE,
        ArchiveFailureCode.SOURCE_CHANGED,
        ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE,
        -> ArchiveFailureStage.INPUT

        ArchiveFailureCode.INVALID_SIGNATURE -> ArchiveFailureStage.FORMAT_DETECTION

        ArchiveFailureCode.MALFORMED_ARCHIVE,
        ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET,
        ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
        ArchiveFailureCode.INVALID_PATH,
        ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
        ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
        ArchiveFailureCode.DUPLICATE_PATH,
        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
        -> ArchiveFailureStage.INDEX

        ArchiveFailureCode.PASSWORD_REQUIRED,
        ArchiveFailureCode.WRONG_PASSWORD,
        -> ArchiveFailureStage.PASSWORD

        ArchiveFailureCode.UNSUPPORTED_METHOD,
        ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
        ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
        ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
        ArchiveFailureCode.SIZE_MISMATCH,
        ArchiveFailureCode.CRC_MISMATCH,
        ArchiveFailureCode.EMPTY_SELECTION,
        ArchiveFailureCode.UNKNOWN_SELECTION,
        -> ArchiveFailureStage.ENTRY_DATA

        ArchiveFailureCode.INVALID_DESTINATION_NAME,
        ArchiveFailureCode.OUTPUT_FAILURE,
        -> ArchiveFailureStage.OUTPUT
    }

open class ArchiveException(
    val code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
    val format: ArchiveFormat? = null,
    val stage: ArchiveFailureStage = code.defaultStage,
) : IOException(message, cause)

class ArchiveValidationException(
    code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
    format: ArchiveFormat? = null,
    stage: ArchiveFailureStage = code.defaultStage,
) : ArchiveException(code, message, cause, format, stage)

class ArchiveSelectionException(
    code: ArchiveFailureCode,
    message: String,
    format: ArchiveFormat? = null,
    stage: ArchiveFailureStage = code.defaultStage,
) : ArchiveException(code, message, format = format, stage = stage)

class ArchiveExtractionException(
    code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
    format: ArchiveFormat? = null,
    stage: ArchiveFailureStage = code.defaultStage,
) : ArchiveException(code, message, cause, format, stage)
