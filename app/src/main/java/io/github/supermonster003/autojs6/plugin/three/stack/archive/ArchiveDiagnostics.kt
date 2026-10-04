package io.github.supermonster003.autojs6.plugin.three.stack.archive

import java.io.PrintWriter
import java.io.StringWriter

internal data class ArchiveFailureDiagnostic(
    val format: ArchiveFormat?,
    val stage: ArchiveFailureStage,
    val code: ArchiveFailureCode?,
    val technicalReason: String,
    val splitArchiveInfo: ZipSplitArchiveInfo?,
    private val error: Throwable,
) {

    fun wireSummary(): String = if (code == ArchiveFailureCode.MISSING_VOLUME) buildString {
        append("Code: MISSING_VOLUME; Reason: ")
        append(technicalReason)
        append("; Format: ")
        append(format?.displayName ?: "Unknown")
        append("; Stage: ")
        append(stage.name)
    } else buildString {
        append("Format: ")
        append(format?.displayName ?: "Unknown")
        append("; Stage: ")
        append(stage.name)
        append("; Code: ")
        append(code?.name ?: "UNKNOWN")
        append("; Reason: ")
        append(technicalReason)
    }

    fun debugReport(): String = buildString {
        appendLine("Archive Manager diagnostic")
        appendLine("Format: ${format?.displayName ?: "Unknown"}")
        appendLine("Stage: ${stage.name}")
        appendLine("Code: ${code?.name ?: "UNKNOWN"}")
        appendLine("Reason: $technicalReason")
        appendLine()
        appendLine("Cause chain:")
        causeChain(error).forEachIndexed { index, cause ->
            append("  ")
            append(index + 1)
            append(". ")
            append(cause.javaClass.name)
            cause.message?.trim()?.takeIf(String::isNotEmpty)?.let { message ->
                append(": ")
                append(message)
            }
            appendLine()
        }
        appendLine()
        appendLine("Stack trace:")
        append(stackTrace(error))
    }

    companion object {
        fun from(
            error: Throwable,
            formatHint: ArchiveFormat? = null,
            stageHint: ArchiveFailureStage = ArchiveFailureStage.INPUT,
            archiveDisplayName: String? = null,
        ): ArchiveFailureDiagnostic {
            val causes = causeChain(error)
            val archiveError = causes.filterIsInstance<ArchiveException>().firstOrNull()
            val splitArchiveInfo = causes.filterIsInstance<ZipSplitArchiveException>()
                .firstOrNull()
                ?.info
            return ArchiveFailureDiagnostic(
                format = archiveError?.format ?: formatHint,
                stage = archiveError?.stage ?: stageHint,
                code = archiveError?.code,
                technicalReason = splitArchiveInfo?.technicalReason(archiveDisplayName)
                    ?: archiveError?.message
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
                    ?: "Unexpected archive operation failure",
                splitArchiveInfo = splitArchiveInfo,
                error = error,
            )
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

        private fun stackTrace(error: Throwable): String = StringWriter().also { writer ->
            PrintWriter(writer).use(error::printStackTrace)
        }.toString()
    }
}
