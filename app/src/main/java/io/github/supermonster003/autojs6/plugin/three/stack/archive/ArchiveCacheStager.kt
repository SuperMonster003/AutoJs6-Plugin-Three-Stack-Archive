package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private const val ARCHIVE_INPUT_DIRECTORY_PREFIX = "archive-input-"

internal enum class ArchiveInputStorage {
    SEEKABLE_DESCRIPTOR,
    PRIVATE_CACHE,
}

internal class StagedArchive(
    val source: ArchiveReadSource,
    val bytes: Long,
    val storage: ArchiveInputStorage = ArchiveInputStorage.PRIVATE_CACHE,
    private val descriptorLease: ParcelFileDescriptor? = null,
    private val privateCacheFiles: List<File>? = null,
    private val onClosed: () -> Unit = {},
) : Closeable {
    private val closed = AtomicBoolean(false)

    init {
        require(bytes >= 0L)
        require((storage == ArchiveInputStorage.SEEKABLE_DESCRIPTOR) == (descriptorLease != null))
        require(privateCacheFiles == null || storage == ArchiveInputStorage.PRIVATE_CACHE)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            runCatching { descriptorLease?.close() }
            if (storage == ArchiveInputStorage.PRIVATE_CACHE) {
                val localFile = requireNotNull(source.localFile)
                (privateCacheFiles ?: listOf(localFile)).forEach { file ->
                    runCatching { file.delete() }
                }
                localFile.parentFile
                    ?.takeIf { it.name.startsWith(ARCHIVE_INPUT_DIRECTORY_PREFIX) }
                    ?.let { parent ->
                        runCatching { parent.delete() }
                    }
            }
        } finally {
            onClosed()
        }
    }
}

internal object ArchiveCacheStager {

    private val activeCacheDirectories = Collections.synchronizedSet(mutableSetOf<String>())

    fun stage(
        source: ParcelFileDescriptor,
        cacheDirectory: File,
        reportedSize: Long,
    ): StagedArchive = stageDescriptor(
        source = source,
        cacheDirectory = cacheDirectory,
        reportedSize = reportedSize,
        checkCancelled = {},
        onProgress = {},
    )

    suspend fun stage(
        source: ParcelFileDescriptor,
        cacheDirectory: File,
        reportedSize: Long,
        onProgress: (Long) -> Unit,
    ): StagedArchive {
        val coroutineContext = currentCoroutineContext()
        return stageDescriptor(
            source = source,
            cacheDirectory = cacheDirectory,
            reportedSize = reportedSize,
            checkCancelled = coroutineContext::ensureActive,
            onProgress = onProgress,
        )
    }

    /** Supplies a deterministic capacity ceiling to device tests; production callers use [stage]. */
    fun stageWithCopyLimitForTesting(
        source: ParcelFileDescriptor,
        cacheDirectory: File,
        reportedSize: Long,
        copyLimitBytes: Long,
    ): StagedArchive {
        require(copyLimitBytes >= 0L)
        return stageDescriptor(
            source = source,
            cacheDirectory = cacheDirectory,
            reportedSize = reportedSize,
            checkCancelled = {},
            onProgress = {},
            copyLimitOverride = copyLimitBytes,
        )
    }

    private fun stageDescriptor(
        source: ParcelFileDescriptor,
        cacheDirectory: File,
        reportedSize: Long,
        checkCancelled: () -> Unit,
        onProgress: (Long) -> Unit,
        copyLimitOverride: Long? = null,
    ): StagedArchive {
        if (!ArchiveIntentPolicy.isReportedSizeAccepted(reportedSize)) {
            source.close()
            throw IllegalArgumentException("Archive size is invalid")
        }
        cleanupStaleInputs(cacheDirectory)
        try {
            tryLeaseSeekableDescriptor(source, reportedSize)?.let { direct ->
                onProgress(direct.bytes)
                return direct
            }
            resetToStartIfSeekable(source)
            return copyToPrivateCache(
                source = ParcelFileDescriptor.AutoCloseInputStream(source),
                cacheDirectory = cacheDirectory,
                reportedSize = reportedSize,
                checkCancelled = checkCancelled,
                onProgress = onProgress,
                copyLimitOverride = copyLimitOverride,
            )
        } catch (error: Throwable) {
            runCatching { source.close() }
            throw error
        }
    }

    suspend fun stage(
        contentResolver: ContentResolver,
        source: Uri,
        cacheDirectory: File,
        reportedSize: Long,
        onProgress: (Long) -> Unit = {},
    ): StagedArchive {
        require(source.scheme == ContentResolver.SCHEME_CONTENT) { "Archive URI must use content scheme" }
        if (!ArchiveIntentPolicy.isReportedSizeAccepted(reportedSize)) {
            throw IllegalArgumentException("Archive size is invalid")
        }
        val descriptor = contentResolver.openFileDescriptor(source, READ_MODE)
        if (descriptor != null) {
            return stage(descriptor, cacheDirectory, reportedSize, onProgress)
        }
        cleanupStaleInputs(cacheDirectory)
        val coroutineContext = currentCoroutineContext()
        val input = contentResolver.openInputStream(source)
            ?: throw IOException("Cannot open the archive URI")
        return copyToPrivateCache(
            source = input,
            cacheDirectory = cacheDirectory,
            reportedSize = reportedSize,
            checkCancelled = coroutineContext::ensureActive,
            onProgress = onProgress,
        )
    }

    private fun tryLeaseSeekableDescriptor(
        source: ParcelFileDescriptor,
        reportedSize: Long,
    ): StagedArchive? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val candidate = try {
            val descriptor = source.fileDescriptor
            val flags = readDescriptorFlags(source) ?: return null
            if (flags and OsConstants.O_ACCMODE != OsConstants.O_RDONLY) return null
            val stat = Os.fstat(descriptor)
            if (!OsConstants.S_ISREG(stat.st_mode)) return null
            Os.lseek(descriptor, 0L, OsConstants.SEEK_CUR)
            val directSource = DescriptorArchiveReadSource(descriptor)
            val verifiedSize = directSource.identity().length
            if (verifiedSize > 0L) {
                val probe = ByteArray(1)
                if (Os.pread(descriptor, probe, 0, 1, 0L) != 1) {
                    throw IOException("Cannot read the archive descriptor")
                }
                if (Os.pread(descriptor, probe, 0, 1, verifiedSize - 1L) != 1) {
                    throw IOException("Cannot seek the archive descriptor")
                }
            }
            DirectDescriptorCandidate(directSource, stat.st_size, verifiedSize)
        } catch (_: ErrnoException) {
            return null
        } catch (_: IOException) {
            return null
        } catch (_: SecurityException) {
            return null
        }
        if (candidate.statSize < 0L || candidate.statSize != candidate.verifiedSize) {
            throw IOException("Archive changed while its descriptor was being verified")
        }
        if (reportedSize >= 0L && candidate.verifiedSize != reportedSize) {
            throw IOException("Archive changed before it was opened")
        }
        return StagedArchive(
            source = candidate.source,
            bytes = candidate.verifiedSize,
            storage = ArchiveInputStorage.SEEKABLE_DESCRIPTOR,
            descriptorLease = source,
        )
    }

    fun materialize(
        staged: StagedArchive,
        cacheDirectory: File,
    ): StagedArchive = materializeDescriptorSource(
        staged = staged,
        cacheDirectory = cacheDirectory,
        checkCancelled = {},
        onProgress = {},
    )

    /** Copies a virtual seekable source, including numbered volumes, into one private file. */
    fun materialize(
        source: ArchiveReadSource,
        cacheDirectory: File,
    ): StagedArchive {
        val identityBeforeCopy = source.inputIdentity()
        cleanupStaleInputs(cacheDirectory)
        val materialized = copyToPrivateCache(
            source = source.openInputStream(),
            cacheDirectory = cacheDirectory,
            reportedSize = identityBeforeCopy.primary.length,
            checkCancelled = {},
            onProgress = {},
        )
        return try {
            if (source.inputIdentity() != identityBeforeCopy) {
                throw ArchiveVolumeChangedException(
                    "Archive volume set changed while it was being materialized",
                )
            }
            materialized
        } catch (error: Throwable) {
            materialized.close()
            throw error
        }
    }

    /** Copies one complete, host-authorized volume group into a single private directory. */
    fun materializeVolumeGroup(
        primary: StagedArchive,
        primaryDisplayName: String,
        volumeSet: ArchiveVolumeSet,
        requiredVolumeNames: List<String>,
        cacheDirectory: File,
    ): StagedArchive? {
        val names = listOf(primaryDisplayName) + requiredVolumeNames
        require(names.size in 2..ExplorerActionProtocol.MAX_ARCHIVE_VOLUMES) {
            "Archive volume group size is invalid"
        }
        names.forEach(::requireSafeVolumeName)
        require(names.map(::collisionKey).distinct().size == names.size) {
            "Archive volume group contains an ambiguous name"
        }
        val expectedVolumeIdentities = volumeSet.inspectIdentities()
        val volumeMetadata = expectedVolumeIdentities.associateBy { collisionKey(it.displayName) }
        val required = requiredVolumeNames.map { name ->
            val metadata = volumeMetadata[collisionKey(name)] ?: return null
            val source = volumeSet.openSource(metadata.displayName) ?: return null
            VolumeCopyInput(metadata.displayName, metadata.length, source)
        }
        val primaryIdentity = primary.source.identity()
        val inputs = listOf(
            VolumeCopyInput(primaryDisplayName, primaryIdentity.length, primary.source),
        ) + required
        val copyLimit = cacheDirectory.copyLimit()
        val expectedTotal = inputs.fold(0L) { total, input ->
            try {
                Math.addExact(total, input.expectedSize)
            } catch (error: ArithmeticException) {
                throw ArchiveInputLimitException("Archive volume group size overflows")
            }
        }
        if (expectedTotal > copyLimit) {
            throw ArchiveInputLimitException("Insufficient cache storage for the archive volumes")
        }

        cleanupStaleInputs(cacheDirectory)
        val directory = File(cacheDirectory, "$ARCHIVE_INPUT_DIRECTORY_PREFIX${UUID.randomUUID()}")
        if (!directory.mkdirs()) {
            throw IOException("Cannot create the archive volume cache directory")
        }
        val created = ArrayList<File>(inputs.size)
        try {
            var copiedTotal = 0L
            inputs.forEach { input ->
                val target = File(directory, input.displayName)
                created += target
                val copied = copyExactVolume(input.source, target, copyLimit - copiedTotal)
                if (copied != input.expectedSize) {
                    throw ArchiveVolumeChangedException(
                        "Archive volume changed while it was being staged",
                    )
                }
                copiedTotal = Math.addExact(copiedTotal, copied)
            }
            if (volumeSet.inspectIdentities() != expectedVolumeIdentities) {
                throw ArchiveVolumeChangedException(
                    "Archive volume catalog changed while it was being staged",
                )
            }
            val primaryFile = created.first()
            val companionSources = created.drop(1).associate { file ->
                collisionKey(file.name) to file.asArchiveReadSource()
            }
            val localVolumeSet = LocalArchiveVolumeSet(directory, companionSources)
            val source = VolumeAwareArchiveReadSource(
                source = primaryFile.asArchiveReadSource(),
                displayName = primaryDisplayName,
                volumeSet = localVolumeSet,
            )
            val cacheKey = directory.absolutePath
            activeCacheDirectories += cacheKey
            return StagedArchive(
                source = source,
                bytes = primaryIdentity.length,
                storage = ArchiveInputStorage.PRIVATE_CACHE,
                privateCacheFiles = created.toList(),
                onClosed = { activeCacheDirectories -= cacheKey },
            )
        } catch (error: Throwable) {
            created.forEach { file -> runCatching { file.delete() } }
            runCatching { directory.delete() }
            throw error
        }
    }

    suspend fun materialize(
        staged: StagedArchive,
        cacheDirectory: File,
        onProgress: (Long) -> Unit,
    ): StagedArchive {
        val coroutineContext = currentCoroutineContext()
        return materializeDescriptorSource(
            staged = staged,
            cacheDirectory = cacheDirectory,
            checkCancelled = coroutineContext::ensureActive,
            onProgress = onProgress,
        )
    }

    private fun materializeDescriptorSource(
        staged: StagedArchive,
        cacheDirectory: File,
        checkCancelled: () -> Unit,
        onProgress: (Long) -> Unit,
    ): StagedArchive {
        require(staged.storage == ArchiveInputStorage.SEEKABLE_DESCRIPTOR) {
            "Only a descriptor-backed archive can be materialized"
        }
        cleanupStaleInputs(cacheDirectory)
        checkCancelled()
        return copyToPrivateCache(
            source = staged.source.openInputStream(),
            cacheDirectory = cacheDirectory,
            reportedSize = staged.bytes,
            checkCancelled = checkCancelled,
            onProgress = onProgress,
        )
    }

    private fun copyToPrivateCache(
        source: InputStream,
        cacheDirectory: File,
        reportedSize: Long,
        checkCancelled: () -> Unit,
        onProgress: (Long) -> Unit,
        copyLimitOverride: Long? = null,
    ): StagedArchive {
        val copyLimit = copyLimitOverride ?: cacheDirectory.copyLimit()
        if (reportedSize >= 0L && reportedSize > copyLimit) {
            source.close()
            throw ArchiveInputLimitException("Insufficient cache storage for the archive")
        }
        val directory = File(cacheDirectory, "$ARCHIVE_INPUT_DIRECTORY_PREFIX${UUID.randomUUID()}")
        if (!directory.mkdirs()) {
            source.close()
            throw IOException("Cannot create the archive cache directory")
        }
        val target = File(directory, "source.archive")
        try {
            var copied = 0L
            var lastReported = 0L
            BufferedInputStream(source).use { sourceStream ->
                BufferedOutputStream(FileOutputStream(target)).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        checkCancelled()
                        val read = sourceStream.read(buffer)
                        if (read < 0) break
                        copied = Math.addExact(copied, read.toLong())
                        if (copied > copyLimit) {
                            throw ArchiveInputLimitException("Insufficient cache storage for the archive")
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
            if (reportedSize >= 0L && copied != reportedSize) {
                throw IOException("Archive changed while it was being staged")
            }
            val cacheKey = directory.absolutePath
            activeCacheDirectories += cacheKey
            return StagedArchive(
                source = target.asArchiveReadSource(),
                bytes = copied,
                storage = ArchiveInputStorage.PRIVATE_CACHE,
                onClosed = { activeCacheDirectories -= cacheKey },
            )
        } catch (error: Throwable) {
            runCatching { source.close() }
            runCatching { target.delete() }
            runCatching { directory.delete() }
            throw error
        }
    }

    private fun copyExactVolume(
        source: ArchiveReadSource,
        target: File,
        limit: Long,
    ): Long {
        var copied = 0L
        BufferedInputStream(source.openInputStream()).use { input ->
            BufferedOutputStream(FileOutputStream(target)).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied = Math.addExact(copied, read.toLong())
                    if (copied > limit) {
                        throw ArchiveInputLimitException(
                            "Insufficient cache storage for the archive volumes",
                        )
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
        return copied
    }

    private fun requireSafeVolumeName(value: String) {
        require(value.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_VOLUME_DISPLAY_NAME_LENGTH) {
            "Archive volume name length is invalid"
        }
        require(value != "." && value != ".." && value.any { !it.isWhitespace() }) {
            "Archive volume name is invalid"
        }
        require(value.none { character ->
            character == '/' ||
                character == '\\' ||
                character == '\u0000' ||
                character.code < 0x20 ||
                character.code == 0x7F
        }) { "Archive volume name contains an unsafe character" }
    }

    private fun resetToStartIfSeekable(source: ParcelFileDescriptor) {
        runCatching { Os.lseek(source.fileDescriptor, 0L, OsConstants.SEEK_SET) }
    }

    private fun readDescriptorFlags(source: ParcelFileDescriptor): Int? = try {
        File(PROCESS_FDINFO_DIRECTORY, source.fd.toString()).useLines { lines ->
            lines.firstOrNull { it.startsWith(FDINFO_FLAGS_PREFIX) }
                ?.substringAfter(':')
                ?.trim()
                ?.toLongOrNull(FDINFO_FLAGS_RADIX)
                ?.toInt()
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    @SuppressLint("UsableSpace")
    private fun File.copyLimit(): Long = compatibleUsableSpace()
        .takeIf { it > 0L }
        ?.minus(MINIMUM_FREE_CACHE_BYTES)
        ?.coerceAtLeast(0L)
        ?: Long.MAX_VALUE

    @SuppressLint("UsableSpace")
    private fun File.compatibleUsableSpace(): Long = usableSpace

    private fun cleanupStaleInputs(cacheDirectory: File) {
        val cutoff = System.currentTimeMillis() - STALE_INPUT_AGE_MILLIS
        val activeDirectories = synchronized(activeCacheDirectories) {
            activeCacheDirectories.toSet()
        }
        cacheDirectory.listFiles()?.asSequence()
            ?.filter { child ->
                child.isDirectory &&
                    child.name.startsWith(ARCHIVE_INPUT_DIRECTORY_PREFIX) &&
                    child.absolutePath !in activeDirectories &&
                    child.lastModified() <= cutoff
            }
            ?.forEach { child -> runCatching { child.deleteRecursively() } }
    }

    private data class VolumeCopyInput(
        val displayName: String,
        val expectedSize: Long,
        val source: ArchiveReadSource,
    )

    private data class DirectDescriptorCandidate(
        val source: ArchiveReadSource,
        val statSize: Long,
        val verifiedSize: Long,
    )

    private const val MINIMUM_FREE_CACHE_BYTES = 128L * 1024L * 1024L
    private const val PROCESS_FDINFO_DIRECTORY = "/proc/self/fdinfo"
    private const val FDINFO_FLAGS_PREFIX = "flags:"
    private const val FDINFO_FLAGS_RADIX = 8
    private const val PROGRESS_REPORT_BYTES = 8L * 1024L * 1024L
    private const val READ_MODE = "r"
    private const val STALE_INPUT_AGE_MILLIS = 7L * 24L * 60L * 60L * 1_000L
}

internal class ArchiveInputLimitException(message: String) : ArchiveException(
    code = ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE,
    message = message,
    stage = ArchiveFailureStage.INPUT,
)
