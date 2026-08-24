@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.android.material.textfield.TextInputLayout
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.IExplorerActionHostSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CreateArchiveActivityInstrumentationTest {

    @Test
    fun mismatchedPasswordConfirmationStaysInTheFormAndDoesNotPrepareOutput() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.EditText>(R.id.password).setText("first-password")
                activity.findViewById<android.widget.EditText>(R.id.passwordConfirmation)
                    .setText("different-password")

                activity.findViewById<android.view.View>(R.id.createButton).performClick()

                val confirmationLayout = activity.findViewById<TextInputLayout>(
                    R.id.passwordConfirmationLayout,
                )
                assertEquals(
                    activity.getString(R.string.error_password_confirmation_mismatch),
                    confirmationLayout.error?.toString(),
                )
                assertFalse(activity.isFinishing)
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun formatSelectionReplacesTheCompleteSuffixAndAppliesFormatCapabilities() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val formatView = activity.findViewById<android.widget.AutoCompleteTextView>(R.id.format)
                val outputName = activity.findViewById<android.widget.EditText>(R.id.outputName)
                val password = activity.findViewById<android.widget.EditText>(R.id.password)
                val compressionLevel = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.compressionLevel,
                )
                val labels = (0 until formatView.adapter.count).map { index ->
                    formatView.adapter.getItem(index).toString()
                }
                assertEquals(
                    listOf("ZIP", "7Z", "TAR", "TAR.GZ", "TAR.XZ", "TAR.BZ2", "TAR.ZST"),
                    labels,
                )
                password.setText("transient-password")

                selectFormat(formatView, labels.indexOf("7Z"))

                assertEquals("report.txt.7z", outputName.text.toString())
                assertEquals("transient-password", password.text.toString())
                assertTrue(password.isEnabled)
                assertTrue(compressionLevel.isEnabled)

                selectFormat(formatView, labels.indexOf("TAR"))

                assertEquals("report.txt.tar", outputName.text.toString())
                assertEquals("", password.text.toString())
                assertFalse(password.isEnabled)
                assertFalse(compressionLevel.isEnabled)

                selectFormat(formatView, labels.indexOf("TAR.ZST"))

                assertEquals("report.txt.tar.zst", outputName.text.toString())
                assertFalse(password.isEnabled)
                assertTrue(compressionLevel.isEnabled)

                selectFormat(formatView, labels.indexOf("ZIP"))

                assertEquals("report.txt.zip", outputName.text.toString())
                assertTrue(password.isEnabled)
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun formatAndCompressionLevelOpenFromARealClick() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val formatView = activity.findViewById<android.widget.AutoCompleteTextView>(R.id.format)
                val compressionLevel = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.compressionLevel,
                )

                assertTrue(formatView.performClick())
                assertTrue(formatView.isPopupShowing)
                formatView.dismissDropDown()

                assertTrue(compressionLevel.performClick())
                assertTrue(compressionLevel.isPopupShowing)
                compressionLevel.dismissDropDown()
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun creationConflictPolicyDefaultsToAutoRenameAndSurvivesRecreation() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val policy = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.creationConflictPolicy,
                )
                val details = activity.findViewById<android.widget.TextView>(
                    R.id.creationConflictPolicyDetails,
                )
                assertEquals(
                    listOf(
                        activity.getString(R.string.text_conflict_policy_auto_rename),
                        activity.getString(R.string.text_conflict_policy_ask),
                    ),
                    (0 until policy.adapter.count).map { policy.adapter.getItem(it).toString() },
                )
                assertEquals(
                    activity.getString(R.string.text_conflict_policy_auto_rename),
                    policy.text.toString(),
                )
                assertEquals(
                    activity.getString(
                        R.string.text_creation_conflict_policy_auto_rename_details,
                    ),
                    details.text.toString(),
                )

                selectDropdown(policy, 1)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(
                    activity.getString(R.string.text_conflict_policy_ask),
                    activity.findViewById<android.widget.AutoCompleteTextView>(
                        R.id.creationConflictPolicy,
                    ).text.toString(),
                )
                assertEquals(
                    activity.getString(R.string.text_creation_conflict_policy_ask_details),
                    activity.findViewById<android.widget.TextView>(
                        R.id.creationConflictPolicyDetails,
                    ).text.toString(),
                )
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun askRetriesWithAutomaticNumberingWithoutReadingSources() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val hostSession = RecordingHostSession(rejectExactName = true)
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val policy = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.creationConflictPolicy,
                )
                selectDropdown(policy, 1)
                activity.findViewById<android.view.View>(R.id.createButton).performClick()
            }

            assertTrue(hostSession.exactNameAttempt.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val dialog = requireNotNull(activity.currentUnavailableNameDialog())
                assertTrue(dialog.isShowing)
                assertEquals(
                    activity.getString(
                        R.string.dialog_message_archive_name_unavailable,
                        "report.txt.zip",
                        "/Documents",
                    ),
                    requireNotNull(
                        dialog.findViewById<android.widget.TextView>(android.R.id.message),
                    ).text.toString(),
                )
                assertTrue(
                    dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).isShown,
                )
                assertTrue(
                    dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).isShown,
                )
                assertTrue(
                    dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).isShown,
                )
                dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            }

            assertTrue(hostSession.autoRenameAttempt.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            assertEquals(
                listOf(
                    ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL,
                    ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
                ),
                hostSession.conflictPolicies.toList(),
            )
            assertEquals(0, hostSession.sourceAccessCalls)
        }
    }

    private fun selectFormat(
        view: android.widget.AutoCompleteTextView,
        position: Int,
    ) = selectDropdown(view, position)

    private fun selectDropdown(
        view: android.widget.AutoCompleteTextView,
        position: Int,
    ) {
        val adapter = view.adapter
        view.onItemClickListener.onItemClick(
            null,
            adapter.getView(position, null, null),
            position,
            adapter.getItemId(position),
        )
    }

    private fun compressionIntent(hostSession: IExplorerActionHostSession): Intent {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parentUri = Uri.parse("content://archive-manager-test/Documents")
        val targetUri = Uri.parse("content://archive-manager-test/Documents/report.txt")
        val target = Bundle().apply {
            putString(ExplorerActionTargetKeys.ID, "target-1")
            putParcelable(ExplorerActionTargetKeys.URI, targetUri)
            putString(ExplorerActionTargetKeys.DISPLAY_NAME, "report.txt")
            putInt(ExplorerActionTargetKeys.KIND, ExplorerActionValues.TARGET_FILE)
            putString(ExplorerActionTargetKeys.MIME_TYPE, "text/plain")
            putLong(ExplorerActionTargetKeys.SIZE, 6L)
            putLong(ExplorerActionTargetKeys.LAST_MODIFIED, 1_700_000_000_000L)
        }
        val session = Bundle().apply {
            putBinder(ExplorerActionHostSessionKeys.BINDER, hostSession.asBinder())
        }
        return Intent(ExplorerActionPluginActions.EXECUTE)
            .setClass(context, CreateArchiveActivity::class.java)
            .setDataAndType(targetUri, "text/plain")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ArchiveManagerPlugin.ACTION_COMPRESS_SINGLE_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
            .putExtra(ExplorerActionIntentExtras.REQUEST_ID, REQUEST_ID)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(ExplorerActionIntentExtras.PARENT_DISPLAY_PATH, "/Documents")
            .putExtra(
                ExplorerActionIntentExtras.SOURCE_SURFACE,
                ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
            )
            .putParcelableArrayListExtra(ExplorerActionIntentExtras.TARGETS, arrayListOf(target))
            .putExtra(ExplorerActionIntentExtras.HOST_SESSION, session)
            .apply {
                clipData = ClipData(
                    ClipDescription("Compression target", arrayOf("text/plain")),
                    ClipData.Item(targetUri),
                )
            }
    }

    private class RecordingHostSession(
        private val rejectExactName: Boolean = false,
    ) : IExplorerActionHostSession.Stub() {
        var prepareOutputCalls = 0
            private set
        var sourceAccessCalls = 0
            private set
        val conflictPolicies = CopyOnWriteArrayList<Int>()
        val exactNameAttempt = CountDownLatch(1)
        val autoRenameAttempt = CountDownLatch(1)

        override fun listChildren(targetId: String, relativePath: String, offset: Int, limit: Int): Bundle {
            sourceAccessCalls += 1
            error("Directory access is not expected")
        }

        override fun openFile(targetId: String, relativePath: String): ParcelFileDescriptor {
            sourceAccessCalls += 1
            error("File access is not expected")
        }

        override fun prepareOutput(displayName: String, mimeType: String, conflictPolicy: Int): Bundle {
            prepareOutputCalls += 1
            conflictPolicies += conflictPolicy
            if (rejectExactName && conflictPolicy == ExplorerActionHostSessionValues.OUTPUT_CONFLICT_FAIL) {
                exactNameAttempt.countDown()
                throw IllegalArgumentException("Synthetic exact-name conflict")
            }
            if (
                rejectExactName &&
                conflictPolicy == ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME
            ) {
                autoRenameAttempt.countDown()
                error("Stop after observing automatic numbering")
            }
            error("Output preparation is not expected")
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor =
            error("Output access is not expected")

        override fun commitOutput(transactionId: String): Bundle = error("Commit is not expected")

        override fun abortOutput(transactionId: String) = Unit

        override fun close() = Unit
    }

    private companion object {
        const val REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000"
    }
}
