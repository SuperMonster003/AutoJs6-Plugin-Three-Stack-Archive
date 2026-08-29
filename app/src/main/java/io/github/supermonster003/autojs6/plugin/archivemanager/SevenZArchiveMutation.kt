package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.ParcelFileDescriptor
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.tukaani.xz.LZMA2Options
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.security.MessageDigest
import java.util.Date
import java.util.zip.CRC32

internal val SEVEN_Z_MUTATION_CAPABILITIES = ArchiveMutationCapabilities(
    operations = setOf(
        ArchiveOperation.ADD,
        ArchiveOperation.DELETE,
        ArchiveOperation.RENAME,
    ),
    strategy = ArchiveMutationStrategy.FULL_REWRITE,
    minimumHostProtocolVersion = 8,
    metadataEffects = setOf(
        ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED,
        ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
    ),
)

internal const val SEVEN_Z_MUTATION_COMPRESSION_LEVEL = 3
internal const val SEVEN_Z_MUTATION_MAX_ENCODER_MEMORY_KIB = 40 * 1_024
internal const val SEVEN_Z_MUTATION_MAX_DECODER_MEMORY_KIB = 64 * 1_024

internal fun sevenZMutationCompressionLevel(): Int =
    SEVEN_Z_MUTATION_COMPRESSION_LEVEL.also { level ->
        val encoderMemoryUsageKiB = LZMA2Options(level).encoderMemoryUsage
        check(encoderMemoryUsageKiB <= SEVEN_Z_MUTATION_MAX_ENCODER_MEMORY_KIB) {
            "7Z mutation encoder requires $encoderMemoryUsageKiB KiB"
        }
    }

internal fun sevenZMutationAvailability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
    when {
        snapshot.format != ArchiveFormat.SEVEN_Z -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
        )
        snapshot.volumeIdentities.isNotEmpty() -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE,
        )
        snapshot.entries.any { it.pathStatus != ArchiveEntryPathStatus.SAFE } ->
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH,
            )
        snapshot.readerOptions.hasPassword || snapshot.entries.any { it.isEncrypted } ->
            ArchiveMutationAvailability.unavailable(
                ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            )
        snapshot.entries.any {
            ArchiveEntryLimitation.SOLID_COMPRESSION in it.capabilities.limitations
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
        )
        snapshot.entries.any {
            ArchiveEntryLimitation.MUTATION_RESOURCE_BUDGET_EXCEEDED in
                it.capabilities.limitations
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
        )
        snapshot.entries.any {
            !it.isDirectory &&
                (
                    !it.capabilities.canOpen ||
                        ArchiveEntryLimitation.UNSUPPORTED_COMPRESSION_METHOD in
                        it.capabilities.limitations ||
                        ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE in
                        it.capabilities.limitations
                    )
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
        )
        snapshot.entries.any {
            !it.capabilities.canDelete || !it.capabilities.canRename
        } -> ArchiveMutationAvailability.unavailable(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
        )
        else -> ArchiveMutationAvailability.available(SEVEN_Z_MUTATION_CAPABILITIES)
    }

/** Reuses the format-neutral directory planner already exercised by every TAR rewrite. */
internal object SevenZArchiveMutationPlanner {
    fun plan(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): TarArchiveMutationPlan = TarArchiveMutationPlanner.plan(
        snapshot = snapshot,
        request = request,
        capabilitiesOverride = SEVEN_Z_MUTATION_CAPABILITIES,
        validateRetainedEntry = ::requireRewritableSevenZEntry,
    )

    private fun requireRewritableSevenZEntry(
        planned: TarArchiveMutationEntry,
        format: ArchiveFormat,
    ) {
        val entry = (planned.source as? TarArchiveMutationSource.Existing)?.entry ?: return
        if (entry.pathStatus != ArchiveEntryPathStatus.SAFE) {
            fail(
                ArchiveFailureCode.INVALID_PATH,
                "Unsafe 7Z paths cannot be preserved by rewriting",
                format,
            )
        }
        if (entry.isDirectory) return
        if (
            entry.isEncrypted ||
            !entry.capabilities.canOpen ||
            ArchiveEntryLimitation.SOLID_COMPRESSION in entry.capabilities.limitations ||
            ArchiveEntryLimitation.MUTATION_RESOURCE_BUDGET_EXCEEDED in
            entry.capabilities.limitations ||
            ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE in entry.capabilities.limitations
        ) {
            fail(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "A retained 7Z entry cannot be rewritten within the safe provider boundary",
                format,
            )
        }
    }

    private fun fail(
        code: ArchiveFailureCode,
        message: String,
        format: ArchiveFormat,
    ): Nothing = throw ArchiveValidationException(code, message, format = format)
}

/**
 * Rebuilds a single-volume, unencrypted, non-solid 7Z through the host's atomic replacement
 * transaction. Encrypted and solid inputs remain readable but deliberately stay outside this
 * provider because Commons Compress cannot preserve header encryption or solid block structure.
 */
internal class SevenZArchiveMutationProvider(
    private val session: ExplorerActionHostSessionClient,
    cacheDirectory: java.io.File,
    private val engine: ArchiveEngine = ArchiveEngine.DEFAULT,
) : ArchiveMutationProvider {
    override val format = ArchiveFormat.SEVEN_Z
    override val capabilities = SEVEN_Z_MUTATION_CAPABILITIES

    private val compressionLevel = sevenZMutationCompressionLevel()
    private val verifier = CreatedArchiveVerifier(cacheDirectory, engine)

    override fun availability(snapshot: ArchiveSnapshot): ArchiveMutationAvailability =
        sevenZMutationAvailability(snapshot)

    override fun prepare(
        snapshot: ArchiveSnapshot,
        request: ArchiveMutationRequest,
    ): PreparedArchiveMutation {
        val availability = availability(snapshot)
        if (!availability.isAvailable) {
            throw unavailableMutation(availability.unavailableReason)
        }
        check(capabilities.supports(request.operation)) {
            "7Z mutation provider does not support ${request.operation}"
        }
        return SevenZArchiveMutationPlanner.plan(snapshot, request)
    }

    override fun execute(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        prepared: PreparedArchiveMutation,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): HostOutputTransaction {
        checkCancelled()
        val plan = prepared as? TarArchiveMutationPlan
            ?: throw IllegalArgumentException("Prepared mutation belongs to another provider")
        require(plan.format == format) { "Prepared mutation format changed" }
        if (!plan.sourceVersion.matches(snapshot)) {
            sourceChanged("7Z mutation plan belongs to another source snapshot")
        }
        val availability = availability(snapshot)
        if (!availability.isAvailable) {
            throw unavailableMutation(availability.unavailableReason)
        }
        val manifest = plan.toSevenZManifest()
        progress.onProgress(
            ArchiveMutationProgress(
                phase = ArchiveMutationPhase.PREPARING,
                currentPath = null,
                completedEntries = 0,
                totalEntries = plan.entries.size,
            ),
        )
        return runTransaction(
            source = source,
            snapshot = snapshot,
            targetId = targetId,
            displayName = displayName,
            plan = plan,
            manifest = manifest,
            checkCancelled = checkCancelled,
            progress = progress,
        )
    }

    private fun unavailableMutation(reason: ArchiveMutationUnavailableReason?): ArchiveException =
        when (reason) {
            ArchiveMutationUnavailableReason.MULTI_VOLUME_ARCHIVE -> ArchiveValidationException(
                ArchiveFailureCode.MISSING_VOLUME,
                "Multi-volume 7Z archives are read-only",
                format = format,
            )
            ArchiveMutationUnavailableReason.UNSAFE_ENTRY_PATH -> ArchiveValidationException(
                ArchiveFailureCode.INVALID_PATH,
                "A 7Z archive with unsafe entry paths cannot be rewritten",
                format = format,
            )
            ArchiveMutationUnavailableReason.PASSWORD_REQUIRED -> ArchiveValidationException(
                ArchiveFailureCode.PASSWORD_REQUIRED,
                "This 7Z archive requires a password",
                format = format,
            )
            ArchiveMutationUnavailableReason.UNSUPPORTED_ENTRY_METHOD,
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            -> ArchiveValidationException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "This 7Z variant cannot be rewritten safely by the installed backend",
                format = format,
            )
            ArchiveMutationUnavailableReason.FORMAT_NOT_SUPPORTED,
            null,
            -> ArchiveValidationException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "The selected archive does not belong to the 7Z mutation provider",
                format = format,
            )
        }

    private fun runTransaction(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        targetId: String,
        displayName: String,
        plan: TarArchiveMutationPlan,
        manifest: ArchiveSourceManifest,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): HostOutputTransaction {
        val prepared = runCreationOutputOperation(
            operation = ArchiveCreationOutputOperation.PREPARE,
            outputDisplayName = displayName,
            format = format,
        ) {
            session.prepareTargetReplacement(targetId, displayName)
        }
        try {
            val descriptor = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.OPEN,
                outputDisplayName = displayName,
                format = format,
            ) {
                session.openOutput(prepared.id)
            }
            val counters = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.WRITE,
                outputDisplayName = displayName,
                format = format,
            ) {
                descriptor.use { output ->
                    writeReplacement(
                        descriptor = output,
                        source = source,
                        snapshot = snapshot,
                        plan = plan,
                        checkCancelled = checkCancelled,
                        progress = progress,
                    )
                }
            }
            checkCancelled()
            progress.onProgress(
                ArchiveMutationProgress(
                    phase = ArchiveMutationPhase.VERIFYING,
                    currentPath = null,
                    completedEntries = plan.entries.size,
                    totalEntries = plan.entries.size,
                ),
            )
            val pendingDescriptor = runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.VERIFY,
                outputDisplayName = displayName,
                format = format,
            ) {
                session.openPendingOutput(prepared.id)
            }
            runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.VERIFY,
                outputDisplayName = displayName,
                format = format,
            ) {
                pendingDescriptor.use { pending ->
                    verifier.verifyPendingOutput(
                        descriptor = pending,
                        format = format,
                        manifest = manifest,
                        counters = counters,
                        password = null,
                        checkCancelled = checkCancelled,
                    )
                }
            }
            checkCancelled()
            progress.onProgress(
                ArchiveMutationProgress(
                    phase = ArchiveMutationPhase.COMMITTING,
                    currentPath = null,
                    completedEntries = plan.entries.size,
                    totalEntries = plan.entries.size,
                ),
            )
            return runCreationOutputOperation(
                operation = ArchiveCreationOutputOperation.COMMIT,
                outputDisplayName = displayName,
                format = format,
            ) {
                session.commitExactOutput(prepared.id, displayName)
            }
        } catch (operationFailure: Throwable) {
            val rollbackFailure = runCatching { session.abortOutput(prepared.id) }.exceptionOrNull()
            if (rollbackFailure != null) {
                throw ArchiveCreationRollbackException(
                    pendingOutputDisplayName = prepared.displayName,
                    pendingOutputDisplayPath = prepared.displayPath,
                    operationFailure = operationFailure,
                    rollbackFailure = rollbackFailure,
                    format = format,
                )
            }
            throw operationFailure
        }
    }

    private fun writeReplacement(
        descriptor: ParcelFileDescriptor,
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        plan: TarArchiveMutationPlan,
        checkCancelled: () -> Unit,
        progress: ArchiveMutationProgressListener,
    ): ArchiveCreationCounters {
        requireUnchangedSource(source, snapshot)
        val readerOptions = snapshot.readerOptions.retainedCopy()
        val cancellableSource = CancellationCheckingArchiveReadSource(source, checkCancelled)
        try {
            engine.openReader(cancellableSource, format, readerOptions).use { reader ->
                validateReaderSnapshot(reader, snapshot)
                val counters = ArchiveCreationCounters(plan.entries.size)
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { rawOutput ->
                    SevenZOutputFile(rawOutput.channel).use { output ->
                        output.setContentMethods(
                            listOf(
                                SevenZMethodConfiguration(
                                    SevenZMethod.LZMA2,
                                    LZMA2Options(compressionLevel),
                                ),
                            ),
                        )
                        plan.entries.forEachIndexed { index, entry ->
                            checkCancelled()
                            progress.onProgress(
                                ArchiveMutationProgress(
                                    phase = ArchiveMutationPhase.WRITING,
                                    currentPath = entry.archivePath,
                                    completedEntries = index,
                                    totalEntries = plan.entries.size,
                                ),
                            )
                            output.putArchiveEntry(sevenZEntry(entry))
                            if (entry.isDirectory) {
                                output.closeArchiveEntry()
                                counters.directories++
                            } else {
                                val expected = expectedInput(entry, reader)
                                val fingerprint = expected.input.use { input ->
                                    copyAndFingerprint(
                                        input = input,
                                        output = SevenZMutationOutputStream(output),
                                        expectedSize = entry.size.takeIf { it >= 0L },
                                        expectedCrc = expected.crc,
                                        checkCancelled = checkCancelled,
                                        onBytesCopied = { copied ->
                                            counters.bytesRead = Math.addExact(
                                                counters.bytesRead,
                                                copied.toLong(),
                                            )
                                        },
                                    )
                                }
                                counters.recordSourceFingerprint(index, fingerprint)
                                output.closeArchiveEntry()
                                counters.files++
                            }
                        }
                    }
                }
                requireUnchangedSource(source, snapshot)
                return counters
            }
        } finally {
            readerOptions.clearPassword()
        }
    }

    private fun expectedInput(
        planned: TarArchiveMutationEntry,
        reader: ArchiveReader,
    ): ExpectedMutationInput = when (val source = planned.source) {
        is TarArchiveMutationSource.Existing -> {
            val readerEntry = reader.entryAt(source.entry.ordinal)
                ?: sourceChanged("A retained 7Z entry disappeared while rewriting")
            ExpectedMutationInput(
                input = reader.openEntry(readerEntry),
                crc = source.entry.crc32,
            )
        }
        is TarArchiveMutationSource.AddedFile -> ExpectedMutationInput(
            input = try {
                source.file.openInputStream()
            } catch (error: IOException) {
                throw ArchiveCreationSourceException(
                    sourceArchivePath = planned.archivePath,
                    message = "7Z source file cannot be opened: ${planned.archivePath}",
                    cause = error,
                )
            },
            crc = null,
        )
        is TarArchiveMutationSource.AddedDirectory -> error("7Z directories have no input data")
    }

    private fun sevenZEntry(planned: TarArchiveMutationEntry): SevenZArchiveEntry =
        SevenZArchiveEntry().apply {
            name = planned.archivePath.trimEnd('/')
            isDirectory = planned.isDirectory
            if (planned.lastModified > 0L) lastModifiedDate = Date(planned.lastModified)
        }

    private fun validateReaderSnapshot(reader: ArchiveReader, snapshot: ArchiveSnapshot) {
        if (reader.format != format || reader.entries.size != snapshot.entries.size) {
            sourceChanged("7Z directory metadata changed before rewriting")
        }
        reader.entries.forEachIndexed { index, current ->
            val expected = snapshot.entries[index]
            if (
                current.ordinal != expected.ordinal ||
                current.name != expected.sourceName ||
                current.isDirectory != expected.isDirectory ||
                current.compressionMethod != expected.compressionMethod ||
                current.compressionMethodId != expected.compressionMethodId ||
                current.isEncrypted != expected.isEncrypted ||
                current.encryptionMethod != expected.encryptionMethod ||
                current.compressedSize != expected.compressedSize ||
                current.size != expected.uncompressedSize ||
                current.crc != expected.crc32 ||
                current.time != expected.modifiedTimeMillis
            ) {
                sourceChanged("7Z directory metadata changed before rewriting")
            }
        }
    }

    private fun requireUnchangedSource(source: ArchiveReadSource, snapshot: ArchiveSnapshot) {
        val identity = try {
            source.identity()
        } catch (error: IOException) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.SOURCE_CHANGED,
                message = "7Z source cannot be inspected while rewriting",
                cause = error,
                format = format,
            )
        }
        if (
            identity.length != snapshot.sourceLength ||
            identity.lastModifiedMillis != snapshot.sourceLastModifiedMillis
        ) {
            sourceChanged("7Z source changed while rewriting")
        }
    }

    private fun copyAndFingerprint(
        input: InputStream,
        output: OutputStream,
        expectedSize: Long?,
        expectedCrc: Long?,
        checkCancelled: () -> Unit,
        onBytesCopied: (Int) -> Unit,
    ): ArchiveCreationSourceFingerprint {
        val digest = MessageDigest.getInstance(SHA_256)
        val crc = CRC32()
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
        while (true) {
            checkCancelled()
            val read = buffered.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            copied = Math.addExact(copied, read.toLong())
            if (expectedSize != null && copied > expectedSize) {
                sourceChanged("7Z entry data grew while it was being rewritten")
            }
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            crc.update(buffer, 0, read)
            onBytesCopied(read)
        }
        if (expectedSize != null && copied != expectedSize) {
            sourceChanged("7Z entry size changed while it was being rewritten")
        }
        if (expectedCrc != null && expectedCrc >= 0L && crc.value != expectedCrc) {
            throw ArchiveValidationException(
                code = ArchiveFailureCode.CRC_MISMATCH,
                message = "7Z entry CRC changed while it was being rewritten",
                format = format,
                stage = ArchiveFailureStage.ENTRY_DATA,
            )
        }
        return ArchiveCreationSourceFingerprint(copied, digest.digest())
    }

    private fun TarArchiveMutationPlan.toSevenZManifest(): ArchiveSourceManifest {
        var knownBytes = 0L
        var unknownFiles = 0L
        var files = 0L
        var directories = 0L
        val manifestEntries = entries.mapIndexed { index, entry ->
            if (entry.isDirectory) {
                directories++
            } else {
                files++
                if (entry.size >= 0L) {
                    knownBytes = try {
                        Math.addExact(knownBytes, entry.size)
                    } catch (error: ArithmeticException) {
                        throw ArchiveValidationException(
                            code = ArchiveFailureCode.MALFORMED_ARCHIVE,
                            message = "Rewritten 7Z size metadata overflows",
                            cause = error,
                            format = format,
                        )
                    }
                } else {
                    unknownFiles++
                }
            }
            ArchiveSourceEntry(
                targetId = "7z-mutation-$index",
                relativePath = entry.archivePath,
                archivePath = entry.archivePath,
                kind = if (entry.isDirectory) {
                    ExplorerActionValues.TARGET_DIRECTORY
                } else {
                    ExplorerActionValues.TARGET_FILE
                },
                size = entry.size,
                lastModified = entry.lastModified,
            )
        }
        return ArchiveSourceManifest(
            entries = manifestEntries,
            fileCount = files,
            directoryCount = directories,
            knownSourceBytes = knownBytes,
            unknownSizeFileCount = unknownFiles,
        )
    }

    private fun sourceChanged(message: String): Nothing =
        throw ArchiveValidationException(
            code = ArchiveFailureCode.SOURCE_CHANGED,
            message = message,
            format = format,
            stage = ArchiveFailureStage.INPUT,
        )

    private data class ExpectedMutationInput(
        val input: InputStream,
        val crc: Long?,
    )

    private class SevenZMutationOutputStream(
        private val output: SevenZOutputFile,
    ) : OutputStream() {
        override fun write(value: Int) = output.write(value)

        override fun write(buffer: ByteArray, offset: Int, length: Int) =
            output.write(buffer, offset, length)

        override fun close() = Unit
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1_024
        const val SHA_256 = "SHA-256"
    }
}

private class CancellationCheckingArchiveReadSource(
    private val delegate: ArchiveReadSource,
    private val checkCancelled: () -> Unit,
) : ArchiveReadSource by delegate {
    override fun openSeekableChannel(): SeekableByteChannel =
        CancellationCheckingSeekableByteChannel(
            delegate = delegate.openSeekableChannel(),
            checkCancelled = checkCancelled,
        )

    override fun openInputStream(): InputStream = Channels.newInputStream(openSeekableChannel())
}

private class CancellationCheckingSeekableByteChannel(
    private val delegate: SeekableByteChannel,
    private val checkCancelled: () -> Unit,
) : SeekableByteChannel {
    override fun read(destination: ByteBuffer): Int {
        checkCancelled()
        return delegate.read(destination).also { checkCancelled() }
    }

    override fun write(source: ByteBuffer): Int = delegate.write(source)

    override fun position(): Long = delegate.position()

    override fun position(newPosition: Long): SeekableByteChannel {
        checkCancelled()
        delegate.position(newPosition)
        return this
    }

    override fun size(): Long = delegate.size()

    override fun truncate(size: Long): SeekableByteChannel = delegate.truncate(size).let { this }

    override fun isOpen(): Boolean = delegate.isOpen

    override fun close() = delegate.close()
}
