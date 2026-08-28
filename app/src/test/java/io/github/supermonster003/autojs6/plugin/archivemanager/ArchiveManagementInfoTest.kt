package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArchiveManagementInfoTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun snapshot(): ArchiveSnapshot = ArchiveScanner().scan(
        writeZip(
            temporaryFolder.newFile(),
            FixtureEntry("docs/readme.txt", "readme".encodeToByteArray()),
            FixtureEntry("root.bin", byteArrayOf(1, 2, 3)),
        ),
    )

    private fun tarSnapshot(): ArchiveSnapshot = ArchiveScanner().scan(
        writeTar(
            temporaryFolder.newFile(),
            TarFixtureEntry("docs/", type = TarFixtureEntryType.DIRECTORY),
            TarFixtureEntry("docs/readme.txt", "readme".encodeToByteArray()),
        ),
    )

    @Test
    fun `ordinary zip is writable only in a management session with target replacement`() {
        val archive = snapshot()
        val engine = ArchiveEngine.DEFAULT

        val writable = engine.managementStatus(
            archive,
            ArchiveRequestedAction.MANAGE,
            hasHostReplacementSession = true,
        )
        assertTrue(writable.isWritable)
        assertEquals(null, writable.readOnlyReason)
        assertNotNull(writable.capabilities)

        val openOnly = engine.managementStatus(
            archive,
            ArchiveRequestedAction.OPEN,
            hasHostReplacementSession = true,
        )
        assertFalse(openOnly.isWritable)
        assertEquals(
            ArchiveManagementReadOnlyReason.CURRENT_SESSION_READ_ONLY,
            openOnly.readOnlyReason,
        )
        assertNotNull(openOnly.capabilities)

        val noReplacement = engine.managementStatus(
            archive,
            ArchiveRequestedAction.MANAGE,
            hasHostReplacementSession = false,
        )
        assertFalse(noReplacement.isWritable)
        assertEquals(
            ArchiveManagementReadOnlyReason.HOST_REPLACEMENT_UNAVAILABLE,
            noReplacement.readOnlyReason,
        )
    }

    @Test
    fun `format without a mutation provider reports a backend boundary`() {
        val status = ArchiveEngine.DEFAULT.managementStatus(
            snapshot().copy(format = ArchiveFormat.SEVEN_Z),
            ArchiveRequestedAction.MANAGE,
            hasHostReplacementSession = true,
        )

        assertFalse(status.isWritable)
        assertEquals(null, status.capabilities)
        assertEquals(
            ArchiveManagementReadOnlyReason.FORMAT_NOT_SUPPORTED,
            status.readOnlyReason,
        )
    }

    @Test
    fun `ordinary tar uses the same management session and replacement boundary`() {
        val archive = tarSnapshot()

        val writable = ArchiveEngine.DEFAULT.managementStatus(
            archive,
            ArchiveRequestedAction.MANAGE,
            hasHostReplacementSession = true,
        )
        val readOnlyOpen = ArchiveEngine.DEFAULT.managementStatus(
            archive,
            ArchiveRequestedAction.OPEN,
            hasHostReplacementSession = true,
        )

        assertTrue(writable.isWritable)
        assertEquals(TAR_MUTATION_CAPABILITIES, writable.capabilities)
        assertEquals(null, writable.readOnlyReason)
        assertFalse(readOnlyOpen.isWritable)
        assertEquals(
            ArchiveManagementReadOnlyReason.CURRENT_SESSION_READ_ONLY,
            readOnlyOpen.readOnlyReason,
        )
    }

    @Test
    fun `archive specific boundaries take precedence over the session access reason`() {
        val ordinary = snapshot()
        val engine = ArchiveEngine.DEFAULT
        val cases = listOf(
            ordinary.copy(
                volumeIdentities = listOf(ArchiveVolumeIdentity("archive.z01", 1L, 2L)),
            ) to ArchiveManagementReadOnlyReason.MULTI_VOLUME_ARCHIVE,
            ordinary.copy(
                entries = ordinary.entries.mapIndexed { index, entry ->
                    if (index == 0) {
                        entry.copy(pathStatus = ArchiveEntryPathStatus.UNSAFE_ISOLATED)
                    } else {
                        entry
                    }
                },
            ) to ArchiveManagementReadOnlyReason.UNSAFE_ENTRY_PATH,
            ordinary.copy(
                entries = ordinary.entries.mapIndexed { index, entry ->
                    if (index == 0) {
                        entry.copy(
                            isEncrypted = true,
                            capabilities = entry.capabilities.copy(canOpen = false),
                        )
                    } else {
                        entry
                    }
                },
            ) to ArchiveManagementReadOnlyReason.PASSWORD_REQUIRED,
            ordinary.copy(
                entries = ordinary.entries.mapIndexed { index, entry ->
                    if (index == 0) {
                        entry.copy(capabilities = entry.capabilities.copy(canOpen = false))
                    } else {
                        entry
                    }
                },
            ) to ArchiveManagementReadOnlyReason.UNSUPPORTED_ENTRY_METHOD,
            ordinary.copy(
                entries = ordinary.entries.mapIndexed { index, entry ->
                    if (index == 0) {
                        entry.copy(capabilities = entry.capabilities.copy(canDelete = false))
                    } else {
                        entry
                    }
                },
            ) to ArchiveManagementReadOnlyReason.BACKEND_VARIANT_READ_ONLY,
        )

        cases.forEach { (archive, expectedReason) ->
            val status = engine.managementStatus(
                archive,
                ArchiveRequestedAction.OPEN,
                hasHostReplacementSession = true,
            )
            assertFalse(status.isWritable)
            assertEquals(expectedReason, status.readOnlyReason)
            assertEquals(ZIP_MUTATION_CAPABILITIES, status.capabilities)
        }
    }
}
