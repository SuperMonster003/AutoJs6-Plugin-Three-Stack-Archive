package io.github.supermonster003.autojs6.plugin.archivemanager

internal enum class ArchiveExtractionScope {
    ENTIRE_ARCHIVE,
    CURRENT_DIRECTORY,
    CURRENT_SELECTION,
}

internal data class ArchiveExtractionScopeOption(
    val scope: ArchiveExtractionScope,
    val requestedPaths: Set<String>,
)

/** Turns a user-facing extraction scope into an immutable archive-path request. */
internal object ArchiveExtractionScopeResolver {

    fun options(
        currentDirectory: String,
        selectedPaths: Collection<String>,
        currentDirectoryCanExtract: Boolean = true,
    ): List<ArchiveExtractionScopeOption> = buildList {
        add(
            ArchiveExtractionScopeOption(
                scope = ArchiveExtractionScope.ENTIRE_ARCHIVE,
                requestedPaths = linkedSetOf(ArchivePathPolicy.ROOT_PATH),
            ),
        )
        if (currentDirectoryCanExtract && currentDirectory != ArchivePathPolicy.ROOT_PATH) {
            add(
                ArchiveExtractionScopeOption(
                    scope = ArchiveExtractionScope.CURRENT_DIRECTORY,
                    requestedPaths = linkedSetOf(currentDirectory),
                ),
            )
        }
        if (selectedPaths.isNotEmpty()) {
            add(
                ArchiveExtractionScopeOption(
                    scope = ArchiveExtractionScope.CURRENT_SELECTION,
                    requestedPaths = selectedPaths.toCollection(LinkedHashSet()),
                ),
            )
        }
    }
}
