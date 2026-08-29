package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.OsConstants
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tukaani.xz.LZMA2Options
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.util.Date
import java.util.Random
import java.util.UUID
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class SevenZArchiveMutatorInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun renameDeleteAddAndTreeImportCommitVerifiedNonSolidReplacements() {
        val directory = newTestDirectory()
        try {
            val archive = writeTestSevenZ(
                File(directory, ARCHIVE_NAME),
                SevenZFixture("docs", directory = true),
                SevenZFixture("docs/readme.txt", "readme".encodeToByteArray()),
                SevenZFixture("root.bin", byteArrayOf(1, 2, 3)),
            )
            val host = ReplacementHostSession(archive, directory)
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                ArchiveFormat.SEVEN_Z,
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

            ArchiveEngine.DEFAULT.openReader(archive, ArchiveFormat.SEVEN_Z).use { reader ->
                assertEquals(
                    listOf(
                        "manual",
                        "manual/readme.txt",
                        "manual/empty",
                        "manual/notes.txt",
                        "manual/bundle",
                        "manual/bundle/empty",
                        "manual/bundle/nested.txt",
                    ),
                    reader.entries.map(ArchiveReaderEntry::name),
                )
                assertEquals(
                    "readme",
                    readText(reader, "manual/readme.txt"),
                )
                assertEquals(
                    "notes",
                    readText(reader, "manual/notes.txt"),
                )
                assertEquals(
                    "nested",
                    readText(reader, "manual/bundle/nested.txt"),
                )
                assertTrue(
                    reader.entries.none {
                        ArchiveEntryLimitation.SOLID_COMPRESSION in it.capabilities.limitations
                    },
                )
            }
            val snapshot = ArchiveScanner().scan(archive)
            assertTrue(ArchiveEngine.DEFAULT.mutationAvailability(snapshot).isAvailable)
            assertEquals(1, unknownInputOpenCount)
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
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancellationStopsCompressedInputAndAbortsWithoutReplacingTheOriginal() {
        val directory = newTestDirectory()
        try {
            val payload = ByteArray(LARGE_PAYLOAD_SIZE).also {
                Random(20260829L).nextBytes(it)
            }
            val archive = writeTestSevenZ(
                File(directory, ARCHIVE_NAME),
                SevenZFixture("large.bin", payload),
            )
            val originalBytes = archive.readBytes()
            val host = ReplacementHostSession(archive, directory)
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                ArchiveFormat.SEVEN_Z,
                ExplorerActionHostSessionClient(host),
                context.cacheDir,
            )
            val snapshot = ArchiveScanner().scan(archive)
            val source = CountingArchiveReadSource(archive)

            assertThrows(CancellationException::class.java) {
                mutator.mutate(
                    source = source,
                    snapshot = snapshot,
                    targetId = TARGET_ID,
                    displayName = archive.name,
                    request = ArchiveMutationRequest.Rename("large.bin", "renamed.bin"),
                    checkCancelled = {
                        if (source.bytesRead >= CANCEL_AFTER_ARCHIVE_BYTES) {
                            throw CancellationException("cancel 7Z rewrite")
                        }
                    },
                )
            }

            assertTrue(source.channelOpenCount >= 1)
            assertTrue(source.bytesRead >= CANCEL_AFTER_ARCHIVE_BYTES)
            assertTrue(source.bytesRead < archive.length())
            assertArrayEquals(originalBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun sourceIdentityChangeAfterPlanningAbortsBeforePublishing() {
        val directory = newTestDirectory()
        try {
            val archive = writeTestSevenZ(
                File(directory, ARCHIVE_NAME),
                SevenZFixture("original.txt", "original".encodeToByteArray()),
            )
            val host = ReplacementHostSession(archive, directory)
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                ArchiveFormat.SEVEN_Z,
                ExplorerActionHostSessionClient(host),
                context.cacheDir,
            )
            val snapshot = ArchiveScanner().scan(archive)
            val prepared = mutator.prepare(
                snapshot,
                ArchiveMutationRequest.Rename("original.txt", "renamed.txt"),
            )
            FileOutputStream(archive, true).use { it.write(0x5A) }
            val externallyChangedBytes = archive.readBytes()

            val error = assertThrows(ArchiveValidationException::class.java) {
                mutator.execute(
                    source = archive.asArchiveReadSource(),
                    snapshot = snapshot,
                    targetId = TARGET_ID,
                    displayName = archive.name,
                    prepared = prepared,
                    checkCancelled = {},
                )
            }

            assertEquals(ArchiveFailureCode.SOURCE_CHANGED, error.code)
            assertEquals(ArchiveFormat.SEVEN_Z, error.format)
            assertArrayEquals(externallyChangedBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun damagedPendingHeaderDuringReadbackAbortsWithoutReplacingTheOriginal() {
        val directory = newTestDirectory()
        try {
            val archive = writeTestSevenZ(
                File(directory, ARCHIVE_NAME),
                SevenZFixture("original.txt", "original".encodeToByteArray()),
            )
            val originalBytes = archive.readBytes()
            val host = ReplacementHostSession(
                target = archive,
                directory = directory,
                corruptPendingBeforeVerify = true,
            )
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                ArchiveFormat.SEVEN_Z,
                ExplorerActionHostSessionClient(host),
                context.cacheDir,
            )
            val snapshot = ArchiveScanner().scan(archive)

            val error = assertThrows(ArchiveCreationOutputException::class.java) {
                mutator.mutate(
                    source = archive.asArchiveReadSource(),
                    snapshot = snapshot,
                    targetId = TARGET_ID,
                    displayName = archive.name,
                    request = ArchiveMutationRequest.Rename("original.txt", "renamed.txt"),
                    checkCancelled = {},
                )
            }

            assertEquals(ArchiveCreationOutputOperation.VERIFY, error.operation)
            assertEquals(ArchiveFailureCode.OUTPUT_FAILURE, error.code)
            assertArrayEquals(originalBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.pendingOpenCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun outputWriteFailureAbortsWithoutPublishingAPartialReplacement() {
        val directory = newTestDirectory()
        try {
            val archive = writeTestSevenZ(
                File(directory, ARCHIVE_NAME),
                SevenZFixture("original.txt", "original".encodeToByteArray()),
            )
            val originalBytes = archive.readBytes()
            val host = ReplacementHostSession(
                target = archive,
                directory = directory,
                failOutputWrite = true,
            )
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                ArchiveFormat.SEVEN_Z,
                ExplorerActionHostSessionClient(host),
                context.cacheDir,
            )
            val snapshot = ArchiveScanner().scan(archive)

            val error = assertThrows(ArchiveCreationOutputException::class.java) {
                mutator.mutate(
                    source = archive.asArchiveReadSource(),
                    snapshot = snapshot,
                    targetId = TARGET_ID,
                    displayName = archive.name,
                    request = ArchiveMutationRequest.AddDirectory("", "new-folder"),
                    checkCancelled = {},
                )
            }

            assertEquals(ArchiveCreationOutputOperation.WRITE, error.operation)
            assertEquals(ArchiveFailureCode.OUTPUT_FAILURE, error.code)
            if (File(DEVICE_FULL_PATH).canWrite()) {
                assertTrue(error.hasErrno(OsConstants.ENOSPC))
            }
            assertArrayEquals(originalBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(0, host.pendingOpenCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        } finally {
            directory.deleteRecursively()
        }
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
            displayName = archive.name,
            request = request,
            checkCancelled = {},
            progress = ArchiveMutationProgressListener { phases += it.phase },
        )
        assertTrue(source.channelOpenCount >= 2)
        assertEquals(archive.name, committed.displayName)
        assertEquals(archive.length(), committed.size)
        assertTrue(ArchiveMutationPhase.PREPARING in phases)
        assertTrue(ArchiveMutationPhase.WRITING in phases || snapshot.entries.isEmpty())
        assertTrue(ArchiveMutationPhase.VERIFYING in phases)
        assertTrue(ArchiveMutationPhase.COMMITTING in phases)
    }

    private fun readText(reader: ArchiveReader, name: String): String =
        reader.openEntry(requireNotNull(reader.entries.firstOrNull { it.name == name }))
            .use { it.readBytes().decodeToString() }

    private fun newTestDirectory(): File = File(
        context.cacheDir,
        "7z-mutation-${UUID.randomUUID()}",
    ).also { check(it.mkdirs()) }

    private fun writeTestSevenZ(file: File, vararg fixtures: SevenZFixture): File {
        SevenZOutputFile(file).use { output ->
            output.setContentMethods(
                listOf(
                    SevenZMethodConfiguration(
                        SevenZMethod.LZMA2,
                        LZMA2Options(FIXTURE_COMPRESSION_LEVEL),
                    ),
                ),
            )
            fixtures.forEach { fixture ->
                val entry = SevenZArchiveEntry().apply {
                    name = fixture.name.trimEnd('/')
                    isDirectory = fixture.directory
                    lastModifiedDate = Date(FIXED_TIME)
                }
                output.putArchiveEntry(entry)
                if (!fixture.directory) output.write(fixture.bytes)
                output.closeArchiveEntry()
            }
        }
        return file
    }

    private fun Throwable.hasErrno(expected: Int): Boolean {
        val visited = hashSetOf<Throwable>()
        val pending = ArrayDeque<Throwable>()
        pending.add(this)
        while (pending.isNotEmpty()) {
            val failure = pending.removeFirst()
            if (!visited.add(failure)) continue
            if (failure is ErrnoException && failure.errno == expected) return true
            failure.cause?.let(pending::addLast)
            failure.suppressed.forEach(pending::addLast)
        }
        return false
    }

    private data class SevenZFixture(
        val name: String,
        val bytes: ByteArray = byteArrayOf(),
        val directory: Boolean = false,
    )

    private class CountingArchiveReadSource(
        private val file: File,
    ) : ArchiveReadSource {
        var channelOpenCount = 0
            private set
        var bytesRead = 0L
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

        override fun openSeekableChannel(): SeekableByteChannel {
            channelOpenCount++
            return CountingSeekableByteChannel(RandomAccessFile(file, "r").channel) { count ->
                bytesRead += count.toLong()
            }
        }
    }

    private class CountingSeekableByteChannel(
        private val delegate: SeekableByteChannel,
        private val onRead: (Int) -> Unit,
    ) : SeekableByteChannel {
        override fun read(destination: ByteBuffer): Int = delegate.read(destination).also { count ->
            if (count > 0) onRead(count)
        }

        override fun write(source: ByteBuffer): Int = delegate.write(source)

        override fun position(): Long = delegate.position()

        override fun position(newPosition: Long): SeekableByteChannel {
            delegate.position(newPosition)
            return this
        }

        override fun size(): Long = delegate.size()

        override fun truncate(size: Long): SeekableByteChannel = delegate.truncate(size).let { this }

        override fun isOpen(): Boolean = delegate.isOpen

        override fun close() = delegate.close()
    }

    private class ReplacementHostSession(
        private val target: File,
        private val directory: File,
        private val corruptPendingBeforeVerify: Boolean = false,
        private val failOutputWrite: Boolean = false,
    ) : UnusedTestExplorerActionHostSession() {
        var commitCalls = 0
        var pendingOpenCalls = 0
        var abortCalls = 0
        private var transactionId = ""
        private var pendingCorrupted = false
        val pendingFile: File
            get() = File(directory, ".archive-manager-7z-replacement.tmp")

        override fun prepareTargetReplacement(targetId: String): Bundle {
            require(targetId == TARGET_ID)
            check(transactionId.isEmpty())
            transactionId = UUID.randomUUID().toString()
            return outputBundle(includeIdentity = false)
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            requireActive(transactionId)
            if (failOutputWrite) {
                val fullDevice = File(DEVICE_FULL_PATH)
                if (fullDevice.canWrite()) {
                    return ParcelFileDescriptor.open(
                        fullDevice,
                        ParcelFileDescriptor.MODE_WRITE_ONLY,
                    )
                }
                val pipe = ParcelFileDescriptor.createPipe()
                pipe[0].close()
                return pipe[1]
            }
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
            if (corruptPendingBeforeVerify && !pendingCorrupted) {
                RandomAccessFile(pendingFile, "rw").use { file ->
                    check(file.length() > DAMAGED_TRAILER_BYTES)
                    file.setLength(file.length() - DAMAGED_TRAILER_BYTES)
                }
                pendingCorrupted = true
            }
            pendingOpenCalls++
            return ParcelFileDescriptor.open(pendingFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun commitOutput(transactionId: String): Bundle {
            requireActive(transactionId)
            check(pendingFile.isFile)
            if (!target.delete() || !pendingFile.renameTo(target)) {
                throw IOException("Test host could not replace the 7Z")
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
            pendingCorrupted = false
        }

        private fun requireActive(value: String) {
            require(value == transactionId && value.isNotEmpty())
        }

        private fun outputBundle(includeIdentity: Boolean): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, target.name)
            putString(
                ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH,
                "/Archive tests/${target.name}",
            )
            if (includeIdentity) {
                putLong(ExplorerActionHostSessionKeys.SIZE, target.length())
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, target.lastModified())
            }
        }
    }

    private companion object {
        const val ARCHIVE_NAME = "managed.7z"
        const val EXPORTED_ARCHIVE_NAME = "7z-mutation-e2e.7z"
        const val EXPORT_VALIDATION_ARGUMENT = "exportSevenZMutationArtifact"
        const val FIXED_TIME = 1_700_000_000_000L
        const val FIXTURE_COMPRESSION_LEVEL = 1
        const val LARGE_PAYLOAD_SIZE = 2 * 1_024 * 1_024
        const val CANCEL_AFTER_ARCHIVE_BYTES = 128L * 1_024L
        const val DAMAGED_TRAILER_BYTES = 2L
        const val DEVICE_FULL_PATH = "/dev/full"
        const val TARGET_ID = "archive-target"
    }
}
