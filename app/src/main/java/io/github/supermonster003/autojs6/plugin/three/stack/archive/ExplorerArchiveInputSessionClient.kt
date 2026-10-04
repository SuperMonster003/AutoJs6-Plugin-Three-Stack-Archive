package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.core.os.BundleCompat
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveInputKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveInputValues
import org.autojs.plugin.explorer.api.IExplorerArchiveInputSession
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal data class ExplorerArchiveInputMutation(
    val request: ArchiveMutationRequest,
    val nodeCount: Int,
)

/** Strict client for one host-frozen v20 archive-input grant. */
internal class ExplorerArchiveInputSessionClient(
    private val remote: IExplorerArchiveInputSession,
    expectedArchiveSessionId: String,
    expectedOperationId: String,
) : AutoCloseable {

    private val info = decodeInfo(remote.info, expectedArchiveSessionId, expectedOperationId)
    private val closed = AtomicBoolean(false)

    fun buildMutation(parentPath: String): ExplorerArchiveInputMutation {
        check(!closed.get()) { "Archive input session is closed" }
        val nodesById = LinkedHashMap<String, InputNode>()
        val orderedNodes = ArrayList<InputNode>(info.nodeCount)
        val pending = ArrayDeque<PendingDirectory>()
        var cumulativeNameBytes = 0L
        var knownTotalBytes = 0L
        var fileCount = 0
        var directoryCount = 0

        fun accept(
            node: InputNode,
            depth: Int,
            relativePath: String,
            inputRootId: String,
        ) {
            require(depth in 1..ExplorerActionProtocol.MAX_ARCHIVE_INPUT_DEPTH) {
                "Archive input tree exceeds the negotiated depth"
            }
            require(node.id !in info.reservedIds) {
                "Archive input node reuses a reserved identity"
            }
            require(nodesById.put(node.id, node) == null) {
                "Archive input contains a duplicate node ID"
            }
            require(nodesById.size <= info.nodeCount) {
                "Archive input returned more nodes than declared"
            }
            cumulativeNameBytes = Math.addExact(
                cumulativeNameBytes,
                node.name.toByteArray(StandardCharsets.UTF_8).size.toLong(),
            )
            require(cumulativeNameBytes <= ExplorerActionProtocol.MAX_ARCHIVE_INPUT_NAME_BYTES) {
                "Archive input names exceed the negotiated text limit"
            }
            val validatedPath = ArchivePathPolicy.validateEntryPath(
                sourceName = relativePath,
                isDirectory = node.isDirectory,
                limits = ArchiveStructureLimits.DEFAULT,
            ).path
            require(validatedPath == relativePath) {
                "Archive input contains an ambiguous path"
            }
            orderedNodes += node.copy(
                relativePath = relativePath,
                inputRootId = inputRootId,
            )
            if (node.isDirectory) {
                directoryCount += 1
                pending += PendingDirectory(node.id, depth, relativePath, inputRootId)
            } else {
                fileCount += 1
                knownTotalBytes = Math.addExact(knownTotalBytes, node.size)
            }
        }

        val roots = listAllChildren(info.virtualRootId).also { rootNodes ->
            require(rootNodes.size == info.rootCount) {
                "Archive input root count changed"
            }
        }
        roots.forEach { root -> accept(root, 1, root.name, root.id) }
        while (pending.isNotEmpty()) {
            val parent = pending.removeFirst()
            listAllChildren(parent.id).forEach { child ->
                accept(
                    node = child,
                    depth = parent.depth + 1,
                    relativePath = "${parent.relativePath}/${child.name}",
                    inputRootId = parent.inputRootId,
                )
            }
        }
        require(nodesById.size == info.nodeCount) { "Archive input node count changed" }
        require(fileCount == info.fileCount && directoryCount == info.directoryCount) {
            "Archive input type counts changed"
        }
        require(knownTotalBytes == info.knownTotalBytes) {
            "Archive input byte count changed"
        }

        val request = if (roots.all { root -> !root.isDirectory }) {
            ArchiveMutationRequest.AddFiles(
                parentPath = parentPath,
                files = roots.map(::addedFile),
            )
        } else {
            ArchiveMutationRequest.AddTree(
                parentPath = parentPath,
                entries = orderedNodes.map { node ->
                    if (node.isDirectory) {
                        ArchiveMutationAddedTreeEntry.Directory(
                            relativePath = node.relativePath,
                            lastModified = node.lastModified,
                            inputRootId = node.inputRootId,
                        )
                    } else {
                        ArchiveMutationAddedTreeEntry.FileEntry(
                            relativePath = node.relativePath,
                            file = addedFile(node),
                            inputRootId = node.inputRootId,
                        )
                    }
                },
            )
        }
        return ExplorerArchiveInputMutation(request, info.nodeCount)
    }

    fun verifySnapshot() {
        check(!closed.get()) { "Archive input session is closed" }
        val result = remote.verifySnapshot()
        when (
            result.getInt(
                ExplorerArchiveInputKeys.VERIFICATION_STATUS,
                Int.MIN_VALUE,
            )
        ) {
            ExplorerArchiveInputValues.VERIFICATION_VALID -> Unit
            ExplorerArchiveInputValues.VERIFICATION_SOURCE_CHANGED -> throw ArchiveValidationException(
                code = ArchiveFailureCode.SOURCE_CHANGED,
                message = "A selected archive input changed before publication",
                stage = ArchiveFailureStage.INPUT,
            )
            else -> error("Host returned an invalid archive input verification state")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) remote.close()
    }

    private fun listAllChildren(parentId: String): List<InputNode> {
        requireOpaqueId(parentId, "parent")
        val result = ArrayList<InputNode>()
        var offset = 0
        while (true) {
            val page = remote.listChildren(
                parentId,
                offset,
                ExplorerActionProtocol.MAX_ARCHIVE_INPUT_PAGE_SIZE,
            )
            val items = BundleCompat.getParcelableArrayList(
                page,
                ExplorerArchiveInputKeys.ITEMS,
                Bundle::class.java,
            ).orEmpty()
            require(items.size <= ExplorerActionProtocol.MAX_ARCHIVE_INPUT_PAGE_SIZE) {
                "Archive input returned an oversized page"
            }
            items.forEach { item -> result += decodeNode(item, parentId) }
            require(result.size <= info.nodeCount) {
                "Archive input returned too many children"
            }
            val nextOffset = page.getInt(ExplorerArchiveInputKeys.NEXT_OFFSET, -1)
            val complete = page.getBoolean(ExplorerArchiveInputKeys.COMPLETE, false)
            if (complete) {
                require(nextOffset == result.size) {
                    "Archive input returned an invalid terminal offset"
                }
                if (parentId != info.virtualRootId) {
                    require(
                        result.map { node ->
                            ArchivePathPolicy.destinationCollisionKey(node.name)
                        }.distinct().size == result.size,
                    ) { "Archive input contains ambiguous sibling names" }
                }
                return result
            }
            require(items.isNotEmpty() && nextOffset == offset + items.size) {
                "Archive input pagination did not advance"
            }
            offset = nextOffset
        }
    }

    private fun decodeNode(bundle: Bundle, expectedParentId: String): InputNode {
        val id = requireOpaqueId(bundle.getString(ExplorerArchiveInputKeys.ID), "node")
        val parentId = requireOpaqueId(
            bundle.getString(ExplorerArchiveInputKeys.PARENT_ID),
            "node parent",
        )
        require(parentId == expectedParentId) { "Archive input node has the wrong parent" }
        val name = requireNotNull(
            ArchiveIntentPolicy.validateDisplayName(bundle.getString(ExplorerArchiveInputKeys.NAME)),
        ) { "Archive input node has an invalid name" }
        val kind = bundle.getInt(ExplorerArchiveInputKeys.KIND, Int.MIN_VALUE)
        require(
            kind == ExplorerArchiveInputValues.KIND_FILE ||
                kind == ExplorerArchiveInputValues.KIND_DIRECTORY,
        ) { "Archive input node has an invalid kind" }
        val size = bundle.getLong(ExplorerArchiveInputKeys.SIZE, Long.MIN_VALUE)
        if (kind == ExplorerArchiveInputValues.KIND_FILE) {
            require(size >= 0L) { "Archive input file has an invalid size" }
        } else {
            require(size == -1L) { "Archive input directory has an invalid size" }
        }
        val lastModified = bundle.getLong(ExplorerArchiveInputKeys.LAST_MODIFIED, -1L)
        require(lastModified >= 0L) { "Archive input node has an invalid timestamp" }
        return InputNode(
            id = id,
            parentId = parentId,
            name = name,
            isDirectory = kind == ExplorerArchiveInputValues.KIND_DIRECTORY,
            size = size,
            lastModified = lastModified,
        )
    }

    private fun addedFile(node: InputNode): ArchiveMutationAddedFile {
        require(!node.isDirectory)
        val opened = AtomicBoolean(false)
        return ArchiveMutationAddedFile(
            displayName = node.name,
            size = node.size,
            lastModified = node.lastModified,
        ) {
            check(opened.compareAndSet(false, true)) {
                "Archive input file stream was requested more than once"
            }
            val descriptor = requireNotNull(remote.openFile(node.id)) {
                "Host returned no archive input descriptor"
            }
            try {
                require(descriptor.fileDescriptor.valid()) {
                    "Host returned an invalid archive input descriptor"
                }
                ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            } catch (error: Throwable) {
                descriptor.close()
                throw error
            }
        }
    }

    private fun decodeInfo(
        bundle: Bundle,
        expectedArchiveSessionId: String,
        expectedOperationId: String,
    ): InputInfo {
        val grantId = requireCanonicalUuid(
            bundle.getString(ExplorerArchiveInputKeys.GRANT_ID),
            "grant",
        )
        val archiveSessionId = requireOpaqueId(
            bundle.getString(ExplorerArchiveInputKeys.ARCHIVE_SESSION_ID),
            "archive session",
            ExplorerActionProtocol.MAX_ARCHIVE_SESSION_ID_LENGTH,
        )
        require(archiveSessionId == expectedArchiveSessionId) {
            "Archive input grant belongs to another archive session"
        }
        val operationId = requireCanonicalUuid(
            bundle.getString(ExplorerArchiveInputKeys.OPERATION_ID),
            "operation",
        )
        require(operationId == expectedOperationId) {
            "Archive input grant belongs to another operation"
        }
        val virtualRootId = requireOpaqueId(
            bundle.getString(ExplorerArchiveInputKeys.VIRTUAL_ROOT_ID),
            "virtual root",
        )
        val reservedIds = setOf(grantId, archiveSessionId, operationId, virtualRootId)
        require(reservedIds.size == 4) {
            "Archive input grant reuses an opaque identity"
        }
        val rootCount = bundle.getInt(ExplorerArchiveInputKeys.ROOT_COUNT, -1)
        val nodeCount = bundle.getInt(ExplorerArchiveInputKeys.NODE_COUNT, -1)
        val fileCount = bundle.getInt(ExplorerArchiveInputKeys.FILE_COUNT, -1)
        val directoryCount = bundle.getInt(ExplorerArchiveInputKeys.DIRECTORY_COUNT, -1)
        val knownTotalBytes = bundle.getLong(ExplorerArchiveInputKeys.KNOWN_TOTAL_BYTES, -1L)
        require(rootCount in 1..ExplorerActionProtocol.MAX_ARCHIVE_INPUT_ROOTS) {
            "Archive input root count is invalid"
        }
        require(nodeCount in rootCount..ExplorerActionProtocol.MAX_ARCHIVE_INPUT_NODES) {
            "Archive input node count is invalid"
        }
        require(fileCount >= 0 && directoryCount >= 0 && fileCount + directoryCount == nodeCount) {
            "Archive input type counts are invalid"
        }
        require(knownTotalBytes >= 0L) { "Archive input byte count is invalid" }
        return InputInfo(
            virtualRootId = virtualRootId,
            reservedIds = reservedIds,
            rootCount = rootCount,
            nodeCount = nodeCount,
            fileCount = fileCount,
            directoryCount = directoryCount,
            knownTotalBytes = knownTotalBytes,
        )
    }

    private fun requireCanonicalUuid(value: String?, label: String): String {
        val normalized = requireNotNull(value) { "Archive input $label ID is missing" }
        val parsed = runCatching { UUID.fromString(normalized) }.getOrNull()
        require(parsed?.toString() == normalized) { "Archive input $label ID is invalid" }
        return normalized
    }

    private fun requireOpaqueId(
        value: String?,
        label: String,
        maxLength: Int = ExplorerActionProtocol.MAX_ARCHIVE_INPUT_NODE_ID_LENGTH,
    ): String = requireNotNull(value?.takeIf { id ->
        id.length in 1..maxLength && id.none { character ->
            character.isWhitespace() || character.code < 0x20 || character.code == 0x7F
        }
    }) { "Archive input $label ID is invalid" }

    private data class InputInfo(
        val virtualRootId: String,
        val reservedIds: Set<String>,
        val rootCount: Int,
        val nodeCount: Int,
        val fileCount: Int,
        val directoryCount: Int,
        val knownTotalBytes: Long,
    )

    private data class InputNode(
        val id: String,
        val parentId: String,
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val relativePath: String = "",
        val inputRootId: String = "",
    )

    private data class PendingDirectory(
        val id: String,
        val depth: Int,
        val relativePath: String,
        val inputRootId: String,
    )
}
