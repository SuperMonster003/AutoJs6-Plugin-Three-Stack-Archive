package io.github.supermonster003.autojs6.plugin.archivemanager

import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.CRC32

/** Reopens and verifies one scanned regular entry before exposing its bytes to the host. */
internal class ArchiveEntryStreamer(
    private val source: File,
    private val snapshot: ArchiveSnapshot,
    private val engine: ArchiveEngine = ArchiveEngine.DEFAULT,
) {

    fun stream(
        entry: ArchiveEntry,
        output: OutputStream,
        cancellationCheck: () -> Unit = {},
    ): Long {
        cancellationCheck()
        validateRequestedEntry(entry)
        verifySourceIdentity()

        val measurement = engine.openReader(
            source = source,
            format = snapshot.format,
            options = snapshot.readerOptions,
        ).use { reader ->
            cancellationCheck()
            val liveEntry = reader.entryAt(entry.ordinal)
                ?: changed("Archive entry no longer exists")
            validateCentralEntry(liveEntry, entry)
            reader.openEntry(liveEntry).use { rawInput ->
                val input = BufferedInputStream(rawInput)
                val crc32 = CRC32()
                val buffer = ByteArray(BUFFER_SIZE)
                var bytes = 0L
                while (true) {
                    cancellationCheck()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    if (bytes > entry.uncompressedSize - read.toLong()) {
                        failure(
                            ArchiveFailureCode.SIZE_MISMATCH,
                            "Archive entry expands beyond its scanned size",
                        )
                    }
                    output.write(buffer, 0, read)
                    crc32.update(buffer, 0, read)
                    bytes += read
                }
                output.flush()
                EntryMeasurement(bytes, crc32.value)
            }
        }

        if (measurement.bytes != entry.uncompressedSize) {
            failure(
                ArchiveFailureCode.SIZE_MISMATCH,
                "Archive entry size does not match its scanned size",
            )
        }
        if (entry.crc32 != null && measurement.crc32 != entry.crc32) {
            failure(
                ArchiveFailureCode.CRC_MISMATCH,
                "Archive entry CRC does not match its scanned CRC",
            )
        }
        cancellationCheck()
        verifySourceIdentity()
        return measurement.bytes
    }

    private fun validateRequestedEntry(entry: ArchiveEntry) {
        if (entry.isDirectory) {
            failure(ArchiveFailureCode.UNKNOWN_SELECTION, "Archive entry is a directory")
        }
        if (!entry.canOpen) {
            failure(
                if (entry.isEncrypted) {
                    ArchiveFailureCode.PASSWORD_REQUIRED
                } else {
                    ArchiveFailureCode.UNSUPPORTED_METHOD
                },
                "Archive entry is encrypted or uses an unsupported compression method",
            )
        }
        if (snapshot.entries.getOrNull(entry.ordinal) != entry) {
            changed("Archive entry does not belong to this snapshot")
        }
    }

    private fun validateCentralEntry(
        liveEntry: ArchiveReaderEntry,
        scannedEntry: ArchiveEntry,
    ) {
        val same = liveEntry.name == scannedEntry.sourceName &&
            liveEntry.isDirectory == scannedEntry.isDirectory &&
            liveEntry.compressionMethod == scannedEntry.compressionMethod &&
            liveEntry.compressionMethodId == scannedEntry.compressionMethodId &&
            liveEntry.capabilities == scannedEntry.capabilities &&
            liveEntry.isEncrypted == scannedEntry.isEncrypted &&
            liveEntry.size == scannedEntry.uncompressedSize &&
            liveEntry.compressedSize == scannedEntry.compressedSize &&
            liveEntry.crc == scannedEntry.crc32
        if (!same) changed("Archive central-directory metadata changed")
    }

    private fun verifySourceIdentity() {
        if (source.length() != snapshot.sourceLength ||
            source.lastModified() != snapshot.sourceLastModifiedMillis
        ) {
            changed("Archive source changed after scanning")
        }
    }

    private fun changed(message: String): Nothing = failure(
        ArchiveFailureCode.SOURCE_CHANGED,
        message,
    )

    private fun failure(code: ArchiveFailureCode, message: String): Nothing =
        throw ArchiveExtractionException(
            code = code,
            message = message,
            format = snapshot.format,
            stage = code.defaultStage,
        )

    private data class EntryMeasurement(
        val bytes: Long,
        val crc32: Long,
    )

    private companion object {
        const val BUFFER_SIZE = 32 * 1_024
    }
}
