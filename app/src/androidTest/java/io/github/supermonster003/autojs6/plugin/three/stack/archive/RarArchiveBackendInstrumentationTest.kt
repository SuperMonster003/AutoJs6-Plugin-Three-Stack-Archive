package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class RarArchiveBackendInstrumentationTest {

    @Test
    fun rar4DescriptorReaderStreamsExactDataOnTheDeviceRuntime() {
        val source = writeFixture("runtime-rar4.rar", RAR4_BASE64)
        try {
            val snapshot = ArchiveScanner().scan(source)
            try {
                assertEquals(ArchiveFormat.RAR, snapshot.format)
                val entry = snapshot.entries.single()
                assertEquals("restart-idea.ps1", entry.path)
                assertTrue(entry.canOpen)
                val output = ByteArrayOutputStream()
                ArchiveEntryStreamer(source, snapshot).stream(entry, output)
                assertEquals(EXPECTED_ENTRY_SHA256, output.toByteArray().sha256())
            } finally {
                snapshot.readerOptions.clearPassword()
            }
        } finally {
            assertTrue(source.delete())
        }
    }

    @Test
    fun encryptedRar5UsesTypedRetriesWithoutCrossSessionKeyRetention() {
        val source = writeFixture("runtime-encrypted-rar5.rar", RAR5_PASSWORD_BASE64)
        try {
            val withoutPassword = ArchiveScanner().scan(source)
            try {
                val entry = withoutPassword.entries.single()
                assertTrue(entry.isEncrypted)
                assertFalse(entry.canExtract)
            } finally {
                withoutPassword.readerOptions.clearPassword()
            }

            val wrongOptions = ArchiveReaderOptions(password = "incorrect".toCharArray())
            try {
                try {
                    ArchiveScanner().scan(source, wrongOptions)
                    fail("Expected a wrong-password failure")
                } catch (error: ArchiveValidationException) {
                    assertEquals(ArchiveFailureCode.WRONG_PASSWORD, error.code)
                    assertEquals(ArchiveFailureStage.PASSWORD, error.stage)
                }
            } finally {
                wrongOptions.clearPassword()
            }

            repeat(2) {
                val correctOptions = ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray())
                try {
                    val snapshot = ArchiveScanner().scan(source, correctOptions)
                    try {
                        val entry = snapshot.entries.single()
                        assertTrue(entry.canOpen)
                        val output = ByteArrayOutputStream()
                        ArchiveEntryStreamer(source, snapshot).stream(entry, output)
                        assertEquals(EXPECTED_ENTRY_SHA256, output.toByteArray().sha256())
                    } finally {
                        snapshot.readerOptions.clearPassword()
                    }
                } finally {
                    correctOptions.clearPassword()
                }
            }
        } finally {
            assertTrue(source.delete())
        }
    }

    private fun writeFixture(name: String, encoded: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.cacheDir, name).apply {
            writeBytes(Base64.decode(encoded, Base64.DEFAULT))
        }
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val FIXTURE_PASSWORD = "archive-test-2026"
        const val EXPECTED_ENTRY_SHA256 =
            "8d94ef79e32233d326553de00255a20eb6cdf8a709faa9e38bc00582d42ddbb3"

        const val RAR4_BASE64 =
            "UmFyIRoHAM+QcwAADQAAAAAAAADcaHQgkDUAawEAACMCAAAC/eeUJw2fWVwdMxAAIAAAAHJlc3RhcnQtaWRlYS5wczEA8IPFGQnBEQzL1cGX7oPg+kaFlRsbVS/O/KjUvwVoVCoIidZOlebL0p06NUV93SiEtk2O9zBY+GcaZm5uZneP7/Jm53cZ2H794fqMo2l3f3gDukWpCc/fYxqjfPJJ1TucL4OIgOa9zPH9DucTTtBF7OxE+xyryHfI3euXJ5tTUzY1ll0HXbwT/iSKJkiUGWiPLyeFSgLeFESzqFhnIv2a/P2XA9wFKIVBA683kXjClSfS6LSIQdtpj3MY7QewL8aLjY5/nJgqpUpKAEn4axk8E2aJPc+4P03mzC5jNRtjuavtn8CqSsUDdnUWNUM/X1PQNLIZMsUTNDCHsEUch+kw3lKoKTnaWJjgDOo1FoaDg+wanWJGjav7H9BNIdraj/QTUOUQ14Y1UybZ2vb7hC7cmLRThl/ap3kWgmXVIgaFlir7g5igFKvrmHXWS6w5CLYZDeZ+PBGmFCtIQINS5necFZtWikZVaTL6ptQFYq38oMQ9ewBABwA="
        const val RAR5_PASSWORD_BASE64 =
            "UmFyIRoHAQDz4YLrCwEFBwAGAQGAgIAA/ZLm7V0CAzzwAgSjBCAXsntogAMAEHJlc3RhcnQtaWRlYS5wczEwAQADDx1iK+7VSoF9F3BXhlsgXxX4pRvlGPkLvbp+la5UF0dDE+Z90iABVYq3ldcjCgMCA62SxU2m3AHdwupGT+l+YQg5PIvYxdzw3Ev1uJa3/UGCEmuqy3XOaBxQtLIN7EnPIRr3BI1C84dIzcBPuZW/cH9icSQySVWnBa4N/TQ/zRyqDpEj1BSQjD8SgDUTd2UnhHv9mIIFaHun4lrf1p1JQy6q7Ip5Qo6J5IQRpfGZUyFohqMF2O/lPgNt0p3rfcI6vB43n1YLDuDq9izSrQe0XChqzx/HNCZjW+cT5alIrN2MT8XnTIOsHZb8jKqcP2VE0nOqA7ahecvkfwo2neXBNdmRVZKXFM+xO1nDbRr8pY4QBnN6cMxflK7BE0wNXh4DXuQTSIZEI67f9aX1bLpl5UFQqh0Ill/s7vV3fTwhNVbo8FBL1E/renlX784MuLNKaCegO3aV171q5Tv5YT4Qj72/m+svhaDJ+y6uJ2L4/12b6tAMsrXubbzYqEgbJw9pN6pVDY9xRVeZO2o4bF7Z51N6CWp5zcuZ23PBH/96EYx8y4Nk/G/CHR13VlEDBQQA"
    }
}
