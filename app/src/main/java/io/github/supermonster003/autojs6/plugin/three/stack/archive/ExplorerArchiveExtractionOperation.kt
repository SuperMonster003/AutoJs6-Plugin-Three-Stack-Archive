package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOperationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback

/** Executes one extraction attempt into a rollback-safe host v9 output tree. */
internal class ExplorerArchiveExtractionOperation(
    private val operationId: String,
    private val displayName: String,
    private val source: ArchiveReadSource,
    private val snapshot: ArchiveSnapshot,
    private val selectedPaths: Set<String>,
    private val options: ExplorerArchiveExtractionRequestOptions =
        ExplorerArchiveExtractionRequestOptions(),
    private val resourceBudget: ArchiveResourceBudget = ArchiveResourceBudget.COMPATIBLE,
    private val outputSession: IExplorerActionHostSession,
    private val callback: IExplorerArchiveOperationCallback,
) {

    suspend fun run() {
        var hostClient: ExplorerActionHostSessionClient? = null
        var attemptSnapshot: ArchiveSnapshot? = null
        try {
            attemptSnapshot = snapshotForAttempt()
            hostClient = ExplorerActionHostSessionClient(outputSession)
            val result = ArchiveExtractor(resourceBudget = resourceBudget).extractToWriter(
                source = source,
                snapshot = attemptSnapshot,
                selectedPaths = selectedPaths,
                rootName = ArchiveExtractionNaming.rootName(displayName, attemptSnapshot.format),
                writer = HostArchiveOutputWriter(hostClient),
                skipUnsafePaths = options.skipUnsafePaths,
                allowResourceBudgetOverride = options.allowResourceBudgetOverride,
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
            attemptSnapshot
                ?.takeUnless { it === snapshot }
                ?.readerOptions
                ?.clearPassword()
        }
    }

    private suspend fun snapshotForAttempt(): ArchiveSnapshot {
        val password = options.passwordChars() ?: return snapshot
        val readerOptions = try {
            ArchiveReaderOptions(
                filenameCharsetName = snapshot.readerOptions.filenameCharsetName,
                password = password,
            )
        } finally {
            password.fill('\u0000')
        }
        val coroutineContext = currentCoroutineContext()
        val rescanned = try {
            ArchiveScanner(snapshot.structureLimits).scan(source, readerOptions) {
                coroutineContext.ensureActive()
            }
        } finally {
            readerOptions.clearPassword()
        }
        return try {
            ArchiveRetrySnapshotValidator.requireSameArchive(snapshot, rescanned)
            rescanned
        } catch (error: Throwable) {
            rescanned.readerOptions.clearPassword()
            throw error
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
        val interactionKind = archiveError?.code?.extractionInteractionKind()
        val code = when {
            interactionKind != null ->
                ExplorerArchiveSessionValues.EXTRACTION_ERROR_INTERACTION_REQUIRED

            else -> when (archiveError?.code) {
            ArchiveFailureCode.SOURCE_CHANGED,
            ArchiveFailureCode.SOURCE_NOT_FILE,
            ArchiveFailureCode.SOURCE_UNREADABLE,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_SOURCE_CHANGED

            ArchiveFailureCode.UNSUPPORTED_METHOD,
            ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET,
            ArchiveFailureCode.MISSING_VOLUME,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_UNSUPPORTED

            ArchiveFailureCode.INVALID_DESTINATION_NAME,
            ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
            ArchiveFailureCode.OUTPUT_FAILURE,
            -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_OUTPUT

            else -> ExplorerArchiveSessionValues.EXTRACTION_ERROR_UNKNOWN
            }
        }
        val message = archiveError?.let { "${it.code.name}: ${it.message.orEmpty()}" }
            ?: error.message
            ?: "Archive extraction failed"
        publishFailure(code, message, interactionKind)
    }

    private fun publishFailure(code: Int, message: String, interactionKind: Int? = null) {
        runCatching {
            callback.onFailed(
                Bundle().apply {
                    putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                    putInt(ExplorerArchiveOperationKeys.ERROR_CODE, code)
                    putString(ExplorerArchiveOperationKeys.ERROR_MESSAGE, sanitizeMessage(message))
                    interactionKind?.let { kind ->
                        putInt(ExplorerArchiveOperationKeys.INTERACTION_KIND, kind)
                    }
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

internal fun ArchiveFailureCode.extractionInteractionKind(): Int? = when (this) {
    ArchiveFailureCode.PASSWORD_REQUIRED ->
        ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_PASSWORD_REQUIRED
    ArchiveFailureCode.WRONG_PASSWORD ->
        ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_WRONG_PASSWORD
    ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED ->
        ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_UNSAFE_PATHS
    ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED ->
        ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_RESOURCE_BUDGET
    else -> null
}
