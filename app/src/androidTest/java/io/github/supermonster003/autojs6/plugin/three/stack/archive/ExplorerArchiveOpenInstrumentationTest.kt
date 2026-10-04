package io.github.supermonster003.autojs6.plugin.three.stack.archive

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
import net.lingala.zip4j.io.outputstream.SplitOutputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenValues
import org.autojs.plugin.explorer.api.ExplorerArchiveRequestKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveVolumeSourceKeys
import org.autojs.plugin.explorer.api.IExplorerActionPlugin
import org.autojs.plugin.explorer.api.IExplorerArchiveSession
import org.autojs.plugin.explorer.api.IExplorerArchiveVolumeSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.CRC32

@RunWith(AndroidJUnit4::class)
class ExplorerArchiveOpenInstrumentationTest {

    @Test
    fun sessionReportsDetectedFormatForRecognizedAndDisguisedArchiveNames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "archive-open-format-${UUID.randomUUID()}")
        assertTrue(root.mkdirs())
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

        try {
            assertTrue("Timed out binding Explorer Action service", connected.await(10, TimeUnit.SECONDS))
            val plugin = IExplorerActionPlugin.Stub.asInterface(requireNotNull(serviceBinder.get()))
            listOf(
                "recognized.zip" to true,
                "disguised.data" to false,
            ).forEach { (fileName, displayNameMatchesFormat) ->
                val archive = File(root, fileName)
                createStoredZip(archive, "format metadata".toByteArray())
                var session: IExplorerArchiveSession? = null
                try {
                    val opened = openArchive(plugin, archive)
                    assertEquals(
                        opened.getString(ExplorerArchiveOpenKeys.ERROR_MESSAGE),
                        ExplorerArchiveOpenValues.ERROR_NONE,
                        opened.getInt(ExplorerArchiveOpenKeys.ERROR_CODE),
                    )
                    session = requireNotNull(
                        IExplorerArchiveSession.Stub.asInterface(
                            opened.getBinder(ExplorerArchiveOpenKeys.SESSION_BINDER),
                        ),
                    )
                    val info = session.info()
                    assertEquals(ArchiveFormat.ZIP.id, info.getString(ExplorerArchiveSessionKeys.FORMAT_ID))
                    assertEquals(
                        ArchiveFormat.ZIP.displayName,
                        info.getString(ExplorerArchiveSessionKeys.FORMAT_DISPLAY_NAME),
                    )
                    assertEquals(
                        displayNameMatchesFormat,
                        info.getBoolean(ExplorerArchiveSessionKeys.DISPLAY_NAME_MATCHES_FORMAT),
                    )
                } finally {
                    session?.close()
                }
            }
        } finally {
            context.unbindService(connection)
            root.deleteRecursively()
        }
    }

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

    @Test
    fun hostAuthorizedSplitZipVolumesRoundTripThroughTheExplorerService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "archive-open-volumes-${UUID.randomUUID()}")
        assertTrue(root.mkdirs())
        val payload = ByteArray(SPLIT_PAYLOAD_BYTES) { index -> (index * 31).toByte() }
        val terminal = File(root, "authorized-split.zip")
        SplitOutputStream(terminal, SPLIT_VOLUME_BYTES).use { splitOutput ->
            ZipOutputStream(splitOutput).use { zipOutput ->
                zipOutput.putNextEntry(
                    ZipParameters().apply {
                        fileNameInZip = SPLIT_ENTRY_NAME
                        compressionMethod = CompressionMethod.STORE
                        entrySize = payload.size.toLong()
                        entryCRC = CRC32().apply { update(payload) }.value
                    },
                )
                zipOutput.write(payload)
                zipOutput.closeEntry()
            }
        }
        val firstVolume = File(root, "authorized-split.z01")
        assertTrue(firstVolume.isFile)
        assertTrue(terminal.isFile)
        val volumeSource = TestArchiveVolumeSource(firstVolume)
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
            val opened = openArchive(plugin, terminal, volumeSource = volumeSource.asBinder())
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
            val entry = entries.single {
                it.getString(ExplorerArchiveSessionKeys.NAME) == SPLIT_ENTRY_NAME
            }
            assertTrue(entry.getBoolean(ExplorerArchiveSessionKeys.CAN_OPEN))
            val descriptor = session.openEntry(
                requireNotNull(entry.getString(ExplorerArchiveSessionKeys.ID)),
            )
            val actual = ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            assertArrayEquals(payload, actual)
            assertEquals(1, volumeSource.openCalls.get())
        } finally {
            session?.close()
            context.unbindService(connection)
            root.deleteRecursively()
        }
    }

    @Test
    fun hostAuthorizedNumberedZipAndSevenZVolumesRoundTripThroughTheExplorerService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "archive-open-numbered-${UUID.randomUUID()}")
        assertTrue(root.mkdirs())
        val payload = ByteArray(NUMBERED_PAYLOAD_BYTES).also {
            Random(NUMBERED_PAYLOAD_SEED).nextBytes(it)
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

        try {
            assertTrue("Timed out binding Explorer Action service", connected.await(10, TimeUnit.SECONDS))
            val plugin = IExplorerActionPlugin.Stub.asInterface(requireNotNull(serviceBinder.get()))
            listOf(ArchiveFormat.ZIP, ArchiveFormat.SEVEN_Z).forEach { format ->
                val source = File(root, "numbered-source.${format.primaryExtension}")
                when (format) {
                    ArchiveFormat.ZIP -> createStoredZip(source, payload)
                    ArchiveFormat.SEVEN_Z -> createCopySevenZ(source, payload)
                    else -> error("Unexpected numbered-volume test format")
                }
                val volumes = splitIntoThreeNumberedVolumes(source)
                val volumeSource = TestArchiveVolumeSource(volumes.drop(1))
                var session: IExplorerArchiveSession? = null
                try {
                    val opened = openArchive(
                        plugin = plugin,
                        archive = volumes.first(),
                        volumeSource = volumeSource.asBinder(),
                    )
                    assertEquals(
                        opened.getString(ExplorerArchiveOpenKeys.ERROR_MESSAGE),
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
                    val entries = page.getParcelableArrayList<Bundle>(
                        ExplorerArchiveSessionKeys.ITEMS,
                    ).orEmpty()
                    val entry = entries.single {
                        it.getString(ExplorerArchiveSessionKeys.NAME) == SPLIT_ENTRY_NAME
                    }
                    assertTrue(entry.getBoolean(ExplorerArchiveSessionKeys.CAN_OPEN))
                    val descriptor = session.openEntry(
                        requireNotNull(entry.getString(ExplorerArchiveSessionKeys.ID)),
                    )
                    val actual = ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
                        it.readBytes()
                    }
                    assertArrayEquals(payload, actual)
                    assertEquals(2, volumeSource.openCalls.get())
                } finally {
                    session?.close()
                }
            }
        } finally {
            context.unbindService(connection)
            root.deleteRecursively()
        }
    }

    private fun createStoredZip(target: File, payload: ByteArray) {
        ZipOutputStream(FileOutputStream(target)).use { output ->
            output.putNextEntry(
                ZipParameters().apply {
                    fileNameInZip = SPLIT_ENTRY_NAME
                    compressionMethod = CompressionMethod.STORE
                    entrySize = payload.size.toLong()
                    entryCRC = CRC32().apply { update(payload) }.value
                },
            )
            output.write(payload)
            output.closeEntry()
        }
    }

    private fun createCopySevenZ(target: File, payload: ByteArray) {
        FileOutputStream(target).use { fileOutput ->
            SevenZOutputFile(fileOutput.channel).use { output ->
                output.setContentCompression(SevenZMethod.COPY)
                output.putArchiveEntry(SevenZArchiveEntry().apply { name = SPLIT_ENTRY_NAME })
                output.write(payload)
                output.closeArchiveEntry()
            }
        }
    }

    private fun splitIntoThreeNumberedVolumes(source: File): List<File> {
        val bytes = source.readBytes()
        assertTrue(bytes.size > 3)
        val chunkSize = (bytes.size + 2) / 3
        val volumes = bytes.asList().chunked(chunkSize).mapIndexed { index, chunk ->
            File(source.parentFile, "${source.name}.${(index + 1).toString().padStart(3, '0')}")
                .apply { writeBytes(chunk.toByteArray()) }
        }
        assertEquals(3, volumes.size)
        assertTrue(source.delete())
        return volumes
    }

    private fun openArchive(
        plugin: IExplorerActionPlugin,
        archive: File,
        password: CharArray? = null,
        volumeSource: IBinder? = null,
    ): Bundle {
        val request = Bundle().apply {
            putString(ExplorerArchiveRequestKeys.DISPLAY_NAME, archive.name)
            putLong(ExplorerArchiveRequestKeys.SIZE, archive.length())
            putLong(ExplorerArchiveRequestKeys.LAST_MODIFIED, archive.lastModified())
            password?.let { putCharArray(ExplorerArchiveRequestKeys.PASSWORD, it) }
            volumeSource?.let {
                putBinder(ExplorerArchiveRequestKeys.VOLUME_SOURCE_BINDER, it)
            }
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

    private class TestArchiveVolumeSource(
        private val volumes: List<File>,
    ) : IExplorerArchiveVolumeSource.Stub() {
        constructor(volume: File) : this(listOf(volume))

        val openCalls = AtomicInteger(0)

        private val volumesById = volumes.mapIndexed { index, volume ->
            "authorized-volume-${index + 1}" to volume
        }.toMap()

        override fun getCatalog(): Bundle = Bundle().apply {
            putParcelableArrayList(
                ExplorerArchiveVolumeSourceKeys.VOLUMES,
                ArrayList(volumesById.map { (id, volume) ->
                    Bundle().apply {
                        putString(ExplorerArchiveVolumeSourceKeys.VOLUME_ID, id)
                        putString(ExplorerArchiveVolumeSourceKeys.DISPLAY_NAME, volume.name)
                        putLong(ExplorerArchiveVolumeSourceKeys.SIZE, volume.length())
                        putLong(
                            ExplorerArchiveVolumeSourceKeys.LAST_MODIFIED,
                            volume.lastModified(),
                        )
                    }
                }),
            )
        }

        override fun openVolume(volumeId: String): ParcelFileDescriptor {
            val volume = requireNotNull(volumesById[volumeId])
            openCalls.incrementAndGet()
            return ParcelFileDescriptor.open(volume, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    private fun IExplorerArchiveSession.info(): Bundle = getInfo()

    private companion object {
        const val FIXTURE_BASE64 =
            "N3q8ryccAARMQ6T5AAEAAAAAAAA+AAAAAAAAAP6z/wK6bTuz+D587CsgJEmAbBI4l4GxT81nBONjSCyPqxYEH+Bj4K3VWdzPQLaM2JJLrHGHDAeE/Wp34YWZFt8KXmJGTFtd2BvZA2EW0zASfXjCbnBKOpTPldTix2uylGda1urkWqdO2m0xcS9vyZbIMQ+hnV7slmTv3yhJOvXCtHxb/gr0oxIwHDSFW7HTDc8a+lMZyYgIRJSUmH0JLPbZvGhOXi9RUL3iooPNIP0bOmfInMb076meIG1vHuRx45rPzxoq5cWZuxHrS8YJG+EIotWwMmEyIDnvYwdpPgONECma9ox/x1tKcC6Xs+Dn/swuNl1gVBqqG3tptEslokQdtPdrFwZQAQmAsAAHCwEAAiQG8QcBElMP3GqU402p1mvr4L8ZSx8AByMDAQEFXQAQAAABAAyAo4DOCgGyX+zeAAA="
        const val SOLID_FIXTURE_BASE64 =
            "N3q8ryccAARYHq5O0wAAAAAAAAAiAAAAAAAAAE0lNTEBAEpleHRlcm5hbCBzZXZlbiB6aXAgcGF5bG9hZAojIDdaIOWFvOWuueaApwoK5Zu65a6e5qGj5qGI5LiOIFVuaWNvZGUg6Lev5b6ELgoAAACBMweuD9K0iL1AwJDS/31pTYWReB5L+LlZgagXS3YrHdhkTZ8EF/5GVIgU1kGWiA4Zo8DLnLeE4rjv+RqUedOI4QmTF7yYjXS7/kQ2o5kR08034CxsjGK8q3lA4uudC+wQ+k6OerxiDYds4CLpg869qWJ+XqNgD5mmte4Efy/k9SAAFwZPAQmAhAAHCwEAASMDAQEFXQAQAAAMgK4KAYKqCDwAAA=="
        const val PASSWORD = "archive-secret"
        const val SPLIT_ENTRY_NAME = "资料.bin"
        const val SPLIT_PAYLOAD_BYTES = 70_000
        const val SPLIT_VOLUME_BYTES = 65_536L
        const val NUMBERED_PAYLOAD_BYTES = 70_000
        const val NUMBERED_PAYLOAD_SEED = 0x5A17C0DEL
        const val EXPECTED_PLAIN_CRC = 0x6501718CL
        val EXPECTED_PLAIN_BYTES = "external seven zip payload\n".toByteArray()
    }
}
