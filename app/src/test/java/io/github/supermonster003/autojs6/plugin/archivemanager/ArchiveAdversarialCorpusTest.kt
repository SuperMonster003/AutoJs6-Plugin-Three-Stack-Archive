package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveAdversarialCorpusTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `high ratio zip remains metadata browsable and triggers the extraction budget`() {
        val source = writeHighRatioZip(
            temporaryFolder.newFile("high-ratio.zip"),
            HIGH_RATIO_EXPANDED_BYTES,
        )

        val snapshot = ArchiveScanner().scan(source)
        val entry = snapshot.entries.single()
        val assessment = ArchiveResourceBudgetEvaluator.assess(
            ArchiveSelection.resolve(snapshot, listOf(ArchivePathPolicy.ROOT_PATH)),
            ArchiveResourceBudget.STRICT.copy(maxCompressionRatio = TEST_COMPRESSION_RATIO_LIMIT),
        )

        assertEquals(HIGH_RATIO_EXPANDED_BYTES, entry.uncompressedSize)
        assertTrue(entry.compressedSize > 0L)
        assertTrue(entry.compressedSize * TEST_COMPRESSION_RATIO_LIMIT < entry.uncompressedSize)
        assertTrue(
            assessment.violations.any {
                it.kind == ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO
            },
        )
    }

    @Test
    fun `twenty thousand entry zip remains browsable but exceeds the strict budget`() {
        val source = writeManyEntryZip(
            temporaryFolder.newFile("many-entries.zip"),
            MANY_ENTRY_COUNT,
        )

        val snapshot = ArchiveScanner().scan(source)
        val assessment = ArchiveResourceBudgetEvaluator.assess(
            ArchiveSelection.resolve(snapshot, listOf(ArchivePathPolicy.ROOT_PATH)),
            ArchiveResourceBudget.STRICT,
        )
        val entryCountViolation = assessment.violations.single {
            it.kind == ArchiveResourceBudgetViolationKind.ENTRY_COUNT
        }

        assertEquals(MANY_ENTRY_COUNT, snapshot.entries.size)
        assertEquals("entry-00000.txt", snapshot.entries.first().path)
        assertEquals("entry-20000.txt", snapshot.entries.last().path)
        assertEquals(MANY_ENTRY_COUNT.toLong(), entryCountViolation.actual)
        assertEquals(ArchiveResourceBudget.STRICT.maxEntries.toLong(), entryCountViolation.limit)
    }

    @Test
    fun `deep path remains browsable within the hard ceiling and is rejected beyond it`() {
        val supportedPath = pathAtDepth(SUPPORTED_STRESS_DEPTH)
        val supported = writeZip(
            temporaryFolder.newFile("deep-supported.zip"),
            FixtureEntry(supportedPath, byteArrayOf(1), ZipEntry.STORED),
        )

        val snapshot = ArchiveScanner().scan(supported)
        val assessment = ArchiveResourceBudgetEvaluator.assess(
            ArchiveSelection.resolve(snapshot, listOf(ArchivePathPolicy.ROOT_PATH)),
            ArchiveResourceBudget.COMPATIBLE,
        )
        val depthViolation = assessment.violations.single {
            it.kind == ArchiveResourceBudgetViolationKind.DEPTH
        }

        assertEquals(supportedPath, snapshot.entries.single().path)
        assertEquals(SUPPORTED_STRESS_DEPTH.toLong(), depthViolation.actual)
        assertEquals(ArchiveResourceBudget.COMPATIBLE.maxDepth.toLong(), depthViolation.limit)

        val excessiveDepth = ArchiveStructureLimits.DEFAULT.maxDepth + 1
        val excessive = writeZip(
            temporaryFolder.newFile("deep-rejected.zip"),
            FixtureEntry(pathAtDepth(excessiveDepth), byteArrayOf(1), ZipEntry.STORED),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED) {
            ArchiveScanner().scan(excessive)
        }
    }

    @Test
    fun `malformed noncritical zip extra field does not hide valid entry data`() {
        val expected = "payload behind malformed extra field".toByteArray()
        val source = writeZipWithMalformedExtra(
            file = temporaryFolder.newFile("malformed-extra.zip"),
            name = "payload.txt",
            bytes = expected,
        )

        val snapshot = ArchiveScanner().scan(source)
        val entry = snapshot.entries.single()
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(entry, output)

        assertEquals("payload.txt", entry.path)
        assertTrue(entry.canOpen)
        assertTrue(entry.canExtract)
        assertArrayEquals(expected, output.toByteArray())
    }

    private fun writeHighRatioZip(file: File, expandedBytes: Long): File {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { output ->
            output.putNextEntry(ZipEntry("expanded-zeroes.bin").apply { time = FIXED_TEST_TIME })
            val block = ByteArray(WRITE_BLOCK_BYTES)
            var remaining = expandedBytes
            while (remaining > 0L) {
                val bytesToWrite = minOf(remaining, block.size.toLong()).toInt()
                output.write(block, 0, bytesToWrite)
                remaining -= bytesToWrite
            }
            output.closeEntry()
        }
        return file
    }

    private fun writeManyEntryZip(file: File, count: Int): File {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { output ->
            repeat(count) { index ->
                val entry = ZipEntry("entry-${index.toString().padStart(5, '0')}.txt").apply {
                    method = ZipEntry.STORED
                    time = FIXED_TEST_TIME
                    size = 0L
                    compressedSize = 0L
                    crc = 0L
                }
                output.putNextEntry(entry)
                output.closeEntry()
            }
        }
        return file
    }

    private fun pathAtDepth(depth: Int): String {
        require(depth > 0)
        return List(depth - 1) { "d" }.plus("payload.txt").joinToString("/")
    }

    private companion object {
        const val FIXED_TEST_TIME = 1_700_000_000_000L
        const val HIGH_RATIO_EXPANDED_BYTES = 8L * 1_024L * 1_024L
        const val MANY_ENTRY_COUNT = 20_001
        const val SUPPORTED_STRESS_DEPTH = 512
        const val TEST_COMPRESSION_RATIO_LIMIT = 100L
        const val WRITE_BLOCK_BYTES = 64 * 1_024
    }
}
