package io.github.supermonster003.autojs6.plugin.archivemanager

import java.util.Locale

object ArchiveSelection {

    fun resolve(
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        index: ArchiveIndex = ArchiveIndex(snapshot),
    ): ResolvedArchiveSelection {
        if (selectedPaths.isEmpty()) {
            throw ArchiveSelectionException(
                ArchiveFailureCode.EMPTY_SELECTION,
                "At least one archive path must be selected",
            )
        }

        val requested = selectedPaths.mapTo(LinkedHashSet()) { path ->
            ArchivePathPolicy.normalizeSelectionPath(
                path = path,
                limits = snapshot.limits,
                allowRoot = true,
            )
        }
        val validatedNodes = requested.associateWith { path ->
            index.node(path) ?: run {
                throw ArchiveSelectionException(
                    ArchiveFailureCode.UNKNOWN_SELECTION,
                    "Selected archive path does not exist",
                )
            }
        }
        val allSelectedDirectories = validatedNodes.values.asSequence()
            .filter(ArchiveNode::isDirectory)
            .mapTo(HashSet(), ArchiveNode::path)
        val effectiveRequested = if (ArchivePathPolicy.ROOT_PATH in requested) {
            linkedSetOf(ArchivePathPolicy.ROOT_PATH)
        } else {
            requested.filterTo(LinkedHashSet()) { path ->
                !hasSelectedParentDirectory(path, allSelectedDirectories)
            }
        }
        val requestedNodes = effectiveRequested.associateWith(validatedNodes::getValue)

        val selectedEntries = if (ArchivePathPolicy.ROOT_PATH in effectiveRequested) {
            snapshot.entries
        } else {
            val selectedFiles = requestedNodes.values.asSequence()
                .filterNot(ArchiveNode::isDirectory)
                .mapTo(HashSet(), ArchiveNode::path)
            val selectedDirectories = requestedNodes.values.asSequence()
                .filter(ArchiveNode::isDirectory)
                .mapTo(HashSet(), ArchiveNode::path)
            snapshot.entries.filter { entry ->
                entry.path in selectedFiles || hasSelectedDirectory(entry.path, selectedDirectories)
            }
        }

        val safeSelectedEntries = selectedEntries.filter(ArchiveEntry::isOutputPathSafe)
        val skippedUnsafeEntries = selectedEntries.asSequence()
            .filterNot(ArchiveEntry::isOutputPathSafe)
            .sortedBy(ArchiveEntry::ordinal)
            .toList()
        val directories = LinkedHashSet<String>()
        requestedNodes.values.forEach { selectedNode ->
            selectedNode.takeIf(ArchiveNode::isDirectory)
                ?.path
                ?.takeIf(String::isNotEmpty)
                ?.takeUnless(snapshot::isIsolatedPath)
                ?.let(directories::add)
        }
        safeSelectedEntries.forEach { entry ->
            if (entry.isDirectory) directories += entry.path
            addParentDirectories(entry.path, directories)
        }

        return ResolvedArchiveSelection(
            requestedPaths = effectiveRequested,
            directories = directories.sortedWith(
                compareBy<String> { it.count { character -> character == '/' } }
                    .thenBy { it.lowercase(Locale.ROOT) }
                    .thenBy { it },
            ),
            files = safeSelectedEntries.asSequence()
                .filterNot(ArchiveEntry::isDirectory)
                .sortedBy(ArchiveEntry::ordinal)
                .toList(),
            skippedUnsafeEntries = skippedUnsafeEntries,
        )
    }

    private fun addParentDirectories(path: String, destination: MutableSet<String>) {
        var separator = path.indexOf('/')
        while (separator >= 0) {
            destination += path.substring(0, separator)
            separator = path.indexOf('/', separator + 1)
        }
    }

    private fun hasSelectedDirectory(path: String, selectedDirectories: Set<String>): Boolean {
        var candidate = path
        while (candidate.isNotEmpty()) {
            if (candidate in selectedDirectories) return true
            candidate = candidate.substringBeforeLast('/', "")
        }
        return false
    }

    private fun hasSelectedParentDirectory(path: String, selectedDirectories: Set<String>): Boolean {
        var parent = path.substringBeforeLast('/', "")
        while (parent.isNotEmpty()) {
            if (parent in selectedDirectories) return true
            parent = parent.substringBeforeLast('/', "")
        }
        return ArchivePathPolicy.ROOT_PATH in selectedDirectories
    }
}
