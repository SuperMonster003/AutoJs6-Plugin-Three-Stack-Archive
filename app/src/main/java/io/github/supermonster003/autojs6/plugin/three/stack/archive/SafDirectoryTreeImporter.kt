package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.CancellationException

/**
 * Takes a bounded, deterministic snapshot of one SAF directory tree for an archive mutation.
 * File contents remain lazy and are opened only while the replacement archive is written.
 */
internal class SafDirectoryTreeImporter(
    private val contentResolver: ContentResolver,
    private val format: ArchiveFormat = ArchiveFormat.ZIP,
    limits: ArchiveStructureLimits = ArchiveStructureLimits.DEFAULT,
    private val maxEntries: Int = minOf(limits.maxEntries, MAX_IMPORT_ENTRIES),
    private val maxPathCharacters: Long = MAX_IMPORT_PATH_CHARACTERS,
) {
    private val limits = limits.restrictedToHardLimits()

    init {
        require(maxEntries in 1..minOf(this.limits.maxEntries, MAX_IMPORT_ENTRIES))
        require(maxPathCharacters in 1L..MAX_IMPORT_PATH_CHARACTERS)
    }

    fun scan(
        treeUri: Uri,
        checkCancelled: () -> Unit,
    ): List<ArchiveMutationAddedTreeEntry> = try {
        scanChecked(treeUri, checkCancelled)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: ArchiveValidationException) {
        throw failure
    } catch (failure: RuntimeException) {
        throw unreadable("The selected folder cannot be read", failure)
    }

    private fun scanChecked(
        treeUri: Uri,
        checkCancelled: () -> Unit,
    ): List<ArchiveMutationAddedTreeEntry> {
        checkCancelled()
        if (
            !treeUri.isHierarchical ||
            !treeUri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true) ||
            treeUri.authority.isNullOrBlank() ||
            !DocumentsContract.isTreeUri(treeUri)
        ) {
            invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "The selected folder URI is invalid")
        }
        val rootId = requireDocumentId(DocumentsContract.getTreeDocumentId(treeUri))
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val root = querySingleDocument(rootUri, checkCancelled)
        if (root.documentId != rootId || !root.isDirectory) {
            invalid(ArchiveFailureCode.SOURCE_NOT_FILE, "The selected document is not a folder")
        }
        val rootName = requireLeafName(root.displayName)
        val rootPath = validatedPath(rootName, isDirectory = true)

        val entries = ArrayList<ArchiveMutationAddedTreeEntry>(minOf(maxEntries, INITIAL_CAPACITY))
        entries += ArchiveMutationAddedTreeEntry.Directory(rootPath, root.lastModified)
        var pathCharacters = rootPath.length.toLong()
        val visitedDocumentIds = hashSetOf(rootId)
        val pending = ArrayDeque<PendingDirectory>()
        pending.addLast(PendingDirectory(rootId, rootPath))

        while (pending.isNotEmpty()) {
            checkCancelled()
            val parent = pending.removeLast()
            val children = queryChildren(treeUri, parent.documentId, checkCancelled)
            validatePortableSiblingNames(children)
            val childDirectories = ArrayList<PendingDirectory>()
            children.forEach { child ->
                if (!visitedDocumentIds.add(child.documentId)) {
                    invalid(
                        ArchiveFailureCode.INVALID_PATH,
                        "The selected folder contains a repeated or cyclic document",
                    )
                }
                if (entries.size >= maxEntries) {
                    invalid(
                        ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                        "The selected folder contains too many entries",
                    )
                }
                val leaf = requireLeafName(child.displayName)
                val relativePath = validatedPath(
                    sourceName = "${parent.relativePath}/$leaf",
                    isDirectory = child.isDirectory,
                )
                pathCharacters = try {
                    Math.addExact(pathCharacters, relativePath.length.toLong())
                } catch (overflow: ArithmeticException) {
                    invalid(ArchiveFailureCode.PATH_LIMIT_EXCEEDED, "Folder path metadata is too large")
                }
                if (pathCharacters > maxPathCharacters) {
                    invalid(ArchiveFailureCode.PATH_LIMIT_EXCEEDED, "Folder path metadata is too large")
                }
                if (child.isDirectory) {
                    entries += ArchiveMutationAddedTreeEntry.Directory(
                        relativePath = relativePath,
                        lastModified = child.lastModified,
                    )
                    childDirectories += PendingDirectory(child.documentId, relativePath)
                } else {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        child.documentId,
                    )
                    entries += ArchiveMutationAddedTreeEntry.FileEntry(
                        relativePath = relativePath,
                        file = ArchiveMutationAddedFile(
                            displayName = leaf,
                            size = child.size,
                            lastModified = child.lastModified,
                        ) {
                            contentResolver.openInputStream(documentUri)
                                ?: throw IOException("Selected file cannot be opened")
                        },
                    )
                }
            }
            childDirectories.asReversed().forEach(pending::addLast)
        }
        checkCancelled()
        return entries
    }

    private fun querySingleDocument(
        documentUri: Uri,
        checkCancelled: () -> Unit,
    ): DocumentMetadata {
        checkCancelled()
        val result = contentResolver.query(
            documentUri,
            DOCUMENT_PROJECTION,
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val document = cursor.readDocumentMetadata()
            if (cursor.moveToNext()) {
                invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "Folder metadata is ambiguous")
            }
            document
        } ?: invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "Folder metadata is unavailable")
        checkCancelled()
        return result
    }

    private fun queryChildren(
        treeUri: Uri,
        parentDocumentId: String,
        checkCancelled: () -> Unit,
    ): List<DocumentMetadata> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            parentDocumentId,
        )
        checkCancelled()
        val children = contentResolver.query(
            childrenUri,
            DOCUMENT_PROJECTION,
            null,
            null,
            null,
        )?.use { cursor ->
            buildList<DocumentMetadata> {
                while (cursor.moveToNext()) {
                    checkCancelled()
                    if (this.size >= maxEntries) {
                        invalid(
                            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                            "The selected folder contains too many entries",
                        )
                    }
                    add(cursor.readDocumentMetadata())
                }
            }
        } ?: invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "Folder contents are unavailable")
        checkCancelled()
        return children.sortedWith(
            compareBy<DocumentMetadata>(
                { ArchivePathPolicy.destinationCollisionKey(it.displayName) },
                DocumentMetadata::displayName,
                DocumentMetadata::documentId,
            ),
        )
    }

    private fun Cursor.readDocumentMetadata(): DocumentMetadata {
        val documentId = requireDocumentId(requiredString(DOCUMENT_ID_COLUMN))
        val displayName = requiredString(DISPLAY_NAME_COLUMN)
        val mimeType = requiredString(MIME_TYPE_COLUMN)
        if (mimeType.length > MAX_MIME_TYPE_LENGTH) {
            invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "A selected document type is invalid")
        }
        val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val size = if (isDirectory) {
            0L
        } else {
            optionalLong(SIZE_COLUMN).takeIf { it >= 0L } ?: -1L
        }
        val lastModified = optionalLong(LAST_MODIFIED_COLUMN).takeIf { it >= 0L } ?: -1L
        return DocumentMetadata(documentId, displayName, isDirectory, size, lastModified)
    }

    private fun Cursor.requiredString(columnName: String): String {
        val column = getColumnIndex(columnName)
        if (column < 0 || isNull(column)) {
            invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "Folder metadata is incomplete")
        }
        return getString(column)
    }

    private fun Cursor.optionalLong(columnName: String): Long {
        val column = getColumnIndex(columnName)
        return if (column >= 0 && !isNull(column)) getLong(column) else -1L
    }

    private fun validatePortableSiblingNames(children: List<DocumentMetadata>) {
        val seen = HashMap<String, DocumentMetadata>(children.size)
        children.forEach { child ->
            val leaf = requireLeafName(child.displayName)
            val key = ArchivePathPolicy.destinationCollisionKey(leaf)
            val previous = seen.putIfAbsent(key, child) ?: return@forEach
            invalid(
                if (previous.isDirectory == child.isDirectory) {
                    ArchiveFailureCode.DUPLICATE_PATH
                } else {
                    ArchiveFailureCode.FILE_DIRECTORY_CONFLICT
                },
                "The selected folder contains names that collide on portable filesystems",
            )
        }
    }

    private fun validatedPath(sourceName: String, isDirectory: Boolean): String =
        ArchivePathPolicy.validateEntryPath(sourceName, isDirectory, limits).path

    private fun requireLeafName(value: String): String =
        ArchiveIntentPolicy.validateDisplayName(value)
            ?: invalid(ArchiveFailureCode.INVALID_DESTINATION_NAME, "A selected item name is invalid")

    private fun requireDocumentId(value: String): String {
        if (
            value.isBlank() ||
            value.length > MAX_DOCUMENT_ID_LENGTH ||
            value.any { it == '\u0000' || it.code < 0x20 || it.code == 0x7f }
        ) {
            invalid(ArchiveFailureCode.SOURCE_UNREADABLE, "A selected document identifier is invalid")
        }
        return value
    }

    private fun unreadable(message: String, cause: Throwable): ArchiveValidationException =
        ArchiveValidationException(
            code = ArchiveFailureCode.SOURCE_UNREADABLE,
            message = message,
            cause = cause,
            format = format,
            stage = ArchiveFailureStage.INPUT,
        )

    private fun invalid(code: ArchiveFailureCode, message: String): Nothing =
        throw ArchiveValidationException(
            code = code,
            message = message,
            format = format,
            stage = ArchiveFailureStage.INPUT,
        )

    private data class PendingDirectory(
        val documentId: String,
        val relativePath: String,
    )

    private data class DocumentMetadata(
        val documentId: String,
        val displayName: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
    )

    companion object {
        const val MAX_IMPORT_ENTRIES = 100_000
        private const val INITIAL_CAPACITY = 1_024
        private const val MAX_DOCUMENT_ID_LENGTH = 4_096
        private const val MAX_MIME_TYPE_LENGTH = 255
        private const val MAX_IMPORT_PATH_CHARACTERS = 16L * 1_024L * 1_024L
        private const val DOCUMENT_ID_COLUMN = DocumentsContract.Document.COLUMN_DOCUMENT_ID
        private const val DISPLAY_NAME_COLUMN = DocumentsContract.Document.COLUMN_DISPLAY_NAME
        private const val MIME_TYPE_COLUMN = DocumentsContract.Document.COLUMN_MIME_TYPE
        private const val SIZE_COLUMN = DocumentsContract.Document.COLUMN_SIZE
        private const val LAST_MODIFIED_COLUMN = DocumentsContract.Document.COLUMN_LAST_MODIFIED
        private val DOCUMENT_PROJECTION = arrayOf(
            DOCUMENT_ID_COLUMN,
            DISPLAY_NAME_COLUMN,
            MIME_TYPE_COLUMN,
            SIZE_COLUMN,
            LAST_MODIFIED_COLUMN,
        )
    }
}
