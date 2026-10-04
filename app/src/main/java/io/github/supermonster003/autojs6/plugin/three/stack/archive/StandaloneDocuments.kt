package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

internal data class StandaloneDocument(val uri: Uri, val name: String, val mimeType: String, val size: Long)

internal object StandaloneDocuments {
    /** Only user-selected content capabilities cross this private app boundary. */
    fun inspect(context: Context, uri: Uri): StandaloneDocument {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT && !uri.authority.isNullOrBlank())
        var name = "document"
        var size = -1L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        require(name.length in 1..255 && name.isNotBlank() && name != "." && name != "..")
        require(name.none { it == '/' || it == '\\' || it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() })
        require(size >= -1)
        return StandaloneDocument(uri, name, context.contentResolver.getType(uri) ?: "application/octet-stream", size)
    }
}
