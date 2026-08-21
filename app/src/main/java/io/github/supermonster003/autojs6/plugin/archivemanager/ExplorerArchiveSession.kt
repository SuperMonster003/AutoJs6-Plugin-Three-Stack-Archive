package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Binder
import android.os.Bundle
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerArchiveSession
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.Charsets.UTF_8

internal class ExplorerArchiveSession(
    private val ownerUid: Int,
    private val displayName: String,
    private val stagedArchive: StagedArchive,
    snapshot: ArchiveSnapshot,
    private val onClosed: (ExplorerArchiveSession) -> Unit,
) : IExplorerArchiveSession.Stub() {

    private data class SessionNode(
        val id: String,
        val parentId: String?,
        val node: ArchiveNode,
    )

    private val closed = AtomicBoolean(false)
    private val sessionId = UUID.randomUUID().toString()
    private val rootId = ROOT_ID
    private val childrenByParentId: Map<String, List<SessionNode>>

    init {
        val index = ArchiveIndex(snapshot)
        val nodesByPath = LinkedHashMap<String, SessionNode>()
        val mutableChildren = LinkedHashMap<String, MutableList<SessionNode>>()
        val rootNode = requireNotNull(index.node(ArchivePathPolicy.ROOT_PATH))
        nodesByPath[rootNode.path] = SessionNode(rootId, null, rootNode)

        val queue = ArrayDeque<ArchiveNode>()
        queue += rootNode
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            val parentSessionNode = nodesByPath.getValue(parent.path)
            val children = index.children(parent.path).map { child ->
                val sessionNode = SessionNode(
                    id = stableEntryId(child.path),
                    parentId = parentSessionNode.id,
                    node = child,
                )
                check(nodesByPath.put(child.path, sessionNode) == null) {
                    "Archive index contains a duplicate path"
                }
                if (child.isDirectory) queue += child
                sessionNode
            }
            mutableChildren[parentSessionNode.id] = children.toMutableList()
        }
        childrenByParentId = mutableChildren.mapValues { (_, value) -> value.toList() }
    }

    override fun getInfo(): Bundle {
        checkCaller()
        checkOpen()
        return Bundle().apply {
            putString(ExplorerArchiveSessionKeys.SESSION_ID, sessionId)
            putString(ExplorerArchiveSessionKeys.ROOT_ID, rootId)
            putString(ExplorerArchiveSessionKeys.DISPLAY_NAME, displayName)
            putLong(ExplorerArchiveSessionKeys.SOURCE_SIZE, stagedArchive.bytes)
            putLong(ExplorerArchiveSessionKeys.SOURCE_LAST_MODIFIED, stagedArchive.file.lastModified())
        }
    }

    override fun listChildren(parentId: String?, offset: Int, limit: Int): Bundle {
        checkCaller()
        checkOpen()
        require(parentId != null && parentId.length <= ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
            "Archive parent ID is invalid"
        }
        require(offset >= 0) { "Archive page offset is invalid" }
        require(limit in 1..ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE) {
            "Archive page limit is invalid"
        }
        val children = requireNotNull(childrenByParentId[parentId]) {
            "Archive directory does not exist"
        }
        require(offset <= children.size) { "Archive page offset is out of range" }
        val end = (offset.toLong() + limit).coerceAtMost(children.size.toLong()).toInt()
        val items = ArrayList<Bundle>(end - offset)
        children.subList(offset, end).forEach { child -> items += child.toBundle() }
        return Bundle().apply {
            putParcelableArrayList(ExplorerArchiveSessionKeys.ITEMS, items)
            putInt(ExplorerArchiveSessionKeys.NEXT_OFFSET, end)
            putBoolean(ExplorerArchiveSessionKeys.COMPLETE, end == children.size)
        }
    }

    override fun close() {
        checkCaller()
        closeInternal()
    }

    fun closeFromService() {
        closeInternal()
    }

    private fun closeInternal() {
        if (!closed.compareAndSet(false, true)) return
        stagedArchive.delete()
        onClosed(this)
    }

    private fun checkCaller() {
        check(Binder.getCallingUid() == ownerUid) { "Archive session caller changed" }
    }

    private fun checkOpen() {
        check(!closed.get()) { "Archive session is closed" }
    }

    private fun SessionNode.toBundle(): Bundle {
        val entry = node.entry
        return Bundle().apply {
            putString(ExplorerArchiveSessionKeys.ID, id)
            putString(ExplorerArchiveSessionKeys.PARENT_ID, parentId)
            putString(
                ExplorerArchiveSessionKeys.NAME,
                node.name.take(ExplorerActionProtocol.MAX_ARCHIVE_DISPLAY_NAME_LENGTH),
            )
            putInt(
                ExplorerArchiveSessionKeys.KIND,
                if (node.isDirectory) {
                    ExplorerArchiveSessionValues.KIND_DIRECTORY
                } else {
                    ExplorerArchiveSessionValues.KIND_FILE
                },
            )
            putLong(ExplorerArchiveSessionKeys.SIZE, node.uncompressedSize)
            putLong(ExplorerArchiveSessionKeys.COMPRESSED_SIZE, entry?.compressedSize ?: -1L)
            putLong(ExplorerArchiveSessionKeys.LAST_MODIFIED, entry?.modifiedTimeMillis ?: 0L)
            putBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT, node.isDirectory || entry?.canExtract == true)
        }
    }

    private fun stableEntryId(path: String): String = MessageDigest.getInstance("SHA-256")
        .digest(path.toByteArray(UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val ROOT_ID = "root"
    }
}
