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
        assertEquals(ExtractionPhase.PREPARING, progress.first().phase)
        assertEquals(ExtractionPhase.COMPLETED, progress.last().phase)
        assertEquals(10L, progress.last().bytesWritten)
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
    fun `cleans the newly created root after cancellation`() {
        val source = archive(FixtureEntry("large.bin", ByteArray(100_000) { 7 }, ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        val writer = FakeArchiveOutputWriter()

        try {
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
        } catch (_: CancellationException) {
            // Expected.
        }

        assertNotNull(writer.root)
        assertTrue(writer.rootDeleted)
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
    ) : ArchiveOutputWriter {
        var root: FakeNode? = null
            private set
        var rootDeleted = false
            private set
        private val files = HashMap<String, ByteArrayOutputStream>()

        override fun createRoot(displayName: String): ArchiveOutputWriter.Node =
            FakeNode(displayName, displayName).also { root = it }

        override fun createDirectory(
            parent: ArchiveOutputWriter.Node,
            displayName: String,
        ): ArchiveOutputWriter.Node {
            val fakeParent = parent as FakeNode
            return FakeNode("${fakeParent.path}/$displayName", displayName)
        }

        override fun createFile(
            parent: ArchiveOutputWriter.Node,
            displayName: String,
        ): ArchiveOutputWriter.Node {
            if (displayName == failCreatingFile) throw IOException("Synthetic provider failure")
            val fakeParent = parent as FakeNode
            return FakeNode("${fakeParent.path}/$displayName", displayName)
        }

        override fun openFile(node: ArchiveOutputWriter.Node): OutputStream {
            val fakeNode = node as FakeNode
            return ByteArrayOutputStream().also { files[fakeNode.path] = it }
        }

        override fun deleteRoot(root: ArchiveOutputWriter.Node) {
            check(root === this.root)
            rootDeleted = true
        }

        fun content(path: String): ByteArray = files.getValue(path).toByteArray()

        data class FakeNode(
            val path: String,
            val name: String,
        ) : ArchiveOutputWriter.Node {
            override val location = ArchiveOutputLocation(path, name)
        }
    }
}
