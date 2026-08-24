package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ClipData
import android.content.Intent
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
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

            scenario.onActivity { activity ->
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

    private fun dialogLabels(dialog: androidx.appcompat.app.AlertDialog): List<String> =
        (0 until dialog.listView.adapter.count).map { position ->
            dialog.listView.adapter.getItem(position).toString()
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

    private fun archiveIntent(archiveUri: Uri): Intent {
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
                ArchiveManagerPlugin.ACTION_SELECTIVE_EXTRACT_ID,
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
            .apply { clipData = ClipData.newRawUri(ARCHIVE_NAME, archiveUri) }
    }

    private fun waitForActivity(
        scenario: ActivityScenario<ArchiveManagerActivity>,
        condition: (ArchiveManagerActivity) -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + ACTIVITY_TIMEOUT_MILLIS
        do {
            var satisfied = false
            scenario.onActivity { activity -> satisfied = condition(activity) }
            if (satisfied) return
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("Archive manager did not reach the expected state")
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
