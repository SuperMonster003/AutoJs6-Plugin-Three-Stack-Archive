package io.github.supermonster003.autojs6.plugin.three.stack.archive.tool

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.lingala.zip4j.io.outputstream.ZipOutputStream as Zip4jOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.autojs.plugin.tool.api.IPluginToolCallback
import org.autojs.plugin.tool.api.IPluginToolOutputTree
import org.autojs.plugin.tool.api.IPluginToolProvider
import org.autojs.plugin.tool.api.PluginToolAcceptKeys
import org.autojs.plugin.tool.api.PluginToolCapabilityKeys
import org.autojs.plugin.tool.api.PluginToolCatalogKeys
import org.autojs.plugin.tool.api.PluginToolContract
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorKeys
import org.autojs.plugin.tool.api.PluginToolInvokeKeys
import org.autojs.plugin.tool.api.PluginToolOutputTreeErrors
import org.autojs.plugin.tool.api.PluginToolOutputTreeKeys
import org.autojs.plugin.tool.api.PluginToolResultKeys
import org.autojs.plugin.tool.api.PluginToolValues
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The plugin tool provider (MCP Server roadmap P12, D53 - D56) over its own process boundary: discovery, capability and
 * catalog, list with paging and a folder filter, read of text, truncated and binary files, extraction into a directory
 * output this test serves like AutoJs6 (existing files answer EXISTS and are kept), an encrypted archive with and
 * without its password, and the refusals.
 */
@RunWith(AndroidJUnit4::class)
class ArchiveToolServiceTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var directory: File
    private lateinit var zip: File

    @Before
    fun setUp() {
        directory = File(context.cacheDir, "archive-tool-test-${UUID.randomUUID()}").apply { mkdirs() }
        zip = File(directory, "sample.zip")
        ZipOutputStream(FileOutputStream(zip)).use { output ->
            fun put(name: String, bytes: ByteArray?) {
                output.putNextEntry(ZipEntry(name))
                bytes?.let(output::write)
                output.closeEntry()
            }
            put("readme.txt", "Read me first.\n".toByteArray())
            put("docs/", null)
            put("docs/a.txt", "alpha".toByteArray())
            put("docs/deep/b.txt", "beta 中文 ✓".toByteArray())
            put("bin.dat", byteArrayOf(1, 0, 2, 0, 3))
            put("long.txt", "x".repeat(70_000).toByteArray())
        }
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun discoveryCapabilityCatalogAndListing() {
        withProvider { provider ->
            assertEquals(1, provider.info.capabilities?.getInt(PluginToolCapabilityKeys.CONTRACT_VERSION))
            val catalog = JSONObject(provider.getCatalog(Bundle()).getString(PluginToolCatalogKeys.CATALOG_JSON)!!)
            assertEquals("archive", catalog.getString("shortName"))
            assertEquals(listOf("list", "read", "extract"), (0 until catalog.getJSONArray("tools").length()).map { catalog.getJSONArray("tools").getJSONObject(it).getString("name") })

            val all = call(provider, "list", JSONObject()).result()
            assertEquals("zip", all.getString("format"))
            assertEquals(6, all.getInt("total"))
            assertFalse(all.getBoolean("truncated"))
            val paths = all.getJSONArray("entries").let { entries -> (0 until entries.length()).map { entries.getJSONObject(it).getString("path") } }
            assertTrue(paths.toString(), paths.containsAll(listOf("readme.txt", "docs", "docs/a.txt", "docs/deep/b.txt", "bin.dat", "long.txt")))
            val page = call(provider, "list", JSONObject().put("offset", 1).put("limit", 2)).result()
            assertEquals(2, page.getJSONArray("entries").length())
            assertTrue(page.getBoolean("truncated"))
            val docs = call(provider, "list", JSONObject().put("directory", "docs/")).result()
            assertEquals(2, docs.getInt("total"))
            assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, "list", JSONObject().put("directory", "nothing")).code())
        }
    }

    @Test
    fun readReturnsTextCutsLongFilesAndFlagsBinaryOnes() {
        withProvider { provider ->
            val text = call(provider, "read", JSONObject().put("path", "docs/deep/b.txt")).result()
            assertEquals("beta 中文 ✓", text.getString("text"))
            assertFalse(text.getBoolean("truncated"))
            val long = call(provider, "read", JSONObject().put("path", "long.txt").put("maxBytes", 100)).result()
            assertTrue(long.getBoolean("truncated"))
            assertEquals(100, long.getInt("bytesRead"))
            assertEquals(70_000, long.getInt("size"))
            val binary = call(provider, "read", JSONObject().put("path", "bin.dat")).result()
            assertTrue(binary.getBoolean("binary"))
            assertFalse(binary.has("text"))
            assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, "read", JSONObject().put("path", "docs")).code())
            assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, "read", JSONObject().put("path", "missing.txt")).code())
        }
    }

    @Test
    fun extractionWritesThroughTheTreeAndKeepsExistingFiles() {
        val output = File(directory, "out").apply { mkdirs() }
        File(output, "readme.txt").writeText("mine")
        withProvider { provider ->
            val tree = TestTree(output)
            val answer = call(provider, "extract", JSONObject(), tree = tree).result()
            assertEquals(4, answer.getInt("filesExtracted"))
            assertEquals(1, answer.getInt("kept"))
            assertEquals("[\"readme.txt\"]", answer.getJSONArray("keptPaths").toString())
            assertEquals("mine", File(output, "readme.txt").readText())
            assertEquals("beta 中文 ✓", File(output, "docs/deep/b.txt").readText())
            assertEquals(70_000L, File(output, "long.txt").length())

            val some = File(directory, "some").apply { mkdirs() }
            val partial = call(provider, "extract", JSONObject().put("paths", JSONArray().put("docs/deep")), tree = TestTree(some)).result()
            assertEquals(1, partial.getInt("filesExtracted"))
            assertTrue("selected entries keep their paths", File(some, "docs/deep/b.txt").isFile)
            assertFalse(File(some, "readme.txt").exists())

            val refused = invoke(provider, "extract", JSONObject(), noopCallback(), tree = null)
            assertFalse(refused.getBoolean(PluginToolAcceptKeys.ACCEPTED))
            assertTrue(refused.getString(PluginToolAcceptKeys.ERROR_MESSAGE)!!.contains("directory output files"))
        }
    }

    @Test
    fun encryptedArchivesNeedTheirPassword() {
        zip = File(directory, "secret.zip")
        Zip4jOutputStream(FileOutputStream(zip), PASSWORD.toCharArray()).use { output ->
            output.putNextEntry(ZipParameters().apply {
                fileNameInZip = "secret.txt"
                compressionMethod = CompressionMethod.DEFLATE
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            })
            output.write("hidden words".toByteArray())
            output.closeEntry()
        }
        withProvider { provider ->
            val listed = call(provider, "list", JSONObject()).result().getJSONArray("entries").getJSONObject(0)
            assertTrue(listed.getBoolean("encrypted"))
            val locked = call(provider, "read", JSONObject().put("path", "secret.txt"))
            assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, locked.code())
            assertTrue(locked.error!!.getString(PluginToolErrorKeys.MESSAGE)!!.contains("PASSWORD_REQUIRED"))
            assertEquals("hidden words", call(provider, "read", JSONObject().put("path", "secret.txt").put("password", PASSWORD)).result().getString("text"))
            assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, call(provider, "read", JSONObject().put("path", "secret.txt").put("password", "wrong")).code())
        }
    }

    @Test
    fun badRequestsAreRefused() {
        withProvider { provider ->
            assertEquals(PluginToolErrorCodes.UNKNOWN_TOOL, invoke(provider, "compress", JSONObject(), noopCallback()).getString(PluginToolAcceptKeys.ERROR_CODE))
            val noPath = invoke(provider, "read", JSONObject(), noopCallback())
            assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, noPath.getString(PluginToolAcceptKeys.ERROR_CODE))
            val text = File(directory, "notes.txt").apply { writeText("not an archive") }
            zip = text
            assertEquals(PluginToolErrorCodes.INPUT_UNREADABLE, call(provider, "list", JSONObject()).code())
        }
    }

    /** A directory output served the way AutoJs6 does, minus the staging: existing files answer EXISTS. */
    private class TestTree(private val root: File) : IPluginToolOutputTree.Stub() {
        override fun createDirectory(relativePath: String?): Bundle {
            File(root, relativePath!!).mkdirs()
            return Bundle().apply { putBoolean(PluginToolOutputTreeKeys.OK, true) }
        }

        override fun openFile(relativePath: String?, expectedBytes: Long, answer: Bundle?): ParcelFileDescriptor? {
            val file = File(root, relativePath!!)
            if (file.exists()) {
                answer?.putBoolean(PluginToolOutputTreeKeys.OK, false)
                answer?.putString(PluginToolOutputTreeKeys.ERROR_CODE, PluginToolOutputTreeErrors.EXISTS)
                return null
            }
            file.parentFile?.mkdirs()
            answer?.putBoolean(PluginToolOutputTreeKeys.OK, true)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_WRITE_ONLY)
        }
    }

    private class Answer(val bundle: Bundle?, val error: Bundle?) {
        fun result(): JSONObject {
            error?.let { throw AssertionError("the call failed: ${it.getString(PluginToolErrorKeys.CODE)} ${it.getString(PluginToolErrorKeys.MESSAGE)}") }
            return JSONObject(bundle!!.getString(PluginToolResultKeys.RESULT_JSON)!!)
        }

        fun code(): String? = error?.getString(PluginToolErrorKeys.CODE)
    }

    private fun call(provider: IPluginToolProvider, tool: String, arguments: JSONObject, tree: IPluginToolOutputTree? = null): Answer {
        val done = CountDownLatch(1)
        val result = AtomicReference<Answer?>()
        val callback = object : IPluginToolCallback.Stub() {
            override fun onProgress(progress: Bundle?) = Unit
            override fun onResult(bundle: Bundle?, outputs: Array<out ParcelFileDescriptor>?) {
                result.set(Answer(bundle, null))
                done.countDown()
            }

            override fun onError(error: Bundle?) {
                result.set(Answer(null, error))
                done.countDown()
            }
        }
        val accepted = invoke(provider, tool, arguments, callback, tree)
        assertTrue(accepted.getString(PluginToolAcceptKeys.ERROR_MESSAGE), accepted.getBoolean(PluginToolAcceptKeys.ACCEPTED))
        assertTrue("the $tool call did not answer", done.await(60, TimeUnit.SECONDS))
        return result.get()!!
    }

    private fun invoke(provider: IPluginToolProvider, tool: String, arguments: JSONObject, callback: IPluginToolCallback, tree: IPluginToolOutputTree? = null): Bundle {
        val inputs = JSONArray().put(JSONObject().put("name", "archive").put("source", PluginToolValues.INPUT_SOURCE_FILE).put("displayName", zip.name).put("byteCount", zip.length()))
        val descriptor = ParcelFileDescriptor.open(zip, ParcelFileDescriptor.MODE_READ_ONLY)
        val request = Bundle().apply {
            putInt(PluginToolInvokeKeys.CONTRACT_VERSION, PluginToolContract.CONTRACT_VERSION)
            putString(PluginToolInvokeKeys.INVOCATION_ID, UUID.randomUUID().toString())
            putString(PluginToolInvokeKeys.TOOL, tool)
            putString(PluginToolInvokeKeys.ARGUMENTS_JSON, arguments.toString())
            putString(PluginToolInvokeKeys.INPUTS_JSON, inputs.toString())
            putString(PluginToolInvokeKeys.OUTPUTS_JSON, if (tree != null) "[\"files\"]" else "[]")
            putLong(PluginToolInvokeKeys.TIMEOUT_MS, 60_000L)
            putString(PluginToolInvokeKeys.CONSUMER, PluginToolValues.CONSUMER_SCRIPT)
            tree?.let { putBundle(PluginToolInvokeKeys.OUTPUT_TREES, Bundle().apply { putBinder("files", it.asBinder()) }) }
        }
        try {
            return provider.invoke(request, arrayOf(descriptor), callback)
        } finally {
            runCatching { descriptor.close() }
        }
    }

    private fun noopCallback() = object : IPluginToolCallback.Stub() {
        override fun onProgress(progress: Bundle?) = Unit
        override fun onResult(bundle: Bundle?, outputs: Array<out ParcelFileDescriptor>?) = Unit
        override fun onError(error: Bundle?) = Unit
    }

    private fun withProvider(block: (IPluginToolProvider) -> Unit) {
        val intent = Intent(PluginToolContract.SERVICE_ACTION).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(intent, 0)
        assertEquals("the tool provider discovery must resolve exactly one service", 1, matches.size)
        val service = matches.single().serviceInfo
        assertEquals("${context.packageName}.tool.ArchiveToolService", service.name)
        assertEquals("org.autojs.permission.PLUGIN", service.permission)
        assertEquals("${context.packageName}:tool", service.processName)
        val latch = CountDownLatch(1)
        val binder = AtomicReference<IBinder?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binder.set(service)
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue(context.bindService(Intent(intent).setComponent(ComponentName(service.packageName, service.name)), connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("timed out binding the tool provider", latch.await(15, TimeUnit.SECONDS))
            val proxy = binder.get()
            assertNotNull(proxy)
            block(IPluginToolProvider.Stub.asInterface(proxy))
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PASSWORD = "s3cret!"
    }
}
