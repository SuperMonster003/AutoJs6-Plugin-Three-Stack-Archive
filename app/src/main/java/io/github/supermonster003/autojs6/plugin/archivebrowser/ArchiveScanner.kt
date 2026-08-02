package io.github.supermonster003.autojs6.plugin.archivebrowser

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ArchiveScanner(
    private val limits: ArchiveSecurityLimits = ArchiveSecurityLimits.DEFAULT,
) {

    @JvmOverloads
    fun scan(
        source: File,
        cancellationCheck: () -> Unit = {},
    ): ArchiveSnapshot {
        cancellationCheck()
        if (!source.isFile) {
            throw ArchiveValidationException(
                ArchiveFailureCode.SOURCE_NOT_FILE,
                "Archive source is not a regular file",
            )
        }
        verifySignature(source)

        val sourceLength = source.length()
        val sourceLastModifiedMillis = source.lastModified()
        val entries = ArrayList<ArchiveEntry>()
        val pathRegistry = PathRegistry(limits.maxEntries)
        var totalUncompressedBytes = 0L

        try {
            cancellationCheck()
            ZipCentralDirectoryPreflight.validate(source, limits.maxEntries, cancellationCheck)
            cancellationCheck()
            ZipFile(source).use { zipFile ->
                val enumeration = zipFile.entries()
                while (enumeration.hasMoreElements()) {
                    cancellationCheck()
                    if (entries.size >= limits.maxEntries) {
                        fail(
                            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                            "Archive contains more than ${limits.maxEntries} entries",
                        )
                    }

                    val zipEntry = enumeration.nextElement()
                    val ordinal = entries.size
                    val validatedPath = ArchivePathPolicy.validateEntryPath(
                        sourceName = zipEntry.name,
                        isDirectory = zipEntry.isDirectory,
                        limits = limits,
                    )
                    pathRegistry.register(validatedPath, zipEntry.isDirectory)

                    val compressionMethod = compressionMethod(zipEntry)
                    validateDeclaredMetadata(zipEntry, totalUncompressedBytes)
                    val measurement = zipFile.getInputStream(zipEntry).use { input ->
                        measureEntry(
                            input = BufferedInputStream(input),
                            entry = zipEntry,
                            totalBeforeEntry = totalUncompressedBytes,
                            cancellationCheck = cancellationCheck,
                        )
                    }
                    if (zipEntry.isDirectory && measurement.bytes != 0L) {
                        fail(ArchiveFailureCode.SIZE_MISMATCH, "Directory entry contains file data")
                    }
                    totalUncompressedBytes = checkedTotal(totalUncompressedBytes, measurement.bytes)

                    entries += ArchiveEntry(
                        path = validatedPath.path,
                        sourceName = zipEntry.name,
                        displayName = validatedPath.displayName,
                        isDirectory = zipEntry.isDirectory,
                        compressionMethod = compressionMethod,
                        compressedSize = zipEntry.compressedSize,
                        uncompressedSize = measurement.bytes,
                        crc32 = zipEntry.crc.takeIf { it >= 0L },
                        modifiedTimeMillis = zipEntry.time.takeIf { it >= 0L },
                        ordinal = ordinal,
                    )
                }
            }
        } catch (error: ArchiveException) {
            throw error
        } catch (error: ZipException) {
            val unsupportedMethod = error.message.orEmpty().contains("compression method", ignoreCase = true)
            fail(
                if (unsupportedMethod) {
                    ArchiveFailureCode.UNSUPPORTED_METHOD
                } else {
                    ArchiveFailureCode.MALFORMED_ARCHIVE
                },
                if (unsupportedMethod) {
                    "Archive contains an unsupported compression method"
                } else {
                    "Archive is malformed or encrypted"
                },
                error,
            )
        } catch (error: IOException) {
            fail(ArchiveFailureCode.MALFORMED_ARCHIVE, "Archive cannot be read", error)
        } catch (error: IllegalArgumentException) {
            fail(ArchiveFailureCode.MALFORMED_ARCHIVE, "Archive metadata is malformed", error)
        }

        if (source.length() != sourceLength || source.lastModified() != sourceLastModifiedMillis) {
            fail(ArchiveFailureCode.SOURCE_CHANGED, "Archive changed while it was being scanned")
        }
        return ArchiveSnapshot(
            sourceLength = sourceLength,
            sourceLastModifiedMillis = sourceLastModifiedMillis,
            entries = entries.toList(),
            totalUncompressedBytes = totalUncompressedBytes,
            limits = limits,
        )
    }

    private fun verifySignature(source: File) {
        val signature = ByteArray(ZIP_SIGNATURE_SIZE)
        val count = try {
            FileInputStream(source).use { it.read(signature) }
        } catch (error: IOException) {
            fail(ArchiveFailureCode.INVALID_SIGNATURE, "Archive signature cannot be read", error)
        }
        if (count != ZIP_SIGNATURE_SIZE || !SUPPORTED_SIGNATURES.any(signature::contentEquals)) {
            fail(ArchiveFailureCode.INVALID_SIGNATURE, "File does not have a supported ZIP signature")
        }
    }

    private fun compressionMethod(entry: ZipEntry): ArchiveCompressionMethod =
        ArchiveCompressionMethod.entries.firstOrNull { it.zipMethod == entry.method }
            ?: fail(
                ArchiveFailureCode.UNSUPPORTED_METHOD,
                "Archive entry uses unsupported compression method ${entry.method}",
            )

    private fun validateDeclaredMetadata(entry: ZipEntry, totalBeforeEntry: Long) {
        val size = entry.size
        val compressedSize = entry.compressedSize
        if (size < 0L || compressedSize < 0L) {
            fail(ArchiveFailureCode.MALFORMED_ARCHIVE, "Archive entry has incomplete size metadata")
        }
        if (size > limits.maxSingleUncompressedBytes) {
            fail(
                ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
                "Archive entry exceeds the single-entry extraction limit",
            )
        }
        checkedTotal(totalBeforeEntry, size)
        validateCompressionRatio(size, compressedSize)
    }

    private fun measureEntry(
        input: BufferedInputStream,
        entry: ZipEntry,
        totalBeforeEntry: Long,
        cancellationCheck: () -> Unit,
    ): EntryMeasurement {
        val crc32 = CRC32()
        val buffer = ByteArray(BUFFER_SIZE)
        var actualBytes = 0L
        while (true) {
            cancellationCheck()
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            actualBytes = checkedEntrySize(actualBytes, read.toLong())
            checkedTotal(totalBeforeEntry, actualBytes)
            crc32.update(buffer, 0, read)
        }
        if (entry.size >= 0L && actualBytes != entry.size) {
            fail(ArchiveFailureCode.SIZE_MISMATCH, "Archive entry size does not match its data")
        }
        if (entry.crc >= 0L && crc32.value != entry.crc) {
            fail(ArchiveFailureCode.CRC_MISMATCH, "Archive entry CRC does not match its data")
        }
        validateCompressionRatio(actualBytes, entry.compressedSize)
        return EntryMeasurement(actualBytes, crc32.value)
    }

    private fun checkedEntrySize(current: Long, increment: Long): Long {
        if (increment < 0L || current > limits.maxSingleUncompressedBytes - increment) {
            fail(
                ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
                "Archive entry exceeds the single-entry extraction limit",
            )
        }
        return current + increment
    }

    private fun checkedTotal(current: Long, increment: Long): Long {
        if (increment < 0L || current > limits.maxTotalUncompressedBytes - increment) {
            fail(
                ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
                "Archive exceeds the total extraction limit",
            )
        }
        return current + increment
    }

    private fun validateCompressionRatio(uncompressedSize: Long, compressedSize: Long) {
        if (uncompressedSize == 0L) return
        val threshold = if (compressedSize <= 0L) {
            0L
        } else if (compressedSize > Long.MAX_VALUE / limits.maxCompressionRatio) {
            Long.MAX_VALUE
        } else {
            compressedSize * limits.maxCompressionRatio
        }
        if (uncompressedSize > threshold) {
            fail(
                ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
                "Archive entry exceeds the compression-ratio limit",
            )
        }
    }

    private data class EntryMeasurement(
        val bytes: Long,
        val crc32: Long,
    )

    private class PathRegistry(
        private val maxNodes: Int,
    ) {
        private val explicitEntries = HashMap<String, RegisteredPath>()
        private val fileKeys = HashSet<String>()
        private val directoryRepresentatives = HashMap<String, String>()

        fun register(path: ValidatedArchivePath, isDirectory: Boolean) {
            explicitEntries[path.collisionKey]?.let { existing ->
                val code = if (existing.isDirectory == isDirectory) {
                    ArchiveFailureCode.DUPLICATE_PATH
                } else {
                    ArchiveFailureCode.FILE_DIRECTORY_CONFLICT
                }
                fail(code, "Archive contains a duplicate or conflicting path")
            }

            var current = ""
            path.segments.dropLast(1).forEach { segment ->
                current = if (current.isEmpty()) segment else "$current/$segment"
                val key = ArchivePathPolicy.collisionKey(current)
                if (key in fileKeys) {
                    fail(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "Archive file is also used as a directory",
                    )
                }
                val representative = directoryRepresentatives.putIfAbsent(key, current)
                if (representative == null) checkNodeCount()
                if (representative != null && representative != current) {
                    fail(
                        ArchiveFailureCode.DUPLICATE_PATH,
                        "Archive contains case-folding or Unicode-normalization collisions",
                    )
                }
            }

            if (isDirectory) {
                if (path.collisionKey in fileKeys) {
                    fail(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "Archive path is both a file and a directory",
                    )
                }
                val representative = directoryRepresentatives.putIfAbsent(path.collisionKey, path.path)
                if (representative == null) checkNodeCount()
                if (representative != null && representative != path.path) {
                    fail(
                        ArchiveFailureCode.DUPLICATE_PATH,
                        "Archive contains case-folding or Unicode-normalization collisions",
                    )
                }
            } else {
                if (path.collisionKey in directoryRepresentatives) {
                    fail(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "Archive path is both a file and a directory",
                    )
                }
                if (fileKeys.add(path.collisionKey)) checkNodeCount()
            }
            explicitEntries[path.collisionKey] = RegisteredPath(path.path, isDirectory)
        }

        private fun checkNodeCount() {
            if (fileKeys.size + directoryRepresentatives.size > maxNodes) {
                fail(
                    ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                    "Archive expands to more than $maxNodes path nodes",
                )
            }
        }
    }

    private data class RegisteredPath(
        val path: String,
        val isDirectory: Boolean,
    )

    private companion object {
        const val ZIP_SIGNATURE_SIZE = 4
        const val BUFFER_SIZE = 32 * 1_024

        val SUPPORTED_SIGNATURES = listOf(
            byteArrayOf(0x50, 0x4B, 0x03, 0x04),
            byteArrayOf(0x50, 0x4B, 0x05, 0x06),
            byteArrayOf(0x50, 0x4B, 0x07, 0x08),
        )
    }
}

private fun fail(
    code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
): Nothing = throw ArchiveValidationException(code, message, cause)
