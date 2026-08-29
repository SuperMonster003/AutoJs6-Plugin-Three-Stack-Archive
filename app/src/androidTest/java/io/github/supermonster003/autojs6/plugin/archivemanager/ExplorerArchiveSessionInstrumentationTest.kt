@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveMutationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
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
        assertTrue(info.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT_ENTRIES))

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
            assertFalse(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_DELETE_ENTRIES))
            assertFalse(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_RENAME_ENTRIES))
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
            assertFalse(isolatedEntry.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT))
            assertFalse(isolatedEntry.getBoolean(ExplorerArchiveSessionKeys.CAN_DELETE))
            assertFalse(isolatedEntry.getBoolean(ExplorerArchiveSessionKeys.CAN_RENAME))

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

    @Test
    fun unavailableOnlySnapshotDoesNotAdvertiseHostExtraction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "archive-input-unavailable-session-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val archive = File(directory, "source.zip")
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("split-member.bin"))
            output.write(byteArrayOf(1, 2, 3))
            output.closeEntry()
        }
        val scanned = ArchiveScanner().scan(archive)
        val unavailable = scanned.entries.single().copy(
            capabilities = ArchiveEntryCapabilities(
                canOpen = false,
                canExtract = false,
                canDelete = false,
                canRename = false,
                limitations = setOf(
                    ArchiveEntryLimitation.MISSING_VOLUME,
                    ArchiveEntryLimitation.MUTATION_UNAVAILABLE,
                ),
            ),
        )
        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "split-part01.rar",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = scanned.copy(entries = listOf(unavailable)),
            onClosed = {},
        )

        try {
            assertFalse(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT_ENTRIES))
            assertFalse(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_DELETE_ENTRIES))
            assertFalse(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_RENAME_ENTRIES))
            val item = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty().single()
            assertFalse(item.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT))
            assertFalse(item.getBoolean(ExplorerArchiveSessionKeys.CAN_DELETE))
            assertFalse(item.getBoolean(ExplorerArchiveSessionKeys.CAN_RENAME))
        } finally {
            session.close()
        }

        assertFalse(archive.exists())
        assertFalse(directory.exists())
    }

    @Test
    fun filenameCharsetReindexPreservesStableIdsAndRollsBackInvalidRequests() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "archive-input-filename-charset-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val archive = File(directory, "legacy.zip")
        val expected = "legacy filename content".encodeToByteArray()
        ZipOutputStream(FileOutputStream(archive), Charset.forName("GB18030")).use { output ->
            output.putNextEntry(ZipEntry("目录/文件.txt"))
            output.write(expected)
            output.closeEntry()
        }
        val snapshot = ArchiveScanner().scan(archive)
        assertEquals("GB18030", snapshot.readerOptions.filenameCharsetName)
        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "legacy.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = snapshot,
            onClosed = {},
        )

        try {
            val initialInfo = session.info
            assertEquals(
                "GB18030",
                initialInfo.getString(ExplorerArchiveSessionKeys.FILENAME_CHARSET_NAME),
            )
            assertNull(initialInfo.getString(ExplorerArchiveSessionKeys.FILENAME_CHARSET_OVERRIDE))
            assertTrue(
                initialInfo
                    .getStringArrayList(ExplorerArchiveSessionKeys.FILENAME_CHARSET_NAMES)
                    .orEmpty()
                    .containsAll(listOf("UTF-8", "GB18030", "IBM437")),
            )
            val initialFolder = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty().single()
            val stableFolderId = requireNotNull(
                initialFolder.getString(ExplorerArchiveSessionKeys.ID),
            )
            assertEquals("目录", initialFolder.getString(ExplorerArchiveSessionKeys.NAME))

            val overriddenInfo = session.reindexFilenameCharset("ibm437")
            assertEquals(
                "IBM437",
                overriddenInfo.getString(ExplorerArchiveSessionKeys.FILENAME_CHARSET_OVERRIDE),
            )
            val mojibakeFolder = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty().single()
            assertEquals(stableFolderId, mojibakeFolder.getString(ExplorerArchiveSessionKeys.ID))
            assertNotEquals("目录", mojibakeFolder.getString(ExplorerArchiveSessionKeys.NAME))

            assertTrue(runCatching { session.reindexFilenameCharset("not-a-charset") }.isFailure)
            assertEquals(
                "IBM437",
                session.info.getString(ExplorerArchiveSessionKeys.FILENAME_CHARSET_OVERRIDE),
            )

            val automaticInfo = session.reindexFilenameCharset(null)
            assertEquals(
                "GB18030",
                automaticInfo.getString(ExplorerArchiveSessionKeys.FILENAME_CHARSET_NAME),
            )
            assertFalse(automaticInfo.containsKey(ExplorerArchiveSessionKeys.FILENAME_CHARSET_OVERRIDE))
            val restoredFolder = session.listChildren(
                "root",
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty().single()
            assertEquals(stableFolderId, restoredFolder.getString(ExplorerArchiveSessionKeys.ID))
            assertEquals("目录", restoredFolder.getString(ExplorerArchiveSessionKeys.NAME))
            val nested = session.listChildren(
                stableFolderId,
                0,
                ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
            ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty().single()
            val actual = ParcelFileDescriptor.AutoCloseInputStream(
                session.openEntry(requireNotNull(nested.getString(ExplorerArchiveSessionKeys.ID))),
            ).use { input -> input.readBytes() }
            assertArrayEquals(expected, actual)
        } finally {
            session.close()
        }

        assertFalse(archive.exists())
        assertFalse(directory.exists())
    }

    @Test
    fun writableZipSessionRenamesAndDeletesThroughHostReplacementWhilePreservingStableIds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hostDirectory = File(context.cacheDir, "archive-session-mutation-host-${UUID.randomUUID()}")
        val stagedDirectory = File(context.cacheDir, "archive-input-session-mutation-${UUID.randomUUID()}")
        assertTrue(hostDirectory.mkdirs())
        assertTrue(stagedDirectory.mkdirs())
        val hostArchive = File(hostDirectory, "managed.zip")
        ZipOutputStream(FileOutputStream(hostArchive)).use { output ->
            output.putNextEntry(ZipEntry("folder/"))
            output.closeEntry()
            output.putNextEntry(ZipEntry("folder/nested.txt"))
            output.write("nested payload".encodeToByteArray())
            output.closeEntry()
            output.putNextEntry(ZipEntry("keep.txt"))
            output.write("keep payload".encodeToByteArray())
            output.closeEntry()
        }
        val stagedArchiveFile = File(stagedDirectory, "source.archive")
        hostArchive.copyTo(stagedArchiveFile)
        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = hostArchive.name,
            stagedArchive = StagedArchive(
                stagedArchiveFile.asArchiveReadSource(),
                stagedArchiveFile.length(),
            ),
            cacheDirectory = context.cacheDir,
            snapshot = ArchiveScanner().scan(stagedArchiveFile),
            onClosed = {},
        )

        try {
            assertTrue(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_DELETE_ENTRIES))
            assertTrue(session.info.getBoolean(ExplorerArchiveSessionKeys.CAN_RENAME_ENTRIES))
            val initialRoot = session.rootItems()
            val folder = initialRoot.single { item ->
                item.getString(ExplorerArchiveSessionKeys.NAME) == "folder"
            }
            assertTrue(folder.getBoolean(ExplorerArchiveSessionKeys.CAN_DELETE))
            assertTrue(folder.getBoolean(ExplorerArchiveSessionKeys.CAN_RENAME))
            val folderId = requireNotNull(folder.getString(ExplorerArchiveSessionKeys.ID))
            val nestedId = requireNotNull(
                session.children(folderId).single().getString(ExplorerArchiveSessionKeys.ID),
            )

            val renameHost = ReplacementHostSession(hostArchive, hostDirectory)
            val renamed = session.runMutation(
                host = renameHost,
                request = Bundle().apply {
                    putString(ExplorerArchiveMutationKeys.OPERATION_ID, UUID.randomUUID().toString())
                    putInt(
                        ExplorerArchiveMutationKeys.OPERATION,
                        ExplorerArchiveSessionValues.MUTATION_RENAME,
                    )
                    putString(ExplorerArchiveMutationKeys.ENTRY_ID, folderId)
                    putString(ExplorerArchiveMutationKeys.NEW_NAME, "renamed")
                },
            )
            assertNull(renamed.failure)
            assertEquals(1, requireNotNull(renamed.completed).getInt(ExplorerArchiveMutationKeys.MUTATED_ENTRIES))
            assertTrue(
                renamed.progress.any { update ->
                    update.getInt(ExplorerArchiveMutationKeys.PHASE) ==
                        ExplorerArchiveSessionValues.MUTATION_PHASE_REINDEXING
                },
            )
            assertEquals(1, renameHost.commitCalls)
            val renamedFolder = session.rootItems().single { item ->
                item.getString(ExplorerArchiveSessionKeys.NAME) == "renamed"
            }
            assertEquals(folderId, renamedFolder.getString(ExplorerArchiveSessionKeys.ID))
            assertEquals(
                nestedId,
                session.children(folderId).single().getString(ExplorerArchiveSessionKeys.ID),
            )
            assertEquals(
                setOf("keep.txt", "renamed", "renamed/nested.txt"),
                ArchiveScanner().scan(hostArchive).entries.mapTo(linkedSetOf()) { it.path },
            )

            val deleteHost = ReplacementHostSession(hostArchive, hostDirectory)
            val deleted = session.runMutation(
                host = deleteHost,
                request = Bundle().apply {
                    putString(ExplorerArchiveMutationKeys.OPERATION_ID, UUID.randomUUID().toString())
                    putInt(
                        ExplorerArchiveMutationKeys.OPERATION,
                        ExplorerArchiveSessionValues.MUTATION_DELETE,
                    )
                    putStringArrayList(ExplorerArchiveMutationKeys.ENTRY_IDS, arrayListOf(folderId))
                },
            )
            assertNull(deleted.failure)
            assertEquals(1, requireNotNull(deleted.completed).getInt(ExplorerArchiveMutationKeys.MUTATED_ENTRIES))
            assertEquals(1, deleteHost.commitCalls)
            assertEquals(listOf("keep.txt"), session.rootItems().map { item ->
                item.getString(ExplorerArchiveSessionKeys.NAME)
            })
            assertEquals(
                listOf("keep.txt"),
                ArchiveScanner().scan(hostArchive).entries.map { it.path },
            )
        } finally {
            session.close()
            hostDirectory.deleteRecursively()
        }

        assertFalse(stagedDirectory.exists())
        assertFalse(hostDirectory.exists())
    }

    @Test
    fun writableZipSessionDeletesTheLastEntryAndReopensAsAnEmptyArchive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hostDirectory = File(context.cacheDir, "archive-session-empty-host-${UUID.randomUUID()}")
        val stagedDirectory = File(context.cacheDir, "archive-input-session-empty-${UUID.randomUUID()}")
        assertTrue(hostDirectory.mkdirs())
        assertTrue(stagedDirectory.mkdirs())
        val hostArchive = File(hostDirectory, "empty-after-delete.zip")
        ZipOutputStream(FileOutputStream(hostArchive)).use { output ->
            output.putNextEntry(ZipEntry("last.txt"))
            output.write("last payload".encodeToByteArray())
            output.closeEntry()
        }
        val stagedArchiveFile = File(stagedDirectory, "source.archive")
        hostArchive.copyTo(stagedArchiveFile)
        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = hostArchive.name,
            stagedArchive = StagedArchive(
                stagedArchiveFile.asArchiveReadSource(),
                stagedArchiveFile.length(),
            ),
            cacheDirectory = context.cacheDir,
            snapshot = ArchiveScanner().scan(stagedArchiveFile),
            onClosed = {},
        )

        try {
            val lastEntryId = requireNotNull(
                session.rootItems().single().getString(ExplorerArchiveSessionKeys.ID),
            )
            val deleteHost = ReplacementHostSession(hostArchive, hostDirectory)
            val deleted = session.runMutation(
                host = deleteHost,
                request = Bundle().apply {
                    putString(ExplorerArchiveMutationKeys.OPERATION_ID, UUID.randomUUID().toString())
                    putInt(
                        ExplorerArchiveMutationKeys.OPERATION,
                        ExplorerArchiveSessionValues.MUTATION_DELETE,
                    )
                    putStringArrayList(
                        ExplorerArchiveMutationKeys.ENTRY_IDS,
                        arrayListOf(lastEntryId),
                    )
                },
            )

            assertNull(deleted.failure)
            assertEquals(
                1,
                requireNotNull(deleted.completed)
                    .getInt(ExplorerArchiveMutationKeys.MUTATED_ENTRIES),
            )
            assertEquals(1, deleteHost.commitCalls)
            assertTrue(session.rootItems().isEmpty())
            val committedSnapshot = ArchiveScanner().scan(hostArchive)
            assertEquals(ArchiveFormat.ZIP, committedSnapshot.format)
            assertTrue(committedSnapshot.entries.isEmpty())
            assertEquals(0L, committedSnapshot.totalUncompressedBytes)
            if (
                InstrumentationRegistry.getArguments()
                    .getString(EXPORT_EMPTY_ZIP_ARGUMENT)
                    .toBoolean()
            ) {
                val exportDirectory = requireNotNull(context.getExternalFilesDir("validation"))
                val exported = File(exportDirectory, EXPORTED_EMPTY_ZIP_NAME)
                check(!exported.exists() || exported.delete())
                hostArchive.copyTo(exported)
                assertArrayEquals(hostArchive.readBytes(), exported.readBytes())
            }
        } finally {
            session.close()
        }

        assertFalse(stagedDirectory.exists())
        val reopenedStagedDirectory = File(
            context.cacheDir,
            "archive-input-session-empty-reopened-${UUID.randomUUID()}",
        )
        assertTrue(reopenedStagedDirectory.mkdirs())
        val reopenedStagedArchive = File(reopenedStagedDirectory, "source.archive")
        hostArchive.copyTo(reopenedStagedArchive)
        val reopenedSession = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = hostArchive.name,
            stagedArchive = StagedArchive(
                reopenedStagedArchive.asArchiveReadSource(),
                reopenedStagedArchive.length(),
            ),
            cacheDirectory = context.cacheDir,
            snapshot = ArchiveScanner().scan(reopenedStagedArchive),
            onClosed = {},
        )

        try {
            assertEquals("zip", reopenedSession.info.getString(ExplorerArchiveSessionKeys.FORMAT_ID))
            assertTrue(reopenedSession.rootItems().isEmpty())
        } finally {
            reopenedSession.close()
            hostDirectory.deleteRecursively()
        }

        assertFalse(reopenedStagedDirectory.exists())
        assertFalse(hostDirectory.exists())
    }

    private fun ExplorerArchiveSession.rootItems(): List<Bundle> = children("root")

    private fun ExplorerArchiveSession.children(parentId: String): List<Bundle> = listChildren(
        parentId,
        0,
        ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
    ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS).orEmpty()

    private fun ExplorerArchiveSession.runMutation(
        host: ReplacementHostSession,
        request: Bundle,
    ): MutationAttemptResult {
        val terminal = CountDownLatch(1)
        val completed = AtomicReference<Bundle?>()
        val failure = AtomicReference<Bundle?>()
        val progress = mutableListOf<Bundle>()
        mutateEntries(
            request,
            host,
            object : IExplorerArchiveOperationCallback.Stub() {
                override fun onProgress(update: Bundle) {
                    progress += Bundle(update)
                }

                override fun onCompleted(result: Bundle) {
                    completed.set(result)
                    terminal.countDown()
                }

                override fun onFailed(error: Bundle) {
                    failure.set(error)
                    terminal.countDown()
                }
            },
        )
        assertTrue("Timed out waiting for archive mutation", terminal.await(30, TimeUnit.SECONDS))
        return MutationAttemptResult(completed.get(), failure.get(), progress.toList())
    }

    private class ReplacementHostSession(
        private val target: File,
        private val directory: File,
    ) : UnusedTestExplorerActionHostSession() {
        private var transactionId = ""
        private val pendingFile: File
            get() = File(directory, ".archive-session-replacement-${hashCode()}.tmp")
        var commitCalls = 0
            private set

        override fun prepareTargetReplacement(targetId: String): Bundle {
            require(targetId == SOURCE_TARGET_ID)
            check(transactionId.isEmpty())
            transactionId = UUID.randomUUID().toString()
            return outputBundle(includeIdentity = false)
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            requireActive(transactionId)
            return ParcelFileDescriptor.open(
                pendingFile,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_READ_WRITE,
            )
        }

        override fun openPendingOutput(transactionId: String): ParcelFileDescriptor {
            requireActive(transactionId)
            check(pendingFile.isFile)
            return ParcelFileDescriptor.open(pendingFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun commitOutput(transactionId: String): Bundle {
            requireActive(transactionId)
            check(pendingFile.isFile)
            if (!target.delete() || !pendingFile.renameTo(target)) {
                throw IOException("Test host could not replace the session ZIP")
            }
            commitCalls++
            val result = outputBundle(includeIdentity = true)
            this.transactionId = ""
            return result
        }

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            require(targetId == SOURCE_TARGET_ID)
            require(relativePath == ArchivePathPolicy.ROOT_PATH)
            return ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun abortOutput(transactionId: String) {
            requireActive(transactionId)
            pendingFile.delete()
            this.transactionId = ""
        }

        override fun close() {
            pendingFile.delete()
            transactionId = ""
        }

        private fun requireActive(value: String) {
            require(value == transactionId && value.isNotEmpty())
        }

        private fun outputBundle(includeIdentity: Boolean): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, target.name)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, target.absolutePath)
            if (includeIdentity) {
                putLong(ExplorerActionHostSessionKeys.SIZE, target.length())
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, target.lastModified())
            }
        }
    }

    private data class MutationAttemptResult(
        val completed: Bundle?,
        val failure: Bundle?,
        val progress: List<Bundle>,
    )

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

    private companion object {
        const val EXPORTED_EMPTY_ZIP_NAME = "empty-after-last-entry-delete.zip"
        const val EXPORT_EMPTY_ZIP_ARGUMENT = "exportEmptyZipMutationArtifact"
        const val SOURCE_TARGET_ID = "archive-source"
    }
}
