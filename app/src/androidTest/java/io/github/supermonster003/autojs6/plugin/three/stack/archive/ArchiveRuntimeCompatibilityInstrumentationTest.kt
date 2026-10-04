@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.three.stack.archive

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import net.lingala.zip4j.io.outputstream.SplitOutputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream as Zip4jOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.AndroidSevenZEncryption
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Date
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ArchiveRuntimeCompatibilityInstrumentationTest {

    @Test
    fun zipMetadataAndEntryDataAreReadableOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "runtime-${UUID.randomUUID()}.zip")
        val expected = "archive-runtime-check".toByteArray()

        try {
            ZipOutputStream(FileOutputStream(source)).use { output ->
                output.putNextEntry(ZipEntry("目录/hello.txt"))
                output.write(expected)
                output.closeEntry()
            }

            val snapshot = ArchiveScanner().scan(source)
            val entry = snapshot.entries.single()

            assertEquals("目录/hello.txt", entry.path)
            assertTrue(entry.canExtract)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    @Test
    fun tarMetadataAndEntryDataAreReadableOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "runtime-${UUID.randomUUID()}.tar")
        val expected = "tar-runtime-check".toByteArray()

        try {
            TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(source))).use { output ->
                output.setAddPaxHeadersForNonAsciiNames(true)
                val entry = TarArchiveEntry("目录/hello.txt").apply {
                    size = expected.size.toLong()
                    setModTime(1_700_000_000_000L)
                }
                output.putArchiveEntry(entry)
                output.write(expected)
                output.closeArchiveEntry()
            }

            val snapshot = ArchiveScanner().scan(source)
            val entry = snapshot.entries.single()

            assertEquals(ArchiveFormat.TAR, snapshot.format)
            assertEquals("目录/hello.txt", entry.path)
            assertTrue(entry.canExtract)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    @Test
    fun compressedTarMetadataAndEntryDataAreReadableOnTheDeviceRuntime() {
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
        ).forEach(::verifyCompressedTarRuntime)
    }

    @Test
    fun sevenZSeekableChannelAndAesAreReadableOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "runtime-${UUID.randomUUID()}.7z")
        val expected = "seven-z-runtime-check".toByteArray()

        try {
            FileOutputStream(source).use { fileOutput ->
                SevenZOutputFile(fileOutput.channel).use { output ->
                    AndroidSevenZEncryption.configure(
                        output,
                        TEST_PASSWORD.toCharArray(),
                        SevenZMethodConfiguration(SevenZMethod.LZMA2),
                    )
                    output.putArchiveEntry(
                        SevenZArchiveEntry().apply {
                            name = "目录/hello.txt"
                            lastModifiedDate = Date(1_700_000_000_000L)
                        },
                    )
                    output.write(expected)
                    output.closeArchiveEntry()
                }
            }

            val locked = ArchiveScanner().scan(source)
            val lockedEntry = locked.entries.single()
            assertEquals(ArchiveFormat.SEVEN_Z, locked.format)
            assertEquals("目录/hello.txt", lockedEntry.path)
            assertTrue(lockedEntry.isEncrypted)
            assertTrue(!lockedEntry.canExtract)

            val unlocked = ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(password = TEST_PASSWORD.toCharArray()),
            )
            val entry = unlocked.entries.single()
            assertTrue(entry.canExtract)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = unlocked.format,
                options = unlocked.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    @Test
    fun manualLegacyFilenameEncodingIsReusedOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "legacy-runtime-${UUID.randomUUID()}.zip")
        val charset = Charset.forName("GB18030")
        val expected = "兼容内容".toByteArray()

        try {
            ZipOutputStream(FileOutputStream(source), charset).use { output ->
                output.putNextEntry(ZipEntry("目录/文件.txt"))
                output.write(expected)
                output.closeEntry()
            }

            val snapshot = ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(filenameCharsetName = charset.name()),
            )
            val entry = snapshot.entries.single()

            assertEquals(charset.name(), snapshot.readerOptions.filenameCharsetName)
            assertEquals("目录/文件.txt", entry.path)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    @Test
    fun aesAndZipCryptoEntriesAreReadableOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = "encrypted-runtime-check".toByteArray()

        listOf(EncryptionMethod.AES, EncryptionMethod.ZIP_STANDARD).forEach { encryption ->
            val source = File(context.cacheDir, "encrypted-${encryption.name}-${UUID.randomUUID()}.zip")
            try {
                writeEncryptedZip(source, "payload.txt", expected, encryption)

                val locked = ArchiveScanner().scan(source).entries.single()
                assertTrue(locked.isEncrypted)
                assertTrue(!locked.canExtract)

                val unlocked = ArchiveScanner().scan(
                    source,
                    ArchiveReaderOptions(password = TEST_PASSWORD.toCharArray()),
                )
                val entry = unlocked.entries.single()
                assertTrue(entry.canExtract)
                ArchiveEngine.DEFAULT.openReader(
                    source = source,
                    format = unlocked.format,
                    options = unlocked.readerOptions,
                ).use { reader ->
                    val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                    assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
                }
            } finally {
                source.delete()
            }
        }
    }

    @Test
    fun standardSplitZipIsReportedAsMissingVolumesOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "split-${UUID.randomUUID()}.zip")
        var splitFiles = emptyList<File>()

        try {
            val payload = ByteArray(70_000) { index -> (index * 31 + 17).toByte() }
            Zip4jOutputStream(SplitOutputStream(source, SPLIT_VOLUME_BYTES)).use { output ->
                output.putNextEntry(
                    ZipParameters().apply {
                        fileNameInZip = "payload.bin"
                        compressionMethod = CompressionMethod.STORE
                        entrySize = payload.size.toLong()
                        isWriteExtendedLocalFileHeader = false
                    },
                )
                output.write(payload)
                output.closeEntry()
            }
            splitFiles = listOf(
                File(source.parentFile, "${source.nameWithoutExtension}.z01"),
                source,
            )

            assertTrue(splitFiles.all(File::isFile))
            assertEquals(2, splitFiles.size)
            val info = requireNotNull(ZipSplitArchiveDetector.inspect(source))
            assertEquals(ZipSplitSegmentKind.FINAL_VOLUME, info.segmentKind)
            assertEquals(2, info.totalVolumeCount)
            try {
                ArchiveScanner().scan(source)
                throw AssertionError("Expected a missing-volume diagnostic")
            } catch (error: ArchiveValidationException) {
                assertEquals(ArchiveFailureCode.MISSING_VOLUME, error.code)
                assertEquals(ArchiveFailureStage.INDEX, error.stage)
            }
        } finally {
            splitFiles.forEach(File::delete)
            source.delete()
        }
    }

    private fun writeEncryptedZip(
        target: File,
        entryName: String,
        payload: ByteArray,
        encryption: EncryptionMethod,
    ) {
        Zip4jOutputStream(FileOutputStream(target), TEST_PASSWORD.toCharArray()).use { output ->
            output.putNextEntry(
                ZipParameters().apply {
                    fileNameInZip = entryName
                    compressionMethod = CompressionMethod.DEFLATE
                    isEncryptFiles = true
                    encryptionMethod = encryption
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                },
            )
            output.write(payload)
            output.closeEntry()
        }
    }

    private fun verifyCompressedTarRuntime(case: CompressedTarCase) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "runtime-${UUID.randomUUID()}.${case.extension}")
        val expected = "compressed-tar-${case.extension}-runtime-check".toByteArray()

        try {
            val compressed = case.compressor(BufferedOutputStream(FileOutputStream(source)))
            TarArchiveOutputStream(compressed).use { output ->
                output.setAddPaxHeadersForNonAsciiNames(true)
                val entry = TarArchiveEntry("目录/hello.txt").apply {
                    size = expected.size.toLong()
                    setModTime(1_700_000_000_000L)
                }
                output.putArchiveEntry(entry)
                output.write(expected)
                output.closeArchiveEntry()
            }

            val snapshot = ArchiveScanner().scan(source)
            val entry = snapshot.entries.single()

            assertEquals(case.format, snapshot.format)
            assertEquals("目录/hello.txt", entry.path)
            assertTrue(entry.canExtract)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    private companion object {
        const val SPLIT_VOLUME_BYTES = 65_536L
        const val TEST_PASSWORD = "ArchiveManager-Test-2026"
    }

    private data class CompressedTarCase(
        val extension: String,
        val format: ArchiveFormat,
        val compressor: (OutputStream) -> OutputStream,
    )
}
