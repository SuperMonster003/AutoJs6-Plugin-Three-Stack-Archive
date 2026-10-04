package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.ParcelFileDescriptor
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/**
 * Reserves, fills, and commits every physical file in one standard split ZIP as a coherent group.
 *
 * Names are reserved exactly before any host output is opened. A v15 creation plan supplies a
 * deferred committer and publishes the verified group through one recoverable host batch. Direct
 * legacy callers retain the explicit v7 partial-output fallback.
 */
internal class ZipSplitOutputPublisher(
    private val session: ExplorerActionHostSessionClient,
    private val requestedTerminalDisplayName: String,
    private val conflictPolicy: ArchiveCreationConflictPolicy,
    private val splitVolumeSizeBytes: Long,
    private val outputCommitter: ArchiveOutputCommitter = ImmediateArchiveOutputCommitter,
) {
    private val pending = LinkedHashMap<String, HostOutputTransaction>()
    private val committed = mutableListOf<HostOutputTransaction>()
    private var numberingIndex = 0
    private var expectedOutputCount = 1
    private var terminalDisplayName: String? = null

    fun reserveTerminalBeforeSourceAccess(): HostOutputTransaction {
        check(pending.isEmpty() && committed.isEmpty())
        while (numberingIndex <= MAX_NUMBERING_ATTEMPTS) {
            try {
                return reserveCandidate(suffixes = listOf(ZIP_SUFFIX)).single().also { terminal ->
                    terminalDisplayName = terminal.displayName
                }
            } catch (conflict: OutputNameConflictException) {
                check(pending.isEmpty())
                if (conflictPolicy == ArchiveCreationConflictPolicy.ASK) {
                    throw ArchiveOutputNameUnavailableException(
                        conflict.displayName,
                        conflict.hostFailure,
                    )
                }
                numberingIndex++
            }
        }
        throw ArchiveCreationOutputException(
            operation = ArchiveCreationOutputOperation.PREPARE,
            outputDisplayName = requestedTerminalDisplayName,
            cause = IOException("No ZIP output name is available"),
            format = ArchiveFormat.ZIP,
        )
    }

    fun reserveCompleteGroup(volumes: List<StagedZipVolume>): String {
        require(volumes.isNotEmpty() && volumes.last().terminal)
        expectedOutputCount = volumes.size
        val suffixes = volumes.map(StagedZipVolume::suffix)
        var currentTerminal = requireNotNull(terminalDisplayName)
        try {
            reserveMissingParts(currentTerminal, suffixes)
            return currentTerminal
        } catch (conflict: OutputNameConflictException) {
            abortPendingOrThrow(conflict.cause ?: conflict)
            if (conflictPolicy == ArchiveCreationConflictPolicy.ASK) {
                throw ArchiveOutputNameUnavailableException(
                    conflict.displayName,
                    conflict.hostFailure,
                )
            }
            numberingIndex++
        }

        while (numberingIndex <= MAX_NUMBERING_ATTEMPTS) {
            try {
                val transactions = reserveCandidate(suffixes)
                currentTerminal = transactions.last().displayName
                terminalDisplayName = currentTerminal
                return currentTerminal
            } catch (conflict: OutputNameConflictException) {
                abortPendingOrThrow(conflict.cause ?: conflict)
                numberingIndex++
            }
        }
        throw ArchiveCreationOutputException(
            operation = ArchiveCreationOutputOperation.PREPARE,
            outputDisplayName = requestedTerminalDisplayName,
            cause = IOException("No complete ZIP split-output name group is available"),
            format = ArchiveFormat.ZIP,
        )
    }

    fun writePendingVolumes(
        volumes: List<StagedZipVolume>,
        terminalDisplayName: String,
        checkCancelled: () -> Unit,
    ) {
        require(volumes.size == pending.size)
        try {
            volumes.forEach { volume ->
                checkCancelled()
                val displayName = volume.outputDisplayName(terminalDisplayName)
                val transaction = requireNotNull(pending[displayName])
                val descriptor = runCreationOutputOperation(
                    operation = ArchiveCreationOutputOperation.OPEN,
                    outputDisplayName = displayName,
                    format = ArchiveFormat.ZIP,
                ) {
                    session.openOutput(transaction.id)
                }
                runCreationOutputOperation(
                    operation = ArchiveCreationOutputOperation.WRITE,
                    outputDisplayName = displayName,
                    format = ArchiveFormat.ZIP,
                ) {
                    descriptor.use { outputDescriptor ->
                        copyVolume(volume, outputDescriptor, checkCancelled)
                    }
                }
            }
        } catch (error: Throwable) {
            abortPendingOrThrow(error)
            throw error
        }
    }

    fun verifyPendingVolumes(
        volumes: List<StagedZipVolume>,
        terminalDisplayName: String,
        checkCancelled: () -> Unit,
    ) {
        require(volumes.size == pending.size)
        try {
            volumes.forEach { volume ->
                checkCancelled()
                val displayName = volume.outputDisplayName(terminalDisplayName)
                val transaction = requireNotNull(pending[displayName])
                val expectedSize = volume.file.length()
                val expectedFingerprint = runCreationOutputOperation(
                    operation = ArchiveCreationOutputOperation.VERIFY,
                    outputDisplayName = displayName,
                    format = ArchiveFormat.ZIP,
                ) {
                    volume.file.inputStream().use { input ->
                        fingerprint(input, checkCancelled)
                    }.also { fingerprint ->
                        if (
                            volume.file.length() != expectedSize ||
                            fingerprint.bytes != expectedSize
                        ) {
                            throw IOException(
                                "ZIP staging volume changed while it was being verified",
                            )
                        }
                    }
                }
                val pendingFingerprint = runCreationOutputOperation(
                    operation = ArchiveCreationOutputOperation.VERIFY,
                    outputDisplayName = displayName,
                    format = ArchiveFormat.ZIP,
                ) {
                    val descriptor = session.openPendingOutput(transaction.id)
                    ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                        fingerprint(input, checkCancelled)
                    }
                }
                if (
                    pendingFingerprint.bytes != expectedFingerprint.bytes ||
                    !MessageDigest.isEqual(
                        pendingFingerprint.sha256,
                        expectedFingerprint.sha256,
                    )
                ) {
                    throw ArchiveCreationOutputException(
                        operation = ArchiveCreationOutputOperation.VERIFY,
                        outputDisplayName = displayName,
                        cause = IOException("Host pending ZIP volume differs from its staging file"),
                        format = ArchiveFormat.ZIP,
                    )
                }
            }
        } catch (error: Throwable) {
            abortPendingOrThrow(error)
            throw error
        }
    }

    fun commitVolumes(
        volumes: List<StagedZipVolume>,
        terminalDisplayName: String,
        checkCancelled: () -> Unit,
    ): List<HostOutputTransaction> {
        require(volumes.size == pending.size)
        (outputCommitter as? ArchiveOutputBatchCommitter)?.requireCapacity(
            volumes.size,
            terminalDisplayName,
        )
        volumes.forEach { volume ->
            val displayName = volume.outputDisplayName(terminalDisplayName)
            val transaction = requireNotNull(pending[displayName])
            try {
                checkCancelled()
                val result = runCreationOutputOperation(
                    operation = ArchiveCreationOutputOperation.COMMIT,
                    outputDisplayName = displayName,
                    format = ArchiveFormat.ZIP,
                ) {
                    outputCommitter.commitExactOutput(
                        session = session,
                        transaction = transaction,
                        expectedDisplayName = displayName,
                        format = ArchiveFormat.ZIP,
                    )
                }
                pending.remove(displayName)
                committed += result
            } catch (error: Throwable) {
                val mapped = mapCreationOutputFailure(
                    operation = ArchiveCreationOutputOperation.COMMIT,
                    outputDisplayName = displayName,
                    format = ArchiveFormat.ZIP,
                    error = error,
                )
                abortPendingOrThrow(mapped)
                if (committed.isNotEmpty() && outputCommitter === ImmediateArchiveOutputCommitter) {
                    throw ArchiveCreationPartialOutputException(
                        committedOutputs = committed.toList(),
                        totalOutputs = expectedOutputCount,
                        failedOutputDisplayName = displayName,
                        operationFailure = mapped,
                        format = ArchiveFormat.ZIP,
                    )
                }
                throw mapped
            }
        }
        check(pending.isEmpty())
        return committed.toList()
    }

    fun abortAndRethrow(error: Throwable): Nothing {
        abortPendingOrThrow(error)
        throw error
    }

    private fun reserveCandidate(suffixes: List<String>): List<HostOutputTransaction> {
        require(pending.isEmpty())
        val terminalCandidate = ArchiveCompressionPolicy.numberedCreationOutputDisplayName(
            value = requestedTerminalDisplayName,
            format = ArchiveFormat.ZIP,
            index = numberingIndex,
            splitVolumeSizeBytes = splitVolumeSizeBytes,
        )
        return try {
            suffixes.map { suffix ->
                val displayName = if (suffix == ZIP_SUFFIX) {
                    terminalCandidate
                } else {
                    terminalCandidate.dropLast(ZIP_SUFFIX.length) + suffix
                }
                reserveExact(displayName, suffix == ZIP_SUFFIX).also { transaction ->
                    pending[displayName] = transaction
                }
            }
        } catch (conflict: OutputNameConflictException) {
            throw conflict
        } catch (error: Throwable) {
            abortPendingOrThrow(error)
            throw mapCreationOutputFailure(
                operation = ArchiveCreationOutputOperation.PREPARE,
                outputDisplayName = terminalCandidate,
                format = ArchiveFormat.ZIP,
                error = error,
            )
        }
    }

    private fun reserveMissingParts(terminal: String, suffixes: List<String>) {
        require(pending.keys == setOf(terminal))
        suffixes.dropLast(1).forEach { suffix ->
            val displayName = terminal.dropLast(ZIP_SUFFIX.length) + suffix
            reserveExact(displayName, terminal = false).also { transaction ->
                pending[displayName] = transaction
            }
        }
    }

    private fun reserveExact(displayName: String, terminal: Boolean): HostOutputTransaction = try {
        session.prepareExactOutput(
            displayName = displayName,
            mimeType = if (terminal) ArchiveFormat.ZIP.primaryMimeType else SPLIT_PART_MIME_TYPE,
        )
    } catch (error: ArchiveOutputNameUnavailableException) {
        throw OutputNameConflictException(displayName, error.hostFailure)
    }

    private fun copyVolume(
        volume: StagedZipVolume,
        descriptor: ParcelFileDescriptor,
        checkCancelled: () -> Unit,
    ) {
        val expectedSize = volume.file.length()
        var copied = 0L
        BufferedInputStream(volume.file.inputStream(), BUFFER_SIZE).use { input ->
            BufferedOutputStream(
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor),
                BUFFER_SIZE,
            ).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    checkCancelled()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied = Math.addExact(copied, read.toLong())
                }
            }
        }
        if (copied != expectedSize || volume.file.length() != expectedSize) {
            throw IOException("ZIP staging volume changed while it was published")
        }
    }

    private fun fingerprint(
        source: InputStream,
        checkCancelled: () -> Unit,
    ): VolumeFingerprint {
        val digest = MessageDigest.getInstance(SHA_256)
        var bytes = 0L
        BufferedInputStream(source, BUFFER_SIZE).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                checkCancelled()
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                digest.update(buffer, 0, read)
                bytes = Math.addExact(bytes, read.toLong())
            }
        }
        return VolumeFingerprint(bytes, digest.digest())
    }

    private fun abortPendingOrThrow(operationFailure: Throwable) {
        if (pending.isEmpty()) return
        val residual = mutableListOf<HostOutputTransaction>()
        val failures = mutableListOf<Throwable>()
        pending.values.toList().forEach { transaction ->
            val failure = runCatching { session.abortOutput(transaction.id) }.exceptionOrNull()
            if (failure == null) {
                pending.remove(transaction.displayName)
            } else {
                residual += transaction
                failures += failure
            }
        }
        if (failures.isNotEmpty()) {
            throw ArchiveCreationOutputGroupRollbackException(
                committedOutputs = committed.toList(),
                residualOutputs = residual,
                totalOutputs = expectedOutputCount,
                operationFailure = operationFailure,
                rollbackFailures = failures,
                format = ArchiveFormat.ZIP,
            )
        }
    }

    private class OutputNameConflictException(
        val displayName: String,
        val hostFailure: IllegalArgumentException,
    ) : IllegalStateException("ZIP split output name is unavailable", hostFailure)

    private data class VolumeFingerprint(
        val bytes: Long,
        val sha256: ByteArray,
    )

    private companion object {
        const val ZIP_SUFFIX = ".zip"
        const val SPLIT_PART_MIME_TYPE = "application/octet-stream"
        const val MAX_NUMBERING_ATTEMPTS = 10_000
        const val BUFFER_SIZE = 64 * 1_024
        const val SHA_256 = "SHA-256"
    }
}
