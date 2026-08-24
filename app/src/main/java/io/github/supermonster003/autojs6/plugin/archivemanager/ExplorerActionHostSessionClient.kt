package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import java.util.UUID

internal data class HostSessionEntry(
    val relativePath: String,
    val displayName: String,
    val kind: Int,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val readable: Boolean,
    val symbolicLink: Boolean,
)

internal data class HostSessionPage(
    val items: List<HostSessionEntry>,
    val nextOffset: Int,
    val complete: Boolean,
)

internal data class HostOutputTransaction(
    val id: String,
    val displayName: String,
    val displayPath: String,
)

internal class ExplorerActionHostSessionClient(
    private val remote: IExplorerActionHostSession,
) {

    fun listChildren(targetId: String, relativePath: String, offset: Int): HostSessionPage {
        val result = remote.listChildren(
            targetId,
            relativePath,
            offset,
            ExplorerActionProtocol.MAX_SESSION_PAGE_SIZE,
        ) ?: error("Host returned no directory page")
        val bundles = result.parcelableBundleArrayList(ExplorerActionHostSessionKeys.ITEMS)
            ?: error("Host directory page has no item list")
        require(bundles.size <= ExplorerActionProtocol.MAX_SESSION_PAGE_SIZE) {
            "Host directory page exceeds the negotiated limit"
        }
        val items = bundles.map(::decodeEntry)
        val nextOffset = result.getInt(ExplorerActionHostSessionKeys.NEXT_OFFSET, -1)
        val complete = result.getBoolean(ExplorerActionHostSessionKeys.COMPLETE, false)
        require(nextOffset == offset + items.size) { "Host directory page offset is inconsistent" }
        require(complete || items.isNotEmpty()) { "Host directory pagination did not advance" }
        return HostSessionPage(items, nextOffset, complete)
    }

    fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor =
        remote.openFile(targetId, relativePath) ?: error("Host returned no source descriptor")

    fun prepareOutput(
        displayName: String,
        format: ArchiveFormat,
        conflictPolicy: ArchiveCreationConflictPolicy,
    ): HostOutputTransaction {
        val hostConflictPolicy = when (conflictPolicy) {
            ArchiveCreationConflictPolicy.AUTO_RENAME ->
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME
            ArchiveCreationConflictPolicy.ASK ->
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL
        }
        val result = try {
            remote.prepareOutput(
                displayName,
                format.primaryMimeType,
                hostConflictPolicy,
            )
        } catch (error: IllegalArgumentException) {
            if (conflictPolicy == ArchiveCreationConflictPolicy.ASK) {
                throw ArchiveOutputNameUnavailableException(displayName, error)
            }
            throw error
        } ?: error("Host returned no output transaction")
        return decodeOutput(result, format)
    }

    fun openOutput(transactionId: String): ParcelFileDescriptor =
        remote.openOutput(transactionId) ?: error("Host returned no output descriptor")

    fun commitOutput(
        transactionId: String,
        format: ArchiveFormat,
    ): HostOutputTransaction = decodeOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no commit result"),
        format,
    )

    fun abortOutput(transactionId: String) {
        remote.abortOutput(transactionId)
    }

    fun close() {
        remote.close()
    }

    private fun decodeEntry(bundle: Bundle): HostSessionEntry {
        val relativePath = bundle.getString(ExplorerActionHostSessionKeys.RELATIVE_PATH)
            ?.takeIf { it.length <= ExplorerActionProtocol.MAX_SESSION_RELATIVE_PATH_LENGTH }
            ?: error("Host source entry has an invalid relative path")
        val displayName = ArchiveCompressionIntentPolicy.validateLeafName(
            bundle.getString(ExplorerActionHostSessionKeys.DISPLAY_NAME),
        ) ?: error("Host source entry has an invalid display name")
        val kind = bundle.getInt(ExplorerActionHostSessionKeys.KIND, Int.MIN_VALUE)
        require(kind == ExplorerActionValues.TARGET_FILE || kind == ExplorerActionValues.TARGET_DIRECTORY) {
            "Host source entry has an invalid kind"
        }
        val mimeType = bundle.getString(ExplorerActionHostSessionKeys.MIME_TYPE)
            ?.takeIf { it.isNotBlank() && it.length <= ExplorerActionProtocol.MAX_OUTPUT_MIME_TYPE_LENGTH }
            ?: error("Host source entry has an invalid MIME type")
        val size = bundle.getLong(ExplorerActionHostSessionKeys.SIZE, Long.MIN_VALUE)
        require(size >= SIZE_UNKNOWN) { "Host source entry has an invalid size" }
        val lastModified = bundle.getLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, Long.MIN_VALUE)
        require(lastModified >= SIZE_UNKNOWN) { "Host source entry has an invalid timestamp" }
        val expectedPath = relativePath.substringAfterLast('/')
        require(expectedPath == displayName) { "Host source entry path and display name differ" }
        return HostSessionEntry(
            relativePath = relativePath,
            displayName = displayName,
            kind = kind,
            mimeType = mimeType,
            size = size,
            lastModified = lastModified,
            readable = bundle.getBoolean(ExplorerActionHostSessionKeys.READABLE, false),
            symbolicLink = bundle.getBoolean(ExplorerActionHostSessionKeys.SYMBOLIC_LINK, true),
        )
    }

    private fun decodeOutput(
        bundle: Bundle,
        format: ArchiveFormat,
    ): HostOutputTransaction {
        val id = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host output transaction ID is invalid")
        val displayName = ArchiveCompressionPolicy.normalizeOutputDisplayName(
            bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME),
            format,
        ) ?: error("Host output display name is invalid")
        val displayPath = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH)
            ?.takeIf { it.length in 1..ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH }
            ?: error("Host output display path is invalid")
        return HostOutputTransaction(id, displayName, displayPath)
    }

    @Suppress("DEPRECATION")
    private fun Bundle.parcelableBundleArrayList(name: String): ArrayList<Bundle>? =
        getParcelableArrayList(name)

    private fun isCanonicalUuid(value: String): Boolean {
        val parsed = runCatching { UUID.fromString(value) }.getOrNull() ?: return false
        return parsed.toString().equals(value, ignoreCase = true)
    }

    private companion object {
        const val SIZE_UNKNOWN = -1L
    }
}
