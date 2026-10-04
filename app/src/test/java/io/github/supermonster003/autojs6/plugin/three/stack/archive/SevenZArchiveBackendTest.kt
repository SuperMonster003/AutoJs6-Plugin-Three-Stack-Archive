package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

class SevenZArchiveBackendTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `external fixture hashes match the compatibility manifest`() {
        FIXTURE_HASHES.forEach { (name, expectedHash) ->
            assertEquals(name, expectedHash, copyFixture(name).sha256())
        }
    }

    @Test
    fun `scans and streams an external solid lzma2 archive`() {
        val source = copyFixture(SOLID_FIXTURE)
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.SEVEN_Z, snapshot.format)
        assertEquals(
            setOf("empty.bin", "plain.txt", "资料", "资料/说明.md"),
            snapshot.entries.map(ArchiveEntry::path).toSet(),
        )
        assertTrue(snapshot.entries.none(ArchiveEntry::isEncrypted))
        assertTrue(
            snapshot.entries.filterNot(ArchiveEntry::isDirectory).all(ArchiveEntry::canExtract),
        )
        assertTrue(snapshot.entries.none { it.capabilities.canDelete })
        assertTrue(snapshot.entries.none { it.capabilities.canRename })
        assertTrue(
            snapshot.entries.all {
                ArchiveEntryLimitation.SOLID_COMPRESSION in it.capabilities.limitations
            },
        )
        assertEquals(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            ArchiveEngine.DEFAULT.mutationAvailability(snapshot).unavailableReason,
        )
        assertTrue(
            snapshot.entries.filter { it.uncompressedSize > 0L }.all {
                it.compressionMethod == ArchiveCompressionMethod.LZMA2 &&
                    "LZMA2" in it.compressionMethodId
            },
        )
        assertTrue(
            snapshot.entries.filter { it.uncompressedSize > 0L }.all {
                it.compressedSize == -1L
            },
        )

        val entry = snapshot.entries.single { it.path == "plain.txt" }
        assertEquals(
            listOf("资料", "empty.bin", "plain.txt", "资料/说明.md"),
            snapshot.entries.map(ArchiveEntry::path),
        )
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(entry, output)

        assertArrayEquals(EXPECTED_PLAIN_BYTES, output.toByteArray())
    }

    @Test
    fun `reads an external bcj and lzma2 method pipeline`() {
        val source = copyFixture(BCJ_FIXTURE)
        val snapshot = ArchiveScanner().scan(source)
        val entry = snapshot.entries.single()

        assertEquals(ArchiveCompressionMethod.LZMA2, entry.compressionMethod)
        assertTrue("BCJ_X86_FILTER" in entry.compressionMethodId)
        assertTrue("LZMA2" in entry.compressionMethodId)
        assertTrue(entry.canExtract)
        assertTrue(entry.capabilities.canDelete)
        assertTrue(entry.capabilities.canRename)
        assertTrue(ArchiveEngine.DEFAULT.mutationAvailability(snapshot).isAvailable)

        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(entry, output)
        assertArrayEquals(EXPECTED_PLAIN_BYTES, output.toByteArray())
    }

    @Test
    fun `content encrypted archive requires a password and verifies it while reading`() {
        val source = copyFixture(CONTENT_ENCRYPTED_FIXTURE)

        val lockedSnapshot = ArchiveScanner().scan(source)
        val lockedEntry = lockedSnapshot.entries.single { it.path == "plain.txt" }
        assertTrue(lockedEntry.isEncrypted)
        assertFalse(lockedEntry.canExtract)
        assertEquals(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            ArchiveEngine.DEFAULT.mutationAvailability(lockedSnapshot).unavailableReason,
        )
        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.PASSWORD_REQUIRED) {
            ArchiveEntryStreamer(source, lockedSnapshot).stream(
                lockedEntry,
                ByteArrayOutputStream(),
            )
        }

        val wrongSnapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(password = "wrong-password".toCharArray()),
        )
        val wrongEntry = wrongSnapshot.entries.single { it.path == "plain.txt" }
        assertTrue(wrongEntry.isEncrypted)
        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.WRONG_PASSWORD) {
            ArchiveEntryStreamer(source, wrongSnapshot).stream(
                wrongEntry,
                ByteArrayOutputStream(),
            )
        }
        wrongSnapshot.readerOptions.clearPassword()

        val correctSnapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray()),
        )
        assertTrue(
            correctSnapshot.entries.filter { it.uncompressedSize > 0L }.all {
                it.isEncrypted && it.encryptionMethod == ArchiveEncryptionMethod.AES
            },
        )
        assertEquals(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            ArchiveEngine.DEFAULT.mutationAvailability(correctSnapshot).unavailableReason,
        )
        val correctEntry = correctSnapshot.entries.single { it.path == "plain.txt" }
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, correctSnapshot).stream(correctEntry, output)
        assertArrayEquals(EXPECTED_PLAIN_BYTES, output.toByteArray())
        correctSnapshot.readerOptions.clearPassword()
    }

    @Test
    fun `header encrypted archive classifies absent and incorrect passwords`() {
        val source = copyFixture(HEADER_ENCRYPTED_FIXTURE)

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.PASSWORD_REQUIRED) {
            ArchiveScanner().scan(source)
        }
        val wrong = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.WRONG_PASSWORD,
        ) {
            ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(password = "wrong-password".toCharArray()),
            )
        }
        assertEquals(ArchiveFailureStage.PASSWORD, wrong.stage)

        val snapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray()),
        )
        assertEquals(ArchiveFormat.SEVEN_Z, snapshot.format)
        assertTrue(snapshot.entries.any(ArchiveEntry::isEncrypted))
        assertEquals(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            ArchiveEngine.DEFAULT.mutationAvailability(snapshot).unavailableReason,
        )
        val entry = snapshot.entries.single { it.path == "plain.txt" }
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(entry, output)
        assertArrayEquals(EXPECTED_PLAIN_BYTES, output.toByteArray())
        snapshot.readerOptions.clearPassword()
    }

    @Test
    fun `generated non-solid archive exposes mutation only inside the proven boundary`() {
        val source = writeSevenZ(
            temporaryFolder.newFile("ordinary.7z"),
            SevenZFixtureEntry("docs", isDirectory = true),
            SevenZFixtureEntry("docs/readme.txt", "readme".encodeToByteArray()),
            SevenZFixtureEntry("empty.bin"),
        )
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.SEVEN_Z, snapshot.format)
        assertTrue(snapshot.entries.all { it.capabilities.canDelete })
        assertTrue(snapshot.entries.all { it.capabilities.canRename })
        assertTrue(
            snapshot.entries.none {
                ArchiveEntryLimitation.SOLID_COMPRESSION in it.capabilities.limitations ||
                    ArchiveEntryLimitation.MUTATION_UNAVAILABLE in it.capabilities.limitations
            },
        )
        assertEquals(
            SEVEN_Z_MUTATION_CAPABILITIES,
            ArchiveEngine.DEFAULT.mutationAvailability(snapshot).capabilities,
        )

        val plan = SevenZArchiveMutationPlanner.plan(
            snapshot,
            ArchiveMutationRequest.Rename("docs", "manual"),
        )
        assertEquals(
            listOf("manual", "manual/readme.txt", "empty.bin"),
            plan.entries.map(ArchiveRewriteEntry::archivePath),
        )
        assertEquals(6L, plan.workEstimate.knownContentBytesToRead)
        assertEquals(
            setOf(
                ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED,
                ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
            ),
            plan.metadataEffects,
        )
    }

    @Test
    fun `mutation decoder budget is enforced independently from ordinary reading`() {
        val source = writeSevenZ(
            temporaryFolder.newFile("ordinary.7z"),
            SevenZFixtureEntry("payload.txt", "payload".encodeToByteArray()),
        )
        val readable = ArchiveScanner().scan(source)
        val overBudget = readable.copy(
            entries = readable.entries.map { entry ->
                entry.copy(
                    capabilities = entry.capabilities.copy(
                        canDelete = false,
                        canRename = false,
                        limitations = entry.capabilities.limitations + setOf(
                            ArchiveEntryLimitation.MUTATION_RESOURCE_BUDGET_EXCEEDED,
                            ArchiveEntryLimitation.MUTATION_UNAVAILABLE,
                        ),
                    ),
                )
            },
        )

        assertTrue(overBudget.entries.all(ArchiveEntry::canOpen))
        assertEquals(
            ArchiveMutationUnavailableReason.BACKEND_VARIANT_READ_ONLY,
            ArchiveEngine.DEFAULT.mutationAvailability(overBudget).unavailableReason,
        )
    }

    @Test
    fun `detects 7z by structure and distinguishes malformed data from a missing signature`() {
        val renamed = copyFixture(SOLID_FIXTURE, "renamed.bin")
        assertEquals(ArchiveFormat.SEVEN_Z, ArchiveEngine.DEFAULT.probe(renamed).format)

        val truncated = temporaryFolder.newFile("truncated.7z").apply {
            writeBytes(renamed.readBytes().copyOf(20))
        }
        val malformed = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MALFORMED_ARCHIVE,
        ) {
            ArchiveScanner().scan(truncated)
        }
        assertEquals(ArchiveFormat.SEVEN_Z, malformed.format)

        val plain = temporaryFolder.newFile("plain.7z").apply { writeText("not an archive") }
        val invalid = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.INVALID_SIGNATURE,
        ) {
            ArchiveScanner().scan(plain)
        }
        assertFalse(invalid.format == ArchiveFormat.SEVEN_Z)
    }

    private fun copyFixture(name: String, targetName: String = name): File {
        val target = temporaryFolder.newFile(targetName)
        val resource = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("archive-fixtures/$name"),
        )
        resource.use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val SOLID_FIXTURE = "7zip-22-lzma2-solid-unicode.7z"
        const val CONTENT_ENCRYPTED_FIXTURE = "7zip-22-aes256-solid-unicode.7z"
        const val HEADER_ENCRYPTED_FIXTURE = "7zip-22-aes256-header-unicode.7z"
        const val BCJ_FIXTURE = "7zip-22-bcj-lzma2.7z"
        const val FIXTURE_PASSWORD = "archive-secret"
        val EXPECTED_PLAIN_BYTES = "external seven zip payload\n".toByteArray()
        val FIXTURE_HASHES = mapOf(
            SOLID_FIXTURE to
                "094c9ecd3800481b0d2dac228e72d8bb07a680150c59305e82f9c508a2f267a3",
            CONTENT_ENCRYPTED_FIXTURE to
                "2dbf44e00c280129b6542c45f0cae2e6dc96aa225202a2f3355595e4225c2d50",
            HEADER_ENCRYPTED_FIXTURE to
                "8c3b5edf2663302a5199ae81a5d4a307a8a10854fa3bcb48da9460f619c5faa3",
            BCJ_FIXTURE to
                "1bcf2d38631441b91e47c23b8bc67704797e159d27537c7dc49d4d38dce98a1a",
        )
    }
}
