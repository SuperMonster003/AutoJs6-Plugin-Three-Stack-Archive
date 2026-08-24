package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveDiagnosticsTest {

    @Test
    fun `diagnostic finds a typed archive failure through wrapper exceptions`() {
        val root = IllegalStateException("synthetic parser detail")
        val archiveError = ArchiveValidationException(
            code = ArchiveFailureCode.MALFORMED_ARCHIVE,
            message = "ZIP directory metadata cannot be read",
            cause = root,
            format = ArchiveFormat.ZIP,
            stage = ArchiveFailureStage.INDEX,
        )

        val diagnostic = ArchiveFailureDiagnostic.from(
            IllegalStateException("service wrapper", archiveError),
        )

        assertEquals(ArchiveFormat.ZIP, diagnostic.format)
        assertEquals(ArchiveFailureStage.INDEX, diagnostic.stage)
        assertEquals(ArchiveFailureCode.MALFORMED_ARCHIVE, diagnostic.code)
        assertEquals("ZIP directory metadata cannot be read", diagnostic.technicalReason)
        assertTrue(diagnostic.wireSummary().contains("Code: MALFORMED_ARCHIVE"))
        assertFalse(diagnostic.wireSummary().contains("synthetic parser detail"))
        assertTrue(diagnostic.debugReport().contains("synthetic parser detail"))
        assertTrue(diagnostic.debugReport().contains("Stack trace:"))
    }

    @Test
    fun `untyped failure uses caller supplied operation context`() {
        val diagnostic = ArchiveFailureDiagnostic.from(
            error = IllegalArgumentException("untrusted provider detail"),
            formatHint = ArchiveFormat.ZIP,
            stageHint = ArchiveFailureStage.OUTPUT,
        )

        assertEquals(ArchiveFormat.ZIP, diagnostic.format)
        assertEquals(ArchiveFailureStage.OUTPUT, diagnostic.stage)
        assertEquals(null, diagnostic.code)
        assertEquals("Unexpected archive operation failure", diagnostic.technicalReason)
        assertFalse(diagnostic.wireSummary().contains("untrusted provider detail"))
    }

    @Test
    fun `split diagnostic uses the validated display name and keeps the volume list bounded`() {
        val splitFailure = ZipSplitArchiveException(
            ZipSplitArchiveInfo(
                segmentKind = ZipSplitSegmentKind.FINAL_VOLUME,
                lastDiskNumber = 10,
            ),
        )
        val archiveError = ArchiveValidationException(
            code = ArchiveFailureCode.MISSING_VOLUME,
            message = "Archive volumes are missing or unavailable",
            cause = splitFailure,
            format = ArchiveFormat.ZIP,
            stage = ArchiveFailureStage.INDEX,
        )

        val diagnostic = ArchiveFailureDiagnostic.from(
            error = archiveError,
            archiveDisplayName = "backup.zip",
        )

        assertEquals(ArchiveFailureCode.MISSING_VOLUME, diagnostic.code)
        assertEquals(splitFailure.info, diagnostic.splitArchiveInfo)
        assertTrue(diagnostic.technicalReason.contains("backup.z01"))
        assertTrue(diagnostic.technicalReason.contains("backup.z08 (+2)"))
        assertTrue(diagnostic.technicalReason.contains("final volume: backup.zip"))
        val wireSummary = diagnostic.wireSummary()
        assertTrue(wireSummary.startsWith("Code: MISSING_VOLUME; Reason: Required volumes: backup.z01"))
        assertTrue(wireSummary.contains("Format: ZIP; Stage: INDEX"))
    }

    @Test
    fun `failure codes map to stable user facing stages`() {
        assertEquals(ArchiveFailureStage.INPUT, ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE.defaultStage)
        assertEquals(ArchiveFailureStage.INPUT, ArchiveFailureCode.SOURCE_UNREADABLE.defaultStage)
        assertEquals(ArchiveFailureStage.FORMAT_DETECTION, ArchiveFailureCode.INVALID_SIGNATURE.defaultStage)
        assertEquals(ArchiveFailureStage.INDEX, ArchiveFailureCode.MISSING_VOLUME.defaultStage)
        assertEquals(ArchiveFailureStage.INDEX, ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET.defaultStage)
        assertEquals(ArchiveFailureStage.PASSWORD, ArchiveFailureCode.PASSWORD_REQUIRED.defaultStage)
        assertEquals(ArchiveFailureStage.ENTRY_DATA, ArchiveFailureCode.CRC_MISMATCH.defaultStage)
        assertEquals(ArchiveFailureStage.OUTPUT, ArchiveFailureCode.OUTPUT_FAILURE.defaultStage)
    }
}
