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
            checkCancelled()
            val completed = session.writeArchiveOutput(
                outputDisplayName = outputName,
                format = format,
                conflictPolicy = options.conflictPolicy,
                prepareAfterReservation = {
                    progress.reportScanStarted()
                    sourceWalker.scan(request.targets, checkCancelled, progress::reportScan)
                },
                write = { descriptor, manifest ->
                    passwordChars = options.password
                        ?.takeIf(CharArray::isNotEmpty)
                        ?.clone()
                    writeArchive(
                        descriptor = descriptor,
                        manifest = manifest,
                        compressionLevel = compressionLevel,
                        password = passwordChars,
                        checkCancelled = checkCancelled,
                        progress = progress,
                    ).also { checkCancelled() }
                },
                beforeCommit = { manifest, counters ->
                    checkCancelled()
                    progress.reportCommitting(manifest, counters)
                    checkCancelled()
                },
            )
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
        manifest: ArchiveSourceManifest,
        compressionLevel: Int,
        password: CharArray?,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): ArchiveCreationCounters {
        val counters = ArchiveCreationCounters()
        progress.reportCompression(manifest, counters, currentEntry = null)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
            ZipOutputStream(BufferedOutputStream(rawOutput, BUFFER_SIZE), password).use { zipOutput ->
                manifest.entries.forEach { item ->
                    checkCancelled()
                    progress.reportCompression(manifest, counters, item.archivePath)
                    processSource(
                        item = item,
                        manifest = manifest,
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
        manifest: ArchiveSourceManifest,
        zipOutput: ZipOutputStream,
        counters: ArchiveCreationCounters,
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
                progress.reportCompression(manifest, counters, item.archivePath)
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
                        progress.reportCompression(manifest, counters, item.archivePath)
                    },
                )
                zipOutput.closeEntry()
                counters.files++
                progress.reportCompression(manifest, counters, item.archivePath)
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

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
