package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.util.Locale
import java.util.UUID

internal data class ArchiveOpenRequest(
    val archiveUri: Uri,
    val parentUri: Uri,
    val parentDisplayPath: String,
    val requestId: String,
    val displayName: String,
    val targetId: String,
    /** Provider-reported size, or [ArchiveIntentPolicy.SIZE_UNKNOWN] when unavailable. */
    val reportedSize: Long,
    val requestedAction: ArchiveRequestedAction,
    val hostSession: ExplorerActionHostSessionClient?,
)

internal enum class ArchiveRequestedAction {
    OPEN,
    MANAGE,
    EXTRACT_TO,
}

/** Validates the Explorer Action v5 activity envelope used by explicit extraction. */
internal object ArchiveIntentPolicy {

    const val MAX_DISPLAY_NAME_LENGTH = ExplorerActionProtocol.MAX_TARGET_DISPLAY_NAME_LENGTH
    const val SIZE_UNKNOWN = -1L

    private val supportedMimeTypes = ArchiveManagerPlugin.MIME_TYPES.toSet()
    private val supportedFormats = ArchiveEngine.DEFAULT.readableFormats

    fun resolve(intent: Intent): ArchiveOpenRequest? {
        if (intent.action != ExplorerActionPluginActions.EXECUTE) return null
        val requestedAction = when (intent.getStringExtra(ExplorerActionIntentExtras.ACTION_ID)) {
            ArchiveManagerPlugin.ACTION_OPEN_ID -> ArchiveRequestedAction.OPEN
            ArchiveManagerPlugin.ACTION_MANAGE_ID -> ArchiveRequestedAction.MANAGE
            ArchiveManagerPlugin.ACTION_EXTRACT_TO_ID -> ArchiveRequestedAction.EXTRACT_TO
            else -> return null
        }
        if (
            intent.getIntExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, Int.MIN_VALUE) !=
            ExplorerActionProtocol.VERSION
        ) {
            return null
        }
        if (
            intent.getStringExtra(ExplorerActionIntentExtras.SOURCE_SURFACE) !=
            ExplorerActionIntentValues.SOURCE_SURFACE_MAIN
        ) {
            return null
        }

        if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) return null
        val archiveUri = intent.data?.takeIf(::isUsableContentUri) ?: return null
        val parentUri = intent.parcelableUriExtra(ExplorerActionIntentExtras.PARENT_URI)
            ?.takeIf(::isUsableContentUri)
            ?: return null
        // Content URI path segments are provider-defined capabilities, not filesystem paths.
        // DocumentsProvider commonly represents a parent as /tree/... and its child as
        // /document/..., so string-prefix ancestry checks reject otherwise valid requests.
        if (parentUri.authority != archiveUri.authority) return null

        val requestId = validateRequestId(
            intent.getStringExtra(ExplorerActionIntentExtras.REQUEST_ID),
        ) ?: return null
        val parentDisplayPath = validateParentDisplayPath(
            intent.getStringExtra(ExplorerActionIntentExtras.PARENT_DISPLAY_PATH),
        ) ?: return null
        val targets = intent.parcelableBundleArrayListExtra(ExplorerActionIntentExtras.TARGETS)
            ?.takeIf { it.size == 1 }
            ?: return null
        val target = targets.single()
        val targetId = validateOpaqueTargetId(target.getString(ExplorerActionTargetKeys.ID)) ?: return null
        if (
            target.getInt(ExplorerActionTargetKeys.KIND, Int.MIN_VALUE) !=
            ExplorerActionValues.TARGET_FILE
        ) {
            return null
        }
        val bundledArchiveUri = target.parcelableUri(ExplorerActionTargetKeys.URI)
            ?.takeIf(::isUsableContentUri)
            ?: return null
        if (bundledArchiveUri != archiveUri) return null

        val clipData = intent.clipData ?: return null
        if (clipData.itemCount != ExplorerActionIntentValues.v4ClipItemCount(targets.size)) return null
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_TARGET_INDEX).uri != archiveUri) {
            return null
        }

        val displayName = validateDisplayName(target.getString(ExplorerActionTargetKeys.DISPLAY_NAME))
            ?: return null
        if (
            validateDisplayName(intent.getStringExtra(ExplorerActionIntentExtras.DISPLAY_NAME)) !=
            displayName
        ) {
            return null
        }
        val targetMimeType = normalizeMimeType(target.getString(ExplorerActionTargetKeys.MIME_TYPE))
            ?: return null
        if (normalizeMimeType(intent.type) != targetMimeType) return null
        if (!isSupportedArchive(targetMimeType, displayName)) return null
        if (
            requestedAction == ArchiveRequestedAction.MANAGE &&
            !isSupportedZipArchive(displayName)
        ) {
            return null
        }

        if (!target.containsKey(ExplorerActionTargetKeys.SIZE)) return null
        val reportedSize = target.getLong(ExplorerActionTargetKeys.SIZE, SIZE_UNKNOWN)
        if (!isReportedSizeAccepted(reportedSize)) return null
        if (!intent.hasExtra(ExplorerActionIntentExtras.SIZE)) return null
        if (intent.getLongExtra(ExplorerActionIntentExtras.SIZE, Long.MIN_VALUE) != reportedSize) return null
        if (!target.containsKey(ExplorerActionTargetKeys.LAST_MODIFIED)) return null
        if (target.getLong(ExplorerActionTargetKeys.LAST_MODIFIED, Long.MIN_VALUE) < SIZE_UNKNOWN) return null
        val hostSession = if (
            requestedAction == ArchiveRequestedAction.MANAGE ||
            requestedAction == ArchiveRequestedAction.EXTRACT_TO
        ) {
            resolveHostSession(intent) ?: return null
        } else {
            null
        }

        return ArchiveOpenRequest(
            archiveUri = archiveUri,
            parentUri = parentUri,
            parentDisplayPath = parentDisplayPath,
            requestId = requestId,
            displayName = displayName,
            targetId = targetId,
            reportedSize = reportedSize,
            requestedAction = requestedAction,
            hostSession = hostSession,
        )
    }

    fun validateDisplayName(value: String?): String? {
        val name = value ?: return null
        if (name.length !in 1..MAX_DISPLAY_NAME_LENGTH) return null
        if (name == "." || name == ".." || name.isBlank()) return null
        if (name.any { it == '/' || it == '\\' || it == '\u0000' || it.code < 0x20 || it.code == 0x7f }) {
            return null
        }
        return name
    }

    fun isReportedSizeAccepted(size: Long): Boolean = size >= SIZE_UNKNOWN

    fun isSupportedArchive(mimeType: String?, displayName: String): Boolean {
        val normalizedMimeType = normalizeMimeType(mimeType)
        val extensionMatches = supportedFormats.any { format -> format.matchesFileName(displayName) }
        return normalizedMimeType in supportedMimeTypes ||
            extensionMatches ||
            NumberedArchiveVolumePolicy.matchesFirstVolume(displayName)
    }

    private fun isSupportedZipArchive(displayName: String): Boolean =
        displayName.substringAfterLast('.', missingDelimiterValue = "")
            .equals(ArchiveFormat.ZIP.primaryExtension, ignoreCase = true)

    private fun isUsableContentUri(uri: Uri): Boolean =
        uri.isHierarchical &&
            uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true) &&
            !uri.authority.isNullOrBlank()

    private fun validateRequestId(value: String?): String? {
        val requestId = value?.takeIf { it.length <= MAX_REQUEST_ID_LENGTH } ?: return null
        val parsed = runCatching { UUID.fromString(requestId) }.getOrNull() ?: return null
        return requestId.takeIf { parsed.toString().equals(requestId, ignoreCase = true) }
    }

    private fun validateParentDisplayPath(value: String?): String? {
        val path = value ?: return null
        if (path.length !in 1..ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH) return null
        if (path.any { it == '\u0000' || it.code < 0x20 || it.code == 0x7f }) return null
        return path
    }

    private fun validateOpaqueTargetId(value: String?): String? {
        val id = value ?: return null
        if (id.length !in 1..ExplorerActionProtocol.MAX_TARGET_ID_LENGTH) return null
        if (id.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7f }) return null
        return id
    }

    private fun normalizeMimeType(value: String?): String? {
        val normalized = value
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.length <= MAX_MIME_TYPE_LENGTH }
            ?: return null
        return normalized.takeIf(MIME_TYPE_PATTERN::matches)
    }

    private fun resolveHostSession(intent: Intent): ExplorerActionHostSessionClient? {
        val binder = intent.getBundleExtra(ExplorerActionIntentExtras.HOST_SESSION)
            ?.getBinder(ExplorerActionHostSessionKeys.BINDER)
            ?: return null
        val remote = IExplorerActionHostSession.Stub.asInterface(binder) ?: return null
        return runCatching { ExplorerActionHostSessionClient(remote) }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableUriExtra(name: String): Uri? = getParcelableExtra(name)

    @Suppress("DEPRECATION")
    private fun Intent.parcelableBundleArrayListExtra(name: String): ArrayList<Bundle>? =
        getParcelableArrayListExtra(name)

    @Suppress("DEPRECATION")
    private fun Bundle.parcelableUri(name: String): Uri? = getParcelable(name)

    private const val MAX_REQUEST_ID_LENGTH = 36
    private const val MAX_MIME_TYPE_LENGTH = 255
    private val MIME_TYPE_PATTERN = Regex(
        "(?:[a-z0-9][a-z0-9!#$&^_.+-]*|\\*)/(?:[a-z0-9][a-z0-9!#$&^_.+-]*|\\*)",
    )
}
