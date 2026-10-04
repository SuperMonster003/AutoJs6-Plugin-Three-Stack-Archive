package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.settingRow

internal class AboutActivity : ConfiguredActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scaffold = buildScaffold(R.string.standalone_about_title, contentPadding = ContentPadding.SCREEN)
        scaffold.content.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = getString(R.string.app_name)
        }, LinearLayout.LayoutParams(uiDp(96), uiDp(96)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        scaffold.content.addView(TextView(this).apply {
            text = getString(R.string.app_name) + "\n" + version + "\n\n" + getString(R.string.plugin_description)
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(appPalette.primaryText)
            setPadding(0, uiDp(16), 0, uiDp(24))
        })
        scaffold.content.addView(settingRow(
            title = getString(R.string.standalone_home_sources),
            summary = getString(R.string.standalone_home_sources_summary),
            iconResource = R.drawable.ic_open_in_new_24,
            onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive"))) },
        ).view)
        setContentView(scaffold.root)
    }
}
