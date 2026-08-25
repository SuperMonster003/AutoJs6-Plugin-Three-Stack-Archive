package io.github.supermonster003.autojs6.plugin.archivemanager

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

internal object ArchiveManagerPlugin {
    const val ID = "archive-manager"
    const val ACTION_OPEN_ID = "open-archive"
    const val ACTION_MANAGE_ID = "manage-archive"
    const val ACTION_EXTRACT_TO_ID = "extract-to"
    const val ACTION_COMPRESS_SINGLE_ID = "compress"
    const val ACTION_COMPRESS_MULTIPLE_ID = "compress-selection"
    const val VARIANT = "default"
    const val REQUIRED_HOST_VERSION = 5276L
    const val OPEN_LABEL_RESOURCE_NAME = "action_open_archive"
    const val OPEN_LABEL_FALLBACK = "Open archive"
    const val MANAGE_LABEL_RESOURCE_NAME = "action_manage_archive"
    const val MANAGE_LABEL_FALLBACK = "Manage archive..."
    const val EXTRACT_TO_LABEL_RESOURCE_NAME = "action_extract_to"
    const val EXTRACT_TO_LABEL_FALLBACK = "Extract to..."
    const val COMPRESS_LABEL_RESOURCE_NAME = "action_compress"
    const val COMPRESS_LABEL_FALLBACK = "Compress..."
    const val ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.archivemanager.ArchiveManagerActivity"
    const val CREATE_ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.archivemanager.CreateArchiveActivity"
    const val OPEN_ACTION_PRIORITY = 80
    const val MANAGE_ACTION_PRIORITY = 75
    const val EXTRACT_TO_ACTION_PRIORITY = 70
    const val COMPRESS_ACTION_PRIORITY = 60
    val SUPPORTED_ABIS = arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    val MANAGE_EXTENSIONS = arrayOf(ArchiveFormat.ZIP.primaryExtension)

    val MIME_TYPES = ArchiveEngine.DEFAULT.readableFormats
        .flatMap(ArchiveFormat::mimeTypes)
        .distinct()
        .sorted()
        .toTypedArray()
    val EXTENSIONS = ArchiveEngine.DEFAULT.readableFormats
        .flatMap(ArchiveFormat::catalogExtensions)
        .distinct()
        .sorted()
        .toTypedArray()
}

internal fun Context.archiveManagerPluginInfo(): PluginInfo {
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
        id = ArchiveManagerPlugin.ID
        engine = ExplorerActionPluginIds.ENGINE
        variant = ArchiveManagerPlugin.VARIANT
        supportedAbis = ArchiveManagerPlugin.SUPPORTED_ABIS.clone()
        capabilities = Bundle().apply {
            putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ArchiveManagerPlugin.REQUIRED_HOST_VERSION)
            putInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
        }
    }
}

internal fun archiveManagerActionCatalog(): Bundle {
    val actions = arrayListOf(
        archiveManagerAction(
            id = ArchiveManagerPlugin.ACTION_OPEN_ID,
            labelResourceName = ArchiveManagerPlugin.OPEN_LABEL_RESOURCE_NAME,
            labelFallback = ArchiveManagerPlugin.OPEN_LABEL_FALLBACK,
            priority = ArchiveManagerPlugin.OPEN_ACTION_PRIORITY,
            placement = ExplorerActionValues.PLACEMENT_PRIMARY,
            presentation = ExplorerActionValues.PRESENTATION_HOST_EXPLORER,
        ),
        archiveManagerAction(
            id = ArchiveManagerPlugin.ACTION_MANAGE_ID,
            labelResourceName = ArchiveManagerPlugin.MANAGE_LABEL_RESOURCE_NAME,
            labelFallback = ArchiveManagerPlugin.MANAGE_LABEL_FALLBACK,
            priority = ArchiveManagerPlugin.MANAGE_ACTION_PRIORITY,
            accessMode = ExplorerActionValues.ACCESS_MANAGE_TARGET,
            mimeTypes = emptyArray(),
            extensions = ArchiveManagerPlugin.MANAGE_EXTENSIONS,
        ),
        archiveManagerAction(
            id = ArchiveManagerPlugin.ACTION_EXTRACT_TO_ID,
            labelResourceName = ArchiveManagerPlugin.EXTRACT_TO_LABEL_RESOURCE_NAME,
            labelFallback = ArchiveManagerPlugin.EXTRACT_TO_LABEL_FALLBACK,
            priority = ArchiveManagerPlugin.EXTRACT_TO_ACTION_PRIORITY,
            accessMode = ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
        ),
        archiveManagerAction(
            id = ArchiveManagerPlugin.ACTION_COMPRESS_SINGLE_ID,
            labelResourceName = ArchiveManagerPlugin.COMPRESS_LABEL_RESOURCE_NAME,
            labelFallback = ArchiveManagerPlugin.COMPRESS_LABEL_FALLBACK,
            priority = ArchiveManagerPlugin.COMPRESS_ACTION_PRIORITY,
            activityClassName = ArchiveManagerPlugin.CREATE_ACTIVITY_CLASS_NAME,
            targetKind = ExplorerActionValues.TARGET_MIXED,
            cardinality = ExplorerActionValues.CARDINALITY_SINGLE,
            accessMode = ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
            placement = ExplorerActionValues.PLACEMENT_OVERFLOW,
            mimeTypes = arrayOf("*/*"),
            extensions = emptyArray(),
        ),
        archiveManagerAction(
            id = ArchiveManagerPlugin.ACTION_COMPRESS_MULTIPLE_ID,
            labelResourceName = ArchiveManagerPlugin.COMPRESS_LABEL_RESOURCE_NAME,
            labelFallback = ArchiveManagerPlugin.COMPRESS_LABEL_FALLBACK,
            priority = ArchiveManagerPlugin.COMPRESS_ACTION_PRIORITY,
            activityClassName = ArchiveManagerPlugin.CREATE_ACTIVITY_CLASS_NAME,
            targetKind = ExplorerActionValues.TARGET_MIXED,
            cardinality = ExplorerActionValues.CARDINALITY_MULTIPLE,
            accessMode = ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
            placement = ExplorerActionValues.PLACEMENT_SELECTION_TOOLBAR,
            mimeTypes = arrayOf("*/*"),
            extensions = emptyArray(),
        ),
    )
    return Bundle().apply {
        putInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
        putParcelableArrayList(ExplorerActionCatalogKeys.ACTIONS, actions)
    }
}

private fun archiveManagerAction(
    id: String,
    labelResourceName: String,
    labelFallback: String,
    priority: Int,
    activityClassName: String = ArchiveManagerPlugin.ACTIVITY_CLASS_NAME,
    targetKind: Int = ExplorerActionValues.TARGET_FILE,
    cardinality: Int = ExplorerActionValues.CARDINALITY_SINGLE,
    accessMode: Int = ExplorerActionValues.ACCESS_READ_ONLY,
    placement: Int = ExplorerActionValues.PLACEMENT_OVERFLOW,
    presentation: Int = ExplorerActionValues.PRESENTATION_ACTIVITY,
    mimeTypes: Array<String> = ArchiveManagerPlugin.MIME_TYPES,
    extensions: Array<String> = ArchiveManagerPlugin.EXTENSIONS,
) = Bundle().apply {
    putString(ExplorerActionCatalogKeys.ID, id)
    putString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME, labelResourceName)
    putString(ExplorerActionCatalogKeys.LABEL_FALLBACK, labelFallback)
    putString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME, activityClassName)
    putInt(ExplorerActionCatalogKeys.PRIORITY, priority)
    putInt(ExplorerActionCatalogKeys.TARGET_KIND, targetKind)
    putInt(ExplorerActionCatalogKeys.CARDINALITY, cardinality)
    putInt(ExplorerActionCatalogKeys.ACCESS_MODE, accessMode)
    putInt(ExplorerActionCatalogKeys.PLACEMENT, placement)
    putInt(ExplorerActionCatalogKeys.PRESENTATION, presentation)
    putStringArrayList(
        ExplorerActionCatalogKeys.MIME_TYPES,
        ArrayList(mimeTypes.asList()),
    )
    putStringArrayList(
        ExplorerActionCatalogKeys.EXTENSIONS,
        ArrayList(extensions.asList()),
    )
}
