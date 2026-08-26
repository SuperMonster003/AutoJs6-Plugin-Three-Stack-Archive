package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import kotlinx.coroutines.CancellationException
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOperationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback

/** Executes one non-interactive extraction into a rollback-safe host v9 output tree. */
internal class ExplorerArchiveExtractionOperation(
    private val operationId: String,
    private val displayName: String,
    private val source: ArchiveReadSource,
    private val snapshot: ArchiveSnapshot,
    private val selectedPaths: Set<String>,
    private val outputSession: IExplorerActionHostSession,
    private val callback: IExplorerArchiveOperationCallback,
) {

    suspend fun run() {
        var hostClient: ExplorerActionHostSessionClient? = null
        try {
            hostClient = ExplorerActionHostSessionClient(outputSession)
            val result = ArchiveExtractor().extractToWriter(
                source = source,
                snapshot = snapshot,
                selectedPaths = selectedPaths,
                rootName = ArchiveExtractionNaming.rootName(displayName, snapshot.format),
                writer = HostArchiveOutputWriter(hostClient),
                skipUnsafePaths = false,
                allowResourceBudgetOverride = false,
                conflictPolicy = ArchiveExtractionConflictPolicy.AUTO_RENAME,
                progress = ArchiveProgressListener(::publishProgress),
            )
            publishCompleted(result)
        } catch (error: CancellationException) {
            publishFailure(
                code = ExplorerArchiveSessionValues.EXTRACTION_ERROR_CANCELLED,
                message = "Archive extraction was cancelled",
            )
        } catch (error: Throwable) {
            publishFailure(error)
        } finally {
            hostClient?.let { client -> runCatching(client::close) }
        }
    }

    private fun publishProgress(progress: ExtractionProgress) {
        val phase = when (progress.phase) {
            ExtractionPhase.PREPARING -> ExplorerArchiveSessionValues.EXTRACTION_PHASE_PREPARING
            ExtractionPhase.EXTRACTING -> ExplorerArchiveSessionValues.EXTRACTION_PHASE_EXTRACTING
            ExtractionPhase.COMMITTING -> ExplorerArchiveSessionValues.EXTRACTION_PHASE_COMMITTING
            ExtractionPhase.CLEANING_UP,
            ExtractionPhase.CLEANUP_FAILED,
            -> ExplorerArchiveSessionValues.EXTRACTION_PHASE_CLEANING_UP
            ExtractionPhase.COMPLETED -> return
        }
        callback.onProgress(
            Bundle().apply {
                putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                putInt(ExplorerArchiveOperationKeys.PHASE, phase)
                progress.currentPath?.let { path ->
                    putString(ExplorerArchiveOperationKeys.CURRENT_ENTRY, sanitizeMessage(path))
                }
                putInt(ExplorerArchiveOperationKeys.COMPLETED_ENTRIES, progress.completedEntries)
                putInt(ExplorerArchiveOperationKeys.TOTAL_ENTRIES, progress.totalEntries)
                putLong(ExplorerArchiveOperationKeys.BYTES_WRITTEN, progress.bytesWritten)
                putLong(ExplorerArchiveOperationKeys.TOTAL_BYTES, progress.totalBytes)
            },
        )
    }

    private fun publishCompleted(result: ExtractionResult) {
        callback.onCompleted(
            Bundle().apply {
                putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                putString(
                    ExplorerArchiveOperationKeys.OUTPUT_DISPLAY_NAME,
                    sanitizeMessage(result.root.displayName),
                )
                putString(
                    ExplorerArchiveOperationKeys.OUTPUT_DISPLAY_PATH,
                    sanitizeMessage(
                        result.root.identifier,
                        ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH,
                    ),
                )
                putInt(ExplorerArchiveOperationKeys.FILES_EXTRACTED, result.filesExtracted)
                putInt(ExplorerArchiveOperationKeys.DIRECTORIES_CREATED, result.directoriesCreated)
                putInt(ExplorerArchiveOperationKeys.ENTRIES_SKIPPED, result.entriesSkipped)
                putLong(ExplorerArchiveOperationKeys.BYTES_WRITTEN, result.bytesWritten)
            },
        )
    }

    private fun publishFailure(error: Throwable) {
        val archiveError = error as? ArchiveException
        val code = when (archiveError?.code) {
            ArchiveFailureCode.PASSWORD_REQUIRED,
            ArchiveFailureCode.WRONG_PASSWORD,
            ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
            ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
            ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_INTERACTION_REQUIRED

            ArchiveFailureCode.SOURCE_CHANGED,
            ArchiveFailureCode.SOURCE_NOT_FILE,
            ArchiveFailureCode.SOURCE_UNREADABLE,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_SOURCE_CHANGED

            ArchiveFailureCode.UNSUPPORTED_METHOD,
            ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET,
            ArchiveFailureCode.MISSING_VOLUME,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_UNSUPPORTED

            ArchiveFailureCode.INVALID_DESTINATION_NAME,
            ArchiveFailureCode.OUTPUT_FAILURE,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_OUTPUT

            else -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_UNKNOWN
        }
        val message = archiveError?.let { "${it.code.name}: ${it.message.orEmpty()}" }
            ?: error.message
            ?: "Archive extraction failed"
        publishFailure(code, message)
    }

    private fun publishFailure(code: Int, message: String) {
        runCatching {
            callback.onFailed(
                Bundle().apply {
                    putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                    putInt(ExplorerArchiveOperationKeys.ERROR_CODE, code)
                    putString(ExplorerArchiveOperationKeys.ERROR_MESSAGE, sanitizeMessage(message))
                },
            )
        }
    }

    private fun sanitizeMessage(
        value: String,
        maxLength: Int = ExplorerActionProtocol.MAX_ARCHIVE_OPERATION_MESSAGE_LENGTH,
    ): String = value
        .replace(Regex("[\\p{Cc}\\p{Cf}]+"), " ")
        .trim()
        .take(maxLength)
        .ifEmpty { "Archive extraction" }
}
