package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.util.Locale
import java.util.UUID

internal data class ArchiveCompressionTarget(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val kind: Int,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
)

internal data class ArchiveCompressionRequest(
    val requestId: String,
    val parentUri: Uri,
    val parentDisplayPath: String,
    val targets: List<ArchiveCompressionTarget>,
    val hostSession: IExplorerActionHostSession,
)

/** Validates create-in-parent Explorer Action v4 requests before exposing the host capability. */
internal object ArchiveCompressionIntentPolicy {

    fun resolve(intent: Intent): ArchiveCompressionRequest? {
        if (!ExplorerActionIntentSizePolicy.isSafe(intent)) return null
        if (intent.action != ExplorerActionPluginActions.EXECUTE) return null
        val actionId = intent.getStringExtra(ExplorerActionIntentExtras.ACTION_ID)
        if (
            actionId != ThreeStackArchivePlugin.ACTION_COMPRESS_SINGLE_ID &&
            actionId != ThreeStackArchivePlugin.ACTION_COMPRESS_MULTIPLE_ID
        ) {
            return null
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

        val requestId = canonicalRequestId(
            intent.getStringExtra(ExplorerActionIntentExtras.REQUEST_ID),
        ) ?: return null
        val parentUri = intent.parcelableUriExtra(ExplorerActionIntentExtras.PARENT_URI)
            ?.takeIf(::isUsableContentUri)
            ?: return null
        val parentDisplayPath = validateParentDisplayPath(
            intent.getStringExtra(ExplorerActionIntentExtras.PARENT_DISPLAY_PATH),
        ) ?: return null
        val targetBundles = intent.parcelableBundleArrayListExtra(ExplorerActionIntentExtras.TARGETS)
            ?.takeIf { it.size in 1..ExplorerActionProtocol.MAX_TARGETS_PER_REQUEST }
            ?: return null
        if (actionId == ThreeStackArchivePlugin.ACTION_COMPRESS_SINGLE_ID && targetBundles.size != 1) {
            return null
        }
        val clipData = intent.clipData ?: return null
        if (clipData.itemCount != ExplorerActionIntentValues.v4ClipItemCount(targetBundles.size)) {
            return null
        }

        val ids = HashSet<String>(targetBundles.size)
        val uris = HashSet<Uri>(targetBundles.size)
        val targets = targetBundles.mapIndexed { index, bundle ->
            val id = validateOpaqueId(bundle.getString(ExplorerActionTargetKeys.ID))
                ?: return null
            if (!ids.add(id)) return null
            val uri = bundle.parcelableUri(ExplorerActionTargetKeys.URI)
                ?.takeIf(::isUsableContentUri)
                ?: return null
            if (!uris.add(uri) || uri.authority != parentUri.authority) return null
            if (clipData.getItemAt(index).uri != uri) return null
            val displayName = validateLeafName(bundle.getString(ExplorerActionTargetKeys.DISPLAY_NAME))
                ?: return null
            val kind = bundle.getInt(ExplorerActionTargetKeys.KIND, Int.MIN_VALUE)
            if (kind != ExplorerActionValues.TARGET_FILE && kind != ExplorerActionValues.TARGET_DIRECTORY) {
                return null
            }
            val mimeType = normalizeMimeType(bundle.getString(ExplorerActionTargetKeys.MIME_TYPE))
                ?: return null
            if (!bundle.containsKey(ExplorerActionTargetKeys.SIZE)) return null
            val size = bundle.getLong(ExplorerActionTargetKeys.SIZE, Long.MIN_VALUE)
            if (size < SIZE_UNKNOWN || kind == ExplorerActionValues.TARGET_DIRECTORY && size != SIZE_UNKNOWN) {
                return null
            }
            if (!bundle.containsKey(ExplorerActionTargetKeys.LAST_MODIFIED)) return null
            val lastModified = bundle.getLong(ExplorerActionTargetKeys.LAST_MODIFIED, Long.MIN_VALUE)
            if (lastModified < SIZE_UNKNOWN) return null
            ArchiveCompressionTarget(
                id = id,
                uri = uri,
                displayName = displayName,
                kind = kind,
                mimeType = mimeType,
                size = size,
                lastModified = lastModified,
            )
        }
        if (intent.data != targets.first().uri) return null

        val sessionBundle = intent.parcelableBundleExtra(ExplorerActionIntentExtras.HOST_SESSION)
            ?: return null
        val binder = sessionBundle.getBinder(ExplorerActionHostSessionKeys.BINDER) ?: return null
        if (
            runCatching { binder.interfaceDescriptor }.getOrNull() !=
            IExplorerActionHostSession.DESCRIPTOR
        ) {
            return null
        }
        val hostSession = IExplorerActionHostSession.Stub.asInterface(binder) ?: return null

        return ArchiveCompressionRequest(
            requestId = requestId,
            parentUri = parentUri,
            parentDisplayPath = parentDisplayPath,
            targets = targets,
            hostSession = hostSession,
        )
    }

    internal fun validateLeafName(value: String?): String? {
        val name = value ?: return null
        if (name.length !in 1..ExplorerActionProtocol.MAX_TARGET_DISPLAY_NAME_LENGTH) return null
        if (name == "." || name == ".." || name.isBlank()) return null
        if (name.any { it == '/' || it == '\u0000' || it.code < 0x20 || it.code == 0x7f }) return null
        return name
    }

    private fun canonicalRequestId(value: String?): String? {
        val requestId = value?.takeIf { it.length <= MAX_REQUEST_ID_LENGTH } ?: return null
        val parsed = runCatching { UUID.fromString(requestId) }.getOrNull() ?: return null
        return requestId.takeIf { parsed.toString().equals(requestId, ignoreCase = true) }
    }

    private fun validateOpaqueId(value: String?): String? {
        val id = value ?: return null
        if (id.length !in 1..ExplorerActionProtocol.MAX_TARGET_ID_LENGTH) return null
        if (id.any { it.isWhitespace() || it.code < 0x20 || it.code == 0x7f }) return null
        return id
    }

    private fun validateParentDisplayPath(value: String?): String? {
        val path = value ?: return null
        if (path.length !in 1..ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH) return null
        if (path.any { it == '\u0000' || it.code < 0x20 || it.code == 0x7f }) return null
        return path
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

    private fun isUsableContentUri(uri: Uri): Boolean =
        uri.isHierarchical &&
            uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true) &&
            !uri.authority.isNullOrBlank()

    @Suppress("DEPRECATION")
    private fun Intent.parcelableUriExtra(name: String): Uri? = getParcelableExtra(name)

    @Suppress("DEPRECATION")
    private fun Intent.parcelableBundleExtra(name: String): Bundle? = getParcelableExtra(name)

    @Suppress("DEPRECATION")
    private fun Intent.parcelableBundleArrayListExtra(name: String): ArrayList<Bundle>? =
        getParcelableArrayListExtra(name)

    @Suppress("DEPRECATION")
    private fun Bundle.parcelableUri(name: String): Uri? = getParcelable(name)

    private const val SIZE_UNKNOWN = -1L
    private const val MAX_REQUEST_ID_LENGTH = 36
    private const val MAX_MIME_TYPE_LENGTH = 255
    private val MIME_TYPE_PATTERN = Regex(
        "(?:[a-z0-9][a-z0-9!#$&^_.+-]*|\\*)/(?:[a-z0-9][a-z0-9!#$&^_.+-]*|\\*)",
    )
}
