package io.github.supermonster003.autojs6.plugin.archivebrowser

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
    fun declaredSizeMustBeWithinTheFourGibibyteBoundary() {
        assertTrue(ArchiveIntentPolicy.isDeclaredSizeAccepted(0L))
        assertTrue(
            ArchiveIntentPolicy.isDeclaredSizeAccepted(ArchiveCacheStager.MAX_ARCHIVE_BYTES),
        )
        assertFalse(ArchiveIntentPolicy.isDeclaredSizeAccepted(-1L))
        assertFalse(
            ArchiveIntentPolicy.isDeclaredSizeAccepted(ArchiveCacheStager.MAX_ARCHIVE_BYTES + 1L),
        )
    }

    @Test
    fun supportedMimeTypesAndExtensionsUseOrSemantics() {
        ArchiveBrowserPlugin.MIME_TYPES.forEach { mimeType ->
            assertTrue(mimeType, ArchiveIntentPolicy.isSupportedArchive(mimeType, "archive.bin"))
        }
        ArchiveBrowserPlugin.EXTENSIONS.forEach { extension ->
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
