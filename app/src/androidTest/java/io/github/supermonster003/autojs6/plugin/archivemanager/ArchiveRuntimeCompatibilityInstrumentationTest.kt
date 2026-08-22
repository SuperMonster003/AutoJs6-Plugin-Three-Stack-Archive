@file:Suppress("DEPRECATION")

package io.github.supermonster003.autojs6.plugin.archivemanager

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
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
}
