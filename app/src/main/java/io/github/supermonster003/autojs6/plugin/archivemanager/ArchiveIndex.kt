package io.github.supermonster003.autojs6.plugin.archivemanager

import java.text.Normalizer
import java.util.Locale

class ArchiveIndex(
    snapshot: ArchiveSnapshot,
) {
    private val limits = snapshot.limits
    private val nodesByPath: Map<String, ArchiveNode>
    private val childrenByPath: Map<String, List<ArchiveNode>>
    private var nodeCount = 0

    init {
        val mutableRoot = MutableNode(
            path = ArchivePathPolicy.ROOT_PATH,
            name = ArchivePathPolicy.ROOT_PATH,
            isDirectory = true,
        )
        snapshot.entries.forEach { entry -> insert(mutableRoot, entry) }

        val nodes = LinkedHashMap<String, ArchiveNode>()
        val children = LinkedHashMap<String, List<ArchiveNode>>()
        freeze(mutableRoot, nodes, children)
        nodesByPath = nodes
        childrenByPath = children
    }

    @JvmOverloads
    fun children(directory: String = ArchivePathPolicy.ROOT_PATH): List<ArchiveNode> {
        val path = normalizedLookupPath(directory)
        val node = nodesByPath[path]
            ?: throw ArchiveSelectionException(
                ArchiveFailureCode.UNKNOWN_SELECTION,
                "Archive directory does not exist",
            )
        if (!node.isDirectory) {
            throw ArchiveSelectionException(
                ArchiveFailureCode.UNKNOWN_SELECTION,
                "Archive path is not a directory",
            )
        }
        return childrenByPath.getValue(path)
    }

    fun search(query: String): List<ArchiveNode> {
        val needle = Normalizer.normalize(query.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
        if (needle.isEmpty()) return emptyList()
        return nodesByPath.values.asSequence()
            .filter { it.path.isNotEmpty() }
            .filter {
                Normalizer.normalize(it.path, Normalizer.Form.NFC)
                    .lowercase(Locale.ROOT)
                    .contains(needle)
            }
            .sortedWith(NODE_COMPARATOR)
            .toList()
    }

    fun node(path: String): ArchiveNode? = nodesByPath[normalizedLookupPath(path)]

    fun contains(path: String): Boolean = node(path) != null

    private fun insert(root: MutableNode, entry: ArchiveEntry) {
        val segments = entry.path.split('/')
        var parent = root
        segments.forEachIndexed { index, segment ->
            val isLast = index == segments.lastIndex
            val childPath = if (parent.path.isEmpty()) segment else "${parent.path}/$segment"
            val mustBeDirectory = !isLast || entry.isDirectory
            val child = parent.children[segment]
            if (child == null) {
                nodeCount++
                if (nodeCount > limits.maxEntries) {
                    throw ArchiveValidationException(
                        ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                        "Archive snapshot exceeds the path-node limit",
                    )
                }
                MutableNode(childPath, segment, mustBeDirectory).also {
                    parent.children[segment] = it
                    parent = it
                }
            } else {
                if (!child.isDirectory && mustBeDirectory || child.isDirectory && isLast && !entry.isDirectory) {
                    throw ArchiveValidationException(
                        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                        "Archive snapshot contains a file-directory conflict",
                    )
                }
                parent = child
            }
            if (isLast) {
                if (parent.entry != null) {
                    throw ArchiveValidationException(
                        ArchiveFailureCode.DUPLICATE_PATH,
                        "Archive snapshot contains a duplicate path",
                    )
                }
                parent.entry = entry
            }
        }
    }

    private fun freeze(
        mutable: MutableNode,
        nodes: MutableMap<String, ArchiveNode>,
        childMap: MutableMap<String, List<ArchiveNode>>,
    ): ArchiveNode {
        val frozenChildren = mutable.children.values.map { freeze(it, nodes, childMap) }
            .sortedWith(NODE_COMPARATOR)
        val descendantFileCount = if (mutable.isDirectory) {
            frozenChildren.sumOf(ArchiveNode::descendantFileCount)
        } else {
            1
        }
        val uncompressedSize = if (mutable.isDirectory) {
            frozenChildren.sumOf(ArchiveNode::uncompressedSize)
        } else {
            mutable.entry?.uncompressedSize ?: 0L
        }
        val node = ArchiveNode(
            path = mutable.path,
            name = mutable.name,
            isDirectory = mutable.isDirectory,
            entry = mutable.entry,
            childCount = frozenChildren.size,
            descendantFileCount = descendantFileCount,
            uncompressedSize = uncompressedSize,
        )
        nodes[mutable.path] = node
        if (mutable.isDirectory) childMap[mutable.path] = frozenChildren
        return node
    }

    private fun normalizedLookupPath(path: String): String =
        ArchivePathPolicy.normalizeSelectionPath(path, limits = limits, allowRoot = true)

    private data class MutableNode(
        val path: String,
        val name: String,
        val isDirectory: Boolean,
        var entry: ArchiveEntry? = null,
        val children: LinkedHashMap<String, MutableNode> = LinkedHashMap(),
    )

    private companion object {
        val NODE_COMPARATOR = compareByDescending<ArchiveNode> { it.isDirectory }
            .thenBy { it.name.lowercase(Locale.ROOT) }
            .thenBy(ArchiveNode::name)
            .thenBy(ArchiveNode::path)
    }
}
