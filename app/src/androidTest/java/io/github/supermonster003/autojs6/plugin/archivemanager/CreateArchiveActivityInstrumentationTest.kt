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
import java.io.IOException
import java.util.UUID
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
    fun zipSplitVolumePresetsFitNamesAndSurviveFormatChangesAndRecreation() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val splitVolume = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.splitVolume,
                )
                val splitLayout = activity.findViewById<TextInputLayout>(R.id.splitVolumeLayout)
                val outputName = activity.findViewById<android.widget.EditText>(R.id.outputName)
                val labels = (0 until splitVolume.adapter.count).map { index ->
                    splitVolume.adapter.getItem(index).toString()
                }
                assertEquals(
                    listOf(
                        activity.getString(R.string.text_none),
                        activity.getString(R.string.text_split_volume_mib, 10L),
                        activity.getString(R.string.text_split_volume_mib, 50L),
                        activity.getString(R.string.text_split_volume_mib, 100L),
                        activity.getString(R.string.text_split_volume_mib, 500L),
                        activity.getString(R.string.text_split_volume_mib, 1_024L),
                        activity.getString(R.string.text_split_volume_mib, 4_096L),
                    ),
                    labels,
                )
                assertTrue(splitVolume.isEnabled)
                assertEquals(activity.getString(R.string.text_none), splitVolume.text.toString())
                assertEquals(
                    activity.getString(
                        R.string.text_split_volume_helper,
                        ArchiveSplitVolumePolicy.MIN_SIZE_MIB,
                        ArchiveSplitVolumePolicy.MAX_SIZE_MIB,
                    ),
                    splitLayout.helperText?.toString(),
                )
                outputName.setText("a".repeat(251) + ".zip")

                splitVolume.setText(labels[3], false)
                selectDropdown(splitVolume, 3)

                assertEquals(labels[3], splitVolume.text.toString())
                assertEquals(
                    ArchiveSplitVolumePolicy.MAX_TERMINAL_DISPLAY_NAME_LENGTH,
                    outputName.text.length,
                )
                val format = activity.findViewById<android.widget.AutoCompleteTextView>(R.id.format)
                selectFormat(format, 1)
                assertFalse(splitVolume.isEnabled)
                assertEquals(activity.getString(R.string.text_none), splitVolume.text.toString())
                assertEquals(
                    activity.getString(R.string.text_split_unavailable_for_format, "7Z"),
                    splitLayout.helperText?.toString(),
                )

                selectFormat(format, 0)
                assertTrue(splitVolume.isEnabled)
                assertEquals(labels[3], splitVolume.text.toString())
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(
                    activity.getString(R.string.text_split_volume_mib, 100L),
                    activity.findViewById<android.widget.AutoCompleteTextView>(
                        R.id.splitVolume,
                    ).text.toString(),
                )
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun customSplitVolumeRestoresAndInvalidValueBlocksCreation() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.AutoCompleteTextView>(R.id.splitVolume)
                    .setText("257")
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                val splitVolume = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.splitVolume,
                )
                assertEquals("257", splitVolume.text.toString())

                splitVolume.setText("0")
                activity.findViewById<android.view.View>(R.id.createButton).performClick()

                assertEquals(
                    activity.getString(
                        R.string.error_split_volume_invalid,
                        ArchiveSplitVolumePolicy.MIN_SIZE_MIB,
                        ArchiveSplitVolumePolicy.MAX_SIZE_MIB,
                    ),
                    activity.findViewById<TextInputLayout>(R.id.splitVolumeLayout)
                        .error
                        ?.toString(),
                )
                assertFalse(activity.isFinishing)
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
    fun separateModePreviewsEveryRequestedNameAndKeepsAutomaticNumbering() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(multipleCompressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val separate = activity.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                    R.id.separateArchives,
                )
                val preview = activity.findViewById<android.widget.TextView>(
                    R.id.separateArchivesPreview,
                )
                val outputNameLayout = activity.findViewById<TextInputLayout>(R.id.outputNameLayout)
                val policy = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.creationConflictPolicy,
                )
                val policyDetails = activity.findViewById<android.widget.TextView>(
                    R.id.creationConflictPolicyDetails,
                )

                assertTrue(separate.isEnabled)
                assertFalse(separate.isChecked)
                assertFalse(preview.isShown)
                assertEquals("Documents.zip", activity.findViewById<android.widget.EditText>(
                    R.id.outputName,
                ).text.toString())
                selectDropdown(policy, 1)

                separate.performClick()

                assertTrue(separate.isChecked)
                assertEquals(android.view.View.GONE, outputNameLayout.visibility)
                assertFalse(policy.isEnabled)
                assertEquals(
                    activity.getString(R.string.text_conflict_policy_auto_rename),
                    policy.text.toString(),
                )
                assertEquals(
                    activity.getString(R.string.text_separate_archive_conflict_note),
                    policyDetails.text.toString(),
                )
                assertTrue(preview.isShown)
                val previewText = preview.text.toString()
                assertTrue(previewText.contains(activity.getString(
                    R.string.text_separate_archive_preview_count,
                    3,
                )))
                assertTrue(previewText.contains("report.txt.zip"))
                assertTrue(previewText.contains("photos.zip"))
                assertTrue(previewText.contains("notes.md.zip"))
                assertTrue(previewText.contains(
                    activity.getString(R.string.text_separate_archive_conflict_note),
                ))
                assertFalse(previewText.contains('\u2026'))

                val formatView = activity.findViewById<android.widget.AutoCompleteTextView>(R.id.format)
                selectFormat(formatView, 1)
                assertTrue(preview.text.toString().contains("report.txt.7z"))
                selectFormat(formatView, 0)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                val separate = activity.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                    R.id.separateArchives,
                )
                val policy = activity.findViewById<android.widget.AutoCompleteTextView>(
                    R.id.creationConflictPolicy,
                )
                assertTrue(separate.isChecked)
                assertTrue(activity.findViewById<android.view.View>(
                    R.id.separateArchivesPreview,
                ).isShown)

                separate.performClick()

                assertFalse(separate.isChecked)
                assertTrue(activity.findViewById<android.view.View>(R.id.outputNameLayout).isShown)
                assertTrue(policy.isEnabled)
                assertEquals(
                    activity.getString(R.string.text_conflict_policy_ask),
                    policy.text.toString(),
                )
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun batchProgressIncludesTheCurrentOutputOrdinalAndName() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(multipleCompressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                activity.renderCreationBatchProgress(
                    ArchiveCreationBatchProgress(
                        archiveIndex = 2,
                        totalArchives = 3,
                        requestedOutputDisplayName = "photos.zip",
                        sourceDisplayName = "photos",
                        creation = ArchiveCreationProgress(
                            phase = ArchiveCreationPhase.COMMITTING,
                            currentEntry = null,
                            completedFiles = 4L,
                            completedDirectories = 1L,
                            sourceBytesRead = 20L,
                            totalFiles = 4L,
                            totalDirectories = 1L,
                            knownSourceBytes = 20L,
                            unknownSizeFiles = 0L,
                        ),
                    ),
                )

                assertEquals(
                    activity.getString(
                        R.string.text_separate_archive_progress,
                        2,
                        3,
                        "photos.zip",
                        activity.getString(R.string.text_committing_archive),
                    ),
                    activity.findViewById<android.widget.TextView>(R.id.status).text.toString(),
                )
                assertEquals(0, hostSession.prepareOutputCalls)
            }
        }
    }

    @Test
    fun partialBatchFailureClosesTheSessionAndDisablesBlindRetry() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(multipleCompressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val operationFailure = IOException("synthetic later failure")
                activity.renderPartialCreationFailure(
                    ArchiveCreationPartialFailureException(
                        completedOutputs = listOf(
                            ArchiveCreationResult(
                                outputDisplayName = "report.txt.zip",
                                outputDisplayPath = "/Documents/report.txt.zip",
                                filesCompressed = 1L,
                                directoriesAdded = 0L,
                                sourceBytesRead = 6L,
                            ),
                        ),
                        totalOutputs = 3,
                        failedOutputIndex = 2,
                        failedRequestedOutputDisplayName = "photos.zip",
                        failedSourceDisplayName = "photos",
                        operationFailure = operationFailure,
                    ),
                )

                assertEquals(
                    activity.getString(
                        R.string.error_separate_compression_partial_failure,
                        1,
                        3,
                        "photos.zip",
                        "synthetic later failure",
                    ),
                    activity.findViewById<android.widget.TextView>(R.id.status).text.toString(),
                )
                assertFalse(activity.findViewById<android.view.View>(R.id.createButton).isEnabled)
                assertFalse(activity.findViewById<android.view.View>(R.id.separateArchives).isEnabled)
                assertEquals(
                    activity.getString(android.R.string.ok),
                    activity.findViewById<android.widget.Button>(R.id.cancelButton).text.toString(),
                )
                assertEquals(1, hostSession.closeCalls)
                assertFalse(activity.isFinishing)
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

    @Test
    fun rollbackFailureClosesTheSessionAndRemainsTerminalAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val hostSession = RollbackFailingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<android.widget.EditText>(R.id.password)
                    .setText("must-be-cleared")
                activity.findViewById<android.widget.EditText>(R.id.passwordConfirmation)
                    .setText("must-be-cleared")
                activity.findViewById<android.view.View>(R.id.createButton).performClick()
            }

            assertTrue(hostSession.abortAttempt.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            lateinit var expectedMessage: String
            scenario.onActivity { activity ->
                expectedMessage = activity.getString(
                    R.string.error_compression_cleanup_unconfirmed,
                    "/Documents/report.txt.zip",
                    "simulated output open failure",
                )
                assertEquals(
                    expectedMessage,
                    activity.findViewById<android.widget.TextView>(R.id.status).text.toString(),
                )
                assertFalse(activity.findViewById<android.view.View>(R.id.createButton).isEnabled)
                assertEquals(
                    activity.getString(android.R.string.ok),
                    activity.findViewById<android.widget.Button>(R.id.cancelButton).text.toString(),
                )
                assertEquals(
                    "",
                    activity.findViewById<android.widget.EditText>(R.id.password).text.toString(),
                )
                assertFalse(activity.isFinishing)
            }
            assertEquals(1, hostSession.prepareOutputCalls)
            assertEquals(1, hostSession.abortCalls)
            assertEquals(1, hostSession.closeCalls)

            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(
                    expectedMessage,
                    activity.findViewById<android.widget.TextView>(R.id.status).text.toString(),
                )
                assertFalse(activity.findViewById<android.view.View>(R.id.createButton).isEnabled)
                assertEquals(
                    activity.getString(android.R.string.ok),
                    activity.findViewById<android.widget.Button>(R.id.cancelButton).text.toString(),
                )
            }
            assertEquals(1, hostSession.prepareOutputCalls)
            assertEquals(1, hostSession.abortCalls)
            assertEquals(2, hostSession.closeCalls)
        }
    }

    @Test
    fun creationProgressRendersScanningExactUnknownAndCommitPhases() {
        val hostSession = RecordingHostSession()
        ActivityScenario.launch<CreateArchiveActivity>(compressionIntent(hostSession)).use { scenario ->
            scenario.onActivity { activity ->
                val status = activity.findViewById<android.widget.TextView>(R.id.status)

                activity.renderCreationProgress(
                    ArchiveCreationProgress(
                        phase = ArchiveCreationPhase.SCANNING,
                        currentEntry = null,
                        completedFiles = 0L,
                        completedDirectories = 0L,
                        sourceBytesRead = 0L,
                        totalFiles = 0L,
                        totalDirectories = 0L,
                        knownSourceBytes = 0L,
                        unknownSizeFiles = 0L,
                    ),
                )
                assertEquals(
                    activity.getString(R.string.text_scanning_compression_sources),
                    status.text.toString(),
                )

                activity.renderCreationProgress(
                    ArchiveCreationProgress(
                        phase = ArchiveCreationPhase.SCANNING,
                        currentEntry = "folder/report.txt",
                        completedFiles = 2L,
                        completedDirectories = 1L,
                        sourceBytesRead = 0L,
                        totalFiles = 2L,
                        totalDirectories = 1L,
                        knownSourceBytes = 8L,
                        unknownSizeFiles = 0L,
                    ),
                )
                assertEquals(
                    activity.resources.getQuantityString(
                        R.plurals.text_scanning_compression_progress,
                        2,
                        "folder/report.txt",
                        2L,
                        1L,
                    ),
                    status.text.toString(),
                )

                activity.renderCreationProgress(
                    ArchiveCreationProgress(
                        phase = ArchiveCreationPhase.COMPRESSING,
                        currentEntry = "folder/report.txt",
                        completedFiles = 1L,
                        completedDirectories = 1L,
                        sourceBytesRead = 4L,
                        totalFiles = 2L,
                        totalDirectories = 1L,
                        knownSourceBytes = 8L,
                        unknownSizeFiles = 0L,
                    ),
                )
                assertEquals(
                    activity.resources.getQuantityString(
                        R.plurals.text_compressing_progress,
                        2,
                        "folder/report.txt",
                        1L,
                        2L,
                        android.text.format.Formatter.formatShortFileSize(activity, 4L),
                        android.text.format.Formatter.formatShortFileSize(activity, 8L),
                    ),
                    status.text.toString(),
                )

                activity.renderCreationProgress(
                    ArchiveCreationProgress(
                        phase = ArchiveCreationPhase.COMPRESSING,
                        currentEntry = "folder/stream.bin",
                        completedFiles = 1L,
                        completedDirectories = 1L,
                        sourceBytesRead = 7L,
                        totalFiles = 2L,
                        totalDirectories = 1L,
                        knownSourceBytes = 4L,
                        unknownSizeFiles = 1L,
                    ),
                )
                assertEquals(
                    activity.resources.getQuantityString(
                        R.plurals.text_compressing_progress_unknown_sizes,
                        1,
                        "folder/stream.bin",
                        1L,
                        2L,
                        android.text.format.Formatter.formatShortFileSize(activity, 7L),
                        1L,
                    ),
                    status.text.toString(),
                )

                activity.renderCreationProgress(
                    ArchiveCreationProgress(
                        phase = ArchiveCreationPhase.COMMITTING,
                        currentEntry = null,
                        completedFiles = 2L,
                        completedDirectories = 1L,
                        sourceBytesRead = 8L,
                        totalFiles = 2L,
                        totalDirectories = 1L,
                        knownSourceBytes = 8L,
                        unknownSizeFiles = 0L,
                    ),
                )
                assertEquals(
                    activity.getString(R.string.text_committing_archive),
                    status.text.toString(),
                )
                assertEquals(0, hostSession.prepareOutputCalls)
            }
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

    private fun compressionIntent(hostSession: IExplorerActionHostSession): Intent =
        compressionIntent(hostSession, listOf("report.txt"))

    private fun multipleCompressionIntent(hostSession: IExplorerActionHostSession): Intent =
        compressionIntent(hostSession, listOf("report.txt", "photos", "notes.md"))

    private fun compressionIntent(
        hostSession: IExplorerActionHostSession,
        targetNames: List<String>,
    ): Intent {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parentUri = Uri.parse("content://archive-manager-test/Documents")
        val targetUris = targetNames.map { name ->
            Uri.parse("content://archive-manager-test/Documents/$name")
        }
        val targets = targetNames.mapIndexedTo(arrayListOf()) { index, name ->
            Bundle().apply {
                putString(ExplorerActionTargetKeys.ID, "target-${index + 1}")
                putParcelable(ExplorerActionTargetKeys.URI, targetUris[index])
                putString(ExplorerActionTargetKeys.DISPLAY_NAME, name)
                putInt(ExplorerActionTargetKeys.KIND, ExplorerActionValues.TARGET_FILE)
                putString(ExplorerActionTargetKeys.MIME_TYPE, "text/plain")
                putLong(ExplorerActionTargetKeys.SIZE, 6L + index)
                putLong(ExplorerActionTargetKeys.LAST_MODIFIED, 1_700_000_000_000L + index)
            }
        }
        val session = Bundle().apply {
            putBinder(ExplorerActionHostSessionKeys.BINDER, hostSession.asBinder())
        }
        val clip = ClipData(
            ClipDescription("Compression target", arrayOf("text/plain")),
            ClipData.Item(targetUris.first()),
        ).apply {
            targetUris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
        }
        return Intent(ExplorerActionPluginActions.EXECUTE)
            .setClass(context, CreateArchiveActivity::class.java)
            .setDataAndType(targetUris.first(), "text/plain")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(
                ExplorerActionIntentExtras.ACTION_ID,
                if (targetNames.size == 1) {
                    ArchiveManagerPlugin.ACTION_COMPRESS_SINGLE_ID
                } else {
                    ArchiveManagerPlugin.ACTION_COMPRESS_MULTIPLE_ID
                },
            )
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
            .putExtra(ExplorerActionIntentExtras.REQUEST_ID, REQUEST_ID)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(ExplorerActionIntentExtras.PARENT_DISPLAY_PATH, "/Documents")
            .putExtra(
                ExplorerActionIntentExtras.SOURCE_SURFACE,
                ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
            )
            .putParcelableArrayListExtra(ExplorerActionIntentExtras.TARGETS, targets)
            .putExtra(ExplorerActionIntentExtras.HOST_SESSION, session)
            .apply {
                clipData = clip
            }
    }

    private class RecordingHostSession(
        private val rejectExactName: Boolean = false,
    ) : IExplorerActionHostSession.Stub() {
        var prepareOutputCalls = 0
            private set
        var sourceAccessCalls = 0
            private set
        var closeCalls = 0
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

        override fun close() {
            closeCalls++
        }
    }

    private class RollbackFailingHostSession : IExplorerActionHostSession.Stub() {
        private val transactionId = UUID.randomUUID().toString()
        var prepareOutputCalls = 0
            private set
        var abortCalls = 0
            private set
        var closeCalls = 0
            private set
        val abortAttempt = CountDownLatch(1)

        override fun listChildren(
            targetId: String,
            relativePath: String,
            offset: Int,
            limit: Int,
        ): Bundle = error("Directory access is not expected")

        override fun openFile(
            targetId: String,
            relativePath: String,
        ): ParcelFileDescriptor = error("Source access is not expected")

        override fun prepareOutput(
            displayName: String,
            mimeType: String,
            conflictPolicy: Int,
        ): Bundle {
            prepareOutputCalls++
            assertEquals("report.txt.zip", displayName)
            assertEquals(ArchiveFormat.ZIP.primaryMimeType, mimeType)
            assertEquals(
                ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME,
                conflictPolicy,
            )
            return Bundle().apply {
                putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
                putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, displayName)
                putString(
                    ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH,
                    "/Documents/report.txt.zip",
                )
            }
        }

        override fun openOutput(transactionId: String): ParcelFileDescriptor {
            assertEquals(this.transactionId, transactionId)
            throw IOException("simulated output open failure")
        }

        override fun commitOutput(transactionId: String): Bundle = error("Commit is not expected")

        override fun abortOutput(transactionId: String) {
            assertEquals(this.transactionId, transactionId)
            abortCalls++
            abortAttempt.countDown()
            throw SecurityException("simulated output cleanup denial")
        }

        override fun close() {
            closeCalls++
        }
    }

    private companion object {
        const val REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000"
    }
}
