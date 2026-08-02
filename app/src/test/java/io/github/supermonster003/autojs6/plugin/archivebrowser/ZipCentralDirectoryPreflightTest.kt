package io.github.supermonster003.autojs6.plugin.archivebrowser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry

class ZipCentralDirectoryPreflightTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `accepts bounded normal and ZIP64 central directories`() {
        val normal = archive(
            FixtureEntry("a.txt", "alpha".toByteArray(), ZipEntry.STORED),
            FixtureEntry("folder/b.txt", "bravo".toByteArray()),
        )

        val normalInfo = ZipCentralDirectoryPreflight.validate(normal, maxEntries = 2)

        assertEquals(2, normalInfo.entryCount)
        assertTrue(normalInfo.centralDirectorySize > 0L)
        assertFalse(normalInfo.isZip64)

        val zip64 = convertToZip64(normal, temporaryFolder.newFile("valid-zip64.zip"))
        val zip64Info = ZipCentralDirectoryPreflight.validate(zip64, maxEntries = 2)

        assertEquals(2, zip64Info.entryCount)
        assertEquals(normalInfo.centralDirectoryOffset, zip64Info.centralDirectoryOffset)
        assertEquals(normalInfo.centralDirectorySize, zip64Info.centralDirectorySize)
        assertTrue(zip64Info.isZip64)
        assertEquals(2, ArchiveScanner().scan(zip64).entries.size)

        val zip64WithLegacyValues = convertToZip64(
            normal,
            temporaryFolder.newFile("valid-zip64-with-legacy-values.zip"),
            useLegacySentinels = false,
        )
        assertTrue(ZipCentralDirectoryPreflight.validate(zip64WithLegacyValues, maxEntries = 2).isZip64)
        assertEquals(2, ArchiveScanner().scan(zip64WithLegacyValues).entries.size)
    }

    @Test
    fun `rejects malformed EOCD disk and ZIP64 locator fields`() {
        val multiDisk = archive(FixtureEntry("a", byteArrayOf(1), ZipEntry.STORED))
        patchEocdUInt16(multiDisk, EOCD_DISK_NUMBER_OFFSET, 1)

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.MALFORMED_ARCHIVE) {
            ZipCentralDirectoryPreflight.validate(multiDisk, maxEntries = 10)
        }

        val normal = archive(FixtureEntry("b", byteArrayOf(2), ZipEntry.STORED))
        val zip64 = convertToZip64(normal, temporaryFolder.newFile("bad-locator.zip"))
        val bytes = Files.readAllBytes(zip64.toPath())
        val eocdOffset = findEocd(bytes)
        val locatorOffset = eocdOffset - ZIP64_LOCATOR_SIZE
        writeUInt32(bytes, locatorOffset + ZIP64_LOCATOR_TOTAL_DISKS_OFFSET, 2L)
        Files.write(zip64.toPath(), bytes)

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.MALFORMED_ARCHIVE) {
            ZipCentralDirectoryPreflight.validate(zip64, maxEntries = 10)
        }
    }

    @Test
    fun `rejects declared entry count before platform ZIP indexing`() {
        val source = archive(
            FixtureEntry("a", byteArrayOf(1), ZipEntry.STORED),
            FixtureEntry("b", byteArrayOf(2), ZipEntry.STORED),
        )

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ZipCentralDirectoryPreflight.validate(source, maxEntries = 1)
        }
    }

    @Test
    fun `rejects oversized and out of bounds central directories`() {
        val oversized = archive(FixtureEntry("large", byteArrayOf(1), ZipEntry.STORED))
        patchEocdUInt32(
            oversized,
            EOCD_DIRECTORY_SIZE_OFFSET,
            ZipCentralDirectoryPreflight.MAX_CENTRAL_DIRECTORY_BYTES + 1L,
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ZipCentralDirectoryPreflight.validate(oversized, maxEntries = 10)
        }

        val outOfBounds = archive(FixtureEntry("boundary", byteArrayOf(1), ZipEntry.STORED))
        val bytes = Files.readAllBytes(outOfBounds.toPath())
        patchEocdUInt32(outOfBounds, EOCD_DIRECTORY_OFFSET_OFFSET, bytes.size.toLong())
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.MALFORMED_ARCHIVE) {
            ZipCentralDirectoryPreflight.validate(outOfBounds, maxEntries = 10)
        }
    }

    @Test
    fun `rejects EOCD counts that do not match central records`() {
        val source = archive(
            FixtureEntry("a", byteArrayOf(1), ZipEntry.STORED),
            FixtureEntry("b", byteArrayOf(2), ZipEntry.STORED),
        )
        patchEocdUInt16(source, EOCD_ENTRIES_ON_DISK_OFFSET, 1)
        patchEocdUInt16(source, EOCD_TOTAL_ENTRIES_OFFSET, 1)

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.MALFORMED_ARCHIVE) {
            ZipCentralDirectoryPreflight.validate(source, maxEntries = 10)
        }
    }

    @Test
    fun `rejects truncated EOCD`() {
        val source = archive(FixtureEntry("a", byteArrayOf(1), ZipEntry.STORED))
        val bytes = Files.readAllBytes(source.toPath())
        Files.write(source.toPath(), bytes.copyOf(bytes.size - 1))

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.MALFORMED_ARCHIVE) {
            ZipCentralDirectoryPreflight.validate(source, maxEntries = 10)
        }
    }

    @Test
    fun `extractor rejects a changed EOCD before creating output`() {
        val source = archive(FixtureEntry("a", byteArrayOf(1), ZipEntry.STORED))
        val snapshot = ArchiveScanner().scan(source)
        patchEocdUInt16(source, EOCD_DISK_NUMBER_OFFSET, 1)
        assertTrue(source.setLastModified(snapshot.sourceLastModifiedMillis))
        var rootCreationAttempted = false
        val writer = object : ArchiveOutputWriter {
            override fun createRoot(displayName: String): ArchiveOutputWriter.Node {
                rootCreationAttempted = true
                throw AssertionError("Output root must not be created")
            }

            override fun createDirectory(
                parent: ArchiveOutputWriter.Node,
                displayName: String,
            ): ArchiveOutputWriter.Node = throw AssertionError("Output directory must not be created")

            override fun createFile(
                parent: ArchiveOutputWriter.Node,
                displayName: String,
            ): ArchiveOutputWriter.Node = throw AssertionError("Output file must not be created")

            override fun openFile(node: ArchiveOutputWriter.Node): OutputStream =
                throw AssertionError("Output stream must not be opened")

            override fun deleteRoot(root: ArchiveOutputWriter.Node) = Unit
        }

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.SOURCE_CHANGED) {
            runBlocking {
                ArchiveExtractor().extractToWriter(source, snapshot, listOf("a"), "changed", writer)
            }
        }
        assertFalse(rootCreationAttempted)
    }

    @Test
    fun `supports cancellation while walking central directory headers`() {
        val entries = Array(130) { index ->
            FixtureEntry("entry-$index", byteArrayOf(), ZipEntry.STORED)
        }
        val source = archive(*entries)
        var checks = 0

        try {
            ZipCentralDirectoryPreflight.validate(source, maxEntries = entries.size) {
                checks++
                if (checks == 4) throw CancellationException("test cancellation")
            }
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            // Expected after the start, tail-read, entry-zero, and entry-64 checks.
        }

        assertEquals(4, checks)
    }

    private fun archive(vararg entries: FixtureEntry): File = writeZip(
        temporaryFolder.newFile("preflight-${temporaryFolder.root.list().orEmpty().size}.zip"),
        *entries,
    )

    private fun convertToZip64(
        source: File,
        destination: File,
        useLegacySentinels: Boolean = true,
    ): File {
        val bytes = Files.readAllBytes(source.toPath())
        val eocdOffset = findEocd(bytes)
        val entries = readUInt16(bytes, eocdOffset + EOCD_TOTAL_ENTRIES_OFFSET)
        val centralDirectorySize = readUInt32(bytes, eocdOffset + EOCD_DIRECTORY_SIZE_OFFSET)
        val centralDirectoryOffset = readUInt32(bytes, eocdOffset + EOCD_DIRECTORY_OFFSET_OFFSET)
        val output = ByteArrayOutputStream(bytes.size + ZIP64_TERMINAL_ADDITION)
        output.write(bytes, 0, eocdOffset)
        val zip64EocdOffset = eocdOffset.toLong()

        output.writeUInt32(ZIP64_EOCD_SIGNATURE)
        output.writeUInt64(ZIP64_EOCD_PAYLOAD_SIZE)
        output.writeUInt16(ZIP64_MIN_VERSION)
        output.writeUInt16(ZIP64_MIN_VERSION)
        output.writeUInt32(0L)
        output.writeUInt32(0L)
        output.writeUInt64(entries.toLong())
        output.writeUInt64(entries.toLong())
        output.writeUInt64(centralDirectorySize)
        output.writeUInt64(centralDirectoryOffset)

        output.writeUInt32(ZIP64_LOCATOR_SIGNATURE)
        output.writeUInt32(0L)
        output.writeUInt64(zip64EocdOffset)
        output.writeUInt32(1L)

        output.writeUInt32(EOCD_SIGNATURE)
        output.writeUInt16(0)
        output.writeUInt16(0)
        output.writeUInt16(if (useLegacySentinels) UINT16_MAX else entries)
        output.writeUInt16(if (useLegacySentinels) UINT16_MAX else entries)
        output.writeUInt32(if (useLegacySentinels) UINT32_MAX else centralDirectorySize)
        output.writeUInt32(if (useLegacySentinels) UINT32_MAX else centralDirectoryOffset)
        output.writeUInt16(0)

        Files.write(destination.toPath(), output.toByteArray())
        return destination
    }

    private fun patchEocdUInt16(file: File, fieldOffset: Int, value: Int) {
        val bytes = Files.readAllBytes(file.toPath())
        writeUInt16(bytes, findEocd(bytes) + fieldOffset, value)
        Files.write(file.toPath(), bytes)
    }

    private fun patchEocdUInt32(file: File, fieldOffset: Int, value: Long) {
        val bytes = Files.readAllBytes(file.toPath())
        writeUInt32(bytes, findEocd(bytes) + fieldOffset, value)
        Files.write(file.toPath(), bytes)
    }

    private fun findEocd(bytes: ByteArray): Int {
        for (offset in bytes.size - EOCD_MIN_SIZE downTo 0) {
            if (readUInt32(bytes, offset) != EOCD_SIGNATURE) continue
            val commentLength = readUInt16(bytes, offset + EOCD_COMMENT_LENGTH_OFFSET)
            if (offset + EOCD_MIN_SIZE + commentLength == bytes.size) return offset
        }
        throw AssertionError("EOCD not found in test fixture")
    }

    private fun readUInt16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readUInt32(bytes: ByteArray, offset: Int): Long =
        readUInt16(bytes, offset).toLong() or (readUInt16(bytes, offset + 2).toLong() shl 16)

    private fun writeUInt16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeUInt32(bytes: ByteArray, offset: Int, value: Long) {
        repeat(4) { index -> bytes[offset + index] = (value ushr (index * 8)).toByte() }
    }

    private fun ByteArrayOutputStream.writeUInt16(value: Int) {
        repeat(2) { index -> write(value ushr (index * 8)) }
    }

    private fun ByteArrayOutputStream.writeUInt32(value: Long) {
        repeat(4) { index -> write((value ushr (index * 8)).toInt()) }
    }

    private fun ByteArrayOutputStream.writeUInt64(value: Long) {
        repeat(8) { index -> write((value ushr (index * 8)).toInt()) }
    }

    private companion object {
        const val EOCD_SIGNATURE = 0x06054B50L
        const val EOCD_MIN_SIZE = 22
        const val EOCD_DISK_NUMBER_OFFSET = 4
        const val EOCD_ENTRIES_ON_DISK_OFFSET = 8
        const val EOCD_TOTAL_ENTRIES_OFFSET = 10
        const val EOCD_DIRECTORY_SIZE_OFFSET = 12
        const val EOCD_DIRECTORY_OFFSET_OFFSET = 16
        const val EOCD_COMMENT_LENGTH_OFFSET = 20

        const val ZIP64_EOCD_SIGNATURE = 0x06064B50L
        const val ZIP64_EOCD_PAYLOAD_SIZE = 44L
        const val ZIP64_MIN_VERSION = 45
        const val ZIP64_LOCATOR_SIGNATURE = 0x07064B50L
        const val ZIP64_LOCATOR_SIZE = 20
        const val ZIP64_LOCATOR_TOTAL_DISKS_OFFSET = 16
        const val ZIP64_TERMINAL_ADDITION = 76

        const val UINT16_MAX = 0xFFFF
        const val UINT32_MAX = 0xFFFF_FFFFL
    }
}
