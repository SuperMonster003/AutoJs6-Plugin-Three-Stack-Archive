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
import org.autojs.plugin.explorer.api.ExplorerArchiveMutationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveOperationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.autojs.plugin.explorer.api.IExplorerArchiveInputSession
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback
import org.autojs.plugin.explorer.api.IExplorerArchiveSession
import java.io.Closeable
import java.io.File
import java.io.InterruptedIOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.Charsets.UTF_8

internal class ExplorerArchiveSession(
    private val ownerUid: Int,
    private val displayName: String,
    stagedArchive: StagedArchive,
    source: ArchiveReadSource = stagedArchive.source,
    private val volumeLease: Closeable? = null,
    private val cacheDirectory: File = stagedArchive.source.localFile?.parentFile ?: File("."),
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
        val mutationAvailability: ArchiveMutationAvailability,
        val filenameCharsetOverride: String?,
        val nodesById: Map<String, SessionNode>,
        val childrenByParentId: Map<String, List<SessionNode>>,
    )

    private val closed = AtomicBoolean(false)
    private val sessionId = UUID.randomUUID().toString()
    private val rootId = ROOT_ID
    private val sessionStateLock = Any()
    private var stagedArchive = stagedArchive
    private var source = source
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
    private val activeOperations = LinkedHashMap<String, AsyncOperationState>()

    private fun buildIndexState(
        snapshot: ArchiveSnapshot,
        filenameCharsetOverride: String?,
        preferredIdsByPath: Map<String, String> = emptyMap(),
    ): SessionIndexState {
        val index = ArchiveIndex(snapshot, isolatedPathDisplayName)
        val nodesByPath = LinkedHashMap<String, SessionNode>()
        val mutableNodesById = LinkedHashMap<String, SessionNode>()
        val mutableChildren = LinkedHashMap<String, MutableList<SessionNode>>()
        val reservedEntryIds = preferredIdsByPath.values.toSet()
        check(reservedEntryIds.size == preferredIdsByPath.size && rootId !in reservedEntryIds) {
            "Archive preferred entry IDs are invalid"
        }
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
                    id = preferredIdsByPath[child.path] ?: unusedStableEntryId(
                        child,
                        index,
                        reservedEntryIds,
                        mutableNodesById,
                    ),
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
            mutationAvailability = ArchiveEngine.DEFAULT.mutationAvailability(snapshot),
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
            putString(ExplorerArchiveSessionKeys.FORMAT_ID, snapshot.format.id)
            putString(ExplorerArchiveSessionKeys.FORMAT_DISPLAY_NAME, snapshot.format.displayName)
            putBoolean(
                ExplorerArchiveSessionKeys.DISPLAY_NAME_MATCHES_FORMAT,
                snapshot.format.matchesFileName(displayName) ||
                    NumberedArchiveVolumePolicy.inspectFirstVolume(displayName)?.format == snapshot.format,
            )
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
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_DELETE_ENTRIES,
                canRequestAnyMutation(this@toInfoBundle, ArchiveOperation.DELETE),
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_RENAME_ENTRIES,
                canRequestAnyMutation(this@toInfoBundle, ArchiveOperation.RENAME),
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_CREATE_DIRECTORY,
                nodesById.getValue(rootId).canCreateChildren(this@toInfoBundle),
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_ADD_ENTRIES,
                nodesById.getValue(rootId).canCreateChildren(this@toInfoBundle),
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
        var operationState: AsyncOperationState? = null
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
                val createdState = AsyncOperationState(
                    kind = AsyncOperationKind.EXTRACTION,
                    job = createdJob,
                    callbackBinder = callback.asBinder(),
                    deathRecipient = createdDeathRecipient,
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
        synchronized(operationLock) {
            activeOperations[normalizedId]?.takeIf { it.kind == AsyncOperationKind.EXTRACTION }
        }
            ?.job
            ?.cancel(CancellationException("Archive extraction was cancelled by the host"))
    }

    override fun mutateEntries(
        request: Bundle,
        outputSession: IExplorerActionHostSession,
        callback: IExplorerArchiveOperationCallback,
    ) {
        checkCaller()
        val operationId = requireCanonicalOperationId(
            request.getString(ExplorerArchiveMutationKeys.OPERATION_ID),
        )
        startMutationOperation(
            operationId = operationId,
            outputSession = outputSession,
            callback = callback,
            prepareDecoded = { indexState ->
                val decoded = request.decodeMutation(indexState)
                val decode: () -> DecodedMutation = { decoded }
                decode
            },
        )
    }

    override fun addEntries(
        request: Bundle,
        outputSession: IExplorerActionHostSession,
        inputSession: IExplorerArchiveInputSession,
        callback: IExplorerArchiveOperationCallback,
    ) {
        checkCaller()
        val operationId = requireCanonicalOperationId(
            request.getString(ExplorerArchiveMutationKeys.OPERATION_ID),
        )
        val inputClient = ExplorerArchiveInputSessionClient(
            remote = inputSession,
            expectedArchiveSessionId = sessionId,
            expectedOperationId = operationId,
        )
        try {
            startMutationOperation(
                operationId = operationId,
                outputSession = outputSession,
                callback = callback,
                prepareDecoded = { indexState ->
                    val parentEntryId = requireNotNull(
                        request.getString(ExplorerArchiveMutationKeys.PARENT_ENTRY_ID),
                    ) { "Archive destination directory ID is missing" }
                    require(
                        parentEntryId.length in
                            1..ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH,
                    ) { "Archive destination directory ID is invalid" }
                    val parent = requireNotNull(indexState.nodesById[parentEntryId]) {
                        "Archive destination directory does not exist"
                    }
                    require(parent.canCreateChildren(indexState)) {
                        "Archive destination does not support child creation"
                    }
                    val parentPath = parent.node.path
                    val decode: () -> DecodedMutation = {
                        inputClient.buildMutation(parentPath).let { mutation ->
                            DecodedMutation(mutation.request, mutation.nodeCount)
                        }
                    }
                    decode
                },
                beforeOutputVerification = inputClient::verifySnapshot,
                onFinished = inputClient::close,
            )
        } catch (error: Throwable) {
            runCatching { inputClient.close() }
            throw error
        }
    }

    private fun startMutationOperation(
        operationId: String,
        outputSession: IExplorerActionHostSession,
        callback: IExplorerArchiveOperationCallback,
        prepareDecoded: (SessionIndexState) -> (() -> DecodedMutation),
        beforeOutputVerification: () -> Unit = {},
        onFinished: () -> Unit = {},
    ) {

        var job: Job? = null
        var operationState: AsyncOperationState? = null
        var deathRecipient: IBinder.DeathRecipient? = null
        try {
            synchronized(sessionStateLock) {
                val indexState = readyStateLocked()
                check(synchronized(streamLock) { activeStreamWriters.isEmpty() }) {
                    "Archive entries are still being read"
                }
                val decode = prepareDecoded(indexState)
                reindexing = true
                val createdJob = operationScope.launch(start = CoroutineStart.LAZY) {
                    var unownedOutcome: ExplorerArchiveMutationOutcome? = null
                    var resourcesFinished = false
                    fun finishResources() {
                        if (resourcesFinished) return
                        resourcesFinished = true
                        runCatching { onFinished() }
                    }
                    var operationFinished = false
                    fun finishOperationLifecycle() {
                        finishResources()
                        if (operationFinished) return
                        operationFinished = true
                        operationState?.let { expected -> finishOperation(operationId, expected) }
                    }
                    try {
                        val decoded = decode()
                        val retainedIdsByPath = retainedIdsByPath(indexState, decoded.request)
                        val outcome = ExplorerArchiveMutationOperation(
                            operationId = operationId,
                            displayName = displayName,
                            cacheDirectory = cacheDirectory,
                            source = source,
                            snapshot = indexState.snapshot,
                            request = decoded.request,
                            outputSession = outputSession,
                            callback = callback,
                            beforeOutputVerification = beforeOutputVerification,
                        ).run()
                        unownedOutcome = outcome
                        val replacementState = buildIndexState(
                            snapshot = outcome.snapshot,
                            filenameCharsetOverride = indexState.filenameCharsetOverride,
                            preferredIdsByPath = retainedIdsByPath,
                        )
                        val previousStaged = synchronized(sessionStateLock) {
                            checkOpen()
                            check(reindexing) { "Archive mutation state changed unexpectedly" }
                            val oldStaged = stagedArchive
                            stagedArchive = outcome.stagedArchive
                            source = outcome.stagedArchive.source
                            sessionState = replacementState
                            reindexing = false
                            unownedOutcome = null
                            oldStaged
                        }
                        indexState.snapshot.readerOptions.clearPassword()
                        previousStaged.close()
                        finishOperationLifecycle()
                        runCatching {
                            callback.onCompleted(
                                Bundle().apply {
                                    putString(ExplorerArchiveMutationKeys.OPERATION_ID, operationId)
                                    putBundle(
                                        ExplorerArchiveMutationKeys.SESSION_INFO,
                                        replacementState.toInfoBundle(),
                                    )
                                    putInt(
                                        ExplorerArchiveMutationKeys.MUTATED_ENTRIES,
                                        decoded.mutatedEntries,
                                    )
                                },
                            )
                        }
                    } catch (error: Throwable) {
                        unownedOutcome?.let { outcome ->
                            outcome.snapshot.readerOptions.clearPassword()
                            outcome.stagedArchive.close()
                        }
                        synchronized(sessionStateLock) {
                            if (!closed.get()) reindexing = false
                        }
                        finishOperationLifecycle()
                        reportMutationFailure(callback, operationId, error)
                    } finally {
                        finishOperationLifecycle()
                    }
                }
                val createdDeathRecipient = IBinder.DeathRecipient {
                    createdJob.cancel(
                        CancellationException("Archive mutation host callback was released"),
                    )
                }
                val createdState = AsyncOperationState(
                    kind = AsyncOperationKind.MUTATION,
                    job = createdJob,
                    callbackBinder = callback.asBinder(),
                    deathRecipient = createdDeathRecipient,
                )
                synchronized(operationLock) {
                    check(activeOperations.isEmpty()) {
                        "Another archive operation is already running"
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
            synchronized(sessionStateLock) {
                if (!closed.get()) reindexing = false
            }
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
            throw error
        }
    }

    override fun cancelMutation(operationId: String?) {
        checkCaller()
        checkOpen()
        val normalizedId = requireCanonicalOperationId(operationId)
        synchronized(operationLock) {
            activeOperations[normalizedId]?.takeIf { it.kind == AsyncOperationKind.MUTATION }
        }
            ?.job
            ?.cancel(CancellationException("Archive mutation was cancelled by the host"))
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

    private fun finishOperation(operationId: String, expected: AsyncOperationState) {
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
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_DELETE,
                canRequestMutation(state, ArchiveOperation.DELETE),
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_RENAME,
                canRequestMutation(state, ArchiveOperation.RENAME),
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_CREATE_CHILDREN,
                canCreateChildren(state),
            )
            putBoolean(
                ExplorerArchiveSessionKeys.CAN_ADD_CHILDREN,
                canCreateChildren(state),
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

    private fun canRequestAnyMutation(
        state: SessionIndexState,
        operation: ArchiveOperation,
    ): Boolean = state.nodesById.values.any { sessionNode ->
        sessionNode.canRequestMutation(state, operation)
    }

    private fun SessionNode.canRequestMutation(
        state: SessionIndexState,
        operation: ArchiveOperation,
    ): Boolean {
        val path = node.path
        val capabilities = state.mutationAvailability.capabilities ?: return false
        if (
            path == ArchivePathPolicy.ROOT_PATH ||
            state.snapshot.isIsolatedPath(path) ||
            !capabilities.supports(operation)
        ) {
            return false
        }
        val affected = state.snapshot.entries.filter { entry ->
            entry.path == path || entry.path.startsWith("$path/")
        }
        if (affected.isEmpty()) return false
        return affected.all { entry ->
            entry.pathStatus == ArchiveEntryPathStatus.SAFE && when (operation) {
                ArchiveOperation.DELETE -> entry.capabilities.canDelete
                ArchiveOperation.RENAME -> entry.capabilities.canRename
                else -> false
            }
        }
    }

    private fun SessionNode.canCreateChildren(state: SessionIndexState): Boolean {
        val path = node.path
        val capabilities = state.mutationAvailability.capabilities ?: return false
        if (!node.isDirectory || !capabilities.supports(ArchiveOperation.ADD)) return false
        if (path == ArchivePathPolicy.ROOT_PATH) return true
        if (state.snapshot.isIsolatedPath(path)) return false
        return state.snapshot.entries.any { entry ->
            entry.pathStatus == ArchiveEntryPathStatus.SAFE &&
                (entry.path == path || entry.path.startsWith("$path/"))
        }
    }

    private fun ArchiveEntry.isPasswordRecoverable(): Boolean = isEncrypted &&
        ArchiveEntryLimitation.MISSING_VOLUME !in capabilities.limitations &&
        ArchiveEntryLimitation.UNSUPPORTED_COMPRESSION_METHOD !in capabilities.limitations &&
        ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE !in capabilities.limitations

    private fun Bundle.decodeMutation(state: SessionIndexState): DecodedMutation {
        return when (getInt(ExplorerArchiveMutationKeys.OPERATION, Int.MIN_VALUE)) {
            ExplorerArchiveSessionValues.MUTATION_DELETE -> {
                @Suppress("DEPRECATION")
                val entryIds = getStringArrayList(ExplorerArchiveMutationKeys.ENTRY_IDS)
                    ?: throw IllegalArgumentException("Archive mutation target IDs are missing")
                require(
                    entryIds.isNotEmpty() &&
                        entryIds.size <= ExplorerActionProtocol.MAX_ARCHIVE_MUTATION_TARGETS,
                ) { "Archive mutation target count is invalid" }
                require(entryIds.distinct().size == entryIds.size) {
                    "Archive mutation contains duplicate target IDs"
                }
                val paths = entryIds.mapTo(LinkedHashSet()) { entryId ->
                    require(entryId.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
                        "Archive mutation target ID is invalid"
                    }
                    val node = requireNotNull(state.nodesById[entryId]) {
                        "Archive mutation target does not exist"
                    }
                    require(node.canRequestMutation(state, ArchiveOperation.DELETE)) {
                        "Archive mutation target cannot be deleted"
                    }
                    node.node.path
                }
                DecodedMutation(ArchiveMutationRequest.Delete(paths), entryIds.size)
            }
            ExplorerArchiveSessionValues.MUTATION_RENAME -> {
                val entryId = requireNotNull(
                    getString(ExplorerArchiveMutationKeys.ENTRY_ID),
                ) { "Archive rename target ID is missing" }
                require(entryId.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH) {
                    "Archive rename target ID is invalid"
                }
                val newName = requireNotNull(
                    getString(ExplorerArchiveMutationKeys.NEW_NAME),
                ) { "Archive rename entry name is missing" }
                require(newName.length <= ExplorerActionProtocol.MAX_ARCHIVE_MUTATION_NAME_LENGTH) {
                    "Archive rename entry name is too long"
                }
                val validatedName = requireNotNull(ArchiveIntentPolicy.validateDisplayName(newName)) {
                    "Archive rename entry name is invalid"
                }
                val node = requireNotNull(state.nodesById[entryId]) {
                    "Archive rename target does not exist"
                }
                require(node.canRequestMutation(state, ArchiveOperation.RENAME)) {
                    "Archive mutation target cannot be renamed"
                }
                DecodedMutation(
                    ArchiveMutationRequest.Rename(node.node.path, validatedName),
                    mutatedEntries = 1,
                )
            }
            ExplorerArchiveSessionValues.MUTATION_CREATE_DIRECTORY -> {
                val parentEntryId = requireNotNull(
                    getString(ExplorerArchiveMutationKeys.PARENT_ENTRY_ID),
                ) { "Archive destination directory ID is missing" }
                require(
                    parentEntryId.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_ENTRY_ID_LENGTH,
                ) { "Archive destination directory ID is invalid" }
                val newName = requireNotNull(
                    getString(ExplorerArchiveMutationKeys.NEW_NAME),
                ) { "Archive directory name is missing" }
                require(newName.length <= ExplorerActionProtocol.MAX_ARCHIVE_MUTATION_NAME_LENGTH) {
                    "Archive directory name is too long"
                }
                val validatedName = requireNotNull(ArchiveIntentPolicy.validateDisplayName(newName)) {
                    "Archive directory name is invalid"
                }
                val parent = requireNotNull(state.nodesById[parentEntryId]) {
                    "Archive destination directory does not exist"
                }
                require(parent.canCreateChildren(state)) {
                    "Archive destination does not support child creation"
                }
                DecodedMutation(
                    ArchiveMutationRequest.AddDirectory(parent.node.path, validatedName),
                    mutatedEntries = 1,
                )
            }
            else -> throw IllegalArgumentException("Archive mutation operation is invalid")
        }
    }

    private fun retainedIdsByPath(
        state: SessionIndexState,
        request: ArchiveMutationRequest,
    ): Map<String, String> {
        val retained = LinkedHashMap<String, String>()
        state.nodesById.values.forEach { sessionNode ->
            val oldPath = sessionNode.node.path
            if (oldPath == ArchivePathPolicy.ROOT_PATH) return@forEach
            val newPath = when (request) {
                is ArchiveMutationRequest.Delete -> oldPath.takeUnless { candidate ->
                    request.paths.any { deleted ->
                        candidate == deleted || candidate.startsWith("$deleted/")
                    }
                }
                is ArchiveMutationRequest.Rename -> {
                    val target = request.path
                    val parent = target.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
                    val renamedRoot = if (parent.isEmpty()) {
                        request.newDisplayName
                    } else {
                        "$parent/${request.newDisplayName}"
                    }
                    when {
                        oldPath == target -> renamedRoot
                        oldPath.startsWith("$target/") -> renamedRoot + oldPath.removePrefix(target)
                        else -> oldPath
                    }
                }
                is ArchiveMutationRequest.AddDirectory,
                is ArchiveMutationRequest.AddFiles,
                is ArchiveMutationRequest.AddTree,
                -> oldPath
            } ?: return@forEach
            check(retained.put(newPath, sessionNode.id) == null) {
                "Archive mutation produced duplicate stable entry paths"
            }
        }
        return retained
    }

    private fun reportMutationFailure(
        callback: IExplorerArchiveOperationCallback,
        operationId: String,
        error: Throwable,
    ) {
        val archiveError = generateSequence(error) { cause -> cause.cause }
            .filterIsInstance<ArchiveException>()
            .firstOrNull()
        val errorCode = when {
            error is CancellationException || error is InterruptedIOException ->
                ExplorerArchiveSessionValues.MUTATION_ERROR_CANCELLED
            archiveError?.code == ArchiveFailureCode.SOURCE_CHANGED ->
                ExplorerArchiveSessionValues.MUTATION_ERROR_SOURCE_CHANGED
            error is IllegalArgumentException || archiveError?.code in MUTATION_REQUEST_FAILURES ->
                ExplorerArchiveSessionValues.MUTATION_ERROR_INVALID_REQUEST
            archiveError?.code in MUTATION_UNSUPPORTED_FAILURES ->
                ExplorerArchiveSessionValues.MUTATION_ERROR_UNSUPPORTED
            archiveError?.stage == ArchiveFailureStage.OUTPUT ||
                archiveError?.code == ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE ->
                ExplorerArchiveSessionValues.MUTATION_ERROR_OUTPUT
            else -> ExplorerArchiveSessionValues.MUTATION_ERROR_UNKNOWN
        }
        val message = (error.message ?: "Archive mutation failed")
            .replace(Regex("[\\p{Cc}\\p{Cf}]+"), " ")
            .trim()
            .ifEmpty { "Archive mutation failed" }
            .take(ExplorerActionProtocol.MAX_ARCHIVE_OPERATION_MESSAGE_LENGTH)
        runCatching {
            callback.onFailed(
                Bundle().apply {
                    putString(ExplorerArchiveMutationKeys.OPERATION_ID, operationId)
                    putInt(ExplorerArchiveMutationKeys.ERROR_CODE, errorCode)
                    putString(ExplorerArchiveMutationKeys.ERROR_MESSAGE, message)
                },
            )
        }
    }

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
        val normalized = requireNotNull(value) { "Archive operation ID is missing" }
        val parsed = runCatching { UUID.fromString(normalized) }.getOrNull()
        require(parsed?.toString()?.equals(normalized, ignoreCase = true) == true) {
            "Archive operation ID is invalid"
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

    private fun unusedStableEntryId(
        node: ArchiveNode,
        index: ArchiveIndex,
        reservedEntryIds: Set<String>,
        assignedNodes: Map<String, SessionNode>,
    ): String {
        repeat(ExplorerActionProtocol.MAX_ARCHIVE_ITEMS + 1) { collisionIndex ->
            val candidate = stableEntryId(node, index, collisionIndex)
            if (candidate !in reservedEntryIds && candidate !in assignedNodes) return candidate
        }
        error("Archive entry ID allocation exhausted")
    }

    private fun stableEntryId(
        node: ArchiveNode,
        index: ArchiveIndex,
        collisionIndex: Int = 0,
    ): String {
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
        val collisionSafeIdentity = if (collisionIndex == 0) {
            identity
        } else {
            "$identity:collision:$collisionIndex:${node.path}"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(collisionSafeIdentity.toByteArray(UTF_8))
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
        val MUTATION_REQUEST_FAILURES = setOf(
            ArchiveFailureCode.EMPTY_SELECTION,
            ArchiveFailureCode.UNKNOWN_SELECTION,
            ArchiveFailureCode.INVALID_DESTINATION_NAME,
            ArchiveFailureCode.DUPLICATE_PATH,
            ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
        )
        val MUTATION_UNSUPPORTED_FAILURES = setOf(
            ArchiveFailureCode.MISSING_VOLUME,
            ArchiveFailureCode.UNSUPPORTED_METHOD,
            ArchiveFailureCode.PASSWORD_REQUIRED,
            ArchiveFailureCode.WRONG_PASSWORD,
            ArchiveFailureCode.INVALID_PATH,
        )
    }

    private data class DecodedMutation(
        val request: ArchiveMutationRequest,
        val mutatedEntries: Int,
    )

    private data class AsyncOperationState(
        val kind: AsyncOperationKind,
        val job: Job,
        val callbackBinder: IBinder,
        val deathRecipient: IBinder.DeathRecipient,
    )

    private enum class AsyncOperationKind {
        EXTRACTION,
        MUTATION,
    }
}
