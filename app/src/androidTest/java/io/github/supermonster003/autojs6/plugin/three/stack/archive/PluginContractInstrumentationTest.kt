@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCatalogKeys
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionPluginIds
import org.autojs.plugin.explorer.api.ExplorerActionPluginPermissions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PluginContractInstrumentationTest {

    @Test
    fun pluginInfoDeclaresPackagedNativeAbisForExplorerEngine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.archiveManagerPluginInfo()

        assertEquals(ThreeStackArchivePlugin.ID, info.id)
        assertEquals(ExplorerActionPluginIds.ENGINE, info.engine)
        assertArrayEquals(ThreeStackArchivePlugin.SUPPORTED_ABIS, info.supportedAbis)
        assertEquals(
            ThreeStackArchivePlugin.REQUIRED_HOST_VERSION,
            info.capabilities?.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION),
        )
        assertEquals(
            ExplorerActionProtocol.VERSION,
            info.capabilities?.getInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION),
        )
    }

    @Test
    fun catalogDeclaresArchiveManagementExtractionAndCompressionActions() {
        val catalog = archiveManagerActionCatalog()
        val actions = catalog.getParcelableArrayList<Bundle>(ExplorerActionCatalogKeys.ACTIONS)
        val actionsById = actions.orEmpty().associateBy {
            it.getString(ExplorerActionCatalogKeys.ID)
        }
        val openAction = actionsById[ThreeStackArchivePlugin.ACTION_OPEN_ID]
        val openAsArchiveAction = actionsById[ThreeStackArchivePlugin.ACTION_OPEN_AS_ARCHIVE_ID]
        val manageAction = actionsById[ThreeStackArchivePlugin.ACTION_MANAGE_ID]
        val extractToAction = actionsById[ThreeStackArchivePlugin.ACTION_EXTRACT_TO_ID]
        val compressSingleAction = actionsById[ThreeStackArchivePlugin.ACTION_COMPRESS_SINGLE_ID]
        val compressMultipleAction = actionsById[ThreeStackArchivePlugin.ACTION_COMPRESS_MULTIPLE_ID]

        assertEquals(ExplorerActionProtocol.VERSION, catalog.getInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION))
        assertEquals(6, actionsById.size)
        assertNotNull(openAction)
        assertNotNull(openAsArchiveAction)
        assertNotNull(manageAction)
        assertNotNull(extractToAction)
        assertNotNull(compressSingleAction)
        assertNotNull(compressMultipleAction)
        assertEquals(
            ThreeStackArchivePlugin.OPEN_LABEL_RESOURCE_NAME,
            openAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
        )
        assertEquals(
            ThreeStackArchivePlugin.MANAGE_LABEL_RESOURCE_NAME,
            manageAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
        )
        assertEquals(
            ThreeStackArchivePlugin.EXTRACT_TO_LABEL_RESOURCE_NAME,
            extractToAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
        )
        assertEquals(
            ThreeStackArchivePlugin.OPEN_ACTION_PRIORITY,
            openAction?.getInt(ExplorerActionCatalogKeys.PRIORITY),
        )
        assertEquals(
            ThreeStackArchivePlugin.MANAGE_ACTION_PRIORITY,
            manageAction?.getInt(ExplorerActionCatalogKeys.PRIORITY),
        )
        assertEquals(
            ThreeStackArchivePlugin.EXTRACT_TO_ACTION_PRIORITY,
            extractToAction?.getInt(ExplorerActionCatalogKeys.PRIORITY),
        )

        assertCommonFileAction(
            requireNotNull(openAction),
            ExplorerActionValues.PLACEMENT_PRIMARY,
            ExplorerActionValues.PRESENTATION_HOST_EXPLORER,
            ExplorerActionValues.ACCESS_READ_ONLY,
        )
        assertUnmatchedFileProbe(requireNotNull(openAsArchiveAction))
        assertManageFileAction(requireNotNull(manageAction))
        assertCommonFileAction(
            requireNotNull(extractToAction),
            ExplorerActionValues.PLACEMENT_OVERFLOW,
            ExplorerActionValues.PRESENTATION_ACTIVITY,
            ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
        )
        assertCompressionAction(
            requireNotNull(compressSingleAction),
            ExplorerActionValues.CARDINALITY_SINGLE,
            ExplorerActionValues.PLACEMENT_OVERFLOW,
        )
        assertCompressionAction(
            requireNotNull(compressMultipleAction),
            ExplorerActionValues.CARDINALITY_MULTIPLE,
            ExplorerActionValues.PLACEMENT_SELECTION_TOOLBAR,
        )
    }

    private fun assertCompressionAction(action: Bundle, cardinality: Int, placement: Int) {
        assertEquals(
            ThreeStackArchivePlugin.CREATE_ACTIVITY_CLASS_NAME,
            action.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME),
        )
        assertEquals(ExplorerActionValues.TARGET_MIXED, action.getInt(ExplorerActionCatalogKeys.TARGET_KIND))
        assertEquals(cardinality, action.getInt(ExplorerActionCatalogKeys.CARDINALITY))
        assertEquals(
            ExplorerActionValues.ACCESS_CREATE_IN_PARENT,
            action.getInt(ExplorerActionCatalogKeys.ACCESS_MODE),
        )
        assertEquals(placement, action.getInt(ExplorerActionCatalogKeys.PLACEMENT))
        assertEquals(
            ExplorerActionValues.PRESENTATION_ACTIVITY,
            action.getInt(ExplorerActionCatalogKeys.PRESENTATION),
        )
        assertEquals(listOf("*/*"), action.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES))
        assertTrue(action.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS).orEmpty().isEmpty())
        assertTrue(
            action.getStringArrayList(ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES)
                .orEmpty()
                .isEmpty(),
        )
    }

    private fun assertUnmatchedFileProbe(action: Bundle) {
        assertEquals(
            ThreeStackArchivePlugin.ACTIVITY_CLASS_NAME,
            action.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME),
        )
        assertEquals(ExplorerActionValues.TARGET_FILE, action.getInt(ExplorerActionCatalogKeys.TARGET_KIND))
        assertEquals(
            ExplorerActionValues.CARDINALITY_SINGLE,
            action.getInt(ExplorerActionCatalogKeys.CARDINALITY),
        )
        assertEquals(ExplorerActionValues.ACCESS_READ_ONLY, action.getInt(ExplorerActionCatalogKeys.ACCESS_MODE))
        assertEquals(ExplorerActionValues.PLACEMENT_OVERFLOW, action.getInt(ExplorerActionCatalogKeys.PLACEMENT))
        assertEquals(
            ExplorerActionValues.PRESENTATION_HOST_EXPLORER,
            action.getInt(ExplorerActionCatalogKeys.PRESENTATION),
        )
        assertTrue(action.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES).orEmpty().isEmpty())
        assertTrue(action.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS).orEmpty().isEmpty())
        assertTrue(
            action.getStringArrayList(ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES).orEmpty().isEmpty(),
        )
        assertEquals(
            ThreeStackArchivePlugin.ACTION_OPEN_ID,
            action.getString(ExplorerActionCatalogKeys.PROBE_FOR_ACTION_ID),
        )
    }

    private fun assertManageFileAction(action: Bundle) {
        assertEquals(
            ThreeStackArchivePlugin.ACTIVITY_CLASS_NAME,
            action.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME),
        )
        assertEquals(ExplorerActionValues.TARGET_FILE, action.getInt(ExplorerActionCatalogKeys.TARGET_KIND))
        assertEquals(
            ExplorerActionValues.CARDINALITY_SINGLE,
            action.getInt(ExplorerActionCatalogKeys.CARDINALITY),
        )
        assertEquals(
            ExplorerActionValues.ACCESS_MANAGE_TARGET,
            action.getInt(ExplorerActionCatalogKeys.ACCESS_MODE),
        )
        assertEquals(ExplorerActionValues.PLACEMENT_OVERFLOW, action.getInt(ExplorerActionCatalogKeys.PLACEMENT))
        assertEquals(
            ExplorerActionValues.PRESENTATION_ACTIVITY,
            action.getInt(ExplorerActionCatalogKeys.PRESENTATION),
        )
        assertEquals(
            emptyList<String>(),
            action.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES),
        )
        assertEquals(
            ThreeStackArchivePlugin.MANAGE_EXTENSIONS.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS),
        )
        assertEquals(
            ThreeStackArchivePlugin.MANAGE_FILE_NAME_SUFFIXES.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES),
        )
    }

    private fun assertCommonFileAction(
        action: Bundle,
        placement: Int,
        presentation: Int,
        accessMode: Int,
    ) {
        assertEquals(
            ThreeStackArchivePlugin.ACTIVITY_CLASS_NAME,
            action.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME),
        )
        assertEquals(ExplorerActionValues.TARGET_FILE, action.getInt(ExplorerActionCatalogKeys.TARGET_KIND))
        assertEquals(
            ExplorerActionValues.CARDINALITY_SINGLE,
            action.getInt(ExplorerActionCatalogKeys.CARDINALITY),
        )
        assertEquals(accessMode, action.getInt(ExplorerActionCatalogKeys.ACCESS_MODE))
        assertEquals(placement, action.getInt(ExplorerActionCatalogKeys.PLACEMENT))
        assertEquals(presentation, action.getInt(ExplorerActionCatalogKeys.PRESENTATION))
        val isPrimary = placement == ExplorerActionValues.PLACEMENT_PRIMARY
        assertEquals(
            ThreeStackArchivePlugin.MIME_TYPES.filterNot {
                isPrimary && it == "application/vnd.android.package-archive"
            },
            action.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES),
        )
        assertEquals(
            ThreeStackArchivePlugin.EXTENSIONS.filterNot {
                isPrimary && it in ThreeStackArchivePlugin.ANDROID_PACKAGE_EXTENSIONS
            },
            action.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS),
        )
        assertEquals(
            ThreeStackArchivePlugin.FILE_NAME_SUFFIXES.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES),
        )
    }

    @Test
    fun serviceBindsAnExplicitComponentWithoutAnAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent().setComponent(ComponentName(context, ExplorerActionService::class.java))

        assertNotNull(ExplorerActionService().onBind(intent))
    }

    @Test
    fun manifestProtectsAndExportsDiscoveryExecutionAndWakeComponents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val serviceInfo = packageManager.getServiceInfo(
            ComponentName(context, ExplorerActionService::class.java),
            0,
        )
        val executionActivityInfo = packageManager.getActivityInfo(
            ComponentName(context.packageName, ThreeStackArchivePlugin.ACTIVITY_CLASS_NAME),
            0,
        )
        val createActivityInfo = packageManager.getActivityInfo(
            ComponentName(context.packageName, ThreeStackArchivePlugin.CREATE_ACTIVITY_CLASS_NAME),
            0,
        )
        val wakeActivityInfo = packageManager.getActivityInfo(
            ComponentName(context, WakeActivity::class.java),
            0,
        )

        assertTrue(serviceInfo.exported)
        assertEquals(ExplorerActionPluginPermissions.PLUGIN, serviceInfo.permission)
        assertTrue(executionActivityInfo.exported)
        assertEquals(ExplorerActionPluginPermissions.PLUGIN, executionActivityInfo.permission)
        assertTrue(createActivityInfo.exported)
        assertEquals(ExplorerActionPluginPermissions.PLUGIN, createActivityInfo.permission)
        assertTrue(wakeActivityInfo.exported)
        assertEquals(ExplorerActionPluginPermissions.PLUGIN, wakeActivityInfo.permission)

        val discovery = packageManager.queryIntentServices(
            Intent(ExplorerActionPluginActions.EXPLORER_ACTION).setPackage(context.packageName),
            0,
        )
        assertTrue(discovery.any { it.serviceInfo.name == ExplorerActionService::class.java.name })

        val execution = packageManager.queryIntentActivities(
            Intent(ExplorerActionPluginActions.EXECUTE)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(context.packageName),
            0,
        )
        assertTrue(execution.any { it.activityInfo.name == ThreeStackArchivePlugin.ACTIVITY_CLASS_NAME })
        assertTrue(execution.any { it.activityInfo.name == ThreeStackArchivePlugin.CREATE_ACTIVITY_CLASS_NAME })
    }
}
