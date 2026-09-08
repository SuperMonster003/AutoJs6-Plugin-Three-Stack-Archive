package io.github.supermonster003.autojs6.plugin.archivemanager

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Optional artifacts from the same Activities exercised by the device regression suite. */
internal object ArchiveUiScreenshots {
    fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("archiveUiScreenshots") != "true") return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // RecyclerView's first-insert animation can outlive the main queue becoming idle.
        android.os.SystemClock.sleep(400)
        instrumentation.waitForIdleSync()
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-screenshots")
            check(directory.isDirectory || directory.mkdirs())
            File(directory, "$name.png").outputStream().use {
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            screenshot.recycle()
        }
    }
}
