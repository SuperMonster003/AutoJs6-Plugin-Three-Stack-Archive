@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.OsConstants
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.github.luben.zstd.ZstdOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.tukaani.xz.LZMA2Options
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
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.util.Random
import java.util.UUID
import java.util.concurrent.CancellationException

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
        exerciseSuccessfulMutations(
            directory = directory,
            archive = archive,
            format = ArchiveFormat.TAR,
            exportArgument = EXPORT_VALIDATION_ARGUMENT,
            exportedArchiveName = EXPORTED_ARCHIVE_NAME,
        )
        directory.deleteRecursively()
    }

    @Test
    fun gzipTarMutationStreamsOneSourcePassAndNormalizesContainerMetadata() {
        val directory = newTestDirectory()
        val archive = writeTestTarGzip(
            File(directory, GZIP_ARCHIVE_NAME),
            TarFixture("docs", directory = true),
            TarFixture("docs/readme.txt", "readme".encodeToByteArray()),
            TarFixture("root.bin", byteArrayOf(1, 2, 3)),
        )

        val originalHeader = archive.readBytes().copyOf(GZIP_HEADER_SIZE)
        assertTrue(originalHeader[GZIP_FLAGS_OFFSET].toInt() and GZIP_FLAG_NAME != 0)
        assertTrue(originalHeader[GZIP_FLAGS_OFFSET].toInt() and GZIP_FLAG_COMMENT != 0)
        assertFalse(
            originalHeader.copyOfRange(GZIP_MTIME_OFFSET, GZIP_MTIME_OFFSET + 4)
                .contentEquals(byteArrayOf(0, 0, 0, 0)),
        )

        exerciseSuccessfulMutations(
            directory = directory,
            archive = archive,
            format = ArchiveFormat.TAR_GZIP,
            exportArgument = EXPORT_GZIP_VALIDATION_ARGUMENT,
            exportedArchiveName = EXPORTED_GZIP_ARCHIVE_NAME,
        )

        val rewrittenHeader = archive.readBytes().copyOf(GZIP_HEADER_SIZE)
        assertArrayEquals(byteArrayOf(0x1F, 0x8B.toByte(), 0x08), rewrittenHeader.copyOf(3))
        assertEquals(0, rewrittenHeader[GZIP_FLAGS_OFFSET].toInt() and 0xFF)
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0),
            rewrittenHeader.copyOfRange(GZIP_MTIME_OFFSET, GZIP_MTIME_OFFSET + 4),
        )
        assertEquals(GZIP_OS_UNKNOWN, rewrittenHeader[GZIP_OS_OFFSET].toInt() and 0xFF)
        directory.deleteRecursively()
    }

    @Test
    fun xzTarMutationStreamsOneSourcePassWithABoundedEncoderPreset() {
        val directory = newTestDirectory()
        val archive = writeTestTarXz(
            File(directory, XZ_ARCHIVE_NAME),
            TarFixture("docs", directory = true),
            TarFixture("docs/readme.txt", "readme".encodeToByteArray()),
            TarFixture("root.bin", byteArrayOf(1, 2, 3)),
        )

        exerciseSuccessfulMutations(
            directory = directory,
            archive = archive,
            format = ArchiveFormat.TAR_XZ,
            exportArgument = EXPORT_XZ_VALIDATION_ARGUMENT,
            exportedArchiveName = EXPORTED_XZ_ARCHIVE_NAME,
        )

        val rewrittenHeader = archive.readBytes().copyOf(XZ_STREAM_FLAGS_END)
        assertArrayEquals(XZ_MAGIC, rewrittenHeader.copyOf(XZ_MAGIC.size))
        assertArrayEquals(
            byteArrayOf(0, XZ_CHECK_CRC64),
            rewrittenHeader.copyOfRange(XZ_STREAM_FLAGS_START, XZ_STREAM_FLAGS_END),
        )
        val options = LZMA2Options(tarMutationCompressionLevel(ArchiveFormat.TAR_XZ))
        assertEquals(4 * 1_024 * 1_024, options.dictSize)
        assertEquals(48_058, options.encoderMemoryUsage)
        assertTrue(options.encoderMemoryUsage <= TAR_XZ_MUTATION_MAX_ENCODER_MEMORY_KIB)
        directory.deleteRecursively()
    }

    @Test
    fun bzip2TarMutationStreamsOneSourcePassWithABoundedEncoderPreset() {
        val directory = newTestDirectory()
        val archive = writeTestTarBzip2(
            File(directory, BZIP2_ARCHIVE_NAME),
            TarFixture("docs", directory = true),
            TarFixture("docs/readme.txt", "readme".encodeToByteArray()),
            TarFixture("root.bin", byteArrayOf(1, 2, 3)),
        )

        exerciseSuccessfulMutations(
            directory = directory,
            archive = archive,
            format = ArchiveFormat.TAR_BZIP2,
            exportArgument = EXPORT_BZIP2_VALIDATION_ARGUMENT,
            exportedArchiveName = EXPORTED_BZIP2_ARCHIVE_NAME,
        )

        val rewrittenHeader = archive.readBytes().copyOf(BZIP2_HEADER_SIZE)
        assertArrayEquals(BZIP2_MAGIC, rewrittenHeader.copyOf(BZIP2_MAGIC.size))
        assertEquals(
            '0'.code + TAR_BZIP2_MUTATION_COMPRESSION_LEVEL,
            rewrittenHeader[BZIP2_BLOCK_SIZE_OFFSET].toInt() and 0xFF,
        )
        assertTrue(
            estimateTarBzip2MutationEncoderMemoryBytes(TAR_BZIP2_MUTATION_COMPRESSION_LEVEL) <=
                TAR_BZIP2_MUTATION_MAX_ENCODER_MEMORY_BYTES,
        )
        directory.deleteRecursively()
    }

    @Test
    fun zstandardTarMutationStreamsOneSourcePassWithABoundedEncoderWindow() {
        val directory = newTestDirectory()
        val archive = writeTestTarZstd(
            File(directory, ZSTD_ARCHIVE_NAME),
            TarFixture("docs", directory = true),
            TarFixture("docs/readme.txt", "readme".encodeToByteArray()),
            TarFixture("root.bin", byteArrayOf(1, 2, 3)),
        )

        exerciseSuccessfulMutations(
            directory = directory,
            archive = archive,
            format = ArchiveFormat.TAR_ZSTD,
            exportArgument = EXPORT_ZSTD_VALIDATION_ARGUMENT,
            exportedArchiveName = EXPORTED_ZSTD_ARCHIVE_NAME,
        )

        val rewrittenHeader = archive.readBytes().copyOf(ZSTD_FRAME_HEADER_SIZE)
        assertArrayEquals(ZSTD_MAGIC, rewrittenHeader.copyOf(ZSTD_MAGIC.size))
        assertTrue(
            rewrittenHeader[ZSTD_FRAME_DESCRIPTOR_OFFSET].toInt() and
                ZSTD_CHECKSUM_FLAG != 0,
        )
        assertEquals(
            0,
            rewrittenHeader[ZSTD_FRAME_DESCRIPTOR_OFFSET].toInt() and
                ZSTD_SINGLE_SEGMENT_FLAG,
        )
        assertEquals(
            ZSTD_WINDOW_DESCRIPTOR_1_MIB,
            rewrittenHeader[ZSTD_WINDOW_DESCRIPTOR_OFFSET].toInt() and 0xFF,
        )
        assertTrue(
            estimateTarZstdMutationEncoderMemoryBytes() <=
                TAR_ZSTD_MUTATION_MAX_ENCODER_MEMORY_BYTES,
        )
        directory.deleteRecursively()
    }

    private fun exerciseSuccessfulMutations(
        directory: File,
        archive: File,
        format: ArchiveFormat,
        exportArgument: String,
        exportedArchiveName: String,
    ) {
        val host = ReplacementHostSession(archive, directory)
        val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
            format,
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

        ArchiveEngine.DEFAULT.openReader(archive, format).use { reader ->
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
                .getString(exportArgument)
                .toBoolean()
        ) {
            val exportDirectory = requireNotNull(context.getExternalFilesDir("validation"))
            val exported = File(exportDirectory, exportedArchiveName)
            check(!exported.exists() || exported.delete())
            archive.copyTo(exported)
            assertArrayEquals(archive.readBytes(), exported.readBytes())
        }
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

    @Test
    fun cancellingCompressedTarRewriteDoesNotDrainTheRemainingSource() {
        val directory = newTestDirectory()
        val payload = ByteArray(LARGE_PAYLOAD_SIZE).also { Random(20260829L).nextBytes(it) }
        WRITABLE_COMPRESSED_TAR_FORMATS.forEach { format ->
            val archive = writeTestCompressedTar(
                format,
                File(directory, archiveName(format)),
                TarFixture("large.bin", payload),
            )
            val originalBytes = archive.readBytes()
            val host = ReplacementHostSession(archive, directory)
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                format,
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
                        if (source.bytesRead >= CANCEL_AFTER_COMPRESSED_BYTES) {
                            throw CancellationException("cancel compressed TAR rewrite")
                        }
                    },
                )
            }

            assertEquals(1, source.inputOpenCount)
            assertTrue(source.bytesRead >= CANCEL_AFTER_COMPRESSED_BYTES)
            assertTrue(source.bytesRead < archive.length())
            assertArrayEquals(originalBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        }
        directory.deleteRecursively()
    }

    @Test
    fun compressedTarSourceIdentityChangeAfterPlanningAbortsBeforeReplacement() {
        val directory = newTestDirectory()
        WRITABLE_COMPRESSED_TAR_FORMATS.forEach { format ->
            val archive = writeTestCompressedTar(
                format,
                File(directory, archiveName(format)),
                TarFixture("original.txt", "original".encodeToByteArray()),
            )
            val host = ReplacementHostSession(archive, directory)
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                format,
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
            assertEquals(format, error.format)
            assertArrayEquals(externallyChangedBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        }
        directory.deleteRecursively()
    }

    @Test
    fun damagedCompressedTarTrailerDuringReadbackAbortsWithoutReplacingTheOriginal() {
        val directory = newTestDirectory()
        WRITABLE_COMPRESSED_TAR_FORMATS.forEach { format ->
            val archive = writeTestCompressedTar(
                format,
                File(directory, archiveName(format)),
                TarFixture("original.txt", "original".encodeToByteArray()),
            )
            val originalBytes = archive.readBytes()
            val host = ReplacementHostSession(
                target = archive,
                directory = directory,
                corruptPendingBeforeVerify = true,
                flipPendingTrailerByte = format == ArchiveFormat.TAR_ZSTD,
            )
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                format,
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
            assertEquals(format, error.format)
            assertArrayEquals(originalBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.pendingOpenCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        }
        directory.deleteRecursively()
    }

    @Test
    fun compressedTarOutputWriteFailureAbortsWithoutPublishingAPartialReplacement() {
        val directory = newTestDirectory()
        val payload = ByteArray(LARGE_PAYLOAD_SIZE).also { Random(20260830L).nextBytes(it) }
        WRITABLE_COMPRESSED_TAR_FORMATS.forEach { format ->
            val archive = writeTestCompressedTar(
                format,
                File(directory, archiveName(format)),
                TarFixture("original.txt", "original".encodeToByteArray()),
            )
            val originalBytes = archive.readBytes()
            val host = ReplacementHostSession(
                target = archive,
                directory = directory,
                failOutputWrite = true,
            )
            val mutator = ArchiveEngine.DEFAULT.createMutationProvider(
                format,
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
                    request = ArchiveMutationRequest.AddFiles(
                        parentPath = "",
                        files = listOf(
                            ArchiveMutationAddedFile(
                                displayName = "incompressible.bin",
                                size = payload.size.toLong(),
                            ) {
                                ByteArrayInputStream(payload)
                            },
                        ),
                    ),
                    checkCancelled = {},
                )
            }

            assertEquals(ArchiveCreationOutputOperation.WRITE, error.operation)
            assertEquals(ArchiveFailureCode.OUTPUT_FAILURE, error.code)
            assertEquals(format, error.format)
            if (File(DEVICE_FULL_PATH).canWrite()) {
                assertTrue(error.hasErrno(OsConstants.ENOSPC))
            }
            assertArrayEquals(originalBytes, archive.readBytes())
            assertEquals(0, host.commitCalls)
            assertEquals(0, host.pendingOpenCalls)
            assertEquals(1, host.abortCalls)
            assertFalse(host.pendingFile.exists())
        }
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
            displayName = archive.name,
            request = request,
            checkCancelled = {},
            progress = ArchiveMutationProgressListener { phases += it.phase },
        )
        assertEquals(1, source.inputOpenCount)
        assertEquals(archive.name, committed.displayName)
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

    private fun writeTestTar(file: File, vararg fixtures: TarFixture): File {
        FileOutputStream(file).use { rawOutput -> writeTarPayload(rawOutput, fixtures) }
        return file
    }

    private fun writeTestTarGzip(file: File, vararg fixtures: TarFixture): File {
        val parameters = GzipParameters().apply {
            setCompressionLevel(6)
            setFilename("original-source.tar")
            setComment("Archive Manager mutation metadata fixture")
            setOperatingSystem(3)
        }
        FileOutputStream(file).use { rawOutput ->
            GzipCompressorOutputStream(rawOutput, parameters).use { gzipOutput ->
                writeTarPayload(gzipOutput, fixtures)
            }
        }
        RandomAccessFile(file, "rw").use { archive ->
            val modificationTimeSeconds = FIXED_TIME / 1_000L
            archive.seek(GZIP_MTIME_OFFSET.toLong())
            repeat(4) { byteIndex ->
                archive.write((modificationTimeSeconds ushr (byteIndex * 8)).toInt() and 0xFF)
            }
        }
        return file
    }

    private fun writeTestTarXz(file: File, vararg fixtures: TarFixture): File {
        FileOutputStream(file).use { rawOutput ->
            XZCompressorOutputStream(rawOutput, XZ_FIXTURE_COMPRESSION_LEVEL).use { xzOutput ->
                writeTarPayload(xzOutput, fixtures)
            }
        }
        return file
    }

    private fun writeTestTarBzip2(file: File, vararg fixtures: TarFixture): File {
        FileOutputStream(file).use { rawOutput ->
            BZip2CompressorOutputStream(rawOutput, BZIP2_FIXTURE_COMPRESSION_LEVEL).use { bzip2Output ->
                writeTarPayload(bzip2Output, fixtures)
            }
        }
        return file
    }

    private fun writeTestTarZstd(file: File, vararg fixtures: TarFixture): File {
        FileOutputStream(file).use { rawOutput ->
            ZstdOutputStream(rawOutput, ZSTD_FIXTURE_COMPRESSION_LEVEL).apply {
                setWorkers(0)
                setChecksum(true)
            }.use { zstdOutput ->
                writeTarPayload(zstdOutput, fixtures)
            }
        }
        return file
    }

    private fun writeTestCompressedTar(
        format: ArchiveFormat,
        file: File,
        vararg fixtures: TarFixture,
    ): File = when (format) {
        ArchiveFormat.TAR_GZIP -> writeTestTarGzip(file, *fixtures)
        ArchiveFormat.TAR_XZ -> writeTestTarXz(file, *fixtures)
        ArchiveFormat.TAR_BZIP2 -> writeTestTarBzip2(file, *fixtures)
        ArchiveFormat.TAR_ZSTD -> writeTestTarZstd(file, *fixtures)
        else -> error("$format is not a writable compressed TAR test format")
    }

    private fun archiveName(format: ArchiveFormat): String = when (format) {
        ArchiveFormat.TAR_GZIP -> GZIP_ARCHIVE_NAME
        ArchiveFormat.TAR_XZ -> XZ_ARCHIVE_NAME
        ArchiveFormat.TAR_BZIP2 -> BZIP2_ARCHIVE_NAME
        ArchiveFormat.TAR_ZSTD -> ZSTD_ARCHIVE_NAME
        else -> error("$format is not a writable compressed TAR test format")
    }

    private fun writeTarPayload(outputStream: OutputStream, fixtures: Array<out TarFixture>) {
        TarArchiveOutputStream(outputStream, StandardCharsets.UTF_8.name()).use { output ->
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

        override fun openSeekableChannel(): SeekableByteChannel = RandomAccessFile(file, "r").channel

        override fun openInputStream(): InputStream {
            inputOpenCount++
            return object : FilterInputStream(FileInputStream(file)) {
                override fun read(): Int = super.read().also { value ->
                    if (value >= 0) bytesRead++
                }

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    super.read(buffer, offset, length).also { count ->
                        if (count > 0) bytesRead += count.toLong()
                    }
            }
        }
    }

    private class ReplacementHostSession(
        private val target: File,
        private val directory: File,
        private val corruptPendingBeforeVerify: Boolean = false,
        private val flipPendingTrailerByte: Boolean = false,
        private val failOutputWrite: Boolean = false,
    ) : UnusedTestExplorerActionHostSession() {
        var commitCalls = 0
        var pendingOpenCalls = 0
        var abortCalls = 0
        var transactionId = ""
        private var pendingCorrupted = false
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
            if (failOutputWrite) {
                val fullDevice = File(DEVICE_FULL_PATH)
                if (fullDevice.canWrite()) {
                    return ParcelFileDescriptor.open(
                        fullDevice,
                        ParcelFileDescriptor.MODE_WRITE_ONLY,
                    )
                }
                check(pendingFile.createNewFile())
                return ParcelFileDescriptor.open(
                    pendingFile,
                    ParcelFileDescriptor.MODE_READ_ONLY,
                )
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
                    if (flipPendingTrailerByte) {
                        val lastByteOffset = file.length() - 1L
                        file.seek(lastByteOffset)
                        val lastByte = file.read()
                        check(lastByte >= 0)
                        file.seek(lastByteOffset)
                        file.write(lastByte xor 0x01)
                    } else {
                        file.setLength(file.length() - DAMAGED_TRAILER_BYTES)
                    }
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
            pendingCorrupted = false
        }

        private fun requireActive(value: String) {
            require(value == transactionId && value.isNotEmpty())
        }

        private fun outputBundle(includeIdentity: Boolean): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, target.name)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, "/Archive tests/${target.name}")
            if (includeIdentity) {
                putLong(ExplorerActionHostSessionKeys.SIZE, target.length())
                putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, target.lastModified())
            }
        }
    }

    private companion object {
        const val ARCHIVE_NAME = "managed.tar"
        const val GZIP_ARCHIVE_NAME = "managed.tar.gz"
        const val XZ_ARCHIVE_NAME = "managed.tar.xz"
        const val BZIP2_ARCHIVE_NAME = "managed.tar.bz2"
        const val ZSTD_ARCHIVE_NAME = "managed.tar.zst"
        const val EXPORTED_ARCHIVE_NAME = "ordinary-tar-mutation-e2e.tar"
        const val EXPORTED_GZIP_ARCHIVE_NAME = "gzip-tar-mutation-e2e.tar.gz"
        const val EXPORTED_XZ_ARCHIVE_NAME = "xz-tar-mutation-e2e.tar.xz"
        const val EXPORTED_BZIP2_ARCHIVE_NAME = "bzip2-tar-mutation-e2e.tar.bz2"
        const val EXPORTED_ZSTD_ARCHIVE_NAME = "zstd-tar-mutation-e2e.tar.zst"
        const val EXPORT_VALIDATION_ARGUMENT = "exportTarMutationArtifact"
        const val EXPORT_GZIP_VALIDATION_ARGUMENT = "exportTarGzipMutationArtifact"
        const val EXPORT_XZ_VALIDATION_ARGUMENT = "exportTarXzMutationArtifact"
        const val EXPORT_BZIP2_VALIDATION_ARGUMENT = "exportTarBzip2MutationArtifact"
        const val EXPORT_ZSTD_VALIDATION_ARGUMENT = "exportTarZstdMutationArtifact"
        const val FIXED_TIME = 1_700_000_000_000L
        const val LARGE_PAYLOAD_SIZE = 2 * 1_024 * 1_024
        const val CANCEL_AFTER_COMPRESSED_BYTES = 128L * 1_024L
        const val GZIP_HEADER_SIZE = 10
        const val GZIP_FLAGS_OFFSET = 3
        const val GZIP_MTIME_OFFSET = 4
        const val GZIP_OS_OFFSET = 9
        const val GZIP_FLAG_NAME = 0x08
        const val GZIP_FLAG_COMMENT = 0x10
        const val GZIP_OS_UNKNOWN = 255
        const val XZ_FIXTURE_COMPRESSION_LEVEL = 1
        const val BZIP2_FIXTURE_COMPRESSION_LEVEL = 1
        const val ZSTD_FIXTURE_COMPRESSION_LEVEL = 1
        const val BZIP2_HEADER_SIZE = 4
        const val BZIP2_BLOCK_SIZE_OFFSET = 3
        const val ZSTD_FRAME_HEADER_SIZE = 6
        const val ZSTD_FRAME_DESCRIPTOR_OFFSET = 4
        const val ZSTD_WINDOW_DESCRIPTOR_OFFSET = 5
        const val ZSTD_CHECKSUM_FLAG = 0x04
        const val ZSTD_SINGLE_SEGMENT_FLAG = 0x20
        const val ZSTD_WINDOW_DESCRIPTOR_1_MIB = 0x50
        const val DAMAGED_TRAILER_BYTES = 2L
        const val XZ_STREAM_FLAGS_START = 6
        const val XZ_STREAM_FLAGS_END = 8
        const val XZ_CHECK_CRC64: Byte = 0x04
        const val DEVICE_FULL_PATH = "/dev/full"
        const val TARGET_ID = "archive-target"
        val XZ_MAGIC = byteArrayOf(
            0xFD.toByte(),
            0x37,
            0x7A,
            0x58,
            0x5A,
            0x00,
        )
        val BZIP2_MAGIC = byteArrayOf(0x42, 0x5A, 0x68)
        val ZSTD_MAGIC = byteArrayOf(
            0x28,
            0xB5.toByte(),
            0x2F,
            0xFD.toByte(),
        )
        val WRITABLE_COMPRESSED_TAR_FORMATS = listOf(
            ArchiveFormat.TAR_GZIP,
            ArchiveFormat.TAR_XZ,
            ArchiveFormat.TAR_BZIP2,
            ArchiveFormat.TAR_ZSTD,
        )
    }
}
