package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HostArchiveOutputWriterInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun writesNestedOutputAndCommitsTheHostReservedRoot() {
        val directory = newTestDirectory()
        val remote = DirectoryOutputHost(directory, committedDisplayName = "archive (2)")
        val writer = HostArchiveOutputWriter(ExplorerActionHostSessionClient(remote))

        val root = writer.createRoot("archive")
        val docs = writer.createDirectory(root, "Docs")
        val note = writer.createFile(docs, "note.txt")
        writer.openFile(note).use { it.write("host output".encodeToByteArray()) }

        assertEquals(docs, writer.findChild(root, "docs"))
        assertFalse(writer.canOverwrite(note, incomingIsDirectory = false))
        val committed = writer.commitRoot(root)

        assertEquals("archive (2)", committed.location.displayName)
        assertTrue(committed.location.identifier.endsWith("archive (2)"))
        assertArrayEquals(
            "host output".encodeToByteArray(),
            File(remote.root, "Docs/note.txt").readBytes(),
        )
        assertEquals(1, remote.commitCalls)
        assertEquals(0, remote.abortCalls)
        assertThrows(IOException::class.java) { writer.deleteRoot(root) }
        directory.deleteRecursively()
    }

    @Test
    fun confirmsCommitByQueryWhenTheCommitResponseIsLost() {
        val directory = newTestDirectory()
        val remote = DirectoryOutputHost(directory, failAfterCommit = true)
        val writer = HostArchiveOutputWriter(ExplorerActionHostSessionClient(remote))
        val root = writer.createRoot("archive")
        val file = writer.createFile(root, "file.txt")
        writer.openFile(file).use { it.write(byteArrayOf(1, 2, 3)) }

        val committed = writer.commitRoot(root)

        assertEquals("archive", committed.location.displayName)
        assertEquals(1, remote.commitCalls)
        assertEquals(1, remote.queryCalls)
        assertEquals(0, remote.abortCalls)
        directory.deleteRecursively()
    }

    @Test
    fun abortRemovesTheEntirePendingHostTree() {
        val directory = newTestDirectory()
        val remote = DirectoryOutputHost(directory)
        val writer = HostArchiveOutputWriter(ExplorerActionHostSessionClient(remote))
        val root = writer.createRoot("archive")
        val file = writer.createFile(root, "file.txt")
        writer.openFile(file).use { it.write(byteArrayOf(7)) }

        writer.deleteRoot(root)
        writer.deleteRoot(root)

        assertFalse(remote.root.exists())
        assertEquals(1, remote.abortCalls)
        directory.deleteRecursively()
    }

    @Test
    fun recoveryAbortsOnlyIncompleteTransactions() {
        val directory = newTestDirectory()
        val remote = DirectoryOutputHost(directory)
        remote.listedStates += ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
        remote.listedStates += ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED
        remote.listedStates += ExplorerActionHostSessionValues.OUTPUT_STATE_ABORTED
        val client = ExplorerActionHostSessionClient(remote)

        assertEquals(1, client.abortIncompleteOutputs())
        assertEquals(1, remote.abortCalls)
        directory.deleteRecursively()
    }

    private fun newTestDirectory(): File = File(
        context.cacheDir,
        "host-output-writer-${UUID.randomUUID()}",
    ).also { check(it.mkdirs()) }

    private class DirectoryOutputHost(
        private val parent: File,
        private val committedDisplayName: String = "archive",
        private val failAfterCommit: Boolean = false,
    ) : UnusedTestExplorerActionHostSession() {
        private var transactionId = UUID.randomUUID().toString()
        private var state = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
        val root: File
            get() = File(parent, committedDisplayName)
        var commitCalls = 0
        var queryCalls = 0
        var abortCalls = 0
        val listedStates = mutableListOf<Int>()

        override fun prepareOutputTree(displayName: String, conflictPolicy: Int): Bundle {
            assertEquals(
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
                conflictPolicy,
            )
            check(root.mkdir())
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
            return outputBundle(transactionId, state)
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
            val file = File(root, relativePath)
            check(file.parentFile?.isDirectory == true)
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING
            return ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun commitOutput(transactionId: String): Bundle {
            requireActive(transactionId)
            commitCalls++
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED
            val result = outputBundle(transactionId, state)
            if (failAfterCommit) throw IOException("Synthetic lost commit response")
            return result
        }

        override fun queryOutput(transactionId: String): Bundle {
            require(transactionId == this.transactionId)
            queryCalls++
            return outputBundle(transactionId, state)
        }

        override fun listOutputs(): Bundle = Bundle().apply {
            putParcelableArrayList(
                ExplorerActionHostSessionKeys.OUTPUTS,
                ArrayList(
                    listedStates.mapIndexed { index, listedState ->
                        outputBundle(
                            id = UUID.nameUUIDFromBytes("listed-$index".encodeToByteArray()).toString(),
                            outputState = listedState,
                        )
                    },
                ),
            )
        }

        override fun abortOutput(transactionId: String) {
            if (listedStates.isNotEmpty() && transactionId != this.transactionId) {
                abortCalls++
                return
            }
            requireActive(transactionId)
            root.deleteRecursively()
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_ABORTED
            abortCalls++
        }

        private fun requireActive(value: String) {
            require(value == transactionId)
            check(
                state == ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED ||
                    state == ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING,
            )
        }

        private fun outputBundle(id: String, outputState: Int): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, id)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, committedDisplayName)
            putString(
                ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH,
                File(parent, committedDisplayName).absolutePath,
            )
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_KIND,
                ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE,
            )
            putInt(ExplorerActionHostSessionKeys.OUTPUT_STATE, outputState)
            putInt(ExplorerActionHostSessionKeys.OUTPUT_FILE_COUNT, 1)
            putInt(ExplorerActionHostSessionKeys.OUTPUT_DIRECTORY_COUNT, 1)
            putLong(
                ExplorerActionHostSessionKeys.OUTPUT_BYTES,
                root.walkTopDown().filter(File::isFile).sumOf(File::length),
            )
        }
    }
}
