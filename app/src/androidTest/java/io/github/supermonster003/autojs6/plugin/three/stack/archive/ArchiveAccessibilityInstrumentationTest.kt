package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class ArchiveAccessibilityInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext

    @Test
    fun formsKeepNavigationLabelsAndTouchTargetsAcrossCompactTabletLandscapeAndRtl() {
        val specifications = listOf(
            LayoutSpecification(320, 568, 1.5F, Locale.ENGLISH),
            LayoutSpecification(360, 640, 1.5F, Locale.SIMPLIFIED_CHINESE),
            LayoutSpecification(600, 960, 1.5F, Locale.ENGLISH),
            LayoutSpecification(568, 320, 1.5F, Locale.ENGLISH),
            LayoutSpecification(640, 360, 1.5F, Locale("ar")),
        )

        specifications.forEach { specification ->
            onMain {
                val context = configuredContext(specification)
                listOf(R.layout.activity_create_archive, R.layout.activity_archive_manager).forEach { layout ->
                    val root = LayoutInflater.from(context).inflate(layout, null, false)
                    if (layout == R.layout.activity_archive_manager) {
                        applyManagerPresentation(root, specification)
                    }
                    measureAndLayout(root, specification.widthDp, specification.heightDp, context)
                    val toolbar = root.findViewById<MaterialToolbar>(R.id.toolbar)
                    assertEquals(context.getString(R.string.text_navigate_up), toolbar.navigationContentDescription)
                    visibleClickableDescendants(root).forEach { target ->
                        assertTouchTarget(context, target)
                    }
                }
            }
        }
    }

    @Test
    fun archiveRowsExposeOneCheckableFileActionAndSeparateFolderOpenAndSelectActions() = onMain {
        val context = configuredContext(LayoutSpecification(320, 568, 1.5F, Locale.ENGLISH))
        val parent = FrameLayout(context)
        val adapter = ArchiveEntryAdapter(
            onOpenDirectory = {},
            onSelectionChanged = { _, _ -> },
        )
        val holder = adapter.onCreateViewHolder(parent, 0)

        holder.bind(
            ArchiveEntryRow(
                path = "report.txt",
                displayName = "report.txt",
                details = "12 KiB",
                isDirectory = false,
                isBlocked = false,
                isSelected = true,
            ),
        )
        measureAndLayout(holder.itemView, 320, 80, context)
        val fileNode = AccessibilityNodeInfoCompat.obtain()
        holder.itemView.onInitializeAccessibilityNodeInfo(fileNode.unwrap())
        assertEquals(CheckBox::class.java.name, fileNode.className)
        assertTrue(fileNode.isCheckable)
        assertTrue(fileNode.isChecked)
        assertTrue(holder.itemView.contentDescription.contains("report.txt"))
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO,
            holder.itemView.findViewById<View>(R.id.selected).importantForAccessibility,
        )
        fileNode.recycle()

        holder.bind(
            ArchiveEntryRow(
                path = "photos/",
                displayName = "photos",
                details = "3 files",
                isDirectory = true,
                isBlocked = false,
                isSelected = false,
            ),
        )
        measureAndLayout(holder.itemView, 320, 80, context)
        val directoryNode = AccessibilityNodeInfoCompat.obtain()
        holder.itemView.onInitializeAccessibilityNodeInfo(directoryNode.unwrap())
        assertEquals(Button::class.java.name, directoryNode.className)
        val selectDirectory = holder.itemView.findViewById<View>(R.id.selected)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, selectDirectory.importantForAccessibility)
        assertTrue(selectDirectory.contentDescription.contains("photos"))
        assertTouchTarget(context, selectDirectory)
        directoryNode.recycle()
    }

    private fun visibleClickableDescendants(root: View): List<View> {
        val result = ArrayList<View>()
        fun visit(view: View) {
            if (view.visibility != View.VISIBLE) return
            if (view.isClickable) result += view
            if (view is ViewGroup) {
                repeat(view.childCount) { index -> visit(view.getChildAt(index)) }
            }
        }
        visit(root)
        return result
    }

    private fun applyManagerPresentation(root: View, specification: LayoutSpecification) {
        val compact = specification.heightDp < 600
        val ultraCompact = specification.heightDp < 400
        root.findViewById<View>(R.id.archiveName).visibility = visibleUnless(compact)
        root.findViewById<View>(R.id.archiveSummary).visibility = visibleUnless(compact)
        root.findViewById<View>(R.id.compactOptions).visibility = visibleIf(compact)
        root.findViewById<View>(R.id.extractionBudgetLayout).visibility = visibleUnless(compact)
        root.findViewById<View>(R.id.extractionConflictPolicyLayout).visibility = visibleUnless(compact)
        root.findViewById<View>(R.id.currentPath).visibility = visibleUnless(ultraCompact)
        root.findViewById<View>(R.id.selectedCount).visibility = visibleUnless(ultraCompact)
        root.findViewById<View>(R.id.upButton).visibility = visibleUnless(compact)
        root.findViewById<View>(R.id.searchLayout).visibility = visibleUnless(ultraCompact)
        root.findViewById<View>(R.id.compactSearchButton).visibility = visibleIf(ultraCompact)
    }

    private fun visibleIf(condition: Boolean): Int = if (condition) View.VISIBLE else View.GONE

    private fun visibleUnless(condition: Boolean): Int = visibleIf(!condition)

    private fun assertTouchTarget(context: Context, target: View) {
        val minimum = dp(context, 48)
        assertTrue("${resourceName(target.id)} width was ${target.width}px", target.width >= minimum)
        assertTrue("${resourceName(target.id)} height was ${target.height}px", target.height >= minimum)
    }

    private fun configuredContext(specification: LayoutSpecification): Context {
        val configuration = Configuration(targetContext.resources.configuration).apply {
            fontScale = specification.fontScale
            screenWidthDp = specification.widthDp
            screenHeightDp = specification.heightDp
            smallestScreenWidthDp = minOf(specification.widthDp, specification.heightDp)
            orientation = if (specification.widthDp > specification.heightDp) {
                Configuration.ORIENTATION_LANDSCAPE
            } else {
                Configuration.ORIENTATION_PORTRAIT
            }
            setLocale(specification.locale)
            setLayoutDirection(specification.locale)
        }
        return ContextThemeWrapper(
            targetContext.createConfigurationContext(configuration),
            R.style.AppTheme,
        )
    }

    private fun measureAndLayout(
        view: View,
        widthDp: Int,
        heightDp: Int,
        context: Context,
    ) {
        val width = dp(context, widthDp)
        val height = dp(context, heightDp)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun resourceName(id: Int): String = runCatching {
        targetContext.resources.getResourceEntryName(id)
    }.getOrDefault(id.toString())

    private fun <T> onMain(block: () -> T): T {
        val outcome = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { outcome.set(runCatching(block)) }
        return requireNotNull(outcome.get()).getOrThrow()
    }

    private data class LayoutSpecification(
        val widthDp: Int,
        val heightDp: Int,
        val fontScale: Float,
        val locale: Locale,
    )
}
