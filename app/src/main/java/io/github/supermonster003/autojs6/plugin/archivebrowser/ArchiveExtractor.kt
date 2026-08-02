package io.github.supermonster003.autojs6.plugin.archivebrowser

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.min

class ArchiveExtractor @JvmOverloads constructor(
    private val contentResolver: ContentResolver? = null,
    private val extractionLimits: ArchiveSecurityLimits? = null,
) {

    suspend fun extract(
        source: File,
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        treeUri: Uri,
        rootName: String,
        progress: ArchiveProgressListener = ArchiveProgressListener.NONE,
    ): ExtractionResult {
        val resolver = contentResolver ?: throw IllegalStateException(
            "ArchiveExtractor requires a ContentResolver for SAF extraction",
        )
        val writer = try {
            SafArchiveOutputWriter(resolver, treeUri)
        } catch (error: RuntimeException) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.OUTPUT_FAILURE,
                "Extraction destination is invalid",
                error,
            )
        }
        return extractToWriter(source, snapshot, selectedPaths, rootName, writer, progress)
    }

    suspend fun extractToWriter(
        source: File,
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        rootName: String,
        writer: ArchiveOutputWriter,
        progress: ArchiveProgressListener = ArchiveProgressListener.NONE,
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val extractionContext = currentCoroutineContext()
        val limits = extractionLimits?.let { snapshot.limits.restrictedBy(it) } ?: snapshot.limits
        val safeRootName = ArchivePathPolicy.validateDestinationRootName(rootName, limits)
        validateSnapshot(source, snapshot, limits)
        val selection = ArchiveSelection.resolve(snapshot, selectedPaths)
        validateSelection(selection, limits)
        preflightCentralDirectory(source, snapshot, limits.maxEntries) {
            extractionContext.ensureActive()
        }

        progress.onProgress(
            ExtractionProgress(
                phase = ExtractionPhase.PREPARING,
                currentPath = null,
                completedEntries = 0,
                totalEntries = selection.totalEntries,
                bytesWritten = 0L,
                totalBytes = selection.totalUncompressedBytes,
            ),
        )
        currentCoroutineContext().ensureActive()

        var root: ArchiveOutputWriter.Node? = null
        try {
            root = writer.createRoot(safeRootName)
            currentCoroutineContext().ensureActive()
            val directories = HashMap<String, ArchiveOutputWriter.Node>()
            directories[ArchivePathPolicy.ROOT_PATH] = root
            var completedEntries = 0
            var bytesWritten = 0L

            selection.directories.forEach { directoryPath ->
                currentCoroutineContext().ensureActive()
                val parent = directories[parentPath(directoryPath)]
                    ?: extractionFailure("Extraction directory parent is missing")
                val directory = writer.createDirectory(parent, displayName(directoryPath))
                currentCoroutineContext().ensureActive()
                directories[directoryPath] = directory
                completedEntries++
                progress.onProgress(
                    ExtractionProgress(
                        phase = ExtractionPhase.EXTRACTING,
                        currentPath = directoryPath,
                        completedEntries = completedEntries,
                        totalEntries = selection.totalEntries,
                        bytesWritten = bytesWritten,
                        totalBytes = selection.totalUncompressedBytes,
                    ),
                )
            }

            validateCentralDirectoryStructure(source, snapshot, limits.maxEntries) {
                extractionContext.ensureActive()
            }
            ZipFile(source).use { zipFile ->
                selection.files.forEach { entry ->
                    currentCoroutineContext().ensureActive()
                    val zipEntry = zipFile.getEntry(entry.sourceName)
                        ?: changed("Selected archive entry no longer exists")
                    validateCentralEntry(zipEntry, entry)
                    val parent = directories[parentPath(entry.path)]
                        ?: extractionFailure("Extraction file parent is missing")
                    val outputNode = writer.createFile(parent, entry.displayName)
                    val measurement = writer.openFile(outputNode).use { rawOutput ->
                        zipFile.getInputStream(zipEntry).use { rawInput ->
                            copyEntry(
                                input = BufferedInputStream(rawInput),
                                output = BufferedOutputStream(rawOutput),
                                entry = entry,
                                compressedSize = zipEntry.compressedSize,
                                totalBeforeEntry = bytesWritten,
                                totalExpected = selection.totalUncompressedBytes,
                                completedEntries = completedEntries,
                                totalEntries = selection.totalEntries,
                                limits = limits,
                                progress = progress,
                            )
                        }
                    }
                    bytesWritten = checkedAdd(bytesWritten, measurement.bytes, limits.maxTotalUncompressedBytes) {
                        ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED
                    }
                    validateMeasurement(entry, zipEntry, measurement, limits)
                    completedEntries++
                    progress.onProgress(
                        ExtractionProgress(
                            phase = ExtractionPhase.EXTRACTING,
                            currentPath = entry.path,
                            completedEntries = completedEntries,
                            totalEntries = selection.totalEntries,
                            bytesWritten = bytesWritten,
                            totalBytes = selection.totalUncompressedBytes,
                        ),
                    )
                }
            }

            currentCoroutineContext().ensureActive()
            verifySourceIdentity(source, snapshot)
            val result = ExtractionResult(
                root = root.location,
                filesExtracted = selection.files.size,
                directoriesCreated = selection.directories.size,
                bytesWritten = bytesWritten,
            )
            progress.onProgress(
                ExtractionProgress(
                    phase = ExtractionPhase.COMPLETED,
                    currentPath = null,
                    completedEntries = selection.totalEntries,
                    totalEntries = selection.totalEntries,
                    bytesWritten = bytesWritten,
                    totalBytes = selection.totalUncompressedBytes,
                ),
            )
            result
        } catch (error: Throwable) {
            val mapped = mapExtractionFailure(error)
            root?.let { createdRoot ->
                val cleanupFailure = withContext(NonCancellable) {
                    val notificationFailure = runCatching {
                        progress.onProgress(
                            ExtractionProgress(
                                phase = ExtractionPhase.CLEANING_UP,
                                currentPath = null,
                                completedEntries = 0,
                                totalEntries = selection.totalEntries,
                                bytesWritten = 0L,
                                totalBytes = selection.totalUncompressedBytes,
                            ),
                        )
                    }.exceptionOrNull()
                    val deletionFailure = runCatching {
                        writer.deleteRoot(createdRoot)
                    }.exceptionOrNull()
                    deletionFailure?.also { failure ->
                        notificationFailure?.let(failure::addSuppressed)
                    } ?: notificationFailure
                }
                cleanupFailure?.let(mapped::addSuppressed)
            }
            throw mapped
        }
    }

    private suspend fun copyEntry(
        input: BufferedInputStream,
        output: BufferedOutputStream,
        entry: ArchiveEntry,
        compressedSize: Long,
        totalBeforeEntry: Long,
        totalExpected: Long,
        completedEntries: Int,
        totalEntries: Int,
        limits: ArchiveSecurityLimits,
        progress: ArchiveProgressListener,
    ): EntryMeasurement {
        val crc32 = CRC32()
        val buffer = ByteArray(BUFFER_SIZE)
        var entryBytes = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            entryBytes = checkedAdd(entryBytes, read.toLong(), limits.maxSingleUncompressedBytes) {
                ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED
            }
            val totalBytes = checkedAdd(totalBeforeEntry, entryBytes, limits.maxTotalUncompressedBytes) {
                ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED
            }
            validateCompressionRatio(entryBytes, compressedSize, limits)
            output.write(buffer, 0, read)
            crc32.update(buffer, 0, read)
            progress.onProgress(
                ExtractionProgress(
                    phase = ExtractionPhase.EXTRACTING,
                    currentPath = entry.path,
                    completedEntries = completedEntries,
                    totalEntries = totalEntries,
                    bytesWritten = totalBytes,
                    totalBytes = totalExpected,
                ),
            )
        }
        output.flush()
        return EntryMeasurement(entryBytes, crc32.value)
    }

    private fun validateSnapshot(
        source: File,
        snapshot: ArchiveSnapshot,
        limits: ArchiveSecurityLimits,
    ) {
        if (!source.isFile) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SOURCE_NOT_FILE,
                "Archive source is not a regular file",
            )
        }
        verifySourceIdentity(source, snapshot)
        if (snapshot.entries.size > limits.maxEntries) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                "Archive snapshot exceeds the entry limit",
            )
        }
        var total = 0L
        snapshot.entries.forEach { entry ->
            val validated = ArchivePathPolicy.validateEntryPath(entry.sourceName, entry.isDirectory, limits)
            if (validated.path != entry.path || validated.displayName != entry.displayName) {
                changed("Archive snapshot path metadata is inconsistent")
            }
            if (entry.compressionMethod.zipMethod !in SUPPORTED_METHODS) {
                throw ArchiveExtractionException(
                    ArchiveFailureCode.UNSUPPORTED_METHOD,
                    "Archive snapshot contains an unsupported compression method",
                )
            }
            if (entry.uncompressedSize < 0L || entry.uncompressedSize > limits.maxSingleUncompressedBytes) {
                throw ArchiveExtractionException(
                    ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
                    "Archive snapshot exceeds the single-entry limit",
                )
            }
            validateCompressionRatio(entry.uncompressedSize, entry.compressedSize, limits)
            total = checkedAdd(total, entry.uncompressedSize, limits.maxTotalUncompressedBytes) {
                ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED
            }
        }
        if (total != snapshot.totalUncompressedBytes) {
            changed("Archive snapshot total size is inconsistent")
        }
    }

    private fun validateSelection(
        selection: ResolvedArchiveSelection,
        limits: ArchiveSecurityLimits,
    ) {
        var total = 0L
        selection.files.forEach { entry ->
            total = checkedAdd(total, entry.uncompressedSize, limits.maxTotalUncompressedBytes) {
                ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED
            }
        }
        if (total != selection.totalUncompressedBytes) {
            changed("Archive selection size is inconsistent")
        }
    }

    private fun preflightCentralDirectory(
        source: File,
        snapshot: ArchiveSnapshot,
        maxEntries: Int,
        cancellationCheck: () -> Unit,
    ) {
        try {
            validateCentralDirectoryStructure(source, snapshot, maxEntries, cancellationCheck)
            ZipFile(source).use { zipFile ->
                val enumeration = zipFile.entries()
                var ordinal = 0
                while (enumeration.hasMoreElements()) {
                    if (ordinal % PREFLIGHT_CANCELLATION_CHECK_INTERVAL == 0) cancellationCheck()
                    if (ordinal >= snapshot.entries.size) changed("Archive entry count changed")
                    validateCentralEntry(enumeration.nextElement(), snapshot.entries[ordinal])
                    ordinal++
                }
                if (ordinal != snapshot.entries.size) changed("Archive entry count changed")
            }
        } catch (error: ArchiveException) {
            throw error
        } catch (error: IOException) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SOURCE_CHANGED,
                "Archive cannot be reopened for extraction",
                error,
            )
        }
    }

    private fun validateCentralDirectoryStructure(
        source: File,
        snapshot: ArchiveSnapshot,
        maxEntries: Int,
        cancellationCheck: () -> Unit,
    ) {
        val directory = try {
            ZipCentralDirectoryPreflight.validate(source, maxEntries, cancellationCheck)
        } catch (error: ArchiveValidationException) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SOURCE_CHANGED,
                "Archive central-directory structure changed after scanning",
                error,
            )
        }
        if (directory.entryCount != snapshot.entries.size) {
            changed("Archive entry count changed")
        }
    }

    private fun validateCentralEntry(zipEntry: ZipEntry, snapshotEntry: ArchiveEntry) {
        if (zipEntry.method !in SUPPORTED_METHODS) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "Archive entry uses an unsupported compression method",
            )
        }
        val same = zipEntry.name == snapshotEntry.sourceName &&
            zipEntry.isDirectory == snapshotEntry.isDirectory &&
            zipEntry.method == snapshotEntry.compressionMethod.zipMethod &&
            zipEntry.size == snapshotEntry.uncompressedSize &&
            zipEntry.compressedSize == snapshotEntry.compressedSize &&
            zipEntry.crc.takeIf { it >= 0L } == snapshotEntry.crc32
        if (!same) changed("Archive central-directory metadata changed")
    }

    private fun validateMeasurement(
        entry: ArchiveEntry,
        zipEntry: ZipEntry,
        measurement: EntryMeasurement,
        limits: ArchiveSecurityLimits,
    ) {
        if (measurement.bytes != entry.uncompressedSize || measurement.bytes != zipEntry.size) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SIZE_MISMATCH,
                "Extracted entry size does not match the scanned size",
            )
        }
        if (entry.crc32 != null && measurement.crc32 != entry.crc32) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.CRC_MISMATCH,
                "Extracted entry CRC does not match the scanned CRC",
            )
        }
        validateCompressionRatio(measurement.bytes, zipEntry.compressedSize, limits)
    }

    private fun validateCompressionRatio(
        uncompressedSize: Long,
        compressedSize: Long,
        limits: ArchiveSecurityLimits,
    ) {
        if (uncompressedSize == 0L) return
        val threshold = if (compressedSize <= 0L) {
            0L
        } else if (compressedSize > Long.MAX_VALUE / limits.maxCompressionRatio) {
            Long.MAX_VALUE
        } else {
            compressedSize * limits.maxCompressionRatio
        }
        if (uncompressedSize > threshold) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
                "Archive entry exceeds the compression-ratio limit",
            )
        }
    }

    private fun verifySourceIdentity(source: File, snapshot: ArchiveSnapshot) {
        if (source.length() != snapshot.sourceLength ||
            source.lastModified() != snapshot.sourceLastModifiedMillis
        ) {
            changed("Archive source changed after scanning")
        }
    }

    private fun ArchiveSecurityLimits.restrictedBy(other: ArchiveSecurityLimits) = ArchiveSecurityLimits(
        maxEntries = min(maxEntries, other.maxEntries),
        maxPathLength = min(maxPathLength, other.maxPathLength),
        maxDepth = min(maxDepth, other.maxDepth),
        maxSingleUncompressedBytes = min(maxSingleUncompressedBytes, other.maxSingleUncompressedBytes),
        maxTotalUncompressedBytes = min(maxTotalUncompressedBytes, other.maxTotalUncompressedBytes),
        maxCompressionRatio = min(maxCompressionRatio, other.maxCompressionRatio),
    )

    private fun checkedAdd(
        current: Long,
        increment: Long,
        limit: Long,
        code: () -> ArchiveFailureCode,
    ): Long {
        if (increment < 0L || current > limit - increment) {
            throw ArchiveExtractionException(code(), "Archive extraction limit exceeded")
        }
        return current + increment
    }

    private fun parentPath(path: String): String = path.substringBeforeLast('/', "")

    private fun displayName(path: String): String = path.substringAfterLast('/')

    private fun changed(message: String): Nothing = throw ArchiveExtractionException(
        ArchiveFailureCode.SOURCE_CHANGED,
        message,
    )

    private fun extractionFailure(message: String): Nothing = throw ArchiveExtractionException(
        ArchiveFailureCode.OUTPUT_FAILURE,
        message,
    )

    private fun mapExtractionFailure(error: Throwable): Throwable = when (error) {
        is CancellationException -> error
        is ArchiveException -> error
        else -> ArchiveExtractionException(
            ArchiveFailureCode.OUTPUT_FAILURE,
            "Archive extraction failed",
            error,
        )
    }

    private data class EntryMeasurement(
        val bytes: Long,
        val crc32: Long,
    )

    private companion object {
        const val BUFFER_SIZE = 32 * 1_024
        const val PREFLIGHT_CANCELLATION_CHECK_INTERVAL = 64
        val SUPPORTED_METHODS = setOf(ZipEntry.STORED, ZipEntry.DEFLATED)
    }
}
