package io.github.supermonster003.autojs6.plugin.archivemanager

/**
 * Records an output root that could not be removed after extraction failed or was cancelled.
 *
 * The original operation failure remains primary. This exception is attached as a suppressed
 * failure so callers can report the stable residual location without replacing the real cause.
 */
internal class ArchiveCleanupException(
    val residualOutput: ArchiveOutputLocation,
    cause: Throwable,
) : ArchiveException(
    code = ArchiveFailureCode.OUTPUT_FAILURE,
    message = "Partial extraction output could not be removed",
    cause = cause,
    stage = ArchiveFailureStage.CLEANUP,
)

/** Finds typed cleanup failures through causes and suppressed failures without looping. */
internal fun Throwable.residualArchiveOutputs(): List<ArchiveOutputLocation> {
    val pending = ArrayDeque<Throwable>()
    val visited = HashSet<Throwable>()
    val outputs = LinkedHashMap<String, ArchiveOutputLocation>()
    pending.add(this)
    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        if (!visited.add(current)) continue
        if (current is ArchiveCleanupException) {
            outputs.putIfAbsent(current.residualOutput.identifier, current.residualOutput)
        }
        current.cause?.let(pending::addLast)
        current.suppressed.forEach(pending::addLast)
    }
    return outputs.values.toList()
}
