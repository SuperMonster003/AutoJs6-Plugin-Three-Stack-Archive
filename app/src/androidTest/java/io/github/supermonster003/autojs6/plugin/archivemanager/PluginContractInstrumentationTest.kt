@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

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

        assertEquals(ArchiveManagerPlugin.ID, info.id)
        assertEquals(ExplorerActionPluginIds.ENGINE, info.engine)
        assertArrayEquals(ArchiveManagerPlugin.SUPPORTED_ABIS, info.supportedAbis)
        assertEquals(
            ArchiveManagerPlugin.REQUIRED_HOST_VERSION,
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
        val openAction = actionsById[ArchiveManagerPlugin.ACTION_OPEN_ID]
        val openAsArchiveAction = actionsById[ArchiveManagerPlugin.ACTION_OPEN_AS_ARCHIVE_ID]
        val manageAction = actionsById[ArchiveManagerPlugin.ACTION_MANAGE_ID]
        val extractToAction = actionsById[ArchiveManagerPlugin.ACTION_EXTRACT_TO_ID]
        val compressSingleAction = actionsById[ArchiveManagerPlugin.ACTION_COMPRESS_SINGLE_ID]
        val compressMultipleAction = actionsById[ArchiveManagerPlugin.ACTION_COMPRESS_MULTIPLE_ID]

        assertEquals(ExplorerActionProtocol.VERSION, catalog.getInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION))
        assertEquals(6, actionsById.size)
        assertNotNull(openAction)
        assertNotNull(openAsArchiveAction)
        assertNotNull(manageAction)
        assertNotNull(extractToAction)
        assertNotNull(compressSingleAction)
        assertNotNull(compressMultipleAction)
        assertEquals(
            ArchiveManagerPlugin.OPEN_LABEL_RESOURCE_NAME,
            openAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
        )
        assertEquals(
            ArchiveManagerPlugin.MANAGE_LABEL_RESOURCE_NAME,
            manageAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
        )
        assertEquals(
            ArchiveManagerPlugin.EXTRACT_TO_LABEL_RESOURCE_NAME,
            extractToAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
        )
        assertEquals(
            ArchiveManagerPlugin.OPEN_ACTION_PRIORITY,
            openAction?.getInt(ExplorerActionCatalogKeys.PRIORITY),
        )
        assertEquals(
            ArchiveManagerPlugin.MANAGE_ACTION_PRIORITY,
            manageAction?.getInt(ExplorerActionCatalogKeys.PRIORITY),
        )
        assertEquals(
            ArchiveManagerPlugin.EXTRACT_TO_ACTION_PRIORITY,
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
            ArchiveManagerPlugin.CREATE_ACTIVITY_CLASS_NAME,
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
        assertEquals(
            ArchiveManagerPlugin.MANAGE_FILE_NAME_SUFFIXES.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES),
        )
    }

    private fun assertUnmatchedFileProbe(action: Bundle) {
        assertEquals(
            ArchiveManagerPlugin.ACTIVITY_CLASS_NAME,
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
            ArchiveManagerPlugin.ACTION_OPEN_ID,
            action.getString(ExplorerActionCatalogKeys.PROBE_FOR_ACTION_ID),
        )
    }

    private fun assertManageFileAction(action: Bundle) {
        assertEquals(
            ArchiveManagerPlugin.ACTIVITY_CLASS_NAME,
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
            ArchiveManagerPlugin.MANAGE_EXTENSIONS.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS),
        )
        assertTrue(
            action.getStringArrayList(ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES)
                .orEmpty()
                .isEmpty(),
        )
    }

    private fun assertCommonFileAction(
        action: Bundle,
        placement: Int,
        presentation: Int,
        accessMode: Int,
    ) {
        assertEquals(
            ArchiveManagerPlugin.ACTIVITY_CLASS_NAME,
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
        assertEquals(
            ArchiveManagerPlugin.MIME_TYPES.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES),
        )
        assertEquals(
            ArchiveManagerPlugin.EXTENSIONS.toList(),
            action.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS),
        )
        assertEquals(
            ArchiveManagerPlugin.FILE_NAME_SUFFIXES.toList(),
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
            ComponentName(context.packageName, ArchiveManagerPlugin.ACTIVITY_CLASS_NAME),
            0,
        )
        val createActivityInfo = packageManager.getActivityInfo(
            ComponentName(context.packageName, ArchiveManagerPlugin.CREATE_ACTIVITY_CLASS_NAME),
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
        assertTrue(execution.any { it.activityInfo.name == ArchiveManagerPlugin.ACTIVITY_CLASS_NAME })
        assertTrue(execution.any { it.activityInfo.name == ArchiveManagerPlugin.CREATE_ACTIVITY_CLASS_NAME })
    }
}
