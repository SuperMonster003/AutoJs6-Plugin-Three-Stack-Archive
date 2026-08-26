package io.github.supermonster003.autojs6.plugin.archivemanager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveExtractorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `extracts selected content with progress and a newly created root`() = runBlocking {
        val source = archive(
            FixtureEntry("folder/a.txt", "alpha".toByteArray(), ZipEntry.STORED),
            FixtureEntry("b.txt", "bravo".toByteArray()),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()
        val progress = mutableListOf<ExtractionProgress>()

        val result = ArchiveExtractor().extractToWriter(
            source = source,
            snapshot = snapshot,
            selectedPaths = listOf(""),
            rootName = "archive",
            writer = writer,
            progress = ArchiveProgressListener(progress::add),
        )

        assertEquals("archive", result.root.identifier)
        assertEquals(2, result.filesExtracted)
        assertEquals(1, result.directoriesCreated)
        assertEquals(10L, result.bytesWritten)
        assertArrayEquals("alpha".toByteArray(), writer.content("archive/folder/a.txt"))
        assertArrayEquals("bravo".toByteArray(), writer.content("archive/b.txt"))
        assertFalse(writer.rootDeleted)
        assertTrue(writer.rootCommitted)
        assertEquals(ExtractionPhase.PREPARING, progress.first().phase)
        assertTrue(progress.indexOfFirst { it.phase == ExtractionPhase.COMMITTING } in 1 until progress.lastIndex)
        assertEquals(ExtractionPhase.COMPLETED, progress.last().phase)
        assertEquals(10L, progress.last().bytesWritten)
    }

    @Test
    fun `extracts a tar through the format neutral workflow`() = runBlocking {
        val source = writeTar(
            temporaryFolder.newFile("extract.tar"),
            TarFixtureEntry("folder/", type = TarFixtureEntryType.DIRECTORY),
            TarFixtureEntry("folder/文件.txt", "tar payload".toByteArray()),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        val result = ArchiveExtractor().extractToWriter(
            source = source,
            snapshot = snapshot,
            selectedPaths = listOf(""),
            rootName = "tar-output",
            writer = writer,
        )

        assertEquals(ArchiveFormat.TAR, snapshot.format)
        assertEquals(1, result.filesExtracted)
        assertEquals(1, result.directoriesCreated)
        assertArrayEquals(
            "tar payload".toByteArray(),
            writer.content("tar-output/folder/文件.txt"),
        )
    }

    @Test
    fun `tar links with external targets cannot reach the output writer`() {
        val source = writeTar(
            temporaryFolder.newFile("link-boundary.tar"),
            TarFixtureEntry("safe.txt", "safe".toByteArray()),
            TarFixtureEntry(
                name = "symbolic-link",
                type = TarFixtureEntryType.SYMBOLIC_LINK,
                linkName = "../../outside.txt",
            ),
            TarFixtureEntry(
                name = "hard-link",
                type = TarFixtureEntryType.HARD_LINK,
                linkName = "/absolute/outside.txt",
            ),
        )
        val snapshot = ArchiveScanner().scan(source)
        val rejectedWriter = FakeArchiveOutputWriter()

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.UNSUPPORTED_METHOD) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                    rootName = "rejected-links",
                    writer = rejectedWriter,
                )
            }
        }
        assertEquals(null, rejectedWriter.root)

        val safeWriter = FakeArchiveOutputWriter()
        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf("safe.txt"),
                rootName = "safe-only",
                writer = safeWriter,
            )
        }
        assertEquals(1, result.filesExtracted)
        assertEquals(setOf("safe-only/safe.txt"), safeWriter.filePaths())
        assertArrayEquals("safe".toByteArray(), safeWriter.content("safe-only/safe.txt"))
    }

    @Test
    fun `split archive extraction preserves the missing volume diagnosis`() {
        val source = copyFixture("winrar-6.10-store-split.part1.rar")
        val snapshot = ArchiveScanner().scan(source)

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.MISSING_VOLUME) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                    rootName = "split-output",
                    writer = FakeArchiveOutputWriter(),
                )
            }
        }
    }

    @Test
    fun `extraction reuses a manually selected filename encoding`() = runBlocking {
        val source = temporaryFolder.newFile("manual-encoding.zip")
        val expected = "兼容内容".toByteArray()
        ZipOutputStream(FileOutputStream(source), Charset.forName("GB18030")).use { output ->
            output.putNextEntry(ZipEntry("目录/文件.txt"))
            output.write(expected)
            output.closeEntry()
        }
        val snapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(filenameCharsetName = "GB18030"),
        )
        val writer = FakeArchiveOutputWriter()

        ArchiveExtractor().extractToWriter(
            source = source,
            snapshot = snapshot,
            selectedPaths = listOf(""),
            rootName = "encoded",
            writer = writer,
        )

        assertEquals(Charset.forName("GB18030").name(), snapshot.readerOptions.filenameCharsetName)
        assertArrayEquals(expected, writer.content("encoded/目录/文件.txt"))
    }

    @Test
    fun `extraction reuses the password held by the scanned snapshot`() = runBlocking {
        val source = copyFixture("7zip-22-aes256-unicode.zip")
        val snapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(password = "ArchiveManager-Test-2026".toCharArray()),
        )
        val writer = FakeArchiveOutputWriter()

        ArchiveExtractor().extractToWriter(
            source = source,
            snapshot = snapshot,
            selectedPaths = listOf("文件.txt"),
            rootName = "encrypted",
            writer = writer,
        )

        assertTrue(
            writer.content("encrypted/文件.txt")
                .toString(Charsets.UTF_8)
                .contains("UTF-8 文件名"),
        )
    }

    @Test
    fun `unsafe paths require explicit skip confirmation and are never written`() {
        val source = archive(
            FixtureEntry("safe.txt", "safe".toByteArray(), ZipEntry.STORED),
            FixtureEntry("../escape.txt", "escape".toByteArray(), ZipEntry.STORED),
            FixtureEntry("/absolute.txt", "absolute".toByteArray(), ZipEntry.STORED),
            FixtureEntry("C:/drive.txt", "drive".toByteArray(), ZipEntry.STORED),
            FixtureEntry("bidi\u202Ename.txt", "bidi".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val selection = ArchiveSelection.resolve(snapshot, listOf(ArchivePathPolicy.ROOT_PATH))
        val rejectedWriter = FakeArchiveOutputWriter()

        assertEquals(
            setOf("../escape.txt", "/absolute.txt", "C:/drive.txt", "bidi\u202Ename.txt"),
            selection.skippedUnsafeEntries.map(ArchiveEntry::sourceName).toSet(),
        )

        expectArchiveFailure<ArchiveExtractionException>(
            ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
        ) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(""),
                    rootName = "rejected",
                    writer = rejectedWriter,
                )
            }
        }
        assertEquals(null, rejectedWriter.root)

        val confirmedWriter = FakeArchiveOutputWriter()
        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf(""),
                rootName = "confirmed",
                writer = confirmedWriter,
                skipUnsafePaths = true,
            )
        }

        assertEquals(1, result.filesExtracted)
        assertArrayEquals("safe".toByteArray(), confirmedWriter.content("confirmed/safe.txt"))
        assertEquals(setOf("confirmed/safe.txt"), confirmedWriter.filePaths())

        val isolatedOnlyWriter = FakeArchiveOutputWriter()
        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.EMPTY_SELECTION) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(requireNotNull(snapshot.isolatedPathRoot)),
                    rootName = "empty",
                    writer = isolatedOnlyWriter,
                    skipUnsafePaths = true,
                )
            }
        }
        assertEquals(null, isolatedOnlyWriter.root)
    }

    @Test
    fun `unsafe extraction root names are rejected before output creation`() {
        val source = archive(FixtureEntry("safe.txt", "safe".toByteArray(), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val invalidNames = listOf(
            "../outside",
            "/absolute",
            "C:\\outside",
            "bidi\u202Ename",
        )

        invalidNames.forEach { rootName ->
            val writer = FakeArchiveOutputWriter()
            expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.INVALID_DESTINATION_NAME,
            ) {
                runBlocking {
                    ArchiveExtractor().extractToWriter(
                        source = source,
                        snapshot = snapshot,
                        selectedPaths = listOf("safe.txt"),
                        rootName = rootName,
                        writer = writer,
                    )
                }
            }
            assertEquals(null, writer.root)
            assertFalse(writer.rootDeleted)
        }
    }

    @Test
    fun `over budget extraction requires confirmation before creating output`() {
        val source = archive(FixtureEntry("large.txt", "large payload".toByteArray(), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val budget = ArchiveResourceBudget.COMPATIBLE.copy(
            maxSingleUncompressedBytes = 4L,
            maxTotalUncompressedBytes = 4L,
        )
        val rejectedWriter = FakeArchiveOutputWriter()

        expectArchiveFailure<ArchiveExtractionException>(
            ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
        ) {
            runBlocking {
                ArchiveExtractor(resourceBudget = budget).extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf("large.txt"),
                    rootName = "rejected-budget",
                    writer = rejectedWriter,
                )
            }
        }
        assertEquals(null, rejectedWriter.root)

        val confirmedWriter = FakeArchiveOutputWriter()
        val result = runBlocking {
            ArchiveExtractor(resourceBudget = budget).extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf("large.txt"),
                rootName = "confirmed-budget",
                writer = confirmedWriter,
                allowResourceBudgetOverride = true,
            )
        }

        assertEquals("large payload".toByteArray().size.toLong(), result.bytesWritten)
        assertArrayEquals(
            "large payload".toByteArray(),
            confirmedWriter.content("confirmed-budget/large.txt"),
        )
        assertFalse(confirmedWriter.rootDeleted)
    }

    @Test
    fun `confirmed compression ratio budget expands to the declared selection`() {
        val payload = ByteArray(100_000) { 7 }
        val source = archive(FixtureEntry("compressible.bin", payload))
        val snapshot = ArchiveScanner().scan(source)
        val budget = ArchiveResourceBudget.COMPATIBLE.copy(
            maxSingleUncompressedBytes = Long.MAX_VALUE,
            maxTotalUncompressedBytes = Long.MAX_VALUE,
            maxCompressionRatio = 2L,
        )
        val selection = ArchiveSelection.resolve(snapshot, listOf("compressible.bin"))
        val assessment = ArchiveResourceBudgetEvaluator.assess(selection, budget)
        assertEquals(
            listOf(ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO),
            assessment.violations.map(ArchiveResourceBudgetViolation::kind),
        )

        val writer = FakeArchiveOutputWriter()
        val result = runBlocking {
            ArchiveExtractor(resourceBudget = budget).extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf("compressible.bin"),
                rootName = "confirmed-ratio",
                writer = writer,
                allowResourceBudgetOverride = true,
            )
        }

        assertEquals(payload.size.toLong(), result.bytesWritten)
        assertArrayEquals(payload, writer.content("confirmed-ratio/compressible.bin"))
        assertFalse(writer.rootDeleted)
    }

    @Test
    fun `extracts an empty archive as an empty output root`() = runBlocking {
        val source = archive()
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        val result = ArchiveExtractor().extractToWriter(
            source = source,
            snapshot = snapshot,
            selectedPaths = listOf(""),
            rootName = "empty-archive",
            writer = writer,
        )

        assertEquals(0, result.filesExtracted)
        assertEquals(0, result.directoriesCreated)
        assertNotNull(writer.root)
        assertFalse(writer.rootDeleted)
    }

    @Test
    fun `cleans the newly created root after cancellation`() {
        val source = archive(FixtureEntry("large.bin", ByteArray(100_000) { 7 }, ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        val cancellation = try {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf("large.bin"),
                    rootName = "cancelled",
                    writer = writer,
                    progress = ArchiveProgressListener { update ->
                        if (update.phase == ExtractionPhase.EXTRACTING && update.bytesWritten > 0L) {
                            throw CancellationException("test cancellation")
                        }
                        if (update.phase == ExtractionPhase.CLEANING_UP) {
                            throw IllegalStateException("cleanup notification failure")
                        }
                    },
                )
            }
            throw AssertionError("Expected cancellation")
        } catch (expected: CancellationException) {
            expected
        }

        assertNotNull(writer.root)
        assertTrue(writer.rootDeleted)
        assertTrue(cancellation.residualArchiveOutputs().isEmpty())
    }

    @Test
    fun `cancellation reports a residual root even when its exception drops suppressed failures`() {
        val source = archive(FixtureEntry("large.bin", ByteArray(100_000) { 7 }, ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter(failDeletingRoot = true)
        var reportedResiduals = emptyList<ArchiveOutputLocation>()

        try {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf("large.bin"),
                    rootName = "cancelled-residual",
                    writer = writer,
                    progress = ArchiveProgressListener { update ->
                        if (update.phase == ExtractionPhase.CLEANUP_FAILED) {
                            reportedResiduals = update.residualOutputs
                        }
                        if (update.phase == ExtractionPhase.EXTRACTING && update.bytesWritten > 0L) {
                            throw CancellationException("test cancellation")
                        }
                    },
                )
            }
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertTrue(writer.rootDeletionAttempted)
        assertFalse(writer.rootDeleted)
        assertEquals(
            listOf(ArchiveOutputLocation("cancelled-residual", "cancelled-residual")),
            reportedResiduals,
        )
    }

    @Test
    fun `cleans the newly created root after destination failure`() {
        val source = archive(FixtureEntry("broken.txt", "data".toByteArray(), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter(failCreatingFile = "broken.txt")

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.OUTPUT_FAILURE) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source,
                    snapshot,
                    listOf("broken.txt"),
                    "failed",
                    writer,
                )
            }
        }

        assertTrue(writer.rootDeleted)
    }

    @Test
    fun `reports the stable residual root when destination cleanup fails`() {
        val source = archive(FixtureEntry("broken.txt", "data".toByteArray(), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter(
            failCreatingFile = "broken.txt",
            failDeletingRoot = true,
        )

        val error = expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.OUTPUT_FAILURE) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source,
                    snapshot,
                    listOf("broken.txt"),
                    "residual-output",
                    writer,
                )
            }
        }

        assertTrue(writer.rootDeletionAttempted)
        assertFalse(writer.rootDeleted)
        assertEquals(
            listOf(ArchiveOutputLocation("residual-output", "residual-output")),
            error.residualArchiveOutputs(),
        )
        val cleanup = error.suppressed.filterIsInstance<ArchiveCleanupException>().single()
        assertEquals(ArchiveFailureStage.CLEANUP, cleanup.stage)
        assertTrue(cleanup.cause is IOException)
    }

    @Test
    fun `counts and verifies entry data again during extraction`() {
        val source = archive(FixtureEntry("changed.txt", "original".toByteArray(), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        patchFirstStoredEntryData(source)
        assertTrue(source.setLastModified(snapshot.sourceLastModifiedMillis))
        val writer = FakeArchiveOutputWriter()

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.CRC_MISMATCH) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source,
                    snapshot,
                    listOf("changed.txt"),
                    "changed",
                    writer,
                )
            }
        }

        assertTrue(writer.rootDeleted)
    }

    @Test
    fun `rejects changed source before creating output`() {
        val source = archive(FixtureEntry("a.txt", "a".toByteArray(), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        source.appendBytes(byteArrayOf(0))
        val writer = FakeArchiveOutputWriter()

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.SOURCE_CHANGED) {
            runBlocking {
                ArchiveExtractor().extractToWriter(source, snapshot, listOf(""), "changed", writer)
            }
        }

        assertEquals(null, writer.root)
        assertFalse(writer.rootDeleted)
    }

    @Test
    fun `auto rename preserves every folded file conflict and keeps the extension`() {
        val source = archive(
            FixtureEntry("A.txt", "first".toByteArray(), ZipEntry.STORED),
            FixtureEntry("a.txt", "second".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                rootName = "renamed",
                writer = writer,
                conflictPolicy = ArchiveExtractionConflictPolicy.AUTO_RENAME,
            )
        }

        assertEquals(setOf("renamed/A.txt", "renamed/a (2).txt"), writer.filePaths())
        assertArrayEquals("first".toByteArray(), writer.content("renamed/A.txt"))
        assertArrayEquals("second".toByteArray(), writer.content("renamed/a (2).txt"))
        assertEquals(2, result.filesExtracted)
        assertEquals(0, result.entriesSkipped)
        assertEquals(0, result.entriesOverwritten)
        assertEquals(1, result.entriesAutoRenamed)
    }

    @Test
    fun `skip keeps the first folded output and reports every omitted entry`() {
        val source = archive(
            FixtureEntry("A.txt", "first".toByteArray(), ZipEntry.STORED),
            FixtureEntry("a.txt", "second".toByteArray(), ZipEntry.STORED),
            FixtureEntry("A.TXT", "third".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()
        val progress = mutableListOf<ExtractionProgress>()

        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                rootName = "skipped",
                writer = writer,
                conflictPolicy = ArchiveExtractionConflictPolicy.SKIP,
                progress = ArchiveProgressListener(progress::add),
            )
        }

        assertEquals(setOf("skipped/A.txt"), writer.filePaths())
        assertArrayEquals("first".toByteArray(), writer.content("skipped/A.txt"))
        assertEquals(1, result.filesExtracted)
        assertEquals(2, result.entriesSkipped)
        assertEquals(0, result.entriesOverwritten)
        assertEquals(0, result.entriesAutoRenamed)
        assertEquals(1, progress.last().completedEntries)
        assertEquals(1, progress.last().totalEntries)
        assertEquals("first".toByteArray().size.toLong(), progress.last().bytesWritten)
        assertEquals("first".toByteArray().size.toLong(), progress.last().totalBytes)
    }

    @Test
    fun `overwrite reuses only a compatible node created by this extraction`() {
        val source = archive(
            FixtureEntry("A.txt", "first".toByteArray(), ZipEntry.STORED),
            FixtureEntry("a.txt", "second".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                rootName = "overwritten",
                writer = writer,
                conflictPolicy = ArchiveExtractionConflictPolicy.OVERWRITE,
            )
        }

        assertEquals(setOf("overwritten/A.txt"), writer.filePaths())
        assertArrayEquals("second".toByteArray(), writer.content("overwritten/A.txt"))
        assertEquals(2, result.filesExtracted)
        assertEquals(0, result.entriesSkipped)
        assertEquals(1, result.entriesOverwritten)
        assertEquals(0, result.entriesAutoRenamed)
    }

    @Test
    fun `ask applies one skip decision to all remaining compatible conflicts`() {
        val source = archive(
            FixtureEntry("A.txt", "first".toByteArray(), ZipEntry.STORED),
            FixtureEntry("a.txt", "second".toByteArray(), ZipEntry.STORED),
            FixtureEntry("A.TXT", "third".toByteArray(), ZipEntry.STORED),
            FixtureEntry("a.TXT", "fourth".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()
        var resolverCalls = 0

        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                rootName = "apply-all",
                writer = writer,
                conflictPolicy = ArchiveExtractionConflictPolicy.ASK,
                conflictResolver = ArchiveExtractionConflictResolver {
                    resolverCalls++
                    ArchiveExtractionConflictResolution(
                        decision = ArchiveExtractionConflictDecision.SKIP,
                        applyToAll = true,
                    )
                },
            )
        }

        assertEquals(1, resolverCalls)
        assertEquals(setOf("apply-all/A.txt"), writer.filePaths())
        assertEquals(3, result.entriesSkipped)
    }

    @Test
    fun `overwrite auto renames an incompatible file-directory conflict`() {
        val source = archive(
            FixtureEntry("node/child.txt", "child".toByteArray(), ZipEntry.STORED),
            FixtureEntry("Node", "file".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        val result = runBlocking {
            ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                rootName = "type-conflict",
                writer = writer,
                conflictPolicy = ArchiveExtractionConflictPolicy.OVERWRITE,
            )
        }

        assertEquals(
            setOf("type-conflict/node/child.txt", "type-conflict/Node (2)"),
            writer.filePaths(),
        )
        assertEquals(0, result.entriesOverwritten)
        assertEquals(1, result.entriesAutoRenamed)
    }

    @Test
    fun `commit failure is reported as output failure and rolls back the pending root`() {
        val source = archive(FixtureEntry("file.txt", "data".encodeToByteArray()))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter(failCommittingRoot = true)

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.OUTPUT_FAILURE) {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                    rootName = "commit-failure",
                    writer = writer,
                )
            }
        }

        assertTrue(writer.rootDeletionAttempted)
        assertTrue(writer.rootDeleted)
        assertFalse(writer.rootCommitted)
    }

    @Test
    fun `progress failure after commit never removes committed output`() {
        val source = archive(FixtureEntry("file.txt", "data".encodeToByteArray()))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        runCatching {
            runBlocking {
                ArchiveExtractor().extractToWriter(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                    rootName = "committed",
                    writer = writer,
                    progress = ArchiveProgressListener { update ->
                        if (update.phase == ExtractionPhase.COMPLETED) {
                            throw IOException("Synthetic UI callback failure")
                        }
                    },
                )
            }
        }

        assertTrue(writer.rootCommitted)
        assertFalse(writer.rootDeletionAttempted)
        assertFalse(writer.rootDeleted)
    }

    private fun archive(vararg entries: FixtureEntry): File =
        writeZip(temporaryFolder.newFile("archive-${temporaryFolder.root.list().orEmpty().size}.zip"), *entries)

    private fun copyFixture(name: String): File {
        val target = temporaryFolder.newFile(name)
        val resource = requireNotNull(javaClass.classLoader?.getResourceAsStream("archive-fixtures/$name"))
        resource.use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    private class FakeArchiveOutputWriter(
        private val failCreatingFile: String? = null,
        private val failDeletingRoot: Boolean = false,
        private val failCommittingRoot: Boolean = false,
    ) : ArchiveOutputWriter {
        var root: FakeNode? = null
            private set
        var rootDeleted = false
            private set
        var rootDeletionAttempted = false
            private set
        var rootCommitted = false
            private set
        private val files = HashMap<String, ByteArrayOutputStream>()
        private val nodes = LinkedHashMap<String, FakeNode>()

        override fun createRoot(displayName: String): ArchiveOutputWriter.Node =
            FakeNode(displayName, displayName, isDirectory = true, parentPath = null).also {
                root = it
                nodes[it.path] = it
            }

        override fun createDirectory(
            parent: ArchiveOutputWriter.Node,
            displayName: String,
        ): ArchiveOutputWriter.Node {
            val fakeParent = parent as FakeNode
            return createNode(fakeParent, displayName, isDirectory = true)
        }

        override fun createFile(
            parent: ArchiveOutputWriter.Node,
            displayName: String,
        ): ArchiveOutputWriter.Node {
            if (displayName == failCreatingFile) throw IOException("Synthetic provider failure")
            val fakeParent = parent as FakeNode
            return createNode(fakeParent, displayName, isDirectory = false)
        }

        override fun findChild(
            parent: ArchiveOutputWriter.Node,
            displayName: String,
        ): ArchiveOutputWriter.Node? {
            val fakeParent = parent as FakeNode
            val collisionKey = ArchivePathPolicy.destinationCollisionKey(displayName)
            return nodes.values.firstOrNull {
                it.parentPath == fakeParent.path &&
                    ArchivePathPolicy.destinationCollisionKey(it.name) == collisionKey
            }
        }

        override fun canOverwrite(
            node: ArchiveOutputWriter.Node,
            incomingIsDirectory: Boolean,
        ): Boolean {
            val fakeNode = node as FakeNode
            return nodes[fakeNode.path] === fakeNode && fakeNode.isDirectory == incomingIsDirectory
        }

        override fun openFile(node: ArchiveOutputWriter.Node): OutputStream {
            val fakeNode = node as FakeNode
            return ByteArrayOutputStream().also { files[fakeNode.path] = it }
        }

        override fun commitRoot(root: ArchiveOutputWriter.Node): ArchiveOutputWriter.Node {
            check(root === this.root)
            if (failCommittingRoot) throw IOException("Synthetic commit failure")
            rootCommitted = true
            return root
        }

        override fun deleteRoot(root: ArchiveOutputWriter.Node) {
            check(root === this.root)
            rootDeletionAttempted = true
            if (failDeletingRoot) throw IOException("Synthetic cleanup failure")
            rootDeleted = true
        }

        fun content(path: String): ByteArray = files.getValue(path).toByteArray()

        fun filePaths(): Set<String> = files.keys.toSet()

        private fun createNode(
            parent: FakeNode,
            displayName: String,
            isDirectory: Boolean,
        ): FakeNode {
            check(findChild(parent, displayName) == null) { "Synthetic destination conflict" }
            val path = "${parent.path}/$displayName"
            return FakeNode(path, displayName, isDirectory, parent.path).also { nodes[path] = it }
        }

        data class FakeNode(
            val path: String,
            val name: String,
            override val isDirectory: Boolean,
            val parentPath: String?,
        ) : ArchiveOutputWriter.Node {
            override val location = ArchiveOutputLocation(path, name)
        }
    }
}
