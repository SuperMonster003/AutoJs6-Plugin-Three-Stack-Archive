package io.github.supermonster003.autojs6.plugin.archivebrowser

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

internal data class StagedArchive(
    val file: File,
    val bytes: Long,
) {
    fun delete() {
        runCatching { file.delete() }
        file.parentFile?.takeIf { it.name.startsWith("archive-input-") }?.let { parent ->
            runCatching { parent.delete() }
        }
    }
}

internal object ArchiveCacheStager {

    suspend fun stage(
        contentResolver: ContentResolver,
        source: Uri,
        cacheDirectory: File,
        declaredSize: Long,
        onProgress: (Long) -> Unit = {},
    ): StagedArchive {
        require(source.scheme == ContentResolver.SCHEME_CONTENT) { "Archive URI must use content scheme" }
        if (declaredSize !in 0L..MAX_ARCHIVE_BYTES) {
            throw ArchiveInputLimitException("Archive exceeds the input size limit")
        }
        cleanupStaleInputs(cacheDirectory)

        val available = cacheDirectory.usableSpace
            .takeIf { it > 0L }
            ?.minus(MINIMUM_FREE_CACHE_BYTES)
            ?.coerceAtLeast(0L)
            ?: MAX_ARCHIVE_BYTES
        val copyLimit = minOf(MAX_ARCHIVE_BYTES, available)
        if (declaredSize > copyLimit) {
            throw ArchiveInputLimitException("Insufficient cache storage for the archive")
        }

        val directory = File(cacheDirectory, "$INPUT_DIRECTORY_PREFIX${UUID.randomUUID()}")
        if (!directory.mkdirs()) throw IOException("Cannot create the archive cache directory")
        val target = File(directory, "source.zip")

        try {
            val input = contentResolver.openInputStream(source)
                ?: throw IOException("Cannot open the archive URI")
            var copied = 0L
            var lastReported = 0L
            BufferedInputStream(input).use { sourceStream ->
                BufferedOutputStream(FileOutputStream(target)).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = sourceStream.read(buffer)
                        if (read < 0) break
                        copied = Math.addExact(copied, read.toLong())
                        if (copied > declaredSize) {
                            throw IOException("Archive size changed while it was being staged")
                        }
                        if (copied > copyLimit) {
                            throw ArchiveInputLimitException("Archive exceeds the input size limit")
                        }
                        output.write(buffer, 0, read)
                        if (copied - lastReported >= PROGRESS_REPORT_BYTES) {
                            lastReported = copied
                            onProgress(copied)
                        }
                    }
                }
            }
            if (lastReported != copied) onProgress(copied)
            if (copied != declaredSize) {
                throw IOException("Archive size changed while it was being staged")
            }
            return StagedArchive(target, copied)
        } catch (error: Throwable) {
            runCatching { target.delete() }
            runCatching { directory.delete() }
            throw error
        }
    }

    const val MAX_ARCHIVE_BYTES = 4L * 1024L * 1024L * 1024L
    private fun cleanupStaleInputs(cacheDirectory: File) {
        val cutoff = System.currentTimeMillis() - STALE_INPUT_AGE_MILLIS
        cacheDirectory.listFiles()?.asSequence()
            ?.filter { child ->
                child.isDirectory &&
                    child.name.startsWith(INPUT_DIRECTORY_PREFIX) &&
                    child.lastModified() <= cutoff
            }
            ?.forEach { child -> runCatching { child.deleteRecursively() } }
    }

    private const val INPUT_DIRECTORY_PREFIX = "archive-input-"
    private const val MINIMUM_FREE_CACHE_BYTES = 128L * 1024L * 1024L
    private const val PROGRESS_REPORT_BYTES = 8L * 1024L * 1024L
    private const val STALE_INPUT_AGE_MILLIS = 7L * 24L * 60L * 60L * 1_000L
}

internal class ArchiveInputLimitException(message: String) : IOException(message)
