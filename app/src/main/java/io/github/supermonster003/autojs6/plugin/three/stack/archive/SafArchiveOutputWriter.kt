package io.github.supermonster003.autojs6.plugin.three.stack.archive

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
        val uniqueName = uniqueRootName(
            displayName,
            existing.mapTo(LinkedHashSet()) { it.location.displayName },
        )
        return createDocument(
            parent = treeDocumentUri,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            displayName = uniqueName,
            existingDocumentIds = existing.mapTo(HashSet(), SafNode::documentId),
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

    override fun findChild(
        parent: ArchiveOutputWriter.Node,
        displayName: String,
    ): ArchiveOutputWriter.Node? {
        val collisionKey = ArchivePathPolicy.destinationCollisionKey(displayName)
        return queryChildren(requireSafNode(parent).uri).firstOrNull {
            ArchivePathPolicy.destinationCollisionKey(it.location.displayName) == collisionKey
        }
    }

    override fun canOverwrite(
        node: ArchiveOutputWriter.Node,
        incomingIsDirectory: Boolean,
    ): Boolean {
        val safNode = requireSafNode(node)
        return safNode.documentId in createdDocumentIds &&
            safNode.isDirectory == incomingIsDirectory
    }

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
            documentId = documentId,
            uri = uri,
            location = ArchiveOutputLocation(uri.toString(), actualName),
            isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
        )
    }

    private fun createChildDocument(
        parent: Uri,
        mimeType: String,
        displayName: String,
    ): SafNode = createDocument(
        parent = parent,
        mimeType = mimeType,
        displayName = displayName,
        existingDocumentIds = queryChildren(parent).mapTo(HashSet(), SafNode::documentId),
    )

    private fun queryChildren(parent: Uri): List<SafNode> = safCall("Cannot inspect extraction output") {
        val parentDocumentId = DocumentsContract.getDocumentId(parent)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            if (idColumn < 0 || nameColumn < 0 || mimeColumn < 0) {
                throw IOException("Document provider omitted required child metadata")
            }
            buildList {
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn)
                        ?: throw IOException("Document provider returned a child without an ID")
                    val displayName = cursor.getString(nameColumn)
                        ?: throw IOException("Document provider returned a child without a name")
                    val mimeType = cursor.getString(mimeColumn)
                        ?: throw IOException("Document provider returned a child without a MIME type")
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    add(
                        SafNode(
                            documentId = documentId,
                            uri = uri,
                            location = ArchiveOutputLocation(uri.toString(), displayName),
                            isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
                        ),
                    )
                }
            }
        } ?: throw IOException("Document provider returned no child cursor")
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
        val documentId: String,
        val uri: Uri,
        override val location: ArchiveOutputLocation,
        override val isDirectory: Boolean,
    ) : ArchiveOutputWriter.Node

    private companion object {
        const val BINARY_MIME_TYPE = "application/octet-stream"
        const val MAX_ROOT_NAME_ATTEMPTS = 10_000
        const val WRITE_MODE = "wt"
    }
}
