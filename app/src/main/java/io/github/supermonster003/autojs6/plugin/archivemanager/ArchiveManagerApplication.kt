package io.github.supermonster003.autojs6.plugin.archivemanager

import android.app.Application
import com.google.android.material.color.DynamicColors

class ArchiveManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
