package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.ArrayDeque
import java.util.HashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class ZipArchiveCreator(
    remoteSession: org.autojs.plugin.explorer.api.IExplorerActionHostSession,
) : ArchiveWriter {

    override val format = ArchiveFormat.ZIP
    override val formatCapabilities = ZipArchiveBackend.capabilities

    private val session = ExplorerActionHostSessionClient(remoteSession)

    override fun create(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): ArchiveCreationResult {
        val outputName = ArchiveCompressionPolicy.normalizeOutputDisplayName(options.outputDisplayName)
            ?: throw IllegalArgumentException("ZIP output name is invalid")
        val compressionLevel = ArchiveCompressionPolicy.requireCompressionLevel(options.compressionLevel)
        val transaction = session.prepareOutput(outputName)
        try {
            val descriptor = session.openOutput(transaction.id)
            val counters = writeArchive(
                descriptor = descriptor,
                targets = request.targets,
                compressionLevel = compressionLevel,
                checkCancelled = checkCancelled,
                progress = progress,
            )
            checkCancelled()
            val committed = session.commitOutput(transaction.id)
            return ArchiveCreationResult(
                outputDisplayName = committed.displayName,
                outputDisplayPath = committed.displayPath,
                filesCompressed = counters.files,
                directoriesAdded = counters.directories,
                sourceBytesRead = counters.bytesRead,
            )
        } catch (error: Throwable) {
            runCatching { session.abortOutput(transaction.id) }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            throw error
        }
    }

    private fun writeArchive(
        descriptor: ParcelFileDescriptor,
        targets: List<ArchiveCompressionTarget>,
        compressionLevel: Int,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): Counters {
        val queue = ArrayDeque<WorkItem>()
        targets.forEach { target ->
            val rootEntryPath = ArchiveCompressionPolicy.requirePortableEntrySegment(target.displayName)
            queue.addLast(
                WorkItem.Source(
                    target = target,
                    relativePath = "",
                    entryPath = rootEntryPath,
                    kind = target.kind,
                    lastModified = target.lastModified,
                    readable = true,
                    symbolicLink = false,
                ),
            )
        }
        val writtenNames = HashSet<String>()
        val counters = Counters()
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
            ZipOutputStream(BufferedOutputStream(rawOutput, BUFFER_SIZE)).use { zipOutput ->
                zipOutput.setLevel(compressionLevel)
                while (queue.isNotEmpty()) {
                    checkCancelled()
                    when (val item = queue.removeFirst()) {
                        is WorkItem.Source -> processSource(
                            item = item,
                            queue = queue,
                            zipOutput = zipOutput,
                            writtenNames = writtenNames,
                            counters = counters,
                            checkCancelled = checkCancelled,
                            progress = progress,
                        )
                        is WorkItem.DirectoryPage -> processDirectoryPage(
                            item = item,
                            queue = queue,
                            checkCancelled = checkCancelled,
                        )
                    }
                }
            }
        }
        return counters
    }

    private fun processSource(
        item: WorkItem.Source,
        queue: ArrayDeque<WorkItem>,
        zipOutput: ZipOutputStream,
        writtenNames: MutableSet<String>,
        counters: Counters,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ) {
        require(item.readable) { "Source is not readable: ${item.entryPath}" }
        require(!item.symbolicLink) { "Symbolic links are not followed: ${item.entryPath}" }
        when (item.kind) {
            ExplorerActionValues.TARGET_DIRECTORY -> {
                val directoryEntryName = item.entryPath.trimEnd('/') + '/'
                require(writtenNames.add(directoryEntryName)) { "Duplicate ZIP entry: $directoryEntryName" }
                val entry = ZipEntry(directoryEntryName)
                if (item.lastModified > 0L) entry.time = item.lastModified
                zipOutput.putNextEntry(entry)
                zipOutput.closeEntry()
                counters.directories++
                progress.report(item.entryPath, counters)
                queue.addFirst(
                    WorkItem.DirectoryPage(
                        target = item.target,
                        relativePath = item.relativePath,
                        entryPath = item.entryPath,
                        offset = 0,
                    ),
                )
            }
            ExplorerActionValues.TARGET_FILE -> {
                require(writtenNames.add(item.entryPath)) { "Duplicate ZIP entry: ${item.entryPath}" }
                val entry = ZipEntry(item.entryPath)
                if (item.lastModified > 0L) entry.time = item.lastModified
                zipOutput.putNextEntry(entry)
                val sourceDescriptor = session.openFile(item.target.id, item.relativePath)
                ParcelFileDescriptor.AutoCloseInputStream(sourceDescriptor).use { rawInput ->
                    BufferedInputStream(rawInput, BUFFER_SIZE).use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            checkCancelled()
                            val count = input.read(buffer)
                            if (count < 0) break
                            zipOutput.write(buffer, 0, count)
                            counters.bytesRead = Math.addExact(counters.bytesRead, count.toLong())
                        }
                    }
                }
                zipOutput.closeEntry()
                counters.files++
                progress.report(item.entryPath, counters)
            }
            else -> error("Host returned an unsupported source kind")
        }
    }

    private fun processDirectoryPage(
        item: WorkItem.DirectoryPage,
        queue: ArrayDeque<WorkItem>,
        checkCancelled: () -> Unit,
    ) {
        checkCancelled()
        val page = session.listChildren(item.target.id, item.relativePath, item.offset)
        if (!page.complete) {
            queue.addFirst(item.copy(offset = page.nextOffset))
        }
        page.items.asReversed().forEach { child ->
            queue.addFirst(
                WorkItem.Source(
                    target = item.target,
                    relativePath = child.relativePath,
                    entryPath = ArchiveCompressionPolicy.joinEntryPath(item.entryPath, child.displayName),
                    kind = child.kind,
                    lastModified = child.lastModified,
                    readable = child.readable,
                    symbolicLink = child.symbolicLink,
                ),
            )
        }
    }

    private fun ArchiveCreationProgressListener.report(entryPath: String, counters: Counters) {
        onProgress(
            ArchiveCreationProgress(
                currentEntry = entryPath,
                completedFiles = counters.files,
                completedDirectories = counters.directories,
                sourceBytesRead = counters.bytesRead,
            ),
        )
    }

    private sealed interface WorkItem {
        data class Source(
            val target: ArchiveCompressionTarget,
            val relativePath: String,
            val entryPath: String,
            val kind: Int,
            val lastModified: Long,
            val readable: Boolean,
            val symbolicLink: Boolean,
        ) : WorkItem

        data class DirectoryPage(
            val target: ArchiveCompressionTarget,
            val relativePath: String,
            val entryPath: String,
            val offset: Int,
        ) : WorkItem
    }

    private data class Counters(
        var files: Long = 0,
        var directories: Long = 0,
        var bytesRead: Long = 0L,
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
