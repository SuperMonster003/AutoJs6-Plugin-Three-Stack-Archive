package io.github.supermonster003.autojs6.plugin.archivemanager

/** One independently committed output in an archive-creation plan. */
internal data class PlannedArchiveCreation(
    val sourceDisplayName: String?,
    val request: ArchiveCompressionRequest,
    val options: ArchiveCreationOptions,
)

/**
 * An immutable plan for either one combined archive or one archive per selected source item.
 *
 * Separate outputs deliberately use automatic numbering. Explorer Action output transactions do
 * not overwrite existing files, and asking about a later collision after earlier commits would
 * leave an ambiguous retry boundary.
 */
internal data class ArchiveCreationPlan(
    val format: ArchiveFormat,
    val separateArchives: Boolean,
    val jobs: List<PlannedArchiveCreation>,
) {
    init {
        require(jobs.isNotEmpty())
        if (separateArchives) {
            require(jobs.size > 1)
            require(jobs.all { job ->
                job.sourceDisplayName != null &&
                    job.request.targets.size == 1 &&
                    job.options.conflictPolicy == ArchiveCreationConflictPolicy.AUTO_RENAME
            })
        } else {
            require(jobs.size == 1)
        }
        require(jobs.all { job ->
            ArchiveCompressionPolicy.normalizeOutputDisplayName(
                job.options.outputDisplayName,
                format,
            ) == job.options.outputDisplayName
        })
    }
}

internal object ArchiveCreationPlanner {

    fun plan(
        request: ArchiveCompressionRequest,
        format: ArchiveFormat,
        options: ArchiveCreationOptions,
        separateArchives: Boolean,
        fallbackStem: String,
    ): ArchiveCreationPlan {
        if (!separateArchives) {
            return ArchiveCreationPlan(
                format = format,
                separateArchives = false,
                jobs = listOf(
                    PlannedArchiveCreation(
                        sourceDisplayName = null,
                        request = request,
                        options = options,
                    ),
                ),
            )
        }

        require(request.targets.size > 1) {
            "Separate archive creation requires more than one source item"
        }
        return ArchiveCreationPlan(
            format = format,
            separateArchives = true,
            jobs = request.targets.map { target ->
                PlannedArchiveCreation(
                    sourceDisplayName = target.displayName,
                    request = request.copy(targets = listOf(target)),
                    options = options.copy(
                        outputDisplayName = ArchiveCompressionPolicy.defaultOutputDisplayName(
                            targetDisplayNames = listOf(target.displayName),
                            parentDisplayPath = request.parentDisplayPath,
                            fallbackStem = fallbackStem,
                            format = format,
                        ),
                        conflictPolicy = ArchiveCreationConflictPolicy.AUTO_RENAME,
                    ),
                )
            },
        )
    }

    fun separateOutputDisplayNames(
        targetDisplayNames: List<String>,
        parentDisplayPath: String,
        fallbackStem: String,
        format: ArchiveFormat,
    ): List<String> = targetDisplayNames.map { targetDisplayName ->
        ArchiveCompressionPolicy.defaultOutputDisplayName(
            targetDisplayNames = listOf(targetDisplayName),
            parentDisplayPath = parentDisplayPath,
            fallbackStem = fallbackStem,
            format = format,
        )
    }
}

internal data class ArchiveCreationBatchProgress(
    /** One-based position of the output currently being created. */
    val archiveIndex: Int,
    val totalArchives: Int,
    val requestedOutputDisplayName: String,
    val sourceDisplayName: String?,
    val creation: ArchiveCreationProgress,
) {
    init {
        require(archiveIndex in 1..totalArchives)
        require(requestedOutputDisplayName.isNotBlank())
    }
}

internal fun interface ArchiveCreationBatchProgressListener {
    fun onProgress(progress: ArchiveCreationBatchProgress)
}

internal data class ArchiveCreationBatchResult(
    val outputs: List<ArchiveCreationResult>,
) {
    init {
        require(outputs.isNotEmpty())
    }

    val filesCompressed: Long = outputs.sumSaturated(ArchiveCreationResult::filesCompressed)
    val directoriesAdded: Long = outputs.sumSaturated(ArchiveCreationResult::directoriesAdded)
    val sourceBytesRead: Long = outputs.sumSaturated(ArchiveCreationResult::sourceBytesRead)
}

/**
 * A later output failed after at least one earlier output had already been committed.
 *
 * Explorer Action v6 commits each output independently and has no batch rollback primitive. The
 * completed list is therefore part of the public failure state and callers must not offer a blind
 * whole-plan retry.
 */
internal class ArchiveCreationPartialFailureException(
    val completedOutputs: List<ArchiveCreationResult>,
    val totalOutputs: Int,
    /** One-based position of the output that did not complete. */
    val failedOutputIndex: Int,
    val failedRequestedOutputDisplayName: String,
    val failedSourceDisplayName: String?,
    val operationFailure: Throwable,
) : IllegalStateException(
    "Archive creation stopped after ${completedOutputs.size} of $totalOutputs outputs",
    operationFailure,
) {
    init {
        require(completedOutputs.isNotEmpty())
        require(totalOutputs > completedOutputs.size)
        require(failedOutputIndex == completedOutputs.size + 1)
        require(failedOutputIndex in 1..totalOutputs)
    }
}

internal object ArchiveCreationBatchExecutor {

    fun execute(
        writer: ArchiveWriter,
        plan: ArchiveCreationPlan,
        checkCancelled: () -> Unit,
        progress: ArchiveCreationBatchProgressListener,
    ): ArchiveCreationBatchResult {
        require(writer.format == plan.format) {
            "Archive writer and creation plan formats differ"
        }
        val completed = ArrayList<ArchiveCreationResult>(plan.jobs.size)
        plan.jobs.forEachIndexed { index, job ->
            try {
                checkCancelled()
                completed += writer.create(
                    request = job.request,
                    options = job.options,
                    checkCancelled = checkCancelled,
                    progress = ArchiveCreationProgressListener { update ->
                        progress.onProgress(
                            ArchiveCreationBatchProgress(
                                archiveIndex = index + 1,
                                totalArchives = plan.jobs.size,
                                requestedOutputDisplayName = job.options.outputDisplayName,
                                sourceDisplayName = job.sourceDisplayName,
                                creation = update,
                            ),
                        )
                    },
                )
            } catch (error: Throwable) {
                if (completed.isEmpty() || error is Error) throw error
                throw ArchiveCreationPartialFailureException(
                    completedOutputs = completed.toList(),
                    totalOutputs = plan.jobs.size,
                    failedOutputIndex = index + 1,
                    failedRequestedOutputDisplayName = job.options.outputDisplayName,
                    failedSourceDisplayName = job.sourceDisplayName,
                    operationFailure = error,
                )
            }
        }
        return ArchiveCreationBatchResult(completed.toList())
    }
}

private inline fun List<ArchiveCreationResult>.sumSaturated(
    selector: (ArchiveCreationResult) -> Long,
): Long = fold(0L) { total, result ->
    val value = selector(result)
    require(value >= 0L)
    if (value > Long.MAX_VALUE - total) Long.MAX_VALUE else total + value
}
