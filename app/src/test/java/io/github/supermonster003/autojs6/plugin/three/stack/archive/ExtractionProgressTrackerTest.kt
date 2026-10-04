package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractionProgressTrackerTest {

    @Test
    fun `derives byte fraction average speed and remaining time from a monotonic clock`() {
        var now = 10_000L
        val tracker = ExtractionProgressTracker { now }
        tracker.update(progress(phase = ExtractionPhase.PREPARING))

        now += 2_000_000_000L
        val active = tracker.update(
            progress(
                phase = ExtractionPhase.EXTRACTING,
                bytesWritten = 1_000L,
                totalBytes = 3_000L,
            ),
        )

        assertEquals(1.0 / 3.0, requireNotNull(active.fraction), 0.000_001)
        assertEquals(500L, active.bytesPerSecond)
        assertEquals(4_000L, active.estimatedRemainingMillis)
        assertEquals(2_000L, active.elapsedMillis)

        now += 4_000_000_000L
        val completed = tracker.update(
            progress(
                phase = ExtractionPhase.COMPLETED,
                completedEntries = 1,
                bytesWritten = 3_000L,
                totalBytes = 3_000L,
            ),
        )
        assertEquals(1.0, requireNotNull(completed.fraction), 0.0)
        assertEquals(500L, completed.bytesPerSecond)
        assertEquals(0L, completed.estimatedRemainingMillis)
        assertEquals(6_000L, completed.elapsedMillis)
    }

    @Test
    fun `uses entry progress for zero byte work and clears estimates during cleanup`() {
        var now = 0L
        val tracker = ExtractionProgressTracker { now }
        tracker.update(progress(phase = ExtractionPhase.PREPARING, totalEntries = 4))
        now += 1_000_000_000L

        val directories = tracker.update(
            progress(
                phase = ExtractionPhase.EXTRACTING,
                completedEntries = 2,
                totalEntries = 4,
            ),
        )
        assertEquals(0.5, requireNotNull(directories.fraction), 0.0)
        assertNull(directories.bytesPerSecond)
        assertNull(directories.estimatedRemainingMillis)

        val cleanup = tracker.update(
            progress(
                phase = ExtractionPhase.CLEANING_UP,
                completedEntries = 2,
                totalEntries = 4,
            ),
        )
        assertNull(cleanup.fraction)
        assertNull(cleanup.bytesPerSecond)
        assertNull(cleanup.estimatedRemainingMillis)

        val cleanupFailed = tracker.update(
            progress(
                phase = ExtractionPhase.CLEANUP_FAILED,
                completedEntries = 2,
                totalEntries = 4,
            ),
        )
        assertNull(cleanupFailed.fraction)
        assertNull(cleanupFailed.bytesPerSecond)
        assertNull(cleanupFailed.estimatedRemainingMillis)
    }

    @Test
    fun `committing is complete work with no remaining estimate`() {
        var now = 0L
        val tracker = ExtractionProgressTracker { now }
        tracker.update(progress(phase = ExtractionPhase.PREPARING, totalEntries = 2))
        now += 1_000_000_000L
        tracker.update(
            progress(
                phase = ExtractionPhase.EXTRACTING,
                completedEntries = 2,
                totalEntries = 2,
                bytesWritten = 100L,
                totalBytes = 100L,
            ),
        )

        val committing = tracker.update(
            progress(
                phase = ExtractionPhase.COMMITTING,
                completedEntries = 2,
                totalEntries = 2,
                bytesWritten = 100L,
                totalBytes = 100L,
            ),
        )

        assertEquals(1.0, requireNotNull(committing.fraction), 0.0)
        assertEquals(0L, committing.estimatedRemainingMillis)
    }

    @Test
    fun `clock regression cannot produce negative speed elapsed time or eta`() {
        var now = 5_000_000_000L
        val tracker = ExtractionProgressTracker { now }
        tracker.update(progress(phase = ExtractionPhase.PREPARING))
        now = 1_000_000_000L

        val metrics = tracker.update(
            progress(
                phase = ExtractionPhase.EXTRACTING,
                bytesWritten = 100L,
                totalBytes = 200L,
            ),
        )

        assertEquals(0L, metrics.elapsedMillis)
        assertNull(metrics.bytesPerSecond)
        assertNull(metrics.estimatedRemainingMillis)
        assertEquals(0.5, requireNotNull(metrics.fraction), 0.0)
    }

    private fun progress(
        phase: ExtractionPhase,
        completedEntries: Int = 0,
        totalEntries: Int = 1,
        bytesWritten: Long = 0L,
        totalBytes: Long = 0L,
    ) = ExtractionProgress(
        phase = phase,
        currentPath = null,
        completedEntries = completedEntries,
        totalEntries = totalEntries,
        bytesWritten = bytesWritten,
        totalBytes = totalBytes,
    )
}
