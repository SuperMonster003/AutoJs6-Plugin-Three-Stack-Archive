@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ExplorerArchiveSessionInstrumentationTest {

    @Test
    fun sessionPagesRootListsNestedDirectoryAndDeletesStagedInputOnClose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "archive-input-session-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val archive = File(directory, "source.archive")
        createArchive(archive)

        var closed = false
        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "session-test.zip",
            stagedArchive = StagedArchive(archive, archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = { closed = true },
        )

        val info = session.info
        assertEquals("session-test.zip", info.getString(ExplorerArchiveSessionKeys.DISPLAY_NAME))
        assertEquals("root", info.getString(ExplorerArchiveSessionKeys.ROOT_ID))
        assertTrue(info.getBoolean(ExplorerArchiveSessionKeys.CAN_OPEN_ENTRIES))

        val firstPage = session.listChildren(
            "root",
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        )
        val firstItems = firstPage.getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty()
        assertEquals(ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE, firstItems.size)
        assertFalse(firstPage.getBoolean(ExplorerArchiveSessionKeys.COMPLETE))
        assertEquals(
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            firstPage.getInt(ExplorerArchiveSessionKeys.NEXT_OFFSET),
        )

        val secondPage = session.listChildren(
            "root",
            firstPage.getInt(ExplorerArchiveSessionKeys.NEXT_OFFSET),
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        )
        val secondItems = secondPage.getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty()
        assertEquals(3, secondItems.size)
        assertTrue(secondPage.getBoolean(ExplorerArchiveSessionKeys.COMPLETE))
        assertEquals(131, secondPage.getInt(ExplorerArchiveSessionKeys.NEXT_OFFSET))

        val folder = firstItems.single { item ->
            item.getString(ExplorerArchiveSessionKeys.NAME) == "folder"
        }
        assertEquals(
            ExplorerArchiveSessionValues.KIND_DIRECTORY,
            folder.getInt(ExplorerArchiveSessionKeys.KIND),
        )
        val nestedPage = session.listChildren(
            requireNotNull(folder.getString(ExplorerArchiveSessionKeys.ID)),
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        )
        val nestedItems = nestedPage.getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty()
        assertEquals(listOf("nested.txt"), nestedItems.map { it.getString(ExplorerArchiveSessionKeys.NAME) })
        assertTrue(nestedPage.getBoolean(ExplorerArchiveSessionKeys.COMPLETE))

        val nestedFile = nestedItems.single()
        val nestedContent = ParcelFileDescriptor.AutoCloseInputStream(
            session.openEntry(requireNotNull(nestedFile.getString(ExplorerArchiveSessionKeys.ID))),
        ).use { input -> input.readBytes().decodeToString() }
        assertEquals("nested", nestedContent)
        assertTrue(
            runCatching {
                session.openEntry(requireNotNull(folder.getString(ExplorerArchiveSessionKeys.ID)))
            }.isFailure,
        )

        session.close()
        assertTrue(closed)
        assertFalse(archive.exists())
        assertFalse(directory.exists())
        assertTrue(runCatching { session.info }.isFailure)
        assertTrue(
            runCatching {
                session.openEntry(requireNotNull(nestedFile.getString(ExplorerArchiveSessionKeys.ID)))
            }.isFailure,
        )
    }

    private fun createArchive(target: File) {
        ZipOutputStream(FileOutputStream(target)).use { output ->
            output.putNextEntry(ZipEntry("folder/"))
            output.closeEntry()
            output.putNextEntry(ZipEntry("folder/nested.txt"))
            output.write("nested".toByteArray())
            output.closeEntry()
            repeat(130) { index ->
                output.putNextEntry(ZipEntry("file-${index.toString().padStart(3, '0')}.txt"))
                output.write("entry-$index".toByteArray())
                output.closeEntry()
            }
        }
    }
}
