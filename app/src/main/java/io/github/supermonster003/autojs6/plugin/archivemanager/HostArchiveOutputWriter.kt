package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import java.io.IOException
import java.io.OutputStream

/** Writes one rollback-safe sibling directory through an Explorer Action v9 host session. */
internal class HostArchiveOutputWriter(
    private val session: ExplorerActionHostSessionClient,
) : ArchiveOutputWriter {

    private var transaction: HostOutputTransaction? = null
    private var rootNode: HostNode? = null
    private val children = LinkedHashMap<String, HostNode>()
    private var committed = false
    private var aborted = false

    override fun createRoot(displayName: String): ArchiveOutputWriter.Node {
        check(transaction == null) { "Host output root has already been prepared" }
        val prepared = session.prepareOutputTree(displayName)
        val root = HostNode(
            transactionId = prepared.id,
            relativePath = "",
            location = ArchiveOutputLocation(
                identifier = prepared.displayPath,
                displayName = prepared.displayName,
            ),
            isDirectory = true,
        )
        transaction = prepared
        rootNode = root
        return root
    }

    override fun createDirectory(
        parent: ArchiveOutputWriter.Node,
        displayName: String,
    ): ArchiveOutputWriter.Node {
        val parentNode = requireNode(parent, requireDirectory = true)
        val child = childNode(parentNode, displayName, isDirectory = true)
        registerChild(parentNode, child)
        session.createOutputDirectory(child.transactionId, child.relativePath)
        return child
    }

    override fun createFile(
        parent: ArchiveOutputWriter.Node,
        displayName: String,
    ): ArchiveOutputWriter.Node {
        val parentNode = requireNode(parent, requireDirectory = true)
        val child = childNode(parentNode, displayName, isDirectory = false)
        registerChild(parentNode, child)
        return child
    }

    override fun findChild(
        parent: ArchiveOutputWriter.Node,
        displayName: String,
    ): ArchiveOutputWriter.Node? {
        val parentNode = requireNode(parent, requireDirectory = true)
        return children[childKey(parentNode.relativePath, displayName)]
    }

    override fun canOverwrite(
        node: ArchiveOutputWriter.Node,
        incomingIsDirectory: Boolean,
    ): Boolean {
        requireNode(node, requireDirectory = false)
        return false
    }

    override fun openFile(node: ArchiveOutputWriter.Node): OutputStream {
        val hostNode = requireNode(node, requireDirectory = false)
        check(!hostNode.isDirectory) { "Host output directory cannot be opened as a file" }
        check(!hostNode.opened) { "Host output file has already been opened" }
        val descriptor = session.openOutputFile(hostNode.transactionId, hostNode.relativePath)
        hostNode.opened = true
        return ParcelFileDescriptor.AutoCloseOutputStream(descriptor)
    }

    override fun commitRoot(root: ArchiveOutputWriter.Node): ArchiveOutputWriter.Node {
        val hostRoot = requireNode(root, requireDirectory = true)
        check(hostRoot.relativePath.isEmpty()) { "Host output commit requires the root node" }
        check(!aborted) { "Host output tree has already been aborted" }
        val result = try {
            session.commitOutputTree(hostRoot.transactionId)
        } catch (commitError: Throwable) {
            val queried = runCatching { session.queryOutput(hostRoot.transactionId) }
                .onFailure(commitError::addSuppressed)
                .getOrNull()
            if (queried?.state == ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED) {
                queried
            } else {
                throw commitError
            }
        }
        check(result.state == ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED) {
            "Host did not confirm the directory output commit"
        }
        committed = true
        return hostRoot.copy(
            location = ArchiveOutputLocation(
                identifier = result.displayPath,
                displayName = result.displayName,
            ),
        )
    }

    override fun deleteRoot(root: ArchiveOutputWriter.Node) {
        val hostRoot = requireNode(root, requireDirectory = true)
        check(hostRoot.relativePath.isEmpty()) { "Host output cleanup requires the root node" }
        if (aborted) return
        if (committed) {
            throw IOException("Committed host output cannot be rolled back")
        }
        session.abortOutput(hostRoot.transactionId)
        aborted = true
    }

    private fun childNode(
        parent: HostNode,
        displayName: String,
        isDirectory: Boolean,
    ): HostNode {
        check(!committed && !aborted) { "Host output tree is no longer writable" }
        val relativePath = if (parent.relativePath.isEmpty()) {
            displayName
        } else {
            "${parent.relativePath}/$displayName"
        }
        val root = requireNotNull(rootNode)
        return HostNode(
            transactionId = root.transactionId,
            relativePath = relativePath,
            location = ArchiveOutputLocation(
                identifier = "${root.location.identifier}/$relativePath",
                displayName = displayName,
            ),
            isDirectory = isDirectory,
        )
    }

    private fun registerChild(parent: HostNode, child: HostNode) {
        val key = childKey(parent.relativePath, child.location.displayName)
        check(children.putIfAbsent(key, child) == null) {
            "Host output tree child already exists"
        }
    }

    private fun childKey(parentPath: String, displayName: String): String =
        "$parentPath\u0000${ArchivePathPolicy.destinationCollisionKey(displayName)}"

    private fun requireNode(
        node: ArchiveOutputWriter.Node,
        requireDirectory: Boolean,
    ): HostNode {
        val hostNode = node as? HostNode
            ?: throw IOException("Output node does not belong to this host writer")
        val expectedTransaction = transaction?.id
            ?: throw IOException("Host output root has not been prepared")
        if (hostNode.transactionId != expectedTransaction) {
            throw IOException("Output node belongs to another host transaction")
        }
        if (requireDirectory && !hostNode.isDirectory) {
            throw IOException("Host output parent is not a directory")
        }
        return hostNode
    }

    private data class HostNode(
        val transactionId: String,
        val relativePath: String,
        override val location: ArchiveOutputLocation,
        override val isDirectory: Boolean,
        var opened: Boolean = false,
    ) : ArchiveOutputWriter.Node
}
