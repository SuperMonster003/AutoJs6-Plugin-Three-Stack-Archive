package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveResourceBudgetTest {

    @Test
    fun `assessment reports every exceeded dimension in a stable order`() {
        val selection = ResolvedArchiveSelection(
            requestedPaths = setOf(""),
            directories = listOf("deep", "deep/folder"),
            files = listOf(
                entry("deep/folder/long.bin", ordinal = 0, compressed = 1L, uncompressed = 20L),
                entry("second.bin", ordinal = 1, compressed = 5L, uncompressed = 5L),
            ),
        )
        val budget = ArchiveResourceBudget(
            maxEntries = 1,
            maxPathLength = 3,
            maxDepth = 1,
            maxSingleUncompressedBytes = 10L,
            maxTotalUncompressedBytes = 15L,
            maxCompressionRatio = 2L,
        )

        val assessment = ArchiveResourceBudgetEvaluator.assess(selection, budget)

        assertEquals(
            ArchiveResourceBudgetViolationKind.entries,
            assessment.violations.map(ArchiveResourceBudgetViolation::kind),
        )
        assertEquals(25L, assessment.estimatedOutputBytes)
        assertEquals(4, assessment.totalEntries)
        assertTrue(assessment.exceedsBudget)
    }

    @Test
    fun `unknown compressed size does not invent a ratio violation`() {
        val selection = ResolvedArchiveSelection(
            requestedPaths = setOf("stream.bin"),
            directories = emptyList(),
            files = listOf(entry("stream.bin", ordinal = 0, compressed = -1L, uncompressed = 20L)),
        )
        val budget = ArchiveResourceBudget.COMPATIBLE.copy(
            maxSingleUncompressedBytes = 100L,
            maxTotalUncompressedBytes = 100L,
            maxCompressionRatio = 1L,
        )

        val assessment = ArchiveResourceBudgetEvaluator.assess(selection, budget)

        assertFalse(
            assessment.violations.any {
                it.kind == ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO
            },
        )
    }

    @Test
    fun `confirmed budget expands only to the selected declaration`() {
        val selection = ResolvedArchiveSelection(
            requestedPaths = setOf("large.bin"),
            directories = emptyList(),
            files = listOf(entry("large.bin", ordinal = 0, compressed = 2L, uncompressed = 20L)),
        )
        val budget = ArchiveResourceBudget.COMPATIBLE.copy(
            maxSingleUncompressedBytes = 10L,
            maxTotalUncompressedBytes = 10L,
            maxCompressionRatio = 2L,
        )
        val assessment = ArchiveResourceBudgetEvaluator.assess(selection, budget)

        val confirmed = budget.expandedToInclude(assessment)

        assertEquals(20L, confirmed.maxSingleUncompressedBytes)
        assertEquals(20L, confirmed.maxTotalUncompressedBytes)
        assertEquals(10L, confirmed.maxCompressionRatio)
        assertEquals(budget.maxEntries, confirmed.maxEntries)
        assertEquals(budget.maxPathLength, confirmed.maxPathLength)
        assertEquals(budget.maxDepth, confirmed.maxDepth)
    }

    @Test
    fun `strict preset is no more permissive than compatible preset`() {
        val strict = ArchiveResourceBudget.STRICT
        val compatible = ArchiveResourceBudget.COMPATIBLE

        assertTrue(strict.maxEntries <= compatible.maxEntries)
        assertTrue(strict.maxPathLength <= compatible.maxPathLength)
        assertTrue(strict.maxDepth <= compatible.maxDepth)
        assertTrue(strict.maxSingleUncompressedBytes <= compatible.maxSingleUncompressedBytes)
        assertTrue(strict.maxTotalUncompressedBytes <= compatible.maxTotalUncompressedBytes)
        assertTrue(strict.maxCompressionRatio <= compatible.maxCompressionRatio)
    }

    private fun entry(
        path: String,
        ordinal: Int,
        compressed: Long,
        uncompressed: Long,
    ) = ArchiveEntry(
        path = path,
        sourceName = path,
        displayName = path.substringAfterLast('/'),
        isDirectory = false,
        compressionMethod = ArchiveCompressionMethod.DEFLATED,
        compressedSize = compressed,
        uncompressedSize = uncompressed,
        crc32 = null,
        modifiedTimeMillis = null,
        ordinal = ordinal,
    )
}
