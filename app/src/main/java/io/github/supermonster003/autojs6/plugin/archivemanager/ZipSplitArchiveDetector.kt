package io.github.supermonster003.autojs6.plugin.archivemanager

import net.lingala.zip4j.ZipFile as Zip4jFile
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

internal enum class ZipSplitSegmentKind {
    FIRST_VOLUME,
    FINAL_VOLUME,
}

internal data class ZipSplitArchiveInfo(
    val segmentKind: ZipSplitSegmentKind,
    /** Zero-based disk number from the ordinary EOCD, or null for a Zip64 sentinel/unknown count. */
    val lastDiskNumber: Int? = null,
) {
    init {
        require(lastDiskNumber == null || lastDiskNumber > 0)
        require(segmentKind == ZipSplitSegmentKind.FINAL_VOLUME || lastDiskNumber == null)
    }

    val totalVolumeCount: Int?
        get() = lastDiskNumber?.let { it + 1 }

    fun finalVolumeName(displayName: String): String = when (segmentKind) {
        ZipSplitSegmentKind.FINAL_VOLUME -> displayName
        ZipSplitSegmentKind.FIRST_VOLUME -> {
            STANDARD_PART_PATTERN.matchEntire(displayName)?.groupValues?.get(1)
                ?.plus(ZIP_EXTENSION)
                ?: displayName
        }
    }

    fun requiredVolumeSummary(
        displayName: String,
        maxListedVolumes: Int = MAX_LISTED_VOLUMES,
    ): String {
        require(maxListedVolumes > 0)
        val numberOfEarlierVolumes = lastDiskNumber
            ?: return STANDARD_VOLUME_WILDCARD
        val stem = displayName.substringBeforeLast('.', displayName)
        val listedCount = minOf(numberOfEarlierVolumes, maxListedVolumes)
        val listed = (1..listedCount).joinToString(NAME_SEPARATOR) { number ->
            stem + if (number < 10) ".z0$number" else ".z$number"
        }
        val remaining = numberOfEarlierVolumes - listedCount
        return if (remaining > 0) "$listed (+$remaining)" else listed
    }

    fun technicalReason(displayName: String? = null): String {
        val volumeCount = totalVolumeCount?.toString() ?: "multiple"
        if (displayName == null) {
            return "ZIP is split across $volumeCount volumes; earlier volumes are unavailable"
        }
        return buildString {
            append("Required volumes: ")
            append(requiredVolumeSummary(displayName))
            append("; final volume: ")
            append(finalVolumeName(displayName))
            append("; ZIP is split across ")
            append(volumeCount)
            append(" volumes")
        }
    }

    private companion object {
        const val MAX_LISTED_VOLUMES = 8
        const val NAME_SEPARATOR = ", "
        const val STANDARD_VOLUME_WILDCARD = "*.z01, *.z02, ..."
        const val ZIP_EXTENSION = ".zip"
        val STANDARD_PART_PATTERN = Regex("(?i)^(.*)\\.z[0-9]{2,}$")
    }
}

internal class ZipSplitArchiveException(
    val info: ZipSplitArchiveInfo,
) : IOException(info.technicalReason())

/**
 * Detects standard PKZIP-style split archives without opening entry data or enumerating every
 * declared disk. Zip4j validates the EOCD/Zip64 directory shape; the bounded tail reader is used
 * only to recover the ordinary EOCD disk count for a useful, resource-safe diagnostic.
 */
internal object ZipSplitArchiveDetector {

    fun inspect(source: File): ZipSplitArchiveInfo? {
        if (!source.isFile || source.length() < ZIP_SIGNATURE_SIZE) return null
        if (hasLeadingSplitSignature(source)) {
            return ZipSplitArchiveInfo(ZipSplitSegmentKind.FIRST_VOLUME)
        }
        val isSplitArchive = try {
            Zip4jFile(source).use(Zip4jFile::isSplitArchive)
        } catch (_: Exception) {
            return null
        }
        if (!isSplitArchive) return null
        return ZipSplitArchiveInfo(
            segmentKind = ZipSplitSegmentKind.FINAL_VOLUME,
            lastDiskNumber = ordinaryEndRecordDiskNumber(source),
        )
    }

    private fun hasLeadingSplitSignature(source: File): Boolean = try {
        RandomAccessFile(source, READ_MODE).use { input ->
            input.readLittleEndianInt() == SPLIT_ARCHIVE_SIGNATURE
        }
    } catch (_: IOException) {
        false
    }

    private fun ordinaryEndRecordDiskNumber(source: File): Int? {
        val sourceLength = source.length()
        val tailLength = minOf(sourceLength, MAX_EOCD_SEARCH_BYTES.toLong()).toInt()
        if (tailLength < MIN_EOCD_SIZE) return null
        val tail = ByteArray(tailLength)
        try {
            RandomAccessFile(source, READ_MODE).use { input ->
                input.seek(sourceLength - tailLength)
                input.readFully(tail)
            }
        } catch (_: IOException) {
            return null
        }
        for (offset in tailLength - MIN_EOCD_SIZE downTo 0) {
            if (tail.littleEndianInt(offset) != END_OF_CENTRAL_DIRECTORY_SIGNATURE) continue
            val commentLength = tail.littleEndianUnsignedShort(offset + EOCD_COMMENT_LENGTH_OFFSET)
            if (offset + MIN_EOCD_SIZE + commentLength > tailLength) continue
            val diskNumber = tail.littleEndianUnsignedShort(offset + EOCD_DISK_NUMBER_OFFSET)
            return diskNumber.takeUnless { it == ZIP64_UNSIGNED_SHORT_SENTINEL || it == 0 }
        }
        return null
    }

    private fun RandomAccessFile.readLittleEndianInt(): Int {
        val first = read()
        val second = read()
        val third = read()
        val fourth = read()
        if (first or second or third or fourth < 0) throw IOException("Unexpected end of ZIP input")
        return first or (second shl 8) or (third shl 16) or (fourth shl 24)
    }

    private fun ByteArray.littleEndianInt(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)

    private fun ByteArray.littleEndianUnsignedShort(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private const val READ_MODE = "r"
    private const val ZIP_SIGNATURE_SIZE = 4L
    private const val MIN_EOCD_SIZE = 22
    private const val EOCD_DISK_NUMBER_OFFSET = 4
    private const val EOCD_COMMENT_LENGTH_OFFSET = 20
    private const val MAX_EOCD_SEARCH_BYTES = 1024 * 1024
    private const val ZIP64_UNSIGNED_SHORT_SENTINEL = 0xFFFF
    private const val SPLIT_ARCHIVE_SIGNATURE = 0x08074B50
    private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50
}
