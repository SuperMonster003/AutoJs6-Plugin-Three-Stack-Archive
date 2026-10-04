package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import java.io.IOException

internal interface ArchiveOutputCommitter {
    fun commitOutput(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        format: ArchiveFormat,
    ): HostOutputTransaction

    fun commitExactOutput(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        expectedDisplayName: String,
        format: ArchiveFormat,
    ): HostOutputTransaction
}

internal object ImmediateArchiveOutputCommitter : ArchiveOutputCommitter {
    override fun commitOutput(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        format: ArchiveFormat,
    ): HostOutputTransaction = session.commitOutput(transaction.id, format)

    override fun commitExactOutput(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        expectedDisplayName: String,
        format: ArchiveFormat,
    ): HostOutputTransaction = session.commitExactOutput(transaction.id, expectedDisplayName)
}

/**
 * Defers publication until every physical output in one creation plan has been verified.
 *
 * A one-file plan falls back to the existing single-output commit. Two or more files use the
 * durable v15 host batch, so a split ZIP and a set of separately created archives share the same
 * all-published or all-rolled-back boundary.
 */
internal class ArchiveOutputBatchCommitter(
    private val format: ArchiveFormat,
) : ArchiveOutputCommitter {

    private data class Pending(
        val session: ExplorerActionHostSessionClient,
        val transaction: HostOutputTransaction,
        val commitSingle: () -> HostOutputTransaction,
    )

    private val pending = ArrayList<Pending>()
    private var session: ExplorerActionHostSessionClient? = null
    private var batchId: String? = null
    private var terminal = false

    fun requireCapacity(additionalOutputs: Int, outputDisplayName: String) {
        require(additionalOutputs > 0)
        if (additionalOutputs > ExplorerActionProtocol.MAX_OUTPUT_BATCH_ENTRIES - pending.size) {
            throw ArchiveCreationOutputException(
                operation = ArchiveCreationOutputOperation.COMMIT,
                outputDisplayName = outputDisplayName,
                cause = IOException("Archive output batch exceeds the negotiated member limit"),
                format = format,
            )
        }
    }

    override fun commitOutput(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        format: ArchiveFormat,
    ): HostOutputTransaction {
        require(format == this.format) { "Archive output batch format changed" }
        return stage(session, transaction) {
            session.commitOutput(transaction.id, format)
        }
    }

    override fun commitExactOutput(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        expectedDisplayName: String,
        format: ArchiveFormat,
    ): HostOutputTransaction {
        require(format == this.format) { "Archive output batch format changed" }
        require(transaction.displayName == expectedDisplayName) {
            "Archive output batch exact name changed"
        }
        return stage(session, transaction) {
            session.commitExactOutput(transaction.id, expectedDisplayName)
        }
    }

    fun commitAll(checkCancelled: () -> Unit): List<HostOutputTransaction> {
        check(!terminal) { "Archive output batch is already terminal" }
        check(pending.isNotEmpty()) { "Archive output batch has no members" }
        checkCancelled()
        if (pending.size == 1) {
            val member = pending.single()
            return try {
                listOf(member.commitSingle()).also {
                    terminal = true
                    pending.clear()
                }
            } catch (error: Throwable) {
                throw mapCreationOutputFailure(
                    operation = ArchiveCreationOutputOperation.COMMIT,
                    outputDisplayName = member.transaction.displayName,
                    format = format,
                    error = error,
                )
            }
        }

        val activeSession = requireNotNull(session)
        val transactions = pending.map(Pending::transaction)
        try {
            val prepared = activeSession.prepareOutputBatch(transactions)
            batchId = prepared.id
            check(
                prepared.state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_PREPARED,
            ) { "Host output batch was not prepared" }
            checkCancelled()
            val committed = activeSession.commitOutputBatch(prepared.id, transactions)
            check(
                committed.state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTED &&
                    committed.publishedCount == transactions.size,
            ) { "Host output batch was not completely committed" }
            terminal = true
            pending.clear()
            return committed.outputs
        } catch (error: Throwable) {
            val recoveredCommit = runCatching {
                batchId?.let(activeSession::queryOutputBatch)
            }.getOrNull()
            if (
                recoveredCommit?.state == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTED &&
                recoveredCommit.transactionIds == transactions.map(HostOutputTransaction::id) &&
                recoveredCommit.publishedCount == transactions.size
            ) {
                terminal = true
                pending.clear()
                return transactions.map { transaction ->
                    transaction.copy(state = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED)
                }
            }
            throw mapCreationOutputFailure(
                operation = ArchiveCreationOutputOperation.COMMIT,
                outputDisplayName = transactions.first().displayName,
                format = format,
                error = error,
            )
        }
    }

    /** Returns the original failure, or a typed cleanup failure when rollback is unconfirmed. */
    fun abortPending(operationFailure: Throwable): Throwable {
        if (terminal || pending.isEmpty()) return operationFailure
        val residual = pending.map(Pending::transaction).toMutableList()
        val failures = mutableListOf<Throwable>()
        val activeSession = session
        val activeBatchId = batchId
        if (activeSession != null && activeBatchId != null) {
            val state = runCatching { activeSession.queryOutputBatch(activeBatchId)?.state }
                .onFailure(failures::add)
                .getOrNull()
            when (state) {
                ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTED -> {
                    terminal = true
                    pending.clear()
                    return operationFailure
                }
                ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_ABORTED -> residual.clear()
                else -> {
                    val abortFailure = runCatching {
                        activeSession.abortOutputBatch(activeBatchId)
                    }.exceptionOrNull()
                    if (abortFailure == null) {
                        residual.clear()
                        failures.clear()
                    } else {
                        failures += abortFailure
                    }
                }
            }
        } else {
            pending.forEach { member ->
                val abortFailure = runCatching {
                    member.session.abortOutput(member.transaction.id)
                }.exceptionOrNull()
                if (abortFailure == null) {
                    residual.removeAll { transaction -> transaction.id == member.transaction.id }
                } else {
                    failures += abortFailure
                }
            }
        }
        if (residual.isEmpty()) {
            terminal = true
            pending.clear()
            return operationFailure
        }
        return ArchiveCreationOutputGroupRollbackException(
            committedOutputs = emptyList(),
            residualOutputs = residual,
            totalOutputs = pending.size,
            operationFailure = operationFailure,
            rollbackFailures = if (failures.isEmpty()) {
                listOf(IOException("Archive output batch cleanup could not be confirmed"))
            } else {
                failures
            },
            format = format,
        )
    }

    private fun stage(
        session: ExplorerActionHostSessionClient,
        transaction: HostOutputTransaction,
        commitSingle: () -> HostOutputTransaction,
    ): HostOutputTransaction {
        check(!terminal && batchId == null) { "Archive output batch is no longer writable" }
        requireCapacity(1, transaction.displayName)
        check(pending.none { member -> member.transaction.id == transaction.id }) {
            "Archive output batch contains a duplicate transaction"
        }
        val existingSession = this.session
        check(existingSession == null || existingSession === session) {
            "Archive output batch spans different host session clients"
        }
        this.session = session
        pending += Pending(session, transaction, commitSingle)
        return transaction.copy(state = ExplorerActionHostSessionValues.OUTPUT_STATE_VERIFYING)
    }
}
