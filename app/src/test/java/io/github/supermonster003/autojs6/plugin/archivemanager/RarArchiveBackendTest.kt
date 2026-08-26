package io.github.supermonster003.autojs6.plugin.archivemanager

import com.github.junrar.ArchiveOptions as JunrarArchiveOptions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.Base64

class RarArchiveBackendTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `rar capabilities are reader only and do not overclaim sibling volumes`() {
        val capabilities = ArchiveEngine.DEFAULT.capabilities(ArchiveFormat.RAR)

        assertTrue(capabilities.canDetect)
        assertTrue(capabilities.canList)
        assertTrue(capabilities.canPreview)
        assertTrue(capabilities.canOpen)
        assertTrue(capabilities.canExtract)
        assertFalse(capabilities.canCreate)
        assertFalse(capabilities.canAdd)
        assertFalse(capabilities.canDelete)
        assertFalse(capabilities.canRename)
        assertEquals(ArchiveOptionMode.OPTIONAL, capabilities.password)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.splitVolumes)
        assertTrue(capabilities.compressionLevels.isEmpty())
        assertTrue(ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT in capabilities.limitations)
        assertTrue(
            ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE in capabilities.limitations,
        )
        assertEquals("application/vnd.rar", ArchiveFormat.RAR.primaryMimeType)
        assertEquals(setOf("rar"), ArchiveFormat.RAR.extensions)
        assertTrue(ArchiveFormat.RAR.matchesFileName("ARCHIVE.RAR"))
    }

    @Test
    fun `rar4 and rar5 fixtures list and stream exact entry data`() {
        listOf(
            "fixture-rar4.rar" to RAR4_BASE64,
            "fixture-rar5.rar" to RAR5_BASE64,
        ).forEach { (name, encoded) ->
            val source = writeFixture(name, encoded)
            val snapshot = ArchiveScanner().scan(source)

            assertEquals(ArchiveFormat.RAR, snapshot.format)
            assertEquals(1, snapshot.entries.size)
            val entry = snapshot.entries.single()
            assertEquals("restart-idea.ps1", entry.path)
            assertEquals(547L, entry.uncompressedSize)
            assertTrue(entry.canOpen)
            assertTrue(entry.canExtract)
            assertFalse(entry.capabilities.canDelete)
            assertFalse(entry.capabilities.canRename)

            ArchiveEngine.DEFAULT.openReader(source, ArchiveFormat.RAR).use { reader ->
                val bytes = reader.openEntry(reader.entries.single()).use { it.readBytes() }
                assertEquals(EXPECTED_ENTRY_SHA256, bytes.sha256())
            }
        }
    }

    @Test
    fun `rar probe uses structure instead of the file extension`() {
        val source = writeFixture("renamed.bin", RAR5_BASE64)

        val detected = ArchiveEngine.DEFAULT.probe(source)

        assertEquals(ArchiveFormat.RAR, detected.format)
        assertTrue(detected.structurallyVerified)
    }

    @Test
    fun `encrypted rar requests and rejects passwords with typed failures`() {
        val source = writeFixture("encrypted.rar", RAR5_PASSWORD_BASE64)

        ArchiveEngine.DEFAULT.openReader(source, ArchiveFormat.RAR).use { reader ->
            val entry = reader.entries.single()
            assertTrue(entry.isEncrypted)
            assertFalse(entry.capabilities.canOpen)
            assertFalse(entry.capabilities.canExtract)
            expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.PASSWORD_REQUIRED) {
                reader.openEntry(entry)
            }
        }

        val wrongOptions = ArchiveReaderOptions(password = "incorrect".toCharArray())
        try {
            expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.WRONG_PASSWORD) {
                ArchiveScanner().scan(source, wrongOptions)
            }
        } finally {
            wrongOptions.clearPassword()
        }

        val correctOptions = ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray())
        try {
            val snapshot = ArchiveScanner().scan(source, correctOptions)
            try {
                val entry = snapshot.entries.single()
                assertTrue(entry.canOpen)
                assertNull(entry.crc32)
                val output = java.io.ByteArrayOutputStream()
                ArchiveEntryStreamer(source, snapshot).stream(entry, output)
                assertEquals(
                    EXPECTED_ENTRY_SHA256,
                    output.toByteArray().sha256(),
                )
            } finally {
                snapshot.readerOptions.clearPassword()
            }
        } finally {
            correctOptions.clearPassword()
        }
    }

    @Test
    fun `patched junrar construction options wipe retained password arrays`() {
        val sourcePassword = FIXTURE_PASSWORD.toCharArray()
        val builder = JunrarArchiveOptions.builder().password(sourcePassword)
        sourcePassword.fill('\u0000')

        val first = builder.build()
        assertArrayEquals(FIXTURE_PASSWORD.toCharArray(), first.password)
        first.close()
        assertArrayEquals(CharArray(FIXTURE_PASSWORD.length), first.password)

        val second = builder.build()
        assertNull(second.password)
        second.close()
    }

    @Test
    fun `rar signature with truncated metadata is malformed`() {
        val source = temporaryFolder.newFile("truncated.rar").apply {
            writeBytes(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00))
        }

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MALFORMED_ARCHIVE,
        ) {
            ArchiveScanner().scan(source)
        }

        assertEquals(ArchiveFormat.RAR, error.format)
    }

    @Test
    fun `rar suffix without a rar signature remains unrecognized`() {
        val source = temporaryFolder.newFile("plain.rar").apply { writeText("plain text") }

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_SIGNATURE) {
            ArchiveEngine.DEFAULT.probe(source)
        }
    }

    private fun writeFixture(name: String, encoded: String): File =
        temporaryFolder.newFile(name).apply { writeBytes(Base64.getDecoder().decode(encoded)) }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val FIXTURE_PASSWORD = "archive-test-2026"
        const val EXPECTED_ENTRY_SHA256 =
            "8d94ef79e32233d326553de00255a20eb6cdf8a709faa9e38bc00582d42ddbb3"

        const val RAR4_BASE64 =
            "UmFyIRoHAM+QcwAADQAAAAAAAADcaHQgkDUAawEAACMCAAAC/eeUJw2fWVwdMxAAIAAAAHJlc3RhcnQtaWRlYS5wczEA8IPFGQnBEQzL1cGX7oPg+kaFlRsbVS/O/KjUvwVoVCoIidZOlebL0p06NUV93SiEtk2O9zBY+GcaZm5uZneP7/Jm53cZ2H794fqMo2l3f3gDukWpCc/fYxqjfPJJ1TucL4OIgOa9zPH9DucTTtBF7OxE+xyryHfI3euXJ5tTUzY1ll0HXbwT/iSKJkiUGWiPLyeFSgLeFESzqFhnIv2a/P2XA9wFKIVBA683kXjClSfS6LSIQdtpj3MY7QewL8aLjY5/nJgqpUpKAEn4axk8E2aJPc+4P03mzC5jNRtjuavtn8CqSsUDdnUWNUM/X1PQNLIZMsUTNDCHsEUch+kw3lKoKTnaWJjgDOo1FoaDg+wanWJGjav7H9BNIdraj/QTUOUQ14Y1UybZ2vb7hC7cmLRThl/ap3kWgmXVIgaFlir7g5igFKvrmHXWS6w5CLYZDeZ+PBGmFCtIQINS5necFZtWikZVaTL6ptQFYq38oMQ9ewBABwA="
        const val RAR5_BASE64 =
            "UmFyIRoHAQDz4YLrCwEFBwAGAQGAgIAAcmz7pCwCAwvwAgSjBCD955QngAMAEHJlc3RhcnQtaWRlYS5wczEKAwIDrZLFTabcAcz7bAE2dTMzL1cFXvEjwdqmS1wjUx/C3XurIwt0AZLCWEhCE9bLiGulyllMCA94yEuOUq1vi8tVkibf58475G3xucbcjcf33j+02nsh4+Ii86HNWzV6XMzZ41UUd57FFiKBcijw9cSvlUHNVMCXdv8ViLPS7OqNJbDgwCDeHo8ubk7dqnX8Yj5WAqbAOloFL1dPrWSV/IuZymgy+yYrtq37sBfhAiYFwE8NPumz5xtH86AerlKG+yccxZrxfZO9hUC1wW1Y+K2ljEYQK/akaiyiVFCLsHCMK09IN5jFeeqtobX/CrAyyVUu1OtbH4LAhFVOpGZTNM+i+TFjmnoV3GLajhIEfhCZGuQtTTFGUTBRvjBfJl6x9j3x60TJeT8U2GZUJ0AM7JO0KcbQ2cvuUPrANXkeiZ+1vbO5aIat0kcqZMtU4uwKEE8+tIvzchh8vKD4s5/CufHOyVbdhSLMJvVFACX4oBBtsu0foPxEEjHoHXdWUQMFBAA="
        const val RAR5_PASSWORD_BASE64 =
            "UmFyIRoHAQDz4YLrCwEFBwAGAQGAgIAA/ZLm7V0CAzzwAgSjBCAXsntogAMAEHJlc3RhcnQtaWRlYS5wczEwAQADDx1iK+7VSoF9F3BXhlsgXxX4pRvlGPkLvbp+la5UF0dDE+Z90iABVYq3ldcjCgMCA62SxU2m3AHdwupGT+l+YQg5PIvYxdzw3Ev1uJa3/UGCEmuqy3XOaBxQtLIN7EnPIRr3BI1C84dIzcBPuZW/cH9icSQySVWnBa4N/TQ/zRyqDpEj1BSQjD8SgDUTd2UnhHv9mIIFaHun4lrf1p1JQy6q7Ip5Qo6J5IQRpfGZUyFohqMF2O/lPgNt0p3rfcI6vB43n1YLDuDq9izSrQe0XChqzx/HNCZjW+cT5alIrN2MT8XnTIOsHZb8jKqcP2VE0nOqA7ahecvkfwo2neXBNdmRVZKXFM+xO1nDbRr8pY4QBnN6cMxflK7BE0wNXh4DXuQTSIZEI67f9aX1bLpl5UFQqh0Ill/s7vV3fTwhNVbo8FBL1E/renlX784MuLNKaCegO3aV171q5Tv5YT4Qj72/m+svhaDJ+y6uJ2L4/12b6tAMsrXubbzYqEgbJw9pN6pVDY9xRVeZO2o4bF7Z51N6CWp5zcuZ23PBH/96EYx8y4Nk/G/CHR13VlEDBQQA"
    }
}
