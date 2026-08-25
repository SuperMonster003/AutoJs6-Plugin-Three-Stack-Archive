package io.github.supermonster003.autojs6.plugin.archivemanager

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
