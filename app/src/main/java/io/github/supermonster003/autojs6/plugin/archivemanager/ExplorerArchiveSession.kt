package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerArchiveSession
import java.io.InterruptedIOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.Charsets.UTF_8

internal class ExplorerArchiveSession(
    private val ownerUid: Int,
    private val displayName: String,
    private val stagedArchive: StagedArchive,
    private val snapshot: ArchiveSnapshot,
    private val onClosed: (ExplorerArchiveSession) -> Unit,
) : IExplorerArchiveSession.Stub() {

    private data class SessionNode(
        val id: String,
        val parentId: String?,
        val node: ArchiveNode,
    )

    private val closed = AtomicBoolean(false)
    private val formatCapabilities = ArchiveEngine.DEFAULT.capabilities(snapshot.format)
    private val sessionId = UUID.randomUUID().toString()
    private val rootId = ROOT_ID
    private val nodesById: Map<String, SessionNode>
    private val childrenByParentId: Map<String, List<SessionNode>>
    private val streamLock = Any()
    private val activeStreamWriters = LinkedHashSet<ParcelFileDescriptor>()
    private val streamExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "archive-entry-stream").apply { isDaemon = true }
    }

    init {
        val index = ArchiveIndex(snapshot)
        val nodesByPath = LinkedHashMap<String, SessionNode>()
        val mutableNodesById = LinkedHashMap<String, SessionNode>()
        val mutableChildren = LinkedHashMap<String, MutableList<SessionNode>>()
        val rootNode = requireNotNull(index.node(ArchivePathPolicy.ROOT_PATH))
        val rootSessionNode = SessionNode(rootId, null, rootNode)
        nodesByPath[rootNode.path] = rootSessionNode
        mutableNodesById[rootId] = rootSessionNode

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
                check(mutableNodesById.put(sessionNode.id, sessionNode) == null) {
                    "Archive entry ID collision"
                }
                if (child.isDirectory) queue += child
                sessionNode
            }
            mutableChildren[parentSessionNode.id] = children.toMutableList()
        }
        nodesById = mutableNodesById.toMap()
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
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_OPEN_ENTRIES,
                formatCapabilities.canPreview,
            )
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

    override fun openEntry(entryId: String?): ParcelFileDescriptor {
        checkCaller()
        checkOpen()
        require(entryId != null && entryId.length <= ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
            "Archive entry ID is invalid"
        }
        val node = requireNotNull(nodesById[entryId]) { "Archive entry does not exist" }.node
        val entry = requireNotNull(node.entry?.takeUnless(ArchiveEntry::isDirectory)) {
            "Archive entry is not a regular file"
        }
        require(formatCapabilities.canPreview && entry.canOpen) {
            "Archive entry is encrypted or uses an unsupported compression method"
        }

        val (readEnd, writeEnd) = ParcelFileDescriptor.createReliablePipe()
        try {
            synchronized(streamLock) {
                checkOpen()
                activeStreamWriters += writeEnd
                streamExecutor.execute {
                    streamEntry(entry, writeEnd)
                }
            }
        } catch (error: Throwable) {
            synchronized(streamLock) { activeStreamWriters.remove(writeEnd) }
            runCatching { readEnd.close() }
            runCatching { writeEnd.closeWithError(STREAM_CLOSED_MESSAGE) }
            throw error
        }
        return readEnd
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
        val streamWriters = synchronized(streamLock) {
            activeStreamWriters.toList().also { activeStreamWriters.clear() }
        }
        streamWriters.forEach { writer ->
            runCatching { writer.closeWithError(STREAM_CLOSED_MESSAGE) }
        }
        streamExecutor.shutdownNow()
        stagedArchive.delete()
        onClosed(this)
    }

    private fun streamEntry(entry: ArchiveEntry, writeEnd: ParcelFileDescriptor) {
        val output = ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)
        try {
            ArchiveEntryStreamer(stagedArchive.file, snapshot).stream(entry, output) {
                if (closed.get() || Thread.currentThread().isInterrupted) {
                    throw InterruptedIOException(STREAM_CLOSED_MESSAGE)
                }
            }
            output.close()
        } catch (error: Throwable) {
            runCatching { writeEnd.closeWithError(streamFailureMessage(error)) }
            runCatching { output.close() }
        } finally {
            synchronized(streamLock) { activeStreamWriters.remove(writeEnd) }
        }
    }

    private fun streamFailureMessage(error: Throwable): String = when (error) {
        is ArchiveException -> "Archive entry read failed (${error.code.name})"
        else -> "Archive entry read failed"
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
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_EXTRACT,
                node.isDirectory || formatCapabilities.canPreview && entry?.canOpen == true,
            )
        }
    }

    private fun stableEntryId(path: String): String = MessageDigest.getInstance("SHA-256")
        .digest(path.toByteArray(UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val ROOT_ID = "root"
        const val STREAM_CLOSED_MESSAGE = "Archive session is closed"
    }
}
