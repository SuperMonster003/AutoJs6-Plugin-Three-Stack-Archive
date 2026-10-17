package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.Ui
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.accentTone
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.cardContainer
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.cardListParams
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.emptyStateView
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ui.roundedFill
import java.util.Locale

/** Maps the UI locale to the bundled changelog asset shipped for it. */
internal object ReleaseHistoryAssetPolicy {
    fun assetFor(locale: Locale): String = when (locale.language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            locale.script.equals("Hans", ignoreCase = true) -> "CHANGELOG-zh-Hans.md"
            locale.country.equals("HK", ignoreCase = true) ||
                locale.country.equals("MO", ignoreCase = true) -> "CHANGELOG-zh-Hant-HK.md"
            locale.script.equals("Hant", ignoreCase = true) ||
                locale.country.equals("TW", ignoreCase = true) -> "CHANGELOG-zh-Hant-TW.md"
            else -> "CHANGELOG-zh-Hans.md"
        }
        "ar" -> "CHANGELOG-ar.md"
        "es" -> "CHANGELOG-es.md"
        "fr" -> "CHANGELOG-fr.md"
        "ja" -> "CHANGELOG-ja.md"
        "ko" -> "CHANGELOG-ko.md"
        "ru" -> "CHANGELOG-ru.md"
        else -> "CHANGELOG-en.md"
    }
}

internal data class ReleaseHistoryItem(val tag: String?, val text: String)

internal data class ReleaseHistoryEntry(
    val version: String,
    val date: String?,
    val items: List<ReleaseHistoryItem>,
)

/**
 * Parses the bundled changelogs written by `.python/generate_markdown.py`: a `# title` line, then
 * per version a `## vX.Y.Z` heading, an `_date_` line and `` - `tag` text `` bullets. Inline code
 * markers inside the text are dropped. Anything else is ignored.
 */
internal object ReleaseHistoryParser {
    private val VERSION_PATTERN = Regex("""^##\s+(v?\d\S*)$""")
    private val DATE_PATTERN = Regex("""^_(.+)_$""")
    private val BULLET_PATTERN = Regex("""^-\s+(?:`([^`]+)`\s*)?(.*)$""")
    private val INLINE_CODE_PATTERN = Regex("""`([^`]+)`""")

    fun parse(markdown: String): List<ReleaseHistoryEntry> {
        val entries = mutableListOf<ReleaseHistoryEntry>()
        var version: String? = null
        var date: String? = null
        var items = mutableListOf<ReleaseHistoryItem>()

        fun flush() {
            val flushedVersion = version ?: return
            entries.add(ReleaseHistoryEntry(flushedVersion, date, items.toList()))
        }

        for (rawLine in markdown.lineSequence()) {
            val line = rawLine.trim()
            val versionMatch = VERSION_PATTERN.matchEntire(line)
            when {
                versionMatch != null -> {
                    flush()
                    version = versionMatch.groupValues[1]
                    date = null
                    items = mutableListOf()
                }
                version == null -> Unit
                date == null && items.isEmpty() && DATE_PATTERN.matches(line) ->
                    date = DATE_PATTERN.matchEntire(line)!!.groupValues[1].trim()
                line.startsWith("- ") -> {
                    val match = BULLET_PATTERN.matchEntire(line) ?: continue
                    val tag = match.groupValues[1].takeIf { it.isNotEmpty() }
                    val text = INLINE_CODE_PATTERN.replace(match.groupValues[2].trim(), "$1")
                    if (tag != null || text.isNotEmpty()) {
                        items.add(ReleaseHistoryItem(tag, text))
                    }
                }
            }
        }
        flush()
        return entries
    }
}

/** Renders the bundled changelog for the current locale as one card per released version. */
internal class ReleaseHistoryActivity : ConfiguredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scaffold = buildScaffold(
            R.string.standalone_release_history_title,
            contentPadding = ContentPadding.SCREEN,
        )
        val entries = loadReleaseHistory()?.let(ReleaseHistoryParser::parse).orEmpty()
        if (entries.isEmpty()) {
            scaffold.content.addView(
                emptyStateView(
                    getString(R.string.standalone_release_history_load_failed),
                    iconResource = R.drawable.ic_warning_24,
                ),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = uiDp(Ui.SECTION_GAP) },
            )
        } else {
            entries.forEach { entry ->
                scaffold.content.addView(versionCard(entry), cardListParams())
            }
        }
        setContentView(scaffold.root)
    }

    private fun loadReleaseHistory(): String? = runCatching {
        val assetName = ReleaseHistoryAssetPolicy.assetFor(resources.configuration.locales[0])
        assets.open("doc/$assetName").use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    private fun versionCard(entry: ReleaseHistoryEntry): LinearLayout {
        val card = cardContainer()
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                        text = entry.version
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_TITLE)
                        typeface = Ui.mediumTypeface
                        setTextColor(appPalette.primaryText)
                        // The Latin version would otherwise align by its own direction and run into the date in RTL.
                        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
                if (entry.date != null) {
                    addView(
                        TextView(context).apply {
                            text = entry.date
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_CAPTION)
                            setTextColor(appPalette.secondaryText)
                        },
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply { marginStart = uiDp(Ui.SPACE_SM) },
                    )
                }
            },
        )
        entry.items.forEach { item ->
            card.addView(
                bulletRow(item),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = uiDp(Ui.SPACE_SM + 2) },
            )
        }
        return card
    }

    private fun bulletRow(item: ReleaseHistoryItem): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        if (item.tag != null) {
            addView(
                TextView(context).apply {
                    text = item.tag
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_CAPTION)
                    typeface = Ui.mediumTypeface
                    setTextColor(appPalette.accent)
                    background = roundedFill(accentTone(appPalette.accent), Ui.RADIUS_CONTROL)
                    setPaddingRelative(uiDp(Ui.SPACE_SM), uiDp(2), uiDp(Ui.SPACE_SM), uiDp(2))
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = uiDp(Ui.SPACE_SM)
                    topMargin = uiDp(1)
                },
            )
        }
        addView(
            TextView(context).apply {
                text = item.text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                setTextColor(appPalette.primaryText)
                setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
    }
}
