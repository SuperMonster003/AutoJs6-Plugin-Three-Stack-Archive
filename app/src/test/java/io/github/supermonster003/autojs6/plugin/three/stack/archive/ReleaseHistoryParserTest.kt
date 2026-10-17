package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale

class ReleaseHistoryParserTest {
    @Test fun generatedDialectBecomesOneEntryPerVersion() {
        val markdown = """
            # Release notes

            ## v3.1.0

            _2026/10/17_

            - `Added` Plugin tools for `.7z` archives
            - `Fixed` A fix

            ## v3.0.0

            _2026/10/04_

            - `Note` A note
        """.trimIndent()

        val entries = ReleaseHistoryParser.parse(markdown)

        assertEquals(listOf("v3.1.0", "v3.0.0"), entries.map { it.version })
        assertEquals(listOf("2026/10/17", "2026/10/04"), entries.map { it.date })
        assertEquals(
            listOf(ReleaseHistoryItem("Added", "Plugin tools for .7z archives"), ReleaseHistoryItem("Fixed", "A fix")),
            entries[0].items,
        )
        assertEquals(listOf(ReleaseHistoryItem("Note", "A note")), entries[1].items)
    }

    @Test fun titleStrayLinesAndUntaggedBulletsAreHandled() {
        val markdown = """
            # Release notes

            - `Ignored` before any version
            _ignored date_

            ## v1.0.0

            Free paragraph
            - Untagged item
            _not a date after items_
        """.trimIndent()

        val entries = ReleaseHistoryParser.parse(markdown)

        assertEquals(1, entries.size)
        assertNull(entries[0].date)
        assertEquals(listOf(ReleaseHistoryItem(null, "Untagged item")), entries[0].items)
    }

    @Test fun everyBundledChangelogParsesCompletely() {
        val docs = listOf(File("src/main/assets/doc"), File("app/src/main/assets/doc")).first { it.isDirectory }
        val versionName = docs.resolve("../../../../../version.properties").readLines()
            .first { it.startsWith("VERSION_NAME=") }.substringAfter('=').trim()
        val locales = listOf("ar", "en", "es", "fr", "ja", "ko", "ru", "zh-Hans", "zh-HK", "zh-TW")
            .map(Locale::forLanguageTag)

        for (asset in locales.map(ReleaseHistoryAssetPolicy::assetFor).distinct()) {
            val markdown = docs.resolve(asset).readText()
            val lines = markdown.lines().map(String::trim)
            val entries = ReleaseHistoryParser.parse(markdown)

            assertEquals(asset, lines.count { it.startsWith("## ") }, entries.size)
            assertEquals(asset, "v$versionName", entries.first().version)
            assertTrue(asset, entries.all { it.date != null && it.items.isNotEmpty() && it.items.all { item -> item.tag != null } })
            assertEquals(asset, lines.count { it.startsWith("- ") }, entries.sumOf { it.items.size })
        }
    }
}
