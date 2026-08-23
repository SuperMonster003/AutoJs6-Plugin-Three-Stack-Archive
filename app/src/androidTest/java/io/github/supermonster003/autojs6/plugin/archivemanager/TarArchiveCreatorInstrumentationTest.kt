@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class TarArchiveCreatorInstrumentationTest {

    @Test
    fun everyTarContainerCreatesAReadableUnicodeArchiveThroughTheHostTransaction() {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val cases = listOf(
            ArchiveFormat.TAR to 0,
            ArchiveFormat.TAR_GZIP to 6,
            ArchiveFormat.TAR_XZ to 1,
            ArchiveFormat.TAR_BZIP2 to 6,
            ArchiveFormat.TAR_ZSTD to 9,
        )

        cases.forEach { (format, compressionLevel) ->
            val session = FakeHostSession(cacheDirectory, format)
            try {
                val result = ArchiveEngine.DEFAULT.createWriter(format, session).create(
                    request = request(session),
                    options = ArchiveCreationOptions(
                        outputDisplayName = session.outputDisplayName,
                        compressionLevel = compressionLevel,
                    ),
                    checkCancelled = {},
                    progress = ArchiveCreationProgressListener {},
                )

                assertEquals(format.displayName, 2L, result.filesCompressed)
                assertEquals(format.displayName, 2L, result.directoriesAdded)
                assertEquals(format.displayName, 10L, result.sourceBytesRead)
                assertEquals(format.displayName, 2, session.openCount("document:"))
                assertTrue(format.displayName, session.committed)
                assertFalse(format.displayName, session.aborted)

                val snapshot = ArchiveScanner().scan(session.outputFile)
                assertEquals(format.displayName, format, snapshot.format)
                assertEquals(
                    format.displayName,
                    setOf("文档.txt", "folder", "folder/child.txt", "folder/空目录"),
                    snapshot.entries.map(ArchiveEntry::path).toSet(),
                )
                assertArrayEquals(
                    format.displayName,
                    "alpha".toByteArray(),
                    readEntry(session.outputFile, snapshot, "文档.txt"),
                )
                assertArrayEquals(
                    format.displayName,
                    "child".toByteArray(),
                    readEntry(session.outputFile, snapshot, "folder/child.txt"),
                )
            } finally {
                session.cleanup()
            }
        }
    }

    @Test
    fun cancellationAbortsThePendingTarOutputWithoutPublishingIt() {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val session = FakeHostSession(cacheDirectory, ArchiveFormat.TAR_ZSTD)
        var cancellationChecks = 0

        try {
            org.junit.Assert.assertThrows(CancellationException::class.java) {
                ArchiveEngine.DEFAULT.createWriter(ArchiveFormat.TAR_ZSTD, session).create(
                    request = request(session),
                    options = ArchiveCreationOptions(session.outputDisplayName, 6),
                    checkCancelled = {
                        cancellationChecks++
                        if (cancellationChecks >= 4) throw CancellationException("test cancellation")
                    },
                    progress = ArchiveCreationProgressListener {},
                )
            }

            assertFalse(session.committed)
            assertTrue(session.aborted)
            assertFalse(session.outputFile.exists())
        } finally {
            session.cleanup()
        }
    }

    @Test
    fun changedSourceSizeAbortsThePendingTarOutputWithoutPublishingIt() {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val session = FakeHostSession(cacheDirectory, ArchiveFormat.TAR)

        try {
            org.junit.Assert.assertThrows(IOException::class.java) {
                ArchiveEngine.DEFAULT.createWriter(ArchiveFormat.TAR, session).create(
                    request = request(session, documentSize = 4L),
                    options = ArchiveCreationOptions(session.outputDisplayName, 0),
                    checkCancelled = {},
                    progress = ArchiveCreationProgressListener {},
                )
            }

            assertFalse(session.committed)
            assertTrue(session.aborted)
            assertFalse(session.outputFile.exists())
        } finally {
            session.cleanup()
        }
    }

    private fun readEntry(
        source: File,
        snapshot: ArchiveSnapshot,
        path: String,
    ): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(
            snapshot.entries.single { it.path == path },
            output,
        )
        return output.toByteArray()
    }

    private fun request(
        session: IExplorerActionHostSession,
        documentSize: Long = -1L,
    ) = ArchiveCompressionRequest(
        requestId = UUID.randomUUID().toString(),
        parentUri = Uri.parse("content://host/root"),
        parentDisplayPath = "/storage/emulated/0/Documents",
        targets = listOf(
            target("document", "文档.txt", ExplorerActionValues.TARGET_FILE, documentSize),
            target("folder", "folder", ExplorerActionValues.TARGET_DIRECTORY, -1L),
        ),
        hostSession = session,
    )

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

    private class FakeHostSession(
        cacheDirectory: File,
        private val format: ArchiveFormat,
    ) : IExplorerActionHostSession.Stub() {

        private val transactionId = UUID.randomUUID().toString()
        private val sourceFiles = mapOf(
            "document:" to cacheDirectory.resolve("source-${UUID.randomUUID()}-document.txt").apply {
                writeText("alpha")
            },
            "folder:child.txt" to cacheDirectory.resolve("source-${UUID.randomUUID()}-child.txt").apply {
                writeText("child")
            },
        )
        private val openCounts = HashMap<String, Int>()
        val outputDisplayName = "Documents.${format.primaryExtension}"
        val outputFile = cacheDirectory.resolve("output-${UUID.randomUUID()}.${format.primaryExtension}")
        var committed = false
            private set
        var aborted = false
            private set

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle {
            val allItems = when (targetId to relativePath) {
                "folder" to "" -> listOf(
                    item("child.txt", "child.txt", ExplorerActionValues.TARGET_FILE, 5L),
                    item("空目录", "空目录", ExplorerActionValues.TARGET_DIRECTORY, -1L),
                )
                "folder" to "空目录" -> emptyList()
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
            val key = "$targetId:$relativePath"
            openCounts[key] = openCounts.getOrDefault(key, 0) + 1
            val file = sourceFiles[key] ?: error("Unexpected file request: $key")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle {
            assertEquals(outputDisplayName, displayName)
            assertEquals(format.primaryMimeType, mimeType)
            assertEquals(ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME, conflictPolicy)
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
            assertEquals(this.transactionId, transactionId)
            aborted = true
            outputFile.delete()
        }

        override fun close() = Unit

        fun openCount(key: String): Int = openCounts.getOrDefault(key, 0)

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
                    if (kind == ExplorerActionValues.TARGET_DIRECTORY) {
                        "inode/directory"
                    } else {
                        "text/plain"
                    },
                )
                putLong(ExplorerActionHostSessionKeys.SIZE, size)
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, 1_700_000_000_000L)
                putBoolean(ExplorerActionHostSessionKeys.READABLE, true)
                putBoolean(ExplorerActionHostSessionKeys.SYMBOLIC_LINK, false)
            }

        private fun outputBundle() = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, outputDisplayName)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, outputFile.path)
        }
    }
}
