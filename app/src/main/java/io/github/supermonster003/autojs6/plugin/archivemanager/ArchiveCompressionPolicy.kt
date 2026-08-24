package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import java.util.Locale

internal object ArchiveCompressionPolicy {

    fun validateOutputDisplayName(value: String?): String? {
        val candidate = value?.takeIf {
            it.length in 1..ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH &&
                it == it.trim() &&
                it != "." &&
                it != ".." &&
                it.any { character -> character != '.' && !character.isWhitespace() } &&
                it.none(::isUnsafeNameCharacter)
        }
        return candidate
    }

    fun defaultOutputDisplayName(
        targetDisplayNames: List<String>,
        parentDisplayPath: String,
        fallbackStem: String,
        format: ArchiveFormat = ArchiveFormat.ZIP,
    ): String {
        val preferredStem = if (targetDisplayNames.size == 1) {
            targetDisplayNames.single()
        } else {
            parentDisplayPath
                .trimEnd('/', '\\')
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .takeIf { it.isNotBlank() }
                ?: fallbackStem
        }
        val sanitizedStem = preferredStem
            .map { character ->
                if (
                    character == '/' || character == '\\' || character == '\u0000' ||
                    character.code < 0x20 || character.code == 0x7f
                ) {
                    '_'
                } else {
                    character
                }
            }
            .joinToString("")
            .trim()
            .trimEnd('.')
            .takeIf { it.isNotBlank() }
            ?: fallbackStem
        return defaultNameCandidate(sanitizedStem, format)
            ?: defaultNameCandidate(fallbackStem, format)
            ?: "$FALLBACK_OUTPUT_STEM.${format.primaryExtension}"
    }

    fun normalizeOutputDisplayName(
        value: String?,
        format: ArchiveFormat = ArchiveFormat.ZIP,
    ): String? = normalizeOutputDisplayName(
        value = value,
        format = format,
        maximumLength = ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH,
    )

    fun normalizeCreationOutputDisplayName(
        value: String?,
        format: ArchiveFormat,
        splitVolumeSizeBytes: Long?,
    ): String? {
        return if (splitVolumeSizeBytes == null) {
            normalizeOutputDisplayName(value, format)
        } else {
            if (format != ArchiveFormat.ZIP) return null
            ArchiveSplitVolumePolicy.requireVolumeSizeBytes(splitVolumeSizeBytes)
            normalizeOutputDisplayName(
                value = value,
                format = format,
                maximumLength = ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH,
            )
        }
    }

    fun fitSplitZipOutputDisplayName(value: String?): String? {
        val normalized = normalizeOutputDisplayName(value, ArchiveFormat.ZIP) ?: return null
        val extension = ".${ArchiveFormat.ZIP.primaryExtension}"
        val maximumStemLength =
            ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH - extension.length
        val stem = normalized.dropLast(extension.length)
        val normalizedExtension = normalized.takeLast(extension.length)
        return normalizeOutputDisplayName(
            value = truncateStem(stem, maximumStemLength)?.plus(normalizedExtension),
            format = ArchiveFormat.ZIP,
            maximumLength = ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH,
        )
    }

    fun numberedCreationOutputDisplayName(
        value: String,
        format: ArchiveFormat,
        index: Int,
        splitVolumeSizeBytes: Long?,
    ): String {
        require(index >= 0)
        val normalized = normalizeCreationOutputDisplayName(
            value = value,
            format = format,
            splitVolumeSizeBytes = splitVolumeSizeBytes,
        ) ?: throw IllegalArgumentException("Archive output name is invalid")
        if (index == 0) return normalized
        val extension = ".${format.primaryExtension}"
        val suffix = " ($index)"
        val maximumLength = if (splitVolumeSizeBytes == null) {
            ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH
        } else {
            ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH
        }
        val maximumStemLength = maximumLength - extension.length - suffix.length
        require(maximumStemLength >= 1) { "Archive output name has no room for numbering" }
        val stem = truncateStem(normalized.dropLast(extension.length), maximumStemLength)
            ?: throw IllegalArgumentException("Archive output name cannot be numbered")
        return "$stem$suffix${normalized.takeLast(extension.length)}"
    }

    private fun normalizeOutputDisplayName(
        value: String?,
        format: ArchiveFormat,
        maximumLength: Int,
    ): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val extension = ".${format.primaryExtension}"
        val withExtension = if (raw.lowercase(Locale.ROOT).endsWith(extension)) {
            raw
        } else {
            raw + extension
        }
        if (withExtension.length !in 1..maximumLength) return null
        if (validateOutputDisplayName(withExtension) == null) return null
        val stem = withExtension.dropLast(extension.length)
        if (stem.all { it == '.' || it.isWhitespace() }) return null
        return withExtension
    }

    fun outputDisplayNameForFormat(
        value: String?,
        previousFormat: ArchiveFormat,
        nextFormat: ArchiveFormat,
    ): String? {
        val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val stem = previousFormat.baseNameWithoutArchiveExtension(raw) ?: raw
        return normalizeOutputDisplayName(stem, nextFormat)
    }

    fun requireCompressionLevel(
        value: Int,
        supportedLevels: List<Int> = (MIN_COMPRESSION_LEVEL..MAX_COMPRESSION_LEVEL).toList(),
        format: ArchiveFormat = ArchiveFormat.ZIP,
    ): Int {
        require(value in supportedLevels) {
            "${format.displayName} compression level $value is not supported"
        }
        return value
    }

    /** Compares transient password buffers without converting either value to an immutable [String]. */
    fun passwordConfirmationMatches(password: CharArray, confirmation: CharArray): Boolean {
        var difference = password.size xor confirmation.size
        val comparedLength = maxOf(password.size, confirmation.size)
        for (index in 0 until comparedLength) {
            val passwordCharacter = password.getOrNull(index)?.code ?: 0
            val confirmationCharacter = confirmation.getOrNull(index)?.code ?: 0
            difference = difference or (passwordCharacter xor confirmationCharacter)
        }
        return difference == 0
    }

    fun requirePortableEntrySegment(value: String): String {
        require(value.isNotBlank() && value != "." && value != "..") {
            "Source name is not a portable archive entry segment"
        }
        require(value.none(::isUnsafeEntryCharacter)) {
            "Source name contains a character that cannot be represented safely in an archive path"
        }
        return value
    }

    fun joinEntryPath(parent: String, child: String): String {
        val segment = requirePortableEntrySegment(child)
        val result = if (parent.isEmpty()) segment else "$parent/$segment"
        require(result.length <= ExplorerActionProtocol.MAX_SESSION_RELATIVE_PATH_LENGTH) {
            "Archive entry path is too long"
        }
        return result
    }

    private fun isUnsafeNameCharacter(value: Char): Boolean =
        value == '/' || value == '\\' || value == '\u0000' || value.code < 0x20 || value.code == 0x7f

    private fun isUnsafeEntryCharacter(value: Char): Boolean =
        isUnsafeNameCharacter(value)

    private fun defaultNameCandidate(
        value: String,
        format: ArchiveFormat,
    ): String? = normalizeOutputDisplayName(value, format)
        ?: truncateDefaultStem(value, format)?.let { boundedStem ->
            normalizeOutputDisplayName(boundedStem, format)
        }

    private fun truncateDefaultStem(value: String, format: ArchiveFormat): String? {
        val extension = ".${format.primaryExtension}"
        val stem = if (value.lowercase(Locale.ROOT).endsWith(extension)) {
            value.dropLast(extension.length)
        } else {
            value
        }
        val maxStemLength = ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH - extension.length
        if (maxStemLength < 1) return null
        return truncateStem(stem, maxStemLength)
    }

    private fun truncateStem(value: String, maximumLength: Int): String? {
        if (maximumLength < 1) return null
        val truncated = value.take(maximumLength).let { candidate ->
            if (candidate.lastOrNull()?.let(Character::isHighSurrogate) == true) {
                candidate.dropLast(1)
            } else {
                candidate
            }
        }
        return truncated
            .trim()
            .trimEnd('.')
            .takeIf { candidate -> candidate.any { it != '.' && !it.isWhitespace() } }
    }

    const val DEFAULT_COMPRESSION_LEVEL = 6
    private const val MIN_COMPRESSION_LEVEL = 0
    private const val MAX_COMPRESSION_LEVEL = 9
    private const val FALLBACK_OUTPUT_STEM = "Archive"
}
