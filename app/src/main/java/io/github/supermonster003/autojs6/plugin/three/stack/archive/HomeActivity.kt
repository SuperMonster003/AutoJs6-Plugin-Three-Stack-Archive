package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.sectionHeader
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.settingRow

class HomeActivity : ConfiguredActivity() {
    private lateinit var actions: HomeActions
    private val actionViews = mutableListOf<View>()
    private lateinit var status: TextView
    internal lateinit var pendingRow: View
    private var working = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actions = HomeActions(this)
        actions.restore(savedInstanceState)
        val scaffold = buildScaffold(R.string.app_name, showBack = false)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(uiDp(24), uiDp(24), uiDp(24), uiDp(16))
        }
        hero.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(uiDp(96), uiDp(96)))
        hero.addView(TextView(this).apply {
            setText(R.string.plugin_description)
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(appPalette.primaryText)
            setPadding(0, uiDp(12), 0, uiDp(8))
        })
        hero.addView(TextView(this).apply {
            setText(R.string.standalone_home_scope)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(appPalette.secondaryText)
        })
        scaffold.content.addView(hero)
        scaffold.content.addView(sectionHeader(R.string.standalone_home_actions))
        actions.populate(scaffold.content)
        scaffold.content.addView(sectionHeader(R.string.standalone_settings_section_information))
        addHomeAction(scaffold.content, R.string.standalone_settings_title,
            R.string.standalone_home_settings_summary, R.drawable.ic_settings_theme) {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
        addHomeAction(scaffold.content, R.string.standalone_home_open_host,
            R.string.standalone_home_host_summary, R.drawable.ic_open_in_new_24) {
            val intent = packageManager.getLaunchIntentForPackage("org.autojs.autojs6")
            if (intent == null) showMessage(R.string.standalone_home_host_missing)
            else runCatching { startActivity(intent) }.onFailure { showMessage(R.string.standalone_home_open_failed) }
        }
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(appPalette.secondaryText)
            setPadding(uiDp(24), uiDp(12), uiDp(24), uiDp(24))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        scaffold.content.addView(status)
        setContentView(scaffold.root)
        applyThemeToControls(scaffold.root)
        actions.refresh()
    }

    internal fun addHomeAction(content: LinearLayout, title: Int, summary: Int?, icon: Int, click: () -> Unit): View {
        val row = settingRow(getString(title), summary?.let(::getString).orEmpty(), iconResource = icon, onClick = click).view
        content.addView(row)
        actionViews.add(row)
        return row
    }

    internal fun runWork(block: suspend () -> Unit) {
        if (working) return
        working = true
        actionViews.forEach { it.isEnabled = false; it.alpha = .5f }
        status.setText(R.string.standalone_home_working)
        lifecycleScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { showMessage(R.string.standalone_home_open_failed) }
            finally {
                working = false
                actionViews.forEach { it.isEnabled = true; it.alpha = 1f }
                status.text = ""
                actions.refresh()
            }
        }
    }

    internal fun showMessage(resource: Int) = Toast.makeText(this, resource, Toast.LENGTH_LONG).show()

    override fun onSaveInstanceState(outState: Bundle) {
        actions.save(outState)
        super.onSaveInstanceState(outState)
    }
}
