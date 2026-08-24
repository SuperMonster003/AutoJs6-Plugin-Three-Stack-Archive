package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveSplitVolumePolicyTest {

    @Test
    fun `split volume presets and custom values use whole MiB units`() {
        assertEquals(listOf(10L, 50L, 100L, 500L, 1_024L, 4_096L),
            ArchiveSplitVolumePolicy.presetSizesMiB)
        assertEquals(1_048_576L, ArchiveSplitVolumePolicy.bytesFromMiB(1L))
        assertEquals(4_294_967_296L, ArchiveSplitVolumePolicy.bytesFromMiB(4_096L))
        assertEquals(2_048L, ArchiveSplitVolumePolicy.parseCustomSizeMiB(" 2048 "))
        assertNull(ArchiveSplitVolumePolicy.parseCustomSizeMiB("1.5"))
        assertNull(ArchiveSplitVolumePolicy.parseCustomSizeMiB("0"))
        assertNull(ArchiveSplitVolumePolicy.parseCustomSizeMiB("4097"))
    }

    @Test
    fun `split volume byte validation rejects out of range and partial MiB values`() {
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveSplitVolumePolicy.bytesFromMiB(0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveSplitVolumePolicy.bytesFromMiB(4_097L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveSplitVolumePolicy.requireVolumeSizeBytes(1_048_577L)
        }
    }

    @Test
    fun `split zip terminal names reserve room for the longest part suffix`() {
        val ordinaryName = "a".repeat(
            ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH - ".zip".length,
        ) + ".zip"

        val fitted = requireNotNull(
            ArchiveCompressionPolicy.fitSplitZipOutputDisplayName(ordinaryName),
        )
        val longestPart = fitted.dropLast(".zip".length) + ".z65534"

        assertEquals(ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH, fitted.length)
        assertEquals(ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH, longestPart.length)
        assertTrue(fitted.endsWith(".zip"))
    }

    @Test
    fun `split zip automatic numbering remains inside the reserved name boundary`() {
        val fitted = requireNotNull(
            ArchiveCompressionPolicy.fitSplitZipOutputDisplayName(
                "a".repeat(
                    ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH - ".zip".length,
                ) + ".zip",
            ),
        )

        val numbered = ArchiveCompressionPolicy.numberedCreationOutputDisplayName(
            value = fitted,
            format = ArchiveFormat.ZIP,
            index = 10_000,
            splitVolumeSizeBytes = ArchiveSplitVolumePolicy.bytesFromMiB(10L),
        )

        assertEquals(ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH, numbered.length)
        assertTrue(numbered.endsWith(" (10000).zip"))
        assertEquals(
            ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH,
            (numbered.dropLast(".zip".length) + ".z65534").length,
        )
    }

    @Test
    fun `split output naming is accepted only for zip`() {
        val size = ArchiveSplitVolumePolicy.bytesFromMiB(10L)

        assertEquals(
            "Archive.zip",
            ArchiveCompressionPolicy.normalizeCreationOutputDisplayName(
                value = "Archive",
                format = ArchiveFormat.ZIP,
                splitVolumeSizeBytes = size,
            ),
        )
        assertNull(
            ArchiveCompressionPolicy.normalizeCreationOutputDisplayName(
                value = "Archive",
                format = ArchiveFormat.SEVEN_Z,
                splitVolumeSizeBytes = size,
            ),
        )
    }

    @Test
    fun `split output fitting and numbering preserve the requested zip suffix case`() {
        val size = ArchiveSplitVolumePolicy.bytesFromMiB(10L)
        val fitted = requireNotNull(
            ArchiveCompressionPolicy.fitSplitZipOutputDisplayName("Archive.ZIP"),
        )

        assertEquals("Archive.ZIP", fitted)
        assertEquals(
            "Archive (1).ZIP",
            ArchiveCompressionPolicy.numberedCreationOutputDisplayName(
                value = fitted,
                format = ArchiveFormat.ZIP,
                index = 1,
                splitVolumeSizeBytes = size,
            ),
        )
    }
}
