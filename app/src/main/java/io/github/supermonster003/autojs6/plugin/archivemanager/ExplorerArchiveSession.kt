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
import java.io.Closeable
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
    private val source: ArchiveReadSource = stagedArchive.source,
    private val volumeLease: Closeable? = null,
    snapshot: ArchiveSnapshot,
    initialFilenameCharsetOverride: String? = null,
    private val isolatedPathDisplayName: String = ArchivePathPolicy.DEFAULT_ISOLATED_PATH_DISPLAY_NAME,
    private val extractionResourceBudget: ArchiveResourceBudget = ArchiveResourceBudget.COMPATIBLE,
    private val onClosed: (ExplorerArchiveSession) -> Unit,
) : IExplorerArchiveSession.Stub() {

    private data class SessionNode(
        val id: String,
        val parentId: String?,
        val node: ArchiveNode,
    )

    private data class SessionIndexState(
        val snapshot: ArchiveSnapshot,
        val formatCapabilities: FormatCapabilities,
        val filenameCharsetOverride: String?,
        val nodesById: Map<String, SessionNode>,
        val childrenByParentId: Map<String, List<SessionNode>>,
    )

    private val closed = AtomicBoolean(false)
    private val sessionId = UUID.randomUUID().toString()
    private val rootId = ROOT_ID
    private val sessionStateLock = Any()
    @Volatile
    private var sessionState = buildIndexState(snapshot, initialFilenameCharsetOverride)
    private var reindexing = false
    private val streamLock = Any()
    private val activeStreamWriters = LinkedHashSet<ParcelFileDescriptor>()
    private val streamExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "archive-entry-stream").apply { isDaemon = true }
    }
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationLock = Any()
    private val activeOperations = LinkedHashMap<String, ExtractionOperationState>()

    private fun buildIndexState(
        snapshot: ArchiveSnapshot,
        filenameCharsetOverride: String?,
    ): SessionIndexState {
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
                    id = stableEntryId(child, index),
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
        return SessionIndexState(
            snapshot = snapshot,
            formatCapabilities = ArchiveEngine.DEFAULT.capabilities(snapshot.format),
            filenameCharsetOverride = filenameCharsetOverride,
            nodesById = mutableNodesById.toMap(),
            childrenByParentId = mutableChildren.mapValues { (_, value) -> value.toList() },
        )
    }

    override fun getInfo(): Bundle {
        checkCaller()
        val state = readyState()
        return state.toInfoBundle()
    }

    private fun SessionIndexState.toInfoBundle(): Bundle {
        check(
            formatCapabilities.filenameCharsetNames.size <=
                ExplorerActionProtocol.MAX_ARCHIVE_FILENAME_CHARSET_NAMES,
        ) { "Archive backend returned too many filename charsets" }
        check(formatCapabilities.filenameCharsetNames.all { name ->
            name.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_FILENAME_CHARSET_NAME_LENGTH
        }) { "Archive backend returned an invalid filename charset" }
        return Bundle().apply {
            putString(ExplorerArchiveSessionKeys.SESSION_ID, sessionId)
            putString(ExplorerArchiveSessionKeys.ROOT_ID, rootId)
            putString(ExplorerArchiveSessionKeys.DISPLAY_NAME, displayName)
            putLong(ExplorerArchiveSessionKeys.SOURCE_SIZE, stagedArchive.bytes)
            putLong(
                ExplorerArchiveSessionKeys.SOURCE_LAST_MODIFIED,
                source.identity().lastModifiedMillis,
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_OPEN_ENTRIES,
                formatCapabilities.canPreview,
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_EXTRACT_ENTRIES,
                canRequestAnyExtraction(this@toInfoBundle),
            )
            putStringArrayList(
                ExplorerArchiveSessionKeys.FILENAME_CHARSET_NAMES,
                ArrayList(formatCapabilities.filenameCharsetNames),
            )
            putString(
                ExplorerArchiveSessionKeys.FILENAME_CHARSET_NAME,
                snapshot.readerOptions.filenameCharsetName,
            )
            filenameCharsetOverride?.let { override ->
                putString(ExplorerArchiveSessionKeys.FILENAME_CHARSET_OVERRIDE, override)
            }
        }
    }

    override fun listChildren(parentId: String?, offset: Int, limit: Int): Bundle {
        checkCaller()
        val state = readyState()
        require(parentId != null && parentId.length <= ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
            "Archive parent ID is invalid"
        }
        require(offset >= 0) { "Archive page offset is invalid" }
        require(limit in 1..ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE) {
            "Archive page limit is invalid"
        }
        val children = requireNotNull(state.childrenByParentId[parentId]) {
            "Archive directory does not exist"
        }
        require(offset <= children.size) { "Archive page offset is out of range" }
        val end = (offset.toLong() + limit).coerceAtMost(children.size.toLong()).toInt()
        val items = ArrayList<Bundle>(end - offset)
        children.subList(offset, end).forEach { child -> items += child.toBundle(state) }
        return Bundle().apply {
            putParcelableArrayList(ExplorerArchiveSessionKeys.ITEMS, items)
            putInt(ExplorerArchiveSessionKeys.NEXT_OFFSET, end)
            putBoolean(ExplorerArchiveSessionKeys.COMPLETE, end == children.size)
        }
    }

    override fun openEntry(entryId: String?): ParcelFileDescriptor {
        checkCaller()
        require(entryId != null && entryId.length <= ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
            "Archive entry ID is invalid"
        }

        val (readEnd, writeEnd) = ParcelFileDescriptor.createReliablePipe()
        try {
            synchronized(sessionStateLock) {
                val state = readyStateLocked()
                val node = requireNotNull(state.nodesById[entryId]) {
                    "Archive entry does not exist"
                }.node
                val entry = requireNotNull(node.entry?.takeUnless(ArchiveEntry::isDirectory)) {
                    "Archive entry is not a regular file"
                }
                require(state.formatCapabilities.canPreview && entry.canOpen) {
                    "Archive entry data is unavailable to this backend"
                }
                synchronized(streamLock) {
                    activeStreamWriters += writeEnd
                    streamExecutor.execute {
                        streamEntry(entry, state.snapshot, writeEnd)
                    }
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
        val extractionOptions = request.extractionOptions()

        var job: Job? = null
        var operationState: ExtractionOperationState? = null
        var deathRecipient: IBinder.DeathRecipient? = null
        try {
            synchronized(sessionStateLock) {
                val indexState = readyStateLocked()
                check(indexState.formatCapabilities.canExtract) {
                    "Archive format cannot be extracted"
                }
                val selectedPaths = entryIds.mapTo(LinkedHashSet()) { entryId ->
                    require(entryId.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
                        "Archive extraction target ID is invalid"
                    }
                    val sessionNode = requireNotNull(indexState.nodesById[entryId]) {
                        "Archive extraction target does not exist"
                    }
                    require(sessionNode.canRequestExtraction(indexState)) {
                        "Archive extraction target is unavailable"
                    }
                    sessionNode.node.path
                }
                val createdJob = operationScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        ExplorerArchiveExtractionOperation(
                            operationId = operationId,
                            displayName = displayName,
                            source = source,
                            snapshot = indexState.snapshot,
                            selectedPaths = selectedPaths,
                            options = extractionOptions,
                            resourceBudget = extractionResourceBudget,
                            outputSession = outputSession,
                            callback = callback,
                        ).run()
                    } finally {
                        extractionOptions.close()
                        operationState?.let { expected -> finishOperation(operationId, expected) }
                    }
                }
                val createdDeathRecipient = IBinder.DeathRecipient {
                    createdJob.cancel(
                        CancellationException("Archive extraction host callback was released"),
                    )
                }
                val createdState = ExtractionOperationState(
                    createdJob,
                    callback.asBinder(),
                    createdDeathRecipient,
                )
                synchronized(operationLock) {
                    check(activeOperations.isEmpty()) {
                        "Another archive extraction is already running"
                    }
                    callback.asBinder().linkToDeath(createdDeathRecipient, 0)
                    activeOperations[operationId] = createdState
                }
                job = createdJob
                deathRecipient = createdDeathRecipient
                operationState = createdState
            }
            requireNotNull(job).start()
        } catch (error: Throwable) {
            val expected = operationState
            synchronized(operationLock) {
                if (expected != null && activeOperations[operationId] === expected) {
                    activeOperations.remove(operationId)
                }
            }
            deathRecipient?.let { recipient ->
                runCatching { callback.asBinder().unlinkToDeath(recipient, 0) }
            }
            job?.cancel()
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

    override fun reindexFilenameCharset(filenameCharsetName: String?): Bundle {
        checkCaller()
        val (previousState, canonicalOverride) = synchronized(sessionStateLock) {
            val current = readyStateLocked()
            val supportedNames = current.formatCapabilities.filenameCharsetNames
            check(supportedNames.isNotEmpty()) {
                "Archive format does not support filename charset overrides"
            }
            val canonical = canonicalFilenameCharset(filenameCharsetName, supportedNames)
            if (canonical == current.filenameCharsetOverride) {
                return current.toInfoBundle()
            }
            check(synchronized(streamLock) { activeStreamWriters.isEmpty() }) {
                "Archive entries are still being read"
            }
            check(synchronized(operationLock) { activeOperations.isEmpty() }) {
                "Archive extraction is still running"
            }
            reindexing = true
            current to canonical
        }

        val password = previousState.snapshot.readerOptions.passwordChars()
        val options = try {
            ArchiveReaderOptions(
                filenameCharsetName = canonicalOverride,
                password = password,
            )
        } finally {
            password?.fill('\u0000')
        }
        var replacementSnapshot: ArchiveSnapshot? = null
        try {
            val scanned = ArchiveScanner().scan(source, options) {
                check(!closed.get()) { "Archive session is closed" }
            }
            replacementSnapshot = scanned
            check(scanned.format == previousState.snapshot.format) {
                "Archive format changed while rebuilding the filename index"
            }
            val replacementState = buildIndexState(scanned, canonicalOverride)
            val replacementInfo = replacementState.toInfoBundle()
            synchronized(sessionStateLock) {
                checkOpen()
                check(reindexing) { "Archive filename reindex state changed unexpectedly" }
                sessionState = replacementState
                reindexing = false
            }
            replacementSnapshot = null
            previousState.snapshot.readerOptions.clearPassword()
            return replacementInfo
        } catch (error: Throwable) {
            replacementSnapshot?.readerOptions?.clearPassword()
            synchronized(sessionStateLock) {
                reindexing = false
            }
            throw error
        } finally {
            options.clearPassword()
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
        val currentSnapshot = synchronized(sessionStateLock) { sessionState.snapshot }
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
        currentSnapshot.readerOptions.clearPassword()
        runCatching { volumeLease?.close() }
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

    private fun streamEntry(
        entry: ArchiveEntry,
        snapshot: ArchiveSnapshot,
        writeEnd: ParcelFileDescriptor,
    ) {
        val output = ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)
        try {
            ArchiveEntryStreamer(source, snapshot).stream(entry, output) {
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

    private fun readyState(): SessionIndexState = synchronized(sessionStateLock) {
        readyStateLocked()
    }

    private fun readyStateLocked(): SessionIndexState {
        checkOpen()
        check(!reindexing) { "Archive filename index is being rebuilt" }
        return sessionState
    }

    private fun SessionNode.toBundle(state: SessionIndexState): Bundle {
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
                state.formatCapabilities.canPreview && entry?.canOpen == true,
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_EXTRACT,
                canRequestExtraction(state),
            )
        }
    }

    private fun canRequestAnyExtraction(state: SessionIndexState): Boolean =
        state.formatCapabilities.canExtract && state.nodesById.values.any { sessionNode ->
            val node = sessionNode.node
            when {
                node.path == ArchivePathPolicy.ROOT_PATH -> false
                !node.isDirectory -> sessionNode.canRequestExtraction(state)
                state.childrenByParentId[sessionNode.id].orEmpty().isNotEmpty() -> false
                state.snapshot.isIsolatedPath(node.path) -> false
                else -> node.entry?.canExtract == true
            }
        }

    private fun SessionNode.canRequestExtraction(state: SessionIndexState): Boolean {
        val archiveNode = node
        return when {
            archiveNode.path == ArchivePathPolicy.ROOT_PATH -> canRequestAnyExtraction(state)
            state.snapshot.isIsolatedPath(archiveNode.path) -> false
            archiveNode.isDirectory -> state.formatCapabilities.canExtract
            else -> state.formatCapabilities.canExtract && archiveNode.entry?.let { entry ->
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

    private fun canonicalFilenameCharset(
        requestedName: String?,
        supportedNames: List<String>,
    ): String? {
        if (requestedName == null) return null
        require(
            requestedName.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_FILENAME_CHARSET_NAME_LENGTH &&
                requestedName == requestedName.trim() &&
                requestedName.all { character ->
                    character.isLetterOrDigit() || character == '-' || character == '_' || character == '.'
                },
        ) { "Archive filename charset name is invalid" }
        return requireNotNull(supportedNames.firstOrNull { supported ->
            supported.equals(requestedName, ignoreCase = true)
        }) { "Archive filename charset is not supported" }
    }

    private fun stableEntryId(node: ArchiveNode, index: ArchiveIndex): String {
        val identity = node.entry?.let { entry ->
            "entry:${entry.ordinal}"
        } ?: buildString {
            val ordinals = ArrayList<Int>()
            collectDescendantOrdinals(node, index, ordinals)
            check(ordinals.isNotEmpty()) { "Synthetic archive directory has no source entries" }
            append("directory:")
            append(node.path.count { character -> character == '/' } + 1)
            append(':')
            append(ordinals.joinToString(","))
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    private fun collectDescendantOrdinals(
        node: ArchiveNode,
        index: ArchiveIndex,
        destination: MutableList<Int>,
    ) {
        node.entry?.let { entry -> destination += entry.ordinal }
        if (node.isDirectory) {
            index.children(node.path).forEach { child ->
                collectDescendantOrdinals(child, index, destination)
            }
        }
    }

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
