package io.github.supermonster003.autojs6.plugin.archivemanager

import java.text.Normalizer
import java.util.Locale

data class ValidatedArchivePath(
    val path: String,
    val displayName: String,
    val segments: List<String>,
    val collisionKey: String,
)

object ArchivePathPolicy {

    fun validateEntryPath(
        sourceName: String,
        isDirectory: Boolean,
        limits: ArchiveStructureLimits = ArchiveStructureLimits.DEFAULT,
    ): ValidatedArchivePath {
        val effectiveLimits = limits.restrictedToHardLimits()
        if (sourceName.isEmpty()) invalid("Archive entry path is empty")
        if (sourceName.length > effectiveLimits.maxPathLength) {
            invalid(ArchiveFailureCode.PATH_LIMIT_EXCEEDED, "Archive entry path is too long")
        }
        if (hasUnsafeCodePoint(sourceName)) invalid("Control characters are not allowed in archive paths")
        val portableName = sourceName.replace('\\', '/')
        if (portableName.startsWith('/')) invalid("Absolute archive paths are not allowed")

        val withoutDirectoryMarker = if (isDirectory && portableName.endsWith('/')) {
            portableName.dropLast(1)
        } else {
            portableName
        }
        if (withoutDirectoryMarker.isEmpty()) invalid("Archive entry path is empty")

        val rawSegments = withoutDirectoryMarker
            .split('/')
            .filterNot { it.isEmpty() || it == "." }
        if (rawSegments.isEmpty()) invalid("Archive entry path is empty")
        if (rawSegments.size > effectiveLimits.maxDepth) {
            invalid(ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED, "Archive entry path is too deep")
        }

        val segments = rawSegments.map { rawSegment ->
            if (rawSegment == "..") invalid("Parent path segments are not allowed")
            if (hasDrivePrefix(rawSegment)) invalid("Drive-prefixed archive paths are not allowed")
            rawSegment.also { segment ->
                if (hasUnsafeCodePoint(segment)) invalid("Archive entry path contains an unsafe Unicode sequence")
            }
        }
        val path = segments.joinToString("/")
        if (path.length > effectiveLimits.maxPathLength) {
            invalid(ArchiveFailureCode.PATH_LIMIT_EXCEEDED, "Archive entry path is too long")
        }
        return ValidatedArchivePath(
            path = path,
            displayName = segments.last(),
            segments = segments,
            collisionKey = collisionKey(path),
        )
    }

    fun normalizeSelectionPath(
        path: String,
        limits: ArchiveStructureLimits = ArchiveStructureLimits.DEFAULT,
        allowRoot: Boolean = true,
    ): String {
        if (path.isEmpty() && allowRoot) return ROOT_PATH
        return validateEntryPath(path, isDirectory = false, limits).path
    }

    fun validateDestinationRootName(
        name: String,
        limits: ArchiveStructureLimits = ArchiveStructureLimits.DEFAULT,
    ): String {
        val effectiveLimits = limits.restrictedToHardLimits()
        if (name.isBlank() || name == "." || name == "..") {
            invalid(ArchiveFailureCode.INVALID_DESTINATION_NAME, "Extraction root name is invalid")
        }
        if (name.length > effectiveLimits.maxPathLength || '/' in name || '\\' in name) {
            invalid(ArchiveFailureCode.INVALID_DESTINATION_NAME, "Extraction root name is invalid")
        }
        if (hasDrivePrefix(name) || hasUnsafeCodePoint(name)) {
            invalid(ArchiveFailureCode.INVALID_DESTINATION_NAME, "Extraction root name is invalid")
        }
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFC)
        if (normalized.isBlank() || hasUnsafeCodePoint(normalized)) {
            invalid(ArchiveFailureCode.INVALID_DESTINATION_NAME, "Extraction root name is invalid")
        }
        return normalized
    }

    fun collisionKey(path: String): String = path

    internal fun destinationCollisionKey(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT)

    internal fun isolatedEntryPath(
        root: String,
        ordinal: Int,
        isDirectory: Boolean,
        limits: ArchiveStructureLimits = ArchiveStructureLimits.DEFAULT,
    ): ValidatedArchivePath {
        require(ordinal >= 0)
        val entryNumber = ordinal.toLong() + 1L
        val leaf = "entry-${entryNumber.toString().padStart(ISOLATED_ENTRY_NUMBER_WIDTH, '0')}"
        return validateEntryPath("$root/$leaf", isDirectory, limits)
    }

    internal fun unsafeSourceNameForDisplay(sourceName: String): String {
        if (sourceName.isEmpty()) return EMPTY_SOURCE_NAME_DISPLAY
        val result = StringBuilder(sourceName.length.coerceAtMost(MAX_UNSAFE_DISPLAY_NAME_LENGTH))
        var offset = 0
        while (offset < sourceName.length && result.length < MAX_UNSAFE_DISPLAY_NAME_LENGTH) {
            val codePoint = sourceName.codePointAt(offset)
            val escaped = when {
                isUnsafeCodePoint(codePoint) || Character.getType(codePoint) == Character.SURROGATE.toInt() ->
                    if (codePoint <= 0xFFFF) {
                        "\\u${codePoint.toString(16).uppercase().padStart(4, '0')}"
                    } else {
                        "\\U${codePoint.toString(16).uppercase().padStart(8, '0')}"
                    }
                else -> String(Character.toChars(codePoint))
            }
            if (result.length + escaped.length > MAX_UNSAFE_DISPLAY_NAME_LENGTH) break
            result.append(escaped)
            offset += Character.charCount(codePoint)
        }
        if (offset < sourceName.length) result.append("...")
        return result.toString().ifEmpty { EMPTY_SOURCE_NAME_DISPLAY }
    }

    private fun hasDrivePrefix(segment: String): Boolean =
        segment.length >= 2 && segment[0].isAsciiLetter() && segment[1] == ':'

    private fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'

    private fun hasUnsafeCodePoint(value: String): Boolean {
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            if (isUnsafeCodePoint(codePoint)) return true
            offset += Character.charCount(codePoint)
        }
        return false
    }

    private fun isUnsafeCodePoint(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.CONTROL.toInt(),
        Character.FORMAT.toInt(),
        Character.LINE_SEPARATOR.toInt(),
        Character.PARAGRAPH_SEPARATOR.toInt(),
        Character.SURROGATE.toInt(),
        -> true
        else -> false
    }

    private fun invalid(
        message: String,
    ): Nothing = invalid(ArchiveFailureCode.INVALID_PATH, message)

    private fun invalid(
        code: ArchiveFailureCode,
        message: String,
    ): Nothing = throw ArchiveValidationException(code, message)

    const val ROOT_PATH = ""
    internal const val ISOLATED_PATH_ROOT_BASENAME = "__archive_manager_unsafe_paths__"
    internal const val DEFAULT_ISOLATED_PATH_DISPLAY_NAME = "Unsafe paths"
    private const val EMPTY_SOURCE_NAME_DISPLAY = "(empty name)"
    private const val ISOLATED_ENTRY_NUMBER_WIDTH = 6
    private const val MAX_UNSAFE_DISPLAY_NAME_LENGTH = 512
}
