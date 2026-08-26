package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOperationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback
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
    private val isolatedPathDisplayName: String = ArchivePathPolicy.DEFAULT_ISOLATED_PATH_DISPLAY_NAME,
    private val extractionResourceBudget: ArchiveResourceBudget = ArchiveResourceBudget.COMPATIBLE,
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
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationLock = Any()
    private val activeOperations = LinkedHashMap<String, ExtractionOperationState>()

    init {
        val index = ArchiveIndex(snapshot, isolatedPathDisplayName)
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
            putLong(
                ExplorerArchiveSessionKeys.SOURCE_LAST_MODIFIED,
                stagedArchive.source.identity().lastModifiedMillis,
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_OPEN_ENTRIES,
                formatCapabilities.canPreview,
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_EXTRACT_ENTRIES,
                canRequestAnyExtraction(),
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
            "Archive entry data is unavailable to this backend"
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

    override fun extractEntries(
        request: Bundle,
        outputSession: IExplorerActionHostSession,
        callback: IExplorerArchiveOperationCallback,
    ) {
        checkCaller()
        checkOpen()
        check(formatCapabilities.canExtract) { "Archive format cannot be extracted" }
        val operationId = requireCanonicalOperationId(
            request.getString(ExplorerArchiveOperationKeys.OPERATION_ID),
        )
        @Suppress("DEPRECATION")
        val entryIds = request.getStringArrayList(ExplorerArchiveOperationKeys.ENTRY_IDS)
            ?: throw IllegalArgumentException("Archive extraction target IDs are missing")
        require(entryIds.isNotEmpty() && entryIds.size <= ExplorerActionProtocol.MAX_ARCHIVE_EXTRACTION_TARGETS) {
            "Archive extraction target count is invalid"
        }
        require(entryIds.distinct().size == entryIds.size) {
            "Archive extraction contains duplicate target IDs"
        }
        val selectedPaths = entryIds.mapTo(LinkedHashSet()) { entryId ->
            require(entryId.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
                "Archive extraction target ID is invalid"
            }
            val sessionNode = requireNotNull(nodesById[entryId]) {
                "Archive extraction target does not exist"
            }
            require(sessionNode.canRequestExtraction()) {
                "Archive extraction target is unavailable"
            }
            sessionNode.node.path
        }
        val extractionOptions = request.extractionOptions()

        lateinit var state: ExtractionOperationState
        val job = operationScope.launch(start = CoroutineStart.LAZY) {
            try {
                ExplorerArchiveExtractionOperation(
                    operationId = operationId,
                    displayName = displayName,
                    source = stagedArchive.source,
                    snapshot = snapshot,
                    selectedPaths = selectedPaths,
                    options = extractionOptions,
                    resourceBudget = extractionResourceBudget,
                    outputSession = outputSession,
                    callback = callback,
                ).run()
            } finally {
                extractionOptions.close()
                finishOperation(operationId, state)
            }
        }
        val deathRecipient = IBinder.DeathRecipient {
            job.cancel(CancellationException("Archive extraction host callback was released"))
        }
        state = ExtractionOperationState(job, callback.asBinder(), deathRecipient)
        try {
            synchronized(operationLock) {
                checkOpen()
                check(activeOperations.isEmpty()) { "Another archive extraction is already running" }
                callback.asBinder().linkToDeath(deathRecipient, 0)
                activeOperations[operationId] = state
            }
            job.start()
        } catch (error: Throwable) {
            synchronized(operationLock) { activeOperations.remove(operationId) }
            runCatching { callback.asBinder().unlinkToDeath(deathRecipient, 0) }
            job.cancel()
            extractionOptions.close()
            throw error
        }
    }

    override fun cancelExtraction(operationId: String?) {
        checkCaller()
        checkOpen()
        val normalizedId = requireCanonicalOperationId(operationId)
        synchronized(operationLock) { activeOperations[normalizedId] }
            ?.job
            ?.cancel(CancellationException("Archive extraction was cancelled by the host"))
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
        val operations = synchronized(operationLock) {
            activeOperations.values.toList().also { activeOperations.clear() }
        }
        operations.forEach { operation ->
            runCatching { operation.callbackBinder.unlinkToDeath(operation.deathRecipient, 0) }
            operation.job.cancel(CancellationException(STREAM_CLOSED_MESSAGE))
        }
        operationScope.cancel(CancellationException(STREAM_CLOSED_MESSAGE))
        val streamWriters = synchronized(streamLock) {
            activeStreamWriters.toList().also { activeStreamWriters.clear() }
        }
        streamWriters.forEach { writer ->
            runCatching { writer.closeWithError(STREAM_CLOSED_MESSAGE) }
        }
        streamExecutor.shutdownNow()
        snapshot.readerOptions.clearPassword()
        stagedArchive.close()
        onClosed(this)
    }

    private fun finishOperation(operationId: String, expected: ExtractionOperationState) {
        val removed = synchronized(operationLock) {
            activeOperations[operationId]
                ?.takeIf { it === expected }
                ?.also { activeOperations.remove(operationId) }
        } ?: return
        runCatching { removed.callbackBinder.unlinkToDeath(removed.deathRecipient, 0) }
    }

    private fun streamEntry(entry: ArchiveEntry, writeEnd: ParcelFileDescriptor) {
        val output = ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)
        try {
            ArchiveEntryStreamer(stagedArchive.source, snapshot).stream(entry, output) {
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
                ExplorerArchiveSessionKeys.CAN_OPEN,
                formatCapabilities.canPreview && entry?.canOpen == true,
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_EXTRACT,
                canRequestExtraction(),
            )
        }
    }

    private fun canRequestAnyExtraction(): Boolean = formatCapabilities.canExtract &&
        nodesById.values.any { sessionNode ->
            val node = sessionNode.node
            when {
                node.path == ArchivePathPolicy.ROOT_PATH -> false
                !node.isDirectory -> sessionNode.canRequestExtraction()
                childrenByParentId[sessionNode.id].orEmpty().isNotEmpty() -> false
                snapshot.isIsolatedPath(node.path) -> false
                else -> node.entry?.canExtract == true
            }
        }

    private fun SessionNode.canRequestExtraction(): Boolean {
        val archiveNode = node
        return when {
            archiveNode.path == ArchivePathPolicy.ROOT_PATH -> canRequestAnyExtraction()
            snapshot.isIsolatedPath(archiveNode.path) -> false
            archiveNode.isDirectory -> formatCapabilities.canExtract
            else -> formatCapabilities.canExtract && archiveNode.entry?.let { entry ->
                entry.canExtract || entry.isPasswordRecoverable()
            } == true
        }
    }

    private fun ArchiveEntry.isPasswordRecoverable(): Boolean = isEncrypted &&
        ArchiveEntryLimitation.MISSING_VOLUME !in capabilities.limitations &&
        ArchiveEntryLimitation.UNSUPPORTED_COMPRESSION_METHOD !in capabilities.limitations &&
        ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE !in capabilities.limitations

    private fun Bundle.extractionOptions(): ExplorerArchiveExtractionRequestOptions {
        val transientPassword = getCharArray(ExplorerArchiveOperationKeys.PASSWORD)
        return try {
            require(
                transientPassword == null ||
                    transientPassword.size in 1..ExplorerActionProtocol.MAX_ARCHIVE_PASSWORD_LENGTH,
            ) { "Archive extraction password length is invalid" }
            ExplorerArchiveExtractionRequestOptions(
                password = transientPassword,
                skipUnsafePaths = getBoolean(
                    ExplorerArchiveOperationKeys.SKIP_UNSAFE_PATHS,
                    false,
                ),
                allowResourceBudgetOverride = getBoolean(
                    ExplorerArchiveOperationKeys.ALLOW_RESOURCE_BUDGET_OVERRIDE,
                    false,
                ),
            )
        } finally {
            transientPassword?.fill('\u0000')
            remove(ExplorerArchiveOperationKeys.PASSWORD)
        }
    }

    private fun requireCanonicalOperationId(value: String?): String {
        val normalized = requireNotNull(value) { "Archive extraction operation ID is missing" }
        val parsed = runCatching { UUID.fromString(normalized) }.getOrNull()
        require(parsed?.toString()?.equals(normalized, ignoreCase = true) == true) {
            "Archive extraction operation ID is invalid"
        }
        return normalized
    }

    private fun stableEntryId(path: String): String = MessageDigest.getInstance("SHA-256")
        .digest(path.toByteArray(UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val ROOT_ID = "root"
        const val STREAM_CLOSED_MESSAGE = "Archive session is closed"
    }

    private data class ExtractionOperationState(
        val job: Job,
        val callbackBinder: IBinder,
        val deathRecipient: IBinder.DeathRecipient,
    )
}
