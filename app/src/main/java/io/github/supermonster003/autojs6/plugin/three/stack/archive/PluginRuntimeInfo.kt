package io.github.supermonster003.autojs6.plugin.three.stack.archive

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

internal object ThreeStackArchivePlugin {
    const val ID = "three-stack-archive"
    const val ACTION_OPEN_ID = "open-archive"
    const val ACTION_OPEN_AS_ARCHIVE_ID = "open-as-archive"
    const val ACTION_MANAGE_ID = "manage-archive"
    const val ACTION_EXTRACT_TO_ID = "extract-to"
    const val ACTION_COMPRESS_SINGLE_ID = "compress"
    const val ACTION_COMPRESS_MULTIPLE_ID = "compress-selection"
    const val VARIANT = "default"
    const val REQUIRED_HOST_VERSION = 5276L
    const val OPEN_LABEL_RESOURCE_NAME = "action_open_archive"
    const val OPEN_LABEL_FALLBACK = "View archive"
    const val OPEN_AS_ARCHIVE_LABEL_RESOURCE_NAME = "action_open_as_archive"
    const val OPEN_AS_ARCHIVE_LABEL_FALLBACK = "Open as archive..."
    const val MANAGE_LABEL_RESOURCE_NAME = "action_manage_archive"
    const val MANAGE_LABEL_FALLBACK = "Manage archive..."
    const val EXTRACT_TO_LABEL_RESOURCE_NAME = "action_extract_to"
    const val EXTRACT_TO_LABEL_FALLBACK = "Extract to..."
    const val COMPRESS_LABEL_RESOURCE_NAME = "action_compress"
    const val COMPRESS_LABEL_FALLBACK = "Compress..."
    const val ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.three.stack.archive.ArchiveManagerActivity"
    const val CREATE_ACTIVITY_CLASS_NAME =
        "io.github.supermonster003.autojs6.plugin.three.stack.archive.CreateArchiveActivity"
    const val OPEN_ACTION_PRIORITY = 80
    const val MANAGE_ACTION_PRIORITY = 75
    const val EXTRACT_TO_ACTION_PRIORITY = 70
    const val OPEN_AS_ARCHIVE_ACTION_PRIORITY = 65
    const val COMPRESS_ACTION_PRIORITY = 60
    val SUPPORTED_ABIS = arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    val ANDROID_PACKAGE_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm", "apkz", "aab")
    fun canModifyFileName(displayName: String): Boolean = ArchiveEngine.DEFAULT.mutationFormats.any { format ->
        if (format == ArchiveFormat.ZIP) displayName.endsWith(".zip", ignoreCase = true)
        else format.matchesFileName(displayName)
    }

    val MANAGE_EXTENSIONS = ArchiveEngine.DEFAULT.readableFormats
        .flatMap(ArchiveFormat::catalogExtensions)
        .filterNot { it in ANDROID_PACKAGE_EXTENSIONS }
        .distinct()
        .sorted()
        .toTypedArray()
    val MANAGE_FILE_NAME_SUFFIXES = ArchiveEngine.DEFAULT.readableFormats
        .flatMap(ArchiveFormat::catalogFileNameSuffixes)
        .plus(NumberedArchiveVolumePolicy.fileNameSuffixes)
        .distinct()
        .sorted()
        .toTypedArray()
    val NUMBERED_VOLUME_FILE_NAME_SUFFIXES = NumberedArchiveVolumePolicy.fileNameSuffixes.clone()

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
    val FILE_NAME_SUFFIXES = (
        ArchiveEngine.DEFAULT.readableFormats.flatMap(ArchiveFormat::catalogFileNameSuffixes) +
            NUMBERED_VOLUME_FILE_NAME_SUFFIXES
        )
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
        id = ThreeStackArchivePlugin.ID
        engine = ExplorerActionPluginIds.ENGINE
        variant = ThreeStackArchivePlugin.VARIANT
        supportedAbis = org.autojs.plugin.runtime.InstalledPackageIdentity.supportedAbis(this@archiveManagerPluginInfo)
        capabilities = Bundle().apply {
            putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeStackArchivePlugin.REQUIRED_HOST_VERSION)
            putInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
        }
    }
}

internal fun archiveManagerActionCatalog(): Bundle {
    val actions = arrayListOf(
        archiveManagerAction(
            id = ThreeStackArchivePlugin.ACTION_OPEN_ID,
            labelResourceName = ThreeStackArchivePlugin.OPEN_LABEL_RESOURCE_NAME,
            labelFallback = ThreeStackArchivePlugin.OPEN_LABEL_FALLBACK,
            priority = ThreeStackArchivePlugin.OPEN_ACTION_PRIORITY,
            placement = ExplorerActionValues.PLACEMENT_PRIMARY,
            presentation = ExplorerActionValues.PRESENTATION_HOST_EXPLORER,
            // Package archives use the explicit probe menu so older hosts also retain their buttons.
            extensions = ThreeStackArchivePlugin.EXTENSIONS.filterNot {
                it in ThreeStackArchivePlugin.ANDROID_PACKAGE_EXTENSIONS
            }.toTypedArray(),
            mimeTypes = ThreeStackArchivePlugin.MIME_TYPES.filterNot {
                it == "application/vnd.android.package-archive"
            }.toTypedArray(),
        ),
        archiveManagerAction(
            id = ThreeStackArchivePlugin.ACTION_MANAGE_ID,
            labelResourceName = ThreeStackArchivePlugin.MANAGE_LABEL_RESOURCE_NAME,
            labelFallback = ThreeStackArchivePlugin.MANAGE_LABEL_FALLBACK,
            priority = ThreeStackArchivePlugin.MANAGE_ACTION_PRIORITY,
            accessMode = ExplorerActionValues.ACCESS_MANAGE_TARGET,
            mimeTypes = emptyArray(),
            extensions = ThreeStackArchivePlugin.MANAGE_EXTENSIONS,
            fileNameSuffixes = ThreeStackArchivePlugin.MANAGE_FILE_NAME_SUFFIXES,
        ),
        archiveManagerAction(
            id = ThreeStackArchivePlugin.ACTION_EXTRACT_TO_ID,
            labelResourceName = ThreeStackArchivePlugin.EXTRACT_TO_LABEL_RESOURCE_NAME,
            labelFallback = ThreeStackArchivePlugin.EXTRACT_TO_LABEL_FALLBACK,
            priority = ThreeStackArchivePlugin.EXTRACT_TO_ACTION_PRIORITY,
            accessMode = ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
        ),
        archiveManagerAction(
            id = ThreeStackArchivePlugin.ACTION_OPEN_AS_ARCHIVE_ID,
            labelResourceName = ThreeStackArchivePlugin.OPEN_AS_ARCHIVE_LABEL_RESOURCE_NAME,
            labelFallback = ThreeStackArchivePlugin.OPEN_AS_ARCHIVE_LABEL_FALLBACK,
            priority = ThreeStackArchivePlugin.OPEN_AS_ARCHIVE_ACTION_PRIORITY,
            placement = ExplorerActionValues.PLACEMENT_OVERFLOW,
            presentation = ExplorerActionValues.PRESENTATION_HOST_EXPLORER,
            mimeTypes = emptyArray(),
            extensions = emptyArray(),
            fileNameSuffixes = emptyArray(),
            probeForActionId = ThreeStackArchivePlugin.ACTION_OPEN_ID,
        ),
        archiveManagerAction(
            id = ThreeStackArchivePlugin.ACTION_COMPRESS_SINGLE_ID,
            labelResourceName = ThreeStackArchivePlugin.COMPRESS_LABEL_RESOURCE_NAME,
            labelFallback = ThreeStackArchivePlugin.COMPRESS_LABEL_FALLBACK,
            priority = ThreeStackArchivePlugin.COMPRESS_ACTION_PRIORITY,
            activityClassName = ThreeStackArchivePlugin.CREATE_ACTIVITY_CLASS_NAME,
            targetKind = ExplorerActionValues.TARGET_MIXED,
            cardinality = ExplorerActionValues.CARDINALITY_SINGLE,
            accessMode = ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
            placement = ExplorerActionValues.PLACEMENT_OVERFLOW,
            mimeTypes = arrayOf("*/*"),
            extensions = emptyArray(),
            fileNameSuffixes = emptyArray(),
        ),
        archiveManagerAction(
            id = ThreeStackArchivePlugin.ACTION_COMPRESS_MULTIPLE_ID,
            labelResourceName = ThreeStackArchivePlugin.COMPRESS_LABEL_RESOURCE_NAME,
            labelFallback = ThreeStackArchivePlugin.COMPRESS_LABEL_FALLBACK,
            priority = ThreeStackArchivePlugin.COMPRESS_ACTION_PRIORITY,
            activityClassName = ThreeStackArchivePlugin.CREATE_ACTIVITY_CLASS_NAME,
            targetKind = ExplorerActionValues.TARGET_MIXED,
            cardinality = ExplorerActionValues.CARDINALITY_MULTIPLE,
            accessMode = ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
            placement = ExplorerActionValues.PLACEMENT_SELECTION_TOOLBAR,
            mimeTypes = arrayOf("*/*"),
            extensions = emptyArray(),
            fileNameSuffixes = emptyArray(),
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
    activityClassName: String = ThreeStackArchivePlugin.ACTIVITY_CLASS_NAME,
    targetKind: Int = ExplorerActionValues.TARGET_FILE,
    cardinality: Int = ExplorerActionValues.CARDINALITY_SINGLE,
    accessMode: Int = ExplorerActionValues.ACCESS_READ_ONLY,
    placement: Int = ExplorerActionValues.PLACEMENT_OVERFLOW,
    presentation: Int = ExplorerActionValues.PRESENTATION_ACTIVITY,
    mimeTypes: Array<String> = ThreeStackArchivePlugin.MIME_TYPES,
    extensions: Array<String> = ThreeStackArchivePlugin.EXTENSIONS,
    fileNameSuffixes: Array<String> = ThreeStackArchivePlugin.FILE_NAME_SUFFIXES,
    probeForActionId: String? = null,
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
    putStringArrayList(
        ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES,
        ArrayList(fileNameSuffixes.asList()),
    )
    probeForActionId?.let { actionId ->
        putString(ExplorerActionCatalogKeys.PROBE_FOR_ACTION_ID, actionId)
    }
}
