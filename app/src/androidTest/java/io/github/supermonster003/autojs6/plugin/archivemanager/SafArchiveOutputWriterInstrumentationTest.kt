@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class SafArchiveOutputWriterInstrumentationTest {

    private val resolver: ContentResolver
        get() = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    @Before
    fun resetProvider() {
        requireNotNull(
            resolver.call(
                PROVIDER_URI,
                CollisionDocumentsProvider.METHOD_RESET,
                null,
                null,
            ),
        )
    }

    @Test
    fun foldedExistingRootNameIsAutoNumberedWithoutReplacingIt() {
        val existing = createDocument(
            parent = TREE_ROOT_URI,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            displayName = "Archive",
        )
        val writer = SafArchiveOutputWriter(resolver, TREE_URI)

        val created = writer.createRoot("archive")

        assertEquals("archive (2)", created.location.displayName)
        assertNotEquals(DocumentsContract.getDocumentId(existing), documentId(created.location))
        assertEquals(setOf("Archive", "archive (2)"), childRecords(TREE_ROOT_URI).displayNames())

        writer.deleteRoot(created)
        assertEquals(setOf("Archive"), childRecords(TREE_ROOT_URI).displayNames())
    }

    @Test
    fun unresolvedFoldedEntryConflictsFailClosedAndPreserveExistingDestinationContent() {
        val cases = listOf(
            CollisionCase(
                rootName = "unicode-output",
                entries = listOf(
                    "\u00E9.txt" to "composed".toByteArray(),
                    "e\u0301.txt" to "decomposed".toByteArray(),
                ),
            ),
            CollisionCase(
                rootName = "file-directory-output",
                entries = listOf(
                    "node/child.txt" to "child".toByteArray(),
                    "Node" to "file".toByteArray(),
                ),
            ),
        )

        cases.forEach { case ->
            resetProvider()
            val sentinelPayload = "preserve-${case.rootName}".toByteArray()
            val sentinel = createDocument(
                parent = TREE_ROOT_URI,
                mimeType = BINARY_MIME_TYPE,
                displayName = SENTINEL_NAME,
            )
            resolver.openOutputStream(sentinel, WRITE_MODE).use { output ->
                requireNotNull(output).write(sentinelPayload)
            }
            val source = writeZip(case.entries)

            try {
                val snapshot = ArchiveScanner().scan(source)
                val error = try {
                    runBlocking {
                        ArchiveExtractor(contentResolver = resolver).extract(
                            source = source,
                            snapshot = snapshot,
                            selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                            treeUri = TREE_URI,
                            rootName = case.rootName,
                        )
                    }
                    fail("Expected the folded destination conflict to fail")
                    error("Unreachable")
                } catch (expected: ArchiveExtractionException) {
                    expected
                }

                assertEquals(
                    ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
                    error.code,
                )
                assertEquals(setOf(SENTINEL_NAME), childRecords(TREE_ROOT_URI).displayNames())
                assertArrayEquals(
                    sentinelPayload,
                    resolver.openInputStream(sentinel).use { input ->
                        requireNotNull(input).readBytes()
                    },
                )
            } finally {
                source.delete()
            }
        }
    }

    @Test
    fun fixedPoliciesSkipOverwriteOrRenameFoldedFileConflicts() {
        val cases = listOf(
            PolicyCase(
                policy = ArchiveExtractionConflictPolicy.SKIP,
                expectedNames = setOf("A.txt"),
                expectedPrimaryPayload = "first",
                expectedSkipped = 1,
            ),
            PolicyCase(
                policy = ArchiveExtractionConflictPolicy.OVERWRITE,
                expectedNames = setOf("A.txt"),
                expectedPrimaryPayload = "two",
                expectedOverwritten = 1,
            ),
            PolicyCase(
                policy = ArchiveExtractionConflictPolicy.AUTO_RENAME,
                expectedNames = setOf("A.txt", "a (2).txt"),
                expectedPrimaryPayload = "first",
                expectedAutoRenamed = 1,
            ),
        )

        cases.forEach { case ->
            resetProvider()
            val source = writeZip(
                listOf(
                    "A.txt" to "first".toByteArray(),
                    "a.txt" to "two".toByteArray(),
                ),
            )
            try {
                val snapshot = ArchiveScanner().scan(source)
                val result = runBlocking {
                    ArchiveExtractor(contentResolver = resolver).extract(
                        source = source,
                        snapshot = snapshot,
                        selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                        treeUri = TREE_URI,
                        rootName = "policy-output",
                        conflictPolicy = case.policy,
                    )
                }
                val root = Uri.parse(result.root.identifier)
                val children = childRecords(root)

                assertEquals(case.expectedNames, children.displayNames())
                assertArrayEquals(
                    case.expectedPrimaryPayload.toByteArray(),
                    readDocument(children.single { it.displayName == "A.txt" }),
                )
                if (case.policy == ArchiveExtractionConflictPolicy.AUTO_RENAME) {
                    assertArrayEquals(
                        "two".toByteArray(),
                        readDocument(children.single { it.displayName == "a (2).txt" }),
                    )
                }
                assertEquals(case.expectedSkipped, result.entriesSkipped)
                assertEquals(case.expectedOverwritten, result.entriesOverwritten)
                assertEquals(case.expectedAutoRenamed, result.entriesAutoRenamed)
            } finally {
                source.delete()
            }
        }
    }

    @Test
    fun askAppliesOneDecisionToAllRemainingFoldedConflicts() {
        val source = writeZip(
            listOf(
                "A.txt" to "first".toByteArray(),
                "a.txt" to "second".toByteArray(),
                "A.TXT" to "third".toByteArray(),
                "a.TXT" to "fourth".toByteArray(),
            ),
        )
        try {
            val snapshot = ArchiveScanner().scan(source)
            var resolverCalls = 0
            val result = runBlocking {
                ArchiveExtractor(contentResolver = resolver).extract(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                    treeUri = TREE_URI,
                    rootName = "ask-output",
                    conflictPolicy = ArchiveExtractionConflictPolicy.ASK,
                    conflictResolver = ArchiveExtractionConflictResolver {
                        resolverCalls++
                        ArchiveExtractionConflictResolution(
                            decision = ArchiveExtractionConflictDecision.SKIP,
                            applyToAll = true,
                        )
                    },
                )
            }

            assertEquals(1, resolverCalls)
            assertEquals(3, result.entriesSkipped)
            assertEquals(
                setOf("A.txt"),
                childRecords(Uri.parse(result.root.identifier)).displayNames(),
            )
        } finally {
            source.delete()
        }
    }

    @Test
    fun overwriteAutoRenamesAnIncompatibleFoldedTypeConflict() {
        val source = writeZip(
            listOf(
                "node/child.txt" to "child".toByteArray(),
                "Node" to "file".toByteArray(),
            ),
        )
        try {
            val snapshot = ArchiveScanner().scan(source)
            val result = runBlocking {
                ArchiveExtractor(contentResolver = resolver).extract(
                    source = source,
                    snapshot = snapshot,
                    selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                    treeUri = TREE_URI,
                    rootName = "type-output",
                    conflictPolicy = ArchiveExtractionConflictPolicy.OVERWRITE,
                )
            }

            assertEquals(0, result.entriesOverwritten)
            assertEquals(1, result.entriesAutoRenamed)
            assertEquals(
                setOf("node", "Node (2)"),
                childRecords(Uri.parse(result.root.identifier)).displayNames(),
            )
        } finally {
            source.delete()
        }
    }

    @Test
    fun cancellationRemovesTheRealSafOutputRootWithoutReportingAResidual() {
        val sentinelPayload = "preserve-after-cancel".toByteArray()
        val sentinel = createDocument(
            parent = TREE_ROOT_URI,
            mimeType = BINARY_MIME_TYPE,
            displayName = SENTINEL_NAME,
        )
        resolver.openOutputStream(sentinel, WRITE_MODE).use { output ->
            requireNotNull(output).write(sentinelPayload)
        }
        val source = writeZip(listOf("large.bin" to ByteArray(256 * 1_024) { 7 }))

        try {
            val snapshot = ArchiveScanner().scan(source)
            val cancellation = try {
                runBlocking {
                    ArchiveExtractor(contentResolver = resolver).extract(
                        source = source,
                        snapshot = snapshot,
                        selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                        treeUri = TREE_URI,
                        rootName = "cancelled-output",
                        progress = ArchiveProgressListener { update ->
                            if (
                                update.phase == ExtractionPhase.EXTRACTING &&
                                update.bytesWritten > 0L
                            ) {
                                throw CancellationException("instrumentation cancellation")
                            }
                        },
                    )
                }
                fail("Expected extraction cancellation")
                error("Unreachable")
            } catch (expected: CancellationException) {
                expected
            }

            assertTrue(cancellation.residualArchiveOutputs().isEmpty())
            assertEquals(setOf(SENTINEL_NAME), childRecords(TREE_ROOT_URI).displayNames())
            assertArrayEquals(
                sentinelPayload,
                resolver.openInputStream(sentinel).use { input ->
                    requireNotNull(input).readBytes()
                },
            )
        } finally {
            source.delete()
        }
    }

    @Test
    fun cleanupRefusalReportsTheExactResidualSafRoot() {
        requireNotNull(
            resolver.call(
                PROVIDER_URI,
                CollisionDocumentsProvider.METHOD_REJECT_DELETES,
                null,
                null,
            ),
        )
        val source = writeZip(
            listOf(
                "node/child.txt" to "child".toByteArray(),
                "Node" to "conflict".toByteArray(),
            ),
        )

        try {
            val snapshot = ArchiveScanner().scan(source)
            val error = try {
                runBlocking {
                    ArchiveExtractor(contentResolver = resolver).extract(
                        source = source,
                        snapshot = snapshot,
                        selectedPaths = listOf(ArchivePathPolicy.ROOT_PATH),
                        treeUri = TREE_URI,
                        rootName = "residual-output",
                    )
                }
                fail("Expected destination conflict")
                error("Unreachable")
            } catch (expected: ArchiveExtractionException) {
                expected
            }

            assertEquals(
                ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
                error.code,
            )
            val residual = error.residualArchiveOutputs().single()
            assertEquals("residual-output", residual.displayName)
            assertEquals(
                "node-1",
                DocumentsContract.getDocumentId(Uri.parse(residual.identifier)),
            )
            assertEquals(setOf("residual-output"), childRecords(TREE_ROOT_URI).displayNames())
        } finally {
            source.delete()
            resetProvider()
        }
    }

    private fun writeZip(entries: List<Pair<String, ByteArray>>): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "saf-output-${UUID.randomUUID()}.zip")
        ZipOutputStream(FileOutputStream(source)).use { output ->
            entries.forEach { (name, payload) ->
                output.putNextEntry(ZipEntry(name))
                output.write(payload)
                output.closeEntry()
            }
        }
        return source
    }

    private fun createDocument(parent: Uri, mimeType: String, displayName: String): Uri =
        requireNotNull(
            DocumentsContract.createDocument(resolver, parent, mimeType, displayName),
        )

    private fun childRecords(parent: Uri): List<ChildRecord> {
        val parentId = DocumentsContract.getDocumentId(parent)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(TREE_URI, parentId)
        return resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            )
            val nameColumn = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )
            buildList {
                while (cursor.moveToNext()) {
                    add(ChildRecord(cursor.getString(idColumn), cursor.getString(nameColumn)))
                }
            }
        } ?: error("Test provider returned no child cursor")
    }

    private fun List<ChildRecord>.displayNames(): Set<String> = mapTo(LinkedHashSet()) {
        it.displayName
    }

    private fun documentId(location: ArchiveOutputLocation): String =
        DocumentsContract.getDocumentId(Uri.parse(location.identifier))

    private fun readDocument(record: ChildRecord): ByteArray {
        val uri = DocumentsContract.buildDocumentUriUsingTree(TREE_URI, record.documentId)
        return resolver.openInputStream(uri).use { input -> requireNotNull(input).readBytes() }
    }

    private data class ChildRecord(
        val documentId: String,
        val displayName: String,
    )

    private data class CollisionCase(
        val rootName: String,
        val entries: List<Pair<String, ByteArray>>,
    )

    private data class PolicyCase(
        val policy: ArchiveExtractionConflictPolicy,
        val expectedNames: Set<String>,
        val expectedPrimaryPayload: String,
        val expectedSkipped: Int = 0,
        val expectedOverwritten: Int = 0,
        val expectedAutoRenamed: Int = 0,
    )

    private companion object {
        const val BINARY_MIME_TYPE = "application/octet-stream"
        const val SENTINEL_NAME = "sentinel.txt"
        const val WRITE_MODE = "w"

        val PROVIDER_URI: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(CollisionDocumentsProvider.AUTHORITY)
            .build()
        val TREE_URI: Uri = DocumentsContract.buildTreeDocumentUri(
            CollisionDocumentsProvider.AUTHORITY,
            CollisionDocumentsProvider.ROOT_ID,
        )
        val TREE_ROOT_URI: Uri = DocumentsContract.buildDocumentUriUsingTree(
            TREE_URI,
            CollisionDocumentsProvider.ROOT_ID,
        )
    }
}
