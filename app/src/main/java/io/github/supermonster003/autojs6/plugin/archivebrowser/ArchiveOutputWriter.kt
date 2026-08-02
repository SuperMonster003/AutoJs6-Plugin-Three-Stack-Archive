package io.github.supermonster003.autojs6.plugin.archivebrowser

import java.io.OutputStream

/**
 * Destination abstraction used by [ArchiveExtractor]. Implementations must create a new root and
 * must never return a pre-existing directory from [createRoot].
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
