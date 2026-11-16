package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider

/** Common metadata entry required for the host's read-only appearance settings contract. */
class PluginInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo() = archiveManagerPluginInfo()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
