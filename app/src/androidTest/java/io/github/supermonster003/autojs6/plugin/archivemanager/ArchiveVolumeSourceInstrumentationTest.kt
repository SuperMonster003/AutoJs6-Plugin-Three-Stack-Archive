package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerArchiveVolumeSourceKeys
import org.autojs.plugin.explorer.api.IExplorerArchiveVolumeSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ArchiveVolumeSourceInstrumentationTest {

    @Test
    fun authorizedVolumeIsStagedOnceAndReleasedWithTheClient() = withTestRoot { root ->
        val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
        val volume = File(root, "set.z01").apply {
            writeBytes(ByteArray(32 * 1_024) { index -> (index * 13).toByte() })
        }
        val remote = MutableVolumeSource(volume)
        val client = requireNotNull(
            ExplorerArchiveVolumeSourceClient.fromBinder(remote.asBinder(), cache),
        )

        val first = requireNotNull(client.openSource(volume.name.uppercase()))
        val second = requireNotNull(client.openSource(volume.name))
        assertSame(first, second)
        assertArrayEquals(volume.readBytes(), first.openInputStream().use { it.readBytes() })
        assertEquals(1, remote.openCalls.get())
        assertEquals(client.volumes, client.inspectIdentities())

        client.close()
        client.close()
        expectFailure<IllegalStateException> { client.inspectIdentities() }
        assertTrue(cache.listFiles().isNullOrEmpty())
    }

    @Test
    fun duplicateCaseFoldedVolumeNamesAreRejectedBeforeOpen() = withTestRoot { root ->
        val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
        val volume = File(root, "set.z01").apply { writeText("volume") }
        val remote = MutableVolumeSource(volume).apply {
            records = listOf(
                record(id = "first", displayName = "set.z01"),
                record(id = "second", displayName = "SET.Z01"),
            )
        }

        expectFailure<IOException> {
            ExplorerArchiveVolumeSourceClient.fromBinder(remote.asBinder(), cache)
        }
        assertEquals(0, remote.openCalls.get())
        assertTrue(cache.listFiles().isNullOrEmpty())
    }

    @Test
    fun unsafeVolumeNameIsRejectedBeforeOpen() = withTestRoot { root ->
        val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
        val volume = File(root, "set.z01").apply { writeText("volume") }
        val remote = MutableVolumeSource(volume).apply {
            records = listOf(record(displayName = "../set.z01"))
        }

        expectFailure<IOException> {
            ExplorerArchiveVolumeSourceClient.fromBinder(remote.asBinder(), cache)
        }
        assertEquals(0, remote.openCalls.get())
    }

    @Test
    fun changedCatalogIsReportedAsAnArchiveVolumeChange() = withTestRoot { root ->
        val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
        val volume = File(root, "set.z01").apply { writeText("volume") }
        val remote = MutableVolumeSource(volume)
        val client = requireNotNull(
            ExplorerArchiveVolumeSourceClient.fromBinder(remote.asBinder(), cache),
        )
        remote.records = listOf(
            remote.record(lastModifiedMillis = volume.lastModified() + 1L),
        )

        try {
            expectFailure<ArchiveVolumeChangedException> { client.inspectIdentities() }
            assertEquals(0, remote.openCalls.get())
        } finally {
            client.close()
        }
    }

    private fun withTestRoot(block: (File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "archive-volume-client-${UUID.randomUUID()}")
        assertTrue(root.mkdirs())
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    private inline fun <reified T : Throwable> expectFailure(block: () -> Unit): T {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return error
            throw AssertionError(
                "Expected ${T::class.java.name}, but received ${error.javaClass.name}",
                error,
            )
        }
        throw AssertionError("Expected ${T::class.java.name}")
    }

    private class MutableVolumeSource(
        private val volume: File,
    ) : IExplorerArchiveVolumeSource.Stub() {
        val openCalls = AtomicInteger(0)
        var records = listOf(record())

        override fun getCatalog(): Bundle = Bundle().apply {
            putParcelableArrayList(
                ExplorerArchiveVolumeSourceKeys.VOLUMES,
                ArrayList(records.map(VolumeRecord::toBundle)),
            )
        }

        override fun openVolume(volumeId: String): ParcelFileDescriptor {
            require(records.any { record -> record.id == volumeId })
            openCalls.incrementAndGet()
            return ParcelFileDescriptor.open(volume, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        fun record(
            id: String = "authorized-volume",
            displayName: String = volume.name,
            size: Long = volume.length(),
            lastModifiedMillis: Long = volume.lastModified(),
        ) = VolumeRecord(id, displayName, size, lastModifiedMillis)
    }

    private data class VolumeRecord(
        val id: String,
        val displayName: String,
        val size: Long,
        val lastModifiedMillis: Long,
    ) {
        fun toBundle(): Bundle = Bundle().apply {
            putString(ExplorerArchiveVolumeSourceKeys.VOLUME_ID, id)
            putString(ExplorerArchiveVolumeSourceKeys.DISPLAY_NAME, displayName)
            putLong(ExplorerArchiveVolumeSourceKeys.SIZE, size)
            putLong(ExplorerArchiveVolumeSourceKeys.LAST_MODIFIED, lastModifiedMillis)
        }
    }
}
