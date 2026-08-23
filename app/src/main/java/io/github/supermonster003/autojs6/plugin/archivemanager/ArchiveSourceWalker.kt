package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.HashSet

internal data class ArchiveSourceEntry(
    val targetId: String,
    val relativePath: String,
    val archivePath: String,
    val kind: Int,
    val size: Long,
    val lastModified: Long,
) {
    val isDirectory: Boolean
        get() = kind == ExplorerActionValues.TARGET_DIRECTORY
}

/**
 * Traverses the host-owned source tree without following symbolic links.
 *
 * Archive writers share this implementation so pagination, portable paths, duplicate detection,
 * cancellation, and source-size verification cannot drift between formats.
 */
internal class ArchiveSourceWalker(
    private val session: ExplorerActionHostSessionClient,
) {

    fun walk(
        targets: List<ArchiveCompressionTarget>,
        checkCancelled: () -> Unit,
        visit: (ArchiveSourceEntry) -> Unit,
    ) {
        val queue = ArrayDeque<WorkItem>()
        targets.forEach { target ->
            queue.addLast(
                WorkItem.Source(
                    entry = ArchiveSourceEntry(
                        targetId = target.id,
                        relativePath = "",
                        archivePath = ArchiveCompressionPolicy.requirePortableEntrySegment(
                            target.displayName,
                        ),
                        kind = target.kind,
                        size = target.size,
                        lastModified = target.lastModified,
                    ),
                    readable = true,
                    symbolicLink = false,
                ),
            )
        }

        val visitedPaths = HashSet<String>()
        while (queue.isNotEmpty()) {
            checkCancelled()
            when (val item = queue.removeFirst()) {
                is WorkItem.Source -> {
                    val entry = item.entry
                    if (!item.readable) throw IOException("Source is not readable: ${entry.archivePath}")
                    if (item.symbolicLink) {
                        throw IOException("Symbolic links are not followed: ${entry.archivePath}")
                    }
                    if (!visitedPaths.add(entry.archivePath)) {
                        throw IOException("Duplicate archive source path: ${entry.archivePath}")
                    }
                    visit(entry)
                    if (entry.isDirectory) {
                        queue.addFirst(
                            WorkItem.DirectoryPage(
                                targetId = entry.targetId,
                                relativePath = entry.relativePath,
                                archivePath = entry.archivePath,
                                offset = 0,
                            ),
                        )
                    }
                }

                is WorkItem.DirectoryPage -> enqueueDirectoryPage(item, queue, checkCancelled)
            }
        }
    }

    fun measureFile(
        entry: ArchiveSourceEntry,
        checkCancelled: () -> Unit,
    ): Long {
        require(!entry.isDirectory) { "Directories do not have source data" }
        var total = 0L
        openFile(entry).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                checkCancelled()
                val count = input.read(buffer)
                if (count < 0) break
                total = Math.addExact(total, count.toLong())
            }
        }
        return total
    }

    fun copyFile(
        entry: ArchiveSourceEntry,
        output: OutputStream,
        expectedSize: Long?,
        checkCancelled: () -> Unit,
        onBytesWritten: (Int) -> Unit,
    ): Long {
        require(!entry.isDirectory) { "Directories do not have source data" }
        require(expectedSize == null || expectedSize >= 0L) { "Expected size cannot be negative" }
        var total = 0L
        openFile(entry).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                checkCancelled()
                val count = input.read(buffer)
                if (count < 0) break
                val nextTotal = Math.addExact(total, count.toLong())
                if (expectedSize != null && nextTotal > expectedSize) {
                    throw IOException("Source size changed while reading: ${entry.archivePath}")
                }
                output.write(buffer, 0, count)
                total = nextTotal
                onBytesWritten(count)
            }
        }
        if (expectedSize != null && total != expectedSize) {
            throw IOException("Source size changed while reading: ${entry.archivePath}")
        }
        return total
    }

    private fun enqueueDirectoryPage(
        item: WorkItem.DirectoryPage,
        queue: ArrayDeque<WorkItem>,
        checkCancelled: () -> Unit,
    ) {
        checkCancelled()
        val page = session.listChildren(item.targetId, item.relativePath, item.offset)
        if (!page.complete) {
            queue.addFirst(item.copy(offset = page.nextOffset))
        }
        page.items.asReversed().forEach { child ->
            val expectedRelativePath = if (item.relativePath.isEmpty()) {
                child.displayName
            } else {
                "${item.relativePath}/${child.displayName}"
            }
            if (child.relativePath != expectedRelativePath) {
                throw IOException("Host child path escapes the requested directory")
            }
            queue.addFirst(
                WorkItem.Source(
                    entry = ArchiveSourceEntry(
                        targetId = item.targetId,
                        relativePath = child.relativePath,
                        archivePath = ArchiveCompressionPolicy.joinEntryPath(
                            item.archivePath,
                            child.displayName,
                        ),
                        kind = child.kind,
                        size = child.size,
                        lastModified = child.lastModified,
                    ),
                    readable = child.readable,
                    symbolicLink = child.symbolicLink,
                ),
            )
        }
    }

    private fun openFile(entry: ArchiveSourceEntry) = BufferedInputStream(
        ParcelFileDescriptor.AutoCloseInputStream(
            session.openFile(entry.targetId, entry.relativePath),
        ),
        BUFFER_SIZE,
    )

    private sealed interface WorkItem {
        data class Source(
            val entry: ArchiveSourceEntry,
            val readable: Boolean,
            val symbolicLink: Boolean,
        ) : WorkItem

        data class DirectoryPage(
            val targetId: String,
            val relativePath: String,
            val archivePath: String,
            val offset: Int,
        ) : WorkItem
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1_024
    }
}
