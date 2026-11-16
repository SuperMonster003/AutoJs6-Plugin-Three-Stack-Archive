@file:Suppress("DEPRECATION")
package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.explorer.api.IExplorerActionPlugin
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Checks installed service discovery, metadata Binder transactions and read-only host appearance IPC. */
class HostSettingsContractInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun infoEntryIsProtectedAndReturnsTheExistingExplorerMetadata() {
        val matches = context.packageManager.queryIntentServices(
            Intent("org.autojs.plugin.INFO").setPackage(context.packageName), 0,
        )
        assertEquals(1, matches.size)
        val service = matches.single().serviceInfo
        assertEquals(context.packageName + ".PluginInfoService", service.name)
        assertTrue(service.enabled)
        assertTrue(service.exported)
        assertEquals("org.autojs.permission.PLUGIN", service.permission)
        val info = withService(service.name) { binder ->
            assertEquals("org.autojs.plugin.common.api.IPluginInfoProvider", binder.interfaceDescriptor)
            readCommonInfo(binder)
        }
        val explorer = withService(context.packageName + ".ExplorerActionService") { binder ->
            assertEquals("org.autojs.plugin.explorer.api.IExplorerActionPlugin", binder.interfaceDescriptor)
            IExplorerActionPlugin.Stub.asInterface(binder).info
        }
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("three-stack-archive", info.id)
        assertEquals(installed.versionName, info.versionName)
        assertEquals(
            if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong(),
            info.versionCode,
        )
        assertEquals(explorer.name, info.name)
        assertEquals(explorer.description, info.description)
        assertEquals(explorer.author, info.author)
        assertEquals(explorer.instruction, info.instruction)
        assertEquals(explorer.versionDate, info.versionDate)
        assertEquals(explorer.id, info.id)
        assertEquals(explorer.engine, info.engine)
        assertEquals(explorer.variant, info.variant)
        assertArrayEquals(explorer.supportedAbis, info.supportedAbis)
        val expected = requireNotNull(explorer.capabilities)
        val actual = requireNotNull(info.capabilities)
        assertEquals(expected.keySet(), actual.keySet())
        expected.keySet().forEach { key -> assertEquals(key, expected.get(key), actual.get(key)) }
    }

    @Test fun installedOfficialHostAcceptsSettingsRequest() {
        // Opt in on a device with the official host and a same-signer plugin; never alter host settings.
        assumeTrue(
            "Run with -e requireHostSettings true on an official-host device",
            InstrumentationRegistry.getArguments().getString("requireHostSettings") == "true",
        )
        val host = context.packageManager.getPackageInfo("org.autojs.autojs6", 0)
        val snapshot = requireNotNull(context.contentResolver.call(
            Uri.parse("content://org.autojs.autojs6.plugin.settings"), "getSettings", null, null,
        ))
        assertEquals(1, snapshot.getInt("protocolVersion"))
        assertEquals("org.autojs.autojs6", snapshot.getString("hostPackageName"))
        assertEquals(host.versionName, snapshot.getString("hostVersionName"))
        assertEquals(
            if (Build.VERSION.SDK_INT >= 28) host.longVersionCode else host.versionCode.toLong(),
            snapshot.getLong("hostVersionCode"),
        )
        for (key in listOf("themeColorPrimary", "themeColorPrimaryDark", "themeColorAccent")) {
            assertTrue(key, snapshot.get(key) is Int)
        }
        assertTrue(snapshot.get("darkModeActive") is Boolean)
        assertTrue(snapshot.getString("darkModePolicy") in listOf("FOLLOW_SYSTEM", "LIGHT", "DARK"))
        assertNotNull(snapshot.getString("languageTag"))
        assertFalse(snapshot.getString("resolvedLanguageTag").isNullOrBlank())
    }

    private fun readCommonInfo(binder: IBinder): PluginInfo {
        // Exercise the public AIDL wire format even when release shrinking renames the Stub class.
        val request = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            request.writeInterfaceToken("org.autojs.plugin.common.api.IPluginInfoProvider")
            assertTrue(binder.transact(IBinder.FIRST_CALL_TRANSACTION, request, reply, 0))
            reply.readException()
            assertEquals(1, reply.readInt())
            @Suppress("UNCHECKED_CAST")
            val creator = PluginInfo::class.java.getField("CREATOR").get(null) as Parcelable.Creator<PluginInfo>
            return creator.createFromParcel(reply)
        } finally {
            reply.recycle()
            request.recycle()
        }
    }

    private fun <T> withService(name: String, block: (IBinder) -> T): T {
        val connected = CountDownLatch(1)
        var binder: IBinder? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service
                connected.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        assertTrue(context.bindService(
            Intent().setComponent(ComponentName(context.packageName, name)),
            connection, Context.BIND_AUTO_CREATE,
        ))
        try {
            assertTrue("Service connection timed out", connected.await(10, TimeUnit.SECONDS))
            return block(requireNotNull(binder))
        } finally {
            context.unbindService(connection)
        }
    }
}
