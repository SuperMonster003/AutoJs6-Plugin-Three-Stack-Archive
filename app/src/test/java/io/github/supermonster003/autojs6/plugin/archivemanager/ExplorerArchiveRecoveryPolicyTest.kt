package io.github.supermonster003.autojs6.plugin.archivemanager

import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenValues
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorerArchiveRecoveryPolicyTest {

    @Test
    fun `archive open password failures use distinct service codes`() {
        assertEquals(
            ExplorerArchiveOpenValues.ERROR_PASSWORD_REQUIRED,
            ArchiveFailureCode.PASSWORD_REQUIRED.archiveOpenServiceErrorCode(),
        )
        assertEquals(
            ExplorerArchiveOpenValues.ERROR_WRONG_PASSWORD,
            ArchiveFailureCode.WRONG_PASSWORD.archiveOpenServiceErrorCode(),
        )
        assertNull(ArchiveFailureCode.MALFORMED_ARCHIVE.archiveOpenServiceErrorCode())
    }

    @Test
    fun `request options copy passwords omit them from diagnostics and clear owned storage`() {
        val callerPassword = "do-not-log-this-password".toCharArray()
        val options = ExplorerArchiveExtractionRequestOptions(
            password = callerPassword,
            skipUnsafePaths = true,
            allowResourceBudgetOverride = true,
        )
        callerPassword.fill('\u0000')

        val firstCopy = requireNotNull(options.passwordChars())
        assertArrayEquals("do-not-log-this-password".toCharArray(), firstCopy)
        firstCopy.fill('\u0000')
        assertFalse(options.toString().contains("do-not-log-this-password"))
        assertTrue(options.hasPassword)

        options.close()
        assertNull(options.passwordChars())
        assertFalse(options.hasPassword)
    }

    @Test
    fun `request options reject oversized passwords`() {
        val password = CharArray(ExplorerActionProtocol.MAX_ARCHIVE_PASSWORD_LENGTH + 1) { 'x' }
        try {
            assertThrows(IllegalArgumentException::class.java) {
                ExplorerArchiveExtractionRequestOptions(password = password)
            }
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun `recoverable extraction failures map to distinct protocol interactions`() {
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_PASSWORD_REQUIRED,
            ArchiveFailureCode.PASSWORD_REQUIRED.extractionInteractionKind(),
        )
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_WRONG_PASSWORD,
            ArchiveFailureCode.WRONG_PASSWORD.extractionInteractionKind(),
        )
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_UNSAFE_PATHS,
            ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED.extractionInteractionKind(),
        )
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_RESOURCE_BUDGET,
            ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED.extractionInteractionKind(),
        )
        assertNull(ArchiveFailureCode.OUTPUT_FAILURE.extractionInteractionKind())
    }

    @Test
    fun `password retry validator accepts only encrypted readability changes`() {
        val locked = snapshot(entry(encrypted = true, readable = false))
        val unlocked = snapshot(entry(encrypted = true, readable = true))

        ArchiveRetrySnapshotValidator.requireSameArchive(locked, unlocked)

        val changedPath = unlocked.copy(
            entries = unlocked.entries.map { it.copy(path = "changed.txt", sourceName = "changed.txt") },
        )
        val sourceChanged = assertThrows(ArchiveExtractionException::class.java) {
            ArchiveRetrySnapshotValidator.requireSameArchive(locked, changedPath)
        }
        assertEquals(ArchiveFailureCode.SOURCE_CHANGED, sourceChanged.code)

        val unreadablePlain = snapshot(entry(encrypted = false, readable = false))
        val readablePlain = snapshot(entry(encrypted = false, readable = true))
        assertThrows(ArchiveExtractionException::class.java) {
            ArchiveRetrySnapshotValidator.requireSameArchive(unreadablePlain, readablePlain)
        }
    }

    private fun snapshot(entry: ArchiveEntry) = ArchiveSnapshot(
        sourceLength = 100L,
        sourceLastModifiedMillis = 200L,
        entries = listOf(entry),
        totalUncompressedBytes = entry.uncompressedSize,
        structureLimits = ArchiveStructureLimits.DEFAULT,
        format = ArchiveFormat.SEVEN_Z,
    )

    private fun entry(encrypted: Boolean, readable: Boolean): ArchiveEntry {
        val limitations = buildSet {
            if (encrypted) add(ArchiveEntryLimitation.ENCRYPTED)
            add(ArchiveEntryLimitation.MUTATION_UNAVAILABLE)
        }
        return ArchiveEntry(
            path = "payload.txt",
            sourceName = "payload.txt",
            displayName = "payload.txt",
            isDirectory = false,
            compressionMethod = ArchiveCompressionMethod.LZMA2,
            compressionMethodId = "LZMA2",
            isEncrypted = encrypted,
            encryptionMethod = ArchiveEncryptionMethod.AES.takeIf { encrypted },
            capabilities = ArchiveEntryCapabilities(
                canOpen = readable,
                canExtract = readable,
                canDelete = false,
                canRename = false,
                limitations = limitations,
            ),
            compressedSize = 50L,
            uncompressedSize = 100L,
            crc32 = 123L,
            modifiedTimeMillis = 456L,
            ordinal = 0,
        )
    }
}
