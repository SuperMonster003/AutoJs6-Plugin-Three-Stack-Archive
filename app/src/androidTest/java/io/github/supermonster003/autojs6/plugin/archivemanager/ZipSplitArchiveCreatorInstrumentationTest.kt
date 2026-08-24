@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import net.lingala.zip4j.ZipFile
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.Random
import java.util.UUID
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class ZipSplitArchiveCreatorInstrumentationTest {

    @Test
    fun standardSplitZipPublishesPartsBeforeTerminalAndRoundTrips() {
        withSession(payloadSize = TWO_AND_A_QUARTER_MIB) { session, payload ->
            val result = createSplitArchive(session)
            val names = result.createdOutputs.map(CreatedArchiveOutput::displayName)

            assertTrue(names.size >= 3)
            assertEquals("Documents.zip", names.last())
            assertTrue(names.dropLast(1).all { SPLIT_PART_PATTERN.matches(it) })
            assertEquals(names, session.commitOrder)
            assertEquals(names.last(), session.commitOrder.last())
            names.dropLast(1).forEach { name ->
                assertTrue(session.outputFile(name).length() in 1..SPLIT_SIZE_BYTES)
            }
            assertTrue(session.outputFile(names.last()).length() > 0L)
            assertArrayEquals(payload, readPayload(session.outputFile(names.last())))
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun outputBelowTheSelectedSplitSizePublishesOneValidTerminalZip() {
        withSession(payloadSize = SMALL_PAYLOAD_SIZE) { session, payload ->
            val result = createSplitArchive(session)
            val terminal = session.outputFile(result.outputDisplayName)

            assertEquals(listOf("Documents.zip"),
                result.createdOutputs.map(CreatedArchiveOutput::displayName))
            assertEquals(listOf("Documents.zip"), session.commitOrder)
            assertFalse(ZipFile(terminal).isSplitArchive)
            assertArrayEquals(payload, readPayload(terminal, expectSplit = false))
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun passwordProtectedSplitZipUsesAesAndRoundTrips() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, payload ->
            val password = TEST_PASSWORD.toCharArray()
            val result = createSplitArchive(session, password = password)
            val terminal = session.outputFile(result.outputDisplayName)
            val archive = ZipFile(terminal, TEST_PASSWORD.toCharArray())
            val header = archive.getFileHeader(SOURCE_DISPLAY_NAME)

            assertTrue(archive.isSplitArchive)
            assertTrue(archive.isEncrypted)
            assertTrue(header.isEncrypted)
            archive.getInputStream(header).use { input ->
                assertArrayEquals(payload, input.readBytes())
            }
            assertArrayEquals(TEST_PASSWORD.toCharArray(), password)
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun uppercaseZipSuffixRemainsExactAcrossThePhysicalOutputGroup() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, payload ->
            val result = createSplitArchive(session, outputDisplayName = "Documents.ZIP")
            val names = result.createdOutputs.map(CreatedArchiveOutput::displayName)

            assertEquals("Documents.ZIP", result.outputDisplayName)
            assertEquals("Documents.ZIP", names.last())
            assertTrue(names.dropLast(1).all { it.startsWith("Documents.z") })
            assertEquals(names, session.commitOrder)
            assertArrayEquals(payload, readPayload(session.outputFile(names.last())))
        }
    }

    @Test
    fun partNameConflictAutomaticallyRenumbersTheCompleteVolumeGroup() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, payload ->
            session.unavailableNames += "Documents.z01"

            val result = createSplitArchive(session)
            val names = result.createdOutputs.map(CreatedArchiveOutput::displayName)

            assertEquals("Documents.zip", session.prepareOrder[0])
            assertEquals("Documents.z01", session.prepareOrder[1])
            assertTrue(names.all { it.startsWith("Documents (1).") })
            assertEquals("Documents (1).zip", names.last())
            assertEquals(listOf("Documents.zip"), session.abortOrder)
            assertEquals(1, session.sourceOpenCalls)
            assertArrayEquals(payload, readPayload(session.outputFile(names.last())))
        }
    }

    @Test
    fun askPolicyReportsAConflictingPartAndLeavesNoOutput() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, _ ->
            session.unavailableNames += "Documents.z01"

            val error = assertThrows(ArchiveOutputNameUnavailableException::class.java) {
                createSplitArchive(
                    session = session,
                    conflictPolicy = ArchiveCreationConflictPolicy.ASK,
                )
            }

            assertEquals("Documents.z01", error.requestedDisplayName)
            assertEquals(listOf("Documents.zip"), session.abortOrder)
            assertEquals(1, session.sourceOpenCalls)
            assertTrue(session.committedNames.isEmpty())
            assertTrue(session.outputFiles().isEmpty())
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun cancellationAtCommitBoundaryAbortsEveryPendingVolume() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, _ ->
            var cancel = false

            assertThrows(CancellationException::class.java) {
                createSplitArchive(
                    session = session,
                    checkCancelled = {
                        if (cancel) throw CancellationException("Synthetic commit cancellation")
                    },
                    progress = ArchiveCreationProgressListener { update ->
                        if (update.phase == ArchiveCreationPhase.COMMITTING) cancel = true
                    },
                )
            }

            assertTrue(session.committedNames.isEmpty())
            assertEquals(session.preparedTransactionCount, session.abortOrder.size)
            assertTrue(session.outputFiles().isEmpty())
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun terminalCommitFailureExposesCommittedPartsAsAnIncompleteSet() {
        withSession(payloadSize = TWO_AND_A_QUARTER_MIB) { session, _ ->
            session.commitFailureNames += "Documents.zip"

            val error = assertThrows(ArchiveCreationPartialOutputException::class.java) {
                createSplitArchive(session)
            }

            assertEquals(error.totalOutputs - 1, error.committedOutputs.size)
            assertEquals("Documents.zip", error.failedOutputDisplayName)
            assertTrue(error.committedOutputs.all { SPLIT_PART_PATTERN.matches(it.displayName) })
            assertEquals(
                ArchiveCreationOutputOperation.COMMIT,
                (error.operationFailure as ArchiveCreationOutputException).operation,
            )
            assertEquals("Documents.zip", session.commitAttemptOrder.last())
            assertFalse(session.outputFile("Documents.zip").exists())
            assertEquals(
                error.committedOutputs.map(HostOutputTransaction::displayName).toSet(),
                session.outputFiles().map(File::getName).toSet(),
            )
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun failedPendingCleanupExposesEveryUnconfirmedTransaction() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, _ ->
            session.openFailureNames += "Documents.z01"
            session.abortFailureNames += "Documents.zip"

            val error = assertThrows(ArchiveCreationOutputGroupRollbackException::class.java) {
                createSplitArchive(session)
            }

            assertTrue(error.committedOutputs.isEmpty())
            assertEquals(2, error.totalOutputs)
            assertEquals(listOf("Documents.zip"),
                error.residualOutputs.map(HostOutputTransaction::displayName))
            assertEquals(1, error.rollbackFailures.size)
            assertTrue(error.rollbackFailures.single() is SecurityException)
            assertTrue(error.operationFailure is ArchiveCreationOutputException)
            assertEquals(setOf("Documents.zip", "Documents.z01"), session.abortOrder.toSet())
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun staleFlatStagingWorkspaceIsRemovedBeforeSplitCreation() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, payload ->
            val staleWorkspace = session.stagingDirectory
                .resolve("archive-output-stale-flat")
                .apply {
                    check(mkdir())
                    resolve("staged.z01").writeBytes(byteArrayOf(1, 2, 3))
                    check(setLastModified(System.currentTimeMillis() - EIGHT_DAYS_MILLIS))
                }

            val result = createSplitArchive(session)

            assertFalse(staleWorkspace.exists())
            assertArrayEquals(payload, readPayload(session.outputFile(result.outputDisplayName)))
            assertFalse(session.hasStagingWorkspace())
        }
    }

    @Test
    fun staleWorkspaceCleanupDoesNotDescendIntoUnexpectedDirectories() {
        withSession(payloadSize = ONE_AND_A_QUARTER_MIB) { session, payload ->
            val staleWorkspace = session.stagingDirectory
                .resolve("archive-output-stale-nested")
                .apply { check(mkdir()) }
            val marker = staleWorkspace.resolve("unexpected").apply { check(mkdir()) }
                .resolve("keep.txt")
                .apply { writeText("keep") }
            check(staleWorkspace.setLastModified(System.currentTimeMillis() - EIGHT_DAYS_MILLIS))

            val result = createSplitArchive(session)

            assertTrue(marker.isFile)
            assertEquals("keep", marker.readText())
            assertArrayEquals(payload, readPayload(session.outputFile(result.outputDisplayName)))
        }
    }

    private fun createSplitArchive(
        session: SplitHostSession,
        outputDisplayName: String = "Documents.zip",
        password: CharArray? = null,
        conflictPolicy: ArchiveCreationConflictPolicy = ArchiveCreationConflictPolicy.AUTO_RENAME,
        checkCancelled: () -> Unit = {},
        progress: ArchiveCreationProgressListener = ArchiveCreationProgressListener {},
    ): ArchiveCreationResult = ArchiveEngine.DEFAULT.createWriter(
        ArchiveFormat.ZIP,
        session,
        session.stagingDirectory,
    ).create(
        request = request(session),
        options = ArchiveCreationOptions(
            outputDisplayName = outputDisplayName,
            compressionLevel = 0,
            password = password,
            conflictPolicy = conflictPolicy,
            splitVolumeSizeBytes = SPLIT_SIZE_BYTES,
        ),
        checkCancelled = checkCancelled,
        progress = progress,
    )

    private fun request(session: IExplorerActionHostSession) = ArchiveCompressionRequest(
        requestId = UUID.randomUUID().toString(),
        parentUri = Uri.parse("content://host/root"),
        parentDisplayPath = "/storage/emulated/0/Documents",
        targets = listOf(
            ArchiveCompressionTarget(
                id = SOURCE_ID,
                uri = Uri.parse("content://host/source"),
                displayName = SOURCE_DISPLAY_NAME,
                kind = ExplorerActionValues.TARGET_FILE,
                mimeType = "application/octet-stream",
                size = -1L,
                lastModified = 1_700_000_000_000L,
            ),
        ),
        hostSession = session,
    )

    private fun readPayload(
        terminal: File,
        expectSplit: Boolean = true,
    ): ByteArray {
        val archive = ZipFile(terminal)
        assertEquals(expectSplit, archive.isSplitArchive)
        val header = archive.getFileHeader(SOURCE_DISPLAY_NAME)
        return archive.getInputStream(header).use { it.readBytes() }
    }

    private inline fun withSession(
        payloadSize: Int,
        block: (SplitHostSession, ByteArray) -> Unit,
    ) {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val payload = ByteArray(payloadSize).also { Random(PAYLOAD_SEED).nextBytes(it) }
        val session = SplitHostSession(cacheDirectory, payload)
        try {
            block(session, payload)
        } finally {
            session.cleanup()
        }
    }

    private class SplitHostSession(
        cacheDirectory: File,
        payload: ByteArray,
    ) : IExplorerActionHostSession.Stub() {

        val stagingDirectory = cacheDirectory.resolve("split-staging-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        private val outputDirectory = cacheDirectory.resolve("split-output-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
        private val sourceFile = cacheDirectory.resolve("split-source-${UUID.randomUUID()}").apply {
            writeBytes(payload)
        }
        private val transactions = linkedMapOf<String, Transaction>()
        val unavailableNames = mutableSetOf<String>()
        val openFailureNames = mutableSetOf<String>()
        val commitFailureNames = mutableSetOf<String>()
        val abortFailureNames = mutableSetOf<String>()
        val prepareOrder = mutableListOf<String>()
        val commitAttemptOrder = mutableListOf<String>()
        val commitOrder = mutableListOf<String>()
        val abortOrder = mutableListOf<String>()
        val committedNames = mutableSetOf<String>()
        var sourceOpenCalls = 0
            private set
        var preparedTransactionCount = 0
            private set

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle = error("The split test source is not a directory")

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            check(targetId == SOURCE_ID && relativePath.isEmpty())
            sourceOpenCalls++
            return ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle {
            prepareOrder += displayName
            assertEquals(ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL, conflictPolicy)
            assertEquals(
                if (displayName.endsWith(".zip", ignoreCase = true)) {
                    "application/zip"
                } else {
                    "application/octet-stream"
                },
                mimeType,
            )
            if (
                displayName in unavailableNames ||
                transactions.values.any { it.displayName == displayName } ||
                outputFile(displayName).exists()
            ) {
                throw IllegalArgumentException("Synthetic exact-name conflict: $displayName")
            }
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                displayName = displayName,
            )
            transactions[transaction.id] = transaction
            preparedTransactionCount++
            return transaction.bundle()
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            val transaction = requireNotNull(transactions[transactionId])
            if (transaction.displayName in openFailureNames) {
                throw IOException("Synthetic output open failure: ${transaction.displayName}")
            }
            return ParcelFileDescriptor.open(
                outputFile(transaction.displayName),
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun commitOutput(transactionId: String): Bundle {
            val transaction = requireNotNull(transactions[transactionId])
            commitAttemptOrder += transaction.displayName
            if (transaction.displayName in commitFailureNames) {
                throw IOException("Synthetic output commit failure: ${transaction.displayName}")
            }
            check(outputFile(transaction.displayName).isFile)
            transaction.committed = true
            committedNames += transaction.displayName
            commitOrder += transaction.displayName
            return transaction.bundle()
        }

        override fun abortOutput(transactionId: String) {
            val transaction = requireNotNull(transactions[transactionId])
            abortOrder += transaction.displayName
            if (transaction.displayName in abortFailureNames) {
                throw SecurityException("Synthetic output cleanup denial: ${transaction.displayName}")
            }
            check(!transaction.committed)
            outputFile(transaction.displayName).delete()
            transactions.remove(transactionId)
        }

        override fun close() = Unit

        fun outputFile(displayName: String): File = outputDirectory.resolve(displayName)

        fun outputFiles(): List<File> = outputDirectory.listFiles()?.filter(File::isFile).orEmpty()

        fun hasStagingWorkspace(): Boolean = stagingDirectory.listFiles().orEmpty().any { child ->
            child.name.startsWith("archive-output-")
        }

        fun cleanup() {
            sourceFile.delete()
            stagingDirectory.deleteRecursively()
            outputDirectory.deleteRecursively()
        }

        private inner class Transaction(
            val id: String,
            val displayName: String,
            var committed: Boolean = false,
        ) {
            fun bundle() = Bundle().apply {
                putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, id)
                putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, displayName)
                putString(
                    ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH,
                    "/storage/emulated/0/Documents/$displayName",
                )
            }
        }
    }

    private companion object {
        const val SOURCE_ID = "source"
        const val SOURCE_DISPLAY_NAME = "payload.bin"
        const val PAYLOAD_SEED = 0x5A17C0DEL
        const val ONE_AND_A_QUARTER_MIB = 1_310_720
        const val TWO_AND_A_QUARTER_MIB = 2_359_296
        const val SMALL_PAYLOAD_SIZE = 32 * 1_024
        const val SPLIT_SIZE_BYTES = 1_048_576L
        const val TEST_PASSWORD = "ArchiveManager-Split-Test-2026"
        const val EIGHT_DAYS_MILLIS = 8L * 24L * 60L * 60L * 1_000L
        val SPLIT_PART_PATTERN = Regex("^Documents(?: \\(1\\))?\\.z[0-9]{2,}$")
    }
}
