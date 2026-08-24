package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedInputStream
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.HashSet
import java.util.concurrent.CancellationException

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
                    if (!item.readable) {
                        throw ArchiveCreationSourceException(
                            sourceArchivePath = entry.archivePath,
                            message = "Archive source is not readable: ${entry.archivePath}",
                        )
                    }
                    if (item.symbolicLink) {
                        throw ArchiveValidationException(
                            code = ArchiveFailureCode.UNSUPPORTED_METHOD,
                            message = "Symbolic links cannot be compressed: ${entry.archivePath}",
                            stage = ArchiveFailureStage.INPUT,
                        )
                    }
                    if (!visitedPaths.add(entry.archivePath)) {
                        throw ArchiveValidationException(
                            code = ArchiveFailureCode.DUPLICATE_PATH,
                            message = "Duplicate archive source path: ${entry.archivePath}",
                        )
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
        useFile(entry) { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                checkCancelled()
                val count = readFile(entry, input, buffer)
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
        useFile(entry) { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                checkCancelled()
                val count = readFile(entry, input, buffer)
                if (count < 0) break
                val nextTotal = Math.addExact(total, count.toLong())
                if (expectedSize != null && nextTotal > expectedSize) {
                    sourceChanged(entry)
                }
                output.write(buffer, 0, count)
                total = nextTotal
                onBytesWritten(count)
            }
        }
        if (expectedSize != null && total != expectedSize) {
            sourceChanged(entry)
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
                throw ArchiveValidationException(
                    code = ArchiveFailureCode.INVALID_PATH,
                    message = "Host child path escapes the requested directory",
                )
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

    private fun openFile(entry: ArchiveSourceEntry): BufferedInputStream = try {
        BufferedInputStream(
            ParcelFileDescriptor.AutoCloseInputStream(
                session.openFile(entry.targetId, entry.relativePath),
            ),
            BUFFER_SIZE,
        )
    } catch (error: Throwable) {
        throw mapSourceFailure(entry, "opened", error)
    }

    private fun readFile(
        entry: ArchiveSourceEntry,
        input: BufferedInputStream,
        buffer: ByteArray,
    ): Int = try {
        input.read(buffer)
    } catch (error: Throwable) {
        throw mapSourceFailure(entry, "read", error)
    }

    private inline fun <T> useFile(
        entry: ArchiveSourceEntry,
        block: (BufferedInputStream) -> T,
    ): T {
        val input = openFile(entry)
        var operationFailure: Throwable? = null
        try {
            return block(input)
        } catch (error: Throwable) {
            operationFailure = error
            throw error
        } finally {
            try {
                input.close()
            } catch (closeFailure: Throwable) {
                val mapped = mapSourceFailure(entry, "closed", closeFailure)
                if (operationFailure == null) {
                    throw mapped
                }
                if (mapped !== operationFailure) operationFailure.addSuppressed(mapped)
            }
        }
    }

    private fun mapSourceFailure(
        entry: ArchiveSourceEntry,
        operation: String,
        error: Throwable,
    ): Throwable = when (error) {
        is CancellationException,
        is ArchiveException,
        is Error,
        -> error
        else -> ArchiveCreationSourceException(
            sourceArchivePath = entry.archivePath,
            message = "Archive source could not be $operation: ${entry.archivePath}",
            cause = error,
        )
    }

    private fun sourceChanged(entry: ArchiveSourceEntry): Nothing =
        throw ArchiveCreationSourceException(
            sourceArchivePath = entry.archivePath,
            message = "Archive source size changed while reading: ${entry.archivePath}",
            code = ArchiveFailureCode.SOURCE_CHANGED,
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
