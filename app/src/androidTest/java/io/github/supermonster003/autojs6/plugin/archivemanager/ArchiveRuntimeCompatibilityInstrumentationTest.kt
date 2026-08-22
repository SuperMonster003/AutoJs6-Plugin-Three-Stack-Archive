@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ArchiveRuntimeCompatibilityInstrumentationTest {

    @Test
    fun zipMetadataAndEntryDataAreReadableOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "runtime-${UUID.randomUUID()}.zip")
        val expected = "archive-runtime-check".toByteArray()

        try {
            ZipOutputStream(FileOutputStream(source)).use { output ->
                output.putNextEntry(ZipEntry("目录/hello.txt"))
                output.write(expected)
                output.closeEntry()
            }

            val snapshot = ArchiveScanner().scan(source)
            val entry = snapshot.entries.single()

            assertEquals("目录/hello.txt", entry.path)
            assertTrue(entry.canExtract)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    @Test
    fun manualLegacyFilenameEncodingIsReusedOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "legacy-runtime-${UUID.randomUUID()}.zip")
        val charset = Charset.forName("GB18030")
        val expected = "兼容内容".toByteArray()

        try {
            ZipOutputStream(FileOutputStream(source), charset).use { output ->
                output.putNextEntry(ZipEntry("目录/文件.txt"))
                output.write(expected)
                output.closeEntry()
            }

            val snapshot = ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(filenameCharsetName = charset.name()),
            )
            val entry = snapshot.entries.single()

            assertEquals(charset.name(), snapshot.readerOptions.filenameCharsetName)
            assertEquals("目录/文件.txt", entry.path)
            ArchiveEngine.DEFAULT.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }

    @Test
    fun aesAndZipCryptoEntriesAreReadableOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = "encrypted-runtime-check".toByteArray()

        listOf(EncryptionMethod.AES, EncryptionMethod.ZIP_STANDARD).forEach { encryption ->
            val source = File(context.cacheDir, "encrypted-${encryption.name}-${UUID.randomUUID()}.zip")
            val payload = File(context.cacheDir, "payload-${UUID.randomUUID()}.txt")
            try {
                payload.writeBytes(expected)
                val parameters = ZipParameters().apply {
                    fileNameInZip = "payload.txt"
                    isEncryptFiles = true
                    encryptionMethod = encryption
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
                ZipFile(source, TEST_PASSWORD.toCharArray()).use { archive ->
                    archive.addFile(payload, parameters)
                }

                val locked = ArchiveScanner().scan(source).entries.single()
                assertTrue(locked.isEncrypted)
                assertTrue(!locked.canExtract)

                val unlocked = ArchiveScanner().scan(
                    source,
                    ArchiveReaderOptions(password = TEST_PASSWORD.toCharArray()),
                )
                val entry = unlocked.entries.single()
                assertTrue(entry.canExtract)
                ArchiveEngine.DEFAULT.openReader(
                    source = source,
                    format = unlocked.format,
                    options = unlocked.readerOptions,
                ).use { reader ->
                    val liveEntry = requireNotNull(reader.entryAt(entry.ordinal))
                    assertArrayEquals(expected, reader.openEntry(liveEntry).use { it.readBytes() })
                }
            } finally {
                payload.delete()
                source.delete()
            }
        }
    }

    private companion object {
        const val TEST_PASSWORD = "ArchiveManager-Test-2026"
    }
}
