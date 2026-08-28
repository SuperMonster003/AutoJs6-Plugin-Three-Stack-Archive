package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ArchiveExtractionScopeInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver

    @Before
    fun resetProviderBeforeTest() = resetProvider()

    @After
    fun resetProviderAfterTest() = resetProvider()

    @Test
    fun managementDialogOffersOnlyScopesAvailableAtTheCurrentLocation() {
        val archiveUri = createArchiveDocument()

        ActivityScenario.launch<ArchiveManagerActivity>(archiveIntent(archiveUri)).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled &&
                    activity.findViewById<RecyclerView>(R.id.entryList)
                        .findViewHolderForAdapterPosition(0) != null
            }
            holdForExternalInspection()

            scenario.onActivity { activity ->
                assertFalse(activity.canUseHostExtractionDestination())
                assertTrue(activity.findViewById<android.view.View>(R.id.addFilesButton).isShown)
                assertTrue(activity.findViewById<android.view.View>(R.id.addFolderButton).isShown)
                assertTrue(activity.findViewById<android.view.View>(R.id.newFolderButton).isShown)
                assertFalse(activity.findViewById<android.view.View>(R.id.renameButton).isEnabled)
                assertFalse(activity.findViewById<android.view.View>(R.id.deleteButton).isEnabled)
                requireNotNull(activity.showExtractionScopeDialog()).let { dialog ->
                    assertEquals(
                        listOf(activity.getString(R.string.text_extraction_scope_entire_archive)),
                        dialogLabels(dialog),
                    )
                    dialog.dismiss()
                }
                val list = activity.findViewById<RecyclerView>(R.id.entryList)
                assertTrue(requireNotNull(list.findViewHolderForAdapterPosition(0)).itemView.performClick())
            }

            waitForActivity(scenario) { activity ->
                activity.findViewById<android.widget.TextView>(R.id.currentPath).text.toString() == "/docs" &&
                    activity.findViewById<RecyclerView>(R.id.entryList)
                        .findViewHolderForAdapterPosition(0) != null
            }

            scenario.onActivity { activity ->
                requireNotNull(activity.showExtractionScopeDialog()).let { dialog ->
                    assertEquals(
                        listOf(
                            activity.getString(R.string.text_extraction_scope_entire_archive),
                            activity.getString(R.string.text_extraction_scope_current_directory, "/docs"),
                        ),
                        dialogLabels(dialog),
                    )
                    dialog.dismiss()
                }
                assertTrue(
                    activity.findViewById<android.view.View>(R.id.selectAllButton).performClick(),
                )
            }

            waitForActivity(scenario) { activity ->
                activity.findViewById<android.widget.TextView>(R.id.selectedCount).text.toString() ==
                    activity.getString(R.string.text_selected_count, 2)
            }

            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<android.view.View>(R.id.renameButton).isEnabled)
                assertTrue(activity.findViewById<android.view.View>(R.id.deleteButton).isEnabled)
                requireNotNull(activity.showExtractionScopeDialog()).let { dialog ->
                    assertEquals(
                        listOf(
                            activity.getString(R.string.text_extraction_scope_entire_archive),
                            activity.getString(R.string.text_extraction_scope_current_directory, "/docs"),
                            activity.getString(R.string.text_extraction_scope_current_selection),
                        ),
                        dialogLabels(dialog),
                    )
                    dialog.dismiss()
                }
            }
        }
    }

    @Test
    fun archiveInformationShowsWritableBoundaryAndMetadataEffects() {
        val archiveUri = createArchiveDocument()

        ActivityScenario.launch<ArchiveManagerActivity>(archiveIntent(archiveUri)).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                val toolbar =
                    activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(
                        R.id.toolbar,
                    )
                assertTrue(toolbar.menu.findItem(R.id.actionArchiveInformation).isEnabled)
                val dialog = requireNotNull(activity.showArchiveInformationDialog())
                val message = requireNotNull(
                    dialog.findViewById<android.widget.TextView>(android.R.id.message),
                ).text.toString()
                assertTrue(message.contains(ArchiveFormat.ZIP.displayName))
                assertTrue(message.contains(activity.getString(R.string.text_archive_management_available)))
                assertTrue(
                    message.contains(
                        activity.getString(
                            R.string.text_archive_metadata_archive_comment_dropped,
                        ),
                    ),
                )
                assertTrue(
                    message.contains(
                        activity.getString(
                            R.string.text_archive_metadata_unix_attributes_dropped,
                        ),
                    ),
                )
                assertTrue(message.contains(activity.getString(R.string.text_archive_information_safety)))
                dialog.dismiss()
            }
        }
    }

    @Test
    fun archiveInformationExplainsAReadOnlyOpenSession() {
        val archiveUri = createArchiveDocument()

        ActivityScenario.launch<ArchiveManagerActivity>(
            archiveIntent(archiveUri, ArchiveManagerPlugin.ACTION_OPEN_ID),
        ).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<android.view.View>(R.id.addFilesButton).isShown)
                val dialog = requireNotNull(activity.showArchiveInformationDialog())
                val message = requireNotNull(
                    dialog.findViewById<android.widget.TextView>(android.R.id.message),
                ).text.toString()
                assertTrue(
                    message.contains(
                        activity.getString(R.string.text_archive_read_only_current_session),
                    ),
                )
                dialog.dismiss()
            }
        }
    }

    @Test
    fun mutationPreflightShowsFactualWorkAndCompletesExactlyOnce() {
        val archiveUri = createArchiveDocument()
        var confirmations = 0
        var cancellations = 0
        lateinit var dialog: androidx.appcompat.app.AlertDialog

        ActivityScenario.launch<ArchiveManagerActivity>(archiveIntent(archiveUri)).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                dialog = activity.createArchiveMutationPreflightDialog(
                    prepared = testPreparedMutation(),
                    onConfirmed = { confirmations++ },
                    onCancelled = { cancellations++ },
                )
                dialog.show()
                val message = requireNotNull(
                    dialog.findViewById<android.widget.TextView>(android.R.id.message),
                ).text.toString()
                assertTrue(
                    message.contains(
                        activity.getString(R.string.text_archive_mutation_output_size_unknown),
                    ),
                )
                assertTrue(
                    message.contains(
                        activity.getString(
                            R.string.text_archive_metadata_extra_fields_normalized,
                        ),
                    ),
                )
                assertTrue(
                    dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick(),
                )
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals(1, confirmations)
                assertEquals(0, cancellations)
                dialog.dismiss()
                assertEquals(1, confirmations)
                assertEquals(0, cancellations)
            }
        }
    }

    @Test
    fun extractionDestinationDialogShowsBothChoicesWithItsMessage() {
        val archiveUri = createArchiveDocument()
        lateinit var dialog: androidx.appcompat.app.AlertDialog

        ActivityScenario.launch<ArchiveManagerActivity>(
            archiveIntent(
                archiveUri = archiveUri,
                actionId = ArchiveManagerPlugin.ACTION_EXTRACT_TO_ID,
            ),
        ).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                assertTrue(activity.canUseHostExtractionDestination())
                dialog = requireNotNull(activity.showExtractionDestinationDialog())
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val message = requireNotNull(
                    dialog.findViewById<android.widget.TextView>(R.id.destinationMessage),
                )
                val currentFolder = requireNotNull(
                    dialog.findViewById<android.widget.Button>(R.id.currentFolderButton),
                )
                val chooseFolder = requireNotNull(
                    dialog.findViewById<android.widget.Button>(R.id.chooseFolderButton),
                )
                assertTrue(message.isShown)
                assertTrue(currentFolder.isShown)
                assertTrue(chooseFolder.isShown)
                assertTrue(message.text.toString().contains("scope"))
                assertEquals(
                    activity.getString(
                        R.string.text_extraction_destination_current_folder,
                        "/Scope test",
                    ),
                    currentFolder.text.toString(),
                )
                assertEquals(
                    activity.getString(R.string.text_extraction_destination_choose_folder),
                    chooseFolder.text.toString(),
                )
                dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
            }
        }
    }

    @Test
    fun shortLandscapeKeepsEntriesAndAllCompactSettingsAvailable() {
        val archiveUri = createArchiveDocument()

        ActivityScenario.launch<ArchiveManagerActivity>(archiveIntent(archiveUri)).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            waitForActivity(scenario) { activity ->
                activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }

            var screenHeightDp = Int.MAX_VALUE
            scenario.onActivity { activity ->
                screenHeightDp = activity.resources.configuration.screenHeightDp
            }
            assumeTrue("Device landscape height is not compact", screenHeightDp < 600)

            waitForActivity(scenario) { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.entryList)
                activity.findViewById<android.view.View>(R.id.compactOptions).isShown &&
                    list.height > 0 &&
                    list.findViewHolderForAdapterPosition(0) != null
            }
            holdForExternalInspection()
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<android.view.View>(R.id.archiveName).isShown)
                assertFalse(
                    activity.findViewById<android.view.View>(R.id.extractionBudgetLayout).isShown,
                )
                assertFalse(
                    activity.findViewById<android.view.View>(
                        R.id.extractionConflictPolicyLayout,
                    ).isShown,
                )
                assertFalse(
                    activity.findViewById<android.view.View>(R.id.filenameEncodingLayout).isShown,
                )
                assertFalse(activity.findViewById<android.view.View>(R.id.upButton).isShown)
                assertTrue(activity.findViewById<android.view.View>(R.id.compactBudgetButton).isShown)
                assertTrue(
                    activity.findViewById<android.view.View>(R.id.compactConflictButton).isShown,
                )
                assertTrue(
                    activity.findViewById<android.view.View>(R.id.compactEncodingButton).isShown,
                )
                assertTrue(
                    activity.findViewById<android.view.View>(R.id.compactBudgetButton)
                        .hasOnClickListeners(),
                )
                assertTrue(
                    activity.findViewById<android.view.View>(R.id.compactConflictButton)
                        .hasOnClickListeners(),
                )
                assertTrue(
                    activity.findViewById<android.view.View>(R.id.compactEncodingButton)
                        .hasOnClickListeners(),
                )
                if (screenHeightDp < 400) {
                    assertFalse(activity.findViewById<android.view.View>(R.id.searchLayout).isShown)
                    assertFalse(activity.findViewById<android.view.View>(R.id.currentPath).isShown)
                    assertFalse(activity.findViewById<android.view.View>(R.id.selectedCount).isShown)
                    assertTrue(
                        activity.findViewById<android.view.View>(R.id.compactSearchButton).isShown,
                    )
                    assertTrue(
                        activity.findViewById<android.view.View>(R.id.compactSearchButton)
                            .hasOnClickListeners(),
                    )
                    val toolbar =
                        activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(
                            R.id.toolbar,
                        )
                    assertTrue(toolbar.subtitle == null)
                    assertTrue(
                        toolbar.title.toString().contains('/'),
                    )
                }

                activity.showCompactResourceBudgetDialog().let { dialog ->
                    assertEquals(
                        listOf(
                            activity.getString(R.string.text_budget_profile_compatible),
                            activity.getString(R.string.text_budget_profile_strict),
                            activity.getString(R.string.text_budget_profile_custom),
                        ),
                        dialogLabels(dialog),
                    )
                    dialog.dismiss()
                }
                activity.showCompactConflictPolicyDialog().let { dialog ->
                    assertEquals(
                        listOf(
                            activity.getString(R.string.text_conflict_policy_ask),
                            activity.getString(R.string.text_conflict_policy_skip),
                            activity.getString(R.string.text_conflict_policy_overwrite),
                            activity.getString(R.string.text_conflict_policy_auto_rename),
                        ),
                        dialogLabels(dialog),
                    )
                    dialog.dismiss()
                }
                requireNotNull(activity.showCompactFilenameEncodingDialog()).let { dialog ->
                    assertTrue(dialogLabels(dialog).isNotEmpty())
                    dialog.dismiss()
                }
            }
        }
    }

    @Test
    fun conflictPolicyDefaultsToAskAndSurvivesActivityRecreation() {
        val archiveUri = createArchiveDocument()

        ActivityScenario.launch<ArchiveManagerActivity>(archiveIntent(archiveUri)).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                assertEquals(
                    activity.getString(R.string.text_conflict_policy_ask),
                    activity.findViewById<android.widget.TextView>(
                        R.id.extractionConflictPolicyInput,
                    ).text.toString(),
                )
                activity.selectConflictPolicy(ArchiveExtractionConflictPolicy.AUTO_RENAME)
                assertEquals(
                    activity.getString(R.string.text_conflict_policy_auto_rename),
                    activity.findViewById<android.widget.TextView>(
                        R.id.extractionConflictPolicyInput,
                    ).text.toString(),
                )
            }

            scenario.recreate()
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.widget.TextView>(
                    R.id.extractionConflictPolicyInput,
                ).text.toString() == activity.getString(
                    R.string.text_conflict_policy_auto_rename,
                )
            }
        }
    }

    @Test
    fun askDialogDisablesUnsafeOverwriteAndReturnsApplyToAllChoice() {
        val archiveUri = createArchiveDocument()
        lateinit var dialog: androidx.appcompat.app.AlertDialog
        var resolution: ArchiveExtractionConflictResolution? = null
        var cancellations = 0

        ActivityScenario.launch<ArchiveManagerActivity>(archiveIntent(archiveUri)).use { scenario ->
            waitForActivity(scenario) { activity ->
                activity.findViewById<android.view.View>(R.id.extractButton).isEnabled
            }
            scenario.onActivity { activity ->
                dialog = activity.createExtractionConflictDialog(
                    conflict = ArchiveExtractionConflict(
                        archivePath = "Node",
                        requestedDisplayName = "Node",
                        existingDisplayName = "node",
                        incomingIsDirectory = false,
                        existingIsDirectory = true,
                        canOverwrite = false,
                    ),
                    onResolution = { resolution = it },
                    onCancelled = { cancellations++ },
                )
                dialog.show()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity {
                assertTrue(requireNotNull(dialog.findViewById<android.view.View>(R.id.conflictMessage)).isShown)
                assertFalse(
                    requireNotNull(
                        dialog.findViewById<android.widget.RadioButton>(R.id.conflictOverwrite),
                    ).isEnabled,
                )
                assertTrue(
                    requireNotNull(
                        dialog.findViewById<android.view.View>(R.id.overwriteUnavailable),
                    ).isShown,
                )
                requireNotNull(
                    dialog.findViewById<android.widget.RadioButton>(R.id.conflictSkip),
                ).performClick()
                requireNotNull(
                    dialog.findViewById<android.widget.CheckBox>(R.id.applyToAll),
                ).performClick()
                assertTrue(
                    requireNotNull(
                        dialog.findViewById<android.widget.RadioButton>(R.id.conflictSkip),
                    ).isChecked,
                )
                assertTrue(
                    requireNotNull(
                        dialog.findViewById<android.widget.CheckBox>(R.id.applyToAll),
                    ).isChecked,
                )
                dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()

                assertEquals(
                    ArchiveExtractionConflictResolution(
                        decision = ArchiveExtractionConflictDecision.SKIP,
                        applyToAll = true,
                    ),
                    resolution,
                )
                assertEquals(0, cancellations)
            }
        }
    }

    private fun dialogLabels(dialog: androidx.appcompat.app.AlertDialog): List<String> =
        (0 until dialog.listView.adapter.count).map { position ->
            dialog.listView.adapter.getItem(position).toString()
        }

    private fun holdForExternalInspection() {
        val requestedMillis = InstrumentationRegistry.getArguments()
            .getString(EXTERNAL_INSPECTION_HOLD_MILLIS_ARGUMENT)
            ?.toLongOrNull()
            ?: return
        SystemClock.sleep(requestedMillis.coerceIn(0L, MAX_EXTERNAL_INSPECTION_HOLD_MILLIS))
    }

    private fun createArchiveDocument(): Uri {
        val archiveUri = requireNotNull(
            DocumentsContract.createDocument(
                resolver,
                ROOT_DOCUMENT_URI,
                ZIP_MIME_TYPE,
                ARCHIVE_NAME,
            ),
        )
        resolver.openOutputStream(archiveUri, "w").use { rawOutput ->
            ZipOutputStream(requireNotNull(rawOutput)).use { zip ->
                zip.putNextEntry(ZipEntry("docs/readme.txt"))
                zip.write("scope test".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("root.txt"))
                zip.write("root".toByteArray())
                zip.closeEntry()
            }
        }
        return archiveUri
    }

    private fun testPreparedMutation(): PreparedArchiveMutation =
        object : PreparedArchiveMutation {
            override val format = ArchiveFormat.ZIP
            override val operation = ArchiveOperation.ADD
            override val sourceVersion = ArchiveMutationSourceVersion(
                format = ArchiveFormat.ZIP,
                sourceLength = 4_096L,
                sourceLastModifiedMillis = 1L,
                entries = emptyList(),
                volumeIdentities = emptyList(),
            )
            override val workEstimate = ArchiveMutationWorkEstimate(
                sourceArchiveBytes = 4_096L,
                resultEntryCount = 3,
                resultFileCount = 2,
                resultDirectoryCount = 1,
                knownContentBytesToRead = 8_192L,
                unknownContentFileCount = 1,
            )
            override val metadataEffects = setOf(
                ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED,
            )
        }

    private fun archiveIntent(
        archiveUri: Uri,
        actionId: String = ArchiveManagerPlugin.ACTION_MANAGE_ID,
    ): Intent {
        val target = Bundle().apply {
            putString(ExplorerActionTargetKeys.ID, "scope-target")
            putParcelable(ExplorerActionTargetKeys.URI, archiveUri)
            putString(ExplorerActionTargetKeys.DISPLAY_NAME, ARCHIVE_NAME)
            putInt(ExplorerActionTargetKeys.KIND, ExplorerActionValues.TARGET_FILE)
            putString(ExplorerActionTargetKeys.MIME_TYPE, ZIP_MIME_TYPE)
            putLong(ExplorerActionTargetKeys.SIZE, ArchiveIntentPolicy.SIZE_UNKNOWN)
            putLong(ExplorerActionTargetKeys.LAST_MODIFIED, 1L)
        }
        return Intent(ExplorerActionPluginActions.EXECUTE)
            .setClass(context, ArchiveManagerActivity::class.java)
            .setDataAndType(archiveUri, ZIP_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            .putExtra(
                ExplorerActionIntentExtras.ACTION_ID,
                actionId,
            )
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
            .putExtra(ExplorerActionIntentExtras.REQUEST_ID, REQUEST_ID)
            .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, ARCHIVE_NAME)
            .putExtra(ExplorerActionIntentExtras.SIZE, ArchiveIntentPolicy.SIZE_UNKNOWN)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, ROOT_DOCUMENT_URI)
            .putExtra(ExplorerActionIntentExtras.PARENT_DISPLAY_PATH, "/Scope test")
            .putExtra(
                ExplorerActionIntentExtras.SOURCE_SURFACE,
                ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
            )
            .putParcelableArrayListExtra(ExplorerActionIntentExtras.TARGETS, arrayListOf(target))
            .putExtra(
                ExplorerActionIntentExtras.HOST_SESSION,
                Bundle().apply {
                    putBinder(
                        ExplorerActionHostSessionKeys.BINDER,
                        UnusedTestExplorerActionHostSession().asBinder(),
                    )
                },
            )
            .apply { clipData = ClipData.newRawUri(ARCHIVE_NAME, archiveUri) }
    }

    private fun waitForActivity(
        scenario: ActivityScenario<ArchiveManagerActivity>,
        condition: (ArchiveManagerActivity) -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + ACTIVITY_TIMEOUT_MILLIS
        var lastState = "activity unavailable"
        do {
            var satisfied = false
            scenario.onActivity { activity ->
                satisfied = condition(activity)
                val list = activity.findViewById<RecyclerView>(R.id.entryList)
                lastState = buildString {
                    append("extractEnabled=")
                    append(activity.findViewById<android.view.View>(R.id.extractButton).isEnabled)
                    append(", adapterItems=")
                    append(list.adapter?.itemCount ?: -1)
                    append(", visibleChildren=")
                    append(list.childCount)
                    append(", size=")
                    append(list.width)
                    append('x')
                    append(list.height)
                    append(", shown=")
                    append(list.isShown)
                    append(", path=")
                    append(activity.findViewById<android.widget.TextView>(R.id.currentPath).text)
                }
            }
            if (satisfied) return
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("Archive manager did not reach the expected state: $lastState")
    }

    private fun resetProvider() {
        assertNotNull(
            resolver.call(
                PROVIDER_URI,
                CollisionDocumentsProvider.METHOD_RESET,
                null,
                null,
            ),
        )
    }

    private companion object {
        const val ACTIVITY_TIMEOUT_MILLIS = 10_000L
        const val ARCHIVE_NAME = "scope.zip"
        const val EXTERNAL_INSPECTION_HOLD_MILLIS_ARGUMENT =
            "archive_manager_hold_millis"
        const val MAX_EXTERNAL_INSPECTION_HOLD_MILLIS = 30_000L
        const val POLL_INTERVAL_MILLIS = 50L
        const val REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000"
        const val ZIP_MIME_TYPE = "application/zip"

        val PROVIDER_URI: Uri = Uri.parse(
            "content://${CollisionDocumentsProvider.AUTHORITY}",
        )
        val ROOT_DOCUMENT_URI: Uri = DocumentsContract.buildDocumentUri(
            CollisionDocumentsProvider.AUTHORITY,
            CollisionDocumentsProvider.ROOT_ID,
        )
    }
}
