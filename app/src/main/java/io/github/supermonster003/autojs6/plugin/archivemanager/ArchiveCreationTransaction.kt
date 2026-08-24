package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import java.util.concurrent.CancellationException

/** The host operation that failed while creating an archive output transaction. */
internal enum class ArchiveCreationOutputOperation {
    PREPARE,
    OPEN,
    WRITE,
    COMMIT,
}

/**
 * A typed failure at one boundary of the host-owned output transaction.
 *
 * Source and cancellation failures remain primary and are not converted into output failures.
 */
internal class ArchiveCreationOutputException(
    val operation: ArchiveCreationOutputOperation,
    val outputDisplayName: String,
    cause: Throwable,
    format: ArchiveFormat,
) : ArchiveException(
    code = ArchiveFailureCode.OUTPUT_FAILURE,
    message = when (operation) {
        ArchiveCreationOutputOperation.PREPARE -> "Archive output could not be reserved"
        ArchiveCreationOutputOperation.OPEN -> "Archive output could not be opened"
        ArchiveCreationOutputOperation.WRITE -> "Archive output could not be written"
        ArchiveCreationOutputOperation.COMMIT -> "Archive output could not be committed"
    },
    cause = cause,
    format = format,
    stage = ArchiveFailureStage.OUTPUT,
)

/**
 * The archive operation failed and aborting its host output transaction failed as well.
 *
 * The operation failure remains the cause. The abort failure is both exposed explicitly and
 * suppressed so diagnostics retain both independent failures without losing the primary reason.
 */
internal class ArchiveCreationRollbackException(
    val pendingOutputDisplayName: String,
    val pendingOutputDisplayPath: String,
    val operationFailure: Throwable,
    val rollbackFailure: Throwable,
    format: ArchiveFormat,
) : ArchiveException(
    code = ArchiveFailureCode.OUTPUT_FAILURE,
    message = "Archive creation failed and temporary output cleanup could not be confirmed",
    cause = operationFailure,
    format = format,
    stage = ArchiveFailureStage.CLEANUP,
) {
    init {
        if (rollbackFailure !== operationFailure) addSuppressed(rollbackFailure)
    }
}

/** A host source could not be opened, read, or verified while creating an archive. */
internal class ArchiveCreationSourceException(
    val sourceArchivePath: String,
    message: String,
    cause: Throwable? = null,
    code: ArchiveFailureCode = ArchiveFailureCode.SOURCE_UNREADABLE,
) : ArchiveException(
    code = code,
    message = message,
    cause = cause,
    stage = ArchiveFailureStage.INPUT,
) {
    init {
        require(
            code == ArchiveFailureCode.SOURCE_UNREADABLE ||
                code == ArchiveFailureCode.SOURCE_CHANGED,
        ) { "Archive creation source failures require a source failure code" }
    }
}

/** A committed host output paired with the values collected while writing it. */
internal data class CommittedArchiveOutput<T>(
    val transaction: HostOutputTransaction,
    val value: T,
)

/**
 * Runs the shared prepare/open/write/commit state machine used by every archive creator.
 *
 * Once preparation succeeds, every non-committed path asks the host to abort exactly once. A
 * failed abort becomes a typed cleanup failure instead of disappearing in a suppressed exception.
 */
internal fun <T> ExplorerActionHostSessionClient.writeArchiveOutput(
    outputDisplayName: String,
    format: ArchiveFormat,
    conflictPolicy: ArchiveCreationConflictPolicy,
    write: (ParcelFileDescriptor) -> T,
): CommittedArchiveOutput<T> {
    val prepared = try {
        prepareOutput(outputDisplayName, format, conflictPolicy)
    } catch (error: ArchiveOutputNameUnavailableException) {
        throw error
    } catch (error: Throwable) {
        throw mapCreationOutputFailure(
            operation = ArchiveCreationOutputOperation.PREPARE,
            outputDisplayName = outputDisplayName,
            format = format,
            error = error,
        )
    }

    try {
        val descriptor = runCreationOutputOperation(
            operation = ArchiveCreationOutputOperation.OPEN,
            outputDisplayName = prepared.displayName,
            format = format,
        ) {
            openOutput(prepared.id)
        }
        val value = runCreationOutputOperation(
            operation = ArchiveCreationOutputOperation.WRITE,
            outputDisplayName = prepared.displayName,
            format = format,
        ) {
            descriptor.use(write)
        }
        val committed = runCreationOutputOperation(
            operation = ArchiveCreationOutputOperation.COMMIT,
            outputDisplayName = prepared.displayName,
            format = format,
        ) {
            commitOutput(prepared.id, format)
        }
        return CommittedArchiveOutput(committed, value)
    } catch (operationFailure: Throwable) {
        val rollbackFailure = runCatching { abortOutput(prepared.id) }.exceptionOrNull()
        if (rollbackFailure != null) {
            throw ArchiveCreationRollbackException(
                pendingOutputDisplayName = prepared.displayName,
                pendingOutputDisplayPath = prepared.displayPath,
                operationFailure = operationFailure,
                rollbackFailure = rollbackFailure,
                format = format,
            )
        }
        throw operationFailure
    }
}

private inline fun <T> runCreationOutputOperation(
    operation: ArchiveCreationOutputOperation,
    outputDisplayName: String,
    format: ArchiveFormat,
    block: () -> T,
): T = try {
    block()
} catch (error: Throwable) {
    throw mapCreationOutputFailure(operation, outputDisplayName, format, error)
}

private fun mapCreationOutputFailure(
    operation: ArchiveCreationOutputOperation,
    outputDisplayName: String,
    format: ArchiveFormat,
    error: Throwable,
): Throwable = when (error) {
    is CancellationException,
    is ArchiveException,
    is Error,
    -> error
    else -> ArchiveCreationOutputException(operation, outputDisplayName, error, format)
}
