package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOperationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ExplorerArchiveExtractionInstrumentationTest {

    @Test
    fun extractsAnOpaqueDirectorySelectionIntoTheHostOutputTree() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-extract-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-extract-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "source.zip")
        val expected = "native host extraction".encodeToByteArray()
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("folder/"))
            output.closeEntry()
            output.putNextEntry(ZipEntry("folder/nested.txt"))
            output.write(expected)
            output.closeEntry()
            output.putNextEntry(ZipEntry("other.txt"))
            output.write("not selected".encodeToByteArray())
            output.closeEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "native.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        val folder = session.listChildren(
            "root",
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
            .orEmpty()
            .single { it.getString(ExplorerArchiveSessionKeys.NAME) == "folder" }
        val operationId = UUID.randomUUID().toString()
        val terminal = CountDownLatch(1)
        val completed = AtomicReference<Bundle?>()
        val failed = AtomicReference<Bundle?>()
        val callback = object : IExplorerArchiveOperationCallback.Stub() {
            override fun onProgress(update: Bundle) = Unit

            override fun onCompleted(result: Bundle) {
                completed.set(result)
                terminal.countDown()
            }

            override fun onFailed(failure: Bundle) {
                failed.set(failure)
                terminal.countDown()
            }
        }
        val host = DirectoryOutputHost(outputDirectory)
        session.extractEntries(
            Bundle().apply {
                putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                putStringArrayList(
                    ExplorerArchiveOperationKeys.ENTRY_IDS,
                    arrayListOf(requireNotNull(folder.getString(ExplorerArchiveSessionKeys.ID))),
                )
            },
            host,
            callback,
        )

        assertTrue("Timed out waiting for archive extraction", terminal.await(30, TimeUnit.SECONDS))
        assertNull(failed.get())
        val result = requireNotNull(completed.get())
        assertEquals(operationId, result.getString(ExplorerArchiveOperationKeys.OPERATION_ID))
        assertEquals("native", result.getString(ExplorerArchiveOperationKeys.OUTPUT_DISPLAY_NAME))
        assertEquals(1, result.getInt(ExplorerArchiveOperationKeys.FILES_EXTRACTED))
        assertArrayEquals(expected, File(host.root, "folder/nested.txt").readBytes())
        assertTrue(!File(host.root, "other.txt").exists())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    @Test
    fun cancellationBeforeOutputCreationReportsCancelledAndPublishesNothing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-cancel-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-cancel-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "source.zip")
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("payload.txt"))
            output.write(ByteArray(32 * 1_024) { it.toByte() })
            output.closeEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "cancel.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        val entry = session.listChildren(
            "root",
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
            .orEmpty()
            .single { it.getString(ExplorerArchiveSessionKeys.NAME) == "payload.txt" }
        val operationId = UUID.randomUUID().toString()
        val terminal = CountDownLatch(1)
        val cancellationRequested = AtomicBoolean(false)
        val completed = AtomicReference<Bundle?>()
        val failed = AtomicReference<Bundle?>()
        val callback = object : IExplorerArchiveOperationCallback.Stub() {
            override fun onProgress(update: Bundle) {
                if (cancellationRequested.compareAndSet(false, true)) {
                    session.cancelExtraction(operationId)
                }
            }

            override fun onCompleted(result: Bundle) {
                completed.set(result)
                terminal.countDown()
            }

            override fun onFailed(failure: Bundle) {
                failed.set(failure)
                terminal.countDown()
            }
        }
        val host = DirectoryOutputHost(outputDirectory, "cancel")
        session.extractEntries(
            Bundle().apply {
                putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                putStringArrayList(
                    ExplorerArchiveOperationKeys.ENTRY_IDS,
                    arrayListOf(requireNotNull(entry.getString(ExplorerArchiveSessionKeys.ID))),
                )
            },
            host,
            callback,
        )

        assertTrue("Timed out waiting for archive cancellation", terminal.await(30, TimeUnit.SECONDS))
        assertNull(completed.get())
        val failure = requireNotNull(failed.get())
        assertEquals(operationId, failure.getString(ExplorerArchiveOperationKeys.OPERATION_ID))
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_ERROR_CANCELLED,
            failure.getInt(ExplorerArchiveOperationKeys.ERROR_CODE),
        )
        assertTrue(!host.root.exists())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    private class DirectoryOutputHost(
        private val parent: File,
        rootName: String = "native",
    ) : UnusedTestExplorerActionHostSession() {
        private val transactionId = UUID.randomUUID().toString()
        private var state = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
        val root = File(parent, rootName)

        override fun prepareOutputTree(displayName: String, conflictPolicy: Int): Bundle {
            assertEquals(root.name, displayName)
            assertEquals(ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME, conflictPolicy)
            check(root.mkdir())
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
            return outputBundle(state)
        }

        override fun createOutputDirectory(transactionId: String, relativePath: String) {
            requireActive(transactionId)
            check(File(root, relativePath).mkdirs())
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING
        }

        override fun openOutputFile(
            transactionId: String,
            relativePath: String,
        ): ParcelFileDescriptor {
            requireActive(transactionId)
            val target = File(root, relativePath)
            check(target.parentFile?.isDirectory == true)
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING
            return ParcelFileDescriptor.open(
                target,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun commitOutput(transactionId: String): Bundle {
            requireActive(transactionId)
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED
            return outputBundle(state)
        }

        override fun queryOutput(transactionId: String): Bundle {
            require(transactionId == this.transactionId)
            return outputBundle(state)
        }

        override fun abortOutput(transactionId: String) {
            require(transactionId == this.transactionId)
            root.deleteRecursively()
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_ABORTED
        }

        override fun close() {
            if (state != ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED) {
                root.deleteRecursively()
            }
        }

        private fun requireActive(value: String) {
            require(value == transactionId)
            check(
                state == ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED ||
                    state == ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING,
            )
        }

        private fun outputBundle(outputState: Int): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, root.name)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, root.absolutePath)
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_KIND,
                ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE,
            )
            putInt(ExplorerActionHostSessionKeys.OUTPUT_STATE, outputState)
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_FILE_COUNT,
                root.walkTopDown().count(File::isFile),
            )
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_DIRECTORY_COUNT,
                root.walkTopDown().count(File::isDirectory).coerceAtLeast(1) - 1,
            )
            putLong(
                ExplorerActionHostSessionKeys.OUTPUT_BYTES,
                root.walkTopDown().filter(File::isFile).sumOf(File::length),
            )
        }
    }
}
