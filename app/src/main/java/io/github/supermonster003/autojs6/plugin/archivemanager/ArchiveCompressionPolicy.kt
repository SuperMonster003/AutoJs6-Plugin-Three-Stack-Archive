package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import java.util.Locale

internal object ArchiveCompressionPolicy {

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
        return normalizeOutputDisplayName(sanitizedStem, format)
            ?: normalizeOutputDisplayName(fallbackStem, format)
            ?: "$FALLBACK_OUTPUT_STEM.${format.primaryExtension}"
    }

    fun normalizeOutputDisplayName(
        value: String?,
        format: ArchiveFormat = ArchiveFormat.ZIP,
    ): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val extension = ".${format.primaryExtension}"
        val withExtension = if (raw.lowercase(Locale.ROOT).endsWith(extension)) {
            raw
        } else {
            raw + extension
        }
        if (withExtension.length !in 1..ExplorerActionProtocol.MAX_OUTPUT_DISPLAY_NAME_LENGTH) return null
        if (withExtension == "." || withExtension == "..") return null
        if (withExtension.any(::isUnsafeNameCharacter)) return null
        val stem = withExtension.dropLast(extension.length)
        if (stem.all { it == '.' || it.isWhitespace() }) return null
        return withExtension
    }

    fun requireCompressionLevel(value: Int): Int {
        require(value in MIN_COMPRESSION_LEVEL..MAX_COMPRESSION_LEVEL) {
            "ZIP compression level must be between $MIN_COMPRESSION_LEVEL and $MAX_COMPRESSION_LEVEL"
        }
        return value
    }

    fun requirePortableEntrySegment(value: String): String {
        require(value.isNotBlank() && value != "." && value != "..") {
            "Source name is not a portable ZIP entry segment"
        }
        require(value.none(::isUnsafeEntryCharacter)) {
            "Source name contains a character that cannot be represented safely in a ZIP path"
        }
        return value
    }

    fun joinEntryPath(parent: String, child: String): String {
        val segment = requirePortableEntrySegment(child)
        val result = if (parent.isEmpty()) segment else "$parent/$segment"
        require(result.length <= ExplorerActionProtocol.MAX_SESSION_RELATIVE_PATH_LENGTH) {
            "ZIP entry path is too long"
        }
        return result
    }

    private fun isUnsafeNameCharacter(value: Char): Boolean =
        value == '/' || value == '\\' || value == '\u0000' || value.code < 0x20 || value.code == 0x7f

    private fun isUnsafeEntryCharacter(value: Char): Boolean =
        isUnsafeNameCharacter(value)

    const val ZIP_MIME_TYPE = "application/zip"
    const val DEFAULT_COMPRESSION_LEVEL = 6
    private const val MIN_COMPRESSION_LEVEL = 0
    private const val MAX_COMPRESSION_LEVEL = 9
    private const val FALLBACK_OUTPUT_STEM = "Archive"
}
