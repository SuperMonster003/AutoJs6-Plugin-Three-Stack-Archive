@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import com.github.luben.zstd.ZstdOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

internal class TarArchiveCreator(
    override val format: ArchiveFormat,
    override val formatCapabilities: FormatCapabilities,
    remoteSession: IExplorerActionHostSession,
    cacheDirectory: File?,
) : ArchiveWriter {

    init {
        require(format.isTarFamily)
        require(formatCapabilities.canCreate)
    }

    private val session = ExplorerActionHostSessionClient(remoteSession)
    private val sourceWalker = ArchiveSourceWalker(session)
    private val verifier = CreatedArchiveVerifier(
        requireNotNull(cacheDirectory) {
            "${format.displayName} creation requires a private cache directory for output verification"
        },
    )

    override fun create(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): ArchiveCreationResult {
        val outputName = ArchiveCompressionPolicy.normalizeOutputDisplayName(
            options.outputDisplayName,
            format,
        ) ?: throw IllegalArgumentException("${format.displayName} output name is invalid")
        val compressionLevel = ArchiveCompressionPolicy.requireCompressionLevel(
            options.compressionLevel,
            formatCapabilities.compressionLevels,
            format,
        )
        require(options.password == null || options.password.isEmpty()) {
            "${format.displayName} does not support password encryption"
        }

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
                writeArchive(
                    descriptor = descriptor,
                    manifest = manifest,
                    compressionLevel = compressionLevel,
                    checkCancelled = checkCancelled,
                    progress = progress,
                ).also { checkCancelled() }
            },
            verify = { descriptor, manifest, counters ->
                checkCancelled()
                progress.reportVerifying(manifest, counters)
                verifier.verifyPendingOutput(
                    descriptor = descriptor,
                    format = format,
                    manifest = manifest,
                    counters = counters,
                    password = null,
                    checkCancelled = checkCancelled,
                )
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
    }

    private fun writeArchive(
        descriptor: ParcelFileDescriptor,
        manifest: ArchiveSourceManifest,
        compressionLevel: Int,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ): ArchiveCreationCounters {
        val counters = ArchiveCreationCounters(manifest.entries.size)
        progress.reportCompression(manifest, counters, currentEntry = null)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
            BufferedOutputStream(rawOutput, BUFFER_SIZE).use { bufferedOutput ->
                val containerOutput = compressedOutput(bufferedOutput, compressionLevel)
                TarArchiveOutputStream(containerOutput, StandardCharsets.UTF_8.name()).use { tarOutput ->
                    tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                    tarOutput.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
                    tarOutput.setAddPaxHeadersForNonAsciiNames(true)
                    manifest.entries.forEachIndexed { entryIndex, item ->
                        checkCancelled()
                        progress.reportCompression(manifest, counters, item.archivePath)
                        writeEntry(
                            entryIndex,
                            item,
                            manifest,
                            tarOutput,
                            counters,
                            checkCancelled,
                            progress,
                        )
                    }
                }
            }
        }
        return counters
    }

    private fun writeEntry(
        entryIndex: Int,
        item: ArchiveSourceEntry,
        manifest: ArchiveSourceManifest,
        output: TarArchiveOutputStream,
        counters: ArchiveCreationCounters,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
    ) {
        when (item.kind) {
            ExplorerActionValues.TARGET_DIRECTORY -> {
                val name = item.archivePath.trimEnd('/') + '/'
                val entry = archiveEntry(
                    name = name,
                    typeFlag = TarConstants.LF_DIR,
                    size = 0L,
                    lastModified = item.lastModified,
                    mode = TarArchiveEntry.DEFAULT_DIR_MODE,
                )
                output.putArchiveEntry(entry)
                output.closeArchiveEntry()
                counters.directories++
                progress.reportCompression(manifest, counters, item.archivePath)
            }

            ExplorerActionValues.TARGET_FILE -> {
                val size = if (item.size >= 0L) {
                    item.size
                } else {
                    sourceWalker.measureFile(item, checkCancelled)
                }
                val entry = archiveEntry(
                    name = item.archivePath,
                    typeFlag = TarConstants.LF_NORMAL,
                    size = size,
                    lastModified = item.lastModified,
                    mode = TarArchiveEntry.DEFAULT_FILE_MODE,
                )
                output.putArchiveEntry(entry)
                val fingerprint = sourceWalker.copyFileWithFingerprint(
                    entry = item,
                    output = output,
                    expectedSize = size,
                    checkCancelled = checkCancelled,
                    onBytesWritten = { count ->
                        counters.bytesRead = Math.addExact(counters.bytesRead, count.toLong())
                        progress.reportCompression(manifest, counters, item.archivePath)
                    },
                )
                counters.recordSourceFingerprint(entryIndex, fingerprint)
                output.closeArchiveEntry()
                counters.files++
                progress.reportCompression(manifest, counters, item.archivePath)
            }

            else -> error("Host returned an unsupported source kind")
        }
    }

    private fun archiveEntry(
        name: String,
        typeFlag: Byte,
        size: Long,
        lastModified: Long,
        mode: Int,
    ) = TarArchiveEntry(name, typeFlag).apply {
        setSize(size)
        setMode(mode)
        setModTime(lastModified.coerceAtLeast(0L))
    }

    private fun compressedOutput(
        output: OutputStream,
        compressionLevel: Int,
    ): OutputStream = try {
        when (format) {
            ArchiveFormat.TAR -> output
            ArchiveFormat.TAR_GZIP -> GzipCompressorOutputStream(
                output,
                GzipParameters().apply {
                    setCompressionLevel(compressionLevel)
                    setModificationTime(0L)
                    setOperatingSystem(GZIP_OS_UNKNOWN)
                },
            )
            ArchiveFormat.TAR_XZ -> XZCompressorOutputStream(output, compressionLevel)
            ArchiveFormat.TAR_BZIP2 -> BZip2CompressorOutputStream(output, compressionLevel)
            ArchiveFormat.TAR_ZSTD -> ZstdOutputStream(output, compressionLevel).setChecksum(true)
            ArchiveFormat.ZIP,
            ArchiveFormat.SEVEN_Z,
            ArchiveFormat.RAR,
            -> error("${format.displayName} does not use the TAR writer")
        }
    } catch (error: LinkageError) {
        throw IOException("${format.displayName} encoder is unavailable on this runtime", error)
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1_024
        const val GZIP_OS_UNKNOWN = 255
    }
}
