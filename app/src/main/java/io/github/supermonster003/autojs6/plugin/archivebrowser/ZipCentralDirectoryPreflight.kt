package io.github.supermonster003.autojs6.plugin.archivebrowser

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.min

internal data class ZipCentralDirectoryInfo(
    val entryCount: Int,
    val centralDirectoryOffset: Long,
    val centralDirectorySize: Long,
    val isZip64: Boolean,
)

/**
 * Performs a bounded structural pass before [java.util.zip.ZipFile] is allowed to index the file.
 * This deliberately reads central-directory headers without materializing names, comments, or
 * extra fields so the configured limits apply before the platform ZIP implementation allocates.
 */
internal object ZipCentralDirectoryPreflight {

    fun validate(
        source: File,
        maxEntries: Int,
        cancellationCheck: () -> Unit = {},
    ): ZipCentralDirectoryInfo {
        cancellationCheck()
        require(maxEntries > 0) { "Maximum ZIP entry count must be positive" }
        if (!source.isFile) {
            throw ArchiveValidationException(
                ArchiveFailureCode.SOURCE_NOT_FILE,
                "Archive source is not a regular file",
            )
        }

        return try {
            RandomAccessFile(source, "r").use { archive ->
                val fileLength = archive.length()
                val eocd = findEndOfCentralDirectory(archive, fileLength, cancellationCheck)
                val directory = parseDirectoryDescriptor(archive, eocd)
                validateLimits(directory, maxEntries)
                validateDirectoryBounds(directory, eocd)
                validateCentralDirectoryRecords(archive, directory, cancellationCheck)
                if (archive.length() != fileLength) {
                    malformed("Archive changed during central-directory validation")
                }
                ZipCentralDirectoryInfo(
                    entryCount = directory.entryCount.toInt(),
                    centralDirectoryOffset = directory.offset,
                    centralDirectorySize = directory.size,
                    isZip64 = directory.zip64RecordOffset != null,
                )
            }
        } catch (error: ArchiveException) {
            throw error
        } catch (error: IOException) {
            throw ArchiveValidationException(
                ArchiveFailureCode.MALFORMED_ARCHIVE,
                "Archive central directory cannot be read",
                error,
            )
        } catch (error: IllegalArgumentException) {
            throw ArchiveValidationException(
                ArchiveFailureCode.MALFORMED_ARCHIVE,
                "Archive central-directory metadata is malformed",
                error,
            )
        }
    }

    private fun findEndOfCentralDirectory(
        archive: RandomAccessFile,
        fileLength: Long,
        cancellationCheck: () -> Unit,
    ): EndOfCentralDirectory {
        if (fileLength < EOCD_MIN_SIZE) malformed("Archive end-of-central-directory record is missing")
        val tailLength = min(fileLength, EOCD_MIN_SIZE + MAX_ZIP_COMMENT_BYTES).toInt()
        val tailOffset = fileLength - tailLength
        val tail = ByteArray(tailLength)
        archive.seek(tailOffset)
        archive.readFully(tail)
        cancellationCheck()

        for (offset in tailLength - EOCD_MIN_SIZE.toInt() downTo 0) {
            if (readUInt32(tail, offset) != EOCD_SIGNATURE) continue
            val commentLength = readUInt16(tail, offset + EOCD_COMMENT_LENGTH_OFFSET)
            if (offset.toLong() + EOCD_MIN_SIZE + commentLength != tailLength.toLong()) continue
            return EndOfCentralDirectory(
                offset = tailOffset + offset,
                diskNumber = readUInt16(tail, offset + EOCD_DISK_NUMBER_OFFSET),
                centralDirectoryDisk = readUInt16(tail, offset + EOCD_CENTRAL_DISK_OFFSET),
                entriesOnDisk = readUInt16(tail, offset + EOCD_ENTRIES_ON_DISK_OFFSET),
                totalEntries = readUInt16(tail, offset + EOCD_TOTAL_ENTRIES_OFFSET),
                centralDirectorySize = readUInt32(tail, offset + EOCD_DIRECTORY_SIZE_OFFSET),
                centralDirectoryOffset = readUInt32(tail, offset + EOCD_DIRECTORY_OFFSET_OFFSET),
            )
        }
        malformed("Archive end-of-central-directory record is missing or truncated")
    }

    private fun parseDirectoryDescriptor(
        archive: RandomAccessFile,
        eocd: EndOfCentralDirectory,
    ): DirectoryDescriptor {
        val usesZip64Sentinel = eocd.diskNumber == UINT16_MAX ||
            eocd.centralDirectoryDisk == UINT16_MAX ||
            eocd.entriesOnDisk == UINT16_MAX ||
            eocd.totalEntries == UINT16_MAX ||
            eocd.centralDirectorySize == UINT32_MAX ||
            eocd.centralDirectoryOffset == UINT32_MAX
        val locatorOffset = eocd.offset - ZIP64_LOCATOR_SIZE
        val hasZip64Locator = locatorOffset >= 0L &&
            readUInt32(readAt(archive, locatorOffset, ZIP_SIGNATURE_SIZE), 0) == ZIP64_LOCATOR_SIGNATURE
        val needsZip64 = usesZip64Sentinel || hasZip64Locator
        if (!needsZip64) {
            if (eocd.diskNumber != 0 || eocd.centralDirectoryDisk != 0) {
                malformed("Multi-disk ZIP archives are not supported")
            }
            if (eocd.entriesOnDisk != eocd.totalEntries) {
                malformed("ZIP entry counts differ across disks")
            }
            return DirectoryDescriptor(
                entryCount = eocd.totalEntries.toLong(),
                size = eocd.centralDirectorySize,
                offset = eocd.centralDirectoryOffset,
                terminalRecordOffset = eocd.offset,
                zip64RecordOffset = null,
            )
        }

        if (locatorOffset < 0L) malformed("ZIP64 end-of-central-directory locator is missing")
        val locator = readAt(archive, locatorOffset, ZIP64_LOCATOR_SIZE.toInt())
        if (readUInt32(locator, 0) != ZIP64_LOCATOR_SIGNATURE) {
            malformed("ZIP64 end-of-central-directory locator is missing")
        }
        if (readUInt32(locator, ZIP64_LOCATOR_DISK_OFFSET) != 0L ||
            readUInt32(locator, ZIP64_LOCATOR_TOTAL_DISKS_OFFSET) != 1L
        ) {
            malformed("Multi-disk ZIP64 archives are not supported")
        }

        val zip64Offset = readUInt64(locator, ZIP64_LOCATOR_RECORD_OFFSET)
        if (zip64Offset > locatorOffset - ZIP64_EOCD_MIN_SIZE) {
            malformed("ZIP64 end-of-central-directory offset is outside the archive")
        }
        val zip64 = readAt(archive, zip64Offset, ZIP64_EOCD_MIN_SIZE.toInt())
        if (readUInt32(zip64, 0) != ZIP64_EOCD_SIGNATURE) {
            malformed("ZIP64 end-of-central-directory record is missing")
        }
        val recordPayloadSize = readUInt64(zip64, ZIP64_EOCD_RECORD_SIZE_OFFSET)
        if (recordPayloadSize < ZIP64_EOCD_MIN_PAYLOAD_SIZE) {
            malformed("ZIP64 end-of-central-directory record is truncated")
        }
        val recordEnd = checkedAdd(zip64Offset, checkedAdd(ZIP64_EOCD_PREFIX_SIZE, recordPayloadSize))
        if (recordEnd != locatorOffset) {
            malformed("ZIP64 terminal records are not contiguous")
        }
        if (readUInt16(zip64, ZIP64_EOCD_VERSION_NEEDED_OFFSET) < ZIP64_MIN_VERSION) {
            malformed("ZIP64 end-of-central-directory version is invalid")
        }

        val diskNumber = readUInt32(zip64, ZIP64_EOCD_DISK_NUMBER_OFFSET)
        val centralDirectoryDisk = readUInt32(zip64, ZIP64_EOCD_CENTRAL_DISK_OFFSET)
        val entriesOnDisk = readUInt64(zip64, ZIP64_EOCD_ENTRIES_ON_DISK_OFFSET)
        val totalEntries = readUInt64(zip64, ZIP64_EOCD_TOTAL_ENTRIES_OFFSET)
        val centralDirectorySize = readUInt64(zip64, ZIP64_EOCD_DIRECTORY_SIZE_OFFSET)
        val centralDirectoryOffset = readUInt64(zip64, ZIP64_EOCD_DIRECTORY_OFFSET_OFFSET)
        if (diskNumber != 0L || centralDirectoryDisk != 0L || entriesOnDisk != totalEntries) {
            malformed("Multi-disk ZIP64 archives are not supported")
        }
        requireLegacyMatch(eocd.diskNumber.toLong(), UINT16_MAX.toLong(), diskNumber, "disk number")
        requireLegacyMatch(
            eocd.centralDirectoryDisk.toLong(),
            UINT16_MAX.toLong(),
            centralDirectoryDisk,
            "central-directory disk number",
        )
        requireLegacyMatch(
            eocd.entriesOnDisk.toLong(),
            UINT16_MAX.toLong(),
            entriesOnDisk,
            "entries-on-disk count",
        )
        requireLegacyMatch(
            eocd.totalEntries.toLong(),
            UINT16_MAX.toLong(),
            totalEntries,
            "total entry count",
        )
        requireLegacyMatch(
            eocd.centralDirectorySize,
            UINT32_MAX,
            centralDirectorySize,
            "central-directory size",
        )
        requireLegacyMatch(
            eocd.centralDirectoryOffset,
            UINT32_MAX,
            centralDirectoryOffset,
            "central-directory offset",
        )

        return DirectoryDescriptor(
            entryCount = totalEntries,
            size = centralDirectorySize,
            offset = centralDirectoryOffset,
            terminalRecordOffset = zip64Offset,
            zip64RecordOffset = zip64Offset,
        )
    }

    private fun validateLimits(
        directory: DirectoryDescriptor,
        maxEntries: Int,
    ) {
        if (directory.entryCount > maxEntries.toLong()) {
            throw ArchiveValidationException(
                ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                "Archive contains more than $maxEntries central-directory entries",
            )
        }
        if (directory.size > MAX_CENTRAL_DIRECTORY_BYTES) {
            throw ArchiveValidationException(
                ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                "Archive central directory exceeds the metadata size limit",
            )
        }
    }

    private fun validateDirectoryBounds(
        directory: DirectoryDescriptor,
        eocd: EndOfCentralDirectory,
    ) {
        if (directory.offset > directory.terminalRecordOffset) {
            malformed("Archive central-directory offset is outside the archive")
        }
        val directoryEnd = checkedAdd(directory.offset, directory.size)
        if (directoryEnd != directory.terminalRecordOffset) {
            malformed("Archive central-directory size or offset is inconsistent")
        }
        if (directory.terminalRecordOffset > eocd.offset) {
            malformed("Archive terminal record ordering is invalid")
        }
        val minimumSize = directory.entryCount * CENTRAL_HEADER_FIXED_SIZE
        if (directory.size < minimumSize) {
            malformed("Archive central directory is too small for its declared entry count")
        }
    }

    private fun validateCentralDirectoryRecords(
        archive: RandomAccessFile,
        directory: DirectoryDescriptor,
        cancellationCheck: () -> Unit,
    ) {
        val directoryEnd = directory.offset + directory.size
        var cursor = directory.offset
        val fixedHeader = ByteArray(CENTRAL_HEADER_FIXED_SIZE.toInt())
        repeat(directory.entryCount.toInt()) { index ->
            if (index % CANCELLATION_CHECK_INTERVAL == 0) cancellationCheck()
            if (cursor > directoryEnd - CENTRAL_HEADER_FIXED_SIZE) {
                malformed("Archive central-directory entry is truncated")
            }
            archive.seek(cursor)
            archive.readFully(fixedHeader)
            if (readUInt32(fixedHeader, 0) != CENTRAL_HEADER_SIGNATURE) {
                malformed("Archive central-directory entry signature is invalid")
            }
            if (readUInt16(fixedHeader, CENTRAL_HEADER_START_DISK_OFFSET) != 0) {
                malformed("Multi-disk ZIP entries are not supported")
            }
            val variableSize = readUInt16(fixedHeader, CENTRAL_HEADER_NAME_LENGTH_OFFSET).toLong() +
                readUInt16(fixedHeader, CENTRAL_HEADER_EXTRA_LENGTH_OFFSET) +
                readUInt16(fixedHeader, CENTRAL_HEADER_COMMENT_LENGTH_OFFSET)
            cursor = checkedAdd(cursor, CENTRAL_HEADER_FIXED_SIZE + variableSize)
            if (cursor > directoryEnd) {
                malformed("Archive central-directory entry exceeds its declared boundary")
            }
        }

        if (cursor == directoryEnd) return
        val remaining = directoryEnd - cursor
        if (remaining < CENTRAL_DIGITAL_SIGNATURE_MIN_SIZE) {
            malformed("Archive central directory contains trailing bytes")
        }
        val signatureHeader = readAt(archive, cursor, CENTRAL_DIGITAL_SIGNATURE_MIN_SIZE.toInt())
        if (readUInt32(signatureHeader, 0) != CENTRAL_DIGITAL_SIGNATURE) {
            malformed("Archive central-directory entry count is inconsistent")
        }
        val signatureLength = readUInt16(signatureHeader, CENTRAL_DIGITAL_SIGNATURE_LENGTH_OFFSET)
        if (remaining != CENTRAL_DIGITAL_SIGNATURE_MIN_SIZE + signatureLength) {
            malformed("Archive central-directory digital signature is truncated")
        }
    }

    private fun requireLegacyMatch(
        legacy: Long,
        sentinel: Long,
        zip64: Long,
        fieldName: String,
    ) {
        if (legacy != sentinel && legacy != zip64) {
            malformed("ZIP64 $fieldName does not match the legacy end record")
        }
    }

    private fun readAt(
        archive: RandomAccessFile,
        offset: Long,
        size: Int,
    ): ByteArray {
        if (offset < 0L || size < 0 || offset > archive.length() - size) {
            malformed("Archive record is outside the file boundary")
        }
        return ByteArray(size).also { bytes ->
            archive.seek(offset)
            archive.readFully(bytes)
        }
    }

    private fun readUInt16(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset > bytes.size - 2) malformed("Archive field is truncated")
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUInt32(bytes: ByteArray, offset: Int): Long {
        if (offset < 0 || offset > bytes.size - 4) malformed("Archive field is truncated")
        return readUInt16(bytes, offset).toLong() or
            (readUInt16(bytes, offset + 2).toLong() shl 16)
    }

    private fun readUInt64(bytes: ByteArray, offset: Int): Long {
        if (offset < 0 || offset > bytes.size - 8) malformed("Archive field is truncated")
        if (bytes[offset + 7].toInt() and 0x80 != 0) {
            malformed("ZIP64 field exceeds the supported signed range")
        }
        var result = 0L
        for (index in 7 downTo 0) {
            result = (result shl 8) or (bytes[offset + index].toLong() and 0xFF)
        }
        return result
    }

    private fun checkedAdd(left: Long, right: Long): Long {
        if (left < 0L || right < 0L || left > Long.MAX_VALUE - right) {
            malformed("Archive offset arithmetic overflowed")
        }
        return left + right
    }

    private fun malformed(message: String): Nothing = throw ArchiveValidationException(
        ArchiveFailureCode.MALFORMED_ARCHIVE,
        message,
    )

    private data class EndOfCentralDirectory(
        val offset: Long,
        val diskNumber: Int,
        val centralDirectoryDisk: Int,
        val entriesOnDisk: Int,
        val totalEntries: Int,
        val centralDirectorySize: Long,
        val centralDirectoryOffset: Long,
    )

    private data class DirectoryDescriptor(
        val entryCount: Long,
        val size: Long,
        val offset: Long,
        val terminalRecordOffset: Long,
        val zip64RecordOffset: Long?,
    )

    const val MAX_CENTRAL_DIRECTORY_BYTES = 64L * 1_024L * 1_024L

    private const val EOCD_SIGNATURE = 0x06054B50L
    private const val EOCD_MIN_SIZE = 22L
    private const val MAX_ZIP_COMMENT_BYTES = 65_535L
    private const val EOCD_DISK_NUMBER_OFFSET = 4
    private const val EOCD_CENTRAL_DISK_OFFSET = 6
    private const val EOCD_ENTRIES_ON_DISK_OFFSET = 8
    private const val EOCD_TOTAL_ENTRIES_OFFSET = 10
    private const val EOCD_DIRECTORY_SIZE_OFFSET = 12
    private const val EOCD_DIRECTORY_OFFSET_OFFSET = 16
    private const val EOCD_COMMENT_LENGTH_OFFSET = 20

    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064B50L
    private const val ZIP_SIGNATURE_SIZE = 4
    private const val ZIP64_LOCATOR_SIZE = 20L
    private const val ZIP64_LOCATOR_DISK_OFFSET = 4
    private const val ZIP64_LOCATOR_RECORD_OFFSET = 8
    private const val ZIP64_LOCATOR_TOTAL_DISKS_OFFSET = 16

    private const val ZIP64_EOCD_SIGNATURE = 0x06064B50L
    private const val ZIP64_EOCD_MIN_SIZE = 56L
    private const val ZIP64_EOCD_PREFIX_SIZE = 12L
    private const val ZIP64_EOCD_MIN_PAYLOAD_SIZE = 44L
    private const val ZIP64_EOCD_RECORD_SIZE_OFFSET = 4
    private const val ZIP64_EOCD_VERSION_NEEDED_OFFSET = 14
    private const val ZIP64_EOCD_DISK_NUMBER_OFFSET = 16
    private const val ZIP64_EOCD_CENTRAL_DISK_OFFSET = 20
    private const val ZIP64_EOCD_ENTRIES_ON_DISK_OFFSET = 24
    private const val ZIP64_EOCD_TOTAL_ENTRIES_OFFSET = 32
    private const val ZIP64_EOCD_DIRECTORY_SIZE_OFFSET = 40
    private const val ZIP64_EOCD_DIRECTORY_OFFSET_OFFSET = 48
    private const val ZIP64_MIN_VERSION = 45

    private const val CENTRAL_HEADER_SIGNATURE = 0x02014B50L
    private const val CENTRAL_HEADER_FIXED_SIZE = 46L
    private const val CENTRAL_HEADER_NAME_LENGTH_OFFSET = 28
    private const val CENTRAL_HEADER_EXTRA_LENGTH_OFFSET = 30
    private const val CENTRAL_HEADER_COMMENT_LENGTH_OFFSET = 32
    private const val CENTRAL_HEADER_START_DISK_OFFSET = 34

    private const val CENTRAL_DIGITAL_SIGNATURE = 0x05054B50L
    private const val CENTRAL_DIGITAL_SIGNATURE_MIN_SIZE = 6L
    private const val CENTRAL_DIGITAL_SIGNATURE_LENGTH_OFFSET = 4

    private const val UINT16_MAX = 0xFFFF
    private const val UINT32_MAX = 0xFFFF_FFFFL
    private const val CANCELLATION_CHECK_INTERVAL = 64
}
