@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivebrowser

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
        val info = context.archiveBrowserPluginInfo()

        assertEquals(ArchiveBrowserPlugin.ID, info.id)
        assertEquals(ExplorerActionPluginIds.ENGINE, info.engine)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(
            ArchiveBrowserPlugin.REQUIRED_HOST_VERSION,
            info.capabilities?.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION),
        )
        assertEquals(
            ExplorerActionProtocol.VERSION,
            info.capabilities?.getInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION),
        )
    }

    @Test
    fun catalogDeclaresTheExactReadOnlyArchiveAction() {
        val catalog = archiveBrowserActionCatalog()
        val actions = catalog.getParcelableArrayList<Bundle>(ExplorerActionCatalogKeys.ACTIONS)
        val action = actions?.single()

        assertEquals(ExplorerActionProtocol.VERSION, catalog.getInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION))
        assertNotNull(action)
        assertEquals(ArchiveBrowserPlugin.ACTION_ID, action?.getString(ExplorerActionCatalogKeys.ID))
        assertEquals(
            ArchiveBrowserPlugin.ACTIVITY_CLASS_NAME,
            action?.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME),
        )
        assertEquals(ArchiveBrowserPlugin.ACTION_PRIORITY, action?.getInt(ExplorerActionCatalogKeys.PRIORITY))
        assertEquals(ExplorerActionValues.TARGET_FILE, action?.getInt(ExplorerActionCatalogKeys.TARGET_KIND))
        assertEquals(ExplorerActionValues.ACCESS_READ_ONLY, action?.getInt(ExplorerActionCatalogKeys.ACCESS_MODE))
        assertEquals(ExplorerActionValues.PLACEMENT_OVERFLOW, action?.getInt(ExplorerActionCatalogKeys.PLACEMENT))
        assertEquals(
            ArchiveBrowserPlugin.MIME_TYPES.toList(),
            action?.getStringArrayList(ExplorerActionCatalogKeys.MIME_TYPES),
        )
        assertEquals(
            ArchiveBrowserPlugin.EXTENSIONS.toList(),
            action?.getStringArrayList(ExplorerActionCatalogKeys.EXTENSIONS),
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
            ComponentName(context.packageName, ArchiveBrowserPlugin.ACTIVITY_CLASS_NAME),
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
        assertTrue(execution.any { it.activityInfo.name == ArchiveBrowserPlugin.ACTIVITY_CLASS_NAME })
    }
}
