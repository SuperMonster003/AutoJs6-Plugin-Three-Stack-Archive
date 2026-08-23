package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.tukaani.xz.LZMA2Options
import java.io.IOException
import java.util.Date

internal class SevenZArchiveCreator(
    remoteSession: IExplorerActionHostSession,
) : ArchiveWriter {
    override val format = ArchiveFormat.SEVEN_Z
    override val formatCapabilities = SevenZArchiveBackend.capabilities

    private val session = ExplorerActionHostSessionClient(remoteSession)
    private val sourceWalker = ArchiveSourceWalker(session)

    override fun create(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): ArchiveCreationResult {
        val outputName = ArchiveCompressionPolicy.normalizeOutputDisplayName(
            options.outputDisplayName,
            format,
        ) ?: throw IllegalArgumentException("7Z output name is invalid")
        val compressionLevel = ArchiveCompressionPolicy.requireCompressionLevel(
            options.compressionLevel,
            formatCapabilities.compressionLevels,
            format,
        )
        val password = options.password?.takeIf(CharArray::isNotEmpty)?.clone()
        val transaction = session.prepareOutput(outputName, format)
        try {
            val descriptor = session.openOutput(transaction.id)
            val counters = writeArchive(
                descriptor = descriptor,
                targets = request.targets,
                compressionLevel = compressionLevel,
                password = password,
                checkCancelled = checkCancelled,
                progress = progress,
            )
            checkCancelled()
            val committed = session.commitOutput(transaction.id, format)
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
        } finally {
            password?.fill('\u0000')
        }
    }

    private fun writeArchive(
        descriptor: ParcelFileDescriptor,
        targets: List<ArchiveCompressionTarget>,
        compressionLevel: Int,
        password: CharArray?,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): Counters {
        val counters = Counters()
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
                SevenZOutputFile(rawOutput.channel, password).use { archive ->
                    configureCompression(archive, compressionLevel)
                    sourceWalker.walk(targets, checkCancelled) { item ->
                        writeEntry(item, archive, counters, checkCancelled, progress)
                    }
                }
            }
        } catch (error: LinkageError) {
            throw IOException("7Z encoder is unavailable on this runtime", error)
        }
        return counters
    }

    private fun configureCompression(output: SevenZOutputFile, compressionLevel: Int) {
        if (compressionLevel == 0) {
            output.setContentCompression(SevenZMethod.COPY)
        } else {
            output.setContentMethods(
                listOf(
                    SevenZMethodConfiguration(
                        SevenZMethod.LZMA2,
                        LZMA2Options(compressionLevel),
                    ),
                ),
            )
        }
    }

    private fun writeEntry(
        item: ArchiveSourceEntry,
        output: SevenZOutputFile,
        counters: Counters,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ) {
        val entry = SevenZArchiveEntry().apply {
            name = item.archivePath.trimEnd('/')
            isDirectory = item.kind == ExplorerActionValues.TARGET_DIRECTORY
            if (item.lastModified > 0L) lastModifiedDate = Date(item.lastModified)
        }
        output.putArchiveEntry(entry)
        when (item.kind) {
            ExplorerActionValues.TARGET_DIRECTORY -> {
                output.closeArchiveEntry()
                counters.directories++
            }
            ExplorerActionValues.TARGET_FILE -> {
                sourceWalker.copyFile(
                    entry = item,
                    output = SevenZArchiveOutputStream(output),
                    expectedSize = item.size.takeIf { it >= 0L },
                    checkCancelled = checkCancelled,
                    onBytesWritten = { count ->
                        counters.bytesRead = Math.addExact(counters.bytesRead, count.toLong())
                    },
                )
                output.closeArchiveEntry()
                counters.files++
            }
            else -> error("Host returned an unsupported source kind")
        }
        progress.onProgress(
            ArchiveCreationProgress(
                currentEntry = item.archivePath,
                completedFiles = counters.files,
                completedDirectories = counters.directories,
                sourceBytesRead = counters.bytesRead,
            ),
        )
    }

    private class SevenZArchiveOutputStream(
        private val output: SevenZOutputFile,
    ) : java.io.OutputStream() {
        override fun write(value: Int) = output.write(value)

        override fun write(buffer: ByteArray, offset: Int, length: Int) =
            output.write(buffer, offset, length)

        override fun close() = Unit
    }

    private data class Counters(
        var files: Long = 0L,
        var directories: Long = 0L,
        var bytesRead: Long = 0L,
    )
}
