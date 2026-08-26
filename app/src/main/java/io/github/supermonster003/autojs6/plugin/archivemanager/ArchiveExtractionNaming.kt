package io.github.supermonster003.autojs6.plugin.archivemanager

internal object ArchiveExtractionNaming {

    fun rootName(displayName: String, format: ArchiveFormat): String {
        val candidate = (format.baseNameWithoutArchiveExtension(displayName)
            ?: displayName.substringBeforeLast('.', displayName))
            .ifBlank { DEFAULT_ROOT_NAME }
            .take(MAX_ROOT_NAME_LENGTH)
        return runCatching { ArchivePathPolicy.validateDestinationRootName(candidate) }
            .getOrDefault(DEFAULT_ROOT_NAME)
    }

    private const val DEFAULT_ROOT_NAME = "archive"
    private const val MAX_ROOT_NAME_LENGTH = 120
}
