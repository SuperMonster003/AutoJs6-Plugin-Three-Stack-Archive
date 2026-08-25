package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ZipArchiveMutatorInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun renameDeleteNewDirectoryAndAddFileCommitVerifiedReplacementArchives() {
        val directory = newTestDirectory()
        val archive = writeTestZip(
            File(directory, ARCHIVE_NAME),
            ZipFixture("docs/readme.txt", "readme".encodeToByteArray()),
            ZipFixture("root.bin", byteArrayOf(1, 2, 3), ZipEntry.STORED),
        )
        val host = ReplacementHostSession(archive, directory)
        val mutator = ZipArchiveMutator(ExplorerActionHostSessionClient(host), context.cacheDir)

        mutate(
            mutator,
            archive,
            ZipArchiveMutationRequest.Rename("docs", "manual"),
        )
        mutate(
            mutator,
            archive,
            ZipArchiveMutationRequest.Delete(setOf("root.bin")),
        )
        mutate(
            mutator,
            archive,
            ZipArchiveMutationRequest.AddDirectory("manual", "empty"),
        )
        mutate(
            mutator,
            archive,
            ZipArchiveMutationRequest.AddFiles(
                parentPath = "manual",
                files = listOf(
                    ZipArchiveAddedFile("notes.txt", size = 5L) {
                        ByteArrayInputStream("notes".encodeToByteArray())
                    },
                ),
            ),
        )

        ArchiveEngine.DEFAULT.openReader(archive, ArchiveFormat.ZIP).use { reader ->
            assertEquals(
                listOf("manual/readme.txt", "manual/empty/", "manual/notes.txt"),
                reader.entries.map(ArchiveReaderEntry::name),
            )
            assertEquals(
                "readme",
                reader.openEntry(requireNotNull(reader.entries.firstOrNull {
                    it.name == "manual/readme.txt"
                })).use { it.readBytes().decodeToString() },
            )
            assertEquals(
                "notes",
                reader.openEntry(requireNotNull(reader.entries.firstOrNull {
                    it.name == "manual/notes.txt"
                })).use { it.readBytes().decodeToString() },
            )
        }
        assertEquals(4, host.commitCalls)
        assertEquals(4, host.pendingOpenCalls)
        assertEquals(0, host.abortCalls)
        assertFalse(host.pendingFile.exists())
        directory.deleteRecursively()
    }

    @Test
    fun changedAddedFileSizeAbortsWithoutReplacingTheOriginalArchive() {
        val directory = newTestDirectory()
        val archive = writeTestZip(
            File(directory, ARCHIVE_NAME),
            ZipFixture("original.txt", "original".encodeToByteArray()),
        )
        val originalBytes = archive.readBytes()
        val host = ReplacementHostSession(archive, directory)
        val mutator = ZipArchiveMutator(ExplorerActionHostSessionClient(host), context.cacheDir)
        val snapshot = ArchiveScanner().scan(archive)

        val error = assertThrows(ArchiveValidationException::class.java) {
            mutator.mutate(
                source = archive.asArchiveReadSource(),
                snapshot = snapshot,
                targetId = TARGET_ID,
                displayName = ARCHIVE_NAME,
                request = ZipArchiveMutationRequest.AddFiles(
                    parentPath = "",
                    files = listOf(
                        ZipArchiveAddedFile("short.txt", size = 10L) {
                            ByteArrayInputStream("short".encodeToByteArray())
                        },
                    ),
                ),
                checkCancelled = {},
            )
        }
        assertEquals(ArchiveFailureCode.SOURCE_CHANGED, error.code)

        assertArrayEquals(originalBytes, archive.readBytes())
        assertEquals(0, host.commitCalls)
        assertEquals(1, host.abortCalls)
        assertFalse(host.pendingFile.exists())
        directory.deleteRecursively()
    }

    @Test
    fun aesEncryptedEntryRemainsEncryptedAndReadableAfterRename() {
        val directory = newTestDirectory()
        val archive = File(directory, ARCHIVE_NAME)
        val password = "correct horse".toCharArray()
        net.lingala.zip4j.io.outputstream.ZipOutputStream(
            FileOutputStream(archive),
            password,
        ).use { output ->
            output.putNextEntry(
                net.lingala.zip4j.model.ZipParameters().apply {
                    fileNameInZip = "secret.txt"
                    compressionMethod = net.lingala.zip4j.model.enums.CompressionMethod.DEFLATE
                    isEncryptFiles = true
                    encryptionMethod = net.lingala.zip4j.model.enums.EncryptionMethod.AES
                    aesKeyStrength = net.lingala.zip4j.model.enums.AesKeyStrength.KEY_STRENGTH_256
                },
            )
            output.write("classified".encodeToByteArray())
            output.closeEntry()
        }
        val options = ArchiveReaderOptions(password = password)
        val snapshot = try {
            ArchiveScanner().scan(archive, options)
        } finally {
            options.clearPassword()
        }
        val host = ReplacementHostSession(archive, directory)
        ZipArchiveMutator(ExplorerActionHostSessionClient(host), context.cacheDir).mutate(
            source = archive.asArchiveReadSource(),
            snapshot = snapshot,
            targetId = TARGET_ID,
            displayName = ARCHIVE_NAME,
            request = ZipArchiveMutationRequest.Rename("secret.txt", "renamed.txt"),
            checkCancelled = {},
        )

        val reopenedOptions = ArchiveReaderOptions(password = password)
        try {
            ArchiveEngine.DEFAULT.openReader(archive, ArchiveFormat.ZIP, reopenedOptions).use { reader ->
                val entry = reader.entries.single()
                assertEquals("renamed.txt", entry.name)
                assertTrue(entry.isEncrypted)
                assertEquals(ArchiveEncryptionMethod.AES, entry.encryptionMethod)
                assertEquals(
                    "classified",
                    reader.openEntry(entry).use { it.readBytes().decodeToString() },
                )
            }
        } finally {
            reopenedOptions.clearPassword()
            snapshot.readerOptions.clearPassword()
            password.fill('\u0000')
            directory.deleteRecursively()
        }
    }

    private fun mutate(
        mutator: ZipArchiveMutator,
        archive: File,
        request: ZipArchiveMutationRequest,
    ) {
        val snapshot = ArchiveScanner().scan(archive)
        val phases = mutableListOf<ZipArchiveMutationPhase>()
        val committed = mutator.mutate(
            source = archive.asArchiveReadSource(),
            snapshot = snapshot,
            targetId = TARGET_ID,
            displayName = ARCHIVE_NAME,
            request = request,
            checkCancelled = {},
            progress = ZipArchiveMutationProgressListener { phases += it.phase },
        )
        assertEquals(ARCHIVE_NAME, committed.displayName)
        assertEquals(archive.length(), committed.size)
        assertTrue(ZipArchiveMutationPhase.PREPARING in phases)
        assertTrue(ZipArchiveMutationPhase.VERIFYING in phases)
        assertTrue(ZipArchiveMutationPhase.COMMITTING in phases)
    }

    private fun newTestDirectory(): File = File(
        context.cacheDir,
        "zip-mutation-${UUID.randomUUID()}",
    ).also { check(it.mkdirs()) }

    private fun writeTestZip(file: File, vararg entries: ZipFixture): File {
        ZipOutputStream(FileOutputStream(file)).use { output ->
            entries.forEach { fixture ->
                val entry = ZipEntry(fixture.name).apply {
                    method = fixture.method
                    time = FIXED_TIME
                    if (fixture.method == ZipEntry.STORED) {
                        val crc = CRC32().apply { update(fixture.bytes) }
                        size = fixture.bytes.size.toLong()
                        compressedSize = fixture.bytes.size.toLong()
                        this.crc = crc.value
                    }
                }
                output.putNextEntry(entry)
                output.write(fixture.bytes)
                output.closeEntry()
            }
        }
        return file
    }

    private data class ZipFixture(
        val name: String,
        val bytes: ByteArray,
        val method: Int = ZipEntry.DEFLATED,
    )

    private class ReplacementHostSession(
        private val target: File,
        private val directory: File,
    ) : UnusedTestExplorerActionHostSession() {
        var commitCalls = 0
        var pendingOpenCalls = 0
        var abortCalls = 0
        var transactionId = ""
        val pendingFile: File
            get() = File(directory, ".archive-manager-replacement.tmp")

        override fun prepareTargetReplacement(targetId: String): Bundle {
            require(targetId == TARGET_ID)
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
            pendingOpenCalls++
            return ParcelFileDescriptor.open(pendingFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun commitOutput(transactionId: String): Bundle {
            requireActive(transactionId)
            check(pendingFile.isFile)
            if (!target.delete() || !pendingFile.renameTo(target)) {
                throw IOException("Test host could not replace the ZIP")
            }
            commitCalls++
            val result = outputBundle(includeIdentity = true)
            this.transactionId = ""
            return result
        }

        override fun abortOutput(transactionId: String) {
            requireActive(transactionId)
            pendingFile.delete()
            abortCalls++
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
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, ARCHIVE_NAME)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, "/Archive tests/$ARCHIVE_NAME")
            if (includeIdentity) {
                putLong(ExplorerActionHostSessionKeys.SIZE, target.length())
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, target.lastModified())
            }
        }
    }

    private companion object {
        const val ARCHIVE_NAME = "managed.zip"
        const val FIXED_TIME = 1_700_000_000_000L
        const val TARGET_ID = "archive-target"
    }
}
