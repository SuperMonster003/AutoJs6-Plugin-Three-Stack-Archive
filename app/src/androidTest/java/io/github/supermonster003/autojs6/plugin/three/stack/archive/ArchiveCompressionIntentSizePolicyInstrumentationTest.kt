package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.test.runner.AndroidJUnit4
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionIntentValues
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveCompressionIntentSizePolicyInstrumentationTest {

    @Test
    fun maximumCountTypicalRequestIsAcceptedButDeepPathRequestIsRejected() {
        val typical = compressionIntent(
            targetCount = ExplorerActionProtocol.MAX_TARGETS_PER_REQUEST,
            sharedUriPathLength = 16,
            displayNameLength = 32,
            parentDisplayPathLength = 64,
        )
        assertTrue(ExplorerActionIntentSizePolicy.isSafe(typical))
        val resolved = ArchiveCompressionIntentPolicy.resolve(typical)
        assertNotNull(resolved)
        assertEquals(ExplorerActionProtocol.MAX_TARGETS_PER_REQUEST, resolved?.targets?.size)

        val deepPath = compressionIntent(
            targetCount = ExplorerActionProtocol.MAX_TARGETS_PER_REQUEST,
            sharedUriPathLength = 2_048,
            displayNameLength = 2_048,
            parentDisplayPathLength = ExplorerActionProtocol.MAX_PARENT_DISPLAY_PATH_LENGTH,
        )
        assertFalse(ExplorerActionIntentSizePolicy.isSafe(deepPath))
        assertNull(ArchiveCompressionIntentPolicy.resolve(deepPath))
    }

    @Test
    fun unknownOversizedExtraCannotBypassTheEnvelopeLimit() {
        val request = compressionIntent(
            targetCount = 1,
            sharedUriPathLength = 16,
            displayNameLength = 32,
            parentDisplayPathLength = 64,
        ).putExtra("unrecognized.padding", "x".repeat(ExplorerActionIntentSizePolicy.MAX_REQUEST_BYTES))

        assertFalse(ExplorerActionIntentSizePolicy.isSafe(request))
        assertNull(ArchiveCompressionIntentPolicy.resolve(request))
    }

    private fun compressionIntent(
        targetCount: Int,
        sharedUriPathLength: Int,
        displayNameLength: Int,
        parentDisplayPathLength: Int,
    ): Intent {
        val parentUri = Uri.parse("content://org.autojs.test.fileprovider/root")
        val sharedPath = "p".repeat(sharedUriPathLength)
        val targets = ArrayList<Bundle>(targetCount)
        val uris = ArrayList<Uri>(targetCount)
        repeat(targetCount) { index ->
            val suffix = index.toString().padStart(3, '0')
            val uri = Uri.parse("content://org.autojs.test.fileprovider/root/$sharedPath/$suffix")
            uris += uri
            targets += Bundle().apply {
                putString(ExplorerActionTargetKeys.ID, "target-$suffix")
                putParcelable(ExplorerActionTargetKeys.URI, uri)
                putString(
                    ExplorerActionTargetKeys.DISPLAY_NAME,
                    "n".repeat(displayNameLength - suffix.length) + suffix,
                )
                putInt(ExplorerActionTargetKeys.KIND, ExplorerActionValues.TARGET_FILE)
                putString(ExplorerActionTargetKeys.MIME_TYPE, "application/octet-stream")
                putLong(ExplorerActionTargetKeys.SIZE, index.toLong())
                putLong(ExplorerActionTargetKeys.LAST_MODIFIED, index.toLong())
            }
        }
        val clipData = ClipData.newRawUri("Archive compression targets", uris.first()).apply {
            uris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
        }
        return Intent(ExplorerActionPluginActions.EXECUTE)
            .setDataAndType(uris.first(), "*/*")
            .apply { this.clipData = clipData }
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ThreeStackArchivePlugin.ACTION_COMPRESS_MULTIPLE_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ExplorerActionProtocol.VERSION)
            .putExtra(ExplorerActionIntentExtras.REQUEST_ID, REQUEST_ID)
            .putExtra(ExplorerActionIntentExtras.PARENT_URI, parentUri)
            .putExtra(
                ExplorerActionIntentExtras.PARENT_DISPLAY_PATH,
                "/" + "d".repeat(parentDisplayPathLength - 1),
            )
            .putExtra(
                ExplorerActionIntentExtras.SOURCE_SURFACE,
                ExplorerActionIntentValues.SOURCE_SURFACE_MAIN,
            )
            .putExtra(
                ExplorerActionIntentExtras.HOST_SESSION,
                Bundle().apply {
                    putBinder(
                        ExplorerActionHostSessionKeys.BINDER,
                        UnusedTestExplorerActionHostSession().asBinder(),
                    )
                },
            )
            .putParcelableArrayListExtra(ExplorerActionIntentExtras.TARGETS, targets)
    }

    private companion object {
        const val REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000"
    }
}
