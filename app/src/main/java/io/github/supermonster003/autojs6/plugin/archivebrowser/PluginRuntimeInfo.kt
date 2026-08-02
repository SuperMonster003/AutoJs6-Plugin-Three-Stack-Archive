package io.github.supermonster003.autojs6.plugin.archivebrowser

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.explorer.api.ExplorerActionCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCatalogKeys
import org.autojs.plugin.explorer.api.ExplorerActionPluginIds
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionValues

internal object ArchiveBrowserPlugin {
    const val ID = "archive-browser"
    const val ACTION_ID = "browse-archive"
    const val VARIANT = "default"
    const val REQUIRED_HOST_VERSION = 5268L
    const val LABEL_RESOURCE_NAME = "action_archive_browser"
    const val LABEL_FALLBACK = "Browse archive"
    const val ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.archivebrowser.ArchiveBrowserActivity"
    const val ACTION_PRIORITY = 80

    val MIME_TYPES = arrayOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/java-archive",
    )
    val EXTENSIONS = arrayOf("zip", "jar", "aar", "war")
}

internal fun Context.archiveBrowserPluginInfo(): PluginInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return PluginInfo().apply {
        name = getString(R.string.app_name)
        description = getString(R.string.plugin_description)
        instruction = null
        author = getString(R.string.plugin_author)
        collaborators = null
        versionName = packageInfo.versionName.orEmpty()
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        versionDate = getString(R.string.plugin_version_date)
        id = ArchiveBrowserPlugin.ID
        engine = ExplorerActionPluginIds.ENGINE
        variant = ArchiveBrowserPlugin.VARIANT
        supportedAbis = emptyArray()
        capabilities = Bundle().apply {
            putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ArchiveBrowserPlugin.REQUIRED_HOST_VERSION)
            putInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
        }
    }
}

internal fun archiveBrowserActionCatalog(): Bundle {
    val action = Bundle().apply {
        putString(ExplorerActionCatalogKeys.ID, ArchiveBrowserPlugin.ACTION_ID)
        putString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME, ArchiveBrowserPlugin.LABEL_RESOURCE_NAME)
        putString(ExplorerActionCatalogKeys.LABEL_FALLBACK, ArchiveBrowserPlugin.LABEL_FALLBACK)
        putString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME, ArchiveBrowserPlugin.ACTIVITY_CLASS_NAME)
        putInt(ExplorerActionCatalogKeys.PRIORITY, ArchiveBrowserPlugin.ACTION_PRIORITY)
        putInt(ExplorerActionCatalogKeys.TARGET_KIND, ExplorerActionValues.TARGET_FILE)
        putInt(ExplorerActionCatalogKeys.ACCESS_MODE, ExplorerActionValues.ACCESS_READ_ONLY)
        putInt(ExplorerActionCatalogKeys.PLACEMENT, ExplorerActionValues.PLACEMENT_OVERFLOW)
        putStringArrayList(
            ExplorerActionCatalogKeys.MIME_TYPES,
            ArrayList(ArchiveBrowserPlugin.MIME_TYPES.asList()),
        )
        putStringArrayList(
            ExplorerActionCatalogKeys.EXTENSIONS,
            ArrayList(ArchiveBrowserPlugin.EXTENSIONS.asList()),
        )
    }
    return Bundle().apply {
        putInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
        putParcelableArrayList(ExplorerActionCatalogKeys.ACTIONS, arrayListOf(action))
    }
}
