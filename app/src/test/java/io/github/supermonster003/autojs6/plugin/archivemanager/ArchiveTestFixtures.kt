package io.github.supermonster003.autojs6.plugin.archivemanager

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.junit.Assert.fail
import org.tukaani.xz.LZMA2Options
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.Date
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal data class FixtureEntry(
    val name: String,
    val bytes: ByteArray = ByteArray(0),
    val method: Int = ZipEntry.DEFLATED,
)

internal data class SevenZFixtureEntry(
    val name: String,
    val bytes: ByteArray = ByteArray(0),
    val isDirectory: Boolean = false,
)

internal fun writeSevenZ(file: File, vararg entries: SevenZFixtureEntry): File {
    SevenZOutputFile(file).use { output ->
        output.setContentMethods(
            listOf(SevenZMethodConfiguration(SevenZMethod.LZMA2, LZMA2Options(3))),
        )
        entries.forEach { fixture ->
            val entry = SevenZArchiveEntry().apply {
                name = fixture.name.trimEnd('/')
                isDirectory = fixture.isDirectory
                lastModifiedDate = Date(FIXED_ZIP_TIME)
            }
            output.putArchiveEntry(entry)
            if (!fixture.isDirectory) output.write(fixture.bytes)
            output.closeArchiveEntry()
        }
    }
    return file
}

internal enum class TarFixtureEntryType {
    FILE,
    DIRECTORY,
    SYMBOLIC_LINK,
    HARD_LINK,
}

internal data class TarFixtureEntry(
    val name: String,
    val bytes: ByteArray = ByteArray(0),
    val type: TarFixtureEntryType = TarFixtureEntryType.FILE,
    val linkName: String = "",
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

internal fun writeZipWithMalformedExtra(
    file: File,
    name: String,
    bytes: ByteArray,
): File {
    ZipOutputStream(FileOutputStream(file)).use { output ->
        val entry = ZipEntry(name).apply {
            time = FIXED_ZIP_TIME
            extra = byteArrayOf(
                0xEF.toByte(),
                0xBE.toByte(),
                0x01,
                0x00,
                0x00,
            )
        }
        output.putNextEntry(entry)
        output.write(bytes)
        output.closeEntry()
    }

    val archive = Files.readAllBytes(file.toPath())
    val localOffset = findSignature(archive, LOCAL_FILE_HEADER_SIGNATURE)
    check(localOffset >= 0)
    corruptFirstExtraFieldLength(
        archive = archive,
        headerOffset = localOffset,
        fixedHeaderSize = ZIP_LOCAL_HEADER_SIZE,
        nameLengthOffset = ZIP_LOCAL_NAME_LENGTH_OFFSET,
        extraLengthOffset = ZIP_LOCAL_EXTRA_LENGTH_OFFSET,
    )
    val centralOffset = findSignature(archive, CENTRAL_DIRECTORY_HEADER_SIGNATURE)
    check(centralOffset >= 0)
    corruptFirstExtraFieldLength(
        archive = archive,
        headerOffset = centralOffset,
        fixedHeaderSize = ZIP_CENTRAL_HEADER_SIZE,
        nameLengthOffset = ZIP_CENTRAL_NAME_LENGTH_OFFSET,
        extraLengthOffset = ZIP_CENTRAL_EXTRA_LENGTH_OFFSET,
    )
    Files.write(file.toPath(), archive)
    return file
}

internal fun writeTar(file: File, vararg entries: TarFixtureEntry): File {
    return writeTar(file, entries) { output -> output }
}

internal fun writeOldGnuSparseTar(
    file: File,
    name: String,
    storedBytes: ByteArray,
    sparseOffset: Long,
    realSize: Long,
): File {
    require(storedBytes.isNotEmpty())
    require(sparseOffset >= 0L)
    require(Math.addExact(sparseOffset, storedBytes.size.toLong()) <= realSize)

    TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(file))).use { output ->
        val entry = TarArchiveEntry(name, TarConstants.LF_GNUTYPE_SPARSE).apply {
            size = storedBytes.size.toLong()
            setModTime(FIXED_ZIP_TIME)
        }
        output.putArchiveEntry(entry)
        output.write(storedBytes)
        output.closeArchiveEntry()
    }

    val archive = Files.readAllBytes(file.toPath())
    check(archive.size >= TAR_RECORD_SIZE)
    GNU_TAR_MAGIC.encodeToByteArray().copyInto(archive, TAR_MAGIC_OFFSET)
    archive[TAR_VERSION_OFFSET] = ' '.code.toByte()
    archive[TAR_VERSION_OFFSET + 1] = 0
    writeTarOctal(archive, TAR_OLDGNU_SPARSE_OFFSET, TAR_NUMBER_LENGTH, sparseOffset)
    writeTarOctal(
        archive,
        TAR_OLDGNU_SPARSE_OFFSET + TAR_NUMBER_LENGTH,
        TAR_NUMBER_LENGTH,
        storedBytes.size.toLong(),
    )
    archive[TAR_OLDGNU_IS_EXTENDED_OFFSET] = 0
    writeTarOctal(archive, TAR_OLDGNU_REAL_SIZE_OFFSET, TAR_NUMBER_LENGTH, realSize)
    rewriteFirstTarHeaderChecksum(archive)
    Files.write(file.toPath(), archive)
    return file
}

internal fun writeTarGzip(file: File, vararg entries: TarFixtureEntry): File =
    writeTar(file, entries, ::GzipCompressorOutputStream)

internal fun writeTarXz(file: File, vararg entries: TarFixtureEntry): File =
    writeTar(file, entries, ::XZCompressorOutputStream)

internal fun writeTarBzip2(file: File, vararg entries: TarFixtureEntry): File =
    writeTar(file, entries, ::BZip2CompressorOutputStream)

internal fun writeTarZstd(file: File, vararg entries: TarFixtureEntry): File =
    writeTar(file, entries, ::zstdTestCompressor)

internal fun zstdTestCompressor(output: OutputStream): OutputStream =
    ZstdCompressorOutputStream.builder().apply {
        setOutputStream(output)
        setLevel(3)
        setChecksum(true)
    }.get()

private fun writeTar(
    file: File,
    entries: Array<out TarFixtureEntry>,
    compressor: (OutputStream) -> OutputStream,
): File {
    val target = compressor(BufferedOutputStream(FileOutputStream(file)))
    TarArchiveOutputStream(target).use { output ->
        output.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
        output.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
        output.setAddPaxHeadersForNonAsciiNames(true)
        entries.forEach { fixture ->
            val typeFlag = when (fixture.type) {
                TarFixtureEntryType.FILE -> TarConstants.LF_NORMAL
                TarFixtureEntryType.DIRECTORY -> TarConstants.LF_DIR
                TarFixtureEntryType.SYMBOLIC_LINK -> TarConstants.LF_SYMLINK
                TarFixtureEntryType.HARD_LINK -> TarConstants.LF_LINK
            }
            val name = if (fixture.type == TarFixtureEntryType.DIRECTORY) {
                fixture.name.trimEnd('/') + '/'
            } else {
                fixture.name
            }
            val entry = TarArchiveEntry(name, typeFlag).apply {
                size = if (fixture.type == TarFixtureEntryType.FILE) {
                    fixture.bytes.size.toLong()
                } else {
                    0L
                }
                setModTime(FIXED_ZIP_TIME)
                if (fixture.type in setOf(
                        TarFixtureEntryType.SYMBOLIC_LINK,
                        TarFixtureEntryType.HARD_LINK,
                    )
                ) {
                    linkName = fixture.linkName
                }
            }
            output.putArchiveEntry(entry)
            if (fixture.type == TarFixtureEntryType.FILE) output.write(fixture.bytes)
            output.closeArchiveEntry()
        }
    }
    return file
}

internal fun corruptFirstTarHeaderChecksum(file: File) {
    val bytes = Files.readAllBytes(file.toPath())
    check(bytes.size >= TAR_RECORD_SIZE)
    bytes[0] = (bytes[0].toInt() xor 0x01).toByte()
    Files.write(file.toPath(), bytes)
}

internal fun convertFirstTarHeaderToV7(file: File) {
    val bytes = Files.readAllBytes(file.toPath())
    check(bytes.size >= TAR_RECORD_SIZE)
    bytes.fill(0, TAR_MAGIC_OFFSET, TAR_VERSION_END)
    rewriteFirstTarHeaderChecksum(bytes)
    Files.write(file.toPath(), bytes)
}

private fun rewriteFirstTarHeaderChecksum(bytes: ByteArray) {
    bytes.fill(' '.code.toByte(), TAR_CHECKSUM_OFFSET, TAR_CHECKSUM_END)
    val checksum = bytes.take(TAR_RECORD_SIZE).sumOf { it.toInt() and 0xFF }
    val encoded = checksum.toString(8).padStart(6, '0').encodeToByteArray()
    encoded.copyInto(bytes, TAR_CHECKSUM_OFFSET)
    bytes[TAR_CHECKSUM_OFFSET + 6] = 0
    bytes[TAR_CHECKSUM_OFFSET + 7] = ' '.code.toByte()
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

private fun corruptFirstExtraFieldLength(
    archive: ByteArray,
    headerOffset: Int,
    fixedHeaderSize: Int,
    nameLengthOffset: Int,
    extraLengthOffset: Int,
) {
    val nameLength = readLittleEndianShort(archive, headerOffset + nameLengthOffset)
    val extraLength = readLittleEndianShort(archive, headerOffset + extraLengthOffset)
    check(extraLength >= MALFORMED_EXTRA_MINIMUM_SIZE)
    val extraOffset = headerOffset + fixedHeaderSize + nameLength
    check(readLittleEndianShort(archive, extraOffset) == MALFORMED_EXTRA_HEADER_ID)
    writeLittleEndianShort(archive, extraOffset + 2, MALFORMED_EXTRA_CLAIMED_SIZE)
}

private fun writeTarOctal(
    bytes: ByteArray,
    offset: Int,
    length: Int,
    value: Long,
) {
    require(value >= 0L)
    val encoded = value.toString(8)
    require(encoded.length <= length - 1)
    bytes.fill('0'.code.toByte(), offset, offset + length - 1)
    encoded.encodeToByteArray().copyInto(bytes, offset + length - 1 - encoded.length)
    bytes[offset + length - 1] = 0
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
private const val ZIP_LOCAL_HEADER_SIZE = 30
private const val ZIP_LOCAL_NAME_LENGTH_OFFSET = 26
private const val ZIP_LOCAL_EXTRA_LENGTH_OFFSET = 28
private const val ZIP_CENTRAL_HEADER_SIZE = 46
private const val ZIP_CENTRAL_NAME_LENGTH_OFFSET = 28
private const val ZIP_CENTRAL_EXTRA_LENGTH_OFFSET = 30
private const val MALFORMED_EXTRA_HEADER_ID = 0xBEEF
private const val MALFORMED_EXTRA_MINIMUM_SIZE = 5
private const val MALFORMED_EXTRA_CLAIMED_SIZE = 0x7FFF
private const val TAR_RECORD_SIZE = 512
private const val TAR_NUMBER_LENGTH = 12
private const val TAR_CHECKSUM_OFFSET = 148
private const val TAR_CHECKSUM_END = 156
private const val TAR_MAGIC_OFFSET = 257
private const val TAR_VERSION_OFFSET = 263
private const val TAR_VERSION_END = 265
private const val TAR_OLDGNU_SPARSE_OFFSET = 386
private const val TAR_OLDGNU_IS_EXTENDED_OFFSET = 482
private const val TAR_OLDGNU_REAL_SIZE_OFFSET = 483
private const val GNU_TAR_MAGIC = "ustar "
