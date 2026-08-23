package io.github.supermonster003.autojs6.plugin.archivemanager

import java.io.OutputStream

/**
 * Destination abstraction used by [ArchiveExtractor]. Every create operation must allocate a new
 * node under the supplied parent. Implementations must never return a pre-existing node or a node
 * from another parent.
 */
interface ArchiveOutputWriter {
    interface Node {
        val location: ArchiveOutputLocation
    }

    fun createRoot(displayName: String): Node

    fun createDirectory(parent: Node, displayName: String): Node

    fun createFile(parent: Node, displayName: String): Node

    fun openFile(node: Node): OutputStream

    /** Deletes the root and every document created below it. */
    fun deleteRoot(root: Node)
}
