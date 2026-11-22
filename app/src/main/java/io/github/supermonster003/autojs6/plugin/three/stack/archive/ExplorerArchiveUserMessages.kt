package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import java.io.InterruptedIOException
import java.util.Locale
import java.util.concurrent.CancellationException

/**
 * Plain-language failure sentences attached to the AutoJs6 file manager's archive failure Bundles.
 *
 * AutoJs6 6.8.1 and later read the optional [KEY] from the open, extraction and mutation failure
 * Bundles and show it instead of their own reason text (see the host's
 * `docs/dev/explorer-archive-user-message-v1.md`). Older hosts ignore the key. The key is defined
 * here because the plugin keeps building against the v21 explorer-action AAR.
 *
 * A sentence is sent only for causes this plugin knows, only in the host's interface language,
 * and never for cancellation or for failures that the host answers with an interaction (password,
 * unsafe paths, resource budget). When the host language cannot be read, nothing is sent and the
 * host keeps its own text.
 */
internal object ExplorerArchiveUserMessages {

    /** Same name in all three failure Bundles. */
    const val KEY = "errorUserMessage"

    /**
     * Host limit in Unicode code points. The host also ignores text that would take more than two
     * lines of a toast (about 34 CJK characters or 60 Latin letters) and shows its own text instead.
     */
    const val MAX_LENGTH = 120

    enum class Operation { OPEN, EXTRACT, MODIFY }

    /** Returns the sentence for [error] in the host language, or `null` when none should be sent. */
    fun message(
        context: Context,
        error: Throwable,
        operation: Operation,
        hostLanguageTag: () -> String? = {
            AutoJs6HostSettingsClient.resolvedLanguageTagBlocking(context)
        },
    ): String? {
        val res = messageRes(error, operation) ?: return null
        val languageTag = runCatching(hostLanguageTag).getOrNull()
            ?.let(AppSettingsPolicy::languageForResolvedTag)
            ?.languageTag
            ?: return null
        val locale = Locale.forLanguageTag(languageTag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        val text = runCatching {
            context.createConfigurationContext(configuration).getString(res)
        }.getOrNull() ?: return null
        return sanitize(text)
    }

    /** Collapses control, format and separator characters; `null` when empty or over the limit. */
    fun sanitize(text: String): String? = text
        .replace(Regex("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotEmpty() && it.codePointCount(0, it.length) <= MAX_LENGTH }

    @StringRes
    fun messageRes(error: Throwable, operation: Operation): Int? {
        val causes = causeChain(error)
        if (causes.any { it is CancellationException || it is InterruptedIOException }) return null
        if (causes.any(::isNoSpace)) return R.string.explorer_failure_storage_full
        val code = causes.filterIsInstance<ArchiveException>().firstOrNull()?.code
            ?: return if (causes.any { it is ZipSplitArchiveException }) {
                R.string.explorer_failure_missing_volumes
            } else {
                null
            }
        return messageRes(code, operation)
    }

    @StringRes
    fun messageRes(code: ArchiveFailureCode, operation: Operation): Int? = when (code) {
        ArchiveFailureCode.MALFORMED_ARCHIVE,
        ArchiveFailureCode.SIZE_MISMATCH,
        ArchiveFailureCode.CRC_MISMATCH,
        -> R.string.explorer_failure_archive_damaged

        ArchiveFailureCode.INVALID_SIGNATURE -> R.string.explorer_failure_not_an_archive
        ArchiveFailureCode.UNSUPPORTED_METHOD -> R.string.explorer_failure_unsupported_method
        ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET -> R.string.explorer_failure_filename_encoding
        ArchiveFailureCode.MISSING_VOLUME -> R.string.explorer_failure_missing_volumes

        // Opening and extraction answer password problems with the host's password dialog.
        ArchiveFailureCode.WRONG_PASSWORD ->
            R.string.explorer_failure_wrong_password.takeIf { operation == Operation.MODIFY }

        ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE -> R.string.explorer_failure_storage_full
        ArchiveFailureCode.OUTPUT_FAILURE -> R.string.explorer_failure_output_unwritable
        ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED -> R.string.explorer_failure_name_exists

        // While editing these mean the new name is taken; elsewhere the archive itself repeats a path.
        ArchiveFailureCode.DUPLICATE_PATH,
        ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
        -> if (operation == Operation.MODIFY) {
            R.string.explorer_failure_name_exists
        } else {
            R.string.explorer_failure_unsafe_path
        }

        // While editing, the files being added are checked for changes too.
        ArchiveFailureCode.SOURCE_CHANGED -> if (operation == Operation.MODIFY) {
            R.string.explorer_failure_source_or_input_changed
        } else {
            R.string.explorer_failure_source_changed
        }

        ArchiveFailureCode.SOURCE_NOT_FILE,
        ArchiveFailureCode.SOURCE_UNREADABLE,
        -> R.string.explorer_failure_source_unreadable

        ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
        ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
        ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
        ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
        ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
        ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
        -> R.string.explorer_failure_safety_limit

        ArchiveFailureCode.INVALID_PATH -> R.string.explorer_failure_unsafe_path
        ArchiveFailureCode.INVALID_DESTINATION_NAME -> R.string.explorer_failure_invalid_name
        ArchiveFailureCode.UNKNOWN_SELECTION -> R.string.explorer_failure_selection_gone

        // The host asks the user (password, unsafe paths, resource budget) or never sends these.
        ArchiveFailureCode.PASSWORD_REQUIRED,
        ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
        ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
        ArchiveFailureCode.EMPTY_SELECTION,
        -> null
    }

    private fun isNoSpace(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("ENOSPC") || message.contains("No space left on device", ignoreCase = true)
    }

    private fun causeChain(error: Throwable): List<Throwable> {
        val result = ArrayList<Throwable>()
        val visited = HashSet<Throwable>()
        var current: Throwable? = error
        while (current != null && visited.add(current)) {
            result += current
            current = current.cause
        }
        return result
    }
}
