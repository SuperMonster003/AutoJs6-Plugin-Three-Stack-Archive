package io.github.supermonster003.autojs6.plugin.archivemanager

import java.io.File
import java.io.IOException
import java.util.zip.ZipException

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
        val sourceLength = source.length()
        val sourceLastModifiedMillis = source.lastModified()
        val entries = ArrayList<ArchiveEntry>()
        val pathRegistry = PathRegistry(limits.maxEntries)
        var totalUncompressedBytes = 0L
        var zipCharsetName: String? = null

        try {
            cancellationCheck()
            val zipCharset = ZipArchiveAccess.detectCharset(source)
            zipCharsetName = zipCharset.name()
            ZipArchiveAccess.open(source, zipCharsetName).use { zipFile ->
                zipFile.entries.forEach { zipEntry ->
                    cancellationCheck()
                    if (entries.size >= limits.maxEntries) {
                        fail(
                            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                            "Archive contains more than ${limits.maxEntries} entries",
                        )
                    }

                    val ordinal = entries.size
                    val validatedPath = ArchivePathPolicy.validateEntryPath(
                        sourceName = zipEntry.name,
                        isDirectory = zipEntry.isDirectory,
                        limits = limits,
                    )
                    pathRegistry.register(validatedPath, zipEntry.isDirectory)

                    val compressionMethod = ArchiveCompressionMethod.fromZipMethod(zipEntry.method)
                    validateDeclaredMetadata(zipEntry)
                    if (zipEntry.isDirectory && zipEntry.size != 0L) {
                        fail(ArchiveFailureCode.SIZE_MISMATCH, "Directory entry contains file data")
                    }
                    totalUncompressedBytes = checkedMetadataTotal(totalUncompressedBytes, zipEntry.size)

                    entries += ArchiveEntry(
                        path = validatedPath.path,
                        sourceName = zipEntry.name,
                        displayName = validatedPath.displayName,
                        isDirectory = zipEntry.isDirectory,
                        compressionMethod = compressionMethod,
                        zipMethod = zipEntry.method,
                        isEncrypted = zipEntry.isEncrypted,
                        canExtract = zipEntry.canExtract,
                        compressedSize = zipEntry.compressedSize,
                        uncompressedSize = zipEntry.size,
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
            zipCharsetName = zipCharsetName,
        )
    }

    private fun validateDeclaredMetadata(entry: ZipEntryMetadata) {
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
): Nothing = throw ArchiveValidationException(code, message, cause)
