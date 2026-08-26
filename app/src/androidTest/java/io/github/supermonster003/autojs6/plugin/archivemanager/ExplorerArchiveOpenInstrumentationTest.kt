package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenValues
import org.autojs.plugin.explorer.api.ExplorerArchiveRequestKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.IExplorerActionPlugin
import org.autojs.plugin.explorer.api.IExplorerArchiveSession
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ExplorerArchiveOpenInstrumentationTest {

    @Test
    fun externalSolidSevenZStreamsOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val archive = File(context.cacheDir, "archive-open-solid.7z")
        archive.writeBytes(Base64.decode(SOLID_FIXTURE_BASE64, Base64.DEFAULT))
        val snapshot = ArchiveScanner().scan(archive)
        try {
            assertEquals(
                listOf("资料", "empty.bin", "plain.txt", "资料/说明.md"),
                snapshot.entries.map(ArchiveEntry::path),
            )
            val plainEntry = snapshot.entries.single { it.path == "plain.txt" }
            assertEquals(
                EXPECTED_PLAIN_CRC,
                plainEntry.crc32,
            )
            val output = ByteArrayOutputStream()
            ArchiveEntryStreamer(archive, snapshot).stream(
                snapshot.entries.single { it.path == "plain.txt" },
                output,
            )
            assertArrayEquals(EXPECTED_PLAIN_BYTES, output.toByteArray())
        } finally {
            snapshot.readerOptions.clearPassword()
            assertTrue(archive.delete())
        }
    }

    @Test
    fun headerEncryptedSevenZUsesTypedOpenRetriesAndClearsPasswords() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val archive = File(context.cacheDir, "archive-open-header-encrypted.7z")
        archive.writeBytes(Base64.decode(FIXTURE_BASE64, Base64.DEFAULT))
        val directSnapshot = ArchiveScanner().scan(
            archive,
            ArchiveReaderOptions(password = PASSWORD.toCharArray()),
        )
        try {
            val directEntry = directSnapshot.entries.single { it.path == "plain.txt" }
            val directOutput = ByteArrayOutputStream()
            ArchiveEntryStreamer(archive, directSnapshot).stream(
                directEntry,
                directOutput,
            )
            assertArrayEquals(EXPECTED_PLAIN_BYTES, directOutput.toByteArray())
        } finally {
            directSnapshot.readerOptions.clearPassword()
        }
        val serviceBinder = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                serviceBinder.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        assertTrue(
            context.bindService(
                Intent(context, ExplorerActionService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            ),
        )

        var session: IExplorerArchiveSession? = null
        try {
            assertTrue("Timed out binding Explorer Action service", connected.await(10, TimeUnit.SECONDS))
            val plugin = IExplorerActionPlugin.Stub.asInterface(requireNotNull(serviceBinder.get()))

            val missing = openArchive(plugin, archive)
            assertEquals(
                ExplorerArchiveOpenValues.ERROR_PASSWORD_REQUIRED,
                missing.getInt(ExplorerArchiveOpenKeys.ERROR_CODE),
            )
            assertNull(missing.getBinder(ExplorerArchiveOpenKeys.SESSION_BINDER))

            val wrongPassword = "wrong-password".toCharArray()
            val wrong = openArchive(plugin, archive, wrongPassword)
            assertTrue(wrongPassword.all { it == '\u0000' })
            assertEquals(
                ExplorerArchiveOpenValues.ERROR_WRONG_PASSWORD,
                wrong.getInt(ExplorerArchiveOpenKeys.ERROR_CODE),
            )
            assertNull(wrong.getBinder(ExplorerArchiveOpenKeys.SESSION_BINDER))

            val correctPassword = PASSWORD.toCharArray()
            val opened = openArchive(plugin, archive, correctPassword)
            assertTrue(correctPassword.all { it == '\u0000' })
            assertEquals(
                ExplorerArchiveOpenValues.ERROR_NONE,
                opened.getInt(ExplorerArchiveOpenKeys.ERROR_CODE),
            )
            session = requireNotNull(
                IExplorerArchiveSession.Stub.asInterface(
                    opened.getBinder(ExplorerArchiveOpenKeys.SESSION_BINDER),
                ),
            )
            val page = session.listChildren(
                session.info().getString(ExplorerArchiveSessionKeys.ROOT_ID),
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            )
            @Suppress("DEPRECATION")
            val entries = page.getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
                .orEmpty()
            val plainEntry = entries.single {
                it.getString(ExplorerArchiveSessionKeys.NAME) == "plain.txt"
            }
            assertTrue(plainEntry.getBoolean(ExplorerArchiveSessionKeys.CAN_OPEN))
            val descriptor = session.openEntry(
                requireNotNull(plainEntry.getString(ExplorerArchiveSessionKeys.ID)),
            )
            val bytes = ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            assertArrayEquals(EXPECTED_PLAIN_BYTES, bytes)
        } finally {
            session?.close()
            context.unbindService(connection)
            assertTrue(archive.delete())
        }
    }

    private fun openArchive(
        plugin: IExplorerActionPlugin,
        archive: File,
        password: CharArray? = null,
    ): Bundle {
        val request = Bundle().apply {
            putString(ExplorerArchiveRequestKeys.DISPLAY_NAME, archive.name)
            putLong(ExplorerArchiveRequestKeys.SIZE, archive.length())
            putLong(ExplorerArchiveRequestKeys.LAST_MODIFIED, archive.lastModified())
            password?.let { putCharArray(ExplorerArchiveRequestKeys.PASSWORD, it) }
        }
        val descriptor = ParcelFileDescriptor.open(
            archive,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        // This test binds a local Binder, so no process boundary duplicates the descriptor.
        // The service owns and closes it just as it owns the marshalled copy in production.
        val result = plugin.openArchiveV11(descriptor, request)
        assertFalse(request.containsKey(ExplorerArchiveRequestKeys.PASSWORD))
        return result
    }

    private fun IExplorerArchiveSession.info(): Bundle = getInfo()

    private companion object {
        const val FIXTURE_BASE64 =
            "N3q8ryccAARMQ6T5AAEAAAAAAAA+AAAAAAAAAP6z/wK6bTuz+D587CsgJEmAbBI4l4GxT81nBONjSCyPqxYEH+Bj4K3VWdzPQLaM2JJLrHGHDAeE/Wp34YWZFt8KXmJGTFtd2BvZA2EW0zASfXjCbnBKOpTPldTix2uylGda1urkWqdO2m0xcS9vyZbIMQ+hnV7slmTv3yhJOvXCtHxb/gr0oxIwHDSFW7HTDc8a+lMZyYgIRJSUmH0JLPbZvGhOXi9RUL3iooPNIP0bOmfInMb076meIG1vHuRx45rPzxoq5cWZuxHrS8YJG+EIotWwMmEyIDnvYwdpPgONECma9ox/x1tKcC6Xs+Dn/swuNl1gVBqqG3tptEslokQdtPdrFwZQAQmAsAAHCwEAAiQG8QcBElMP3GqU402p1mvr4L8ZSx8AByMDAQEFXQAQAAABAAyAo4DOCgGyX+zeAAA="
        const val SOLID_FIXTURE_BASE64 =
            "N3q8ryccAARYHq5O0wAAAAAAAAAiAAAAAAAAAE0lNTEBAEpleHRlcm5hbCBzZXZlbiB6aXAgcGF5bG9hZAojIDdaIOWFvOWuueaApwoK5Zu65a6e5qGj5qGI5LiOIFVuaWNvZGUg6Lev5b6ELgoAAACBMweuD9K0iL1AwJDS/31pTYWReB5L+LlZgagXS3YrHdhkTZ8EF/5GVIgU1kGWiA4Zo8DLnLeE4rjv+RqUedOI4QmTF7yYjXS7/kQ2o5kR08034CxsjGK8q3lA4uudC+wQ+k6OerxiDYds4CLpg869qWJ+XqNgD5mmte4Efy/k9SAAFwZPAQmAhAAHCwEAASMDAQEFXQAQAAAMgK4KAYKqCDwAAA=="
        const val PASSWORD = "archive-secret"
        const val EXPECTED_PLAIN_CRC = 0x6501718CL
        val EXPECTED_PLAIN_BYTES = "external seven zip payload\n".toByteArray()
    }
}
