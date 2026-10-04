package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class HomeActions(private val activity: HomeActivity) {
    private val openDocument = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) activity.runWork {
            val document = withContext(Dispatchers.IO) { StandaloneDocuments.inspect(activity, uri) }
            activity.startActivity(Intent(activity, StandaloneArchiveActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, document.mimeType)
                clipData = ClipData.newRawUri("archive", uri)
                putExtra(StandaloneArchiveActivity.EXTRA_NAME, document.name)
                putExtra(StandaloneArchiveActivity.EXTRA_SIZE, document.size)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        }
    }
    fun populate(content: LinearLayout) {
        activity.addHomeAction(content, R.string.standalone_home_open_archive, null, R.drawable.ic_folder_open_24) {
            openDocument.launch(arrayOf("*/*"))
        }
        activity.pendingRow = View(activity)
    }
    fun refresh() = Unit
    fun save(state: Bundle) = Unit
    fun restore(state: Bundle?) = Unit
}
