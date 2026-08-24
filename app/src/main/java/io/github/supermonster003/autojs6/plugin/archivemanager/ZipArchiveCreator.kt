package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedOutputStream

internal class ZipArchiveCreator(
    remoteSession: org.autojs.plugin.explorer.api.IExplorerActionHostSession,
) : ArchiveWriter {

    override val format = ArchiveFormat.ZIP
    override val formatCapabilities = ZipArchiveBackend.capabilities

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
        )
            ?: throw IllegalArgumentException("ZIP output name is invalid")
        val compressionLevel = ArchiveCompressionPolicy.requireCompressionLevel(
            options.compressionLevel,
            formatCapabilities.compressionLevels,
            format,
        )
        var passwordChars: CharArray? = null
        try {
            val completed = session.writeArchiveOutput(
                outputDisplayName = outputName,
                format = format,
                conflictPolicy = options.conflictPolicy,
            ) { descriptor ->
                passwordChars = options.password
                    ?.takeIf(CharArray::isNotEmpty)
                    ?.clone()
                writeArchive(
                    descriptor = descriptor,
                    targets = request.targets,
                    compressionLevel = compressionLevel,
                    password = passwordChars,
                    checkCancelled = checkCancelled,
                    progress = progress,
                ).also { checkCancelled() }
            }
            val counters = completed.value
            val committed = completed.transaction
            return ArchiveCreationResult(
                outputDisplayName = committed.displayName,
                outputDisplayPath = committed.displayPath,
                filesCompressed = counters.files,
                directoriesAdded = counters.directories,
                sourceBytesRead = counters.bytesRead,
            )
        } finally {
            passwordChars?.fill('\u0000')
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
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
            ZipOutputStream(BufferedOutputStream(rawOutput, BUFFER_SIZE), password).use { zipOutput ->
                sourceWalker.walk(targets, checkCancelled) { item ->
                    processSource(
                        item = item,
                        zipOutput = zipOutput,
                        counters = counters,
                        compressionLevel = compressionLevel,
                        encryptFiles = password != null,
                        checkCancelled = checkCancelled,
                        progress = progress,
                    )
                }
            }
        }
        return counters
    }

    private fun processSource(
        item: ArchiveSourceEntry,
        zipOutput: ZipOutputStream,
        counters: Counters,
        compressionLevel: Int,
        encryptFiles: Boolean,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ) {
        when (item.kind) {
            ExplorerActionValues.TARGET_DIRECTORY -> {
                val directoryEntryName = item.archivePath.trimEnd('/') + '/'
                zipOutput.putNextEntry(
                    zipParameters(
                        entryName = directoryEntryName,
                        lastModified = item.lastModified,
                        compressionLevel = compressionLevel,
                        encryptFiles = false,
                    ),
                )
                zipOutput.closeEntry()
                counters.directories++
                progress.report(item.archivePath, counters)
            }
            ExplorerActionValues.TARGET_FILE -> {
                zipOutput.putNextEntry(
                    zipParameters(
                        entryName = item.archivePath,
                        lastModified = item.lastModified,
                        compressionLevel = compressionLevel,
                        encryptFiles = encryptFiles,
                    ),
                )
                sourceWalker.copyFile(
                    entry = item,
                    output = zipOutput,
                    expectedSize = item.size.takeIf { it >= 0L },
                    checkCancelled = checkCancelled,
                    onBytesWritten = { count ->
                        counters.bytesRead = Math.addExact(counters.bytesRead, count.toLong())
                    },
                )
                zipOutput.closeEntry()
                counters.files++
                progress.report(item.archivePath, counters)
            }
            else -> error("Host returned an unsupported source kind")
        }
    }

    private fun zipParameters(
        entryName: String,
        lastModified: Long,
        compressionLevel: Int,
        encryptFiles: Boolean,
    ): ZipParameters = ZipParameters().apply {
        fileNameInZip = entryName
        compressionMethod = CompressionMethod.DEFLATE
        this.compressionLevel = CompressionLevel.values().first { it.level == compressionLevel }
        if (lastModified > 0L) lastModifiedFileTime = lastModified
        if (encryptFiles) {
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
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

    private data class Counters(
        var files: Long = 0,
        var directories: Long = 0,
        var bytesRead: Long = 0L,
    )

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
