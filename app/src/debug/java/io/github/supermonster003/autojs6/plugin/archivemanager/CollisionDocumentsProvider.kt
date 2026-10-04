package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException
import java.text.Normalizer
import java.util.Locale

/** Test-only provider that folds case and Unicode spelling and returns an existing ID on conflict. */
class CollisionDocumentsProvider : DocumentsProvider() {

    private val nodes = LinkedHashMap<String, Node>()
    private lateinit var storageRoot: File
    private var nextId = 1
    private var rejectDeletes = false

    override fun onCreate(): Boolean {
        storageRoot = File(requireNotNull(context).cacheDir, STORAGE_DIRECTORY)
        reset()
        return true
    }

    @Synchronized
    @SuppressLint("UsableSpace")
    override fun queryRoots(projection: Array<out String>?): Cursor {
        val columns = projection ?: DEFAULT_ROOT_PROJECTION
        return MatrixCursor(columns).apply {
            val row = newRow()
            columns.forEach { column ->
                row.add(
                    when (column) {
                        DocumentsContract.Root.COLUMN_ROOT_ID -> ROOT_ID
                        DocumentsContract.Root.COLUMN_DOCUMENT_ID -> ROOT_ID
                        DocumentsContract.Root.COLUMN_TITLE -> "Collision test root"
                        DocumentsContract.Root.COLUMN_FLAGS ->
                            DocumentsContract.Root.FLAG_LOCAL_ONLY or
                                DocumentsContract.Root.FLAG_SUPPORTS_CREATE
                        DocumentsContract.Root.COLUMN_MIME_TYPES -> "*/*"
                        DocumentsContract.Root.COLUMN_AVAILABLE_BYTES -> storageRoot.usableSpace
                        else -> null
                    },
                )
            }
        }
    }

    @Synchronized
    override fun queryDocument(
        documentId: String,
        projection: Array<out String>?,
    ): Cursor {
        val node = requireNode(documentId)
        val columns = projection ?: DEFAULT_DOCUMENT_PROJECTION
        return MatrixCursor(columns).apply { includeNode(columns, node) }
    }

    @Synchronized
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        requireDirectory(parentDocumentId)
        val columns = projection ?: DEFAULT_DOCUMENT_PROJECTION
        return MatrixCursor(columns).apply {
            nodes.values
                .filter { it.parentId == parentDocumentId }
                .sortedBy(Node::displayName)
                .forEach { includeNode(columns, it) }
        }
    }

    @Synchronized
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String,
    ): String {
        val parent = requireDirectory(parentDocumentId)
        nodes.values.firstOrNull {
            it.parentId == parentDocumentId && collisionKey(it.displayName) == collisionKey(displayName)
        }?.let { existing ->
            return existing.id
        }

        val id = "node-${nextId++}"
        val directory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val backing = File(parent.backingFile, id)
        val created = if (directory) backing.mkdirs() else backing.createNewFile()
        if (!created) throw FileNotFoundException("Cannot create test document")
        nodes[id] = Node(
            id = id,
            parentId = parentDocumentId,
            displayName = displayName,
            mimeType = mimeType,
            directory = directory,
            backingFile = backing,
        )
        return id
    }

    @Synchronized
    override fun deleteDocument(documentId: String) {
        if (documentId == ROOT_ID) throw FileNotFoundException("Cannot delete the test root")
        if (rejectDeletes) throw FileNotFoundException("Synthetic deletion refusal")
        val node = requireNode(documentId)
        descendantsOf(documentId).asReversed().forEach { descendant ->
            nodes.remove(descendant.id)
            descendant.backingFile.deleteRecursively()
        }
        nodes.remove(documentId)
        if (!node.backingFile.deleteRecursively()) {
            throw FileNotFoundException("Cannot delete test document")
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        val node = synchronized(this) { requireNode(documentId) }
        if (node.directory) throw FileNotFoundException("Cannot open a directory")
        return ParcelFileDescriptor.open(node.backingFile, ParcelFileDescriptor.parseMode(mode))
    }

    @Synchronized
    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        var current = nodes[documentId] ?: return false
        while (current.parentId != null) {
            if (current.parentId == parentDocumentId) return true
            current = nodes[current.parentId] ?: return false
        }
        return false
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val platformResult = super.call(method, arg, extras)
        if (platformResult != null) return platformResult
        return when (method) {
            METHOD_RESET -> {
                synchronized(this) { reset() }
                Bundle.EMPTY
            }
            METHOD_REJECT_DELETES -> {
                synchronized(this) { rejectDeletes = true }
                Bundle.EMPTY
            }
            else -> null
        }
    }

    private fun reset() {
        storageRoot.deleteRecursively()
        check(storageRoot.mkdirs())
        nodes.clear()
        nodes[ROOT_ID] = Node(
            id = ROOT_ID,
            parentId = null,
            displayName = ROOT_ID,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            directory = true,
            backingFile = storageRoot,
        )
        nextId = 1
        rejectDeletes = false
    }

    private fun requireNode(documentId: String): Node =
        nodes[documentId] ?: throw FileNotFoundException("Unknown test document")

    private fun requireDirectory(documentId: String): Node = requireNode(documentId).also { node ->
        if (!node.directory) throw FileNotFoundException("Test document is not a directory")
    }

    private fun descendantsOf(documentId: String): List<Node> = buildList {
        nodes.values.filter { it.parentId == documentId }.forEach { child ->
            add(child)
            addAll(descendantsOf(child.id))
        }
    }

    private fun MatrixCursor.includeNode(columns: Array<out String>, node: Node) {
        val row = newRow()
        columns.forEach { column ->
            row.add(
                when (column) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID -> node.id
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME -> node.displayName
                    DocumentsContract.Document.COLUMN_MIME_TYPE -> node.mimeType
                    DocumentsContract.Document.COLUMN_FLAGS -> if (node.directory) {
                        DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE or
                            DocumentsContract.Document.FLAG_SUPPORTS_DELETE
                    } else {
                        DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                            DocumentsContract.Document.FLAG_SUPPORTS_DELETE
                    }
                    DocumentsContract.Document.COLUMN_SIZE ->
                        node.backingFile.takeUnless { node.directory }?.length()
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED -> node.backingFile.lastModified()
                    else -> null
                },
            )
        }
    }

    private fun collisionKey(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC).lowercase(Locale.ROOT)

    private data class Node(
        val id: String,
        val parentId: String?,
        val displayName: String,
        val mimeType: String,
        val directory: Boolean,
        val backingFile: File,
    )

    companion object {
        const val AUTHORITY = "io.github.supermonster003.autojs6.plugin.three.stack.archive.test.documents"
        const val METHOD_REJECT_DELETES = "reject-deletes"
        const val METHOD_RESET = "reset"
        const val ROOT_ID = "root"

        private const val STORAGE_DIRECTORY = "collision-documents-provider"
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
