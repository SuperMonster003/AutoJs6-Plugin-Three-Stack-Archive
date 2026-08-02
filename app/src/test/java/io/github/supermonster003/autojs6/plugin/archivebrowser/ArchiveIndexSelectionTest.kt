package io.github.supermonster003.autojs6.plugin.archivebrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipEntry

class ArchiveIndexSelectionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val snapshot by lazy {
        ArchiveScanner().scan(
            writeZip(
                temporaryFolder.newFile("index.zip"),
                FixtureEntry("docs/readme.txt", "readme".toByteArray()),
                FixtureEntry("docs/empty/", method = ZipEntry.STORED),
                FixtureEntry("assets/icon.bin", byteArrayOf(1, 2, 3), ZipEntry.STORED),
                FixtureEntry("root.txt", "root".toByteArray()),
            ),
        )
    }

    @Test
    fun `builds implicit hierarchy and aggregate metadata`() {
        val index = ArchiveIndex(snapshot)

        assertEquals(listOf("assets", "docs", "root.txt"), index.children().map(ArchiveNode::name))
        val docs = index.node("docs")!!
        assertTrue(docs.isDirectory)
        assertEquals(2, docs.childCount)
        assertEquals(1, docs.descendantFileCount)
        assertEquals(6L, docs.uncompressedSize)
        assertFalse(index.node("docs/readme.txt")!!.isDirectory)
    }

    @Test
    fun `searches paths case insensitively`() {
        val result = ArchiveIndex(snapshot).search("README")

        assertEquals(listOf("docs/readme.txt"), result.map(ArchiveNode::path))
    }

    @Test
    fun `resolves files directories and root without duplicates`() {
        val directory = ArchiveSelection.resolve(snapshot, listOf("docs"))
        assertEquals(listOf("docs", "docs/empty"), directory.directories)
        assertEquals(listOf("docs/readme.txt"), directory.files.map(ArchiveEntry::path))

        val overlapping = ArchiveSelection.resolve(
            snapshot,
            listOf("docs/readme.txt", "docs/empty", "docs"),
        )
        assertEquals(setOf("docs"), overlapping.requestedPaths)
        assertEquals(listOf("docs/readme.txt"), overlapping.files.map(ArchiveEntry::path))

        val all = ArchiveSelection.resolve(snapshot, listOf("docs", "root.txt", ""))
        assertEquals(setOf(""), all.requestedPaths)
        assertEquals(3, all.files.size)
        assertEquals(listOf("assets", "docs", "docs/empty"), all.directories)
    }

    @Test
    fun `resolves an explicitly selected empty directory`() {
        val emptyDirectory = ArchiveSelection.resolve(snapshot, listOf("docs/empty"))

        assertEquals(listOf("docs", "docs/empty"), emptyDirectory.directories)
        assertTrue(emptyDirectory.files.isEmpty())
        assertEquals(2, emptyDirectory.totalEntries)
    }

    @Test
    fun `rejects empty and unknown selections`() {
        expectArchiveFailure<ArchiveSelectionException>(ArchiveFailureCode.EMPTY_SELECTION) {
            ArchiveSelection.resolve(snapshot, emptyList())
        }
        expectArchiveFailure<ArchiveSelectionException>(ArchiveFailureCode.UNKNOWN_SELECTION) {
            ArchiveSelection.resolve(snapshot, listOf("missing"))
        }
    }

    @Test
    fun `rejects crafted snapshots that exceed the path node limit`() {
        val entry = ArchiveEntry(
            path = "one/two/file.bin",
            sourceName = "one/two/file.bin",
            displayName = "file.bin",
            isDirectory = false,
            compressionMethod = ArchiveCompressionMethod.STORED,
            compressedSize = 1L,
            uncompressedSize = 1L,
            crc32 = null,
            modifiedTimeMillis = null,
            ordinal = 0,
        )
        val craftedSnapshot = ArchiveSnapshot(
            sourceLength = 1L,
            sourceLastModifiedMillis = 0L,
            entries = listOf(entry),
            totalUncompressedBytes = 1L,
            limits = ArchiveSecurityLimits(
                maxEntries = 2,
                maxSingleUncompressedBytes = 1L,
                maxTotalUncompressedBytes = 1L,
            ),
        )

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED) {
            ArchiveIndex(craftedSnapshot)
        }
    }

    @Test
    fun `resolves twenty thousand individually selected files without pairwise matching`() {
        val entries = (0 until 20_000).map { ordinal ->
            val path = "file-${ordinal.toString().padStart(5, '0')}.bin"
            ArchiveEntry(
                path = path,
                sourceName = path,
                displayName = path.substringAfterLast('/'),
                isDirectory = false,
                compressionMethod = ArchiveCompressionMethod.STORED,
                compressedSize = 1L,
                uncompressedSize = 1L,
                crc32 = null,
                modifiedTimeMillis = null,
                ordinal = ordinal,
            )
        }
        val largeSnapshot = ArchiveSnapshot(
            sourceLength = 20_000L,
            sourceLastModifiedMillis = 0L,
            entries = entries,
            totalUncompressedBytes = 20_000L,
            limits = ArchiveSecurityLimits.DEFAULT,
        )

        val selection = ArchiveSelection.resolve(largeSnapshot, entries.map(ArchiveEntry::path))

        assertEquals(20_000, selection.requestedPaths.size)
        assertEquals(20_000, selection.files.size)
        assertTrue(selection.directories.isEmpty())
        assertEquals(20_000, selection.totalEntries)
    }
}
