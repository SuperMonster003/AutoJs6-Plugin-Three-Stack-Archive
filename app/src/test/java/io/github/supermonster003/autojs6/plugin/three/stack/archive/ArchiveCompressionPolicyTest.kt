package io.github.supermonster003.autojs6.plugin.three.stack.archive

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
        assertEquals(
            "Documents.tar.zst",
            ArchiveCompressionPolicy.defaultOutputDisplayName(
                targetDisplayNames = listOf("a.txt", "b.txt"),
                parentDisplayPath = "/storage/emulated/0/Documents/",
                fallbackStem = "Archive",
                format = ArchiveFormat.TAR_ZSTD,
            ),
        )
    }

    @Test
    fun separateOutputPreviewUsesEachTargetNameAndSelectedCompleteSuffix() {
        assertEquals(
            listOf("report.txt.tar.gz", "photos.tar.gz", "notes.md.tar.gz"),
            ArchiveCreationPlanner.separateOutputDisplayNames(
                targetDisplayNames = listOf("report.txt", "photos", "notes.md"),
                parentDisplayPath = "/storage/emulated/0/Documents",
                fallbackStem = "Archive",
                format = ArchiveFormat.TAR_GZIP,
            ),
        )
    }

    @Test
    fun defaultNameTruncatesAValidLongTargetInsteadOfLosingItsIdentity() {
        val name = ArchiveCompressionPolicy.defaultOutputDisplayName(
            targetDisplayNames = listOf("a".repeat(ExplorerActionProtocol.MAX_TARGET_DISPLAY_NAME_LENGTH)),
            parentDisplayPath = "/Documents",
            fallbackStem = "Archive",
            format = ArchiveFormat.TAR_ZSTD,
        )

        assertEquals(ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH, name.length)
        assertEquals(true, name.startsWith("a"))
        assertEquals(true, name.endsWith(".tar.zst"))
    }

    @Test
    fun switchingFormatsReplacesTheCompleteRegisteredSuffix() {
        assertEquals(
            "Documents.tar.xz",
            ArchiveCompressionPolicy.outputDisplayNameForFormat(
                "Documents.zip",
                ArchiveFormat.ZIP,
                ArchiveFormat.TAR_XZ,
            ),
        )
        assertEquals(
            "Documents.tar.bz2",
            ArchiveCompressionPolicy.outputDisplayNameForFormat(
                "Documents.TAR.GZ",
                ArchiveFormat.TAR_GZIP,
                ArchiveFormat.TAR_BZIP2,
            ),
        )
        assertEquals(
            "custom-name.tar",
            ArchiveCompressionPolicy.outputDisplayNameForFormat(
                "custom-name",
                ArchiveFormat.ZIP,
                ArchiveFormat.TAR,
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
        assertEquals(
            0,
            ArchiveCompressionPolicy.requireCompressionLevel(
                0,
                listOf(0),
                ArchiveFormat.TAR,
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveCompressionPolicy.requireCompressionLevel(
                0,
                (1..9).toList(),
                ArchiveFormat.TAR_BZIP2,
            )
        }
    }

    @Test
    fun passwordConfirmationRequiresAnExactMatchWithoutStrings() {
        assertEquals(
            true,
            ArchiveCompressionPolicy.passwordConfirmationMatches(CharArray(0), CharArray(0)),
        )
        assertEquals(
            true,
            ArchiveCompressionPolicy.passwordConfirmationMatches(
                "ArchiveManager-Test-2026".toCharArray(),
                "ArchiveManager-Test-2026".toCharArray(),
            ),
        )
        assertEquals(
            false,
            ArchiveCompressionPolicy.passwordConfirmationMatches(
                "ArchiveManager-Test-2026".toCharArray(),
                "ArchiveManager-Test-2027".toCharArray(),
            ),
        )
        assertEquals(
            false,
            ArchiveCompressionPolicy.passwordConfirmationMatches(
                "ArchiveManager-Test-2026".toCharArray(),
                "ArchiveManager-Test-2026-extra".toCharArray(),
            ),
        )
    }
}
