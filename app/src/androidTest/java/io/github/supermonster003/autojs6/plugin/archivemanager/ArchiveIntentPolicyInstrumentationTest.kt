@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveIntentPolicyInstrumentationTest {

    private val parentUri = Uri.parse("content://org.autojs.test.fileprovider/root/archives")
    private val archiveUri = Uri.parse("content://org.autojs.test.fileprovider/root/archives/bundle.zip")

    @Test
    fun completeV4ExplorerContractIsAccepted() {
        val resolved = ArchiveIntentPolicy.resolve(validIntent())

        assertNotNull(resolved)
        assertEquals(archiveUri, resolved?.archiveUri)
        assertEquals(parentUri, resolved?.parentUri)
        assertEquals("/storage/emulated/0/Archives", resolved?.parentDisplayPath)
        assertEquals(REQUEST_ID, resolved?.requestId)
        assertEquals("bundle.zip", resolved?.displayName)
        assertEquals(4096L, resolved?.reportedSize)
        assertEquals(ArchiveRequestedAction.OPEN, resolved?.requestedAction)
    }

    @Test
    fun extractToShortcutUsesTheSameValidatedContract() {
        val resolved = ArchiveIntentPolicy.resolve(
            Intent(validIntent()).putExtra(
                ExplorerActionIntentExtras.ACTION_ID,
                ArchiveManagerPlugin.ACTION_EXTRACT_TO_ID,
            ),
        )

        assertNotNull(resolved)
        assertEquals(ArchiveRequestedAction.EXTRACT_TO, resolved?.requestedAction)
    }

    @Test
    fun selectiveExtractionUsesTheManagementPageWithoutStartingTheShortcut() {
        val resolved = ArchiveIntentPolicy.resolve(
            Intent(validIntent()).putExtra(
                ExplorerActionIntentExtras.ACTION_ID,
                ArchiveManagerPlugin.ACTION_SELECTIVE_EXTRACT_ID,
            ),
        )

        assertNotNull(resolved)
        assertEquals(ArchiveRequestedAction.SELECTIVE_EXTRACT, resolved?.requestedAction)
    }

    @Test
    fun actionIdProtocolSurfaceAndRequestIdAreMandatory() {
        assertNull(ArchiveIntentPolicy.resolve(Intent(validIntent()).setAction(Intent.ACTION_VIEW)))
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.ACTION_ID, "extract-archive"),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, 3),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    removeExtra(ExplorerActionIntentExtras.SOURCE_SURFACE)
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.REQUEST_ID, "not-a-uuid"),
            ),
        )
    }

    @Test
    fun targetMustBeAContentUriWithReadGrant() {
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).setData(Uri.parse("file:///sdcard/bundle.zip")),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    flags = Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                },
            ),
        )
        assertNotNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            ),
        )
    }

    @Test
    fun orderedClipItemsAndTargetBundlesMustMatchExactly() {
        val otherArchive = Uri.parse("content://org.autojs.test.fileprovider/root/archives/other.zip")

        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    clipData = clipData(otherArchive)
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    clipData = clipData(archiveUri).apply {
                        addItem(ClipData.Item(otherArchive))
                    }
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(uri = otherArchive)),
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(), targetBundle(uri = otherArchive)),
                ),
            ),
        )
    }

    @Test
    fun parentIsUngrantableContextFromTheSameProvider() {
        val siblingParent = Uri.parse("content://org.autojs.test.fileprovider/root/other")
        val otherAuthorityParent = Uri.parse("content://org.example.fileprovider/root/archives")

        assertNotNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.PARENT_URI, siblingParent),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(
                    ExplorerActionIntentExtras.PARENT_URI,
                    otherAuthorityParent,
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(
                    ExplorerActionIntentExtras.PARENT_DISPLAY_PATH,
                    "bad\u0000path",
                ),
            ),
        )
    }

    @Test
    fun targetMetadataMustMatchLegacySingleTargetFields() {
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(
                    ExplorerActionIntentExtras.DISPLAY_NAME,
                    "folder/bundle.zip",
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(displayName = "other.zip")),
                ),
            ),
        )
        assertNotNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent())
                    .putExtra(ExplorerActionIntentExtras.SIZE, ArchiveIntentPolicy.SIZE_UNKNOWN)
                    .putParcelableArrayListExtra(
                        ExplorerActionIntentExtras.TARGETS,
                        arrayListOf(targetBundle(size = ArchiveIntentPolicy.SIZE_UNKNOWN)),
                    ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply { removeExtra(ExplorerActionIntentExtras.SIZE) },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent())
                    .putExtra(ExplorerActionIntentExtras.SIZE, ArchiveIntentPolicy.SIZE_UNKNOWN - 1L)
                    .putParcelableArrayListExtra(
                        ExplorerActionIntentExtras.TARGETS,
                        arrayListOf(targetBundle(size = ArchiveIntentPolicy.SIZE_UNKNOWN - 1L)),
                    ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent())
                    .setDataAndType(archiveUri, "text/plain")
                    .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, "archive.rar")
                    .putParcelableArrayListExtra(
                        ExplorerActionIntentExtras.TARGETS,
                        arrayListOf(targetBundle(displayName = "archive.rar", mimeType = "text/plain")),
                    ),
            ),
        )
    }

    @Test
    fun targetKindIdMimeAndTimestampsAreValidated() {
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(kind = ExplorerActionValues.TARGET_DIRECTORY)),
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(id = "bad id")),
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(mimeType = "text/plain")),
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putParcelableArrayListExtra(
                    ExplorerActionIntentExtras.TARGETS,
                    arrayListOf(targetBundle(lastModified = -2L)),
                ),
            ),
        )
    }

    private fun validIntent(): Intent =
        Intent(ExplorerActionPluginActions.EXECUTE)
            .setDataAndType(archiveUri, "application/zip")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ArchiveManagerPlugin.ACTION_OPEN_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
            .putExtra(ExplorerActionIntentExtras.REQUEST_ID, REQUEST_ID)
            .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, "bundle.zip")
            .putExtra(ExplorerActionIntentExtras.SIZE, 4096L)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(ExplorerActionIntentExtras.PARENT_DISPLAY_PATH, "/storage/emulated/0/Archives")
            .putExtra(
                ExplorerActionIntentExtras.SOURCE_SURFACE,
                ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
            )
            .putParcelableArrayListExtra(
                ExplorerActionIntentExtras.TARGETS,
                arrayListOf(targetBundle()),
            )
            .apply { clipData = clipData(archiveUri) }

    private fun targetBundle(
        uri: Uri = archiveUri,
        id: String = "target-1",
        displayName: String = "bundle.zip",
        kind: Int = ExplorerActionValues.TARGET_FILE,
        mimeType: String = "application/zip",
        size: Long = 4096L,
        lastModified: Long = 1234L,
    ) = Bundle().apply {
        putString(ExplorerActionTargetKeys.ID, id)
        putParcelable(ExplorerActionTargetKeys.URI, uri)
        putString(ExplorerActionTargetKeys.DISPLAY_NAME, displayName)
        putInt(ExplorerActionTargetKeys.KIND, kind)
        putString(ExplorerActionTargetKeys.MIME_TYPE, mimeType)
        putLong(ExplorerActionTargetKeys.SIZE, size)
        putLong(ExplorerActionTargetKeys.LAST_MODIFIED, lastModified)
    }

    private fun clipData(target: Uri): ClipData =
        ClipData(
            ClipDescription("Archive target", arrayOf("application/zip")),
            ClipData.Item(target),
        )

    private companion object {
        const val REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000"
    }
}
