package io.github.supermonster003.autojs6.plugin.archivemanager

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel

internal data class ArchiveSourceIdentity(
    val length: Long,
    val lastModifiedMillis: Long,
)

internal data class ArchiveInputIdentity(
    val primary: ArchiveSourceIdentity,
    val volumes: List<ArchiveVolumeIdentity>,
)

@Throws(IOException::class)
internal fun ArchiveReadSource.inputIdentity(): ArchiveInputIdentity = ArchiveInputIdentity(
    primary = identity(),
    volumes = volumeSet?.inspectIdentities().orEmpty(),
)

/** An archive input that can create fresh readers without requiring path access to its storage. */
internal interface ArchiveReadSource {
    val isRegularFile: Boolean
    val localFile: File?
    val displayName: String
    val isMultiVolumeArchive: Boolean
        get() = false
    val volumeSet: ArchiveVolumeSet?
        get() = null

    @Throws(IOException::class)
    fun identity(): ArchiveSourceIdentity

    @Throws(IOException::class)
    fun openSeekableChannel(): SeekableByteChannel

    @Throws(IOException::class)
    fun openInputStream(): InputStream = Channels.newInputStream(openSeekableChannel())
}

internal class VolumeAwareArchiveReadSource(
    private val source: ArchiveReadSource,
    override val displayName: String,
    override val volumeSet: ArchiveVolumeSet,
) : ArchiveReadSource {
    override val isRegularFile: Boolean
        get() = source.isRegularFile
    override val localFile: File?
        get() = source.localFile
    override val isMultiVolumeArchive: Boolean
        get() = source.isMultiVolumeArchive

    override fun identity(): ArchiveSourceIdentity = source.identity()

    override fun openSeekableChannel(): SeekableByteChannel = source.openSeekableChannel()

    override fun openInputStream(): InputStream = source.openInputStream()
}

internal fun File.asArchiveReadSource(): ArchiveReadSource = FileArchiveReadSource(this)

private class FileArchiveReadSource(
    private val source: File,
) : ArchiveReadSource {
    override val isRegularFile: Boolean
        get() = source.isFile

    override val localFile: File = source

    override val displayName: String
        get() = source.name

    override fun identity(): ArchiveSourceIdentity = ArchiveSourceIdentity(
        length = source.length(),
        lastModifiedMillis = source.lastModified(),
    )

    override fun openSeekableChannel(): SeekableByteChannel = RandomAccessFile(source, READ_MODE).channel

    override fun openInputStream(): InputStream = FileInputStream(source)
}

/**
 * Reads an already-authorized descriptor with positional I/O. Each returned channel owns only its
 * logical position; closing it never closes the descriptor lease held by [StagedArchive].
 */
internal class DescriptorArchiveReadSource(
    private val descriptor: FileDescriptor,
    override val displayName: String = DEFAULT_DESCRIPTOR_NAME,
) : ArchiveReadSource {
    override val isRegularFile: Boolean
        get() = try {
            OsConstants.S_ISREG(Os.fstat(descriptor).st_mode)
        } catch (_: ErrnoException) {
            false
        }

    override val localFile: File? = null

    override fun identity(): ArchiveSourceIdentity {
        val stat = try {
            Os.fstat(descriptor)
        } catch (error: ErrnoException) {
            throw IOException("Cannot inspect the archive descriptor", error)
        }
        val modifiedMillis = try {
            Math.multiplyExact(stat.st_mtime, MILLIS_PER_SECOND)
        } catch (error: ArithmeticException) {
            throw IOException("Archive modification time is out of range", error)
        }
        return ArchiveSourceIdentity(stat.st_size, modifiedMillis)
    }

    override fun openSeekableChannel(): SeekableByteChannel =
        PositionalDescriptorChannel(descriptor, ::identity)

    private companion object {
        const val DEFAULT_DESCRIPTOR_NAME = "source.archive"
        const val MILLIS_PER_SECOND = 1_000L
    }
}

private class PositionalDescriptorChannel(
    private val descriptor: FileDescriptor,
    private val identity: () -> ArchiveSourceIdentity,
) : SeekableByteChannel {
    private var open = true
    private var logicalPosition = 0L

    @Synchronized
    override fun read(destination: ByteBuffer): Int {
        checkOpen()
        if (!destination.hasRemaining()) return 0
        val sourceSize = identity().length
        if (logicalPosition >= sourceSize) return -1
        val readLimit = minOf(
            destination.remaining().toLong(),
            sourceSize - logicalPosition,
            MAX_POSITIONAL_READ_BYTES.toLong(),
        ).toInt()
        val read = try {
            if (destination.hasArray() && !destination.isReadOnly) {
                Os.pread(
                    descriptor,
                    destination.array(),
                    destination.arrayOffset() + destination.position(),
                    readLimit,
                    logicalPosition,
                ).also { count ->
                    if (count > 0) destination.position(destination.position() + count)
                }
            } else {
                val buffer = ByteArray(readLimit)
                Os.pread(descriptor, buffer, 0, buffer.size, logicalPosition).also { count ->
                    if (count > 0) destination.put(buffer, 0, count)
                }
            }
        } catch (error: ErrnoException) {
            throw IOException("Cannot read the archive descriptor", error)
        }
        if (read <= 0) return -1
        logicalPosition = Math.addExact(logicalPosition, read.toLong())
        return read
    }

    override fun write(source: ByteBuffer): Int = throw NonWritableChannelException()

    @Synchronized
    override fun position(): Long {
        checkOpen()
        return logicalPosition
    }

    @Synchronized
    override fun position(newPosition: Long): SeekableByteChannel {
        checkOpen()
        require(newPosition >= 0L) { "Archive channel position cannot be negative" }
        logicalPosition = newPosition
        return this
    }

    @Synchronized
    override fun size(): Long {
        checkOpen()
        return identity().length
    }

    override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()

    @Synchronized
    override fun isOpen(): Boolean = open

    @Synchronized
    override fun close() {
        open = false
    }

    private fun checkOpen() {
        if (!open) throw ClosedChannelException()
    }

    private companion object {
        const val MAX_POSITIONAL_READ_BYTES = 1024 * 1024
    }
}

private const val READ_MODE = "r"

internal class ArchiveLocalFileRequiredException(message: String) : IOException(message)
