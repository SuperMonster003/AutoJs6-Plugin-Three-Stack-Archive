package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry

class ZipArchiveMutationPlannerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun snapshot(): ArchiveSnapshot = ArchiveScanner().scan(
        writeZip(
            temporaryFolder.newFile(),
            FixtureEntry("docs/readme.txt", "readme".encodeToByteArray()),
            FixtureEntry("docs/empty/", method = ZipEntry.STORED),
            FixtureEntry("root.bin", byteArrayOf(1, 2, 3), ZipEntry.STORED),
        ),
    )

    @Test
    fun `deleting an implicit directory removes every descendant`() {
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ZipArchiveMutationRequest.Delete(setOf("docs")),
        )

        assertEquals(listOf("root.bin"), plan.entries.map(ZipArchiveMutationEntry::archivePath))
    }

    @Test
    fun `renaming an implicit directory rewrites descendant paths in source order`() {
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ZipArchiveMutationRequest.Rename("docs", "manual"),
        )

        assertEquals(
            listOf("manual/readme.txt", "manual/empty", "root.bin"),
            plan.entries.map(ZipArchiveMutationEntry::archivePath),
        )
        assertTrue(plan.entries.take(2).all { it.source is ZipArchiveMutationSource.Existing })
    }

    @Test
    fun `adds an explicit directory beneath an implicit parent`() {
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ZipArchiveMutationRequest.AddDirectory("docs", "drafts"),
        )

        val added = plan.entries.last()
        assertEquals("docs/drafts", added.archivePath)
        assertTrue(added.isDirectory)
        assertTrue(added.source is ZipArchiveMutationSource.AddedDirectory)
    }

    @Test
    fun `adds files without opening their streams during planning`() {
        var opened = false
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ZipArchiveMutationRequest.AddFiles(
                parentPath = "docs",
                files = listOf(
                    ZipArchiveAddedFile("notes.txt", size = 5L) {
                        opened = true
                        ByteArrayInputStream("notes".encodeToByteArray())
                    },
                ),
            ),
        )

        assertFalse(opened)
        assertEquals("docs/notes.txt", plan.entries.last().archivePath)
        assertEquals(5L, plan.entries.last().size)
    }

    @Test
    fun `rejects rename collisions before output reservation`() {
        expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
        ) {
            ZipArchiveMutationPlanner.plan(
                snapshot(),
                ZipArchiveMutationRequest.Rename("docs", "root.bin"),
            )
        }
    }

    @Test
    fun `rejects a new file that would replace an implicit directory`() {
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.FILE_DIRECTORY_CONFLICT) {
            ZipArchiveMutationPlanner.plan(
                snapshot(),
                ZipArchiveMutationRequest.AddFiles(
                    parentPath = "",
                    files = listOf(
                        ZipArchiveAddedFile("docs") { ByteArrayInputStream(ByteArray(0)) },
                    ),
                ),
            )
        }
    }

    @Test
    fun `rejects newly introduced case-insensitive and Unicode-equivalent names`() {
        val decomposedCafe = "cafe\u0301.txt"
        val archive = ArchiveScanner().scan(
            writeZip(
                temporaryFolder.newFile(),
                FixtureEntry("README.txt", byteArrayOf(1)),
                FixtureEntry("caf\u00E9.txt", byteArrayOf(2)),
            ),
        )

        listOf("readme.TXT", decomposedCafe).forEach { displayName ->
            expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DUPLICATE_PATH) {
                ZipArchiveMutationPlanner.plan(
                    archive,
                    ZipArchiveMutationRequest.AddFiles(
                        parentPath = "",
                        files = listOf(
                            ZipArchiveAddedFile(displayName) {
                                ByteArrayInputStream(byteArrayOf(3))
                            },
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun `preserves pre-existing portable-name collisions when an unrelated entry changes`() {
        val archive = ArchiveScanner().scan(
            writeZip(
                temporaryFolder.newFile(),
                FixtureEntry("Name.txt", byteArrayOf(1)),
                FixtureEntry("name.TXT", byteArrayOf(2)),
            ),
        )

        val plan = ZipArchiveMutationPlanner.plan(
            archive,
            ZipArchiveMutationRequest.AddDirectory("", "other"),
        )

        assertEquals(
            listOf("Name.txt", "name.TXT", "other"),
            plan.entries.map(ZipArchiveMutationEntry::archivePath),
        )
    }

    @Test
    fun `refuses to preserve unsafe entries during a rewrite`() {
        val unsafe = ArchiveScanner().scan(
            writeZip(
                temporaryFolder.newFile(),
                FixtureEntry("safe.txt", byteArrayOf(1)),
                FixtureEntry("../outside.txt", byteArrayOf(2)),
            ),
        )

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_PATH) {
            ZipArchiveMutationPlanner.plan(
                unsafe,
                ZipArchiveMutationRequest.Rename("safe.txt", "renamed.txt"),
            )
        }
    }
}
