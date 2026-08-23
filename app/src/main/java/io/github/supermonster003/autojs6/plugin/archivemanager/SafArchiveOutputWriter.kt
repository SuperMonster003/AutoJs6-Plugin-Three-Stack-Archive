package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.io.OutputStream

class SafArchiveOutputWriter(
    private val contentResolver: ContentResolver,
    private val treeUri: Uri,
) : ArchiveOutputWriter {

    private val createdDocumentIds = HashSet<String>()

    init {
        require(treeUri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Extraction destination must use a content URI"
        }
        require(DocumentsContract.isTreeUri(treeUri)) {
            "Extraction destination must be a document tree URI"
        }
    }

    override fun createRoot(displayName: String): ArchiveOutputWriter.Node {
        val treeDocumentId = safCall("Cannot resolve extraction tree") {
            DocumentsContract.getTreeDocumentId(treeUri)
        }
        val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        val existing = queryChildren(treeDocumentUri)
        val uniqueName = uniqueRootName(displayName, existing.displayNames)
        return createDocument(
            parent = treeDocumentUri,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            displayName = uniqueName,
            existingDocumentIds = existing.documentIds,
        )
    }

    override fun createDirectory(
        parent: ArchiveOutputWriter.Node,
        displayName: String,
    ): ArchiveOutputWriter.Node = createChildDocument(
        parent = requireSafNode(parent).uri,
        mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
        displayName = displayName,
    )

    override fun createFile(
        parent: ArchiveOutputWriter.Node,
        displayName: String,
    ): ArchiveOutputWriter.Node = createChildDocument(
        parent = requireSafNode(parent).uri,
        mimeType = BINARY_MIME_TYPE,
        displayName = displayName,
    )

    override fun openFile(node: ArchiveOutputWriter.Node): OutputStream =
        safCall("Cannot open extraction output") {
            contentResolver.openOutputStream(requireSafNode(node).uri, WRITE_MODE)
                ?: throw IOException("Document provider returned no output stream")
        }

    override fun deleteRoot(root: ArchiveOutputWriter.Node) {
        val deleted = safCall("Cannot clean up extraction output") {
            DocumentsContract.deleteDocument(contentResolver, requireSafNode(root).uri)
        }
        if (!deleted) throw IOException("Document provider refused to delete extraction output")
    }

    private fun createDocument(
        parent: Uri,
        mimeType: String,
        displayName: String,
        existingDocumentIds: Set<String>,
    ): SafNode = safCall("Cannot create extraction output") {
        val uri = DocumentsContract.createDocument(
            contentResolver,
            parent,
            mimeType,
            displayName,
        ) ?: throw IOException("Document provider returned no document URI")
        val documentId = DocumentsContract.getDocumentId(uri)
        if (documentId in existingDocumentIds || !createdDocumentIds.add(documentId)) {
            throw IOException("Document provider returned a pre-existing output document")
        }
        val actualName = runCatching { queryDisplayName(uri) }.getOrNull() ?: displayName
        SafNode(
            uri = uri,
            location = ArchiveOutputLocation(uri.toString(), actualName),
        )
    }

    private fun createChildDocument(
        parent: Uri,
        mimeType: String,
        displayName: String,
    ): SafNode = createDocument(parent, mimeType, displayName, emptySet())

    private fun queryChildren(parent: Uri): ExistingChildren = safCall("Cannot inspect extraction output") {
        val parentDocumentId = DocumentsContract.getDocumentId(parent)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val documentIds = LinkedHashSet<String>()
        val displayNames = LinkedHashSet<String>()
        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (idColumn >= 0) cursor.getString(idColumn)?.let(documentIds::add)
                if (nameColumn >= 0) cursor.getString(nameColumn)?.let(displayNames::add)
            }
        } ?: throw IOException("Document provider returned no child cursor")
        ExistingChildren(documentIds, displayNames)
    }

    private fun queryDisplayName(documentUri: Uri): String? =
        contentResolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val column = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            cursor.takeIf { column >= 0 && it.moveToFirst() }?.getString(column)
        }

    private fun uniqueRootName(requested: String, existingNames: Set<String>): String {
        val collisionKeys = existingNames.mapTo(HashSet(), ArchivePathPolicy::destinationCollisionKey)
        if (ArchivePathPolicy.destinationCollisionKey(requested) !in collisionKeys) return requested
        for (suffix in 2..MAX_ROOT_NAME_ATTEMPTS) {
            val candidate = "$requested ($suffix)"
            if (ArchivePathPolicy.destinationCollisionKey(candidate) !in collisionKeys) return candidate
        }
        throw IOException("Cannot allocate a unique extraction output name")
    }

    private fun requireSafNode(node: ArchiveOutputWriter.Node): SafNode =
        node as? SafNode ?: throw IOException("Output node does not belong to this SAF writer")

    private inline fun <T> safCall(message: String, block: () -> T): T = try {
        block()
    } catch (error: IOException) {
        throw error
    } catch (error: SecurityException) {
        throw IOException(message, error)
    } catch (error: IllegalArgumentException) {
        throw IOException(message, error)
    } catch (error: UnsupportedOperationException) {
        throw IOException(message, error)
    }

    private data class SafNode(
        val uri: Uri,
        override val location: ArchiveOutputLocation,
    ) : ArchiveOutputWriter.Node

    private data class ExistingChildren(
        val documentIds: Set<String>,
        val displayNames: Set<String>,
    )

    private companion object {
        const val BINARY_MIME_TYPE = "application/octet-stream"
        const val MAX_ROOT_NAME_ATTEMPTS = 10_000
        const val WRITE_MODE = "w"
    }
}
