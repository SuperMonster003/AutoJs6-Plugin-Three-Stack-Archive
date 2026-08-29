package io.github.supermonster003.autojs6.plugin.archivemanager

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class CompressedTarArchiveBackendTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `detects compressed tar containers without relying on their extensions`() {
        val expected = "压缩 TAR 内容".toByteArray()
        compressedFixtures(expected).forEach { fixture ->
            val snapshot = ArchiveScanner().scan(fixture.source)

            assertEquals(fixture.format, snapshot.format)
            assertEquals(listOf("目录", "目录/文件.txt"), snapshot.entries.map(ArchiveEntry::path))
            assertFalse(snapshot.readerOptions.hasPassword)
            val entry = snapshot.entries.last()
            assertEquals(ArchiveCompressionMethod.STORED, entry.compressionMethod)
            assertEquals("TAR", entry.compressionMethodId)
            assertEquals(-1L, entry.compressedSize)
            assertEquals(expected.size.toLong(), entry.uncompressedSize)
            assertTrue(entry.canOpen)
            assertTrue(entry.canExtract)
            val canMutate = fixture.format == ArchiveFormat.TAR_GZIP ||
                fixture.format == ArchiveFormat.TAR_XZ ||
                fixture.format == ArchiveFormat.TAR_BZIP2
            assertEquals(canMutate, entry.capabilities.canDelete)
            assertEquals(canMutate, entry.capabilities.canRename)
            assertEquals(
                when (fixture.format) {
                    ArchiveFormat.TAR_GZIP -> TAR_GZIP_MUTATION_CAPABILITIES
                    ArchiveFormat.TAR_XZ -> TAR_XZ_MUTATION_CAPABILITIES
                    ArchiveFormat.TAR_BZIP2 -> TAR_BZIP2_MUTATION_CAPABILITIES
                    else -> null
                },
                ArchiveEngine.DEFAULT.mutationCapabilities(fixture.format),
            )

            val output = ByteArrayOutputStream()
            ArchiveEntryStreamer(fixture.source, snapshot).stream(entry, output)
            assertArrayEquals(expected, output.toByteArray())
        }
    }

    @Test
    fun `accepts empty compressed tar archives`() {
        listOf(
            ArchiveFormat.TAR_GZIP to writeTarGzip(temporaryFolder.newFile("empty.tgz")),
            ArchiveFormat.TAR_XZ to writeTarXz(temporaryFolder.newFile("empty.txz")),
            ArchiveFormat.TAR_BZIP2 to writeTarBzip2(temporaryFolder.newFile("empty.tbz2")),
            ArchiveFormat.TAR_ZSTD to writeTarZstd(temporaryFolder.newFile("empty.tzst")),
        ).forEach { (format, source) ->
            val snapshot = ArchiveScanner().scan(source)

            assertEquals(format, snapshot.format)
            assertTrue(snapshot.entries.isEmpty())
            assertEquals(0L, snapshot.totalUncompressedBytes)
        }
    }

    @Test
    fun `rejects standalone compressor streams whose payload is not tar`() {
        listOf(
            compressedPayload("standalone.gz", ::GzipCompressorOutputStream),
            compressedPayload("standalone.xz", ::XZCompressorOutputStream),
            compressedPayload("standalone.bz2", ::BZip2CompressorOutputStream),
            compressedPayload("standalone.zst", ::zstdTestCompressor),
        ).forEach { source ->
            val error = expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.INVALID_SIGNATURE,
            ) {
                ArchiveScanner().scan(source)
            }

            assertNull(error.format)
            assertEquals(ArchiveFailureStage.FORMAT_DETECTION, error.stage)
        }
    }

    @Test
    fun `reports truncated compressed containers with their detected formats`() {
        listOf(
            ArchiveFormat.TAR_GZIP to byteArrayOf(0x1F, 0x8B.toByte(), 0x08, 0x00),
            ArchiveFormat.TAR_XZ to byteArrayOf(
                0xFD.toByte(),
                0x37,
                0x7A,
                0x58,
                0x5A,
                0x00,
            ),
            ArchiveFormat.TAR_BZIP2 to byteArrayOf(0x42, 0x5A, 0x68, 0x39),
            ArchiveFormat.TAR_ZSTD to byteArrayOf(
                0x28,
                0xB5.toByte(),
                0x2F,
                0xFD.toByte(),
            ),
            ArchiveFormat.TAR_ZSTD to byteArrayOf(0x50, 0x2A, 0x4D, 0x18),
        ).forEachIndexed { index, (format, bytes) ->
            val source = temporaryFolder.newFile("truncated-$index.bin").apply {
                writeBytes(bytes)
            }

            val error = expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.MALFORMED_ARCHIVE,
            ) {
                ArchiveScanner().scan(source)
            }

            assertEquals(format, error.format)
            assertEquals(ArchiveFailureStage.INDEX, error.stage)
        }
    }

    @Test
    fun `rejects damaged compressed stream trailers after reading tar metadata`() {
        listOf(
            ArchiveFormat.TAR_GZIP to writeTarGzip(
                temporaryFolder.newFile("damaged-trailer.tgz"),
                TarFixtureEntry("payload.txt", "payload".toByteArray()),
            ),
            ArchiveFormat.TAR_XZ to writeTarXz(
                temporaryFolder.newFile("damaged-trailer.txz"),
                TarFixtureEntry("payload.txt", "payload".toByteArray()),
            ),
            ArchiveFormat.TAR_BZIP2 to writeTarBzip2(
                temporaryFolder.newFile("damaged-trailer.tbz2"),
                TarFixtureEntry("payload.txt", "payload".toByteArray()),
            ),
            ArchiveFormat.TAR_ZSTD to writeTarZstd(
                temporaryFolder.newFile("damaged-trailer.tzst"),
                TarFixtureEntry("payload.txt", "payload".toByteArray()),
            ),
        ).forEach { (format, source) ->
            val bytes = source.readBytes()
            if (format == ArchiveFormat.TAR_BZIP2) {
                source.writeBytes(bytes.copyOf(bytes.size - 2))
            } else {
                bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
                source.writeBytes(bytes)
            }

            val error = expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.MALFORMED_ARCHIVE,
            ) {
                ArchiveScanner().scan(source)
            }

            assertEquals(format, error.format)
            assertEquals(ArchiveFailureStage.INDEX, error.stage)
        }
    }

    @Test
    fun `reads tar payloads split across concatenated compressor streams`() {
        val expected = "concatenated stream payload".toByteArray()
        val rawTar = writeTar(
            temporaryFolder.newFile("concatenated-source.tar"),
            TarFixtureEntry("payload.txt", expected),
        ).readBytes()
        listOf(
            ConcatenatedFixture(
                ArchiveFormat.TAR_GZIP,
                concatenatedArchive("concatenated.tgz", rawTar, ::GzipCompressorOutputStream),
            ),
            ConcatenatedFixture(
                ArchiveFormat.TAR_XZ,
                concatenatedArchive("concatenated.txz", rawTar, ::XZCompressorOutputStream),
            ),
            ConcatenatedFixture(
                ArchiveFormat.TAR_BZIP2,
                concatenatedArchive("concatenated.tbz2", rawTar, ::BZip2CompressorOutputStream),
            ),
            ConcatenatedFixture(
                ArchiveFormat.TAR_ZSTD,
                concatenatedArchive("concatenated.tzst", rawTar, ::zstdTestCompressor),
            ),
            ConcatenatedFixture(
                ArchiveFormat.TAR_ZSTD,
                zstdArchiveWithLeadingSkippableFrame("skippable-frame.tzst", rawTar),
            ),
        ).forEach { fixture ->
            val snapshot = ArchiveScanner().scan(fixture.source)
            val entry = snapshot.entries.single()
            val output = ByteArrayOutputStream()

            ArchiveEntryStreamer(fixture.source, snapshot).stream(entry, output)

            assertEquals(fixture.format, snapshot.format)
            assertArrayEquals(expected, output.toByteArray())
        }
    }

    private fun compressedFixtures(expected: ByteArray): List<CompressedFixture> = listOf(
        CompressedFixture(
            ArchiveFormat.TAR_GZIP,
            writeTarGzip(
                temporaryFolder.newFile("gzip-renamed.bin"),
                TarFixtureEntry("目录/", type = TarFixtureEntryType.DIRECTORY),
                TarFixtureEntry("目录/文件.txt", expected),
            ),
        ),
        CompressedFixture(
            ArchiveFormat.TAR_XZ,
            writeTarXz(
                temporaryFolder.newFile("xz-renamed.bin"),
                TarFixtureEntry("目录/", type = TarFixtureEntryType.DIRECTORY),
                TarFixtureEntry("目录/文件.txt", expected),
            ),
        ),
        CompressedFixture(
            ArchiveFormat.TAR_BZIP2,
            writeTarBzip2(
                temporaryFolder.newFile("bzip2-renamed.bin"),
                TarFixtureEntry("目录/", type = TarFixtureEntryType.DIRECTORY),
                TarFixtureEntry("目录/文件.txt", expected),
            ),
        ),
        CompressedFixture(
            ArchiveFormat.TAR_ZSTD,
            writeTarZstd(
                temporaryFolder.newFile("zstd-renamed.bin"),
                TarFixtureEntry("目录/", type = TarFixtureEntryType.DIRECTORY),
                TarFixtureEntry("目录/文件.txt", expected),
            ),
        ),
    )

    private fun compressedPayload(
        name: String,
        compressor: (OutputStream) -> OutputStream,
    ): File = temporaryFolder.newFile(name).also { source ->
        compressor(FileOutputStream(source)).use { output ->
            output.write("This is a compressor stream, not a TAR archive.".toByteArray())
        }
    }

    private fun concatenatedArchive(
        name: String,
        payload: ByteArray,
        compressor: (OutputStream) -> OutputStream,
    ): File = temporaryFolder.newFile(name).also { source ->
        val splitAt = payload.size / 2
        listOf(payload.copyOfRange(0, splitAt), payload.copyOfRange(splitAt, payload.size))
            .forEach { member ->
                compressor(FileOutputStream(source, true)).use { output -> output.write(member) }
            }
    }

    private fun zstdArchiveWithLeadingSkippableFrame(
        name: String,
        payload: ByteArray,
    ): File = temporaryFolder.newFile(name).also { source ->
        FileOutputStream(source).use { output ->
            output.write(
                byteArrayOf(
                    0x50,
                    0x2A,
                    0x4D,
                    0x18,
                    0x04,
                    0x00,
                    0x00,
                    0x00,
                    0x01,
                    0x02,
                    0x03,
                    0x04,
                ),
            )
            zstdTestCompressor(output).use { compressed -> compressed.write(payload) }
        }
    }

    private data class CompressedFixture(
        val format: ArchiveFormat,
        val source: File,
    )

    private data class ConcatenatedFixture(
        val format: ArchiveFormat,
        val source: File,
    )
}
