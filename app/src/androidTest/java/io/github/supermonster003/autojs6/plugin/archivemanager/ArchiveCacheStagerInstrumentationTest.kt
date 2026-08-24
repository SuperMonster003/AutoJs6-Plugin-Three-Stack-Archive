package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.io.outputstream.ZipOutputStream as Zip4jOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ArchiveCacheStagerInstrumentationTest {

    @Test
    fun seekableReadOnlyDescriptorSupportsEveryReaderWithoutPrivateCopy() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val expected = "descriptor-backed archive".toByteArray()
            val fixtures = listOf(
                ReaderFixture("sample.zip", ArchiveFormat.ZIP) { createZip(it, expected) },
                ReaderFixture("sample.7z", ArchiveFormat.SEVEN_Z) { createSevenZ(it, expected) },
                ReaderFixture("sample.tar", ArchiveFormat.TAR) {
                    createTar(it, expected, ::identityOutput)
                },
                ReaderFixture("sample.tgz", ArchiveFormat.TAR_GZIP) {
                    createTar(it, expected, ::GzipCompressorOutputStream)
                },
                ReaderFixture("sample.txz", ArchiveFormat.TAR_XZ) {
                    createTar(it, expected, ::XZCompressorOutputStream)
                },
                ReaderFixture("sample.tbz2", ArchiveFormat.TAR_BZIP2) {
                    createTar(it, expected, ::BZip2CompressorOutputStream)
                },
                ReaderFixture("sample.tzst", ArchiveFormat.TAR_ZSTD) {
                    createTar(it, expected, ::zstdOutput)
                },
            )

            fixtures.forEach { fixture ->
                val source = File(root, fixture.name)
                fixture.create(source)
                val descriptor = ParcelFileDescriptor.open(
                    source,
                    ParcelFileDescriptor.MODE_READ_ONLY,
                )
                val originalPosition = minOf(3L, source.length())
                Os.lseek(descriptor.fileDescriptor, originalPosition, OsConstants.SEEK_SET)
                val staged = ArchiveCacheStager.stage(
                    source = descriptor,
                    cacheDirectory = cache,
                    reportedSize = source.length(),
                )
                try {
                    val descriptorBacked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    assertEquals(
                        if (descriptorBacked) {
                            ArchiveInputStorage.SEEKABLE_DESCRIPTOR
                        } else {
                            ArchiveInputStorage.PRIVATE_CACHE
                        },
                        staged.storage,
                    )
                    if (descriptorBacked) {
                        assertEquals(null, staged.source.localFile)
                        assertTrue(cache.listFiles().isNullOrEmpty())
                    } else {
                        assertTrue(requireNotNull(staged.source.localFile).isFile)
                        assertFalse(cache.listFiles().isNullOrEmpty())
                    }
                    assertEquals(source.length(), staged.bytes)

                    val snapshot = ArchiveScanner().scan(staged.source)
                    assertEquals(fixture.format, snapshot.format)
                    val entry = snapshot.entries.single()
                    ArchiveEngine.DEFAULT.openReader(
                        source = staged.source,
                        format = snapshot.format,
                        options = snapshot.readerOptions,
                    ).use { reader ->
                        val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                        assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
                    }
                    if (descriptorBacked) {
                        assertEquals(
                            originalPosition,
                            Os.lseek(descriptor.fileDescriptor, 0L, OsConstants.SEEK_CUR),
                        )
                    }
                } finally {
                    staged.close()
                    staged.close()
                }

                assertTrue(source.exists())
                assertTrue(cache.listFiles().isNullOrEmpty())
            }
        }
    }

    @Test
    fun descriptorLeaseKeepsAnUnlinkedArchiveReadableUntilClose() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val source = File(root, "unlinked.zip")
            val expected = "leased after unlink".toByteArray()
            createZip(source, expected)
            val staged = ArchiveCacheStager.stage(
                source = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY),
                cacheDirectory = cache,
                reportedSize = ArchiveIntentPolicy.SIZE_UNKNOWN,
            )
            assertEquals(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ArchiveInputStorage.SEEKABLE_DESCRIPTOR
                } else {
                    ArchiveInputStorage.PRIVATE_CACHE
                },
                staged.storage,
            )
            assertTrue(source.delete())

            try {
                val snapshot = ArchiveScanner().scan(staged.source)
                val entry = snapshot.entries.single()
                ArchiveEngine.DEFAULT.openReader(
                    source = staged.source,
                    format = snapshot.format,
                    options = snapshot.readerOptions,
                ).use { reader ->
                    val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                    assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
                }
            } finally {
                staged.close()
            }

            assertTrue(cache.listFiles().isNullOrEmpty())
        }
    }

    @Test
    fun encryptedZipMaterializesOnlyAfterItsBackendRequestsALocalFile() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val source = File(root, "encrypted.zip")
            val expected = "encrypted descriptor fallback".toByteArray()
            createEncryptedZip(source, expected)

            val initial = ArchiveCacheStager.stage(
                source = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY),
                cacheDirectory = cache,
                reportedSize = source.length(),
            )
            var readable = initial
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertEquals(ArchiveInputStorage.SEEKABLE_DESCRIPTOR, initial.storage)
                try {
                    ArchiveScanner().scan(initial.source)
                    throw AssertionError("Expected encrypted ZIP to request a local file")
                } catch (_: ArchiveLocalFileRequiredException) {
                    // Expected: Zip4j cannot consume an already-open descriptor directly.
                }
                readable = ArchiveCacheStager.materialize(initial, cache)
                initial.close()
            } else {
                assertEquals(ArchiveInputStorage.PRIVATE_CACHE, initial.storage)
            }

            try {
                assertEquals(ArchiveInputStorage.PRIVATE_CACHE, readable.storage)
                val snapshot = ArchiveScanner().scan(
                    source = readable.source,
                    options = ArchiveReaderOptions(password = TEST_PASSWORD.toCharArray()),
                )
                val entry = snapshot.entries.single()
                ArchiveEngine.DEFAULT.openReader(
                    source = readable.source,
                    format = snapshot.format,
                    options = snapshot.readerOptions,
                ).use { reader ->
                    val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                    assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
                }
            } finally {
                readable.close()
                initial.close()
            }

            assertTrue(source.isFile)
            assertTrue(cache.listFiles().isNullOrEmpty())
        }
    }

    @Test
    fun pipeFallsBackToCacheAndActiveLeaseSurvivesStaleCleanup() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val source = File(root, "pipe-source.zip")
            val expected = "pipe fallback".toByteArray()
            createZip(source, expected)
            val bytes = source.readBytes()
            val pipe = ParcelFileDescriptor.createReliablePipe()
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(bytes) }

            val staged = ArchiveCacheStager.stage(
                source = pipe[0],
                cacheDirectory = cache,
                reportedSize = bytes.size.toLong(),
            )
            assertEquals(ArchiveInputStorage.PRIVATE_CACHE, staged.storage)
            val cachedFile = requireNotNull(staged.source.localFile)
            assertTrue(cachedFile.isFile)
            val activeDirectory = requireNotNull(cachedFile.parentFile)
            val staleTime = System.currentTimeMillis() - STALE_TEST_AGE_MILLIS
            assertTrue(activeDirectory.setLastModified(staleTime))

            val abandonedDirectory = File(cache, "archive-input-abandoned-test").apply {
                assertTrue(mkdirs())
            }
            File(abandonedDirectory, "source.archive").writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(abandonedDirectory.setLastModified(staleTime))

            val trigger = File(root, "cleanup-trigger.zip")
            createZip(trigger, expected)
            ArchiveCacheStager.stage(
                source = ParcelFileDescriptor.open(trigger, ParcelFileDescriptor.MODE_READ_ONLY),
                cacheDirectory = cache,
                reportedSize = trigger.length(),
            ).close()

            assertFalse(abandonedDirectory.exists())
            assertTrue(activeDirectory.isDirectory)
            assertTrue(cachedFile.isFile)
            val snapshot = ArchiveScanner().scan(staged.source)
            assertEquals(ArchiveFormat.ZIP, snapshot.format)

            staged.close()
            assertFalse(cachedFile.exists())
            assertFalse(activeDirectory.exists())
            assertTrue(source.exists())
        }
    }

    @Test
    fun reportedSizeMismatchClosesDescriptorWithoutCreatingCache() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val source = File(root, "mismatch.zip")
            createZip(source, "size mismatch".toByteArray())
            val descriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)

            val error = try {
                ArchiveCacheStager.stage(
                    source = descriptor,
                    cacheDirectory = cache,
                    reportedSize = source.length() + 1L,
                )
                throw AssertionError("Expected the source-size mismatch to fail")
            } catch (expected: IOException) {
                expected
            }

            assertTrue(error.message.orEmpty().contains("changed"))
            assertFalse(descriptor.fileDescriptor.valid())
            assertTrue(source.isFile)
            assertTrue(cache.listFiles().isNullOrEmpty())
        }
    }

    @Test
    fun writableRegularDescriptorUsesAnIsolatedCacheCopy() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val source = File(root, "writable.zip")
            val expected = "read-write descriptor".toByteArray()
            createZip(source, expected)
            val sourceBefore = source.readBytes()

            val staged = ArchiveCacheStager.stage(
                source = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_WRITE),
                cacheDirectory = cache,
                reportedSize = source.length(),
            )
            try {
                assertEquals(ArchiveInputStorage.PRIVATE_CACHE, staged.storage)
                val cachedFile = requireNotNull(staged.source.localFile)
                assertTrue(cachedFile.isFile)
                assertFalse(cachedFile.absolutePath == source.absolutePath)
                val snapshot = ArchiveScanner().scan(staged.source)
                val entry = snapshot.entries.single()
                ArchiveEngine.DEFAULT.openReader(
                    source = staged.source,
                    format = snapshot.format,
                    options = snapshot.readerOptions,
                ).use { reader ->
                    val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                    assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
                }
            } finally {
                staged.close()
            }

            assertArrayEquals(sourceBefore, source.readBytes())
            assertTrue(cache.listFiles().isNullOrEmpty())
        }
    }

    @Test
    fun failedPipeCopyRemovesItsPartialCacheDirectory() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val source = File(root, "failed-pipe.zip")
            createZip(source, "failed pipe copy".toByteArray())
            val bytes = source.readBytes()
            val pipe = ParcelFileDescriptor.createReliablePipe()
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(bytes) }

            try {
                ArchiveCacheStager.stage(
                    source = pipe[0],
                    cacheDirectory = cache,
                    reportedSize = bytes.size.toLong() + 1L,
                )
                throw AssertionError("Expected the copied-size mismatch to fail")
            } catch (expected: IOException) {
                assertTrue(expected.message.orEmpty().contains("changed"))
            }

            assertFalse(pipe[0].fileDescriptor.valid())
            assertTrue(cache.listFiles().isNullOrEmpty())
            assertTrue(source.isFile)
        }
    }

    @Test
    fun pipeCopyExceedingCacheBudgetClosesInputAndRemovesPartialCache() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val pipe = ParcelFileDescriptor.createReliablePipe()
            val writer = thread(name = "archive-stager-space-writer") {
                runCatching {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                        val block = ByteArray(CACHE_PRESSURE_BLOCK_BYTES) { index -> index.toByte() }
                        repeat(CACHE_PRESSURE_BLOCK_COUNT) { output.write(block) }
                    }
                }
            }

            val error = try {
                ArchiveCacheStager.stageWithCopyLimitForTesting(
                    source = pipe[0],
                    cacheDirectory = cache,
                    reportedSize = ArchiveIntentPolicy.SIZE_UNKNOWN,
                    copyLimitBytes = CACHE_PRESSURE_LIMIT_BYTES,
                )
                throw AssertionError("Expected the cache copy budget to be exhausted")
            } catch (expected: ArchiveInputLimitException) {
                expected
            }
            writer.join(5_000L)
            if (writer.isAlive) {
                runCatching { pipe[1].close() }
                writer.join(5_000L)
            }

            assertEquals(ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE, error.code)
            assertFalse(writer.isAlive)
            assertFalse(pipe[0].fileDescriptor.valid())
            assertTrue(cache.listFiles().isNullOrEmpty())
        }
    }

    @Test
    fun cancelledPipeCopyClosesInputAndRemovesPartialCache() {
        withTestDirectory { root ->
            val cache = File(root, "cache").apply { assertTrue(mkdirs()) }
            val pipe = ParcelFileDescriptor.createReliablePipe()
            val writer = thread(name = "archive-stager-cancel-writer") {
                runCatching {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                        val block = ByteArray(64 * 1_024) { index -> index.toByte() }
                        repeat(160) { output.write(block) }
                    }
                }
            }

            lateinit var operation: Job
            runBlocking {
                operation = launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                    ArchiveCacheStager.stage(
                        source = pipe[0],
                        cacheDirectory = cache,
                        reportedSize = ArchiveIntentPolicy.SIZE_UNKNOWN,
                    ) { copied ->
                        if (copied >= CANCEL_AFTER_BYTES) operation.cancel()
                    }
                }
                operation.start()
                operation.join()
            }
            writer.join(5_000L)
            if (writer.isAlive) {
                runCatching { pipe[1].close() }
                writer.join(5_000L)
            }

            assertTrue(operation.isCancelled)
            assertFalse(writer.isAlive)
            assertFalse(pipe[0].fileDescriptor.valid())
            assertTrue(cache.listFiles().isNullOrEmpty())
        }
    }

    private fun withTestDirectory(block: (File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "archive-stager-test-${UUID.randomUUID()}")
        assertTrue(root.mkdirs())
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun createZip(target: File, payload: ByteArray) {
        ZipOutputStream(FileOutputStream(target)).use { output ->
            output.putNextEntry(ZipEntry(ENTRY_NAME))
            output.write(payload)
            output.closeEntry()
        }
    }

    private fun createEncryptedZip(target: File, payload: ByteArray) {
        Zip4jOutputStream(FileOutputStream(target), TEST_PASSWORD.toCharArray()).use { output ->
            output.putNextEntry(
                ZipParameters().apply {
                    fileNameInZip = ENTRY_NAME
                    compressionMethod = CompressionMethod.DEFLATE
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                },
            )
            output.write(payload)
            output.closeEntry()
        }
    }

    private fun createSevenZ(target: File, payload: ByteArray) {
        FileOutputStream(target).use { fileOutput ->
            SevenZOutputFile(fileOutput.channel).use { output ->
                output.setContentCompression(SevenZMethod.LZMA2)
                output.putArchiveEntry(
                    SevenZArchiveEntry().apply {
                        name = ENTRY_NAME
                        lastModifiedDate = Date(1_700_000_000_000L)
                    },
                )
                output.write(payload)
                output.closeArchiveEntry()
            }
        }
    }

    private fun createTar(
        target: File,
        payload: ByteArray,
        compressor: (OutputStream) -> OutputStream,
    ) {
        val compressed = compressor(BufferedOutputStream(FileOutputStream(target)))
        TarArchiveOutputStream(compressed).use { output ->
            val entry = TarArchiveEntry(ENTRY_NAME).apply {
                size = payload.size.toLong()
                setModTime(1_700_000_000_000L)
            }
            output.putArchiveEntry(entry)
            output.write(payload)
            output.closeArchiveEntry()
        }
    }

    private fun identityOutput(output: OutputStream): OutputStream = output

    private fun zstdOutput(output: OutputStream): OutputStream =
        ZstdCompressorOutputStream.builder().apply {
            setOutputStream(output)
            setLevel(3)
            setChecksum(true)
        }.get()

    private data class ReaderFixture(
        val name: String,
        val format: ArchiveFormat,
        val create: (File) -> Unit,
    )

    private companion object {
        const val CACHE_PRESSURE_BLOCK_BYTES = 32 * 1_024
        const val CACHE_PRESSURE_BLOCK_COUNT = 8
        const val CACHE_PRESSURE_LIMIT_BYTES = 64L * 1_024L
        const val CANCEL_AFTER_BYTES = 8L * 1_024L * 1_024L
        const val ENTRY_NAME = "folder/content.txt"
        const val STALE_TEST_AGE_MILLIS = 8L * 24L * 60L * 60L * 1_000L
        const val TEST_PASSWORD = "archive-manager-test"
    }
}
