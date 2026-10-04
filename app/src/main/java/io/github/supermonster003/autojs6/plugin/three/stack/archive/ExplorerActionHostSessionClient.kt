package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.os.Binder
import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.util.UUID

internal data class HostSessionEntry(
    val relativePath: String,
    val displayName: String,
    val kind: Int,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val readable: Boolean,
    val symbolicLink: Boolean,
)

internal data class HostSessionPage(
    val items: List<HostSessionEntry>,
    val nextOffset: Int,
    val complete: Boolean,
)

internal data class HostOutputTransaction(
    val id: String,
    val displayName: String,
    val displayPath: String,
    val size: Long? = null,
    val lastModified: Long? = null,
    val kind: Int = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
    val state: Int = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
    val fileCount: Int = 0,
    val directoryCount: Int = 0,
    val outputBytes: Long = 0L,
    val replacementHistory: HostTargetReplacementHistory? = null,
)

internal data class HostOutputBatch(
    val id: String,
    val state: Int,
    val transactionIds: List<String>,
    val publishedCount: Int,
    val outputs: List<HostOutputTransaction> = emptyList(),
)

internal data class HostTargetTrashResult(
    val state: Int,
    val trashItemIds: List<String>,
    val movedCount: Int,
    val recoveryCount: Int,
    val batchId: String? = null,
)

internal data class HostTargetTrashBatchItem(
    val displayName: String,
    val state: Int,
)

internal data class HostTargetTrashBatch(
    val id: String,
    val state: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val movedCount: Int,
    val restoredCount: Int,
    val recoveryCount: Int,
    val items: List<HostTargetTrashBatchItem>,
) {
    val canUndo: Boolean
        get() = (
            state == ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_AVAILABLE ||
                state == ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_PARTIAL
            ) && items.any { item ->
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED
            }
}

internal data class HostTargetReplacementHistory(
    val id: String,
    val state: Int,
    val previousSize: Long,
    val createdAt: Long,
    val restoredSize: Long? = null,
    val restoredLastModified: Long? = null,
) {
    val isAvailable: Boolean
        get() = state == ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_AVAILABLE
}

internal class ExplorerActionHostSessionClient(
    private val remote: IExplorerActionHostSession,
) {
    private val clientToken = Binder()

    init {
        remote.attachClient(clientToken)
    }

    fun listChildren(targetId: String, relativePath: String, offset: Int): HostSessionPage {
        val result = remote.listChildren(
            targetId,
            relativePath,
            offset,
            ExplorerActionProtocol.MAX_SESSION_PAGE_SIZE,
        ) ?: error("Host returned no directory page")
        val bundles = result.parcelableBundleArrayList(ExplorerActionHostSessionKeys.ITEMS)
            ?: error("Host directory page has no item list")
        require(bundles.size <= ExplorerActionProtocol.MAX_SESSION_PAGE_SIZE) {
            "Host directory page exceeds the negotiated limit"
        }
        val items = bundles.map(::decodeEntry)
        val nextOffset = result.getInt(ExplorerActionHostSessionKeys.NEXT_OFFSET, -1)
        val complete = result.getBoolean(ExplorerActionHostSessionKeys.COMPLETE, false)
        require(nextOffset == offset + items.size) { "Host directory page offset is inconsistent" }
        require(complete || items.isNotEmpty()) { "Host directory pagination did not advance" }
        return HostSessionPage(items, nextOffset, complete)
    }

    fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor =
        remote.openFile(targetId, relativePath) ?: error("Host returned no source descriptor")

    fun prepareOutput(
        displayName: String,
        format: ArchiveFormat,
        conflictPolicy: ArchiveCreationConflictPolicy,
    ): HostOutputTransaction {
        val hostConflictPolicy = when (conflictPolicy) {
            ArchiveCreationConflictPolicy.AUTO_RENAME ->
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME
            ArchiveCreationConflictPolicy.ASK ->
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL
        }
        val result = try {
            remote.prepareOutput(
                displayName,
                format.primaryMimeType,
                hostConflictPolicy,
            )
        } catch (error: IllegalArgumentException) {
            if (conflictPolicy == ArchiveCreationConflictPolicy.ASK) {
                throw ArchiveOutputNameUnavailableException(displayName, error)
            }
            throw error
        } ?: error("Host returned no output transaction")
        return decodeOutput(result, format)
    }

    /** Reserves one exact physical output, including non-terminal split parts such as `.z01`. */
    fun prepareExactOutput(
        displayName: String,
        mimeType: String,
    ): HostOutputTransaction {
        val validatedName = ArchiveCompressionPolicy.validateOutputDisplayName(displayName)
            ?: throw IllegalArgumentException("Host output display name is invalid")
        require(
            mimeType.length <= ExplorerActionProtocol.MAX_OUTPUT_MIME_TYPE_LENGTH &&
                mimeType.matches(OUTPUT_MIME_TYPE_PATTERN),
        ) { "Host output MIME type is invalid" }
        val result = try {
            remote.prepareOutput(
                validatedName,
                mimeType,
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL,
            )
        } catch (error: IllegalArgumentException) {
            throw ArchiveOutputNameUnavailableException(validatedName, error)
        } ?: error("Host returned no output transaction")
        return decodeExactOutput(result, validatedName)
    }

    fun openOutput(transactionId: String): ParcelFileDescriptor =
        remote.openOutput(transactionId) ?: error("Host returned no output descriptor")

    fun prepareTargetReplacement(
        targetId: String,
        expectedDisplayName: String,
    ): HostOutputTransaction = decodeExactOutput(
        remote.prepareTargetReplacement(targetId)
            ?: error("Host returned no replacement transaction"),
        expectedDisplayName,
        defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_TARGET_REPLACEMENT,
    )

    fun prepareOutputTree(displayName: String): HostOutputTransaction {
        val validatedName = ArchivePathPolicy.validateDestinationRootName(displayName)
        val result = remote.prepareOutputTree(
            validatedName,
            ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
        ) ?: error("Host returned no directory output transaction")
        return decodeTreeOutput(
            result,
            defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
        )
    }

    fun createOutputDirectory(transactionId: String, relativePath: String) {
        remote.createOutputDirectory(transactionId, relativePath)
    }

    fun openOutputFile(transactionId: String, relativePath: String): ParcelFileDescriptor =
        remote.openOutputFile(transactionId, relativePath)
            ?: error("Host returned no directory output file descriptor")

    fun openPendingOutput(transactionId: String): ParcelFileDescriptor =
        remote.openPendingOutput(transactionId)
            ?: error("Host returned no pending output descriptor")

    fun commitOutput(
        transactionId: String,
        format: ArchiveFormat,
    ): HostOutputTransaction = decodeOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no commit result"),
        format,
        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
    )

    fun commitExactOutput(
        transactionId: String,
        expectedDisplayName: String,
    ): HostOutputTransaction = decodeExactOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no commit result"),
        expectedDisplayName,
        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
    )

    fun commitOutputTree(transactionId: String): HostOutputTransaction = decodeTreeOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no directory commit result"),
        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
    )

    fun queryOutput(transactionId: String): HostOutputTransaction? {
        val result = remote.queryOutput(transactionId) ?: error("Host returned no output state")
        val state = result.getInt(
            ExplorerActionHostSessionKeys.OUTPUT_STATE,
            ExplorerActionHostSessionValues.OUTPUT_STATE_UNKNOWN,
        )
        if (state == ExplorerActionHostSessionValues.OUTPUT_STATE_UNKNOWN) return null
        return decodeRawOutput(
            result,
            defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
            defaultState = state,
        )
    }

    fun listOutputs(): List<HostOutputTransaction> {
        val result = remote.listOutputs() ?: error("Host returned no output transaction list")
        return result.parcelableBundleArrayList(ExplorerActionHostSessionKeys.OUTPUTS)
            .orEmpty()
            .take(ExplorerActionProtocol.MAX_OUTPUT_TRANSACTION_RESULTS)
            .mapNotNull { bundle ->
                runCatching {
                    decodeRawOutput(
                        bundle,
                        defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
                        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_UNKNOWN,
                    )
                }.getOrNull()
            }
    }

    fun abortIncompleteOutputs(): Int {
        val batches = listOutputBatches().filter { batch ->
            batch.state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_PREPARED ||
                batch.state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTING ||
                batch.state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_RECOVERY_REQUIRED
        }
        val batchedTransactionIds = batches.flatMapTo(HashSet()) { batch -> batch.transactionIds }
        batches.forEach { batch -> remote.abortOutputBatch(batch.id) }
        val incomplete = listOutputs().filter { output ->
            output.id !in batchedTransactionIds &&
                (
                    output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED ||
                        output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING ||
                        output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_VERIFYING
                    )
        }
        incomplete.forEach { output -> remote.abortOutput(output.id) }
        return incomplete.size + batchedTransactionIds.size
    }

    fun abortOutput(transactionId: String) {
        remote.abortOutput(transactionId)
    }

    fun prepareOutputBatch(transactions: List<HostOutputTransaction>): HostOutputBatch {
        require(transactions.size in 2..ExplorerActionProtocol.MAX_OUTPUT_BATCH_ENTRIES) {
            "Host output batch size is invalid"
        }
        require(transactions.map(HostOutputTransaction::id).distinct().size == transactions.size) {
            "Host output batch contains duplicate transactions"
        }
        val result = remote.prepareOutputBatch(
            transactions.map(HostOutputTransaction::id).toMutableList(),
        ) ?: error("Host returned no output batch")
        return decodeOutputBatch(
            result,
            expectedTransactionIds = transactions.map(HostOutputTransaction::id),
        )
    }

    fun commitOutputBatch(batchId: String, expectedTransactions: List<HostOutputTransaction>): HostOutputBatch {
        val result = remote.commitOutputBatch(batchId) ?: error("Host returned no output batch commit result")
        return decodeOutputBatch(
            result,
            expectedBatchId = batchId,
            expectedTransactionIds = expectedTransactions.map(HostOutputTransaction::id),
            requireOutputs = true,
        )
    }

    fun queryOutputBatch(batchId: String): HostOutputBatch? {
        require(isCanonicalUuid(batchId)) { "Host output batch ID is invalid" }
        val result = remote.queryOutputBatch(batchId) ?: error("Host returned no output batch state")
        val returnedId = result.getString(ExplorerActionHostSessionKeys.OUTPUT_BATCH_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host output batch ID is invalid")
        require(returnedId == batchId) { "Host output batch ID changed" }
        val state = result.getInt(
            ExplorerActionHostSessionKeys.OUTPUT_BATCH_STATE,
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_UNKNOWN,
        )
        if (state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_UNKNOWN) return null
        return decodeOutputBatch(result, expectedBatchId = batchId)
    }

    fun listOutputBatches(): List<HostOutputBatch> {
        val result = remote.listOutputBatches() ?: error("Host returned no output batch list")
        return result.parcelableBundleArrayList(ExplorerActionHostSessionKeys.OUTPUT_BATCHES)
            .orEmpty()
            .take(ExplorerActionProtocol.MAX_OUTPUT_BATCH_RESULTS)
            .mapNotNull { bundle -> runCatching { decodeOutputBatch(bundle) }.getOrNull() }
    }

    fun abortOutputBatch(batchId: String) {
        remote.abortOutputBatch(batchId)
    }

    fun moveTargetsToTrash(
        targetIds: List<String>,
        outputTransactionIds: List<String>,
    ): HostTargetTrashResult {
        require(targetIds.isNotEmpty() && targetIds.size <= ExplorerActionProtocol.MAX_TARGETS_PER_REQUEST) {
            "Host target Trash selection is invalid"
        }
        require(targetIds.distinct().size == targetIds.size) {
            "Host target Trash selection contains duplicates"
        }
        require(
            outputTransactionIds.isNotEmpty() &&
                outputTransactionIds.size <= ExplorerActionProtocol.MAX_OUTPUT_TRANSACTION_RESULTS,
        ) { "Host target Trash output proof is invalid" }
        require(outputTransactionIds.distinct().size == outputTransactionIds.size) {
            "Host target Trash output proof contains duplicates"
        }
        require(outputTransactionIds.all(::isCanonicalUuid)) {
            "Host target Trash output proof contains an invalid transaction ID"
        }
        val result = try {
            remote.moveTargetsToTrash(
                targetIds.toMutableList(),
                outputTransactionIds.toMutableList(),
            ) ?: error("Host returned no target Trash result")
        } catch (error: Exception) {
            val recovered = try {
                remote.queryTargetTrash()?.let { bundle ->
                    decodeTargetTrash(bundle, targetIds.size)
                }
            } catch (_: Exception) {
                null
            }
            if (
                recovered != null &&
                recovered.state != ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN
            ) {
                return recovered
            }
            throw error
        }
        return decodeTargetTrash(result, targetIds.size)
    }

    fun queryTargetTrash(expectedTargetCount: Int): HostTargetTrashResult = decodeTargetTrash(
        remote.queryTargetTrash() ?: error("Host returned no target Trash state"),
        expectedTargetCount,
    )

    fun queryTargetTrashBatch(targetTrashBatchId: String): HostTargetTrashBatch? {
        require(isCanonicalUuid(targetTrashBatchId)) {
            "Host target Trash batch ID is invalid"
        }
        val result = remote.queryTargetTrashBatch(targetTrashBatchId)
            ?: error("Host returned no target Trash batch state")
        return decodeTargetTrashBatch(result, targetTrashBatchId)
    }

    fun listTargetTrashBatches(): List<HostTargetTrashBatch> {
        val result = remote.listTargetTrashBatches()
            ?: error("Host returned no target Trash batch list")
        return result.parcelableBundleArrayList(ExplorerActionHostSessionKeys.TARGET_TRASH_BATCHES)
            .orEmpty()
            .take(ExplorerActionProtocol.MAX_TARGET_TRASH_BATCH_RESULTS)
            .mapNotNull { bundle -> runCatching { decodeTargetTrashBatch(bundle) }.getOrNull() }
    }

    fun undoTargetTrashBatch(targetTrashBatchId: String): HostTargetTrashBatch {
        require(isCanonicalUuid(targetTrashBatchId)) {
            "Host target Trash batch ID is invalid"
        }
        val result = try {
            remote.undoTargetTrashBatch(targetTrashBatchId)
                ?: error("Host returned no target Trash batch undo result")
        } catch (error: Exception) {
            val recovered = try {
                remote.queryTargetTrashBatch(targetTrashBatchId)?.let { bundle ->
                    decodeTargetTrashBatch(bundle, targetTrashBatchId)
                }
            } catch (_: Exception) {
                null
            }
            return recovered ?: throw error
        }
        return decodeTargetTrashBatch(result, targetTrashBatchId)
            ?: error("Host returned an empty target Trash batch undo result")
    }

    fun queryTargetReplacement(targetId: String): HostTargetReplacementHistory? =
        decodeTargetReplacement(
            remote.queryTargetReplacement(targetId)
                ?: error("Host returned no target replacement history"),
        )

    fun undoTargetReplacement(
        targetId: String,
        replacementHistoryId: String,
    ): HostTargetReplacementHistory {
        require(isCanonicalUuid(replacementHistoryId)) {
            "Host target replacement history ID is invalid"
        }
        val result = decodeTargetReplacement(
            remote.undoTargetReplacement(targetId, replacementHistoryId)
                ?: error("Host returned no target replacement undo result"),
        ) ?: error("Host returned an empty target replacement undo result")
        require(result.id == replacementHistoryId) {
            "Host target replacement history ID changed"
        }
        require(
            result.state == ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED,
        ) { "Host target replacement undo did not complete" }
        require(result.restoredSize != null && result.restoredLastModified != null) {
            "Host target replacement undo result has no restored identity"
        }
        return result
    }

    fun close() {
        remote.close()
    }

    private fun decodeEntry(bundle: Bundle): HostSessionEntry {
        val relativePath = bundle.getString(ExplorerActionHostSessionKeys.RELATIVE_PATH)
            ?.takeIf { it.length <= ExplorerActionProtocol.MAX_SESSION_RELATIVE_PATH_LENGTH }
            ?: error("Host source entry has an invalid relative path")
        val displayName = ArchiveCompressionIntentPolicy.validateLeafName(
            bundle.getString(ExplorerActionHostSessionKeys.DISPLAY_NAME),
        ) ?: error("Host source entry has an invalid display name")
        val kind = bundle.getInt(ExplorerActionHostSessionKeys.KIND, Int.MIN_VALUE)
        require(kind == ExplorerActionValues.TARGET_FILE || kind == ExplorerActionValues.TARGET_DIRECTORY) {
            "Host source entry has an invalid kind"
        }
        val mimeType = bundle.getString(ExplorerActionHostSessionKeys.MIME_TYPE)
            ?.takeIf { it.isNotBlank() && it.length <= ExplorerActionProtocol.MAX_OUTPUT_MIME_TYPE_LENGTH }
            ?: error("Host source entry has an invalid MIME type")
        val size = bundle.getLong(ExplorerActionHostSessionKeys.SIZE, Long.MIN_VALUE)
        require(size >= SIZE_UNKNOWN) { "Host source entry has an invalid size" }
        val lastModified = bundle.getLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, Long.MIN_VALUE)
        require(lastModified >= SIZE_UNKNOWN) { "Host source entry has an invalid timestamp" }
        val expectedPath = relativePath.substringAfterLast('/')
        require(expectedPath == displayName) { "Host source entry path and display name differ" }
        return HostSessionEntry(
            relativePath = relativePath,
            displayName = displayName,
            kind = kind,
            mimeType = mimeType,
            size = size,
            lastModified = lastModified,
            readable = bundle.getBoolean(ExplorerActionHostSessionKeys.READABLE, false),
            symbolicLink = bundle.getBoolean(ExplorerActionHostSessionKeys.SYMBOLIC_LINK, true),
        )
    }

    private fun decodeTargetTrash(bundle: Bundle, expectedTargetCount: Int): HostTargetTrashResult {
        require(expectedTargetCount in 1..ExplorerActionProtocol.MAX_TARGETS_PER_REQUEST)
        val state = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_STATE,
            ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN,
        )
        require(
            state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_COMMITTED ||
                state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_RECOVERY_REQUIRED ||
                state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_FAILED ||
                state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN,
        ) { "Host target Trash state is invalid" }
        val trashItemIds = bundle.getStringArrayList(
            ExplorerActionHostSessionKeys.TARGET_TRASH_ITEM_IDS,
        ).orEmpty()
        require(
            trashItemIds.size <= expectedTargetCount &&
                trashItemIds.distinct().size == trashItemIds.size &&
                trashItemIds.all(::isCanonicalUuid),
        ) { "Host target Trash item list is invalid" }
        val movedCount = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_MOVED_COUNT,
            -1,
        )
        val recoveryCount = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_RECOVERY_COUNT,
            -1,
        )
        require(movedCount in 0..expectedTargetCount && recoveryCount in 0..expectedTargetCount) {
            "Host target Trash counts are invalid"
        }
        if (state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_COMMITTED) {
            require(
                movedCount == expectedTargetCount &&
                    recoveryCount == 0 &&
                    trashItemIds.size == expectedTargetCount,
            ) { "Host target Trash commit is incomplete" }
        }
        if (
            state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_FAILED ||
            state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN
        ) {
            require(movedCount == 0 && recoveryCount == 0 && trashItemIds.isEmpty()) {
                "Empty host target Trash state contains results"
            }
        }
        if (state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_RECOVERY_REQUIRED) {
            require(
                trashItemIds.isNotEmpty() &&
                    movedCount <= trashItemIds.size &&
                    recoveryCount <= trashItemIds.size,
            ) { "Host target Trash recovery state is inconsistent" }
        }
        val batchId = bundle.getString(ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_ID)?.also {
            require(isCanonicalUuid(it)) { "Host target Trash batch ID is invalid" }
        }
        if (state == ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN) {
            require(batchId == null) { "Unknown host target Trash state contains a batch ID" }
        } else {
            require(batchId != null) { "Host target Trash result has no batch ID" }
        }
        return HostTargetTrashResult(state, trashItemIds, movedCount, recoveryCount, batchId)
    }

    private fun decodeTargetTrashBatch(
        bundle: Bundle,
        expectedBatchId: String? = null,
    ): HostTargetTrashBatch? {
        val id = bundle.getString(ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host target Trash batch ID is invalid")
        expectedBatchId?.let { expected ->
            require(id == expected) { "Host target Trash batch ID changed" }
        }
        val state = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_STATE,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_UNKNOWN,
        )
        require(state in VALID_TARGET_TRASH_BATCH_STATES) {
            "Host target Trash batch state is invalid"
        }
        val itemBundles = bundle.parcelableBundleArrayList(
            ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_ITEMS,
        ).orEmpty()
        require(itemBundles.size <= ExplorerActionProtocol.MAX_TARGET_TRASH_ITEM_RESULTS) {
            "Host target Trash batch item count is invalid"
        }
        val items = itemBundles.map { item ->
            val displayName = ArchiveCompressionIntentPolicy.validateLeafName(
                item.getString(ExplorerActionHostSessionKeys.TARGET_TRASH_ITEM_DISPLAY_NAME),
            ) ?: error("Host target Trash item display name is invalid")
            val itemState = item.getInt(
                ExplorerActionHostSessionKeys.TARGET_TRASH_ITEM_STATE,
                ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_UNKNOWN,
            )
            require(itemState in VALID_TARGET_TRASH_ITEM_STATES) {
                "Host target Trash item state is invalid"
            }
            HostTargetTrashBatchItem(displayName, itemState)
        }
        val movedCount = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_MOVED_COUNT,
            -1,
        )
        val restoredCount = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_RESTORED_COUNT,
            -1,
        )
        val recoveryCount = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_TRASH_RECOVERY_COUNT,
            -1,
        )
        require(
            movedCount in 0..items.size &&
                restoredCount in 0..movedCount &&
                recoveryCount in 0..movedCount,
        ) { "Host target Trash batch counts are invalid" }
        if (state == ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_UNKNOWN) {
            require(
                items.isEmpty() && movedCount == 0 && restoredCount == 0 && recoveryCount == 0 &&
                    !bundle.containsKey(ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_CREATED_AT) &&
                    !bundle.containsKey(ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_UPDATED_AT),
            ) { "Unknown host target Trash batch contains results" }
            return null
        }
        require(items.isNotEmpty()) { "Host target Trash batch has no item results" }
        val createdAt = bundle.getLong(
            ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_CREATED_AT,
            -1L,
        )
        val updatedAt = bundle.getLong(
            ExplorerActionHostSessionKeys.TARGET_TRASH_BATCH_UPDATED_AT,
            -1L,
        )
        require(createdAt >= 0L && updatedAt >= createdAt) {
            "Host target Trash batch timestamps are invalid"
        }
        val crossedTrashBoundary = items.count { item ->
            item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED ||
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RESTORED ||
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_STALE ||
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RECOVERY_REQUIRED
        }
        require(movedCount == crossedTrashBoundary) {
            "Host target Trash moved count is inconsistent"
        }
        require(
            restoredCount == items.count { item ->
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RESTORED
            } && recoveryCount == items.count { item ->
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_STALE ||
                    item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RECOVERY_REQUIRED
            },
        ) { "Host target Trash terminal counts are inconsistent" }
        val hasMoved = items.any { item ->
            item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED
        }
        when (state) {
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_AVAILABLE ->
                require(items.all { item ->
                    item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED
                }) { "Available host target Trash batch is incomplete" }
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_PARTIAL ->
                require(hasMoved && items.any { item ->
                    item.state != ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED
                }) { "Partial host target Trash batch is inconsistent" }
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_RESTORED ->
                require(!hasMoved && restoredCount > 0 && recoveryCount == 0) {
                    "Restored host target Trash batch is inconsistent"
                }
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_STALE ->
                require(!hasMoved && items.any { item ->
                    item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_STALE
                }) { "Stale host target Trash batch is inconsistent" }
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_RECOVERY_REQUIRED ->
                require(!hasMoved && items.any { item ->
                    item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RECOVERY_REQUIRED
                }) { "Host target Trash recovery batch is inconsistent" }
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_FAILED ->
                require(movedCount == 0 && items.all { item ->
                    item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_NOT_MOVED
                }) { "Failed host target Trash batch contains moved items" }
        }
        return HostTargetTrashBatch(
            id = id,
            state = state,
            createdAt = createdAt,
            updatedAt = updatedAt,
            movedCount = movedCount,
            restoredCount = restoredCount,
            recoveryCount = recoveryCount,
            items = items,
        )
    }

    private fun decodeTargetReplacement(bundle: Bundle): HostTargetReplacementHistory? {
        val state = bundle.getInt(
            ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_UNDO_STATE,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_NONE,
        )
        require(state in VALID_TARGET_REPLACEMENT_UNDO_STATES) {
            "Host target replacement undo state is invalid"
        }
        if (state == ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_NONE) {
            require(
                !bundle.containsKey(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_HISTORY_ID) &&
                    !bundle.containsKey(
                        ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_PREVIOUS_SIZE,
                    ) &&
                    !bundle.containsKey(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_CREATED_AT),
            ) { "Empty host target replacement history contains metadata" }
            return null
        }
        val id = bundle.getString(ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_HISTORY_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host target replacement history ID is invalid")
        val previousSize = bundle.getLong(
            ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_PREVIOUS_SIZE,
            -1L,
        )
        val createdAt = bundle.getLong(
            ExplorerActionHostSessionKeys.TARGET_REPLACEMENT_CREATED_AT,
            -1L,
        )
        require(previousSize >= 0L && createdAt >= 0L) {
            "Host target replacement history metadata is invalid"
        }
        val restoredSize = if (
            state == ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED
        ) {
            bundle.getLong(ExplorerActionHostSessionKeys.SIZE)
                .takeIf { bundle.containsKey(ExplorerActionHostSessionKeys.SIZE) && it >= 0L }
        } else {
            null
        }
        val restoredLastModified = if (
            state == ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED
        ) {
            bundle.getLong(ExplorerActionHostSessionKeys.LAST_MODIFIED)
                .takeIf {
                    bundle.containsKey(ExplorerActionHostSessionKeys.LAST_MODIFIED) && it >= 0L
                }
        } else {
            null
        }
        if (state == ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED) {
            require((restoredSize == null) == (restoredLastModified == null)) {
                "Host target replacement restored identity is incomplete"
            }
        }
        return HostTargetReplacementHistory(
            id = id,
            state = state,
            previousSize = previousSize,
            createdAt = createdAt,
            restoredSize = restoredSize,
            restoredLastModified = restoredLastModified,
        )
    }

    private fun decodeOutput(
        bundle: Bundle,
        format: ArchiveFormat,
        defaultState: Int = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
    ): HostOutputTransaction {
        val decoded = decodeRawOutput(
            bundle,
            defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
            defaultState = defaultState,
        )
        require(
            ArchiveCompressionPolicy.normalizeOutputDisplayName(decoded.displayName, format) ==
                decoded.displayName,
        ) { "Host output display name does not match the archive format" }
        return decoded
    }

    private fun decodeExactOutput(
        bundle: Bundle,
        expectedDisplayName: String,
        defaultKind: Int = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
        defaultState: Int = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
    ): HostOutputTransaction = decodeRawOutput(bundle, defaultKind, defaultState).also { decoded ->
        require(decoded.displayName == expectedDisplayName) {
            "Host changed an exactly reserved output name"
        }
    }

    private fun decodeTreeOutput(
        bundle: Bundle,
        defaultState: Int,
    ): HostOutputTransaction = decodeRawOutput(
        bundle,
        defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE,
        defaultState = defaultState,
    ).also { decoded ->
        require(decoded.kind == ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE) {
            "Host output transaction is not a directory tree"
        }
    }

    private fun decodeRawOutput(
        bundle: Bundle,
        defaultKind: Int,
        defaultState: Int,
    ): HostOutputTransaction {
        val id = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host output transaction ID is invalid")
        val displayName = ArchiveCompressionPolicy.validateOutputDisplayName(
            bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME),
        ) ?: error("Host output display name is invalid")
        val displayPath = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH)
            ?.takeIf { it.length in 1..ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH }
            ?: error("Host output display path is invalid")
        val size = bundle.getLong(ExplorerActionHostSessionKeys.SIZE)
            .takeIf { bundle.containsKey(ExplorerActionHostSessionKeys.SIZE) && it >= 0L }
        val lastModified = bundle.getLong(ExplorerActionHostSessionKeys.LAST_MODIFIED)
            .takeIf { bundle.containsKey(ExplorerActionHostSessionKeys.LAST_MODIFIED) && it >= 0L }
        val kind = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_KIND, defaultKind)
        require(
            kind == ExplorerActionHostSessionValues.OUTPUT_KIND_FILE ||
                kind == ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE ||
                kind == ExplorerActionHostSessionValues.OUTPUT_KIND_TARGET_REPLACEMENT,
        ) { "Host output transaction kind is invalid" }
        val state = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_STATE, defaultState)
        require(state in VALID_OUTPUT_STATES) { "Host output transaction state is invalid" }
        val fileCount = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_FILE_COUNT, 0)
        val directoryCount = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_DIRECTORY_COUNT, 0)
        val outputBytes = bundle.getLong(ExplorerActionHostSessionKeys.OUTPUT_BYTES, 0L)
        require(fileCount >= 0 && directoryCount >= 0 && outputBytes >= 0L) {
            "Host output transaction statistics are invalid"
        }
        return HostOutputTransaction(
            id = id,
            displayName = displayName,
            displayPath = displayPath,
            size = size,
            lastModified = lastModified,
            kind = kind,
            state = state,
            fileCount = fileCount,
            directoryCount = directoryCount,
            outputBytes = outputBytes,
            replacementHistory = decodeTargetReplacement(bundle),
        )
    }

    private fun decodeOutputBatch(
        bundle: Bundle,
        expectedBatchId: String? = null,
        expectedTransactionIds: List<String>? = null,
        requireOutputs: Boolean = false,
    ): HostOutputBatch {
        val id = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_BATCH_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host output batch ID is invalid")
        expectedBatchId?.let { expected ->
            require(id == expected) { "Host output batch ID changed" }
        }
        val state = bundle.getInt(
            ExplorerActionHostSessionKeys.OUTPUT_BATCH_STATE,
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_UNKNOWN,
        )
        require(state in VALID_OUTPUT_BATCH_STATES) { "Host output batch state is invalid" }
        val transactionIds = bundle.getStringArrayList(
            ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_IDS,
        )?.toList() ?: error("Host output batch has no transaction list")
        require(transactionIds.size in 2..ExplorerActionProtocol.MAX_OUTPUT_BATCH_ENTRIES) {
            "Host output batch transaction count is invalid"
        }
        require(transactionIds.all(::isCanonicalUuid) && transactionIds.distinct().size == transactionIds.size) {
            "Host output batch transaction IDs are invalid"
        }
        expectedTransactionIds?.let { expected ->
            require(transactionIds == expected) { "Host output batch membership changed" }
        }
        val publishedCount = bundle.getInt(
            ExplorerActionHostSessionKeys.OUTPUT_BATCH_PUBLISHED_COUNT,
            -1,
        )
        require(publishedCount in 0..transactionIds.size) {
            "Host output batch published count is invalid"
        }
        val outputBundles = bundle.parcelableBundleArrayList(ExplorerActionHostSessionKeys.OUTPUTS)
        if (requireOutputs) {
            require(outputBundles?.size == transactionIds.size) {
                "Host output batch commit result is incomplete"
            }
        }
        val outputs = outputBundles.orEmpty().map { output ->
            decodeRawOutput(
                output,
                defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
                defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
            )
        }
        if (outputs.isNotEmpty()) {
            require(outputs.map(HostOutputTransaction::id) == transactionIds) {
                "Host output batch results do not match its membership"
            }
        }
        if (requireOutputs) {
            require(outputs.all { output ->
                output.kind == ExplorerActionHostSessionValues.OUTPUT_KIND_FILE &&
                    output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED
            }) { "Host output batch contains an uncommitted result" }
        }
        return HostOutputBatch(id, state, transactionIds, publishedCount, outputs)
    }

    @Suppress("DEPRECATION")
    private fun Bundle.parcelableBundleArrayList(name: String): ArrayList<Bundle>? =
        getParcelableArrayList(name)

    private fun isCanonicalUuid(value: String): Boolean {
        val parsed = runCatching { UUID.fromString(value) }.getOrNull() ?: return false
        return parsed.toString().equals(value, ignoreCase = true)
    }

    private companion object {
        const val SIZE_UNKNOWN = -1L
        val OUTPUT_MIME_TYPE_PATTERN = Regex("^[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+$")
        val VALID_OUTPUT_STATES = setOf(
            ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
            ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING,
            ExplorerActionHostSessionValues.OUTPUT_STATE_VERIFYING,
            ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
            ExplorerActionHostSessionValues.OUTPUT_STATE_ABORTED,
        )
        val VALID_OUTPUT_BATCH_STATES = setOf(
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_PREPARED,
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTING,
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTED,
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_ABORTED,
            ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_RECOVERY_REQUIRED,
        )
        val VALID_TARGET_REPLACEMENT_UNDO_STATES = setOf(
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_NONE,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_AVAILABLE,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_STALE,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RECOVERY_REQUIRED,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_UNKNOWN,
        )
        val VALID_TARGET_TRASH_BATCH_STATES = setOf(
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_AVAILABLE,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_PARTIAL,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_RESTORED,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_STALE,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_RECOVERY_REQUIRED,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_FAILED,
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_UNKNOWN,
        )
        val VALID_TARGET_TRASH_ITEM_STATES = setOf(
            ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED,
            ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_NOT_MOVED,
            ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RESTORED,
            ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_STALE,
            ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RECOVERY_REQUIRED,
            ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_UNKNOWN,
        )
    }
}
