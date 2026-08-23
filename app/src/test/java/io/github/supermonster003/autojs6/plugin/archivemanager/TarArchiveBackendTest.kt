package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

class TarArchiveBackendTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `detects and reads a tar without relying on its extension`() {
        val expected = "TAR 内容".toByteArray()
        val source = writeTar(
            temporaryFolder.newFile("renamed.bin"),
            TarFixtureEntry("目录/", type = TarFixtureEntryType.DIRECTORY),
            TarFixtureEntry("目录/文件.txt", expected),
            TarFixtureEntry("empty.txt"),
        )

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.TAR, snapshot.format)
        assertEquals(3, snapshot.entries.size)
        assertEquals(expected.size.toLong(), snapshot.totalUncompressedBytes)
        assertFalse(snapshot.readerOptions.hasPassword)
        assertNull(snapshot.readerOptions.filenameCharsetName)
        val file = snapshot.entries[1]
        assertEquals("目录/文件.txt", file.path)
        assertEquals(ArchiveCompressionMethod.STORED, file.compressionMethod)
        assertEquals("TAR", file.compressionMethodId)
        assertEquals(expected.size.toLong(), file.compressedSize)
        assertEquals(expected.size.toLong(), file.uncompressedSize)
        assertNull(file.crc32)
        assertTrue(file.canOpen)
        assertTrue(file.canExtract)

        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(file, output)
        assertArrayEquals(expected, output.toByteArray())
    }

    @Test
    fun `accepts an empty tar`() {
        val source = writeTar(temporaryFolder.newFile("empty.tar"))

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.TAR, snapshot.format)
        assertTrue(snapshot.entries.isEmpty())
        assertEquals(0L, snapshot.totalUncompressedBytes)
    }

    @Test
    fun `accepts a checksum valid v7 tar header`() {
        val source = writeTar(
            temporaryFolder.newFile("legacy-v7.tar"),
            TarFixtureEntry("legacy.txt", "legacy".toByteArray()),
        )
        convertFirstTarHeaderToV7(source)

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.TAR, snapshot.format)
        assertEquals("legacy.txt", snapshot.entries.single().path)
    }

    @Test
    fun `lists links but never exposes them as ordinary files`() {
        val source = writeTar(
            temporaryFolder.newFile("links.tar"),
            TarFixtureEntry("target.txt", "target".toByteArray()),
            TarFixtureEntry(
                name = "symbolic-link",
                type = TarFixtureEntryType.SYMBOLIC_LINK,
                linkName = "target.txt",
            ),
            TarFixtureEntry(
                name = "hard-link",
                type = TarFixtureEntryType.HARD_LINK,
                linkName = "target.txt",
            ),
        )

        val snapshot = ArchiveScanner().scan(source)
        val symbolicLink = snapshot.entries.single { it.path == "symbolic-link" }
        val hardLink = snapshot.entries.single { it.path == "hard-link" }

        listOf(symbolicLink, hardLink).forEach { entry ->
            assertFalse(entry.canOpen)
            assertFalse(entry.canExtract)
            assertTrue(
                ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE in entry.capabilities.limitations,
            )
        }
        assertEquals("SYMBOLIC_LINK", symbolicLink.compressionMethodId)
        assertEquals("HARD_LINK", hardLink.compressionMethodId)
    }

    @Test
    fun `malicious link targets remain inert metadata`() {
        val source = writeTar(
            temporaryFolder.newFile("malicious-links.tar"),
            TarFixtureEntry(
                name = "links/symbolic-link",
                type = TarFixtureEntryType.SYMBOLIC_LINK,
                linkName = "../../outside.txt",
            ),
            TarFixtureEntry(
                name = "links/hard-link",
                type = TarFixtureEntryType.HARD_LINK,
                linkName = "/absolute/outside.txt",
            ),
        )

        val snapshot = ArchiveScanner().scan(source)
        val output = ByteArrayOutputStream()

        assertEquals(2, snapshot.entries.size)
        snapshot.entries.forEach { entry ->
            assertEquals(ArchiveEntryPathStatus.SAFE, entry.pathStatus)
            assertFalse(entry.canOpen)
            assertFalse(entry.canExtract)
            expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.UNSUPPORTED_METHOD) {
                ArchiveEntryStreamer(source, snapshot).stream(entry, output)
            }
        }
        assertEquals(0, output.size())
    }

    @Test
    fun `large old GNU sparse entry is listed without exposing virtual data`() {
        val storedBytes = byteArrayOf(0x11, 0x22, 0x33, 0x44)
        val source = writeOldGnuSparseTar(
            file = temporaryFolder.newFile("sparse.tar"),
            name = "sparse.bin",
            storedBytes = storedBytes,
            sparseOffset = SPARSE_REAL_SIZE - storedBytes.size,
            realSize = SPARSE_REAL_SIZE,
        )

        val snapshot = ArchiveScanner().scan(source)
        val entry = snapshot.entries.single()
        val assessment = ArchiveResourceBudgetEvaluator.assess(
            ArchiveSelection.resolve(snapshot, listOf(ArchivePathPolicy.ROOT_PATH)),
            ArchiveResourceBudget.STRICT,
        )

        assertEquals(ArchiveFormat.TAR, snapshot.format)
        assertEquals("SPARSE_FILE", entry.compressionMethodId)
        assertEquals(storedBytes.size.toLong(), entry.compressedSize)
        assertEquals(SPARSE_REAL_SIZE, entry.uncompressedSize)
        assertEquals(SPARSE_REAL_SIZE, snapshot.totalUncompressedBytes)
        assertFalse(entry.canOpen)
        assertFalse(entry.canExtract)
        assertTrue(
            ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE in entry.capabilities.limitations,
        )
        assertTrue(
            assessment.violations.any {
                it.kind == ArchiveResourceBudgetViolationKind.SINGLE_UNCOMPRESSED_SIZE
            },
        )
        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.UNSUPPORTED_METHOD) {
            ArchiveEntryStreamer(source, snapshot).stream(entry, ByteArrayOutputStream())
        }
    }

    @Test
    fun `rejects an invalid tar header checksum as malformed metadata`() {
        val source = writeTar(
            temporaryFolder.newFile("bad-checksum.tar"),
            TarFixtureEntry("payload.txt", "payload".toByteArray()),
        )
        corruptFirstTarHeaderChecksum(source)

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MALFORMED_ARCHIVE,
        ) {
            ArchiveScanner().scan(source)
        }

        assertEquals(ArchiveFormat.TAR, error.format)
        assertEquals(ArchiveFailureStage.INDEX, error.stage)
    }

    @Test
    fun `rejects truncated tar entry data`() {
        val source = writeTar(
            temporaryFolder.newFile("truncated.tar"),
            TarFixtureEntry("payload.bin", ByteArray(600) { 7 }),
        )
        RandomAccessFile(source, "rw").use { file -> file.setLength(1_024L) }

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MALFORMED_ARCHIVE,
        ) {
            ArchiveScanner().scan(source)
        }

        assertEquals(ArchiveFormat.TAR, error.format)
        assertEquals(ArchiveFailureStage.INDEX, error.stage)
    }

    @Test
    fun `all unmatched backends preserve an invalid signature diagnostic`() {
        val source = temporaryFolder.newFile("not-an-archive.tar").apply {
            writeText("plain text")
        }

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.INVALID_SIGNATURE,
        ) {
            ArchiveScanner().scan(source)
        }

        assertNull(error.format)
        assertEquals(ArchiveFailureStage.FORMAT_DETECTION, error.stage)
    }

    private companion object {
        const val SPARSE_REAL_SIZE = 4L * 1_024L * 1_024L * 1_024L
    }
}
