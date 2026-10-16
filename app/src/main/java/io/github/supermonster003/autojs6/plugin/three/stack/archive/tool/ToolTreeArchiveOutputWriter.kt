package io.github.supermonster003.autojs6.plugin.three.stack.archive.tool

import android.os.Bundle
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveOutputLocation
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveOutputWriter
import org.autojs.plugin.tool.api.IPluginToolOutputTree
import org.autojs.plugin.tool.api.PluginToolOutputTreeErrors
import org.autojs.plugin.tool.api.PluginToolOutputTreeKeys
import java.io.IOException
import java.io.OutputStream

/**
 * Writes an extraction into the directory output AutoJs6 stages for an `archive_extract` call (MCP Server roadmap D53):
 * the root is the output directory itself, so entries land there with their paths inside the archive. AutoJs6 commits
 * the tree when the call reports its result and drops it otherwise, so [commitRoot] and [deleteRoot] have nothing to
 * do. A file that already exists in the output directory (D56) is kept: its data is read and discarded (OutputStream
 * .nullOutputStream needs API 33) so that the extraction goes on, and its path is collected in [kept].
 */
internal class ToolTreeArchiveOutputWriter(private val tree: IPluginToolOutputTree) : ArchiveOutputWriter {

    private class TreeNode(val path: String, override val isDirectory: Boolean, override val location: ArchiveOutputLocation) : ArchiveOutputWriter.Node

    private val created = HashMap<String, TreeNode>()

    /** Paths that already existed in the output directory, in extraction order. */
    val kept = mutableListOf<String>()

    /** Bytes of the kept files that were read and dropped. */
    var discardedBytes = 0L
        private set

    private val discard = object : OutputStream() {
        override fun write(b: Int) {
            discardedBytes += 1
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            discardedBytes += len
        }
    }

    override fun createRoot(displayName: String): ArchiveOutputWriter.Node = TreeNode("", true, ArchiveOutputLocation("", displayName))

    override fun createDirectory(parent: ArchiveOutputWriter.Node, displayName: String): ArchiveOutputWriter.Node {
        val path = childPath(parent, displayName)
        val answer = tree.createDirectory(path)
        if (!answer.getBoolean(PluginToolOutputTreeKeys.OK)) throw IOException(reason(path, answer))
        return TreeNode(path, true, ArchiveOutputLocation(path, displayName)).also { created[path] = it }
    }

    override fun createFile(parent: ArchiveOutputWriter.Node, displayName: String): ArchiveOutputWriter.Node {
        val path = childPath(parent, displayName)
        return TreeNode(path, false, ArchiveOutputLocation(path, displayName)).also { created[path] = it }
    }

    override fun findChild(parent: ArchiveOutputWriter.Node, displayName: String): ArchiveOutputWriter.Node? = created[childPath(parent, displayName)]

    /** Nothing is replaced inside one extraction; AutoJs6 decides about files that existed before (overwrite option). */
    override fun canOverwrite(node: ArchiveOutputWriter.Node, incomingIsDirectory: Boolean): Boolean = false

    override fun openFile(node: ArchiveOutputWriter.Node): OutputStream {
        val path = (node as TreeNode).path
        val answer = Bundle()
        val descriptor = tree.openFile(path, -1L, answer)
        if (descriptor != null) return ParcelFileDescriptor.AutoCloseOutputStream(descriptor)
        if (answer.getString(PluginToolOutputTreeKeys.ERROR_CODE) == PluginToolOutputTreeErrors.EXISTS) {
            kept += path
            return discard
        }
        throw IOException(reason(path, answer))
    }

    override fun deleteRoot(root: ArchiveOutputWriter.Node) = Unit

    private fun childPath(parent: ArchiveOutputWriter.Node, displayName: String): String {
        val parentPath = (parent as TreeNode).path
        return if (parentPath.isEmpty()) displayName else "$parentPath/$displayName"
    }

    private fun reason(path: String, answer: Bundle): String =
        "AutoJs6 refused $path: ${answer.getString(PluginToolOutputTreeKeys.ERROR_CODE)} ${answer.getString(PluginToolOutputTreeKeys.ERROR_MESSAGE).orEmpty()}"
}
