package io.github.supermonster003.autojs6.plugin.archivemanager

import kotlin.math.ceil
import kotlin.math.roundToLong

internal data class ExtractionProgressMetrics(
    /** Byte progress when a byte total exists, otherwise entry progress. */
    val fraction: Double?,
    val bytesPerSecond: Long?,
    val estimatedRemainingMillis: Long?,
    val elapsedMillis: Long,
)

/** Derives stable, monotonic presentation metrics without changing extraction decisions. */
internal class ExtractionProgressTracker(
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private var startedAtNanos: Long? = null

    fun update(progress: ExtractionProgress): ExtractionProgressMetrics {
        val now = nanoTime()
        if (startedAtNanos == null || progress.phase == ExtractionPhase.PREPARING) {
            startedAtNanos = now
        }
        val elapsedNanos = (now - requireNotNull(startedAtNanos)).coerceAtLeast(0L)
        val elapsedMillis = elapsedNanos / NANOS_PER_MILLISECOND
        val fraction = progressFraction(progress)
        val bytesPerSecond = if (
            progress.phase != ExtractionPhase.CLEANING_UP &&
            progress.phase != ExtractionPhase.CLEANUP_FAILED &&
            progress.bytesWritten > 0L &&
            elapsedNanos > 0L
        ) {
            finitePositiveLong(
                progress.bytesWritten.toDouble() * NANOS_PER_SECOND / elapsedNanos.toDouble(),
            )
        } else {
            null
        }
        val remainingBytes = (progress.totalBytes - progress.bytesWritten).coerceAtLeast(0L)
        val estimatedRemainingMillis = if (
            bytesPerSecond != null &&
            progress.totalBytes > 0L
        ) {
            finiteNonNegativeLong(
                ceil(remainingBytes.toDouble() * MILLIS_PER_SECOND / bytesPerSecond.toDouble()),
            )
        } else {
            null
        }
        return ExtractionProgressMetrics(
            fraction = fraction,
            bytesPerSecond = bytesPerSecond,
            estimatedRemainingMillis = estimatedRemainingMillis,
            elapsedMillis = elapsedMillis,
        )
    }

    private fun progressFraction(progress: ExtractionProgress): Double? = when {
        progress.phase == ExtractionPhase.CLEANING_UP ||
            progress.phase == ExtractionPhase.CLEANUP_FAILED -> null
        progress.phase == ExtractionPhase.COMMITTING -> 1.0
        progress.phase == ExtractionPhase.COMPLETED -> 1.0
        progress.totalBytes > 0L ->
            progress.bytesWritten.toDouble().div(progress.totalBytes.toDouble()).coerceIn(0.0, 1.0)
        progress.totalEntries > 0 ->
            progress.completedEntries.toDouble().div(progress.totalEntries.toDouble()).coerceIn(0.0, 1.0)
        else -> null
    }

    private fun finitePositiveLong(value: Double): Long? =
        value.takeIf { it.isFinite() && it > 0.0 }
            ?.coerceAtMost(Long.MAX_VALUE.toDouble())
            ?.roundToLong()
            ?.coerceAtLeast(1L)

    private fun finiteNonNegativeLong(value: Double): Long? =
        value.takeIf { it.isFinite() && it >= 0.0 }
            ?.coerceAtMost(Long.MAX_VALUE.toDouble())
            ?.roundToLong()

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
