package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveExtractionScopeResolverTest {

    @Test
    fun `offers whole archive and current directory without an explicit selection`() {
        val options = ArchiveExtractionScopeResolver.options(
            currentDirectory = "docs/guides",
            selectedPaths = emptyList(),
        )

        assertEquals(
            listOf(
                ArchiveExtractionScopeOption(
                    ArchiveExtractionScope.ENTIRE_ARCHIVE,
                    linkedSetOf(ArchivePathPolicy.ROOT_PATH),
                ),
                ArchiveExtractionScopeOption(
                    ArchiveExtractionScope.CURRENT_DIRECTORY,
                    linkedSetOf("docs/guides"),
                ),
            ),
            options,
        )
    }

    @Test
    fun `adds the current selection with stable order and no duplicates`() {
        val options = ArchiveExtractionScopeResolver.options(
            currentDirectory = "docs",
            selectedPaths = listOf("z.txt", "folder", "z.txt", "a.txt"),
        )

        assertEquals(ArchiveExtractionScope.CURRENT_SELECTION, options.last().scope)
        assertEquals(
            linkedSetOf("z.txt", "folder", "a.txt"),
            options.last().requestedPaths,
        )
    }

    @Test
    fun `does not duplicate the whole archive choice at the archive root`() {
        val options = ArchiveExtractionScopeResolver.options(
            currentDirectory = ArchivePathPolicy.ROOT_PATH,
            selectedPaths = listOf("file.txt"),
        )

        assertEquals(2, options.size)
        assertEquals(ArchiveExtractionScope.ENTIRE_ARCHIVE, options[0].scope)
        assertEquals(ArchiveExtractionScope.CURRENT_SELECTION, options[1].scope)
    }

    @Test
    fun `does not offer the current directory at a read-only archive location`() {
        val options = ArchiveExtractionScopeResolver.options(
            currentDirectory = "unsafe-isolation",
            selectedPaths = emptyList(),
            currentDirectoryCanExtract = false,
        )

        assertEquals(
            listOf(
                ArchiveExtractionScopeOption(
                    ArchiveExtractionScope.ENTIRE_ARCHIVE,
                    linkedSetOf(ArchivePathPolicy.ROOT_PATH),
                ),
            ),
            options,
        )
    }

    @Test
    fun `scope options do not retain a mutable selection collection`() {
        val selected = linkedSetOf("one.txt")
        val options = ArchiveExtractionScopeResolver.options("", selected)

        selected += "two.txt"

        assertEquals(linkedSetOf("one.txt"), options.last().requestedPaths)
    }
}
