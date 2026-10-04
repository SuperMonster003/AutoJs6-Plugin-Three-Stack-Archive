package io.github.supermonster003.autojs6.plugin.three.stack.archive

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
    fun committedOutputProofPreservesArchiveAndSplitVolumeOrder() {
        val firstPartId = UUID.randomUUID().toString()
        val firstTerminalId = UUID.randomUUID().toString()
        val secondOutputId = UUID.randomUUID().toString()
        val result = ArchiveCreationBatchResult(
            outputs = listOf(
                ArchiveCreationResult(
                    outputDisplayName = "one.zip",
                    outputDisplayPath = "/one.zip",
                    filesCompressed = 1,
                    directoriesAdded = 0,
                    sourceBytesRead = 1,
                    committedOutputTransactionIds = listOf(firstPartId, firstTerminalId),
                    createdOutputs = listOf(
                        CreatedArchiveOutput("one.z01", "/one.z01"),
                        CreatedArchiveOutput("one.zip", "/one.zip"),
                    ),
                ),
                ArchiveCreationResult(
                    outputDisplayName = "two.zip",
                    outputDisplayPath = "/two.zip",
                    filesCompressed = 1,
                    directoriesAdded = 0,
                    sourceBytesRead = 2,
                    committedOutputTransactionIds = listOf(secondOutputId),
                ),
            ),
        )

        assertEquals(3L, result.physicalOutputsCreated)
        assertEquals(
            listOf(firstPartId, firstTerminalId, secondOutputId),
            result.committedOutputTransactionIds,
        )
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
            assertEquals(host.batchMemberIds, result.committedOutputTransactionIds)
            assertEquals(2L, result.filesCompressed)
            assertEquals(0, host.commitCalls)
            assertEquals(1, host.batchCommitCalls)
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

    @Test
    fun laterSourceFailureAbortsEveryDeferredOutputBeforeBatchPreparation() {
        val cacheRoot = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "archive-batch-source-failure-${UUID.randomUUID()}",
        ).apply { check(mkdirs()) }
        try {
            val host = MultiOutputHostSession(
                root = cacheRoot,
                sourceContents = listOf("A", "BB"),
                failSourceIndex = 1,
            )
            val plan = ArchiveCreationPlanner.plan(
                request = request("alpha.txt", "beta.txt", hostSession = host),
                format = ArchiveFormat.ZIP,
                options = ArchiveCreationOptions(
                    outputDisplayName = "Documents.zip",
                    compressionLevel = 6,
                    conflictPolicy = ArchiveCreationConflictPolicy.ASK,
                ),
                separateArchives = true,
                fallbackStem = "Archive",
            )

            assertThrows(ArchiveCreationSourceException::class.java) {
                ArchiveCreationBatchExecutor.execute(
                    writer = ZipArchiveCreator(host, cacheRoot),
                    plan = plan,
                    checkCancelled = { },
                    progress = ArchiveCreationBatchProgressListener { },
                )
            }

            assertEquals(0, host.commitCalls)
            assertEquals(0, host.batchCommitCalls)
            assertEquals(2, host.abortCalls)
            assertEquals(0, host.batchAbortCalls)
            assertEquals(0, host.pendingCount)
            assertEquals(0, host.committedCount)
        } finally {
            cacheRoot.deleteRecursively()
        }
    }

    @Test
    fun failedBatchCommitIsAbortedWithoutPublishingAnyArchive() {
        val cacheRoot = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "archive-batch-commit-failure-${UUID.randomUUID()}",
        ).apply { check(mkdirs()) }
        try {
            val host = MultiOutputHostSession(
                root = cacheRoot,
                sourceContents = listOf("A", "BB"),
                batchCommitFailure = BatchCommitFailure.BEFORE_PUBLICATION,
            )
            val plan = ArchiveCreationPlanner.plan(
                request = request("alpha.txt", "beta.txt", hostSession = host),
                format = ArchiveFormat.ZIP,
                options = ArchiveCreationOptions(
                    outputDisplayName = "Documents.zip",
                    compressionLevel = 6,
                    conflictPolicy = ArchiveCreationConflictPolicy.ASK,
                ),
                separateArchives = true,
                fallbackStem = "Archive",
            )

            assertThrows(ArchiveCreationOutputException::class.java) {
                ArchiveCreationBatchExecutor.execute(
                    writer = ZipArchiveCreator(host, cacheRoot),
                    plan = plan,
                    checkCancelled = { },
                    progress = ArchiveCreationBatchProgressListener { },
                )
            }

            assertEquals(0, host.commitCalls)
            assertEquals(1, host.batchCommitCalls)
            assertEquals(0, host.abortCalls)
            assertEquals(1, host.batchAbortCalls)
            assertEquals(0, host.pendingCount)
            assertEquals(0, host.committedCount)
        } finally {
            cacheRoot.deleteRecursively()
        }
    }

    @Test
    fun ambiguousBatchCommitUsesDurableCommittedStateInsteadOfDuplicatingOutputs() {
        val cacheRoot = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "archive-batch-ambiguous-commit-${UUID.randomUUID()}",
        ).apply { check(mkdirs()) }
        try {
            val host = MultiOutputHostSession(
                root = cacheRoot,
                sourceContents = listOf("A", "BB"),
                batchCommitFailure = BatchCommitFailure.AFTER_PUBLICATION,
            )
            val plan = ArchiveCreationPlanner.plan(
                request = request("alpha.txt", "beta.txt", hostSession = host),
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
            assertEquals(1, host.batchCommitCalls)
            assertEquals(1, host.batchQueryCalls)
            assertEquals(0, host.abortCalls)
            assertEquals(0, host.batchAbortCalls)
            assertEquals(0, host.pendingCount)
            assertEquals(2, host.committedCount)
            assertZipContains(host.committedFile("alpha.txt.zip"), "alpha.txt", "A")
            assertZipContains(host.committedFile("beta.txt.zip"), "beta.txt", "BB")
        } finally {
            cacheRoot.deleteRecursively()
        }
    }

    @Test
    fun cancellationWhileStagingSplitVolumesIsRolledBackWithoutAFalsePartialResult() {
        val cacheRoot = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "archive-batch-split-cancel-${UUID.randomUUID()}",
        ).apply { check(mkdirs()) }
        try {
            val host = MultiOutputHostSession(cacheRoot, listOf("unused source"))
            val session = ExplorerActionHostSessionClient(host)
            val committer = ArchiveOutputBatchCommitter(ArchiveFormat.ZIP)
            val publisher = ZipSplitOutputPublisher(
                session = session,
                requestedTerminalDisplayName = "bundle.zip",
                conflictPolicy = ArchiveCreationConflictPolicy.ASK,
                splitVolumeSizeBytes = 1_048_576L,
                outputCommitter = committer,
            )
            publisher.reserveTerminalBeforeSourceAccess()
            val volumes = listOf(
                StagedZipVolume(
                    file = File(cacheRoot, "staged.z01").apply { writeText("first volume") },
                    suffix = ".z01",
                    terminal = false,
                ),
                StagedZipVolume(
                    file = File(cacheRoot, "staged.zip").apply { writeText("terminal volume") },
                    suffix = ".zip",
                    terminal = true,
                ),
            )
            val terminalName = publisher.reserveCompleteGroup(volumes)
            publisher.writePendingVolumes(volumes, terminalName) { }
            publisher.verifyPendingVolumes(volumes, terminalName) { }
            var cancellationChecks = 0

            val cancellation = assertThrows(CancellationException::class.java) {
                publisher.commitVolumes(volumes, terminalName) {
                    cancellationChecks++
                    if (cancellationChecks == 2) {
                        throw CancellationException("Injected split staging cancellation")
                    }
                }
            }

            assertSame(cancellation, committer.abortPending(cancellation))
            assertEquals(2, host.abortCalls)
            assertEquals(0, host.batchCommitCalls)
            assertEquals(0, host.pendingCount)
            assertEquals(0, host.committedCount)
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
        override val supportsOutputBatching = false

        override fun create(
            request: ArchiveCompressionRequest,
            options: ArchiveCreationOptions,
            checkCancelled: () -> Unit,
            progress: ArchiveCreationProgressListener,
            outputCommitter: ArchiveOutputCommitter,
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
        private val failSourceIndex: Int? = null,
        private val batchCommitFailure: BatchCommitFailure = BatchCommitFailure.NONE,
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
        var batchCommitCalls = 0
            private set
        var batchAbortCalls = 0
            private set
        var batchQueryCalls = 0
            private set
        var abortCalls = 0
            private set
        private var batchId: String? = null
        private var batchMembers = emptyList<String>()
        private var batchState = ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_UNKNOWN

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle = error("Directory access is not expected")

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            require(relativePath.isEmpty())
            val index = targetId.removePrefix("target-").toInt() - 1
            if (index == failSourceIndex) throw IOException("Injected source failure")
            return ParcelFileDescriptor.open(sources[index], ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle {
            assertEquals(
                true,
                mimeType == ArchiveFormat.ZIP.primaryMimeType || mimeType == "application/octet-stream",
            )
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

        override fun prepareOutputBatch(transactionIds: MutableList<String>): Bundle {
            require(transactionIds.size >= 2 && transactionIds.all(pending::containsKey))
            check(batchId == null)
            batchId = UUID.randomUUID().toString()
            batchMembers = transactionIds.toList()
            batchState = ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_PREPARED
            return batchBundle()
        }

        override fun commitOutputBatch(batchId: String): Bundle {
            require(batchId == this.batchId)
            batchCommitCalls++
            batchState = ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTING
            if (batchCommitFailure == BatchCommitFailure.BEFORE_PUBLICATION) {
                throw IOException("Injected batch commit failure")
            }
            val results = ArrayList<Bundle>(batchMembers.size)
            batchMembers.forEach { transactionId ->
                val output = requireNotNull(pending.remove(transactionId))
                check(output.file.isFile)
                committed[output.displayName] = output.file
                results += outputBundle(output).apply {
                    putInt(
                        ExplorerActionHostSessionKeys.OUTPUT_STATE,
                        ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED,
                    )
                }
            }
            batchState = ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTED
            if (batchCommitFailure == BatchCommitFailure.AFTER_PUBLICATION) {
                throw IOException("Injected ambiguous batch commit result")
            }
            return batchBundle().apply {
                putParcelableArrayList(ExplorerActionHostSessionKeys.OUTPUTS, results)
            }
        }

        override fun abortOutputBatch(batchId: String) {
            require(batchId == this.batchId)
            batchMembers.forEach { transactionId ->
                pending.remove(transactionId)?.file?.delete()
            }
            batchState = ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_ABORTED
            batchAbortCalls++
        }

        override fun queryOutputBatch(batchId: String): Bundle {
            require(batchId == this.batchId)
            batchQueryCalls++
            return batchBundle()
        }

        override fun close() = Unit

        fun committedFile(displayName: String): File = requireNotNull(committed[displayName])

        val pendingCount: Int
            get() = pending.size

        val committedCount: Int
            get() = committed.size

        val batchMemberIds: List<String>
            get() = batchMembers.toList()

        private fun outputBundle(output: PendingOutput): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, output.id)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, output.displayName)
            putString(
                ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH,
                "/Documents/${output.displayName}",
            )
        }

        private fun batchBundle(): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_BATCH_ID, batchId)
            putInt(ExplorerActionHostSessionKeys.OUTPUT_BATCH_STATE, batchState)
            putStringArrayList(
                ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_IDS,
                ArrayList(batchMembers),
            )
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_BATCH_PUBLISHED_COUNT,
                if (batchState == ExplorerActionHostSessionValues.OUTPUT_BATCH_STATE_COMMITTED) {
                    batchMembers.size
                } else {
                    0
                },
            )
        }
    }

    private enum class BatchCommitFailure {
        NONE,
        BEFORE_PUBLICATION,
        AFTER_PUBLICATION,
    }
}
