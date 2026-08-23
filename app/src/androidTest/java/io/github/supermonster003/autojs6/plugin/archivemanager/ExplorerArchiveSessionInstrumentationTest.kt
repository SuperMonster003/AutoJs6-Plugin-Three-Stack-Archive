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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
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

    @Test
    fun sessionListsAndStreamsTarEntriesThroughTheHostContract() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "archive-input-tar-session-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val archive = File(directory, "source.tar")
        val expected = "host TAR preview".toByteArray()
        TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(archive))).use { output ->
            val entry = TarArchiveEntry("preview.txt").apply {
                size = expected.size.toLong()
                setModTime(1_700_000_000_000L)
            }
            output.putArchiveEntry(entry)
            output.write(expected)
            output.closeArchiveEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "session-test.tar",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        try {
            val page = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            )
            val item = page
                .getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
                .orEmpty()
                .single()

            assertEquals("preview.txt", item.getString(ExplorerArchiveSessionKeys.NAME))
            assertTrue(item.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT))
            val actual = ParcelFileDescriptor.AutoCloseInputStream(
                session.openEntry(requireNotNull(item.getString(ExplorerArchiveSessionKeys.ID))),
            ).use { it.readBytes() }
            assertEquals(expected.toList(), actual.toList())
        } finally {
            session.close()
        }

        assertFalse(archive.exists())
        assertFalse(directory.exists())
    }

    @Test
    fun sessionListsAndStreamsCompressedTarEntriesThroughTheHostContract() {
        listOf(
            CompressedTarCase("tgz", ArchiveFormat.TAR_GZIP, ::GzipCompressorOutputStream),
            CompressedTarCase("txz", ArchiveFormat.TAR_XZ, ::XZCompressorOutputStream),
            CompressedTarCase("tbz2", ArchiveFormat.TAR_BZIP2, ::BZip2CompressorOutputStream),
            CompressedTarCase(
                "tzst",
                ArchiveFormat.TAR_ZSTD,
                { output ->
                    ZstdCompressorOutputStream.builder().apply {
                        setOutputStream(output)
                        setLevel(3)
                        setChecksum(true)
                    }.get()
                },
            ),
        ).forEach(::verifyCompressedTarSession)
    }

    private fun verifyCompressedTarSession(case: CompressedTarCase) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(
            context.cacheDir,
            "archive-input-${case.extension}-session-test-${UUID.randomUUID()}",
        )
        assertTrue(directory.mkdirs())
        val archive = File(directory, "source.${case.extension}")
        val expected = "host ${case.format.displayName} preview".toByteArray()
        val compressed = case.compressor(BufferedOutputStream(FileOutputStream(archive)))
        TarArchiveOutputStream(compressed).use { output ->
            val entry = TarArchiveEntry("preview.txt").apply {
                size = expected.size.toLong()
                setModTime(1_700_000_000_000L)
            }
            output.putArchiveEntry(entry)
            output.write(expected)
            output.closeArchiveEntry()
        }
        val snapshot = ArchiveScanner().scan(archive)
        assertEquals(case.format, snapshot.format)

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "session-test.${case.extension}",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = snapshot,
            onClosed = {},
        )
        try {
            val page = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            )
            val item = page
                .getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
                .orEmpty()
                .single()
            val actual = ParcelFileDescriptor.AutoCloseInputStream(
                session.openEntry(requireNotNull(item.getString(ExplorerArchiveSessionKeys.ID))),
            ).use { it.readBytes() }

            assertEquals("preview.txt", item.getString(ExplorerArchiveSessionKeys.NAME))
            assertTrue(item.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT))
            assertEquals(expected.toList(), actual.toList())
        } finally {
            session.close()
        }

        assertFalse(archive.exists())
        assertFalse(directory.exists())
    }

    @Test
    fun sessionListsUnsafePathsInAReadOnlyFolderAndStillStreamsTheirData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "archive-input-unsafe-session-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val archive = File(directory, "unsafe.zip")
        val expected = "read-only unsafe preview".toByteArray()
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("../preview.txt"))
            output.write(expected)
            output.closeEntry()
        }
        val snapshot = ArchiveScanner().scan(archive)
        assertEquals(ArchiveEntryPathStatus.UNSAFE_ISOLATED, snapshot.entries.single().pathStatus)

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "unsafe.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = snapshot,
            isolatedPathDisplayName = "Quarantined paths",
            onClosed = {},
        )
        try {
            val rootPage = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            )
            val isolatedFolder = rootPage
                .getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
                .orEmpty()
                .single()
            assertEquals(
                ExplorerArchiveSessionValues.KIND_DIRECTORY,
                isolatedFolder.getInt(ExplorerArchiveSessionKeys.KIND),
            )
            assertEquals(
                "Quarantined paths",
                isolatedFolder.getString(ExplorerArchiveSessionKeys.NAME),
            )

            val isolatedPage = session.listChildren(
                requireNotNull(isolatedFolder.getString(ExplorerArchiveSessionKeys.ID)),
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            )
            val isolatedEntry = isolatedPage
                .getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
                .orEmpty()
                .single()
            assertEquals("../preview.txt", isolatedEntry.getString(ExplorerArchiveSessionKeys.NAME))
            assertTrue(isolatedEntry.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT))

            val actual = ParcelFileDescriptor.AutoCloseInputStream(
                session.openEntry(requireNotNull(isolatedEntry.getString(ExplorerArchiveSessionKeys.ID))),
            ).use { it.readBytes() }
            assertEquals(expected.toList(), actual.toList())
        } finally {
            session.close()
        }

        assertFalse(archive.exists())
        assertFalse(directory.exists())
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

    private data class CompressedTarCase(
        val extension: String,
        val format: ArchiveFormat,
        val compressor: (OutputStream) -> OutputStream,
    )
}
