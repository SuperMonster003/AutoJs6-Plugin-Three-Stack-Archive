package io.github.supermonster003.autojs6.plugin.three.stack.archive

import java.io.InputStream

/** Format-neutral requests accepted by an archive mutation provider. */
internal sealed interface ArchiveMutationRequest {
    val operation: ArchiveOperation

    data class Delete(val paths: Set<String>) : ArchiveMutationRequest {
        override val operation = ArchiveOperation.DELETE
    }

    data class Rename(
        val path: String,
        val newDisplayName: String,
    ) : ArchiveMutationRequest {
        override val operation = ArchiveOperation.RENAME
    }

    data class AddDirectory(
        val parentPath: String,
        val displayName: String,
    ) : ArchiveMutationRequest {
        override val operation = ArchiveOperation.ADD
    }

    data class AddFiles(
        val parentPath: String,
        val files: List<ArchiveMutationAddedFile>,
    ) : ArchiveMutationRequest {
        override val operation = ArchiveOperation.ADD
    }

    data class AddTree(
        val parentPath: String,
        val entries: List<ArchiveMutationAddedTreeEntry>,
    ) : ArchiveMutationRequest {
        override val operation = ArchiveOperation.ADD
    }
}

internal class ArchiveMutationAddedFile(
    val displayName: String,
    val size: Long = SIZE_UNKNOWN,
    val lastModified: Long = TIME_UNKNOWN,
    private val inputFactory: () -> InputStream,
) {
    init {
        require(size >= SIZE_UNKNOWN)
        require(lastModified >= TIME_UNKNOWN)
    }

    fun openInputStream(): InputStream = inputFactory()

    private companion object {
        const val SIZE_UNKNOWN = -1L
        const val TIME_UNKNOWN = -1L
    }
}

internal sealed interface ArchiveMutationAddedTreeEntry {
    val relativePath: String
    val lastModified: Long
    val inputRootId: String?

    data class Directory(
        override val relativePath: String,
        override val lastModified: Long = -1L,
        override val inputRootId: String? = null,
    ) : ArchiveMutationAddedTreeEntry

    data class FileEntry(
        override val relativePath: String,
        val file: ArchiveMutationAddedFile,
        override val inputRootId: String? = null,
    ) : ArchiveMutationAddedTreeEntry {
        override val lastModified: Long
            get() = file.lastModified
    }
}

internal enum class ArchiveMutationStrategy {
    FULL_REWRITE,
}

/** Potentially user-visible consequences exposed for preflight and archive-information UI. */
internal enum class ArchiveMutationMetadataEffect {
    ARCHIVE_COMMENT_DROPPED,
    ENTRY_COMMENTS_DROPPED,
    EXTRA_FIELDS_NORMALIZED,
    UNIX_ATTRIBUTES_DROPPED,
    COMPRESSION_SETTINGS_NORMALIZED,
    ENCRYPTION_SETTINGS_NORMALIZED,
}

internal data class ArchiveMutationCapabilities(
    val operations: Set<ArchiveOperation>,
    val strategy: ArchiveMutationStrategy,
    val minimumHostProtocolVersion: Int,
    val metadataEffects: Set<ArchiveMutationMetadataEffect>,
) {
    init {
        require(operations.isNotEmpty())
        require(operations.all { it in MUTATION_OPERATIONS })
        require(minimumHostProtocolVersion >= TARGET_REPLACEMENT_PROTOCOL_VERSION)
    }

    fun supports(operation: ArchiveOperation): Boolean = operation in operations

    private companion object {
        val MUTATION_OPERATIONS = setOf(
            ArchiveOperation.ADD,
            ArchiveOperation.DELETE,
            ArchiveOperation.RENAME,
        )
        const val TARGET_REPLACEMENT_PROTOCOL_VERSION = 8
    }
}

internal enum class ArchiveMutationUnavailableReason {
    FORMAT_NOT_SUPPORTED,
    MULTI_VOLUME_ARCHIVE,
    UNSAFE_ENTRY_PATH,
    PASSWORD_REQUIRED,
    UNSUPPORTED_ENTRY_METHOD,
    BACKEND_VARIANT_READ_ONLY,
}

internal data class ArchiveMutationAvailability(
    val capabilities: ArchiveMutationCapabilities?,
    val unavailableReason: ArchiveMutationUnavailableReason? = null,
) {
    val isAvailable: Boolean
        get() = capabilities != null && unavailableReason == null

    init {
        require((capabilities == null) == (unavailableReason != null))
    }

    companion object {
        fun available(capabilities: ArchiveMutationCapabilities) =
            ArchiveMutationAvailability(capabilities)

        fun unavailable(reason: ArchiveMutationUnavailableReason) =
            ArchiveMutationAvailability(null, reason)
    }
}

/** Factual work totals. Output size is deliberately not guessed before compression. */
internal data class ArchiveMutationWorkEstimate(
    val sourceArchiveBytes: Long,
    val resultEntryCount: Int,
    val resultFileCount: Int,
    val resultDirectoryCount: Int,
    val knownContentBytesToRead: Long,
    val unknownContentFileCount: Int,
) {
    init {
        require(sourceArchiveBytes >= 0L)
        require(resultEntryCount >= 0)
        require(resultFileCount >= 0)
        require(resultDirectoryCount >= 0)
        require(resultEntryCount == resultFileCount + resultDirectoryCount)
        require(knownContentBytesToRead >= 0L)
        require(unknownContentFileCount in 0..resultFileCount)
    }
}

internal data class ArchiveMutationSourceVersion(
    val format: ArchiveFormat,
    val sourceLength: Long,
    val sourceLastModifiedMillis: Long,
    val entries: List<ArchiveEntry>,
    val volumeIdentities: List<ArchiveVolumeIdentity>,
) {
    fun matches(snapshot: ArchiveSnapshot): Boolean =
        format == snapshot.format &&
            sourceLength == snapshot.sourceLength &&
            sourceLastModifiedMillis == snapshot.sourceLastModifiedMillis &&
            entries == snapshot.entries &&
            volumeIdentities == snapshot.volumeIdentities

    companion object {
        fun capture(snapshot: ArchiveSnapshot) = ArchiveMutationSourceVersion(
            format = snapshot.format,
            sourceLength = snapshot.sourceLength,
            sourceLastModifiedMillis = snapshot.sourceLastModifiedMillis,
            entries = snapshot.entries.toList(),
            volumeIdentities = snapshot.volumeIdentities.toList(),
        )
    }
}

internal interface PreparedArchiveMutation {
    val format: ArchiveFormat
    val operation: ArchiveOperation
    val sourceVersion: ArchiveMutationSourceVersion
    val workEstimate: ArchiveMutationWorkEstimate
    val metadataEffects: Set<ArchiveMutationMetadataEffect>
}

internal enum class ArchiveMutationPhase {
    PREPARING,
    WRITING,
    VERIFYING,
    COMMITTING,
}

internal data class ArchiveMutationProgress(
    val phase: ArchiveMutationPhase,
    val currentPath: String?,
    val completedEntries: Int,
    val totalEntries: Int,
) {
    init {
        require(completedEntries in 0..totalEntries)
    }
}

internal fun interface ArchiveMutationProgressListener {
    fun onProgress(progress: ArchiveMutationProgress)

    companion object {
        @JvmField
        val NONE = ArchiveMutationProgressListener { }
    }
}

/**
 * A format-specific rewriter behind a format-neutral lifecycle. The provider prepares one
 * immutable plan before the host replacement is reserved, then executes exactly that plan through
 * the existing Explorer Action v8 target-replacement transaction.
 */
internal interface ArchiveMutationProvider {
    val format: ArchiveFormat
    val capabilities: ArchiveMutationCapabilities

    fun availability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability

    fun prepare(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): PreparedArchiveMutation

    fun execute(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        prepared: PreparedArchiveMutation,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener = ArchiveMutationProgressListener.NONE,
        beforeOutputVerification: () -> Unit = {},
    ): HostOutputTransaction

    fun mutate(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        request: ArchiveMutationRequest,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener = ArchiveMutationProgressListener.NONE,
        beforeOutputVerification: () -> Unit = {},
    ): HostOutputTransaction = execute(
        source = source,
        snapshot = snapshot,
        targetId = targetId,
        displayName = displayName,
        prepared = prepare(snapshot, request),
        checkCancelled = checkCancelled,
        progress = progress,
        beforeOutputVerification = beforeOutputVerification,
    )
}
