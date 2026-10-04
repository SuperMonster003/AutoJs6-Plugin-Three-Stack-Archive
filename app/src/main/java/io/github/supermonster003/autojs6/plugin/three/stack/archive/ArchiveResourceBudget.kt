package io.github.supermonster003.autojs6.plugin.three.stack.archive

enum class ArchiveResourceBudgetProfile {
    COMPATIBLE,
    STRICT,
    CUSTOM,
}

/**
 * User-selectable extraction warning and enforcement thresholds.
 *
 * Exceeding one of these values never prevents metadata browsing. Extraction requires explicit
 * confirmation. A confirmed operation expands live byte and ratio limits only far enough to
 * cover the values declared by the selected entries, so undeclared growth still fails safely.
 */
data class ArchiveResourceBudget(
    val maxEntries: Int,
    val maxPathLength: Int,
    val maxDepth: Int,
    val maxSingleUncompressedBytes: Long,
    val maxTotalUncompressedBytes: Long,
    val maxCompressionRatio: Long,
) {
    init {
        require(maxEntries > 0)
        require(maxPathLength > 0)
        require(maxDepth > 0)
        require(maxSingleUncompressedBytes >= 0L)
        require(maxTotalUncompressedBytes >= maxSingleUncompressedBytes)
        require(maxCompressionRatio > 0L)
    }

    fun expandedToInclude(assessment: ArchiveResourceBudgetAssessment): ArchiveResourceBudget {
        val violations = assessment.violations.associateBy(ArchiveResourceBudgetViolation::kind)
        val expandedSingle = maxOf(
            maxSingleUncompressedBytes,
            violations[ArchiveResourceBudgetViolationKind.SINGLE_UNCOMPRESSED_SIZE]?.actual ?: 0L,
        )
        return copy(
            maxSingleUncompressedBytes = expandedSingle,
            maxTotalUncompressedBytes = maxOf(
                maxTotalUncompressedBytes,
                assessment.estimatedOutputBytes,
                expandedSingle,
            ),
            maxCompressionRatio = maxOf(
                maxCompressionRatio,
                violations[ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO]?.actual ?: 0L,
            ),
        )
    }

    companion object {
        private const val GIB = 1_024L * 1_024L * 1_024L

        @JvmField
        val COMPATIBLE = ArchiveResourceBudget(
            maxEntries = 100_000,
            maxPathLength = 16_384,
            maxDepth = 256,
            maxSingleUncompressedBytes = 32L * GIB,
            maxTotalUncompressedBytes = 256L * GIB,
            maxCompressionRatio = 100_000L,
        )

        @JvmField
        val STRICT = ArchiveResourceBudget(
            maxEntries = 10_000,
            maxPathLength = 4_096,
            maxDepth = 64,
            maxSingleUncompressedBytes = 2L * GIB,
            maxTotalUncompressedBytes = 8L * GIB,
            maxCompressionRatio = 1_000L,
        )
    }
}

enum class ArchiveResourceBudgetViolationKind {
    ENTRY_COUNT,
    PATH_LENGTH,
    DEPTH,
    SINGLE_UNCOMPRESSED_SIZE,
    TOTAL_UNCOMPRESSED_SIZE,
    COMPRESSION_RATIO,
}

data class ArchiveResourceBudgetViolation(
    val kind: ArchiveResourceBudgetViolationKind,
    val actual: Long,
    val limit: Long,
)

data class ArchiveResourceBudgetAssessment(
    val violations: List<ArchiveResourceBudgetViolation>,
    val estimatedOutputBytes: Long,
    val totalEntries: Int,
) {
    val exceedsBudget: Boolean
        get() = violations.isNotEmpty()
}

object ArchiveResourceBudgetEvaluator {

    fun assess(
        selection: ResolvedArchiveSelection,
        budget: ArchiveResourceBudget,
    ): ArchiveResourceBudgetAssessment {
        val paths = sequence {
            yieldAll(selection.directories)
            selection.files.forEach { yield(it.path) }
        }
        var longestPath = 0L
        var deepestPath = 0L
        paths.forEach { path ->
            longestPath = maxOf(longestPath, path.length.toLong())
            deepestPath = maxOf(deepestPath, pathDepth(path).toLong())
        }
        val largestEntry = selection.files.maxOfOrNull(ArchiveEntry::uncompressedSize) ?: 0L
        val highestRatio = selection.files
            .asSequence()
            .mapNotNull(::declaredCompressionRatio)
            .maxOrNull() ?: 0L
        val violations = buildList {
            addIfExceeded(
                ArchiveResourceBudgetViolationKind.ENTRY_COUNT,
                selection.totalEntries.toLong(),
                budget.maxEntries.toLong(),
            )
            addIfExceeded(
                ArchiveResourceBudgetViolationKind.PATH_LENGTH,
                longestPath,
                budget.maxPathLength.toLong(),
            )
            addIfExceeded(
                ArchiveResourceBudgetViolationKind.DEPTH,
                deepestPath,
                budget.maxDepth.toLong(),
            )
            addIfExceeded(
                ArchiveResourceBudgetViolationKind.SINGLE_UNCOMPRESSED_SIZE,
                largestEntry,
                budget.maxSingleUncompressedBytes,
            )
            addIfExceeded(
                ArchiveResourceBudgetViolationKind.TOTAL_UNCOMPRESSED_SIZE,
                selection.totalUncompressedBytes,
                budget.maxTotalUncompressedBytes,
            )
            addIfExceeded(
                ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO,
                highestRatio,
                budget.maxCompressionRatio,
            )
        }
        return ArchiveResourceBudgetAssessment(
            violations = violations,
            estimatedOutputBytes = selection.totalUncompressedBytes,
            totalEntries = selection.totalEntries,
        )
    }

    private fun MutableList<ArchiveResourceBudgetViolation>.addIfExceeded(
        kind: ArchiveResourceBudgetViolationKind,
        actual: Long,
        limit: Long,
    ) {
        if (actual > limit) add(ArchiveResourceBudgetViolation(kind, actual, limit))
    }

    private fun declaredCompressionRatio(entry: ArchiveEntry): Long? {
        if (entry.compressedSize < 0L) return null
        if (entry.uncompressedSize == 0L) return 0L
        if (entry.compressedSize == 0L) return Long.MAX_VALUE
        return 1L + (entry.uncompressedSize - 1L) / entry.compressedSize
    }

    private fun pathDepth(path: String): Int = if (path.isEmpty()) {
        0
    } else {
        path.count { it == '/' } + 1
    }
}
