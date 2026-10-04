package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StandaloneHomeInstrumentationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun launcherAndPrivateSettingsHaveExpectedComponents() {
        val manager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val matches = manager.queryIntentActivities(launcher, 0)
        assertEquals(1, matches.size)
        assertEquals(context.packageName + ".HomeActivity", matches.single().activityInfo.targetActivity)
        for (name in listOf("HomeActivity", "AppSettingsActivity", "AboutActivity", "ReleaseHistoryActivity")) {
            assertFalse(name, manager.getActivityInfo(ComponentName(context.packageName, context.packageName + "." + name), 0).exported)
        }
    }

    @Test fun homeShowsItsOwnIdentityAndSettingsCanCancelAppearanceChanges() {
        ActivityScenario.launch<HomeActivity>(Intent(context, HomeActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val texts = labels(activity.window.decorView)
                assertTrue(texts.contains(activity.getString(R.string.app_name)))
                assertTrue(texts.contains(activity.getString(R.string.standalone_settings_title)))
            }
        }
        val store = ApplicationSettingsStore(context)
        val original = store.load()
        try {
            ActivityScenario.launch<AppSettingsActivity>(Intent(context, AppSettingsActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    val texts = labels(activity.window.decorView)
                    for (resource in listOf(R.string.standalone_settings_language, R.string.standalone_settings_dark_mode,
                        R.string.standalone_settings_theme_color, R.string.launcher_icon_title, R.string.standalone_release_history_title)) {
                        assertTrue(activity.getString(resource), texts.contains(activity.getString(resource)))
                    }
                    var row: View? = findLabel(activity.window.decorView, activity.getString(R.string.standalone_settings_dark_mode))
                    while (row != null && !row.isClickable) row = row.parent as? View
                    assertNotNull(row)
                    row!!.performClick()
                    val dialog = requireNotNull(activity.presentedDialog)
                    val choices = dialog.listView
                    choices.performItemClick(null, 3, 3L)
                    dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
                    assertEquals(original, store.load())
                }
            }
        } finally { store.save(original) }
    }

    private fun labels(view: View): List<String> = buildList {
        if (view is TextView) add(view.text.toString())
        if (view is ViewGroup) for (i in 0 until view.childCount) addAll(labels(view.getChildAt(i)))
    }
    private fun findLabel(view: View, label: String): View? {
        if (view is TextView && view.text.toString() == label) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findLabel(view.getChildAt(i), label)?.let { return it }
        return null
    }
}
