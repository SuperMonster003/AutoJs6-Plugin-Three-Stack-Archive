package io.github.supermonster003.autojs6.plugin.three.stack.archive

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Reads filename bytes directly from a ZIP central directory, including a directory stored in the
 * selected final volume of a split archive. Android 7's Zip4j backend can decode a candidate name
 * before honoring a newly selected charset, so those decoded strings are not reliable input for
 * automatic charset selection.
 */
internal object ZipCentralDirectoryNameReader {

    /** Returns non-ASCII names without the ZIP UTF-8 flag, or null when the directory is unreadable. */
    fun readNonUtf8Names(source: File): List<ByteArray>? {
        if (!source.isFile) return null
        return try {
            RandomAccessFile(source, READ_MODE).use(::readNonUtf8Names)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun readNonUtf8Names(archive: RandomAccessFile): List<ByteArray>? {
        val archiveLength = archive.length()
        if (archiveLength < END_OF_CENTRAL_DIRECTORY_SIZE) return null
        val searchLength = minOf(archiveLength, MAX_END_RECORD_SEARCH_BYTES).toInt()
        val searchStart = archiveLength - searchLength
        val tail = ByteArray(searchLength)
        archive.seek(searchStart)
        archive.readFully(tail)

        for (relativeOffset in tail.size - END_OF_CENTRAL_DIRECTORY_SIZE downTo 0) {
            if (tail.readUnsignedInt(relativeOffset) != END_OF_CENTRAL_DIRECTORY_SIGNATURE) continue
            val recordOffset = searchStart + relativeOffset
            val commentLength = tail.readUnsignedShort(relativeOffset + 20)
            val recordEnd = recordOffset + END_OF_CENTRAL_DIRECTORY_SIZE + commentLength
            if (recordEnd > archiveLength) continue
            val descriptor = centralDirectoryDescriptor(
                archive,
                tail,
                relativeOffset,
                recordOffset,
            ) ?: continue
            descriptor.candidateOffsets.forEach { centralDirectoryOffset ->
                    readCentralDirectory(
                        archive = archive,
                        offset = centralDirectoryOffset,
                        entryCount = descriptor.entryCount,
                    )?.let { return it }
            }
        }
        return null
    }

    private fun centralDirectoryDescriptor(
        archive: RandomAccessFile,
        tail: ByteArray,
        relativeOffset: Int,
        recordOffset: Long,
    ): CentralDirectoryDescriptor? {
        val diskNumber = tail.readUnsignedShort(relativeOffset + 4)
        val centralDirectoryDisk = tail.readUnsignedShort(relativeOffset + 6)
        if (centralDirectoryDisk != diskNumber) return null

        val entryCount = tail.readUnsignedShort(relativeOffset + 10).toLong()
        val directorySize = tail.readUnsignedInt(relativeOffset + 12)
        val directoryOffset = tail.readUnsignedInt(relativeOffset + 16)
        return if (
            entryCount != ZIP64_UNSIGNED_SHORT_SENTINEL &&
            directorySize != ZIP64_UNSIGNED_INT_SENTINEL &&
            directoryOffset != ZIP64_UNSIGNED_INT_SENTINEL
        ) {
            descriptor(
                entryCount = entryCount,
                directoryOffset = directoryOffset,
                physicalDirectoryOffset = recordOffset - directorySize,
            )
        } else {
            readZip64Descriptor(archive, recordOffset)
        }
    }

    private fun readZip64Descriptor(
        archive: RandomAccessFile,
        endRecordOffset: Long,
    ): CentralDirectoryDescriptor? {
        val locatorOffset = endRecordOffset - ZIP64_LOCATOR_SIZE
        if (locatorOffset < 0L) return null
        archive.seek(locatorOffset)
        if (archive.readUnsignedInt() != ZIP64_LOCATOR_SIGNATURE) return null
        archive.skipBytes(4)
        val declaredRecordOffset = archive.readUnsignedLong()
        val recordOffsets = buildList {
            if (declaredRecordOffset >= 0L) add(declaredRecordOffset)
            findZip64RecordBefore(archive, locatorOffset)?.let(::add)
        }.distinct()

        recordOffsets.forEach { recordOffset ->
            if (recordOffset < 0L || recordOffset + ZIP64_END_RECORD_MINIMUM_SIZE > locatorOffset) {
                return@forEach
            }
            archive.seek(recordOffset)
            if (archive.readUnsignedInt() != ZIP64_END_RECORD_SIGNATURE) return@forEach
            val recordBodySize = archive.readUnsignedLong()
            if (
                recordBodySize < ZIP64_END_RECORD_MINIMUM_BODY_SIZE ||
                recordOffset + ZIP64_END_RECORD_PREFIX_SIZE + recordBodySize != locatorOffset
            ) {
                return@forEach
            }
            archive.skipBytes(20)
            val entryCount = archive.readUnsignedLong()
            val directorySize = archive.readUnsignedLong()
            val directoryOffset = archive.readUnsignedLong()
            return descriptor(
                entryCount = entryCount,
                directoryOffset = directoryOffset,
                physicalDirectoryOffset = recordOffset - directorySize,
            )
        }
        return null
    }

    private fun findZip64RecordBefore(archive: RandomAccessFile, locatorOffset: Long): Long? {
        val searchStart = maxOf(0L, locatorOffset - MAX_ZIP64_END_RECORD_SEARCH_BYTES)
        val searchLength = (locatorOffset - searchStart).toInt()
        if (searchLength < ZIP64_END_RECORD_MINIMUM_SIZE) return null
        val bytes = ByteArray(searchLength)
        archive.seek(searchStart)
        archive.readFully(bytes)
        for (offset in bytes.size - ZIP64_END_RECORD_MINIMUM_SIZE downTo 0) {
            if (bytes.readUnsignedInt(offset) != ZIP64_END_RECORD_SIGNATURE) continue
            val bodySize = bytes.readUnsignedLong(offset + 4) ?: continue
            if (bodySize < ZIP64_END_RECORD_MINIMUM_BODY_SIZE) continue
            if (searchStart + offset + ZIP64_END_RECORD_PREFIX_SIZE + bodySize == locatorOffset) {
                return searchStart + offset
            }
        }
        return null
    }

    private fun descriptor(
        entryCount: Long,
        directoryOffset: Long,
        physicalDirectoryOffset: Long,
    ): CentralDirectoryDescriptor? {
        if (entryCount !in 0L..ArchiveStructureLimits.DEFAULT.maxEntries.toLong()) return null
        val candidates = listOf(directoryOffset, physicalDirectoryOffset)
            .filter { it >= 0L }
            .distinct()
        if (candidates.isEmpty()) return null
        return CentralDirectoryDescriptor(entryCount.toInt(), candidates)
    }

    private fun readCentralDirectory(
        archive: RandomAccessFile,
        offset: Long,
        entryCount: Int,
    ): List<ByteArray>? {
        if (offset > archive.length()) return null
        archive.seek(offset)
        val result = ArrayList<ByteArray>()
        var retainedBytes = 0
        repeat(entryCount) {
            if (archive.filePointer + CENTRAL_DIRECTORY_HEADER_SIZE > archive.length()) return null
            if (archive.readUnsignedInt() != CENTRAL_DIRECTORY_SIGNATURE) return null
            val header = ByteArray(CENTRAL_DIRECTORY_HEADER_SIZE - Int.SIZE_BYTES)
            archive.readFully(header)
            val flags = header.readUnsignedShort(CENTRAL_DIRECTORY_FLAGS_OFFSET_AFTER_SIGNATURE)
            val nameLength = header.readUnsignedShort(CENTRAL_DIRECTORY_NAME_LENGTH_OFFSET_AFTER_SIGNATURE)
            val extraLength = header.readUnsignedShort(CENTRAL_DIRECTORY_EXTRA_LENGTH_OFFSET_AFTER_SIGNATURE)
            val commentLength = header.readUnsignedShort(CENTRAL_DIRECTORY_COMMENT_LENGTH_OFFSET_AFTER_SIGNATURE)
            if (nameLength <= 0) return null
            val name = ByteArray(nameLength)
            archive.readFully(name)
            val remainingHeaderBytes = extraLength.toLong() + commentLength
            val nextOffset = Math.addExact(archive.filePointer, remainingHeaderBytes)
            if (nextOffset > archive.length()) return null
            archive.seek(nextOffset)

            if (
                flags and UTF8_FILENAME_FLAG == 0 &&
                name.any { byte -> byte.toInt() and 0x80 != 0 } &&
                retainedBytes <= MAX_RETAINED_NAME_BYTES - name.size
            ) {
                result += name
                retainedBytes += name.size
            }
        }
        return result
    }

    private fun RandomAccessFile.readUnsignedInt(): Long {
        val bytes = ByteArray(Int.SIZE_BYTES)
        readFully(bytes)
        return bytes.readUnsignedInt(0)
    }

    private fun RandomAccessFile.readUnsignedLong(): Long {
        val bytes = ByteArray(Long.SIZE_BYTES)
        readFully(bytes)
        return bytes.readUnsignedLong(0)
            ?: throw IOException("ZIP64 value exceeds the supported signed range")
    }

    private fun ByteArray.readUnsignedShort(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.readUnsignedInt(offset: Int): Long =
        readUnsignedShort(offset).toLong() or
            (readUnsignedShort(offset + 2).toLong() shl 16)

    private fun ByteArray.readUnsignedLong(offset: Int): Long? {
        val low = readUnsignedInt(offset)
        val high = readUnsignedInt(offset + Int.SIZE_BYTES)
        if (high > Int.MAX_VALUE.toLong()) return null
        return low or (high shl 32)
    }

    private data class CentralDirectoryDescriptor(
        val entryCount: Int,
        val candidateOffsets: List<Long>,
    )

    private const val READ_MODE = "r"
    private const val UTF8_FILENAME_FLAG = 0x0800
    private const val CENTRAL_DIRECTORY_FLAGS_OFFSET_AFTER_SIGNATURE = 4
    private const val CENTRAL_DIRECTORY_NAME_LENGTH_OFFSET_AFTER_SIGNATURE = 24
    private const val CENTRAL_DIRECTORY_EXTRA_LENGTH_OFFSET_AFTER_SIGNATURE = 26
    private const val CENTRAL_DIRECTORY_COMMENT_LENGTH_OFFSET_AFTER_SIGNATURE = 28
    private const val CENTRAL_DIRECTORY_HEADER_SIZE = 46
    private const val END_OF_CENTRAL_DIRECTORY_SIZE = 22
    private const val ZIP64_LOCATOR_SIZE = 20L
    private const val ZIP64_END_RECORD_MINIMUM_SIZE = 56
    private const val ZIP64_END_RECORD_PREFIX_SIZE = 12L
    private const val ZIP64_END_RECORD_MINIMUM_BODY_SIZE = 44L
    private const val MAX_END_RECORD_SEARCH_BYTES = 1024L * 1024L
    private const val MAX_ZIP64_END_RECORD_SEARCH_BYTES = 1024L * 1024L
    private const val MAX_RETAINED_NAME_BYTES = 16 * 1024 * 1024
    private const val CENTRAL_DIRECTORY_SIGNATURE = 0x02014B50L
    private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50L
    private const val ZIP64_END_RECORD_SIGNATURE = 0x06064B50L
    private const val ZIP64_LOCATOR_SIGNATURE = 0x07064B50L
    private const val ZIP64_UNSIGNED_SHORT_SENTINEL = 0xFFFFL
    private const val ZIP64_UNSIGNED_INT_SENTINEL = 0xFFFF_FFFFL
}
