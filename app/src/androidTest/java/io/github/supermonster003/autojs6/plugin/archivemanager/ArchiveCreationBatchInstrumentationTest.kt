package io.github.supermonster003.autojs6.plugin.archivemanager

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import net.lingala.zip4j.ZipFile
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ArchiveCreationBatchInstrumentationTest {

    @Test
    fun combinedPlanKeepsOneJobAllTargetsAndRequestedConflictPolicy() {
        val request = request("report.txt", "photos", "notes.md")
        val options = ArchiveCreationOptions(
            outputDisplayName = "Documents.zip",
            compressionLevel = 6,
            conflictPolicy = ArchiveCreationConflictPolicy.ASK,
        )

        val plan = ArchiveCreationPlanner.plan(
            request = request,
            format = ArchiveFormat.ZIP,
            options = options,
            separateArchives = false,
            fallbackStem = "Archive",
        )

        assertEquals(false, plan.separateArchives)
        assertEquals(1, plan.jobs.size)
        assertEquals(request, plan.jobs.single().request)
        assertSame(options, plan.jobs.single().options)
        assertEquals(null, plan.jobs.single().sourceDisplayName)
    }

    @Test
    fun separatePlanCreatesOneAutoNumberedJobPerTargetInSelectionOrder() {
        val request = request("report.txt", "photos", "notes.md")
        val password = "transient-password".toCharArray()
        val plan = ArchiveCreationPlanner.plan(
            request = request,
            format = ArchiveFormat.SEVEN_Z,
            options = ArchiveCreationOptions(
                outputDisplayName = "Documents.7z",
                compressionLevel = 6,
                password = password,
                conflictPolicy = ArchiveCreationConflictPolicy.ASK,
            ),
            separateArchives = true,
            fallbackStem = "Archive",
        )

        assertEquals(true, plan.separateArchives)
        assertEquals(listOf("report.txt", "photos", "notes.md"), plan.jobs.map { it.sourceDisplayName })
        assertEquals(
            listOf("report.txt.7z", "photos.7z", "notes.md.7z"),
            plan.jobs.map { it.options.outputDisplayName },
        )
        assertEquals(
            listOf("target-1", "target-2", "target-3"),
            plan.jobs.map { it.request.targets.single().id },
        )
        assertEquals(
            setOf(ArchiveCreationConflictPolicy.AUTO_RENAME),
            plan.jobs.map { it.options.conflictPolicy }.toSet(),
        )
        assertEquals(true, plan.jobs.all { it.options.password === password })
        password.fill('\u0000')
    }

    @Test
    fun executorCommitsEveryJobAndAggregatesProgressAndCounters() {
        val plan = separatePlan("alpha.txt", "beta.txt", "gamma.txt")
        val writtenNames = mutableListOf<String>()
        val progress = mutableListOf<ArchiveCreationBatchProgress>()
        val writer = FakeWriter { request, options, listener ->
            writtenNames += options.outputDisplayName
            listener.onProgress(scanningProgress(request.targets.single().displayName))
            val index = writtenNames.size.toLong()
            ArchiveCreationResult(
                outputDisplayName = options.outputDisplayName,
                outputDisplayPath = "/Documents/${options.outputDisplayName}",
                filesCompressed = index,
                directoriesAdded = 1L,
                sourceBytesRead = index * 10L,
            )
        }

        val result = ArchiveCreationBatchExecutor.execute(
            writer = writer,
            plan = plan,
            checkCancelled = { },
            progress = ArchiveCreationBatchProgressListener(progress::add),
        )

        assertEquals(listOf("alpha.txt.zip", "beta.txt.zip", "gamma.txt.zip"), writtenNames)
        assertEquals(3, result.outputs.size)
        assertEquals(6L, result.filesCompressed)
        assertEquals(3L, result.directoriesAdded)
        assertEquals(60L, result.sourceBytesRead)
        assertEquals(listOf(1, 2, 3), progress.map { it.archiveIndex })
        assertEquals(setOf(3), progress.map { it.totalArchives }.toSet())
        assertEquals(writtenNames, progress.map { it.requestedOutputDisplayName })
    }

    @Test
    fun laterFailureCarriesCommittedOutputsAndStopsBeforeRemainingJobs() {
        val plan = separatePlan("alpha.txt", "beta.txt", "gamma.txt")
        val failure = IOException("synthetic second-output failure")
        var calls = 0
        val writer = FakeWriter { _, options, _ ->
            calls++
            if (calls == 2) throw failure
            ArchiveCreationResult(
                outputDisplayName = options.outputDisplayName,
                outputDisplayPath = "/Documents/${options.outputDisplayName}",
                filesCompressed = 1L,
                directoriesAdded = 0L,
                sourceBytesRead = 4L,
            )
        }

        val error = assertThrows(ArchiveCreationPartialFailureException::class.java) {
            ArchiveCreationBatchExecutor.execute(writer, plan, { }, ArchiveCreationBatchProgressListener { })
        }

        assertEquals(2, calls)
        assertEquals(1, error.completedOutputs.size)
        assertEquals("alpha.txt.zip", error.completedOutputs.single().outputDisplayName)
        assertEquals(3, error.totalOutputs)
        assertEquals(2, error.failedOutputIndex)
        assertEquals("beta.txt.zip", error.failedRequestedOutputDisplayName)
        assertEquals("beta.txt", error.failedSourceDisplayName)
        assertSame(failure, error.operationFailure)
        assertSame(failure, error.cause)
    }

    @Test
    fun cancellationBetweenCommittedOutputsBecomesExplicitPartialResult() {
        val plan = separatePlan("alpha.txt", "beta.txt")
        var cancellationChecks = 0
        var writes = 0
        val writer = FakeWriter { _, options, _ ->
            writes++
            ArchiveCreationResult(
                outputDisplayName = options.outputDisplayName,
                outputDisplayPath = "/Documents/${options.outputDisplayName}",
                filesCompressed = 1L,
                directoriesAdded = 0L,
                sourceBytesRead = 1L,
            )
        }

        val error = assertThrows(ArchiveCreationPartialFailureException::class.java) {
            ArchiveCreationBatchExecutor.execute(
                writer = writer,
                plan = plan,
                checkCancelled = {
                    cancellationChecks++
                    if (cancellationChecks == 2) throw CancellationException("synthetic cancellation")
                },
                progress = ArchiveCreationBatchProgressListener { },
            )
        }

        assertEquals(1, writes)
        assertEquals(1, error.completedOutputs.size)
        assertEquals(2, error.failedOutputIndex)
        assertEquals(true, error.operationFailure is CancellationException)
    }

    @Test
    fun firstFailureRemainsDirectlyRetryableAndIsNotReportedAsPartial() {
        val plan = separatePlan("alpha.txt", "beta.txt")
        val failure = IOException("synthetic first-output failure")
        val writer = FakeWriter { _, _, _ -> throw failure }

        val error = assertThrows(IOException::class.java) {
            ArchiveCreationBatchExecutor.execute(writer, plan, { }, ArchiveCreationBatchProgressListener { })
        }

        assertSame(failure, error)
    }

    @Test
    fun aggregateCountersSaturateWithoutTurningCommittedOutputsIntoARetryableFailure() {
        val result = ArchiveCreationBatchResult(
            outputs = listOf(
                ArchiveCreationResult("one.zip", "/one.zip", Long.MAX_VALUE, 0L, Long.MAX_VALUE),
                ArchiveCreationResult("two.zip", "/two.zip", 1L, Long.MAX_VALUE, 1L),
            ),
        )

        assertEquals(Long.MAX_VALUE, result.filesCompressed)
        assertEquals(Long.MAX_VALUE, result.directoriesAdded)
        assertEquals(Long.MAX_VALUE, result.sourceBytesRead)
    }

    @Test
    fun separatePlanCreatesTwoRealZipOutputsThroughOneHostSession() {
        val cacheRoot = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "archive-batch-${UUID.randomUUID()}",
        ).apply { check(mkdirs()) }
        try {
            val host = MultiOutputHostSession(cacheRoot, listOf("A", "BB"))
            val request = request("alpha.txt", "beta.txt", hostSession = host)
            val plan = ArchiveCreationPlanner.plan(
                request = request,
                format = ArchiveFormat.ZIP,
                options = ArchiveCreationOptions(
                    outputDisplayName = "Documents.zip",
                    compressionLevel = 6,
                    conflictPolicy = ArchiveCreationConflictPolicy.ASK,
                ),
                separateArchives = true,
                fallbackStem = "Archive",
            )

            val result = ArchiveCreationBatchExecutor.execute(
                writer = ZipArchiveCreator(host, cacheRoot),
                plan = plan,
                checkCancelled = { },
                progress = ArchiveCreationBatchProgressListener { },
            )

            assertEquals(listOf("alpha.txt.zip", "beta.txt.zip"), result.outputs.map {
                it.outputDisplayName
            })
            assertEquals(2L, result.filesCompressed)
            assertEquals(2, host.commitCalls)
            assertEquals(0, host.abortCalls)
            assertEquals(
                listOf(
                    ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
                    ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
                ),
                host.conflictPolicies,
            )
            assertZipContains(host.committedFile("alpha.txt.zip"), "alpha.txt", "A")
            assertZipContains(host.committedFile("beta.txt.zip"), "beta.txt", "BB")
        } finally {
            cacheRoot.deleteRecursively()
        }
    }

    private fun separatePlan(vararg names: String): ArchiveCreationPlan =
        ArchiveCreationPlanner.plan(
            request = request(*names),
            format = ArchiveFormat.ZIP,
            options = ArchiveCreationOptions(
                outputDisplayName = "Documents.zip",
                compressionLevel = 6,
                conflictPolicy = ArchiveCreationConflictPolicy.ASK,
            ),
            separateArchives = true,
            fallbackStem = "Archive",
        )

    private fun request(
        vararg names: String,
        hostSession: IExplorerActionHostSession = NoOpHostSession(),
    ): ArchiveCompressionRequest {
        val parentUri = Uri.parse("content://archive-manager-batch-test/Documents")
        return ArchiveCompressionRequest(
            requestId = "123e4567-e89b-12d3-a456-426614174000",
            parentUri = parentUri,
            parentDisplayPath = "/Documents",
            targets = names.mapIndexed { index, name ->
                ArchiveCompressionTarget(
                    id = "target-${index + 1}",
                    uri = parentUri.buildUpon().appendPath(name).build(),
                    displayName = name,
                    kind = ExplorerActionValues.TARGET_FILE,
                    mimeType = "text/plain",
                    size = (index + 1).toLong(),
                    lastModified = 1_700_000_000_000L + index,
                )
            },
            hostSession = hostSession,
        )
    }

    private fun assertZipContains(
        archive: File,
        expectedName: String,
        expectedContent: String,
    ) {
        ZipFile(archive).use { zip ->
            assertEquals(listOf(expectedName), zip.fileHeaders.map { it.fileName })
            zip.getInputStream(zip.getFileHeader(expectedName)).use { input ->
                assertEquals(expectedContent, input.readBytes().decodeToString())
            }
        }
    }

    private fun scanningProgress(currentEntry: String) = ArchiveCreationProgress(
        phase = ArchiveCreationPhase.SCANNING,
        currentEntry = currentEntry,
        completedFiles = 1L,
        completedDirectories = 0L,
        sourceBytesRead = 0L,
        totalFiles = 1L,
        totalDirectories = 0L,
        knownSourceBytes = 1L,
        unknownSizeFiles = 0L,
    )

    private class FakeWriter(
        private val write: (
            ArchiveCompressionRequest,
            ArchiveCreationOptions,
            ArchiveCreationProgressListener,
        ) -> ArchiveCreationResult,
    ) : ArchiveWriter {
        override val format = ArchiveFormat.ZIP
        override val formatCapabilities = ZipArchiveBackend.capabilities

        override fun create(
            request: ArchiveCompressionRequest,
            options: ArchiveCreationOptions,
            checkCancelled: () -> Unit,
            progress: ArchiveCreationProgressListener,
        ): ArchiveCreationResult = write(request, options, progress)
    }

    private class NoOpHostSession : TestExplorerActionHostSession() {
        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle = error("Not used")

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor =
            error("Not used")

        override fun prepareOutput(displayName: String, mimeType: String, conflictPolicy: Int): Bundle =
            error("Not used")

        override fun openOutput(transactionId: String): ParcelFileDescriptor = error("Not used")

        override fun openPendingOutput(transactionId: String): ParcelFileDescriptor =
            error("Not used")

        override fun commitOutput(transactionId: String): Bundle = error("Not used")

        override fun abortOutput(transactionId: String) = Unit

        override fun close() = Unit
    }

    private class MultiOutputHostSession(
        private val root: File,
        sourceContents: List<String>,
    ) : TestExplorerActionHostSession() {
        private data class PendingOutput(
            val id: String,
            val displayName: String,
            val file: File,
        )

        private val sources = sourceContents.mapIndexed { index, content ->
            File(root, "source-${index + 1}").apply { writeText(content) }
        }
        private val pending = LinkedHashMap<String, PendingOutput>()
        private val committed = LinkedHashMap<String, File>()
        val conflictPolicies = mutableListOf<Int>()
        var commitCalls = 0
            private set
        var abortCalls = 0
            private set

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle = error("Directory access is not expected")

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            require(relativePath.isEmpty())
            val index = targetId.removePrefix("target-").toInt() - 1
            return ParcelFileDescriptor.open(sources[index], ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle {
            assertEquals(ArchiveFormat.ZIP.primaryMimeType, mimeType)
            conflictPolicies += conflictPolicy
            val id = UUID.randomUUID().toString()
            val output = PendingOutput(id, displayName, File(root, "$id.part"))
            pending[id] = output
            return outputBundle(output)
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            val output = requireNotNull(pending[transactionId])
            return ParcelFileDescriptor.open(
                output.file,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun openPendingOutput(transactionId: String): ParcelFileDescriptor {
            val output = requireNotNull(pending[transactionId])
            return ParcelFileDescriptor.open(output.file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun commitOutput(transactionId: String): Bundle {
            val output = requireNotNull(pending.remove(transactionId))
            check(output.file.isFile)
            committed[output.displayName] = output.file
            commitCalls++
            return outputBundle(output)
        }

        override fun abortOutput(transactionId: String) {
            val output = pending.remove(transactionId)
            output?.file?.delete()
            abortCalls++
        }

        override fun close() = Unit

        fun committedFile(displayName: String): File = requireNotNull(committed[displayName])

        private fun outputBundle(output: PendingOutput): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, output.id)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, output.displayName)
            putString(
                ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH,
                "/Documents/${output.displayName}",
            )
        }
    }
}
