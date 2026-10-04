package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class StandaloneArchiveInstrumentationTest {
    @Test fun aPickedArchiveOpensWithoutAnAutoJs6Session() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val uri = Uri.parse("content://" + instrumentation.context.packageName + ".fixture/sample.zip")
        val document = StandaloneDocuments.inspect(context, uri)
        val intent = Intent(context, StandaloneArchiveActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, document.mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(StandaloneArchiveActivity.EXTRA_NAME, document.name)
            putExtra(StandaloneArchiveActivity.EXTRA_SIZE, document.size)
        }
        ActivityScenario.launch<StandaloneArchiveActivity>(intent).use { scenario ->
            var visible = false
            val deadline = android.os.SystemClock.elapsedRealtime() + 15000
            while (!visible && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    assertFalse(activity.isFinishing)
                    visible = contains(activity.window.decorView, "hello.txt")
                }
                if (!visible) Thread.sleep(100)
            }
            assertTrue("The existing archive browser must load the picked ZIP",visible)
        }
    }
    private fun contains(view: View, value: String): Boolean {
        if (view is TextView && view.text.toString() == value) return true
        return view is ViewGroup && (0 until view.childCount).any { contains(view.getChildAt(it),value) }
    }
}
