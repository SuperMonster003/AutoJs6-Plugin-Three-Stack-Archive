package io.github.supermonster003.autojs6.plugin.archivebrowser

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import java.util.Locale

internal data class ArchiveBrowseRequest(
    val archiveUri: Uri,
    val parentUri: Uri,
    val displayName: String,
    val declaredSize: Long,
)

/** Validates the complete read-only Explorer Action contract before opening archive content. */
internal object ArchiveIntentPolicy {

    const val MAX_DISPLAY_NAME_LENGTH = 255

    private val supportedMimeTypes = ArchiveBrowserPlugin.MIME_TYPES.toSet()
    private val supportedExtensions = ArchiveBrowserPlugin.EXTENSIONS.toSet()

    fun resolve(intent: Intent): ArchiveBrowseRequest? {
        if (intent.action != ExplorerActionPluginActions.EXECUTE) return null
        if (intent.getStringExtra(ExplorerActionIntentExtras.ACTION_ID) != ArchiveBrowserPlugin.ACTION_ID) {
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
        if (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) return null

        val archiveUri = intent.data?.takeIf(::isPlainContentUri) ?: return null
        val parentUri = intent.parcelableUriExtra(ExplorerActionIntentExtras.PARENT_URI)
            ?.takeIf(::isPlainContentUri)
            ?: return null
        if (!isStrictDescendant(parentUri, archiveUri)) return null

        val clipData = intent.clipData ?: return null
        if (clipData.itemCount != REQUIRED_CLIP_ITEM_COUNT) return null
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_TARGET_INDEX).uri != archiveUri) {
            return null
        }
        if (clipData.getItemAt(ExplorerActionIntentValues.CLIP_ITEM_PARENT_INDEX).uri != parentUri) {
            return null
        }

        val displayName = validateDisplayName(
            intent.getStringExtra(ExplorerActionIntentExtras.DISPLAY_NAME),
        ) ?: return null
        if (!isSupportedArchive(intent.type, displayName)) return null

        if (!intent.hasExtra(ExplorerActionIntentExtras.SIZE)) return null
        val declaredSize = runCatching {
            intent.getLongExtra(ExplorerActionIntentExtras.SIZE, INVALID_SIZE)
        }.getOrNull() ?: return null
        if (!isDeclaredSizeAccepted(declaredSize)) return null

        return ArchiveBrowseRequest(
            archiveUri = archiveUri,
            parentUri = parentUri,
            displayName = displayName,
            declaredSize = declaredSize,
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

    fun isDeclaredSizeAccepted(size: Long): Boolean =
        size in 0L..ArchiveCacheStager.MAX_ARCHIVE_BYTES

    fun isSupportedArchive(mimeType: String?, displayName: String): Boolean {
        val normalizedMimeType = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
        val extension = displayName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        return normalizedMimeType in supportedMimeTypes || extension in supportedExtensions
    }

    private fun isPlainContentUri(uri: Uri): Boolean =
        uri.isHierarchical &&
            uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true) &&
            !uri.authority.isNullOrBlank() &&
            uri.query == null &&
            uri.fragment == null &&
            uri.pathSegments.isNotEmpty() &&
            uri.pathSegments.none { it == "." || it == ".." }

    private fun isStrictDescendant(parentUri: Uri, targetUri: Uri): Boolean {
        if (parentUri.scheme != targetUri.scheme || parentUri.authority != targetUri.authority) return false
        val parentSegments = parentUri.pathSegments
        val targetSegments = targetUri.pathSegments
        return targetSegments.size > parentSegments.size &&
            targetSegments.take(parentSegments.size) == parentSegments
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableUriExtra(name: String): Uri? = getParcelableExtra(name)

    private const val REQUIRED_CLIP_ITEM_COUNT = 2
    private const val INVALID_SIZE = -1L
}
