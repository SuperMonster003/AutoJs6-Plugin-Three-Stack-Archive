package io.github.supermonster003.autojs6.plugin.archivemanager

import java.io.File
import java.io.IOException

internal class ArchiveScanner @JvmOverloads constructor(
    private val limits: ArchiveSecurityLimits = ArchiveSecurityLimits.DEFAULT,
    private val engine: ArchiveEngine = ArchiveEngine.DEFAULT,
) {

    @JvmOverloads
    fun scan(
        source: File,
        options: ArchiveReaderOptions = ArchiveReaderOptions(),
        cancellationCheck: () -> Unit = {},
    ): ArchiveSnapshot {
        cancellationCheck()
        if (!source.isFile) {
            throw ArchiveValidationException(
                ArchiveFailureCode.SOURCE_NOT_FILE,
                "Archive source is not a regular file",
            )
        }
        val sourceLength = source.length()
        val sourceLastModifiedMillis = source.lastModified()
        val entries = ArrayList<ArchiveEntry>()
        val pathRegistry = PathRegistry(limits.maxEntries)
        var totalUncompressedBytes = 0L
        var detectedFormat: ArchiveFormat? = null
        var readerOptions: ArchiveReaderOptions? = null

        try {
            cancellationCheck()
            engine.openReader(source, options = options).use { reader ->
                detectedFormat = reader.format
                readerOptions = reader.options
                reader.entries.forEach { readerEntry ->
                    cancellationCheck()
                    if (entries.size >= limits.maxEntries) {
                        fail(
                            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                            "Archive contains more than ${limits.maxEntries} entries",
                        )
                    }

                    val ordinal = entries.size
                    val validatedPath = ArchivePathPolicy.validateEntryPath(
                        sourceName = readerEntry.name,
                        isDirectory = readerEntry.isDirectory,
                        limits = limits,
                    )
                    pathRegistry.register(validatedPath, readerEntry.isDirectory)

                    validateDeclaredMetadata(readerEntry)
                    if (readerEntry.isDirectory && readerEntry.size != 0L) {
                        fail(ArchiveFailureCode.SIZE_MISMATCH, "Directory entry contains file data")
                    }
                    totalUncompressedBytes = checkedMetadataTotal(totalUncompressedBytes, readerEntry.size)

                    entries += ArchiveEntry(
                        path = validatedPath.path,
                        sourceName = readerEntry.name,
                        displayName = validatedPath.displayName,
                        isDirectory = readerEntry.isDirectory,
                        compressionMethod = readerEntry.compressionMethod,
                        compressionMethodId = readerEntry.compressionMethodId,
                        isEncrypted = readerEntry.isEncrypted,
                        capabilities = readerEntry.capabilities,
                        compressedSize = readerEntry.compressedSize,
                        uncompressedSize = readerEntry.size,
                        crc32 = readerEntry.crc,
                        modifiedTimeMillis = readerEntry.time,
                        ordinal = ordinal,
                    )
                }
            }
        } catch (error: ArchiveException) {
            val format = detectedFormat
            if (error.format != null || format == null) throw error
            throw ArchiveValidationException(
                code = error.code,
                message = error.message ?: "Archive validation failed",
                cause = error,
                format = format,
                stage = error.stage,
            )
        } catch (error: ArchiveBackendException) {
            fail(
                code = when (error.failure) {
                    ArchiveBackendFailure.INVALID_SIGNATURE -> ArchiveFailureCode.INVALID_SIGNATURE
                    ArchiveBackendFailure.INVALID_OPTIONS ->
                        ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET
                    ArchiveBackendFailure.UNSUPPORTED_METHOD -> ArchiveFailureCode.UNSUPPORTED_METHOD
                    ArchiveBackendFailure.MALFORMED -> ArchiveFailureCode.MALFORMED_ARCHIVE
                },
                message = when (error.failure) {
                    ArchiveBackendFailure.INVALID_SIGNATURE ->
                        "Archive signature is not recognized"
                    ArchiveBackendFailure.INVALID_OPTIONS ->
                        "Archive filename encoding is not supported"
                    ArchiveBackendFailure.UNSUPPORTED_METHOD ->
                        "Archive contains an unsupported compression method"
                    ArchiveBackendFailure.MALFORMED -> "Archive directory metadata is malformed"
                },
                cause = error,
                format = error.format,
                stage = error.stage,
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
            format = requireNotNull(detectedFormat),
            readerOptions = requireNotNull(readerOptions),
        )
    }

    private fun validateDeclaredMetadata(entry: ArchiveReaderEntry) {
        val size = entry.size
        val compressedSize = entry.compressedSize
        if (size < 0L || compressedSize < 0L) {
            fail(ArchiveFailureCode.MALFORMED_ARCHIVE, "Archive entry has incomplete size metadata")
        }
    }

    private fun checkedMetadataTotal(current: Long, increment: Long): Long = try {
        Math.addExact(current, increment)
    } catch (error: ArithmeticException) {
        fail(ArchiveFailureCode.MALFORMED_ARCHIVE, "Archive size metadata overflows", error)
    }

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
                        "Archive contains duplicate directory paths",
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
                        "Archive contains duplicate directory paths",
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
}

private fun fail(
    code: ArchiveFailureCode,
    message: String,
    cause: Throwable? = null,
    format: ArchiveFormat? = null,
    stage: ArchiveFailureStage = code.defaultStage,
): Nothing = throw ArchiveValidationException(code, message, cause, format, stage)
