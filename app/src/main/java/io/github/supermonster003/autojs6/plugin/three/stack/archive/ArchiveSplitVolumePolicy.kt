package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.autojs.plugin.explorer.api.ExplorerActionProtocol

/** Shared validation and presets for standard ZIP split-volume creation. */
internal object ArchiveSplitVolumePolicy {

    val presetSizesMiB: List<Long> = listOf(10L, 50L, 100L, 500L, 1_024L, 4_096L)

    fun bytesFromMiB(sizeMiB: Long): Long {
        require(sizeMiB in MIN_SIZE_MIB..MAX_SIZE_MIB) {
            "ZIP split volume size must be between $MIN_SIZE_MIB and $MAX_SIZE_MIB MiB"
        }
        return Math.multiplyExact(sizeMiB, MIB_BYTES)
    }

    fun requireVolumeSizeBytes(sizeBytes: Long): Long {
        require(sizeBytes % MIB_BYTES == 0L) {
            "ZIP split volume size must use whole MiB units"
        }
        bytesFromMiB(sizeBytes / MIB_BYTES)
        return sizeBytes
    }

    fun sizeMiB(sizeBytes: Long): Long =
        requireVolumeSizeBytes(sizeBytes) / MIB_BYTES

    fun parseCustomSizeMiB(value: String): Long? = value
        .trim()
        .toLongOrNull()
        ?.takeIf { it in MIN_SIZE_MIB..MAX_SIZE_MIB }

    const val MIN_SIZE_MIB = 1L
    const val MAX_SIZE_MIB = 4_096L
    const val MAX_VOLUME_FILES = 65_535

    /**
     * Leaves enough room for the longest classic part suffix, `.z65534`, within the host limit.
     */
    const val MAX_TERMINAL_DISPLAY_NAME_LENGTH =
        ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH - 3

    private const val MIB_BYTES = 1_024L * 1_024L
}
