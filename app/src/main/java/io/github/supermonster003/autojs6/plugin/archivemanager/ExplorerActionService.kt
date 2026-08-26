package io.github.supermonster003.autojs6.plugin.archivemanager

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenValues
import org.autojs.plugin.explorer.api.ExplorerArchiveRequestKeys
import org.autojs.plugin.explorer.api.IExplorerArchiveSession
import org.autojs.plugin.explorer.api.IExplorerActionPlugin
import java.util.Collections

class ExplorerActionService : Service() {

    private val sessions = Collections.synchronizedSet(mutableSetOf<ExplorerArchiveSession>())

    private val binder = object : IExplorerActionPlugin.Stub() {
        override fun getInfo() = archiveManagerPluginInfo()

        override fun getActionCatalog() = archiveManagerActionCatalog()

        override fun openArchive(
            source: ParcelFileDescriptor,
            request: Bundle,
        ): IExplorerArchiveSession = try {
            openArchiveSession(source, request)
        } catch (failure: ExplorerArchiveOpenFailure) {
            throw IllegalStateException(failure.diagnostic.wireSummary(), failure.cause)
        }

        override fun openArchiveV11(
            source: ParcelFileDescriptor,
            request: Bundle,
        ): Bundle = try {
            val session = openArchiveSession(source, request)
            Bundle().apply {
                putInt(ExplorerArchiveOpenKeys.ERROR_CODE, ExplorerArchiveOpenValues.ERROR_NONE)
                putBinder(ExplorerArchiveOpenKeys.SESSION_BINDER, session.asBinder())
            }
        } catch (failure: ExplorerArchiveOpenFailure) {
            Bundle().apply {
                putInt(
                    ExplorerArchiveOpenKeys.ERROR_CODE,
                    failure.diagnostic.code?.archiveOpenServiceErrorCode()
                        ?: ExplorerArchiveOpenValues.ERROR_UNRECOVERABLE,
                )
                putString(
                    ExplorerArchiveOpenKeys.ERROR_MESSAGE,
                    failure.diagnostic.wireSummary()
                        .replace(Regex("[\\p{Cc}\\p{Cf}]+"), " ")
                        .trim()
                        .take(ExplorerActionProtocol.MAX_ARCHIVE_OPERATION_MESSAGE_LENGTH),
                )
            }
        }
    }

    private fun openArchiveSession(
        source: ParcelFileDescriptor,
        request: Bundle,
    ): IExplorerArchiveSession {
        val ownerUid = Binder.getCallingUid()
        val displayName = ArchiveIntentPolicy.validateDisplayName(
            request.getString(ExplorerArchiveRequestKeys.DISPLAY_NAME),
        )
            ?.takeIf { it.length <= ExplorerActionProtocol.MAX_ARCHIVE_DISPLAY_NAME_LENGTH }
            ?: throw IllegalArgumentException("Archive display name is invalid")
        if (!request.containsKey(ExplorerArchiveRequestKeys.SIZE)) {
            throw IllegalArgumentException("Archive size is missing")
        }
        val reportedSize = request.getLong(ExplorerArchiveRequestKeys.SIZE, -1L)
        if (!ArchiveIntentPolicy.isReportedSizeAccepted(reportedSize)) {
            throw IllegalArgumentException("Archive size is invalid")
        }
        val transientPassword = request.getCharArray(ExplorerArchiveRequestKeys.PASSWORD)
        val readerOptions = try {
            require(
                transientPassword == null ||
                    transientPassword.size in 1..ExplorerActionProtocol.MAX_ARCHIVE_PASSWORD_LENGTH,
            ) { "Archive open password length is invalid" }
            ArchiveReaderOptions(password = transientPassword)
        } finally {
            transientPassword?.fill('\u0000')
            request.remove(ExplorerArchiveRequestKeys.PASSWORD)
        }
        var staged: StagedArchive? = null
        var snapshot: ArchiveSnapshot? = null
        try {
            staged = ArchiveCacheStager.stage(source, cacheDir, reportedSize)
            snapshot = try {
                ArchiveScanner().scan(staged.source, readerOptions)
            } catch (_: ArchiveLocalFileRequiredException) {
                val cached = ArchiveCacheStager.materialize(staged, cacheDir)
                staged.close()
                staged = cached
                ArchiveScanner().scan(staged.source, readerOptions)
            }
            return ExplorerArchiveSession(
                ownerUid = ownerUid,
                displayName = displayName,
                stagedArchive = staged,
                snapshot = snapshot,
                isolatedPathDisplayName = getString(R.string.text_unsafe_paths_folder),
                onClosed = sessions::remove,
            ).also(sessions::add)
        } catch (error: Throwable) {
            snapshot?.readerOptions?.clearPassword()
            staged?.close()
            throw ExplorerArchiveOpenFailure(
                diagnostic = ArchiveFailureDiagnostic.from(
                    error = error,
                    stageHint = ArchiveFailureStage.INDEX,
                    archiveDisplayName = displayName,
                ),
                cause = error,
            )
        } finally {
            readerOptions.clearPassword()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        val activeSessions = synchronized(sessions) { sessions.toList() }
        activeSessions.forEach(ExplorerArchiveSession::closeFromService)
        sessions.clear()
        super.onDestroy()
    }
}

private class ExplorerArchiveOpenFailure(
    val diagnostic: ArchiveFailureDiagnostic,
    override val cause: Throwable,
) : Exception(cause)

internal fun ArchiveFailureCode.archiveOpenServiceErrorCode(): Int? = when (this) {
    ArchiveFailureCode.PASSWORD_REQUIRED -> ExplorerArchiveOpenValues.ERROR_PASSWORD_REQUIRED
    ArchiveFailureCode.WRONG_PASSWORD -> ExplorerArchiveOpenValues.ERROR_WRONG_PASSWORD
    else -> null
}
