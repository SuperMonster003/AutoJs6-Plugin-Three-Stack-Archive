package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveIntentPolicyTest {

    @Test
    fun displayNameMustBeAnUnmodifiedLeafNameWithinTheLimit() {
        assertEquals("bundle.AAR", ArchiveIntentPolicy.validateDisplayName("bundle.AAR"))
        assertEquals(
            "a".repeat(ArchiveIntentPolicy.MAX_DISPLAY_NAME_LENGTH),
            ArchiveIntentPolicy.validateDisplayName(
                "a".repeat(ArchiveIntentPolicy.MAX_DISPLAY_NAME_LENGTH),
            ),
        )

        assertNull(ArchiveIntentPolicy.validateDisplayName(null))
        assertNull(ArchiveIntentPolicy.validateDisplayName(""))
        assertNull(ArchiveIntentPolicy.validateDisplayName("   "))
        assertNull(ArchiveIntentPolicy.validateDisplayName("."))
        assertNull(ArchiveIntentPolicy.validateDisplayName(".."))
        assertNull(ArchiveIntentPolicy.validateDisplayName("folder/archive.zip"))
        assertNull(ArchiveIntentPolicy.validateDisplayName("folder\\archive.zip"))
        assertNull(ArchiveIntentPolicy.validateDisplayName("archive\u0000.zip"))
        assertNull(
            ArchiveIntentPolicy.validateDisplayName(
                "a".repeat(ArchiveIntentPolicy.MAX_DISPLAY_NAME_LENGTH + 1),
            ),
        )
    }

    @Test
    fun reportedSizeMayBeUnknownAndHasNoArbitraryFourGibibyteCap() {
        assertTrue(ArchiveIntentPolicy.isReportedSizeAccepted(ArchiveIntentPolicy.SIZE_UNKNOWN))
        assertTrue(ArchiveIntentPolicy.isReportedSizeAccepted(0L))
        assertTrue(ArchiveIntentPolicy.isReportedSizeAccepted(Long.MAX_VALUE))
        assertFalse(ArchiveIntentPolicy.isReportedSizeAccepted(ArchiveIntentPolicy.SIZE_UNKNOWN - 1L))
    }

    @Test
    fun supportedMimeTypesAndExtensionsUseOrSemantics() {
        ArchiveManagerPlugin.MIME_TYPES.forEach { mimeType ->
            assertTrue(mimeType, ArchiveIntentPolicy.isSupportedArchive(mimeType, "archive.bin"))
        }
        ArchiveManagerPlugin.EXTENSIONS.forEach { extension ->
            assertTrue(
                extension,
                ArchiveIntentPolicy.isSupportedArchive("application/octet-stream", "archive.${extension.uppercase()}"),
            )
        }
        assertTrue(
            ArchiveIntentPolicy.isSupportedArchive(
                "application/zip; charset=binary",
                "archive.bin",
            ),
        )
        assertFalse(ArchiveIntentPolicy.isSupportedArchive("text/plain", "archive.rar"))
    }
}
