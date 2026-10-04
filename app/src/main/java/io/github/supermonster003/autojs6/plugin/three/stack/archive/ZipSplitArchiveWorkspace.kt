package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.annotation.SuppressLint
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.outputstream.SplitOutputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.UUID

internal data class StagedZipVolume(
    val file: File,
    val suffix: String,
    val terminal: Boolean,
) {
    init {
        require(suffix == ZIP_SUFFIX || SPLIT_SUFFIX_PATTERN.matches(suffix))
        require(terminal == (suffix == ZIP_SUFFIX))
    }

    fun outputDisplayName(terminalDisplayName: String): String {
        require(terminalDisplayName.endsWith(ZIP_SUFFIX, ignoreCase = true))
        return if (terminal) {
            terminalDisplayName
        } else {
            terminalDisplayName.dropLast(ZIP_SUFFIX.length) + suffix
        }
    }

    private companion object {
        const val ZIP_SUFFIX = ".zip"
        val SPLIT_SUFFIX_PATTERN = Regex("^\\.z[0-9]+$", RegexOption.IGNORE_CASE)
    }
}

/** A flat, private-cache workspace used only while a split ZIP is being assembled. */
internal class ZipSplitArchiveWorkspace private constructor(
    private val directory: File,
    val terminalFile: File,
    private val splitVolumeSizeBytes: Long,
) : Closeable {

    fun openSplitOutput(): SplitOutputStream {
        ensureCapacity()
        return SplitOutputStream(terminalFile, splitVolumeSizeBytes)
    }

    fun collectVolumes(): List<StagedZipVolume> {
        ensureCapacity()
        val files = ZipFile(terminalFile).use { archive ->
            if (archive.isSplitArchive) {
                archive.splitZipFiles.toList()
            } else {
                // Zip4j emits one ordinary terminal ZIP when the completed output is smaller than
                // the requested split size. This is a valid one-volume result, not a failure.
                listOf(terminalFile)
            }
        }
        require(files.isNotEmpty()) { "ZIP split staging produced no output" }
        require(files.size <= ArchiveSplitVolumePolicy.MAX_VOLUME_FILES) {
            "ZIP split staging produced too many volume files"
        }
        val canonicalDirectory = directory.canonicalFile
        return files.mapIndexed { index, file ->
            val canonicalFile = file.canonicalFile
            require(canonicalFile.parentFile == canonicalDirectory && canonicalFile.isFile) {
                "ZIP split staging returned an invalid volume"
            }
            val suffix = canonicalFile.name.removePrefix(INTERNAL_STEM)
            StagedZipVolume(
                file = canonicalFile,
                suffix = suffix,
                terminal = index == files.lastIndex,
            )
        }.also { volumes ->
            require(volumes.count(StagedZipVolume::terminal) == 1 && volumes.last().terminal) {
                "ZIP split staging did not end with its terminal volume"
            }
            require(volumes.map(StagedZipVolume::suffix).distinct().size == volumes.size) {
                "ZIP split staging returned duplicate volume suffixes"
            }
        }
    }

    @SuppressLint("UsableSpace")
    fun ensureCapacity() {
        val usable = directory.usableSpace
        if (usable < MINIMUM_FREE_CACHE_BYTES) {
            throw ArchiveCreationCacheSpaceException(
                "Insufficient private cache space while creating ZIP volumes",
            )
        }
    }

    override fun close() {
        val canonicalDirectory = directory.canonicalFile
        val failures = mutableListOf<IOException>()
        directory.listFiles()?.forEach { child ->
            val canonicalChild = runCatching { child.canonicalFile }.getOrElse { error ->
                failures += IOException("Cannot resolve a ZIP staging file", error)
                return@forEach
            }
            if (canonicalChild.parentFile != canonicalDirectory || canonicalChild.isDirectory) {
                failures += IOException("ZIP staging workspace contains an unexpected entry")
            } else if (canonicalChild.exists() && !canonicalChild.delete()) {
                failures += IOException("Cannot remove a ZIP staging file")
            }
        }
        if (directory.exists() && !directory.delete()) {
            failures += IOException("Cannot remove the ZIP staging directory")
        }
        if (failures.isNotEmpty()) {
            throw ArchiveCreationCacheCleanupException(failures)
        }
    }

    companion object {
        fun create(
            cacheDirectory: File,
            splitVolumeSizeBytes: Long,
        ): ZipSplitArchiveWorkspace {
            ArchiveSplitVolumePolicy.requireVolumeSizeBytes(splitVolumeSizeBytes)
            val canonicalCacheDirectory = cacheDirectory.canonicalFile
            require(canonicalCacheDirectory.isDirectory) {
                "ZIP staging requires an existing private cache directory"
            }
            cleanupStaleWorkspaces(canonicalCacheDirectory)
            val directory = File(
                canonicalCacheDirectory,
                "$WORKSPACE_PREFIX${UUID.randomUUID()}",
            )
            if (!directory.mkdirs()) {
                throw IOException("Cannot create the ZIP staging directory")
            }
            require(directory.canonicalFile.parentFile == canonicalCacheDirectory) {
                "ZIP staging workspace escaped the private cache directory"
            }
            val workspace = ZipSplitArchiveWorkspace(
                directory = directory,
                terminalFile = File(directory, "$INTERNAL_STEM.zip"),
                splitVolumeSizeBytes = splitVolumeSizeBytes,
            )
            return try {
                workspace.ensureCapacity()
                workspace
            } catch (error: Throwable) {
                runCatching { workspace.close() }.exceptionOrNull()?.let(error::addSuppressed)
                throw error
            }
        }

        private fun cleanupStaleWorkspaces(cacheDirectory: File) {
            val cutoff = System.currentTimeMillis() - STALE_WORKSPACE_AGE_MILLIS
            val canonicalCacheDirectory = runCatching { cacheDirectory.canonicalFile }
                .getOrNull()
                ?: return
            cacheDirectory.listFiles()?.asSequence()
                ?.filter { child ->
                    child.isDirectory &&
                        child.name.startsWith(WORKSPACE_PREFIX) &&
                        child.lastModified() <= cutoff
                }
                ?.forEach { child ->
                    runCatching {
                        val canonicalChild = child.canonicalFile
                        if (canonicalChild.parentFile != canonicalCacheDirectory) return@runCatching
                        canonicalChild.listFiles()?.forEach { stagedFile ->
                            val canonicalStagedFile = stagedFile.canonicalFile
                            if (
                                canonicalStagedFile.parentFile == canonicalChild &&
                                canonicalStagedFile.isFile
                            ) {
                                canonicalStagedFile.delete()
                            }
                        }
                        canonicalChild.delete()
                    }
                }
        }

        private const val INTERNAL_STEM = "staged"
        private const val WORKSPACE_PREFIX = "archive-output-"
        private const val MINIMUM_FREE_CACHE_BYTES = 128L * 1_024L * 1_024L
        private const val STALE_WORKSPACE_AGE_MILLIS = 7L * 24L * 60L * 60L * 1_000L
    }
}

internal class ArchiveCreationCacheSpaceException(message: String) : ArchiveException(
    code = ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE,
    message = message,
    format = ArchiveFormat.ZIP,
    stage = ArchiveFailureStage.OUTPUT,
)

internal class ArchiveCreationCacheCleanupException(
    val cleanupFailures: List<IOException>,
) : ArchiveException(
    code = ArchiveFailureCode.OUTPUT_FAILURE,
    message = "ZIP split staging cache could not be removed",
    cause = cleanupFailures.first(),
    format = ArchiveFormat.ZIP,
    stage = ArchiveFailureStage.CLEANUP,
) {
    init {
        require(cleanupFailures.isNotEmpty())
        cleanupFailures.drop(1).forEach(::addSuppressed)
    }
}
