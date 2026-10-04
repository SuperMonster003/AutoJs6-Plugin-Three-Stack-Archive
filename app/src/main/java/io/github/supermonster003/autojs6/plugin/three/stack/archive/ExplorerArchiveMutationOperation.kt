package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveMutationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback
import java.io.File

internal data class ExplorerArchiveMutationOutcome(
    val stagedArchive: StagedArchive,
    val snapshot: ArchiveSnapshot,
    val committedSize: Long,
)

/** Executes one v20 mutation through the host's atomic target-replacement transaction. */
internal class ExplorerArchiveMutationOperation(
    private val operationId: String,
    private val displayName: String,
    private val cacheDirectory: File,
    private val source: ArchiveReadSource,
    private val snapshot: ArchiveSnapshot,
    private val request: ArchiveMutationRequest,
    private val outputSession: IExplorerActionHostSession,
    private val callback: IExplorerArchiveOperationCallback,
    private val beforeOutputVerification: () -> Unit = {},
) {

    suspend fun run(): ExplorerArchiveMutationOutcome {
        val coroutineContext = currentCoroutineContext()
        val hostSession = ExplorerActionHostSessionClient(outputSession)
        try {
            val provider = ArchiveEngine.DEFAULT.createMutationProvider(
                format = snapshot.format,
                session = hostSession,
                cacheDirectory = cacheDirectory,
            )
            val prepared = provider.prepare(snapshot, request)
            val committed = provider.execute(
                source = source,
                snapshot = snapshot,
                targetId = SOURCE_TARGET_ID,
                displayName = displayName,
                prepared = prepared,
                checkCancelled = coroutineContext::ensureActive,
                progress = ArchiveMutationProgressListener(::reportProgress),
                beforeOutputVerification = beforeOutputVerification,
            )
            val committedSize = requireNotNull(committed.size) {
                "Host replacement result has no archive size"
            }
            require(committedSize >= 0L) { "Host replacement result has an invalid archive size" }
            reportProgress(
                ArchiveMutationProgress(
                    phase = ArchiveMutationPhase.COMMITTING,
                    currentPath = null,
                    completedEntries = prepared.workEstimate.resultEntryCount,
                    totalEntries = prepared.workEstimate.resultEntryCount,
                ),
                ExplorerArchiveSessionValues.MUTATION_PHASE_REINDEXING,
            )
            val replacement = ArchiveCacheStager.stage(
                source = hostSession.openFile(SOURCE_TARGET_ID, ArchivePathPolicy.ROOT_PATH),
                cacheDirectory = cacheDirectory,
                reportedSize = committedSize,
            )
            return try {
                val password = snapshot.readerOptions.passwordChars()
                val options = try {
                    ArchiveReaderOptions(
                        filenameCharsetName = snapshot.readerOptions.filenameCharsetName,
                        password = password,
                    )
                } finally {
                    password?.fill('\u0000')
                }
                val rescanned = try {
                    ArchiveScanner(snapshot.structureLimits).scan(replacement.source, options)
                } finally {
                    options.clearPassword()
                }
                check(rescanned.format == snapshot.format) {
                    "Archive format changed after host replacement"
                }
                ExplorerArchiveMutationOutcome(replacement, rescanned, committedSize)
            } catch (error: Throwable) {
                replacement.close()
                throw error
            }
        } finally {
            runCatching { hostSession.close() }
        }
    }

    private fun reportProgress(progress: ArchiveMutationProgress) {
        reportProgress(progress, progress.phase.toProtocolPhase())
    }

    private fun reportProgress(progress: ArchiveMutationProgress, phase: Int) {
        callback.onProgress(
            Bundle().apply {
                putString(ExplorerArchiveMutationKeys.OPERATION_ID, operationId)
                putInt(ExplorerArchiveMutationKeys.PHASE, phase)
                progress.currentPath?.let { path ->
                    putString(
                        ExplorerArchiveMutationKeys.CURRENT_ENTRY,
                        path.take(ExplorerActionProtocol.MAX_ARCHIVE_OPERATION_MESSAGE_LENGTH),
                    )
                }
                putInt(ExplorerArchiveMutationKeys.COMPLETED_ENTRIES, progress.completedEntries)
                putInt(ExplorerArchiveMutationKeys.TOTAL_ENTRIES, progress.totalEntries)
            },
        )
    }

    private fun ArchiveMutationPhase.toProtocolPhase(): Int = when (this) {
        ArchiveMutationPhase.PREPARING -> ExplorerArchiveSessionValues.MUTATION_PHASE_PREPARING
        ArchiveMutationPhase.WRITING -> ExplorerArchiveSessionValues.MUTATION_PHASE_WRITING
        ArchiveMutationPhase.VERIFYING -> ExplorerArchiveSessionValues.MUTATION_PHASE_VERIFYING
        ArchiveMutationPhase.COMMITTING -> ExplorerArchiveSessionValues.MUTATION_PHASE_COMMITTING
    }

    private companion object {
        const val SOURCE_TARGET_ID = "archive-source"
    }
}
