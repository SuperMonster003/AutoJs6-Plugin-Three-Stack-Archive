package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import org.autojs.plugin.explorer.api.IExplorerActionHostSession

internal abstract class TestExplorerActionHostSession : IExplorerActionHostSession.Stub() {
    override fun attachClient(clientToken: IBinder) = Unit

    override fun prepareTargetReplacement(targetId: String): Bundle =
        error("Target replacement is not used by this test host session")

    override fun prepareOutputTree(displayName: String, conflictPolicy: Int): Bundle =
        error("Directory output is not used by this test host session")

    override fun createOutputDirectory(transactionId: String, relativePath: String): Unit =
        error("Directory output is not used by this test host session")

    override fun openOutputFile(transactionId: String, relativePath: String): ParcelFileDescriptor =
        error("Directory output is not used by this test host session")

    override fun queryOutput(transactionId: String): Bundle =
        error("Output recovery is not used by this test host session")

    override fun listOutputs(): Bundle = Bundle()

    override fun prepareOutputBatch(transactionIds: MutableList<String>): Bundle =
        error("Output batches are not used by this test host session")

    override fun commitOutputBatch(batchId: String): Bundle =
        error("Output batches are not used by this test host session")

    override fun abortOutputBatch(batchId: String) = Unit

    override fun queryOutputBatch(batchId: String): Bundle = Bundle()

    override fun listOutputBatches(): Bundle = Bundle()

    override fun moveTargetsToTrash(
        targetIds: MutableList<String>,
        outputTransactionIds: MutableList<String>,
    ): Bundle = error("Target Trash is not used by this test host session")

    override fun queryTargetTrash(): Bundle = Bundle()

    override fun queryTargetTrashBatch(targetTrashBatchId: String): Bundle = Bundle()

    override fun listTargetTrashBatches(): Bundle = Bundle()

    override fun undoTargetTrashBatch(targetTrashBatchId: String): Bundle =
        error("Target Trash batch undo is not used by this test host session")

    override fun queryTargetReplacement(targetId: String): Bundle = Bundle()

    override fun undoTargetReplacement(targetId: String, replacementHistoryId: String): Bundle =
        error("Target replacement undo is not used by this test host session")

    override fun getPlaybackProgress(targetId: String, relativePath: String): Bundle =
        error("Playback progress is not used by this test host session")

    override fun reportPlaybackProgress(
        targetId: String,
        relativePath: String,
        positionMillis: Long,
        durationMillis: Long,
        reportState: Int,
    ) = error("Playback progress is not used by this test host session")
}

internal open class UnusedTestExplorerActionHostSession : TestExplorerActionHostSession() {
    override fun listChildren(
        targetId: String,
        relativePath: String,
        offset: Int,
        limit: Int,
    ): Bundle = error("Host session is not expected to be called")

    override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor =
        error("Host session is not expected to be called")

    override fun prepareOutput(
        displayName: String,
        mimeType: String,
        conflictPolicy: Int,
    ): Bundle = error("Host session is not expected to be called")

    override fun openOutput(transactionId: String): ParcelFileDescriptor =
        error("Host session is not expected to be called")

    override fun openPendingOutput(transactionId: String): ParcelFileDescriptor =
        error("Host session is not expected to be called")

    override fun commitOutput(transactionId: String): Bundle =
        error("Host session is not expected to be called")

    override fun abortOutput(transactionId: String) = Unit

    override fun close() = Unit
}
