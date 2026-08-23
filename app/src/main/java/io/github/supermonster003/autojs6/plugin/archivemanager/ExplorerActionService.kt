package io.github.supermonster003.autojs6.plugin.archivemanager

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
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
            var staged = ArchiveCacheStager.stage(source, cacheDir, reportedSize)
            try {
                val snapshot = try {
                    ArchiveScanner().scan(staged.source)
                } catch (_: ArchiveLocalFileRequiredException) {
                    val cached = ArchiveCacheStager.materialize(staged, cacheDir)
                    staged.close()
                    staged = cached
                    ArchiveScanner().scan(staged.source)
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
                staged.close()
                val diagnostic = ArchiveFailureDiagnostic.from(
                    error = error,
                    stageHint = ArchiveFailureStage.INDEX,
                    archiveDisplayName = displayName,
                )
                throw IllegalStateException(diagnostic.wireSummary(), error)
            }
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
