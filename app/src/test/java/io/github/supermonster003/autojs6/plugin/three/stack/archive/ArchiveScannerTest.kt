package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.apache.commons.compress.archivers.zip.Zip64Mode
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.charset.Charset
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveScannerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `scans stored and deflated entries from directory metadata`() {
        val source = archive(
            FixtureEntry("folder/", method = ZipEntry.STORED),
            FixtureEntry("folder/a.txt", "alpha".toByteArray(), ZipEntry.STORED),
            FixtureEntry("implicit/deep/b.txt", "bravo".toByteArray()),
        )

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
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
    fun `accepts a zip with a self extracting style preamble`() {
        val source = archive(FixtureEntry("payload.txt", "payload".toByteArray()))
        Files.write(
            source.toPath(),
            "MZ-compatible-preamble".toByteArray() + Files.readAllBytes(source.toPath()),
        )

        val snapshot = ArchiveScanner().scan(source)

        assertEquals("payload.txt", snapshot.entries.single().path)
    }

    @Test
    fun `accepts Zip64 directory records for a small compatibility fixture`() {
        val source = temporaryFolder.newFile("forced-zip64.zip")
        val expected = "forced Zip64 payload".toByteArray()
        ZipArchiveOutputStream(source).use { output ->
            output.setUseZip64(Zip64Mode.Always)
            output.putArchiveEntry(ZipArchiveEntry("zip64.txt"))
            output.write(expected)
            output.closeArchiveEntry()
        }

        val snapshot = ArchiveScanner().scan(source)

        assertEquals("zip64.txt", snapshot.entries.single().path)
        assertEquals(expected.size.toLong(), snapshot.entries.single().uncompressedSize)
    }

    @Test
    fun `accepts data descriptors and benign trailing data`() {
        val source = archive(FixtureEntry("descriptor.txt", "descriptor payload".toByteArray()))
        source.appendBytes("benign trailing application data".toByteArray())

        val snapshot = ArchiveScanner().scan(source)

        assertEquals("descriptor.txt", snapshot.entries.single().path)
        assertEquals(ArchiveCompressionMethod.DEFLATED, snapshot.entries.single().compressionMethod)
    }

    @Test
    fun `detects a legacy GB18030 filename`() {
        val source = temporaryFolder.newFile("legacy-gb18030.zip")
        ZipOutputStream(FileOutputStream(source), Charset.forName("GB18030")).use { output ->
            output.putNextEntry(ZipEntry("目录/文件.txt"))
            output.write("内容".toByteArray())
            output.closeEntry()
        }

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(Charset.forName("GB18030").name(), snapshot.readerOptions.filenameCharsetName)
        assertEquals("目录/文件.txt", snapshot.entries.single().path)
    }

    @Test
    fun `manual filename encoding overrides automatic detection and is retained`() {
        val source = temporaryFolder.newFile("manual-gb18030.zip")
        ZipOutputStream(FileOutputStream(source), Charset.forName("GB18030")).use { output ->
            output.putNextEntry(ZipEntry("目录/文件.txt"))
            output.write("内容".toByteArray())
            output.closeEntry()
        }

        val overridden = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(filenameCharsetName = "GB18030"),
        )

        assertEquals(Charset.forName("GB18030").name(), overridden.readerOptions.filenameCharsetName)
        assertEquals("目录/文件.txt", overridden.entries.single().path)
    }

    @Test
    fun `unsupported filename encoding has a stable index diagnostic`() {
        val source = archive(FixtureEntry("entry.txt", byteArrayOf(1)))

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET,
        ) {
            ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(filenameCharsetName = "not-a-real-charset"),
            )
        }

        assertEquals(ArchiveFormat.ZIP, error.format)
        assertEquals(ArchiveFailureStage.INDEX, error.stage)
    }

    @Test
    fun `supports cancellation while indexing metadata`() {
        val source = archive(
            FixtureEntry("large.bin", ByteArray(100_000) { 1 }, ZipEntry.STORED),
        )
        var checks = 0

        try {
            ArchiveScanner().scan(source) {
                checks++
                if (checks == 3) throw CancellationException("test cancellation")
            }
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertTrue(checks >= 3)
    }

    @Test
    fun `allows case and Unicode spelling variants`() {
        val variants = archive(
            FixtureEntry("Folder/a.txt", byteArrayOf(1)),
            FixtureEntry("folder/A.txt", byteArrayOf(2)),
            FixtureEntry("\u00E9.txt", byteArrayOf(1)),
            FixtureEntry("e\u0301.txt", byteArrayOf(2)),
        )

        assertEquals(4, ArchiveScanner().scan(variants).entries.size)
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
    fun `metadata browsing applies hard structure limits but only assesses resource budgets`() {
        val source = archive(
            FixtureEntry("a", ByteArray(20) { 1 }, ZipEntry.STORED),
            FixtureEntry("b", ByteArray(20) { 2 }, ZipEntry.STORED),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ArchiveScanner(ArchiveStructureLimits(maxEntries = 1)).scan(source)
        }
        val implicitDirectoryExpansion = archive(
            FixtureEntry("one/two/three.txt", byteArrayOf(1), ZipEntry.STORED),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ArchiveScanner(
                ArchiveStructureLimits(maxEntries = 2, maxPathNodes = 2),
            ).scan(implicitDirectoryExpansion)
        }
        val browsable = ArchiveScanner().scan(source)
        assertEquals(40L, browsable.totalUncompressedBytes)
        val sizeAssessment = ArchiveResourceBudgetEvaluator.assess(
            ArchiveSelection.resolve(browsable, listOf("")),
            budget(single = 10, total = 10),
        )
        assertEquals(
            setOf(
                ArchiveResourceBudgetViolationKind.SINGLE_UNCOMPRESSED_SIZE,
                ArchiveResourceBudgetViolationKind.TOTAL_UNCOMPRESSED_SIZE,
            ),
            sizeAssessment.violations.map(ArchiveResourceBudgetViolation::kind).toSet(),
        )

        val compressed = archive(FixtureEntry("bomb", ByteArray(20_000)))
        val compressedSnapshot = ArchiveScanner().scan(compressed)
        assertEquals(20_000L, compressedSnapshot.totalUncompressedBytes)
        assertTrue(
            ArchiveResourceBudgetEvaluator.assess(
                ArchiveSelection.resolve(compressedSnapshot, listOf("")),
                budget(single = Long.MAX_VALUE, total = Long.MAX_VALUE, ratio = 2),
            ).violations.any {
                it.kind == ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO
            },
        )

        val longButBrowsable = archive(
            FixtureEntry("a".repeat(ArchiveResourceBudget.COMPATIBLE.maxPathLength + 1), byteArrayOf(1)),
        )
        val longSnapshot = ArchiveScanner().scan(longButBrowsable)
        assertTrue(
            ArchiveResourceBudgetEvaluator.assess(
                ArchiveSelection.resolve(longSnapshot, listOf("")),
                ArchiveResourceBudget.COMPATIBLE,
            ).violations.any { it.kind == ArchiveResourceBudgetViolationKind.PATH_LENGTH },
        )
    }

    @Test
    fun `browsing defers CRC validation and marks unsupported methods`() {
        val corrupt = archive(FixtureEntry("a", "alpha".toByteArray(), ZipEntry.STORED))
        patchFirstStoredEntryData(corrupt)
        assertEquals(1, ArchiveScanner().scan(corrupt).entries.size)

        val unsupported = archive(FixtureEntry("a", "alpha".toByteArray(), ZipEntry.STORED))
        patchCompressionMethod(unsupported, 99)
        val unsupportedEntry = ArchiveScanner().scan(unsupported).entries.single()
        assertEquals(ArchiveCompressionMethod.OTHER, unsupportedEntry.compressionMethod)
        assertEquals("99", unsupportedEntry.compressionMethodId)
        assertTrue(!unsupportedEntry.canExtract)
        assertTrue(
            ArchiveEntryLimitation.UNSUPPORTED_COMPRESSION_METHOD in
                unsupportedEntry.capabilities.limitations,
        )
    }

    @Test
    fun `rejects non archive signatures`() {
        val plain = temporaryFolder.newFile("plain.zip").apply { writeText("not a zip") }
        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.INVALID_SIGNATURE,
        ) {
            ArchiveScanner().scan(plain)
        }
        assertEquals(null, error.format)
        assertEquals(ArchiveFailureStage.FORMAT_DETECTION, error.stage)
    }

    @Test
    fun `isolates unsafe names while keeping safe entries browsable and previewable`() {
        val source = archive(
            FixtureEntry("good.txt", "good".toByteArray(), ZipEntry.STORED),
            FixtureEntry("../escape.txt", "traversal".toByteArray(), ZipEntry.STORED),
            FixtureEntry("/absolute.txt", "absolute".toByteArray(), ZipEntry.STORED),
            FixtureEntry("C:/drive.txt", "drive".toByteArray(), ZipEntry.STORED),
            FixtureEntry("bidi\u202Ename.txt", "bidi".toByteArray(), ZipEntry.STORED),
        )

        val snapshot = ArchiveScanner().scan(source)
        val isolatedRoot = requireNotNull(snapshot.isolatedPathRoot)
        val safeEntry = snapshot.entries.single { it.sourceName == "good.txt" }
        val isolatedEntries = snapshot.entries.filter {
            it.pathStatus == ArchiveEntryPathStatus.UNSAFE_ISOLATED
        }

        assertEquals("good.txt", safeEntry.path)
        assertTrue(safeEntry.canExtract)
        assertEquals(4, isolatedEntries.size)
        assertTrue(isolatedEntries.all { it.path.startsWith("$isolatedRoot/") })
        assertTrue(isolatedEntries.all(ArchiveEntry::canOpen))
        assertTrue(isolatedEntries.none(ArchiveEntry::canExtract))
        assertEquals(
            "bidi\\u202Ename.txt",
            isolatedEntries.single { it.sourceName.startsWith("bidi") }.displayName,
        )

        val traversal = isolatedEntries.single { it.sourceName == "../escape.txt" }
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(traversal, output)
        assertEquals("traversal", output.toString(Charsets.UTF_8.name()))

        val index = ArchiveIndex(snapshot, "Quarantined paths")
        assertEquals(
            listOf("Quarantined paths", "good.txt"),
            index.children().map(ArchiveNode::name),
        )
        assertEquals(
            setOf("../escape.txt", "/absolute.txt", "C:/drive.txt", "bidi\\u202Ename.txt"),
            index.children(isolatedRoot).map(ArchiveNode::name).toSet(),
        )
        assertEquals("Quarantined paths", index.displayPath(isolatedRoot))
        assertEquals(listOf("../escape.txt"), index.search("escape").map(ArchiveNode::name))
    }

    @Test
    fun `isolated virtual root never collides with a safe archive path`() {
        val source = archive(
            FixtureEntry(
                "${ArchivePathPolicy.ISOLATED_PATH_ROOT_BASENAME}/legit.txt",
                "legit".toByteArray(),
                ZipEntry.STORED,
            ),
            FixtureEntry("../escape.txt", "escape".toByteArray(), ZipEntry.STORED),
        )

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(
            "${ArchivePathPolicy.ISOLATED_PATH_ROOT_BASENAME}-2",
            snapshot.isolatedPathRoot,
        )
        assertTrue(snapshot.entries.map(ArchiveEntry::path).distinct().size == 2)
    }

    @Test
    fun `hard path structure limits still fail instead of entering read only isolation`() {
        val tooLong = archive(FixtureEntry("a".repeat(65), byteArrayOf(1), ZipEntry.STORED))
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.PATH_LIMIT_EXCEEDED) {
            ArchiveScanner(ArchiveStructureLimits(maxPathLength = 64)).scan(tooLong)
        }

        val tooDeep = archive(FixtureEntry("a/b/c.txt", byteArrayOf(1), ZipEntry.STORED))
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED) {
            ArchiveScanner(ArchiveStructureLimits(maxDepth = 2)).scan(tooDeep)
        }
    }

    private fun archive(vararg entries: FixtureEntry): File =
        writeZip(temporaryFolder.newFile("archive-${temporaryFolder.root.list().orEmpty().size}.zip"), *entries)

    private fun budget(
        single: Long,
        total: Long,
        ratio: Long = 1_000,
    ) = ArchiveResourceBudget(
        maxEntries = ArchiveResourceBudget.COMPATIBLE.maxEntries,
        maxPathLength = ArchiveResourceBudget.COMPATIBLE.maxPathLength,
        maxDepth = ArchiveResourceBudget.COMPATIBLE.maxDepth,
        maxSingleUncompressedBytes = single,
        maxTotalUncompressedBytes = total,
        maxCompressionRatio = ratio,
    )
}
