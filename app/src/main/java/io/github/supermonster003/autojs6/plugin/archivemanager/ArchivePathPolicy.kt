package io.github.supermonster003.autojs6.plugin.archivemanager

import java.text.Normalizer

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
        limits: ArchiveSecurityLimits = ArchiveSecurityLimits.DEFAULT,
    ): ValidatedArchivePath {
        if (sourceName.isEmpty()) invalid("Archive entry path is empty")
        if (sourceName.length > limits.maxPathLength) {
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
        if (rawSegments.size > limits.maxDepth) {
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
        if (path.length > limits.maxPathLength) {
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
        limits: ArchiveSecurityLimits = ArchiveSecurityLimits.DEFAULT,
        allowRoot: Boolean = true,
    ): String {
        if (path.isEmpty() && allowRoot) return ROOT_PATH
        return validateEntryPath(path, isDirectory = false, limits).path
    }

    fun validateDestinationRootName(
        name: String,
        limits: ArchiveSecurityLimits = ArchiveSecurityLimits.DEFAULT,
    ): String {
        if (name.isBlank() || name == "." || name == "..") {
            invalid(ArchiveFailureCode.INVALID_DESTINATION_NAME, "Extraction root name is invalid")
        }
        if (name.length > limits.maxPathLength || '/' in name || '\\' in name) {
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

    private fun hasDrivePrefix(segment: String): Boolean =
        segment.length >= 2 && segment[0].isAsciiLetter() && segment[1] == ':'

    private fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'

    private fun hasUnsafeCodePoint(value: String): Boolean {
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            when (Character.getType(codePoint)) {
                Character.CONTROL.toInt(),
                Character.FORMAT.toInt(),
                Character.LINE_SEPARATOR.toInt(),
                Character.PARAGRAPH_SEPARATOR.toInt(),
                -> return true
            }
            offset += Character.charCount(codePoint)
        }
        return false
    }

    private fun invalid(
        message: String,
    ): Nothing = invalid(ArchiveFailureCode.INVALID_PATH, message)

    private fun invalid(
        code: ArchiveFailureCode,
        message: String,
    ): Nothing = throw ArchiveValidationException(code, message)

    const val ROOT_PATH = ""
}
