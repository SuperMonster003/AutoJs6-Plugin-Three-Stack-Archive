package io.github.supermonster003.autojs6.plugin.three.stack.archive

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveExtractionConflictControllerTest {

    @Test
    fun `fixed overwrite safely renames an incompatible conflict`() = runBlocking {
        val controller = ArchiveExtractionConflictController(
            policy = ArchiveExtractionConflictPolicy.OVERWRITE,
            resolver = ArchiveExtractionConflictResolver.NONE,
        )

        assertEquals(
            ArchiveExtractionConflictDecision.AUTO_RENAME,
            controller.resolve(conflict(canOverwrite = false)),
        )
    }

    @Test
    fun `applied overwrite is reused only for compatible conflicts`() = runBlocking {
        var resolverCalls = 0
        val controller = ArchiveExtractionConflictController(
            policy = ArchiveExtractionConflictPolicy.ASK,
            resolver = ArchiveExtractionConflictResolver {
                resolverCalls++
                if (resolverCalls == 1) {
                    ArchiveExtractionConflictResolution(
                        decision = ArchiveExtractionConflictDecision.OVERWRITE,
                        applyToAll = true,
                    )
                } else {
                    ArchiveExtractionConflictResolution(
                        decision = ArchiveExtractionConflictDecision.AUTO_RENAME,
                    )
                }
            },
        )

        assertEquals(
            ArchiveExtractionConflictDecision.OVERWRITE,
            controller.resolve(conflict(canOverwrite = true)),
        )
        assertEquals(
            ArchiveExtractionConflictDecision.AUTO_RENAME,
            controller.resolve(conflict(canOverwrite = false)),
        )
        assertEquals(
            ArchiveExtractionConflictDecision.OVERWRITE,
            controller.resolve(conflict(canOverwrite = true)),
        )
        assertEquals(2, resolverCalls)
    }

    @Test
    fun `ask rejects an impossible overwrite response`() {
        val controller = ArchiveExtractionConflictController(
            policy = ArchiveExtractionConflictPolicy.ASK,
            resolver = ArchiveExtractionConflictResolver {
                ArchiveExtractionConflictResolution(
                    decision = ArchiveExtractionConflictDecision.OVERWRITE,
                )
            },
        )

        val error = try {
            runBlocking { controller.resolve(conflict(canOverwrite = false)) }
            throw AssertionError("Expected output conflict confirmation failure")
        } catch (expected: ArchiveExtractionException) {
            expected
        }

        assertEquals(ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED, error.code)
        assertEquals(ArchiveFailureStage.OUTPUT, error.stage)
    }

    private fun conflict(canOverwrite: Boolean) = ArchiveExtractionConflict(
        archivePath = "A.txt",
        requestedDisplayName = "A.txt",
        existingDisplayName = "a.txt",
        incomingIsDirectory = false,
        existingIsDirectory = false,
        canOverwrite = canOverwrite,
    )
}
