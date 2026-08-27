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
        var volumeClient: ExplorerArchiveVolumeSourceClient? = null
        var staged: StagedArchive? = null
        var snapshot: ArchiveSnapshot? = null
        try {
            volumeClient = try {
                ExplorerArchiveVolumeSourceClient.fromBinder(
                    binder = request.getBinder(ExplorerArchiveRequestKeys.VOLUME_SOURCE_BINDER),
                    cacheDirectory = cacheDir,
                )
            } finally {
                request.remove(ExplorerArchiveRequestKeys.VOLUME_SOURCE_BINDER)
            }
            staged = ArchiveCacheStager.stage(source, cacheDir, reportedSize)
            var numberedResolution: NumberedArchiveVolumeResolution? = null
            val numberedInfo = NumberedArchiveVolumePolicy.inspectFirstVolume(displayName)
            var sessionSource = if (numberedInfo == null) {
                sourceWithVolumes(staged.source, displayName, volumeClient)
            } else {
                numberedInfo.resolve(staged.source, volumeClient).also { resolution ->
                    numberedResolution = resolution
                }.source
            }
            if (numberedInfo == null) {
                val splitZip = ZipSplitArchiveDetector.inspect(sessionSource)
                if (splitZip != null && volumeClient != null) {
                    val requiredNames = splitZip.materializationVolumeNames(
                        displayName = displayName,
                        availableNames = volumeClient.volumes.map(ArchiveVolumeIdentity::displayName),
                    )
                    if (requiredNames != null) {
                        val group = ArchiveCacheStager.materializeVolumeGroup(
                            primary = staged,
                            primaryDisplayName = displayName,
                            volumeSet = volumeClient,
                            requiredVolumeNames = requiredNames,
                            cacheDirectory = cacheDir,
                        )
                        if (group != null) {
                            staged.close()
                            volumeClient.close()
                            volumeClient = null
                            staged = group
                            sessionSource = group.source
                        }
                    }
                }
            }
            snapshot = try {
                scanSessionSource(sessionSource, readerOptions, numberedResolution)
            } catch (_: ArchiveLocalFileRequiredException) {
                val cached = if (numberedResolution == null) {
                    ArchiveCacheStager.materialize(staged, cacheDir)
                } else {
                    ArchiveCacheStager.materialize(sessionSource, cacheDir)
                }
                staged.close()
                staged = cached
                sessionSource = numberedResolution?.let { resolution ->
                    volumeClient?.close()
                    volumeClient = null
                    resolution.info.wrapMaterialized(cached.source).also {
                        numberedResolution = resolution.copy(source = it)
                    }
                } ?: sourceWithVolumes(cached.source, displayName, volumeClient)
                scanSessionSource(sessionSource, readerOptions, numberedResolution)
            }
            return ExplorerArchiveSession(
                ownerUid = ownerUid,
                displayName = displayName,
                stagedArchive = staged,
                source = sessionSource,
                volumeLease = volumeClient,
                snapshot = snapshot,
                isolatedPathDisplayName = getString(R.string.text_unsafe_paths_folder),
                onClosed = sessions::remove,
            ).also {
                volumeClient = null
                sessions.add(it)
            }
        } catch (error: Throwable) {
            snapshot?.readerOptions?.clearPassword()
            staged?.close()
            volumeClient?.close()
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

    private fun sourceWithVolumes(
        source: ArchiveReadSource,
        displayName: String,
        volumeSet: ArchiveVolumeSet?,
    ): ArchiveReadSource = if (volumeSet == null) {
        NamedArchiveReadSource(source, displayName)
    } else {
        VolumeAwareArchiveReadSource(source, displayName, volumeSet)
    }

    private fun scanSessionSource(
        source: ArchiveReadSource,
        options: ArchiveReaderOptions,
        numberedResolution: NumberedArchiveVolumeResolution?,
    ): ArchiveSnapshot = try {
        ArchiveScanner().scan(source, options)
    } catch (error: ArchiveValidationException) {
        if (
            numberedResolution != null &&
            error.code in setOf(
                ArchiveFailureCode.INVALID_SIGNATURE,
                ArchiveFailureCode.MALFORMED_ARCHIVE,
            )
        ) {
            throw numberedResolution.info.incompleteOrDamaged(
                error = error,
                lastVolumeIndex = numberedResolution.lastVolumeIndex,
            )
        }
        throw error
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
