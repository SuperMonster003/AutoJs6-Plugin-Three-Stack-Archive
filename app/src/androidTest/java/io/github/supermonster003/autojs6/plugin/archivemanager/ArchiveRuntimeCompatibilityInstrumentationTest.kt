@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
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
            val payload = File(context.cacheDir, "payload-${UUID.randomUUID()}.txt")
            try {
                payload.writeBytes(expected)
                val parameters = ZipParameters().apply {
                    fileNameInZip = "payload.txt"
                    isEncryptFiles = true
                    encryptionMethod = encryption
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
                ZipFile(source, TEST_PASSWORD.toCharArray()).use { archive ->
                    archive.addFile(payload, parameters)
                }

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
                payload.delete()
                source.delete()
            }
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
        const val TEST_PASSWORD = "ArchiveManager-Test-2026"
    }

    private data class CompressedTarCase(
        val extension: String,
        val format: ArchiveFormat,
        val compressor: (OutputStream) -> OutputStream,
    )
}
