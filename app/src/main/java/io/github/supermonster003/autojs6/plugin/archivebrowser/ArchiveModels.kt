package io.github.supermonster003.autojs6.plugin.archivebrowser

import java.io.IOException

data class ArchiveSecurityLimits(
    val maxEntries: Int = 20_000,
    val maxPathLength: Int = 1_024,
    val maxDepth: Int = 64,
    val maxSingleUncompressedBytes: Long = 512L * 1_024L * 1_024L,
    val maxTotalUncompressedBytes: Long = 2L * 1_024L * 1_024L * 1_024L,
    val maxCompressionRatio: Long = 1_000L,
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

enum class ArchiveCompressionMethod(val zipMethod: Int) {
    STORED(0),
    DEFLATED(8),
}

data class ArchiveEntry(
    /** Canonical NFC path without a trailing slash. */
    val path: String,
    /** Exact entry name used to reopen the entry from the ZIP central directory. */
    val sourceName: String,
    val displayName: String,
    val isDirectory: Boolean,
    val compressionMethod: ArchiveCompressionMethod,
    val compressedSize: Long,
    /** Actual number of bytes observed by [ArchiveScanner]. */
    val uncompressedSize: Long,
    val crc32: Long?,
    val modifiedTimeMillis: Long?,
    val ordinal: Int,
)

data class ArchiveSnapshot(
    val sourceLength: Long,
    val sourceLastModifiedMillis: Long,
    val entries: List<ArchiveEntry>,
    val totalUncompressedBytes: Long,
    val limits: ArchiveSecurityLimits,
)

data class ArchiveNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    /** Null for an implicit directory that has no central-directory entry. */
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

enum class ArchiveFailureCode {
    SOURCE_NOT_FILE,
    SOURCE_CHANGED,
    INVALID_SIGNATURE,
    MALFORMED_ARCHIVE,
    UNSUPPORTED_METHOD,
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

open class ArchiveException(
    val code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

class ArchiveValidationException(
    code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
) : ArchiveException(code, message, cause)

class ArchiveSelectionException(
    code: ArchiveFailureCode,
    message: String,
) : ArchiveException(code, message)

class ArchiveExtractionException(
    code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
) : ArchiveException(code, message, cause)
