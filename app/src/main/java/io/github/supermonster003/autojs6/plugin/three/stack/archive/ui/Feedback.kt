package io.github.supermonster003.autojs6.plugin.three.stack.archive.ui

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.snackbar.Snackbar
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ConfiguredActivity

/** Centered placeholder for empty or failed content areas. */
internal fun ConfiguredActivity.emptyStateView(
    title: CharSequence,
    description: CharSequence? = null,
    @DrawableRes iconResource: Int? = null,
    action: View? = null,
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    setPaddingRelative(
        uiDp(Ui.SPACE_XXL),
        uiDp(Ui.SPACE_XXL),
        uiDp(Ui.SPACE_XXL),
        uiDp(Ui.SPACE_XXL),
    )
    if (iconResource != null) {
        addView(
            FrameLayout(context).apply {
                background = roundedFill(appPalette.surfaceVariant, Ui.RADIUS_SHEET * 2)
                addView(
                    ImageView(context).apply {
                        setImageDrawable(tintedDrawable(iconResource, appPalette.secondaryText))
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    FrameLayout.LayoutParams(uiDp(28), uiDp(28), Gravity.CENTER),
                )
            },
            LinearLayout.LayoutParams(uiDp(64), uiDp(64)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
    }
    addView(
        TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_TITLE)
            typeface = Ui.mediumTypeface
            gravity = Gravity.CENTER
            setTextColor(appPalette.primaryText)
            setPaddingRelative(0, uiDp(Ui.SPACE_LG), 0, uiDp(Ui.SPACE_SM))
        },
    )
    if (!description.isNullOrEmpty()) {
        addView(
            TextView(context).apply {
                text = description
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                setTextColor(appPalette.secondaryText)
                gravity = Gravity.CENTER
                setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            },
        )
    }
    if (action != null) {
        addView(
            action,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = uiDp(Ui.SPACE_XL)
            },
        )
    }
}

/** High-contrast snackbar consistent with the app palette in both modes. */
internal fun ConfiguredActivity.showSnackbar(
    anchor: View,
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_SHORT,
) {
    Snackbar.make(anchor, message, duration).apply {
        setBackgroundTint(appPalette.primaryText)
        setTextColor(appPalette.windowBackground)
    }.show()
}
