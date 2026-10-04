package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class SafDirectoryTreeImporterInstrumentationTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver: ContentResolver
        get() = context.contentResolver

    @Before
    fun resetBeforeTest() = resetProvider()

    @After
    fun resetAfterTest() = resetProvider()

    @Test
    fun scansNestedFilesAndEmptyDirectoriesWithoutOpeningFileData() {
        val fixture = createFixture()
        var cancellationChecks = 0

        val entries = SafDirectoryTreeImporter(resolver).scan(fixture.treeUri) {
            cancellationChecks++
        }

        assertTrue(cancellationChecks > 1)
        assertEquals(
            listOf(
                "Bundle",
                "Bundle/Empty",
                "Bundle/Nested",
                "Bundle/note.txt",
                "Bundle/Nested/data.bin",
            ),
            entries.map(ArchiveMutationAddedTreeEntry::relativePath),
        )
        assertTrue(entries[1] is ArchiveMutationAddedTreeEntry.Directory)
        val note = entries[3] as ArchiveMutationAddedTreeEntry.FileEntry
        assertEquals(4L, note.file.size)
        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4),
            note.file.openInputStream().use { it.readBytes() },
        )
        val nested = entries[4] as ArchiveMutationAddedTreeEntry.FileEntry
        assertArrayEquals(
            "nested".encodeToByteArray(),
            nested.file.openInputStream().use { it.readBytes() },
        )
    }

    @Test
    fun enforcesEntryLimitBeforeReturningAnUnboundedTree() {
        val fixture = createFixture()

        val failure = runCatching {
            SafDirectoryTreeImporter(resolver, maxEntries = 2).scan(fixture.treeUri) { }
        }.exceptionOrNull()

        assertTrue(failure is ArchiveValidationException)
        assertEquals(
            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
            (failure as ArchiveValidationException).code,
        )
    }

    @Test
    fun observesCancellationDuringDirectoryTraversal() {
        val fixture = createFixture()
        var checks = 0

        val failure = runCatching {
            SafDirectoryTreeImporter(resolver).scan(fixture.treeUri) {
                checks++
                if (checks >= 4) throw CancellationException("Synthetic cancellation")
            }
        }.exceptionOrNull()

        assertTrue(failure is CancellationException)
    }

    private fun createFixture(): Fixture {
        val rootTreeUri = DocumentsContract.buildTreeDocumentUri(
            CollisionDocumentsProvider.AUTHORITY,
            CollisionDocumentsProvider.ROOT_ID,
        )
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            rootTreeUri,
            CollisionDocumentsProvider.ROOT_ID,
        )
        val bundleUri = requireNotNull(
            DocumentsContract.createDocument(
                resolver,
                rootDocumentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                "Bundle",
            ),
        )
        requireNotNull(
            DocumentsContract.createDocument(
                resolver,
                bundleUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                "Empty",
            ),
        )
        val nestedUri = requireNotNull(
            DocumentsContract.createDocument(
                resolver,
                bundleUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                "Nested",
            ),
        )
        createFile(bundleUri, "note.txt", byteArrayOf(1, 2, 3, 4))
        createFile(nestedUri, "data.bin", "nested".encodeToByteArray())
        val bundleId = DocumentsContract.getDocumentId(bundleUri)
        return Fixture(
            DocumentsContract.buildTreeDocumentUri(
                CollisionDocumentsProvider.AUTHORITY,
                bundleId,
            ),
        )
    }

    private fun createFile(parentUri: Uri, displayName: String, bytes: ByteArray) {
        val uri = requireNotNull(
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                "application/octet-stream",
                displayName,
            ),
        )
        requireNotNull(resolver.openOutputStream(uri, "w")).use { it.write(bytes) }
    }

    private fun resetProvider() {
        resolver.call(
            Uri.parse("content://${CollisionDocumentsProvider.AUTHORITY}"),
            CollisionDocumentsProvider.METHOD_RESET,
            null,
            null,
        )
    }

    private data class Fixture(val treeUri: Uri)
}
