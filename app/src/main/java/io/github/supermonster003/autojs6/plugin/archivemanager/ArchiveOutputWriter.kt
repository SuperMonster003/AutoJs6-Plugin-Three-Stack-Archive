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
        val isDirectory: Boolean
    }

    fun createRoot(displayName: String): Node

    fun createDirectory(parent: Node, displayName: String): Node

    fun createFile(parent: Node, displayName: String): Node

    /** Returns a destination-name-equivalent child, if one is already present. */
    fun findChild(parent: Node, displayName: String): Node?

    /** True only when reusing [node] cannot modify content that predates this extraction. */
    fun canOverwrite(node: Node, incomingIsDirectory: Boolean): Boolean

    fun openFile(node: Node): OutputStream

    /** Deletes the root and every document created below it. */
    fun deleteRoot(root: Node)
}
