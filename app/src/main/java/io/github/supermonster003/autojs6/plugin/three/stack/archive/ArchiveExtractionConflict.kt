package io.github.supermonster003.autojs6.plugin.three.stack.archive

enum class ArchiveExtractionConflictPolicy {
    ASK,
    SKIP,
    OVERWRITE,
    AUTO_RENAME,
}

enum class ArchiveExtractionConflictDecision {
    SKIP,
    OVERWRITE,
    AUTO_RENAME,
}

data class ArchiveExtractionConflict(
    val archivePath: String,
    val requestedDisplayName: String,
    val existingDisplayName: String,
    val incomingIsDirectory: Boolean,
    val existingIsDirectory: Boolean,
    /** Overwrite is limited to same-kind nodes created inside this extraction's new root. */
    val canOverwrite: Boolean,
)

data class ArchiveExtractionConflictResolution(
    val decision: ArchiveExtractionConflictDecision,
    val applyToAll: Boolean = false,
)

fun interface ArchiveExtractionConflictResolver {
    suspend fun resolve(conflict: ArchiveExtractionConflict): ArchiveExtractionConflictResolution

    companion object {
        @JvmField
        val NONE = ArchiveExtractionConflictResolver {
            throw ArchiveExtractionException(
                ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
                "An extraction output conflict requires an explicit decision",
            )
        }
    }
}

/** Resolves fixed policies and implements the user-facing "apply to all" choice. */
internal class ArchiveExtractionConflictController(
    private val policy: ArchiveExtractionConflictPolicy,
    private val resolver: ArchiveExtractionConflictResolver,
) {
    private var repeatedDecision: ArchiveExtractionConflictDecision? = null

    suspend fun resolve(conflict: ArchiveExtractionConflict): ArchiveExtractionConflictDecision {
        when (policy) {
            ArchiveExtractionConflictPolicy.SKIP -> return ArchiveExtractionConflictDecision.SKIP
            ArchiveExtractionConflictPolicy.AUTO_RENAME ->
                return ArchiveExtractionConflictDecision.AUTO_RENAME
            ArchiveExtractionConflictPolicy.OVERWRITE -> return if (conflict.canOverwrite) {
                ArchiveExtractionConflictDecision.OVERWRITE
            } else {
                ArchiveExtractionConflictDecision.AUTO_RENAME
            }
            ArchiveExtractionConflictPolicy.ASK -> Unit
        }

        repeatedDecision?.takeIf {
            it != ArchiveExtractionConflictDecision.OVERWRITE || conflict.canOverwrite
        }?.let { return it }

        val resolution = resolver.resolve(conflict)
        if (
            resolution.decision == ArchiveExtractionConflictDecision.OVERWRITE &&
            !conflict.canOverwrite
        ) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
                "The selected output conflict cannot be overwritten safely",
            )
        }
        if (resolution.applyToAll) repeatedDecision = resolution.decision
        return resolution.decision
    }
}
