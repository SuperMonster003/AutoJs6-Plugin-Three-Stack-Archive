package io.github.supermonster003.autojs6.plugin.archivemanager

internal object ArchiveExtractionNaming {

    fun rootName(displayName: String, format: ArchiveFormat): String {
        val numberedBaseName = NumberedArchiveVolumePolicy.inspectFirstVolume(displayName)
            ?.takeIf { volume -> volume.format == format }
            ?.archiveDisplayName
            ?.let(format::baseNameWithoutArchiveExtension)
        val candidate = (numberedBaseName
            ?: format.baseNameWithoutArchiveExtension(displayName)
            ?: displayName.substringBeforeLast('.', displayName))
            .ifBlank { DEFAULT_ROOT_NAME }
            .take(MAX_ROOT_NAME_LENGTH)
        return runCatching { ArchivePathPolicy.validateDestinationRootName(candidate) }
            .getOrDefault(DEFAULT_ROOT_NAME)
    }

    private const val DEFAULT_ROOT_NAME = "archive"
    private const val MAX_ROOT_NAME_LENGTH = 120
}
