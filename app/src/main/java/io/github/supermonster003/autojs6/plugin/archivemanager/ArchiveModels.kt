package io.github.supermonster003.autojs6.plugin.archivemanager

import java.io.IOException

/**
 * Non-configurable in-memory structure ceilings. These prevent an archive index from exhausting
 * the process and are deliberately separate from user-overridable extraction budgets.
 */
data class ArchiveStructureLimits(
    val maxEntries: Int = 250_000,
    val maxPathNodes: Int = 500_000,
    val maxPathLength: Int = 65_536,
    val maxDepth: Int = 1_024,
) {
    init {
        require(maxEntries > 0)
        require(maxPathNodes >= maxEntries)
        require(maxPathLength > 0)
        require(maxDepth > 0)
    }

    companion object {
        @JvmField
        val DEFAULT = ArchiveStructureLimits()
    }
}

internal fun ArchiveStructureLimits.isWithinHardLimits(): Boolean =
    maxEntries <= ArchiveStructureLimits.DEFAULT.maxEntries &&
        maxPathNodes <= ArchiveStructureLimits.DEFAULT.maxPathNodes &&
        maxPathLength <= ArchiveStructureLimits.DEFAULT.maxPathLength &&
        maxDepth <= ArchiveStructureLimits.DEFAULT.maxDepth

internal fun ArchiveStructureLimits.restrictedToHardLimits(): ArchiveStructureLimits =
    if (isWithinHardLimits()) {
        this
    } else {
        ArchiveStructureLimits(
            maxEntries = minOf(maxEntries, ArchiveStructureLimits.DEFAULT.maxEntries),
            maxPathNodes = minOf(maxPathNodes, ArchiveStructureLimits.DEFAULT.maxPathNodes),
            maxPathLength = minOf(maxPathLength, ArchiveStructureLimits.DEFAULT.maxPathLength),
            maxDepth = minOf(maxDepth, ArchiveStructureLimits.DEFAULT.maxDepth),
        )
    }

enum class ArchiveCompressionMethod {
    STORED,
    DEFLATED,
    DEFLATE64,
    LZMA,
    LZMA2,
    BZIP2,
    OTHER,
}

enum class ArchiveEncryptionMethod {
    ZIP_CRYPTO,
    AES,
    OTHER,
}

enum class ArchiveEntryPathStatus {
    SAFE,
    /** The source name is visible and its data may be previewed, but it must never be a write path. */
    UNSAFE_ISOLATED,
}

data class ArchiveEntry(
    /** Portable separator-normalized path without a trailing slash; Unicode spelling is preserved. */
    val path: String,
    /** Exact backend directory name used to reopen this entry. */
    val sourceName: String,
    val displayName: String,
    val isDirectory: Boolean,
    val pathStatus: ArchiveEntryPathStatus = ArchiveEntryPathStatus.SAFE,
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
    /** Compressed bytes for this entry, or -1 when a stream container has no per-entry value. */
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
        get() = capabilities.canExtract && isOutputPathSafe

    val isOutputPathSafe: Boolean
        get() = pathStatus == ArchiveEntryPathStatus.SAFE
}

data class ArchiveSnapshot(
    val sourceLength: Long,
    val sourceLastModifiedMillis: Long,
    val entries: List<ArchiveEntry>,
    val totalUncompressedBytes: Long,
    val structureLimits: ArchiveStructureLimits,
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val readerOptions: ArchiveReaderOptions = ArchiveReaderOptions(),
    /** Safe virtual root containing flat, read-only representations of unsafe source names. */
    val isolatedPathRoot: String? = null,
) {
    fun isIsolatedPath(path: String): Boolean = isolatedPathRoot?.let { root ->
        path == root || path.startsWith("$root/")
    } == true
}

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
    /** Unsafe source names covered by the request but deliberately excluded from output paths. */
    val skippedUnsafeEntries: List<ArchiveEntry> = emptyList(),
) {
    val totalUncompressedBytes: Long = files.sumOf(ArchiveEntry::uncompressedSize)
    val totalEntries: Int = directories.size + files.size
}

enum class ExtractionPhase {
    PREPARING,
    EXTRACTING,
    COMMITTING,
    CLEANING_UP,
    CLEANUP_FAILED,
    COMPLETED,
}

data class ExtractionProgress(
    val phase: ExtractionPhase,
    val currentPath: String?,
    val completedEntries: Int,
    val totalEntries: Int,
    val bytesWritten: Long,
    val totalBytes: Long,
    /** Stable output roots that could not be removed. Present only for [ExtractionPhase.CLEANUP_FAILED]. */
    val residualOutputs: List<ArchiveOutputLocation> = emptyList(),
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
    val entriesSkipped: Int = 0,
    val entriesOverwritten: Int = 0,
    val entriesAutoRenamed: Int = 0,
)

internal data class ArchiveCreationOptions(
    val outputDisplayName: String,
    val compressionLevel: Int,
    val password: CharArray? = null,
    val conflictPolicy: ArchiveCreationConflictPolicy = ArchiveCreationConflictPolicy.AUTO_RENAME,
    /** Null creates one ordinary output; otherwise this is the maximum size of each ZIP volume. */
    val splitVolumeSizeBytes: Long? = null,
) {
    init {
        splitVolumeSizeBytes?.let(ArchiveSplitVolumePolicy::requireVolumeSizeBytes)
    }
}

internal enum class ArchiveCreationConflictPolicy {
    AUTO_RENAME,
    ASK,
}

internal class ArchiveOutputNameUnavailableException(
    val requestedDisplayName: String,
    val hostFailure: IllegalArgumentException,
) : IllegalStateException("The exact archive output name is unavailable", hostFailure)

internal enum class ArchiveCreationPhase {
    SCANNING,
    COMPRESSING,
    VERIFYING,
    COMMITTING,
}

/**
 * A format-neutral creation update.
 *
 * During [ArchiveCreationPhase.SCANNING], completed counts mean discovered source entries. Once
 * scanning finishes, total counts and source-size metadata remain fixed for the rest of the task.
 * Unknown-sized files are reported separately so the UI never presents a partial byte total as an
 * exact value.
 */
internal data class ArchiveCreationProgress(
    val phase: ArchiveCreationPhase,
    val currentEntry: String?,
    val completedFiles: Long,
    val completedDirectories: Long,
    val sourceBytesRead: Long,
    val totalFiles: Long,
    val totalDirectories: Long,
    val knownSourceBytes: Long,
    val unknownSizeFiles: Long,
) {
    init {
        require(completedFiles >= 0L)
        require(completedDirectories >= 0L)
        require(sourceBytesRead >= 0L)
        require(totalFiles >= 0L)
        require(totalDirectories >= 0L)
        require(knownSourceBytes >= 0L)
        require(unknownSizeFiles in 0L..totalFiles)
        if (phase != ArchiveCreationPhase.SCANNING) {
            require(completedFiles <= totalFiles)
            require(completedDirectories <= totalDirectories)
        }
    }
}

internal data class CreatedArchiveOutput(
    val displayName: String,
    val displayPath: String,
)

internal data class ArchiveCreationResult(
    val outputDisplayName: String,
    val outputDisplayPath: String,
    val filesCompressed: Long,
    val directoriesAdded: Long,
    val sourceBytesRead: Long,
    /** Ordered physical outputs. Split ZIP parts precede the terminal `.zip` volume. */
    val createdOutputs: List<CreatedArchiveOutput> = listOf(
        CreatedArchiveOutput(outputDisplayName, outputDisplayPath),
    ),
) {
    init {
        require(createdOutputs.isNotEmpty())
        require(createdOutputs.map(CreatedArchiveOutput::displayName).distinct().size == createdOutputs.size)
        require(createdOutputs.last().displayName == outputDisplayName)
        require(createdOutputs.last().displayPath == outputDisplayPath)
    }
}

internal fun interface ArchiveCreationProgressListener {
    fun onProgress(progress: ArchiveCreationProgress)
}

enum class ArchiveFailureCode {
    SOURCE_NOT_FILE,
    SOURCE_UNREADABLE,
    SOURCE_CHANGED,
    CACHE_SPACE_UNAVAILABLE,
    INVALID_SIGNATURE,
    MALFORMED_ARCHIVE,
    MISSING_VOLUME,
    UNSUPPORTED_FILENAME_CHARSET,
    UNSUPPORTED_METHOD,
    PASSWORD_REQUIRED,
    WRONG_PASSWORD,
    ENTRY_LIMIT_EXCEEDED,
    INVALID_PATH,
    UNSAFE_PATH_CONFIRMATION_REQUIRED,
    RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
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
    OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
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
        ArchiveFailureCode.SOURCE_UNREADABLE,
        ArchiveFailureCode.SOURCE_CHANGED,
        ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE,
        -> ArchiveFailureStage.INPUT

        ArchiveFailureCode.INVALID_SIGNATURE -> ArchiveFailureStage.FORMAT_DETECTION

        ArchiveFailureCode.MALFORMED_ARCHIVE,
        ArchiveFailureCode.MISSING_VOLUME,
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
        ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
        ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
        -> ArchiveFailureStage.ENTRY_DATA

        ArchiveFailureCode.INVALID_DESTINATION_NAME,
        ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
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
