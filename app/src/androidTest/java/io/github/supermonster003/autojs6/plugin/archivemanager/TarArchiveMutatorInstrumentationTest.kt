@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TarArchiveMutatorInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun renameDeleteAddAndTreeImportCommitVerifiedTarReplacementsInOneSourcePass() {
        val directory = newTestDirectory()
        val archive = writeTestTar(
            File(directory, ARCHIVE_NAME),
            TarFixture("docs", directory = true),
            TarFixture("docs/readme.txt", "readme".encodeToByteArray()),
            TarFixture("root.bin", byteArrayOf(1, 2, 3)),
        )
        val host = ReplacementHostSession(archive, directory)
        val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
            ArchiveFormat.TAR,
            ExplorerActionHostSessionClient(host),
            context.cacheDir,
        )
        var unknownInputOpenCount = 0

        mutate(mutator, archive, ArchiveMutationRequest.Rename("docs", "manual"))
        mutate(mutator, archive, ArchiveMutationRequest.Delete(setOf("root.bin")))
        mutate(mutator, archive, ArchiveMutationRequest.AddDirectory("manual", "empty"))
        mutate(
            mutator,
            archive,
            ArchiveMutationRequest.AddFiles(
                parentPath = "manual",
                files = listOf(
                    ArchiveMutationAddedFile("notes.txt") {
                        unknownInputOpenCount++
                        ByteArrayInputStream("notes".encodeToByteArray())
                    },
                ),
            ),
        )
        mutate(
            mutator,
            archive,
            ArchiveMutationRequest.AddTree(
                parentPath = "manual",
                entries = listOf(
                    ArchiveMutationAddedTreeEntry.Directory("bundle"),
                    ArchiveMutationAddedTreeEntry.Directory("bundle/empty"),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        "bundle/nested.txt",
                        ArchiveMutationAddedFile("nested.txt", size = 6L) {
                            ByteArrayInputStream("nested".encodeToByteArray())
                        },
                    ),
                ),
            ),
        )

        ArchiveEngine.DEFAULT.openReader(archive, ArchiveFormat.TAR).use { reader ->
            assertEquals(
                listOf(
                    "manual/",
                    "manual/readme.txt",
                    "manual/empty/",
                    "manual/notes.txt",
                    "manual/bundle/",
                    "manual/bundle/empty/",
                    "manual/bundle/nested.txt",
                ),
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
            assertEquals(
                "nested",
                reader.openEntry(requireNotNull(reader.entries.firstOrNull {
                    it.name == "manual/bundle/nested.txt"
                })).use { it.readBytes().decodeToString() },
            )
        }
        assertEquals(2, unknownInputOpenCount)
        assertEquals(5, host.commitCalls)
        assertEquals(5, host.pendingOpenCalls)
        assertEquals(0, host.abortCalls)
        assertFalse(host.pendingFile.exists())
        if (
            InstrumentationRegistry.getArguments()
                .getString(EXPORT_VALIDATION_ARGUMENT)
                .toBoolean()
        ) {
            val exportDirectory = requireNotNull(context.getExternalFilesDir("validation"))
            val exported = File(exportDirectory, EXPORTED_ARCHIVE_NAME)
            check(!exported.exists() || exported.delete())
            archive.copyTo(exported)
            assertArrayEquals(archive.readBytes(), exported.readBytes())
        }
        directory.deleteRecursively()
    }

    @Test
    fun changedAddedFileSizeAbortsWithoutReplacingTheOriginalTar() {
        val directory = newTestDirectory()
        val archive = writeTestTar(
            File(directory, ARCHIVE_NAME),
            TarFixture("original.txt", "original".encodeToByteArray()),
        )
        val originalBytes = archive.readBytes()
        val host = ReplacementHostSession(archive, directory)
        val mutator = TarArchiveMutationProvider(
            ExplorerActionHostSessionClient(host),
            context.cacheDir,
        )
        val snapshot = ArchiveScanner().scan(archive)

        val error = assertThrows(ArchiveValidationException::class.java) {
            mutator.mutate(
                source = archive.asArchiveReadSource(),
                snapshot = snapshot,
                targetId = TARGET_ID,
                displayName = ARCHIVE_NAME,
                request = ArchiveMutationRequest.AddFiles(
                    parentPath = "",
                    files = listOf(
                        ArchiveMutationAddedFile("short.txt", size = 10L) {
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

    private fun mutate(
        mutator: ArchiveMutationProvider,
        archive: File,
        request: ArchiveMutationRequest,
    ) {
        val snapshot = ArchiveScanner().scan(archive)
        val source = CountingArchiveReadSource(archive)
        val phases = mutableListOf<ArchiveMutationPhase>()
        val committed = mutator.mutate(
            source = source,
            snapshot = snapshot,
            targetId = TARGET_ID,
            displayName = ARCHIVE_NAME,
            request = request,
            checkCancelled = {},
            progress = ArchiveMutationProgressListener { phases += it.phase },
        )
        assertEquals(1, source.inputOpenCount)
        assertEquals(ARCHIVE_NAME, committed.displayName)
        assertEquals(archive.length(), committed.size)
        assertTrue(ArchiveMutationPhase.PREPARING in phases)
        assertTrue(ArchiveMutationPhase.WRITING in phases || snapshot.entries.isEmpty())
        assertTrue(ArchiveMutationPhase.VERIFYING in phases)
        assertTrue(ArchiveMutationPhase.COMMITTING in phases)
    }

    private fun newTestDirectory(): File = File(
        context.cacheDir,
        "tar-mutation-${UUID.randomUUID()}",
    ).also { check(it.mkdirs()) }

    private fun writeTestTar(file: File, vararg fixtures: TarFixture): File {
        TarArchiveOutputStream(FileOutputStream(file), StandardCharsets.UTF_8.name()).use { output ->
            output.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            output.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            output.setAddPaxHeadersForNonAsciiNames(true)
            fixtures.forEach { fixture ->
                val name = if (fixture.directory) fixture.name.trimEnd('/') + '/' else fixture.name
                val entry = TarArchiveEntry(
                    name,
                    if (fixture.directory) TarConstants.LF_DIR else TarConstants.LF_NORMAL,
                ).apply {
                    setSize(if (fixture.directory) 0L else fixture.bytes.size.toLong())
                    setMode(
                        if (fixture.directory) {
                            TarArchiveEntry.DEFAULT_DIR_MODE
                        } else {
                            TarArchiveEntry.DEFAULT_FILE_MODE
                        },
                    )
                    setModTime(FIXED_TIME)
                }
                output.putArchiveEntry(entry)
                if (!fixture.directory) output.write(fixture.bytes)
                output.closeArchiveEntry()
            }
        }
        return file
    }

    private data class TarFixture(
        val name: String,
        val bytes: ByteArray = byteArrayOf(),
        val directory: Boolean = false,
    )

    private class CountingArchiveReadSource(
        private val file: File,
    ) : ArchiveReadSource {
        var inputOpenCount = 0
            private set

        override val isRegularFile: Boolean
            get() = file.isFile
        override val localFile: File
            get() = file
        override val displayName: String
            get() = file.name

        override fun identity(): ArchiveSourceIdentity = ArchiveSourceIdentity(
            length = file.length(),
            lastModifiedMillis = file.lastModified(),
        )

        override fun openSeekableChannel(): SeekableByteChannel = RandomAccessFile(file, "r").channel

        override fun openInputStream(): InputStream {
            inputOpenCount++
            return FileInputStream(file)
        }
    }

    private class ReplacementHostSession(
        private val target: File,
        private val directory: File,
    ) : UnusedTestExplorerActionHostSession() {
        var commitCalls = 0
        var pendingOpenCalls = 0
        var abortCalls = 0
        var transactionId = ""
        val pendingFile: File
            get() = File(directory, ".archive-manager-tar-replacement.tmp")

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
                throw IOException("Test host could not replace the TAR")
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
        const val ARCHIVE_NAME = "managed.tar"
        const val EXPORTED_ARCHIVE_NAME = "ordinary-tar-mutation-e2e.tar"
        const val EXPORT_VALIDATION_ARGUMENT = "exportTarMutationArtifact"
        const val FIXED_TIME = 1_700_000_000_000L
        const val TARGET_ID = "archive-target"
    }
}
