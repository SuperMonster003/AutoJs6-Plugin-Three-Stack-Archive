package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveExtractionNamingTest {

    @Test
    fun `numbered zip and seven z first volumes use the archive base name`() {
        assertEquals(
            "project-backup",
            ArchiveExtractionNaming.rootName(
                "project-backup.zip.001",
                ArchiveFormat.ZIP,
            ),
        )
        assertEquals(
            "PROJECT-BACKUP",
            ArchiveExtractionNaming.rootName(
                "PROJECT-BACKUP.7Z.001",
                ArchiveFormat.SEVEN_Z,
            ),
        )
    }

    @Test
    fun `numbered suffix is not stripped for a mismatched detected format`() {
        assertEquals(
            "project-backup.zip",
            ArchiveExtractionNaming.rootName(
                "project-backup.zip.001",
                ArchiveFormat.SEVEN_Z,
            ),
        )
    }
}
