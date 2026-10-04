package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedInputStream
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Collections
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

/** A bounded, immutable snapshot of the host source tree used by one creation transaction. */
internal class ArchiveSourceManifest(
    entries: List<ArchiveSourceEntry>,
    val fileCount: Long,
    val directoryCount: Long,
    val knownSourceBytes: Long,
    val unknownSizeFileCount: Long,
) {
    val entries: List<ArchiveSourceEntry> = Collections.unmodifiableList(ArrayList(entries))

    init {
        require(fileCount >= 0L)
        require(directoryCount >= 0L)
        require(knownSourceBytes >= 0L)
        require(unknownSizeFileCount in 0L..fileCount)
        require(entries.size.toLong() == fileCount + directoryCount)
    }
}

internal data class ArchiveSourceScanProgress(
    val currentEntry: String,
    val discoveredFiles: Long,
    val discoveredDirectories: Long,
    val knownSourceBytes: Long,
    val unknownSizeFiles: Long,
)

/**
 * Traverses the host-owned source tree without following symbolic links.
 *
 * Archive writers share this implementation so pagination, portable paths, duplicate detection,
 * cancellation, and source-size verification cannot drift between formats.
 */
internal class ArchiveSourceWalker(
    private val session: ExplorerActionHostSessionClient,
    private val maxEntries: Int = ArchiveStructureLimits.DEFAULT.maxEntries,
    private val maxDepth: Int = ArchiveStructureLimits.DEFAULT.maxDepth,
    private val maxPathCharacters: Long = MAX_MANIFEST_PATH_CHARACTERS,
) {

    init {
        require(maxEntries in 1..ArchiveStructureLimits.DEFAULT.maxEntries)
        require(maxDepth in 1..ArchiveStructureLimits.DEFAULT.maxDepth)
        require(maxPathCharacters in 1L..MAX_MANIFEST_PATH_CHARACTERS)
    }

    fun scan(
        targets: List<ArchiveCompressionTarget>,
        checkCancelled: () -> Unit,
        onProgress: (ArchiveSourceScanProgress) -> Unit,
    ): ArchiveSourceManifest {
        val queue = ArrayDeque<WorkItem>()
        targets.forEach { target ->
            queue.addLast(
                WorkItem.Source(
                    entry = ArchiveSourceEntry(
                        targetId = target.id,
                        relativePath = "",
                        archivePath = portableEntrySegment(target.displayName),
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
        val entries = ArrayList<ArchiveSourceEntry>()
        var files = 0L
        var directories = 0L
        var knownSourceBytes = 0L
        var unknownSizeFiles = 0L
        var pathCharacters = 0L
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
                            stage = ArchiveFailureStage.INPUT,
                        )
                    }
                    if (entries.size >= maxEntries) {
                        throw ArchiveValidationException(
                            code = ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                            message = "Archive source contains more than $maxEntries entries",
                            stage = ArchiveFailureStage.INPUT,
                        )
                    }
                    requireSupportedDepth(entry.archivePath)
                    pathCharacters = addPathCharacters(pathCharacters, entry.archivePath.length)
                    entries += entry
                    if (entry.isDirectory) {
                        directories++
                    } else {
                        files++
                        if (entry.size >= 0L) {
                            knownSourceBytes = addKnownSourceBytes(knownSourceBytes, entry.size)
                        } else {
                            unknownSizeFiles++
                        }
                    }
                    onProgress(
                        ArchiveSourceScanProgress(
                            currentEntry = entry.archivePath,
                            discoveredFiles = files,
                            discoveredDirectories = directories,
                            knownSourceBytes = knownSourceBytes,
                            unknownSizeFiles = unknownSizeFiles,
                        ),
                    )
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
        return ArchiveSourceManifest(
            entries = entries,
            fileCount = files,
            directoryCount = directories,
            knownSourceBytes = knownSourceBytes,
            unknownSizeFileCount = unknownSizeFiles,
        )
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

    fun copyFileWithFingerprint(
        entry: ArchiveSourceEntry,
        output: OutputStream,
        expectedSize: Long?,
        checkCancelled: () -> Unit,
        onBytesWritten: (Int) -> Unit,
    ): ArchiveCreationSourceFingerprint {
        val digest = MessageDigest.getInstance(SHA_256)
        val bytes = copyFile(
            entry = entry,
            output = DigestOutputStream(output, digest),
            expectedSize = expectedSize,
            checkCancelled = checkCancelled,
            onBytesWritten = onBytesWritten,
        )
        return ArchiveCreationSourceFingerprint(bytes, digest.digest())
    }

    private fun enqueueDirectoryPage(
        item: WorkItem.DirectoryPage,
        queue: ArrayDeque<WorkItem>,
        checkCancelled: () -> Unit,
    ) {
        checkCancelled()
        val page = try {
            session.listChildren(item.targetId, item.relativePath, item.offset)
        } catch (error: Throwable) {
            throw mapSourceMetadataFailure(item.archivePath, error)
        }
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
                    stage = ArchiveFailureStage.INPUT,
                )
            }
            val archivePath = portableEntryPath(item.archivePath, child.displayName)
            requireSupportedDepth(archivePath)
            queue.addFirst(
                WorkItem.Source(
                    entry = ArchiveSourceEntry(
                        targetId = item.targetId,
                        relativePath = child.relativePath,
                        archivePath = archivePath,
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

    private fun portableEntrySegment(displayName: String): String = try {
        ArchiveCompressionPolicy.requirePortableEntrySegment(displayName)
    } catch (error: IllegalArgumentException) {
        throw ArchiveValidationException(
            code = ArchiveFailureCode.INVALID_PATH,
            message = "Archive source name cannot be represented as a portable entry path",
            cause = error,
            stage = ArchiveFailureStage.INPUT,
        )
    }

    private fun portableEntryPath(parent: String, displayName: String): String = try {
        ArchiveCompressionPolicy.joinEntryPath(parent, displayName)
    } catch (error: IllegalArgumentException) {
        val code = if (error.message == "Archive entry path is too long") {
            ArchiveFailureCode.PATH_LIMIT_EXCEEDED
        } else {
            ArchiveFailureCode.INVALID_PATH
        }
        throw ArchiveValidationException(
            code = code,
            message = "Archive source path cannot be represented safely",
            cause = error,
            stage = ArchiveFailureStage.INPUT,
        )
    }

    private fun requireSupportedDepth(archivePath: String) {
        val depth = archivePath.count { it == '/' } + 1
        if (depth > maxDepth) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
                message = "Archive source exceeds the supported path depth",
                stage = ArchiveFailureStage.INPUT,
            )
        }
    }

    private fun addKnownSourceBytes(current: Long, size: Long): Long = try {
        Math.addExact(current, size)
    } catch (error: ArithmeticException) {
        throw ArchiveValidationException(
            code = ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
            message = "Archive source size exceeds the supported range",
            cause = error,
            stage = ArchiveFailureStage.INPUT,
        )
    }

    private fun addPathCharacters(current: Long, pathLength: Int): Long {
        val total = Math.addExact(current, pathLength.toLong())
        if (total > maxPathCharacters) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
                message = "Archive source paths exceed the supported in-memory range",
                stage = ArchiveFailureStage.INPUT,
            )
        }
        return total
    }

    private fun mapSourceMetadataFailure(archivePath: String, error: Throwable): Throwable =
        when (error) {
            is CancellationException,
            is ArchiveException,
            is Error,
            -> error
            else -> ArchiveCreationSourceException(
                sourceArchivePath = archivePath,
                message = "Archive source directory could not be read: $archivePath",
                cause = error,
            )
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
        const val SHA_256 = "SHA-256"
        const val MAX_MANIFEST_PATH_CHARACTERS = 16L * 1_024L * 1_024L
    }
}
