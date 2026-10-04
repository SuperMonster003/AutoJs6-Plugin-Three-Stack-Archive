@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveSourceManifestInstrumentationTest {

    @Test
    fun scanBuildsAPagedDepthFirstImmutableManifestWithStableStatistics() {
        val remote = PagedTreeHostSession()
        val walker = ArchiveSourceWalker(ExplorerActionHostSessionClient(remote))
        val updates = mutableListOf<ArchiveSourceScanProgress>()

        val manifest = walker.scan(
            targets = listOf(directoryTarget()),
            checkCancelled = {},
            onProgress = updates::add,
        )

        assertEquals(
            listOf(
                "folder",
                "folder/alpha.txt",
                "folder/nested",
                "folder/nested/child.bin",
                "folder/mystery.bin",
            ),
            manifest.entries.map(ArchiveSourceEntry::archivePath),
        )
        assertEquals(listOf("@0", "@1", "nested@0", "@2"), remote.pages)
        assertEquals(3L, manifest.fileCount)
        assertEquals(2L, manifest.directoryCount)
        assertEquals(10L, manifest.knownSourceBytes)
        assertEquals(1L, manifest.unknownSizeFileCount)
        assertEquals(
            ArchiveSourceScanProgress(
                currentEntry = "folder/mystery.bin",
                discoveredFiles = 3L,
                discoveredDirectories = 2L,
                knownSourceBytes = 10L,
                unknownSizeFiles = 1L,
            ),
            updates.last(),
        )

        @Suppress("UNCHECKED_CAST")
        val mutableView = manifest.entries as MutableList<ArchiveSourceEntry>
        assertThrows(UnsupportedOperationException::class.java) { mutableView.clear() }
        assertEquals(5, manifest.entries.size)
    }

    @Test
    fun scanRejectsEntriesPastItsHardBoundBeforeGrowingTheManifest() {
        val remote = PagedTreeHostSession()
        val walker = ArchiveSourceWalker(
            session = ExplorerActionHostSessionClient(remote),
            maxEntries = 3,
        )
        val updates = mutableListOf<ArchiveSourceScanProgress>()

        val error = assertThrows(ArchiveValidationException::class.java) {
            walker.scan(
                targets = listOf(directoryTarget()),
                checkCancelled = {},
                onProgress = updates::add,
            )
        }

        assertEquals(ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED, error.code)
        assertEquals(ArchiveFailureStage.INPUT, error.stage)
        assertEquals(3, updates.size)
        assertEquals("folder/nested", updates.last().currentEntry)
        assertEquals(listOf("@0", "@1", "nested@0"), remote.pages)
    }

    @Test
    fun scanRejectsOverflowingKnownSourceTotalsAsAnInputFailure() {
        val walker = ArchiveSourceWalker(
            ExplorerActionHostSessionClient(PagedTreeHostSession()),
        )

        val error = assertThrows(ArchiveValidationException::class.java) {
            walker.scan(
                targets = listOf(
                    fileTarget("first", "first.bin", Long.MAX_VALUE),
                    fileTarget("second", "second.bin", 1L),
                ),
                checkCancelled = {},
                onProgress = {},
            )
        }

        assertEquals(ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED, error.code)
        assertEquals(ArchiveFailureStage.INPUT, error.stage)
    }

    @Test
    fun scanBoundsAggregateManifestPathMemory() {
        val walker = ArchiveSourceWalker(
            session = ExplorerActionHostSessionClient(PagedTreeHostSession()),
            maxPathCharacters = 20L,
        )
        val updates = mutableListOf<ArchiveSourceScanProgress>()

        val error = assertThrows(ArchiveValidationException::class.java) {
            walker.scan(
                targets = listOf(directoryTarget()),
                checkCancelled = {},
                onProgress = updates::add,
            )
        }

        assertEquals(ArchiveFailureCode.PATH_LIMIT_EXCEEDED, error.code)
        assertEquals(ArchiveFailureStage.INPUT, error.stage)
        assertEquals(listOf("folder"), updates.map(ArchiveSourceScanProgress::currentEntry))
    }

    @Test
    fun scanRejectsPathsPastItsHardDepthBound() {
        val walker = ArchiveSourceWalker(
            session = ExplorerActionHostSessionClient(PagedTreeHostSession()),
            maxDepth = 2,
        )
        val updates = mutableListOf<ArchiveSourceScanProgress>()

        val error = assertThrows(ArchiveValidationException::class.java) {
            walker.scan(
                targets = listOf(directoryTarget()),
                checkCancelled = {},
                onProgress = updates::add,
            )
        }

        assertEquals(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED, error.code)
        assertEquals(ArchiveFailureStage.INPUT, error.stage)
        assertEquals(
            listOf("folder", "folder/alpha.txt", "folder/nested"),
            updates.map(ArchiveSourceScanProgress::currentEntry),
        )
    }

    private fun directoryTarget() = ArchiveCompressionTarget(
        id = "folder",
        uri = Uri.parse("content://host/folder"),
        displayName = "folder",
        kind = ExplorerActionValues.TARGET_DIRECTORY,
        mimeType = "inode/directory",
        size = -1L,
        lastModified = 1_700_000_000_000L,
    )

    private fun fileTarget(id: String, name: String, size: Long) = ArchiveCompressionTarget(
        id = id,
        uri = Uri.parse("content://host/$id"),
        displayName = name,
        kind = ExplorerActionValues.TARGET_FILE,
        mimeType = "application/octet-stream",
        size = size,
        lastModified = 1_700_000_000_000L,
    )

    private class PagedTreeHostSession : TestExplorerActionHostSession() {
        val pages = mutableListOf<String>()

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle {
            check(targetId == "folder")
            pages += "$relativePath@$offset"
            val children = when (relativePath) {
                "" -> listOf(
                    entry("alpha.txt", ExplorerActionValues.TARGET_FILE, 4L),
                    entry("nested", ExplorerActionValues.TARGET_DIRECTORY, -1L),
                    entry("mystery.bin", ExplorerActionValues.TARGET_FILE, -1L),
                )
                "nested" -> listOf(
                    entry("nested/child.bin", ExplorerActionValues.TARGET_FILE, 6L),
                )
                else -> error("Unexpected directory: $relativePath")
            }
            val page = children.drop(offset).take(minOf(limit, 1))
            return Bundle().apply {
                putParcelableArrayList(ExplorerActionHostSessionKeys.ITEMS, ArrayList(page))
                putInt(ExplorerActionHostSessionKeys.NEXT_OFFSET, offset + page.size)
                putBoolean(ExplorerActionHostSessionKeys.COMPLETE, offset + page.size >= children.size)
            }
        }

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor =
            error("Manifest scanning must not open source data")

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle = error("Manifest scanning must not prepare output")

        override fun openOutput(transactionId: String): ParcelFileDescriptor =
            error("Manifest scanning must not open output")

        override fun openPendingOutput(transactionId: String): ParcelFileDescriptor =
            error("Manifest scanning must not verify output")

        override fun commitOutput(transactionId: String): Bundle =
            error("Manifest scanning must not commit output")

        override fun abortOutput(transactionId: String) = Unit

        override fun close() = Unit

        private fun entry(relativePath: String, kind: Int, size: Long) = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.RELATIVE_PATH, relativePath)
            putString(
                ExplorerActionHostSessionKeys.DISPLAY_NAME,
                relativePath.substringAfterLast('/'),
            )
            putInt(ExplorerActionHostSessionKeys.KIND, kind)
            putString(
                ExplorerActionHostSessionKeys.MIME_TYPE,
                if (kind == ExplorerActionValues.TARGET_DIRECTORY) {
                    "inode/directory"
                } else {
                    "application/octet-stream"
                },
            )
            putLong(ExplorerActionHostSessionKeys.SIZE, size)
            putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, 1_700_000_000_000L)
            putBoolean(ExplorerActionHostSessionKeys.READABLE, true)
            putBoolean(ExplorerActionHostSessionKeys.SYMBOLIC_LINK, false)
        }
    }
}
