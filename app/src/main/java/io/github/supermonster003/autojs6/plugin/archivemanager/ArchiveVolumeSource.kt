package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.core.os.BundleCompat
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveVolumeSourceKeys
import org.autojs.plugin.explorer.api.IExplorerArchiveVolumeSource
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class ArchiveVolumeIdentity(
    val displayName: String,
    val length: Long,
    val lastModifiedMillis: Long,
)

internal interface ArchiveVolumeSet : Closeable {
    val volumes: List<ArchiveVolumeIdentity>
    val localDirectory: File?
        get() = null

    @Throws(IOException::class)
    fun inspectIdentities(): List<ArchiveVolumeIdentity>

    @Throws(IOException::class)
    fun openSource(displayName: String): ArchiveReadSource?
}

internal class ArchiveVolumeChangedException(message: String, cause: Throwable? = null) :
    IOException(message, cause)

/** Strict client for one host-owned v12 sibling-volume Binder. */
internal class ExplorerArchiveVolumeSourceClient private constructor(
    private val remote: IExplorerArchiveVolumeSource,
    private val cacheDirectory: File,
    private val catalog: List<VolumeMetadata>,
) : ArchiveVolumeSet {

    private val closed = AtomicBoolean(false)
    private val catalogByCollisionKey = catalog.associateBy(VolumeMetadata::collisionKey)
    private val staged = LinkedHashMap<String, StagedVolume>()

    override val volumes: List<ArchiveVolumeIdentity> = catalog.map(VolumeMetadata::identity)

    override fun inspectIdentities(): List<ArchiveVolumeIdentity> {
        checkOpen()
        val current = try {
            readCatalog(remote.catalog)
        } catch (error: Exception) {
            throw ArchiveVolumeChangedException(
                "Archive volume catalog cannot be revalidated",
                error,
            )
        }
        if (current != catalog) {
            throw ArchiveVolumeChangedException("Archive volume catalog changed during the session")
        }
        return volumes
    }

    override fun openSource(displayName: String): ArchiveReadSource? {
        checkOpen()
        val metadata = catalogByCollisionKey[collisionKey(displayName)] ?: return null
        synchronized(staged) {
            staged[metadata.id]?.let { return it.source }
            val descriptor = try {
                requireNotNull(remote.openVolume(metadata.id)) {
                    "Archive volume source returned no descriptor"
                }
            } catch (error: Exception) {
                throw ArchiveVolumeChangedException("Archive volume cannot be opened", error)
            }
            val stagedArchive = try {
                require(descriptor.fileDescriptor.valid()) {
                    "Archive volume source returned an invalid descriptor"
                }
                ArchiveCacheStager.stage(
                    source = descriptor,
                    cacheDirectory = cacheDirectory,
                    reportedSize = metadata.size,
                )
            } catch (error: Throwable) {
                runCatching { descriptor.close() }
                throw error
            }
            val actualSize = try {
                stagedArchive.source.identity().length
            } catch (error: Throwable) {
                stagedArchive.close()
                throw error
            }
            if (actualSize != metadata.size) {
                stagedArchive.close()
                throw ArchiveVolumeChangedException(
                    "Archive volume size changed while it was being staged",
                )
            }
            val source = NamedArchiveReadSource(stagedArchive.source, metadata.displayName)
            staged[metadata.id] = StagedVolume(stagedArchive, source)
            return source
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        val opened = synchronized(staged) {
            staged.values.toList().also { staged.clear() }
        }
        opened.forEach { volume -> runCatching { volume.staged.close() } }
    }

    private fun checkOpen() {
        check(!closed.get()) { "Archive volume source client is closed" }
    }

    private data class StagedVolume(
        val staged: StagedArchive,
        val source: ArchiveReadSource,
    )

    private data class VolumeMetadata(
        val id: String,
        val displayName: String,
        val size: Long,
        val lastModifiedMillis: Long,
    ) {
        fun collisionKey(): String = collisionKey(displayName)

        fun identity(): ArchiveVolumeIdentity = ArchiveVolumeIdentity(
            displayName = displayName,
            length = size,
            lastModifiedMillis = lastModifiedMillis,
        )
    }

    companion object {
        fun fromBinder(
            binder: android.os.IBinder?,
            cacheDirectory: File,
        ): ExplorerArchiveVolumeSourceClient? {
            if (binder == null) return null
            val remote = IExplorerArchiveVolumeSource.Stub.asInterface(binder) ?: return null
            val catalog = try {
                readCatalog(remote.catalog)
            } catch (error: Exception) {
                throw IOException("Archive volume catalog cannot be read", error)
            }
            if (catalog.isEmpty()) return null
            return ExplorerArchiveVolumeSourceClient(remote, cacheDirectory, catalog)
        }

        private fun readCatalog(bundle: Bundle): List<VolumeMetadata> {
            val items = BundleCompat.getParcelableArrayList(
                bundle,
                ExplorerArchiveVolumeSourceKeys.VOLUMES,
                Bundle::class.java,
            ).orEmpty()
            require(items.size < ExplorerActionProtocol.MAX_ARCHIVE_VOLUMES) {
                "Archive volume catalog is oversized"
            }
            val ids = HashSet<String>()
            val names = HashSet<String>()
            return items.map { item ->
                val id = requireNotNull(
                    item.getString(ExplorerArchiveVolumeSourceKeys.VOLUME_ID)?.takeIf { value ->
                        value.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_VOLUME_ID_LENGTH &&
                            value.none(::isInvalidOpaqueIdCharacter)
                    },
                ) { "Archive volume ID is invalid" }
                require(ids.add(id)) { "Archive volume catalog contains a duplicate ID" }
                val displayName = requireNotNull(
                    item.getString(ExplorerArchiveVolumeSourceKeys.DISPLAY_NAME)?.takeIf { value ->
                        value.length in 1..ExplorerActionProtocol.MAX_ARCHIVE_VOLUME_DISPLAY_NAME_LENGTH &&
                            value != "." &&
                            value != ".." &&
                            value.any { !it.isWhitespace() } &&
                            value.none(::isInvalidVolumeNameCharacter)
                    },
                ) { "Archive volume display name is invalid" }
                require(names.add(collisionKey(displayName))) {
                    "Archive volume catalog contains an ambiguous display name"
                }
                val size = item.getLong(ExplorerArchiveVolumeSourceKeys.SIZE, -1L)
                val lastModified = item.getLong(
                    ExplorerArchiveVolumeSourceKeys.LAST_MODIFIED,
                    -1L,
                )
                require(size >= 0L && lastModified >= 0L) {
                    "Archive volume identity is invalid"
                }
                VolumeMetadata(id, displayName, size, lastModified)
            }
        }
    }
}

internal class LocalArchiveVolumeSet(
    override val localDirectory: File,
    private val sourcesByCollisionKey: Map<String, ArchiveReadSource>,
) : ArchiveVolumeSet {

    override val volumes: List<ArchiveVolumeIdentity>
        get() = inspectIdentities()

    override fun inspectIdentities(): List<ArchiveVolumeIdentity> = sourcesByCollisionKey.values
        .map { source ->
            val identity = source.identity()
            ArchiveVolumeIdentity(
                displayName = source.displayName,
                length = identity.length,
                lastModifiedMillis = identity.lastModifiedMillis,
            )
        }
        .sortedBy { collisionKey(it.displayName) }

    override fun openSource(displayName: String): ArchiveReadSource? =
        sourcesByCollisionKey[collisionKey(displayName)]

    override fun close() = Unit
}

internal class NamedArchiveReadSource(
    private val source: ArchiveReadSource,
    override val displayName: String,
) : ArchiveReadSource by source {
    override val volumeSet: ArchiveVolumeSet?
        get() = source.volumeSet
}

internal fun collisionKey(value: String): String = Normalizer.normalize(
    value,
    Normalizer.Form.NFC,
).lowercase(Locale.ROOT)

private fun isInvalidOpaqueIdCharacter(character: Char): Boolean =
    character.isWhitespace() || character.code < 0x20 || character.code == 0x7F

private fun isInvalidVolumeNameCharacter(character: Char): Boolean =
    character == '/' ||
        character == '\\' ||
        character == '\u0000' ||
        character.code < 0x20 ||
        character.code == 0x7F
