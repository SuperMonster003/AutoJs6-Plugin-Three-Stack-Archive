package io.github.supermonster003.autojs6.plugin.archivemanager

import org.apache.commons.compress.utils.MultiReadOnlySeekableByteChannel
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import java.io.File
import java.io.IOException
import java.nio.channels.SeekableByteChannel
import java.util.Locale

internal data class NumberedArchiveVolumeInfo(
    val archiveDisplayName: String,
    val format: ArchiveFormat,
) {
    fun resolve(
        primary: ArchiveReadSource,
        volumeSet: ArchiveVolumeSet?,
    ): NumberedArchiveVolumeResolution {
        val available = volumeSet?.volumes.orEmpty()
        val indexed = available.mapNotNull { volume ->
            val match = NUMBERED_VOLUME_PATTERN.matchEntire(volume.displayName)
                ?: return@mapNotNull null
            if (collisionKey(match.groupValues[1]) != collisionKey(archiveDisplayName)) {
                return@mapNotNull null
            }
            val index = match.groupValues[2].toIntOrNull()
                ?.takeIf { it in 2..ExplorerActionProtocol.MAX_ARCHIVE_VOLUMES }
                ?: return@mapNotNull null
            index to volume.displayName
        }.sortedBy(Pair<Int, String>::first)
        val highestIndex = indexed.lastOrNull()?.first ?: FIRST_VOLUME_INDEX
        val byIndex = indexed.associate { it }
        val companionNames = (FIRST_COMPANION_INDEX..highestIndex).map { index ->
            byIndex[index] ?: throw ArchiveValidationException(
                code = ArchiveFailureCode.MISSING_VOLUME,
                message = "Required numbered archive volume ${volumeName(index)} is missing",
                format = format,
                stage = ArchiveFailureStage.INDEX,
            )
        }
        val companionSources = companionNames.map { name ->
            volumeSet?.openSource(name) ?: throw ArchiveValidationException(
                code = ArchiveFailureCode.MISSING_VOLUME,
                message = "Required numbered archive volume $name is unavailable",
                format = format,
                stage = ArchiveFailureStage.INDEX,
            )
        }
        return NumberedArchiveVolumeResolution(
            source = NumberedArchiveReadSource(
                sources = listOf(primary) + companionSources,
                displayName = archiveDisplayName,
                volumeSet = volumeSet,
            ),
            info = this,
            lastVolumeIndex = highestIndex,
        )
    }

    fun incompleteOrDamaged(error: ArchiveValidationException, lastVolumeIndex: Int) =
        ArchiveValidationException(
            code = ArchiveFailureCode.MISSING_VOLUME,
            message = "Numbered ${format.displayName} archive is incomplete or damaged; " +
                "the next expected volume is ${volumeName(lastVolumeIndex + 1)}",
            cause = error,
            format = format,
            stage = ArchiveFailureStage.INDEX,
        )

    fun wrapMaterialized(source: ArchiveReadSource): ArchiveReadSource =
        NumberedArchiveReadSource(
            sources = listOf(source),
            displayName = archiveDisplayName,
            volumeSet = null,
        )

    private fun volumeName(index: Int): String =
        "$archiveDisplayName.${index.toString().padStart(VOLUME_INDEX_WIDTH, '0')}"

    private companion object {
        const val FIRST_VOLUME_INDEX = 1
        const val FIRST_COMPANION_INDEX = 2
        const val VOLUME_INDEX_WIDTH = 3
        val NUMBERED_VOLUME_PATTERN = Regex("(?i)^(.+\\.(?:zip|7z))\\.([0-9]{3})$")
    }
}

internal data class NumberedArchiveVolumeResolution(
    val source: ArchiveReadSource,
    val info: NumberedArchiveVolumeInfo,
    val lastVolumeIndex: Int,
)

internal object NumberedArchiveVolumePolicy {
    val fileNameSuffixes = arrayOf("zip.001", "7z.001")

    fun inspectFirstVolume(displayName: String): NumberedArchiveVolumeInfo? {
        val match = FIRST_VOLUME_PATTERN.matchEntire(displayName) ?: return null
        val format = when (match.groupValues[2].lowercase(Locale.ROOT)) {
            ArchiveFormat.ZIP.primaryExtension -> ArchiveFormat.ZIP
            ArchiveFormat.SEVEN_Z.primaryExtension -> ArchiveFormat.SEVEN_Z
            else -> return null
        }
        return NumberedArchiveVolumeInfo(
            archiveDisplayName = match.groupValues[1],
            format = format,
        )
    }

    fun matchesFirstVolume(displayName: String): Boolean =
        inspectFirstVolume(displayName) != null

    private val FIRST_VOLUME_PATTERN = Regex("(?i)^(.+\\.(zip|7z))\\.001$")
}

private class NumberedArchiveReadSource(
    private val sources: List<ArchiveReadSource>,
    override val displayName: String,
    override val volumeSet: ArchiveVolumeSet?,
) : ArchiveReadSource {
    init {
        require(sources.isNotEmpty())
        require(sources.size <= ExplorerActionProtocol.MAX_ARCHIVE_VOLUMES)
    }

    override val isRegularFile: Boolean
        get() = sources.all(ArchiveReadSource::isRegularFile)

    override val localFile: File?
        get() = sources.singleOrNull()?.localFile

    override val isMultiVolumeArchive: Boolean = true

    override fun identity(): ArchiveSourceIdentity {
        var totalLength = 0L
        var latestModification = 0L
        sources.forEach { source ->
            val identity = source.identity()
            totalLength = try {
                Math.addExact(totalLength, identity.length)
            } catch (error: ArithmeticException) {
                throw IOException("Numbered archive volume size overflows", error)
            }
            latestModification = maxOf(latestModification, identity.lastModifiedMillis)
        }
        return ArchiveSourceIdentity(totalLength, latestModification)
    }

    override fun openSeekableChannel(): SeekableByteChannel {
        val channels = ArrayList<SeekableByteChannel>(sources.size)
        try {
            sources.forEach { source -> channels += source.openSeekableChannel() }
            return if (channels.size == 1) {
                channels.single()
            } else {
                MultiReadOnlySeekableByteChannel.forSeekableByteChannels(
                    *channels.toTypedArray(),
                )
            }
        } catch (error: Throwable) {
            channels.asReversed().forEach { channel -> runCatching { channel.close() } }
            throw error
        }
    }
}
