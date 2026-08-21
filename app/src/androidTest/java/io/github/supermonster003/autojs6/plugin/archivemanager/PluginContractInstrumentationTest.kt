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
    fun pluginInfoDeclaresAbiIndependentExplorerEngine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.archiveManagerPluginInfo()

        assertEquals(ArchiveManagerPlugin.ID, info.id)
        assertEquals(ExplorerActionPluginIds.ENGINE, info.engine)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
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
    fun catalogDeclaresReadOnlyArchiveAndCreateInParentCompressionActions() {
        val catalog = archiveManagerActionCatalog()
        val actions = catalog.getParcelableArrayList<Bundle>(ExplorerActionCatalogKeys.ACTIONS)
        val actionsById = actions.orEmpty().associateBy {
            it.getString(ExplorerActionCatalogKeys.ID)
        }
        val openAction = actionsById[ArchiveManagerPlugin.ACTION_OPEN_ID]
        val extractToAction = actionsById[ArchiveManagerPlugin.ACTION_EXTRACT_TO_ID]
        val compressSingleAction = actionsById[ArchiveManagerPlugin.ACTION_COMPRESS_SINGLE_ID]
        val compressMultipleAction = actionsById[ArchiveManagerPlugin.ACTION_COMPRESS_MULTIPLE_ID]

        assertEquals(ExplorerActionProtocol.VERSION, catalog.getInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION))
        assertEquals(4, actionsById.size)
        assertNotNull(openAction)
        assertNotNull(extractToAction)
        assertNotNull(compressSingleAction)
        assertNotNull(compressMultipleAction)
        assertEquals(
            ArchiveManagerPlugin.OPEN_LABEL_RESOURCE_NAME,
            openAction?.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME),
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
            ArchiveManagerPlugin.EXTRACT_TO_ACTION_PRIORITY,
            extractToAction?.getInt(ExplorerActionCatalogKeys.PRIORITY),
        )

        assertCommonReadOnlyFileAction(
            requireNotNull(openAction),
            ExplorerActionValues.PLACEMENT_PRIMARY,
            ExplorerActionValues.PRESENTATION_HOST_EXPLORER,
        )
        assertCommonReadOnlyFileAction(
            requireNotNull(extractToAction),
            ExplorerActionValues.PLACEMENT_OVERFLOW,
            ExplorerActionValues.PRESENTATION_ACTIVITY,
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
    }

    private fun assertCommonReadOnlyFileAction(action: Bundle, placement: Int, presentation: Int) {
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
