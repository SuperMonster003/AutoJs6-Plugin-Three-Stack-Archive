package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.AndroidSevenZEncryption
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.tukaani.xz.LZMA2Options
import java.io.File
import java.io.IOException
import java.util.Date

internal class SevenZArchiveCreator(
    remoteSession: IExplorerActionHostSession,
    cacheDirectory: File?,
) : ArchiveWriter {
    override val format = ArchiveFormat.SEVEN_Z
    override val formatCapabilities = SevenZArchiveBackend.capabilities

    private val session = ExplorerActionHostSessionClient(remoteSession)
    private val sourceWalker = ArchiveSourceWalker(session)
    private val verifier = CreatedArchiveVerifier(
        requireNotNull(cacheDirectory) {
            "7Z creation requires a private cache directory for output verification"
        },
    )

    override fun create(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
        outputCommitter: ArchiveOutputCommitter,
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
        var password: CharArray? = null
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
                    password = options.password?.takeIf(CharArray::isNotEmpty)?.clone()
                    writeArchive(
                        descriptor = descriptor,
                        manifest = manifest,
                        compressionLevel = compressionLevel,
                        password = password,
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
                        password = password,
                        checkCancelled = checkCancelled,
                    )
                },
                beforeCommit = { manifest, counters ->
                    checkCancelled()
                    progress.reportCommitting(manifest, counters)
                    checkCancelled()
                },
                outputCommitter = outputCommitter,
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
            password?.fill('\u0000')
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
        val counters = ArchiveCreationCounters(manifest.entries.size)
        progress.reportCompression(manifest, counters, currentEntry = null)
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
                SevenZOutputFile(rawOutput.channel).use { archive ->
                    configureCompression(archive, compressionLevel, password)
                    manifest.entries.forEachIndexed { entryIndex, item ->
                        checkCancelled()
                        progress.reportCompression(manifest, counters, item.archivePath)
                        writeEntry(
                            entryIndex,
                            item,
                            manifest,
                            archive,
                            counters,
                            checkCancelled,
                            progress,
                        )
                    }
                }
            }
        } catch (error: LinkageError) {
            throw IOException("7Z encoder is unavailable on this runtime", error)
        }
        return counters
    }

    private fun configureCompression(
        output: SevenZOutputFile,
        compressionLevel: Int,
        password: CharArray?,
    ) {
        val compression = if (compressionLevel == 0) {
            SevenZMethodConfiguration(SevenZMethod.COPY)
        } else {
            SevenZMethodConfiguration(
                SevenZMethod.LZMA2,
                LZMA2Options(compressionLevel),
            )
        }
        if (password == null) {
            output.setContentMethods(listOf(compression))
        } else {
            AndroidSevenZEncryption.configure(output, password, compression)
        }
    }

    private fun writeEntry(
        entryIndex: Int,
        item: ArchiveSourceEntry,
        manifest: ArchiveSourceManifest,
        output: SevenZOutputFile,
        counters: ArchiveCreationCounters,
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
                val fingerprint = sourceWalker.copyFileWithFingerprint(
                    entry = item,
                    output = SevenZArchiveOutputStream(output),
                    expectedSize = item.size.takeIf { it >= 0L },
                    checkCancelled = checkCancelled,
                    onBytesWritten = { count ->
                        counters.bytesRead = Math.addExact(counters.bytesRead, count.toLong())
                        progress.reportCompression(manifest, counters, item.archivePath)
                    },
                )
                counters.recordSourceFingerprint(entryIndex, fingerprint)
                output.closeArchiveEntry()
                counters.files++
            }
            else -> error("Host returned an unsupported source kind")
        }
        progress.reportCompression(manifest, counters, item.archivePath)
    }

    private class SevenZArchiveOutputStream(
        private val output: SevenZOutputFile,
    ) : java.io.OutputStream() {
        override fun write(value: Int) = output.write(value)

        override fun write(buffer: ByteArray, offset: Int, length: Int) =
            output.write(buffer, offset, length)

        override fun close() = Unit
    }

}
