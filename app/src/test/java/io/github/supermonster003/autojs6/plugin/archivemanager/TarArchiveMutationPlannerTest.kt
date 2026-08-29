package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class TarArchiveMutationPlannerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `rename rewrites a complete directory subtree and reports factual work`() {
        val snapshot = ordinarySnapshot()

        val plan = TarArchiveMutationPlanner.plan(
            snapshot,
            ArchiveMutationRequest.Rename("docs", "manual"),
        )

        assertEquals(
            listOf("manual", "manual/readme.txt", "root.bin"),
            plan.entries.map(TarArchiveMutationEntry::archivePath),
        )
        assertEquals(ArchiveOperation.RENAME, plan.operation)
        assertEquals(2, plan.workEstimate.resultFileCount)
        assertEquals(1, plan.workEstimate.resultDirectoryCount)
        assertEquals(9L, plan.workEstimate.knownContentBytesToRead)
        assertEquals(0, plan.workEstimate.unknownContentFileCount)
        assertEquals(TAR_MUTATION_CAPABILITIES.metadataEffects, plan.metadataEffects)
        assertTrue(plan.sourceVersion.matches(snapshot))
    }

    @Test
    fun `delete removes the selected entry and all descendants`() {
        val plan = TarArchiveMutationPlanner.plan(
            ordinarySnapshot(),
            ArchiveMutationRequest.Delete(setOf("docs")),
        )

        assertEquals(listOf("root.bin"), plan.entries.map(TarArchiveMutationEntry::archivePath))
        assertEquals(ArchiveOperation.DELETE, plan.operation)
    }

    @Test
    fun `new directory and files retain unknown size as an honest estimate`() {
        val snapshot = ordinarySnapshot()
        val directoryPlan = TarArchiveMutationPlanner.plan(
            snapshot,
            ArchiveMutationRequest.AddDirectory("docs", "empty"),
        )
        val filePlan = TarArchiveMutationPlanner.plan(
            snapshot,
            ArchiveMutationRequest.AddFiles(
                parentPath = "docs",
                files = listOf(
                    ArchiveMutationAddedFile("known.txt", size = 5L) {
                        ByteArrayInputStream("known".encodeToByteArray())
                    },
                    ArchiveMutationAddedFile("unknown.txt") {
                        ByteArrayInputStream("unknown".encodeToByteArray())
                    },
                ),
            ),
        )

        assertEquals("docs/empty", directoryPlan.entries.last().archivePath)
        assertTrue(directoryPlan.entries.last().isDirectory)
        assertEquals(
            listOf("docs/known.txt", "docs/unknown.txt"),
            filePlan.entries.takeLast(2).map(TarArchiveMutationEntry::archivePath),
        )
        assertEquals(1, filePlan.workEstimate.unknownContentFileCount)
        assertEquals(14L, filePlan.workEstimate.knownContentBytesToRead)
    }

    @Test
    fun `tree import preserves empty directories and safely renames an occupied root`() {
        val plan = TarArchiveMutationPlanner.plan(
            ordinarySnapshot(),
            ArchiveMutationRequest.AddTree(
                parentPath = "",
                entries = listOf(
                    ArchiveMutationAddedTreeEntry.Directory("docs"),
                    ArchiveMutationAddedTreeEntry.Directory("docs/empty"),
                    ArchiveMutationAddedTreeEntry.FileEntry(
                        "docs/nested.txt",
                        ArchiveMutationAddedFile("nested.txt", size = 6L) {
                            ByteArrayInputStream("nested".encodeToByteArray())
                        },
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("docs (2)", "docs (2)/empty", "docs (2)/nested.txt"),
            plan.entries.takeLast(3).map(TarArchiveMutationEntry::archivePath),
        )
        assertTrue(plan.entries[plan.entries.lastIndex - 1].isDirectory)
    }

    @Test
    fun `duplicate portable names and missing selections fail before output reservation`() {
        val snapshot = ordinarySnapshot()

        val duplicate = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.DUPLICATE_PATH,
        ) {
            TarArchiveMutationPlanner.plan(
                snapshot,
                ArchiveMutationRequest.AddFiles(
                    parentPath = "",
                    files = listOf(
                        ArchiveMutationAddedFile("ROOT.bin", size = 1L) {
                            ByteArrayInputStream(byteArrayOf(1))
                        },
                    ),
                ),
            )
        }
        assertEquals(ArchiveFormat.TAR, duplicate.format)

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.UNKNOWN_SELECTION) {
            TarArchiveMutationPlanner.plan(
                snapshot,
                ArchiveMutationRequest.Delete(setOf("missing")),
            )
        }
    }

    @Test
    fun `unsafe and special-entry tar archives remain explicitly read only`() {
        val unsafe = ArchiveScanner().scan(
            writeTar(
                temporaryFolder.newFile("unsafe.tar"),
                TarFixtureEntry("../outside.txt", "outside".encodeToByteArray()),
            ),
        )
        val special = ArchiveScanner().scan(
            writeTar(
                temporaryFolder.newFile("special.tar"),
                TarFixtureEntry("target.txt", "target".encodeToByteArray()),
                TarFixtureEntry(
                    "link",
                    type = TarFixtureEntryType.SYMBOLIC_LINK,
                    linkName = "target.txt",
                ),
            ),
        )

        assertEquals(
            ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH,
            tarMutationAvailability(unsafe).unavailableReason,
        )
        assertEquals(
            ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
            tarMutationAvailability(special).unavailableReason,
        )
        assertFalse(special.entries.last().capabilities.canDelete)
        assertFalse(special.entries.last().capabilities.canRename)
    }

    @Test
    fun `gzip xz and bzip2 tar have explicit mutation while zstd remains read only`() {
        val gzipSnapshot = ArchiveScanner().scan(
            writeTarGzip(
                temporaryFolder.newFile("compressed.tar.gz"),
                TarFixtureEntry("payload.txt", "payload".encodeToByteArray()),
            ),
        )
        val xzSnapshot = ArchiveScanner().scan(
            writeTarXz(
                temporaryFolder.newFile("compressed.tar.xz"),
                TarFixtureEntry("payload.txt", "payload".encodeToByteArray()),
            ),
        )
        val bzip2Snapshot = ArchiveScanner().scan(
            writeTarBzip2(
                temporaryFolder.newFile("compressed.tar.bz2"),
                TarFixtureEntry("payload.txt", "payload".encodeToByteArray()),
            ),
        )

        listOf(
            Triple(ArchiveFormat.TAR_GZIP, TAR_GZIP_MUTATION_CAPABILITIES, gzipSnapshot),
            Triple(ArchiveFormat.TAR_XZ, TAR_XZ_MUTATION_CAPABILITIES, xzSnapshot),
            Triple(ArchiveFormat.TAR_BZIP2, TAR_BZIP2_MUTATION_CAPABILITIES, bzip2Snapshot),
        ).forEach { (format, capabilities, snapshot) ->
            assertEquals(format, snapshot.format)
            assertEquals(
                capabilities,
                ArchiveEngine.DEFAULT.mutationCapabilities(snapshot.format),
            )
            assertEquals(
                capabilities,
                ArchiveEngine.DEFAULT.mutationAvailability(snapshot).capabilities,
            )
            assertTrue(snapshot.entries.single().capabilities.canDelete)
            assertTrue(snapshot.entries.single().capabilities.canRename)
            val plan = TarArchiveMutationPlanner.plan(
                snapshot,
                ArchiveMutationRequest.Rename("payload.txt", "renamed.txt"),
            )
            assertEquals(format, plan.format)
            assertEquals(
                listOf("renamed.txt"),
                plan.entries.map(TarArchiveMutationEntry::archivePath),
            )
            assertTrue(
                ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED in
                    plan.metadataEffects,
            )
        }

        listOf(
            ArchiveScanner().scan(
                writeTarZstd(
                    temporaryFolder.newFile("compressed.tar.zst"),
                    TarFixtureEntry("payload.txt", "payload".encodeToByteArray()),
                ),
            ),
        ).forEach { snapshot ->
            assertEquals(null, ArchiveEngine.DEFAULT.mutationCapabilities(snapshot.format))
            assertEquals(
                ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
                ArchiveEngine.DEFAULT.mutationAvailability(snapshot).unavailableReason,
            )
            assertFalse(snapshot.entries.single().capabilities.canDelete)
            assertFalse(snapshot.entries.single().capabilities.canRename)
        }
    }

    private fun ordinarySnapshot(): ArchiveSnapshot = ArchiveScanner().scan(
        writeTar(
            temporaryFolder.newFile(),
            TarFixtureEntry("docs/", type = TarFixtureEntryType.DIRECTORY),
            TarFixtureEntry("docs/readme.txt", "readme".encodeToByteArray()),
            TarFixtureEntry("root.bin", byteArrayOf(1, 2, 3)),
        ),
    )
}
