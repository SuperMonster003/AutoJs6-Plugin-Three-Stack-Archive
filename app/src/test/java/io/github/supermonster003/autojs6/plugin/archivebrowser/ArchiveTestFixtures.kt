package io.github.supermonster003.autojs6.plugin.archivebrowser

import org.junit.Assert.fail
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal data class FixtureEntry(
    val name: String,
    val bytes: ByteArray = ByteArray(0),
    val method: Int = ZipEntry.DEFLATED,
)

internal fun writeZip(file: File, vararg entries: FixtureEntry): File {
    ZipOutputStream(FileOutputStream(file)).use { output ->
        entries.forEach { fixture ->
            val entry = ZipEntry(fixture.name).apply {
                method = fixture.method
                time = FIXED_ZIP_TIME
                if (fixture.method == ZipEntry.STORED) {
                    val crc32 = CRC32().apply { update(fixture.bytes) }
                    size = fixture.bytes.size.toLong()
                    compressedSize = fixture.bytes.size.toLong()
                    crc = crc32.value
                }
            }
            output.putNextEntry(entry)
            if (!fixture.name.endsWith('/')) output.write(fixture.bytes)
            output.closeEntry()
        }
    }
    return file
}

internal fun patchFirstStoredEntryData(file: File) {
    val bytes = Files.readAllBytes(file.toPath())
    check(readLittleEndianInt(bytes, 0) == LOCAL_FILE_HEADER_SIGNATURE)
    val nameLength = readLittleEndianShort(bytes, 26)
    val extraLength = readLittleEndianShort(bytes, 28)
    val dataOffset = 30 + nameLength + extraLength
    check(dataOffset < bytes.size)
    bytes[dataOffset] = (bytes[dataOffset].toInt() xor 0x01).toByte()
    Files.write(file.toPath(), bytes)
}

internal fun patchCompressionMethod(file: File, method: Int) {
    val bytes = Files.readAllBytes(file.toPath())
    writeLittleEndianShort(bytes, 8, method)
    val centralOffset = findSignature(bytes, CENTRAL_DIRECTORY_HEADER_SIGNATURE)
    check(centralOffset >= 0)
    writeLittleEndianShort(bytes, centralOffset + 10, method)
    Files.write(file.toPath(), bytes)
}

internal inline fun <reified T : ArchiveException> expectArchiveFailure(
    code: ArchiveFailureCode,
    block: () -> Unit,
): T {
    try {
        block()
    } catch (error: ArchiveException) {
        if (error !is T) fail("Expected ${T::class.java.simpleName}, got ${error::class.java.simpleName}")
        if (error.code != code) fail("Expected $code, got ${error.code}")
        @Suppress("UNCHECKED_CAST")
        return error as T
    }
    fail("Expected ${T::class.java.simpleName}")
    throw AssertionError("unreachable")
}

private fun readLittleEndianShort(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int =
    readLittleEndianShort(bytes, offset) or (readLittleEndianShort(bytes, offset + 2) shl 16)

private fun writeLittleEndianShort(bytes: ByteArray, offset: Int, value: Int) {
    bytes[offset] = value.toByte()
    bytes[offset + 1] = (value ushr 8).toByte()
}

private fun findSignature(bytes: ByteArray, signature: Int): Int {
    for (offset in 0..bytes.size - 4) {
        if (readLittleEndianInt(bytes, offset) == signature) return offset
    }
    return -1
}

private const val FIXED_ZIP_TIME = 1_700_000_000_000L
private const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50
private const val CENTRAL_DIRECTORY_HEADER_SIGNATURE = 0x02014B50
