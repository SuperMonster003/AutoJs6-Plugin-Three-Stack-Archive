@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class ZipArchiveCreatorInstrumentationTest {

    @Test
    fun mixedTargetsDirectoriesAndEmptyDirectoriesProduceAReadableZip() {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val session = FakeHostSession(cacheDirectory)
        val request = ArchiveCompressionRequest(
            requestId = UUID.randomUUID().toString(),
            parentUri = Uri.parse("content://host/root"),
            parentDisplayPath = "/storage/emulated/0/Documents",
            targets = listOf(
                target("alpha", "alpha.txt", ExplorerActionValues.TARGET_FILE, 5L),
                target("folder", "folder", ExplorerActionValues.TARGET_DIRECTORY, -1L),
            ),
            hostSession = session,
        )

        val result = ArchiveEngine.DEFAULT.createWriter(ArchiveFormat.ZIP, session).create(
            request = request,
            options = ArchiveCreationOptions("Documents.zip", 6),
            checkCancelled = {},
            progress = ArchiveCreationProgressListener {},
        )

        assertEquals(2L, result.filesCompressed)
        assertEquals(2L, result.directoriesAdded)
        assertEquals(10L, result.sourceBytesRead)
        ZipFile(session.outputFile).use { archive ->
            assertEquals(
                setOf("alpha.txt", "folder/", "folder/child.txt", "folder/empty/"),
                archive.entries().asSequence().map { it.name }.toSet(),
            )
            assertArrayEquals(
                "alpha".toByteArray(),
                archive.getInputStream(archive.getEntry("alpha.txt")).use { it.readBytes() },
            )
            assertArrayEquals(
                "child".toByteArray(),
                archive.getInputStream(archive.getEntry("folder/child.txt")).use { it.readBytes() },
            )
        }
        assertTrue(session.committed)
        session.cleanup()
    }

    private fun target(id: String, name: String, kind: Int, size: Long) = ArchiveCompressionTarget(
        id = id,
        uri = Uri.parse("content://host/$id"),
        displayName = name,
        kind = kind,
        mimeType = if (kind == ExplorerActionValues.TARGET_DIRECTORY) {
            "inode/directory"
        } else {
            "text/plain"
        },
        size = size,
        lastModified = 1_700_000_000_000L,
    )

    private class FakeHostSession(cacheDirectory: File) : IExplorerActionHostSession.Stub() {

        private val transactionId = UUID.randomUUID().toString()
        private val sourceFiles = mapOf(
            "alpha:" to cacheDirectory.resolve("source-${UUID.randomUUID()}-alpha.txt").apply {
                writeText("alpha")
            },
            "folder:child.txt" to cacheDirectory.resolve("source-${UUID.randomUUID()}-child.txt").apply {
                writeText("child")
            },
        )
        val outputFile = cacheDirectory.resolve("output-${UUID.randomUUID()}.zip")
        var committed = false
            private set

        override fun listChildren(targetId: String, relativePath: String, offset: Int, limit: Int): Bundle {
            val allItems = when (targetId to relativePath) {
                "folder" to "" -> listOf(
                    item("child.txt", "child.txt", ExplorerActionValues.TARGET_FILE, 5L),
                    item("empty", "empty", ExplorerActionValues.TARGET_DIRECTORY, -1L),
                )
                "folder" to "empty" -> emptyList()
                else -> error("Unexpected directory request: $targetId:$relativePath")
            }
            val page = allItems.drop(offset).take(limit)
            return Bundle().apply {
                putParcelableArrayList(ExplorerActionHostSessionKeys.ITEMS, ArrayList(page))
                putInt(ExplorerActionHostSessionKeys.NEXT_OFFSET, offset + page.size)
                putBoolean(ExplorerActionHostSessionKeys.COMPLETE, offset + page.size >= allItems.size)
            }
        }

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            val file = sourceFiles["$targetId:$relativePath"]
                ?: error("Unexpected file request: $targetId:$relativePath")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun prepareOutput(displayName: String, mimeType: String, conflictPolicy: Int): Bundle {
            assertEquals("Documents.zip", displayName)
            return outputBundle()
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            assertEquals(this.transactionId, transactionId)
            return ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun commitOutput(transactionId: String): Bundle {
            assertEquals(this.transactionId, transactionId)
            committed = true
            return outputBundle()
        }

        override fun abortOutput(transactionId: String) {
            outputFile.delete()
        }

        override fun close() = Unit

        fun cleanup() {
            sourceFiles.values.forEach(File::delete)
            outputFile.delete()
        }

        private fun item(relativePath: String, displayName: String, kind: Int, size: Long) =
            Bundle().apply {
                putString(ExplorerActionHostSessionKeys.RELATIVE_PATH, relativePath)
                putString(ExplorerActionHostSessionKeys.DISPLAY_NAME, displayName)
                putInt(ExplorerActionHostSessionKeys.KIND, kind)
                putString(
                    ExplorerActionHostSessionKeys.MIME_TYPE,
                    if (kind == ExplorerActionValues.TARGET_DIRECTORY) "inode/directory" else "text/plain",
                )
                putLong(ExplorerActionHostSessionKeys.SIZE, size)
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, 1_700_000_000_000L)
                putBoolean(ExplorerActionHostSessionKeys.READABLE, true)
                putBoolean(ExplorerActionHostSessionKeys.SYMBOLIC_LINK, false)
            }

        private fun outputBundle() = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, "Documents.zip")
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, outputFile.path)
        }
    }
}
