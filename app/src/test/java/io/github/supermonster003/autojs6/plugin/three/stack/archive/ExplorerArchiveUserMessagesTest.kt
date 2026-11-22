package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CancellationException
import io.github.supermonster003.autojs6.plugin.three.stack.archive.ExplorerArchiveUserMessages.Operation

/**
 * Q288 (c): the plain-language sentence that the plugin attaches to the host's archive failure Bundles
 * under the optional `errorUserMessage` key. The plugin keeps the v21 protocol AAR and its declared
 * protocol, so the key is a local constant; the instrumentation tests read it from real failure Bundles.
 */
class ExplorerArchiveUserMessagesTest {

    @Test
    fun `the key and the limit are the ones the host reads`() {
        assertEquals("errorUserMessage", ExplorerArchiveUserMessages.KEY)
        assertEquals(120, ExplorerArchiveUserMessages.MAX_LENGTH)
        assertTrue("the bundled protocol stays at v21", ExplorerActionProtocol.VERSION < 28)
        assertEquals(5276L, ThreeStackArchivePlugin.REQUIRED_HOST_VERSION)
    }

    @Test
    fun `known causes map to a sentence and the rest send none`() {
        val expected = mapOf(
            ArchiveFailureCode.MALFORMED_ARCHIVE to R.string.explorer_failure_archive_damaged,
            ArchiveFailureCode.SIZE_MISMATCH to R.string.explorer_failure_archive_damaged,
            ArchiveFailureCode.CRC_MISMATCH to R.string.explorer_failure_archive_damaged,
            ArchiveFailureCode.INVALID_SIGNATURE to R.string.explorer_failure_not_an_archive,
            ArchiveFailureCode.UNSUPPORTED_METHOD to R.string.explorer_failure_unsupported_method,
            ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET to R.string.explorer_failure_filename_encoding,
            ArchiveFailureCode.MISSING_VOLUME to R.string.explorer_failure_missing_volumes,
            ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE to R.string.explorer_failure_storage_full,
            ArchiveFailureCode.OUTPUT_FAILURE to R.string.explorer_failure_output_unwritable,
            ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED to R.string.explorer_failure_name_exists,
            ArchiveFailureCode.SOURCE_NOT_FILE to R.string.explorer_failure_source_unreadable,
            ArchiveFailureCode.SOURCE_UNREADABLE to R.string.explorer_failure_source_unreadable,
            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED to R.string.explorer_failure_safety_limit,
            ArchiveFailureCode.PATH_LIMIT_EXCEEDED to R.string.explorer_failure_safety_limit,
            ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED to R.string.explorer_failure_safety_limit,
            ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED to R.string.explorer_failure_safety_limit,
            ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED to R.string.explorer_failure_safety_limit,
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED to R.string.explorer_failure_safety_limit,
            ArchiveFailureCode.INVALID_PATH to R.string.explorer_failure_unsafe_path,
            ArchiveFailureCode.INVALID_DESTINATION_NAME to R.string.explorer_failure_invalid_name,
            ArchiveFailureCode.UNKNOWN_SELECTION to R.string.explorer_failure_selection_gone,
        )
        Operation.entries.forEach { operation ->
            expected.forEach { (code, res) ->
                assertEquals("$code $operation", res, ExplorerArchiveUserMessages.messageRes(code, operation))
            }
            listOf(
                ArchiveFailureCode.PASSWORD_REQUIRED,
                ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
                ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
                ArchiveFailureCode.EMPTY_SELECTION,
            ).forEach { code ->
                assertNull("$code $operation", ExplorerArchiveUserMessages.messageRes(code, operation))
            }
        }
        assertEquals(
            R.string.explorer_failure_wrong_password,
            ExplorerArchiveUserMessages.messageRes(ArchiveFailureCode.WRONG_PASSWORD, Operation.MODIFY),
        )
        assertNull(ExplorerArchiveUserMessages.messageRes(ArchiveFailureCode.WRONG_PASSWORD, Operation.OPEN))
        assertNull(ExplorerArchiveUserMessages.messageRes(ArchiveFailureCode.WRONG_PASSWORD, Operation.EXTRACT))
        assertEquals(
            R.string.explorer_failure_source_or_input_changed,
            ExplorerArchiveUserMessages.messageRes(ArchiveFailureCode.SOURCE_CHANGED, Operation.MODIFY),
        )
        listOf(Operation.OPEN, Operation.EXTRACT).forEach { operation ->
            assertEquals(
                R.string.explorer_failure_source_changed,
                ExplorerArchiveUserMessages.messageRes(ArchiveFailureCode.SOURCE_CHANGED, operation),
            )
        }
        listOf(ArchiveFailureCode.DUPLICATE_PATH, ArchiveFailureCode.FILE_DIRECTORY_CONFLICT).forEach { code ->
            assertEquals(R.string.explorer_failure_name_exists, ExplorerArchiveUserMessages.messageRes(code, Operation.MODIFY))
            assertEquals(R.string.explorer_failure_unsafe_path, ExplorerArchiveUserMessages.messageRes(code, Operation.OPEN))
            assertEquals(R.string.explorer_failure_unsafe_path, ExplorerArchiveUserMessages.messageRes(code, Operation.EXTRACT))
        }
    }

    @Test
    fun `failures the host answers with an interaction never carry a sentence`() {
        ArchiveFailureCode.entries.forEach { code ->
            if (code.extractionInteractionKind() != null) {
                assertNull("$code", ExplorerArchiveUserMessages.messageRes(code, Operation.EXTRACT))
            }
            if (code.archiveOpenServiceErrorCode() != null) {
                assertNull("$code", ExplorerArchiveUserMessages.messageRes(code, Operation.OPEN))
            }
        }
    }

    @Test
    fun `the cause chain decides and cancellation or unknown causes send none`() {
        val wrapped = IllegalStateException("wrapper", ArchiveException(ArchiveFailureCode.CRC_MISMATCH, "CRC"))
        assertEquals(R.string.explorer_failure_archive_damaged, ExplorerArchiveUserMessages.messageRes(wrapped, Operation.EXTRACT))
        assertEquals(
            R.string.explorer_failure_storage_full,
            ExplorerArchiveUserMessages.messageRes(
                ArchiveException(ArchiveFailureCode.OUTPUT_FAILURE, "write", IOException("write failed: ENOSPC (No space left on device)")),
                Operation.EXTRACT,
            ),
        )
        assertEquals(
            R.string.explorer_failure_storage_full,
            ExplorerArchiveUserMessages.messageRes(IOException("No space left on device"), Operation.MODIFY),
        )
        assertNull(ExplorerArchiveUserMessages.messageRes(IOException("Broken pipe"), Operation.OPEN))
        assertNull(ExplorerArchiveUserMessages.messageRes(IllegalArgumentException("bad request"), Operation.MODIFY))
        assertNull(ExplorerArchiveUserMessages.messageRes(CancellationException("cancelled"), Operation.EXTRACT))
        assertNull(
            ExplorerArchiveUserMessages.messageRes(
                ArchiveException(ArchiveFailureCode.OUTPUT_FAILURE, "stopped", InterruptedIOException()),
                Operation.MODIFY,
            ),
        )
    }

    @Test
    fun `sanitizing collapses control characters and rejects empty or long text`() {
        assertEquals("A b c.", ExplorerArchiveUserMessages.sanitize(" A\u0000b\u200B\u2028c. "))
        assertNull(ExplorerArchiveUserMessages.sanitize(" \u0007 "))
        val limit = "📦".repeat(ExplorerArchiveUserMessages.MAX_LENGTH)
        assertEquals(limit, ExplorerArchiveUserMessages.sanitize(limit))
        assertNull(ExplorerArchiveUserMessages.sanitize(limit + "x"))
    }

    @Test
    fun `every sentence exists in every language, fits the limit and uses ASCII punctuation`() {
        val names = Regex("R\\.string\\.(explorer_failure_[a-z_]+)")
            .findAll(read("app/src/main/java/io/github/supermonster003/autojs6/plugin/three/stack/archive/ExplorerArchiveUserMessages.kt"))
            .map { it.groupValues[1] }
            .toSortedSet()
        assertEquals(16, names.size)
        val directories = listOf(
            "values", "values-en", "values-zh", "values-zh-rHK", "values-zh-rTW", "values-ja",
            "values-ko", "values-fr", "values-es", "values-ru", "values-ar",
        )
        val pattern = Regex("<string name=\"(explorer_failure_[a-z_]+)\">([^<]*)</string>")
        directories.forEach { directory ->
            val values = pattern.findAll(read("app/src/main/res/$directory/strings.xml"))
                .associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'") }
            assertEquals(directory, names, values.keys.toSortedSet())
            values.forEach { (name, value) ->
                assertEquals("$directory $name", value, ExplorerArchiveUserMessages.sanitize(value))
                assertTrue("$directory $name", value.endsWith("."))
                assertTrue("$directory $name", value.none { it in "，。：；！？（）、“”‘’،" })
                assertTrue("$directory $name takes ${toastLines(value)} toast lines", toastLines(value) <= 2)
            }
        }
    }

    /**
     * The host's two-line rule (AutoJs6 `ExplorerArchiveUserMessage.fitsToast`): greedy breaks at spaces and between
     * East Asian wide letters, letter widths in classes of the system fonts at size 100, [TOAST_LINE_WIDTH] per line.
     */
    private fun toastLines(text: String): Int {
        var lines = 1
        var width = 0
        val word = StringBuilder()
        val tokens = mutableListOf<String>()
        text.forEach { char ->
            if (char == ' ' || isWide(char)) {
                if (word.isNotEmpty()) tokens += word.toString().also { word.clear() }
                tokens += char.toString()
            } else {
                word.append(char)
            }
        }
        if (word.isNotEmpty()) tokens += word.toString()
        tokens.forEach { token ->
            val advance = token.sumOf(::advance)
            when {
                token == " " -> width += advance
                width > 0 && width + advance > TOAST_LINE_WIDTH -> width = advance.also { lines += 1 }
                else -> width += advance
            }
        }
        return lines
    }

    private fun isWide(char: Char): Boolean = char.code in 0x1100..0x115F || char.code in 0x2E80..0xA4CF ||
        char.code in 0xAC00..0xD7A3 || char.code in 0xF900..0xFAFF || char.code in 0xFF00..0xFF60

    private fun advance(char: Char): Int = when {
        isWide(char) -> 100
        char == ' ' -> 25
        char in "ijlfrtI.,:;'\"!|()-" -> 27
        char.code in WIDE_LETTERS -> 82
        char.code in 0x0600..0x06FF -> 60
        char.isUpperCase() -> 64
        char.isDigit() -> 56
        else -> 54
    }

    private companion object {
        const val TOAST_LINE_WIDTH = 1751
        val WIDE_LETTERS = setOf(
            'm'.code, 'w'.code, 'M'.code, 'W'.code,
            0x0436, 0x043C, 0x0448, 0x0449, 0x044B, 0x044E, 0x0416, 0x041C, 0x0428, 0x0429, 0x042B, 0x042E,
        )
    }

    @Test
    fun `all three failure Bundles carry the key and only the open failure uses the service context`() {
        val base = "app/src/main/java/io/github/supermonster003/autojs6/plugin/three/stack/archive"
        listOf(
            "ExplorerActionService.kt",
            "ExplorerArchiveExtractionOperation.kt",
            "ExplorerArchiveSession.kt",
        ).forEach { file ->
            assertTrue(file, read("$base/$file").contains("putString(ExplorerArchiveUserMessages.KEY, text)"))
        }
        val service = read("$base/ExplorerActionService.kt")
        assertTrue(service.contains("userMessage(failure.cause, ExplorerArchiveUserMessages.Operation.OPEN)"))
        assertTrue(service.contains("userMessage = ::userMessage,"))
    }

    private fun read(relativePath: String): String =
        String(Files.readAllBytes(findProjectRoot().resolve(relativePath)), Charsets.UTF_8)

    private fun findProjectRoot(): Path {
        var path: Path? = Paths.get("").toAbsolutePath()
        while (path != null) {
            if (Files.isRegularFile(path.resolve("settings.gradle.kts"))) return path
            path = path.parent
        }
        error("Project root not found")
    }
}
