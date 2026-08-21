package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ArchiveCompressionPolicyTest {

    @Test
    fun singleTargetDefaultsToItsOwnNameAndAddsZipExtension() {
        assertEquals(
            "report.pdf.zip",
            ArchiveCompressionPolicy.defaultOutputDisplayName(
                targetDisplayNames = listOf("report.pdf"),
                parentDisplayPath = "/storage/emulated/0/Documents",
                fallbackStem = "Archive",
            ),
        )
        assertEquals(
            "existing.ZIP",
            ArchiveCompressionPolicy.normalizeOutputDisplayName(" existing.ZIP "),
        )
    }

    @Test
    fun multipleTargetsDefaultToTheirCommonParentName() {
        assertEquals(
            "Documents.zip",
            ArchiveCompressionPolicy.defaultOutputDisplayName(
                targetDisplayNames = listOf("a.txt", "b.txt"),
                parentDisplayPath = "/storage/emulated/0/Documents/",
                fallbackStem = "Archive",
            ),
        )
        assertEquals(
            "Archive.zip",
            ArchiveCompressionPolicy.defaultOutputDisplayName(
                targetDisplayNames = listOf("a.txt", "b.txt"),
                parentDisplayPath = "/",
                fallbackStem = "Archive",
            ),
        )
    }

    @Test
    fun outputNameRejectsTraversalControlsAndOverlongValues() {
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName("../archive"))
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName("folder/archive"))
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName("folder\\archive"))
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName("bad\u0000name"))
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName("."))
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName(".ZIP"))
        assertNull(ArchiveCompressionPolicy.normalizeOutputDisplayName("...zip"))
        assertNull(
            ArchiveCompressionPolicy.normalizeOutputDisplayName(
                "a".repeat(ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH),
            ),
        )
    }

    @Test
    fun entryPathsArePortableBoundedAndSlashSeparated() {
        assertEquals("folder/child.txt", ArchiveCompressionPolicy.joinEntryPath("folder", "child.txt"))
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveCompressionPolicy.joinEntryPath("folder", "..")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveCompressionPolicy.joinEntryPath("folder", "bad\\name")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveCompressionPolicy.joinEntryPath(
                "a".repeat(ExplorerActionProtocol.MAX_SESSION_RELATIVE_PATH_LENGTH),
                "b",
            )
        }
    }

    @Test
    fun compressionLevelIsStrictlyBounded() {
        assertEquals(0, ArchiveCompressionPolicy.requireCompressionLevel(0))
        assertEquals(9, ArchiveCompressionPolicy.requireCompressionLevel(9))
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveCompressionPolicy.requireCompressionLevel(-1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveCompressionPolicy.requireCompressionLevel(10)
        }
    }
}
