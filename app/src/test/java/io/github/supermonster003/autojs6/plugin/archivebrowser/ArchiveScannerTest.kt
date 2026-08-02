package io.github.supermonster003.autojs6.plugin.archivebrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry

class ArchiveScannerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `scans stored and deflated entries and records actual metadata`() {
        val source = archive(
            FixtureEntry("folder/", method = ZipEntry.STORED),
            FixtureEntry("folder/a.txt", "alpha".toByteArray(), ZipEntry.STORED),
            FixtureEntry("implicit/deep/b.txt", "bravo".toByteArray()),
        )

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(3, snapshot.entries.size)
        assertEquals(10L, snapshot.totalUncompressedBytes)
        assertEquals(ArchiveCompressionMethod.STORED, snapshot.entries[1].compressionMethod)
        assertEquals(ArchiveCompressionMethod.DEFLATED, snapshot.entries[2].compressionMethod)
        assertTrue(snapshot.entries.all { it.crc32 != null })
    }

    @Test
    fun `accepts an empty zip`() {
        val snapshot = ArchiveScanner().scan(archive())

        assertTrue(snapshot.entries.isEmpty())
        assertEquals(0L, snapshot.totalUncompressedBytes)
    }

    @Test
    fun `supports cancellation while measuring actual entry data`() {
        val source = archive(
            FixtureEntry("large.bin", ByteArray(100_000) { 1 }, ZipEntry.STORED),
        )
        var checks = 0

        try {
            ArchiveScanner().scan(source) {
                checks++
                if (checks == 9) throw CancellationException("test cancellation")
            }
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertTrue(checks >= 9)
    }

    @Test
    fun `rejects case and Unicode normalization collisions`() {
        val caseCollision = archive(
            FixtureEntry("Folder/a.txt", byteArrayOf(1)),
            FixtureEntry("folder/A.txt", byteArrayOf(2)),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DUPLICATE_PATH) {
            ArchiveScanner().scan(caseCollision)
        }

        val unicodeCollision = archive(
            FixtureEntry("\u00E9.txt", byteArrayOf(1)),
            FixtureEntry("e\u0301.txt", byteArrayOf(2)),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DUPLICATE_PATH) {
            ArchiveScanner().scan(unicodeCollision)
        }
    }

    @Test
    fun `rejects file directory conflicts in either order`() {
        val fileFirst = archive(
            FixtureEntry("node", byteArrayOf(1)),
            FixtureEntry("node/child", byteArrayOf(2)),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.FILE_DIRECTORY_CONFLICT) {
            ArchiveScanner().scan(fileFirst)
        }

        val childFirst = archive(
            FixtureEntry("node/child", byteArrayOf(1)),
            FixtureEntry("node", byteArrayOf(2)),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.FILE_DIRECTORY_CONFLICT) {
            ArchiveScanner().scan(childFirst)
        }
    }

    @Test
    fun `enforces entry size total size and compression ratio limits`() {
        val source = archive(
            FixtureEntry("a", ByteArray(20) { 1 }, ZipEntry.STORED),
            FixtureEntry("b", ByteArray(20) { 2 }, ZipEntry.STORED),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ArchiveScanner(ArchiveSecurityLimits(maxEntries = 1)).scan(source)
        }
        val implicitDirectoryExpansion = archive(
            FixtureEntry("one/two/three.txt", byteArrayOf(1), ZipEntry.STORED),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ArchiveScanner(ArchiveSecurityLimits(maxEntries = 2)).scan(implicitDirectoryExpansion)
        }
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED) {
            ArchiveScanner(limits(single = 10, total = 100)).scan(source)
        }
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED) {
            ArchiveScanner(limits(single = 30, total = 30)).scan(source)
        }

        val compressed = archive(FixtureEntry("bomb", ByteArray(20_000)))
        expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
        ) {
            ArchiveScanner(limits(single = 30_000, total = 30_000, ratio = 2)).scan(compressed)
        }
    }

    @Test
    fun `rejects CRC corruption and unsupported methods`() {
        val corrupt = archive(FixtureEntry("a", "alpha".toByteArray(), ZipEntry.STORED))
        patchFirstStoredEntryData(corrupt)
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.CRC_MISMATCH) {
            ArchiveScanner().scan(corrupt)
        }

        val unsupported = archive(FixtureEntry("a", "alpha".toByteArray(), ZipEntry.STORED))
        patchCompressionMethod(unsupported, 99)
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.UNSUPPORTED_METHOD) {
            ArchiveScanner().scan(unsupported)
        }
    }

    @Test
    fun `rejects non zip signatures and unsafe names`() {
        val plain = temporaryFolder.newFile("plain.zip").apply { writeText("not a zip") }
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_SIGNATURE) {
            ArchiveScanner().scan(plain)
        }

        val traversal = archive(FixtureEntry("../escape", byteArrayOf(1)))
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_PATH) {
            ArchiveScanner().scan(traversal)
        }
    }

    private fun archive(vararg entries: FixtureEntry): File =
        writeZip(temporaryFolder.newFile("archive-${temporaryFolder.root.list().orEmpty().size}.zip"), *entries)

    private fun limits(
        single: Long,
        total: Long,
        ratio: Long = 1_000,
    ) = ArchiveSecurityLimits(
        maxSingleUncompressedBytes = single,
        maxTotalUncompressedBytes = total,
        maxCompressionRatio = ratio,
    )
}
