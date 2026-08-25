package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.os.Binder
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
    val size: Long? = null,
    val lastModified: Long? = null,
    val kind: Int = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
    val state: Int = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
    val fileCount: Int = 0,
    val directoryCount: Int = 0,
    val outputBytes: Long = 0L,
)

internal class ExplorerActionHostSessionClient(
    private val remote: IExplorerActionHostSession,
) {
    private val clientToken = Binder()

    init {
        remote.attachClient(clientToken)
    }

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

    /** Reserves one exact physical output, including non-terminal split parts such as `.z01`. */
    fun prepareExactOutput(
        displayName: String,
        mimeType: String,
    ): HostOutputTransaction {
        val validatedName = ArchiveCompressionPolicy.validateOutputDisplayName(displayName)
            ?: throw IllegalArgumentException("Host output display name is invalid")
        require(
            mimeType.length <= ExplorerActionProtocol.MAX_OUTPUT_MIME_TYPE_LENGTH &&
                mimeType.matches(OUTPUT_MIME_TYPE_PATTERN),
        ) { "Host output MIME type is invalid" }
        val result = try {
            remote.prepareOutput(
                validatedName,
                mimeType,
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL,
            )
        } catch (error: IllegalArgumentException) {
            throw ArchiveOutputNameUnavailableException(validatedName, error)
        } ?: error("Host returned no output transaction")
        return decodeExactOutput(result, validatedName)
    }

    fun openOutput(transactionId: String): ParcelFileDescriptor =
        remote.openOutput(transactionId) ?: error("Host returned no output descriptor")

    fun prepareTargetReplacement(
        targetId: String,
        expectedDisplayName: String,
    ): HostOutputTransaction = decodeExactOutput(
        remote.prepareTargetReplacement(targetId)
            ?: error("Host returned no replacement transaction"),
        expectedDisplayName,
        defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_TARGET_REPLACEMENT,
    )

    fun prepareOutputTree(displayName: String): HostOutputTransaction {
        val validatedName = ArchivePathPolicy.validateDestinationRootName(displayName)
        val result = remote.prepareOutputTree(
            validatedName,
            ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
        ) ?: error("Host returned no directory output transaction")
        return decodeTreeOutput(
            result,
            defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
        )
    }

    fun createOutputDirectory(transactionId: String, relativePath: String) {
        remote.createOutputDirectory(transactionId, relativePath)
    }

    fun openOutputFile(transactionId: String, relativePath: String): ParcelFileDescriptor =
        remote.openOutputFile(transactionId, relativePath)
            ?: error("Host returned no directory output file descriptor")

    fun openPendingOutput(transactionId: String): ParcelFileDescriptor =
        remote.openPendingOutput(transactionId)
            ?: error("Host returned no pending output descriptor")

    fun commitOutput(
        transactionId: String,
        format: ArchiveFormat,
    ): HostOutputTransaction = decodeOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no commit result"),
        format,
        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
    )

    fun commitExactOutput(
        transactionId: String,
        expectedDisplayName: String,
    ): HostOutputTransaction = decodeExactOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no commit result"),
        expectedDisplayName,
        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
    )

    fun commitOutputTree(transactionId: String): HostOutputTransaction = decodeTreeOutput(
        remote.commitOutput(transactionId) ?: error("Host returned no directory commit result"),
        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
    )

    fun queryOutput(transactionId: String): HostOutputTransaction? {
        val result = remote.queryOutput(transactionId) ?: error("Host returned no output state")
        val state = result.getInt(
            ExplorerActionHostSessionKeys.OUTPUT_STATE,
            ExplorerActionHostSessionValues.OUTPUT_STATE_UNKNOWN,
        )
        if (state == ExplorerActionHostSessionValues.OUTPUT_STATE_UNKNOWN) return null
        return decodeRawOutput(
            result,
            defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
            defaultState = state,
        )
    }

    fun listOutputs(): List<HostOutputTransaction> {
        val result = remote.listOutputs() ?: error("Host returned no output transaction list")
        return result.parcelableBundleArrayList(ExplorerActionHostSessionKeys.OUTPUTS)
            .orEmpty()
            .take(ExplorerActionProtocol.MAX_OUTPUT_TRANSACTION_RESULTS)
            .mapNotNull { bundle ->
                runCatching {
                    decodeRawOutput(
                        bundle,
                        defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
                        defaultState = ExplorerActionHostSessionValues.OUTPUT_STATE_UNKNOWN,
                    )
                }.getOrNull()
            }
    }

    fun abortIncompleteOutputs(): Int {
        val incomplete = listOutputs().filter { output ->
            output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED ||
                output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING ||
                output.state == ExplorerActionHostSessionValues.OUTPUT_STATE_VERIFYING
        }
        incomplete.forEach { output -> remote.abortOutput(output.id) }
        return incomplete.size
    }

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
        defaultState: Int = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
    ): HostOutputTransaction {
        val decoded = decodeRawOutput(
            bundle,
            defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
            defaultState = defaultState,
        )
        require(
            ArchiveCompressionPolicy.normalizeOutputDisplayName(decoded.displayName, format) ==
                decoded.displayName,
        ) { "Host output display name does not match the archive format" }
        return decoded
    }

    private fun decodeExactOutput(
        bundle: Bundle,
        expectedDisplayName: String,
        defaultKind: Int = ExplorerActionHostSessionValues.OUTPUT_KIND_FILE,
        defaultState: Int = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
    ): HostOutputTransaction = decodeRawOutput(bundle, defaultKind, defaultState).also { decoded ->
        require(decoded.displayName == expectedDisplayName) {
            "Host changed an exactly reserved output name"
        }
    }

    private fun decodeTreeOutput(
        bundle: Bundle,
        defaultState: Int,
    ): HostOutputTransaction = decodeRawOutput(
        bundle,
        defaultKind = ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE,
        defaultState = defaultState,
    ).also { decoded ->
        require(decoded.kind == ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE) {
            "Host output transaction is not a directory tree"
        }
    }

    private fun decodeRawOutput(
        bundle: Bundle,
        defaultKind: Int,
        defaultState: Int,
    ): HostOutputTransaction {
        val id = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID)
            ?.takeIf(::isCanonicalUuid)
            ?: error("Host output transaction ID is invalid")
        val displayName = ArchiveCompressionPolicy.validateOutputDisplayName(
            bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME),
        ) ?: error("Host output display name is invalid")
        val displayPath = bundle.getString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH)
            ?.takeIf { it.length in 1..ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH }
            ?: error("Host output display path is invalid")
        val size = bundle.getLong(ExplorerActionHostSessionKeys.SIZE)
            .takeIf { bundle.containsKey(ExplorerActionHostSessionKeys.SIZE) && it >= 0L }
        val lastModified = bundle.getLong(ExplorerActionHostSessionKeys.LAST_MODIFIED)
            .takeIf { bundle.containsKey(ExplorerActionHostSessionKeys.LAST_MODIFIED) && it >= 0L }
        val kind = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_KIND, defaultKind)
        require(
            kind == ExplorerActionHostSessionValues.OUTPUT_KIND_FILE ||
                kind == ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE ||
                kind == ExplorerActionHostSessionValues.OUTPUT_KIND_TARGET_REPLACEMENT,
        ) { "Host output transaction kind is invalid" }
        val state = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_STATE, defaultState)
        require(state in VALID_OUTPUT_STATES) { "Host output transaction state is invalid" }
        val fileCount = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_FILE_COUNT, 0)
        val directoryCount = bundle.getInt(ExplorerActionHostSessionKeys.OUTPUT_DIRECTORY_COUNT, 0)
        val outputBytes = bundle.getLong(ExplorerActionHostSessionKeys.OUTPUT_BYTES, 0L)
        require(fileCount >= 0 && directoryCount >= 0 && outputBytes >= 0L) {
            "Host output transaction statistics are invalid"
        }
        return HostOutputTransaction(
            id = id,
            displayName = displayName,
            displayPath = displayPath,
            size = size,
            lastModified = lastModified,
            kind = kind,
            state = state,
            fileCount = fileCount,
            directoryCount = directoryCount,
            outputBytes = outputBytes,
        )
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
        val OUTPUT_MIME_TYPE_PATTERN = Regex("^[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+$")
        val VALID_OUTPUT_STATES = setOf(
            ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED,
            ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING,
            ExplorerActionHostSessionValues.OUTPUT_STATE_VERIFYING,
            ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
            ExplorerActionHostSessionValues.OUTPUT_STATE_ABORTED,
        )
    }
}
