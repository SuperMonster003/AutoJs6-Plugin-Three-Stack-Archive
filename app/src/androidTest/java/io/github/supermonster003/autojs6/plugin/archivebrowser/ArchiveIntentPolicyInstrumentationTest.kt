@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivebrowser

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
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
    fun completeReadOnlyExplorerContractIsAccepted() {
        val resolved = ArchiveIntentPolicy.resolve(validIntent())

        assertNotNull(resolved)
        assertEquals(archiveUri, resolved?.archiveUri)
        assertEquals(parentUri, resolved?.parentUri)
        assertEquals("bundle.zip", resolved?.displayName)
        assertEquals(4096L, resolved?.declaredSize)
    }

    @Test
    fun actionIdProtocolAndSourceSurfaceAreMandatory() {
        assertNull(ArchiveIntentPolicy.resolve(Intent(validIntent()).setAction(Intent.ACTION_VIEW)))
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.ACTION_ID, "extract-archive"),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, 2),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    removeExtra(ExplorerActionIntentExtras.SOURCE_SURFACE)
                },
            ),
        )
    }

    @Test
    fun targetMustBeAPlainContentUriWithReadOnlyGrant() {
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).setData(Uri.parse("file:///sdcard/bundle.zip")),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).setData(
                    Uri.parse("content://org.autojs.test.fileprovider/root/archives/bundle.zip?revision=1"),
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    flags = Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            ),
        )
    }

    @Test
    fun clipItemsMustExactlyMatchDataAndParentExtra() {
        val otherArchive = Uri.parse("content://org.autojs.test.fileprovider/root/archives/other.zip")
        val otherParent = Uri.parse("content://org.autojs.test.fileprovider/root/other")

        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    clipData = clipData(otherArchive, parentUri)
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    clipData = clipData(archiveUri, otherParent)
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    clipData = clipData(archiveUri, parentUri).apply {
                        addItem(ClipData.Item(otherArchive))
                    }
                },
            ),
        )
    }

    @Test
    fun parentMustMatchClipAndBeAnAncestorOfTheTarget() {
        val siblingParent = Uri.parse("content://org.autojs.test.fileprovider/root/other")
        val otherAuthorityParent = Uri.parse("content://org.example.fileprovider/root/archives")

        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent())
                    .putExtra(ExplorerActionIntentExtras.PARENT_URI, siblingParent)
                    .apply { clipData = clipData(archiveUri, siblingParent) },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent())
                    .putExtra(ExplorerActionIntentExtras.PARENT_URI, otherAuthorityParent)
                    .apply { clipData = clipData(archiveUri, otherAuthorityParent) },
            ),
        )
    }

    @Test
    fun displayNameSizeAndArchiveTypeBoundariesAreMandatory() {
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, "folder/bundle.zip"),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).apply {
                    removeExtra(ExplorerActionIntentExtras.SIZE)
                },
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(ExplorerActionIntentExtras.SIZE, -1L),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent()).putExtra(
                    ExplorerActionIntentExtras.SIZE,
                    ArchiveCacheStager.MAX_ARCHIVE_BYTES + 1L,
                ),
            ),
        )
        assertNull(
            ArchiveIntentPolicy.resolve(
                Intent(validIntent())
                    .setDataAndType(archiveUri, "text/plain")
                    .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, "archive.rar"),
            ),
        )
    }

    private fun validIntent(): Intent =
        Intent(ExplorerActionPluginActions.EXECUTE)
            .setDataAndType(archiveUri, "application/zip")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ArchiveBrowserPlugin.ACTION_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
            .putExtra(ExplorerActionIntentExtras.DISPLAY_NAME, "bundle.zip")
            .putExtra(ExplorerActionIntentExtras.SIZE, 4096L)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(
                ExplorerActionIntentExtras.SOURCE_SURFACE,
                ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
            )
            .apply { clipData = clipData(archiveUri, parentUri) }

    private fun clipData(target: Uri, parent: Uri): ClipData =
        ClipData(
            ClipDescription("Archive target", arrayOf("application/zip")),
            ClipData.Item(target),
        ).apply {
            addItem(ClipData.Item(parent))
        }
}
