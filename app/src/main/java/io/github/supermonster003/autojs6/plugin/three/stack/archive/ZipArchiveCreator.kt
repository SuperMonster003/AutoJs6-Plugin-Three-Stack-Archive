package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.ParcelFileDescriptor
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.autojs.plugin.explorer.api.ExplorerActionValues
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream

internal class ZipArchiveCreator(
    remoteSession: org.autojs.plugin.explorer.api.IExplorerActionHostSession,
    private val cacheDirectory: File? = null,
) : ArchiveWriter {

    override val format = ArchiveFormat.ZIP
    override val formatCapabilities = ZipArchiveBackend.capabilities

    private val session = ExplorerActionHostSessionClient(remoteSession)
    private val sourceWalker = ArchiveSourceWalker(session)
    private val verifier = CreatedArchiveVerifier(
        requireNotNull(cacheDirectory) {
            "ZIP creation requires a private cache directory for output verification"
        },
    )

    override fun create(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
        outputCommitter: ArchiveOutputCommitter,
    ): ArchiveCreationResult {
        val splitVolumeSizeBytes = options.splitVolumeSizeBytes
        val outputName = ArchiveCompressionPolicy.normalizeCreationOutputDisplayName(
            value = options.outputDisplayName,
            format = format,
            splitVolumeSizeBytes = splitVolumeSizeBytes,
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
            passwordChars = options.password
                ?.takeIf(CharArray::isNotEmpty)
                ?.clone()
            return if (splitVolumeSizeBytes == null) {
                createSingleOutput(
                    request = request,
                    options = options,
                    outputName = outputName,
                    compressionLevel = compressionLevel,
                    password = passwordChars,
                    checkCancelled = checkCancelled,
                    progress = progress,
                    outputCommitter = outputCommitter,
                )
            } else {
                createSplitOutput(
                    request = request,
                    options = options,
                    outputName = outputName,
                    compressionLevel = compressionLevel,
                    password = passwordChars,
                    splitVolumeSizeBytes = splitVolumeSizeBytes,
                    checkCancelled = checkCancelled,
                    progress = progress,
                    outputCommitter = outputCommitter,
                )
            }
        } finally {
            passwordChars?.fill('\u0000')
        }
    }

    private fun createSingleOutput(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        outputName: String,
        compressionLevel: Int,
        password: CharArray?,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
        outputCommitter: ArchiveOutputCommitter,
    ): ArchiveCreationResult {
        val completed = session.writeArchiveOutput(
            outputDisplayName = outputName,
            format = format,
            conflictPolicy = options.conflictPolicy,
            prepareAfterReservation = {
                progress.reportScanStarted()
                sourceWalker.scan(request.targets, checkCancelled, progress::reportScan)
            },
            write = { descriptor, manifest ->
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
                    writeArchive(
                        output = BufferedOutputStream(rawOutput, BUFFER_SIZE),
                        manifest = manifest,
                        compressionLevel = compressionLevel,
                        password = password,
                        checkCancelled = checkCancelled,
                        progress = progress,
                    )
                }.also { checkCancelled() }
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
            committedOutputTransactionIds = listOf(committed.id),
        )
    }

    private fun createSplitOutput(
        request: ArchiveCompressionRequest,
        options: ArchiveCreationOptions,
        outputName: String,
        compressionLevel: Int,
        password: CharArray?,
        splitVolumeSizeBytes: Long,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
        outputCommitter: ArchiveOutputCommitter,
    ): ArchiveCreationResult {
        val resolvedCacheDirectory = requireNotNull(cacheDirectory) {
            "ZIP split creation requires a private cache directory"
        }
        val publisher = ZipSplitOutputPublisher(
            session = session,
            requestedTerminalDisplayName = outputName,
            conflictPolicy = options.conflictPolicy,
            splitVolumeSizeBytes = splitVolumeSizeBytes,
            outputCommitter = outputCommitter,
        )
        publisher.reserveTerminalBeforeSourceAccess()
        var workspace: ZipSplitArchiveWorkspace? = null
        try {
            progress.reportScanStarted()
            val manifest = sourceWalker.scan(request.targets, checkCancelled, progress::reportScan)
            val activeWorkspace = ZipSplitArchiveWorkspace.create(
                cacheDirectory = resolvedCacheDirectory,
                splitVolumeSizeBytes = splitVolumeSizeBytes,
            )
            workspace = activeWorkspace
            val counters = activeWorkspace.openSplitOutput().use { splitOutput ->
                writeArchive(
                    output = splitOutput,
                    manifest = manifest,
                    compressionLevel = compressionLevel,
                    password = password,
                    checkCancelled = checkCancelled,
                    progress = progress,
                    checkOutputCapacity = activeWorkspace::ensureCapacity,
                )
            }
            checkCancelled()
            val volumes = activeWorkspace.collectVolumes()
            progress.reportVerifying(manifest, counters)
            runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.VERIFY,
                outputDisplayName = outputName,
                format = format,
            ) {
                verifier.verifySplitZip(
                    terminalFile = activeWorkspace.terminalFile,
                    volumes = volumes,
                    manifest = manifest,
                    counters = counters,
                    password = password,
                    checkCancelled = checkCancelled,
                )
            }
            val terminalDisplayName = publisher.reserveCompleteGroup(volumes)
            publisher.writePendingVolumes(volumes, terminalDisplayName, checkCancelled)
            publisher.verifyPendingVolumes(volumes, terminalDisplayName, checkCancelled)

            // Remove the private staging copy after every host pending file matches it byte-for-byte.
            activeWorkspace.close()
            workspace = null

            checkCancelled()
            progress.reportCommitting(manifest, counters)
            val committed = publisher.commitVolumes(
                volumes = volumes,
                terminalDisplayName = terminalDisplayName,
                checkCancelled = checkCancelled,
            )
            val terminal = committed.last()
            return ArchiveCreationResult(
                outputDisplayName = terminal.displayName,
                outputDisplayPath = terminal.displayPath,
                filesCompressed = counters.files,
                directoriesAdded = counters.directories,
                sourceBytesRead = counters.bytesRead,
                committedOutputTransactionIds = committed.map(HostOutputTransaction::id),
                createdOutputs = committed.map { output ->
                    CreatedArchiveOutput(output.displayName, output.displayPath)
                },
            )
        } catch (error: Throwable) {
            val workspaceFailure = runCatching { workspace?.close() }.exceptionOrNull()
            val effectiveFailure = if (workspaceFailure == null) {
                error
            } else {
                workspaceFailure.apply {
                    if (this !== error) addSuppressed(error)
                }
            }
            if (
                error is ArchiveCreationPartialOutputException ||
                error is ArchiveCreationOutputGroupRollbackException
            ) {
                if (workspaceFailure != null) error.addSuppressed(workspaceFailure)
                throw error
            }
            publisher.abortAndRethrow(effectiveFailure)
        }
    }

    private fun writeArchive(
        output: OutputStream,
        manifest: ArchiveSourceManifest,
        compressionLevel: Int,
        password: CharArray?,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
        checkOutputCapacity: () -> Unit = { },
    ): ArchiveCreationCounters {
        val counters = ArchiveCreationCounters(manifest.entries.size)
        progress.reportCompression(manifest, counters, currentEntry = null)
        ZipOutputStream(output, password).use { zipOutput ->
            manifest.entries.forEachIndexed { entryIndex, item ->
                checkCancelled()
                progress.reportCompression(manifest, counters, item.archivePath)
                processSource(
                    entryIndex = entryIndex,
                    item = item,
                    manifest = manifest,
                    zipOutput = zipOutput,
                    counters = counters,
                    compressionLevel = compressionLevel,
                    encryptFiles = password != null,
                    checkCancelled = checkCancelled,
                    progress = progress,
                    checkOutputCapacity = checkOutputCapacity,
                )
            }
        }
        checkOutputCapacity()
        return counters
    }

    private fun processSource(
        entryIndex: Int,
        item: ArchiveSourceEntry,
        manifest: ArchiveSourceManifest,
        zipOutput: ZipOutputStream,
        counters: ArchiveCreationCounters,
        compressionLevel: Int,
        encryptFiles: Boolean,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationProgressListener,
        checkOutputCapacity: () -> Unit,
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
                checkOutputCapacity()
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
                val fingerprint = sourceWalker.copyFileWithFingerprint(
                    entry = item,
                    output = zipOutput,
                    expectedSize = item.size.takeIf { it >= 0L },
                    checkCancelled = checkCancelled,
                    onBytesWritten = { count ->
                        counters.bytesRead = Math.addExact(counters.bytesRead, count.toLong())
                        checkOutputCapacity()
                        progress.reportCompression(manifest, counters, item.archivePath)
                    },
                )
                counters.recordSourceFingerprint(entryIndex, fingerprint)
                zipOutput.closeEntry()
                counters.files++
                checkOutputCapacity()
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
