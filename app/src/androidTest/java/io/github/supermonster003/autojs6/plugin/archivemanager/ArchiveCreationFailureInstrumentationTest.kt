@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class ArchiveCreationFailureInstrumentationTest {

    @Test
    fun everyWriterTypesReservationFailureWithoutAbortingANonexistentTransaction() {
        forEveryFormat(FailureMode.PREPARE) { format, session, error ->
            val outputError = error.requireOutputFailure(format, ArchiveCreationOutputOperation.PREPARE)
            assertTrue(format.displayName, outputError.cause is IllegalStateException)
            assertEquals(format.displayName, 1, session.prepareCalls)
            assertEquals(format.displayName, 0, session.outputOpenCalls)
            assertEquals(format.displayName, 0, session.sourceOpenCalls)
            assertEquals(format.displayName, 0, session.commitCalls)
            assertEquals(format.displayName, 0, session.abortCalls)
            assertFalse(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterTypesOutputOpenFailureAndAbortsThePreparedTransaction() {
        forEveryFormat(FailureMode.OPEN) { format, session, error ->
            error.requireOutputFailure(format, ArchiveCreationOutputOperation.OPEN)
            assertEquals(format.displayName, 1, session.outputOpenCalls)
            assertEquals(format.displayName, 0, session.sourceOpenCalls)
            assertEquals(format.displayName, 0, session.commitCalls)
            assertEquals(format.displayName, 1, session.abortCalls)
            assertFalse(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterTypesOutputWriteFailureAndAbortsThePreparedTransaction() {
        forEveryFormat(FailureMode.WRITE) { format, session, error ->
            error.requireOutputFailure(format, ArchiveCreationOutputOperation.WRITE)
            assertEquals(format.displayName, 1, session.outputOpenCalls)
            assertEquals(format.displayName, 0, session.commitCalls)
            assertEquals(format.displayName, 1, session.abortCalls)
            assertFalse(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterTypesCommitFailureAndRemovesItsUncommittedOutput() {
        forEveryFormat(FailureMode.COMMIT) { format, session, error ->
            error.requireOutputFailure(format, ArchiveCreationOutputOperation.COMMIT)
            assertTrue(format.displayName, session.sourceOpenCalls > 0)
            assertEquals(format.displayName, 1, session.commitCalls)
            assertEquals(format.displayName, 1, session.abortCalls)
            assertFalse(format.displayName, session.committed)
            assertFalse(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterPreservesOperationAndRollbackFailuresWhenCleanupCannotBeConfirmed() {
        forEveryFormat(FailureMode.ROLLBACK) { format, session, error ->
            val rollback = error as ArchiveCreationRollbackException
            assertEquals(format.displayName, ArchiveFailureCode.OUTPUT_FAILURE, rollback.code)
            assertEquals(format.displayName, ArchiveFailureStage.CLEANUP, rollback.stage)
            assertEquals(format.displayName, format, rollback.format)
            assertEquals(format.displayName, session.outputDisplayName, rollback.pendingOutputDisplayName)
            assertEquals(format.displayName, session.outputDisplayPath, rollback.pendingOutputDisplayPath)
            rollback.operationFailure.requireOutputFailure(
                format,
                ArchiveCreationOutputOperation.OPEN,
            )
            assertTrue(format.displayName, rollback.rollbackFailure is SecurityException)
            assertSame(format.displayName, rollback.operationFailure, rollback.cause)
            assertTrue(
                format.displayName,
                rollback.suppressed.any { it === rollback.rollbackFailure },
            )
            assertEquals(format.displayName, 1, session.abortCalls)
            assertTrue(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterTypesSourceOpenFailureAndRollsBackOutput() {
        forEveryFormat(FailureMode.SOURCE_OPEN) { format, session, error ->
            val sourceError = error as ArchiveCreationSourceException
            assertEquals(format.displayName, ArchiveFailureCode.SOURCE_UNREADABLE, sourceError.code)
            assertEquals(format.displayName, ArchiveFailureStage.INPUT, sourceError.stage)
            assertEquals(format.displayName, "source.txt", sourceError.sourceArchivePath)
            assertTrue(format.displayName, sourceError.cause is IOException)
            assertEquals(format.displayName, 1, session.sourceOpenCalls)
            assertEquals(format.displayName, 0, session.commitCalls)
            assertEquals(format.displayName, 1, session.abortCalls)
            assertFalse(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterRejectsUnreadableDirectoryEntriesAndRollsBackOutput() {
        forEveryFormat(FailureMode.UNREADABLE_CHILD) { format, session, error ->
            val sourceError = error as ArchiveCreationSourceException
            assertEquals(format.displayName, ArchiveFailureCode.SOURCE_UNREADABLE, sourceError.code)
            assertEquals(format.displayName, ArchiveFailureStage.INPUT, sourceError.stage)
            assertEquals(format.displayName, "folder/blocked.txt", sourceError.sourceArchivePath)
            assertEquals(format.displayName, null, sourceError.cause)
            assertEquals(format.displayName, 0, session.sourceOpenCalls)
            assertTrue(format.displayName, session.listCalls > 0)
            assertEquals(format.displayName, 0, session.outputOpenCalls)
            assertEquals(format.displayName, 0, session.commitCalls)
            assertEquals(format.displayName, 1, session.abortCalls)
            assertFalse(format.displayName, session.outputFile.exists())
        }
    }

    @Test
    fun everyWriterCancelsScanningBeforeOpeningOutputAndRollsBackTheReservation() {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        ArchiveEngine.DEFAULT.creatableFormats.forEach { format ->
            val session = FailureHostSession(cacheDirectory, format, FailureMode.CANCEL_SCAN)
            try {
                val writer = ArchiveEngine.DEFAULT.createWriter(format, session)
                org.junit.Assert.assertThrows(CancellationException::class.java) {
                    writer.create(
                        request = request(session, FailureMode.CANCEL_SCAN),
                        options = ArchiveCreationOptions(
                            outputDisplayName = session.outputDisplayName,
                            compressionLevel = writer.formatCapabilities.compressionLevels.first(),
                        ),
                        checkCancelled = {
                            if (session.listCalls > 0) {
                                throw CancellationException("cancel source scan")
                            }
                        },
                        progress = ArchiveCreationProgressListener {},
                    )
                }
                assertEquals(format.displayName, 1, session.prepareCalls)
                assertEquals(format.displayName, 1, session.listCalls)
                assertEquals(format.displayName, 0, session.outputOpenCalls)
                assertEquals(format.displayName, 0, session.sourceOpenCalls)
                assertEquals(format.displayName, 0, session.commitCalls)
                assertEquals(format.displayName, 1, session.abortCalls)
                assertFalse(format.displayName, session.outputFile.exists())
            } finally {
                session.cleanup()
            }
        }
    }

    @Test
    fun everyWriterUsesOneScannedManifestAndReportsMonotonicCreationPhases() {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        ArchiveEngine.DEFAULT.creatableFormats.forEach { format ->
            val session = FailureHostSession(cacheDirectory, format, FailureMode.SUCCESS)
            val updates = mutableListOf<ArchiveCreationProgress>()
            try {
                val writer = ArchiveEngine.DEFAULT.createWriter(format, session)
                val result = writer.create(
                    request = request(session, FailureMode.SUCCESS),
                    options = ArchiveCreationOptions(
                        outputDisplayName = session.outputDisplayName,
                        compressionLevel = writer.formatCapabilities.compressionLevels.first(),
                    ),
                    checkCancelled = {},
                    progress = ArchiveCreationProgressListener(updates::add),
                )

                assertEquals(format.displayName, 1L, result.filesCompressed)
                assertEquals(format.displayName, 1L, result.directoriesAdded)
                assertEquals(format.displayName, SOURCE_BYTES.size.toLong(), result.sourceBytesRead)
                assertEquals(format.displayName, 1, session.listCalls)
                assertTrue(
                    format.displayName,
                    session.operationLog.indexOf("list") < session.operationLog.indexOf("open-output"),
                )
                assertTrue(
                    format.displayName,
                    session.operationLog.indexOf("open-output") < session.operationLog.indexOf("open-source"),
                )
                assertEquals(
                    format.displayName,
                    listOf(
                        ArchiveCreationPhase.SCANNING,
                        ArchiveCreationPhase.COMPRESSING,
                        ArchiveCreationPhase.COMMITTING,
                    ),
                    updates.map(ArchiveCreationProgress::phase).distinct(),
                )
                updates.filter { it.phase != ArchiveCreationPhase.SCANNING }.forEach { update ->
                    assertEquals(format.displayName, 1L, update.totalFiles)
                    assertEquals(format.displayName, 1L, update.totalDirectories)
                    assertEquals(format.displayName, SOURCE_BYTES.size.toLong(), update.knownSourceBytes)
                    assertEquals(format.displayName, 0L, update.unknownSizeFiles)
                }
                val committing = updates.last()
                assertEquals(format.displayName, ArchiveCreationPhase.COMMITTING, committing.phase)
                assertEquals(format.displayName, 1L, committing.completedFiles)
                assertEquals(format.displayName, 1L, committing.completedDirectories)
                assertEquals(format.displayName, SOURCE_BYTES.size.toLong(), committing.sourceBytesRead)
            } finally {
                session.cleanup()
            }
        }
    }

    private fun forEveryFormat(
        mode: FailureMode,
        verify: (ArchiveFormat, FailureHostSession, Throwable) -> Unit,
    ) {
        val cacheDirectory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        ArchiveEngine.DEFAULT.creatableFormats.forEach { format ->
            val session = FailureHostSession(cacheDirectory, format, mode)
            try {
                val writer = ArchiveEngine.DEFAULT.createWriter(format, session)
                val error = org.junit.Assert.assertThrows(expectedFailureClass(mode)) {
                    writer.create(
                        request = request(session, mode),
                        options = ArchiveCreationOptions(
                            outputDisplayName = session.outputDisplayName,
                            compressionLevel = writer.formatCapabilities.compressionLevels.first(),
                        ),
                        checkCancelled = {},
                        progress = ArchiveCreationProgressListener {},
                    )
                }
                verify(format, session, error)
            } finally {
                session.cleanup()
            }
        }
    }

    private fun expectedFailureClass(mode: FailureMode): Class<out Throwable> = when (mode) {
        FailureMode.PREPARE,
        FailureMode.OPEN,
        FailureMode.WRITE,
        FailureMode.COMMIT,
        -> ArchiveCreationOutputException::class.java
        FailureMode.ROLLBACK -> ArchiveCreationRollbackException::class.java
        FailureMode.SOURCE_OPEN,
        FailureMode.UNREADABLE_CHILD,
        -> ArchiveCreationSourceException::class.java
        FailureMode.CANCEL_SCAN,
        FailureMode.SUCCESS,
        -> error("Success and cancellation modes use dedicated test paths")
    }

    private fun Throwable.requireOutputFailure(
        format: ArchiveFormat,
        operation: ArchiveCreationOutputOperation,
    ): ArchiveCreationOutputException {
        val outputError = this as ArchiveCreationOutputException
        assertEquals(format.displayName, ArchiveFailureCode.OUTPUT_FAILURE, outputError.code)
        assertEquals(format.displayName, ArchiveFailureStage.OUTPUT, outputError.stage)
        assertEquals(format.displayName, format, outputError.format)
        assertEquals(format.displayName, operation, outputError.operation)
        return outputError
    }

    private fun request(
        session: IExplorerActionHostSession,
        mode: FailureMode,
    ): ArchiveCompressionRequest {
        val directory = mode == FailureMode.UNREADABLE_CHILD ||
            mode == FailureMode.CANCEL_SCAN ||
            mode == FailureMode.SUCCESS
        return ArchiveCompressionRequest(
            requestId = UUID.randomUUID().toString(),
            parentUri = Uri.parse("content://host/root"),
            parentDisplayPath = "/storage/emulated/0/Documents",
            targets = listOf(
                ArchiveCompressionTarget(
                    id = if (directory) "folder" else "source",
                    uri = Uri.parse(if (directory) "content://host/folder" else "content://host/source"),
                    displayName = if (directory) "folder" else "source.txt",
                    kind = if (directory) {
                        ExplorerActionValues.TARGET_DIRECTORY
                    } else {
                        ExplorerActionValues.TARGET_FILE
                    },
                    mimeType = if (directory) "inode/directory" else "text/plain",
                    size = if (directory) -1L else SOURCE_BYTES.size.toLong(),
                    lastModified = 1_700_000_000_000L,
                ),
            ),
            hostSession = session,
        )
    }

    private enum class FailureMode {
        PREPARE,
        OPEN,
        WRITE,
        COMMIT,
        ROLLBACK,
        SOURCE_OPEN,
        UNREADABLE_CHILD,
        CANCEL_SCAN,
        SUCCESS,
    }

    private class FailureHostSession(
        cacheDirectory: File,
        private val format: ArchiveFormat,
        private val mode: FailureMode,
    ) : IExplorerActionHostSession.Stub() {

        private val transactionId = UUID.randomUUID().toString()
        private val sourceFile = cacheDirectory.resolve("creation-source-${UUID.randomUUID()}.txt").apply {
            writeBytes(SOURCE_BYTES)
        }
        val outputDisplayName = "creation-failure.${format.primaryExtension}"
        val outputDisplayPath = "/storage/emulated/0/Documents/$outputDisplayName"
        val outputFile = cacheDirectory.resolve("creation-output-${UUID.randomUUID()}")
        var prepareCalls = 0
            private set
        var outputOpenCalls = 0
            private set
        var sourceOpenCalls = 0
            private set
        var listCalls = 0
            private set
        var commitCalls = 0
            private set
        var abortCalls = 0
            private set
        var committed = false
            private set
        val operationLog = mutableListOf<String>()

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle {
            listCalls++
            operationLog += "list"
            check(
                mode == FailureMode.UNREADABLE_CHILD ||
                    mode == FailureMode.CANCEL_SCAN ||
                    mode == FailureMode.SUCCESS,
            )
            check(targetId == "folder" && relativePath.isEmpty())
            val allItems = listOf(
                Bundle().apply {
                    putString(ExplorerActionHostSessionKeys.RELATIVE_PATH, "blocked.txt")
                    putString(ExplorerActionHostSessionKeys.DISPLAY_NAME, "blocked.txt")
                    putInt(ExplorerActionHostSessionKeys.KIND, ExplorerActionValues.TARGET_FILE)
                    putString(ExplorerActionHostSessionKeys.MIME_TYPE, "text/plain")
                    putLong(ExplorerActionHostSessionKeys.SIZE, SOURCE_BYTES.size.toLong())
                    putLong(ExplorerActionHostSessionKeys.LAST_MODIFIED, 1_700_000_000_000L)
                    putBoolean(
                        ExplorerActionHostSessionKeys.READABLE,
                        mode != FailureMode.UNREADABLE_CHILD,
                    )
                    putBoolean(ExplorerActionHostSessionKeys.SYMBOLIC_LINK, false)
                },
            )
            val page = allItems.drop(offset).take(limit)
            return Bundle().apply {
                putParcelableArrayList(ExplorerActionHostSessionKeys.ITEMS, ArrayList(page))
                putInt(ExplorerActionHostSessionKeys.NEXT_OFFSET, offset + page.size)
                putBoolean(ExplorerActionHostSessionKeys.COMPLETE, offset + page.size >= allItems.size)
            }
        }

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            sourceOpenCalls++
            operationLog += "open-source"
            if (mode == FailureMode.SUCCESS) {
                check(targetId == "folder" && relativePath == "blocked.txt")
            } else {
                check(targetId == "source" && relativePath.isEmpty())
            }
            if (mode == FailureMode.SOURCE_OPEN) {
                throw IOException("simulated source permission failure")
            }
            return ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle {
            prepareCalls++
            operationLog += "prepare"
            assertEquals(outputDisplayName, displayName)
            assertEquals(format.primaryMimeType, mimeType)
            assertEquals(ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME, conflictPolicy)
            if (mode == FailureMode.PREPARE) {
                throw IllegalStateException("simulated output reservation failure")
            }
            assertTrue(outputFile.createNewFile())
            return outputBundle()
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            outputOpenCalls++
            operationLog += "open-output"
            assertEquals(this.transactionId, transactionId)
            if (mode == FailureMode.OPEN || mode == FailureMode.ROLLBACK) {
                throw IOException("simulated output open failure")
            }
            val openMode = if (mode == FailureMode.WRITE) {
                ParcelFileDescriptor.MODE_READ_ONLY
            } else {
                ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY
            }
            return ParcelFileDescriptor.open(outputFile, openMode)
        }

        override fun commitOutput(transactionId: String): Bundle {
            commitCalls++
            operationLog += "commit"
            assertEquals(this.transactionId, transactionId)
            if (mode == FailureMode.COMMIT) {
                throw IOException("simulated output commit failure")
            }
            committed = true
            return outputBundle()
        }

        override fun abortOutput(transactionId: String) {
            abortCalls++
            operationLog += "abort"
            assertEquals(this.transactionId, transactionId)
            if (mode == FailureMode.ROLLBACK) {
                throw SecurityException("simulated output cleanup denial")
            }
            outputFile.delete()
        }

        override fun close() = Unit

        fun cleanup() {
            sourceFile.delete()
            outputFile.delete()
        }

        private fun outputBundle() = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, outputDisplayName)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, outputDisplayPath)
        }
    }

    private companion object {
        val SOURCE_BYTES = "alpha".toByteArray()
    }
}
