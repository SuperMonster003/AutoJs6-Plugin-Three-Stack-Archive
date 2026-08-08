package io.github.supermonster003.autojs6.plugin.archivebrowser

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.explorer.api.IExplorerActionPlugin

class ExplorerActionService : Service() {

    private val binder = object : IExplorerActionPlugin.Stub() {
        override fun getInfo() = archiveBrowserPluginInfo()

        override fun getActionCatalog() = archiveBrowserActionCatalog()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
