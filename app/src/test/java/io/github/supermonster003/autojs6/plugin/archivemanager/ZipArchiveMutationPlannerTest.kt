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
        val source = snapshot()
        val plan = ZipArchiveMutationPlanner.plan(
            source,
            ArchiveMutationRequest.Delete(setOf("docs")),
        )

        assertEquals(listOf("root.bin"), plan.entries.map(ZipArchiveMutationEntry::archivePath))
        assertEquals(ArchiveFormat.ZIP, plan.format)
        assertEquals(ArchiveOperation.DELETE, plan.operation)
        assertEquals(1, plan.workEstimate.resultEntryCount)
        assertEquals(1, plan.workEstimate.resultFileCount)
        assertEquals(0, plan.workEstimate.resultDirectoryCount)
        assertEquals(3L, plan.workEstimate.knownContentBytesToRead)
        assertEquals(0, plan.workEstimate.unknownContentFileCount)
        assertTrue(plan.sourceVersion.matches(source))
        assertFalse(
            plan.sourceVersion.matches(
                source.copy(sourceLastModifiedMillis = source.sourceLastModifiedMillis + 1L),
            ),
        )
        assertTrue(
            ArchiveMutationMetadataEffect.ARCHIVE_COMMENT_DROPPED in plan.metadataEffects,
        )
    }

    @Test
    fun `engine exposes bounded zip mutation availability without overclaiming variants`() {
        val ordinary = snapshot()
        val engine = ArchiveEngine.DEFAULT

        val available = engine.mutationAvailability(ordinary)
        assertTrue(available.isAvailable)
        assertEquals(ZIP_MUTATION_CAPABILITIES, available.capabilities)

        val multiVolume = ordinary.copy(
            volumeIdentities = listOf(
                ArchiveVolumeIdentity("archive.z01", 1L, 2L),
                ArchiveVolumeIdentity("archive.zip", ordinary.sourceLength, 3L),
            ),
        )
        val multiVolumeAvailability = engine.mutationAvailability(multiVolume)
        assertFalse(multiVolumeAvailability.isAvailable)
        assertEquals(
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE,
            multiVolumeAvailability.unavailableReason,
        )

        val unsafe = ordinary.copy(
            entries = ordinary.entries.mapIndexed { index, entry ->
                if (index == 0) entry.copy(pathStatus = ArchiveEntryPathStatus.UNSAFE_ISOLATED)
                else entry
            },
        )
        val unsafeAvailability = engine.mutationAvailability(unsafe)
        assertFalse(unsafeAvailability.isAvailable)
        assertEquals(
            ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH,
            unsafeAvailability.unavailableReason,
        )
    }

    @Test
    fun `renaming an implicit directory rewrites descendant paths in source order`() {
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ArchiveMutationRequest.Rename("docs", "manual"),
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
            ArchiveMutationRequest.AddDirectory("docs", "drafts"),
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
            ArchiveMutationRequest.AddFiles(
                parentPath = "docs",
                files = listOf(
                    ArchiveMutationAddedFile("notes.txt", size = 5L) {
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
    fun `adds a complete folder tree and preserves empty directories without opening files`() {
        var opened = false
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ArchiveMutationRequest.AddTree(
                parentPath = "docs",
                entries = listOf(
                    ArchiveMutationAddedTreeEntry.Directory("bundle", lastModified = 10L),
                    ArchiveMutationAddedTreeEntry.Directory("bundle/empty", lastModified = 20L),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        relativePath = "bundle/notes.txt",
                        file = ArchiveMutationAddedFile("notes.txt", size = 5L, lastModified = 30L) {
                            opened = true
                            ByteArrayInputStream("notes".encodeToByteArray())
                        },
                    ),
                ),
            ),
        )

        assertFalse(opened)
        assertEquals(
            listOf("docs/bundle", "docs/bundle/empty", "docs/bundle/notes.txt"),
            plan.entries.takeLast(3).map(ZipArchiveMutationEntry::archivePath),
        )
        assertTrue(plan.entries[plan.entries.lastIndex - 2].isDirectory)
        assertEquals(20L, plan.entries[plan.entries.lastIndex - 1].lastModified)
        assertEquals(5L, plan.entries.last().size)
    }

    @Test
    fun `auto renames an imported root instead of merging with existing archive content`() {
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ArchiveMutationRequest.AddTree(
                parentPath = "",
                entries = listOf(
                    ArchiveMutationAddedTreeEntry.Directory("docs"),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        "docs/new.txt",
                        ArchiveMutationAddedFile("new.txt") {
                            ByteArrayInputStream(byteArrayOf(9))
                        },
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("docs (2)", "docs (2)/new.txt"),
            plan.entries.takeLast(2).map(ZipArchiveMutationEntry::archivePath),
        )
    }

    @Test
    fun `adds mixed files and multiple folder roots as one batch`() {
        var opened = false
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ArchiveMutationRequest.AddTree(
                parentPath = "",
                entries = listOf(
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        "loose.txt",
                        ArchiveMutationAddedFile("loose.txt", size = 5L) {
                            opened = true
                            ByteArrayInputStream("loose".encodeToByteArray())
                        },
                    ),
                    ArchiveMutationAddedTreeEntry.Directory("docs"),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        "docs/new.txt",
                        ArchiveMutationAddedFile("new.txt", size = 3L) {
                            opened = true
                            ByteArrayInputStream("new".encodeToByteArray())
                        },
                    ),
                    ArchiveMutationAddedTreeEntry.Directory("empty"),
                ),
            ),
        )

        assertFalse(opened)
        assertEquals(
            listOf("loose.txt", "docs (2)", "docs (2)/new.txt", "empty"),
            plan.entries.takeLast(4).map(ZipArchiveMutationEntry::archivePath),
        )
        assertTrue(plan.entries.last().isDirectory)
    }

    @Test
    fun `keeps same named selected folders separate while numbering archive conflicts`() {
        val plan = ZipArchiveMutationPlanner.plan(
            snapshot(),
            ArchiveMutationRequest.AddTree(
                parentPath = "",
                entries = listOf(
                    ArchiveMutationAddedTreeEntry.Directory(
                        relativePath = "docs",
                        inputRootId = "first-root",
                    ),
                    ArchiveMutationAddedTreeEntry.Directory(
                        relativePath = "docs",
                        inputRootId = "second-root",
                    ),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        relativePath = "docs/first.txt",
                        file = ArchiveMutationAddedFile("first.txt") {
                            ByteArrayInputStream("first".encodeToByteArray())
                        },
                        inputRootId = "first-root",
                    ),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        relativePath = "docs/second.txt",
                        file = ArchiveMutationAddedFile("second.txt") {
                            ByteArrayInputStream("second".encodeToByteArray())
                        },
                        inputRootId = "second-root",
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("docs (2)", "docs (3)", "docs (2)/first.txt", "docs (3)/second.txt"),
            plan.entries.takeLast(4).map(ZipArchiveMutationEntry::archivePath),
        )
    }

    @Test
    fun `rejects folder trees with missing parents`() {
        val missingParent = ArchiveMutationRequest.AddTree(
            parentPath = "",
            entries = listOf(
                ArchiveMutationAddedTreeEntry.Directory("bundle"),
                ArchiveMutationAddedTreeEntry.FileEntry(
                    "bundle/missing/file.txt",
                    ArchiveMutationAddedFile("file.txt") { ByteArrayInputStream(byteArrayOf(1)) },
                ),
            ),
        )
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_PATH) {
            ZipArchiveMutationPlanner.plan(snapshot(), missingParent)
        }
    }

    @Test
    fun `direct file collision rejects the complete mixed batch`() {
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.DUPLICATE_PATH) {
            ZipArchiveMutationPlanner.plan(
                snapshot(),
                ArchiveMutationRequest.AddTree(
                    parentPath = "",
                    entries = listOf(
                        ArchiveMutationAddedTreeEntry.FileEntry(
                            "root.bin",
                            ArchiveMutationAddedFile("root.bin", size = 1L) {
                                ByteArrayInputStream(byteArrayOf(9))
                            },
                        ),
                        ArchiveMutationAddedTreeEntry.Directory("new-folder"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `rejects rename collisions before output reservation`() {
        expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
        ) {
            ZipArchiveMutationPlanner.plan(
                snapshot(),
                ArchiveMutationRequest.Rename("docs", "root.bin"),
            )
        }
    }

    @Test
    fun `rejects a new file that would replace an implicit directory`() {
        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.FILE_DIRECTORY_CONFLICT) {
            ZipArchiveMutationPlanner.plan(
                snapshot(),
                ArchiveMutationRequest.AddFiles(
                    parentPath = "",
                    files = listOf(
                        ArchiveMutationAddedFile("docs") { ByteArrayInputStream(ByteArray(0)) },
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
                    ArchiveMutationRequest.AddFiles(
                        parentPath = "",
                        files = listOf(
                            ArchiveMutationAddedFile(displayName) {
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
            ArchiveMutationRequest.AddDirectory("", "other"),
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
                ArchiveMutationRequest.Rename("safe.txt", "renamed.txt"),
            )
        }
    }
}
