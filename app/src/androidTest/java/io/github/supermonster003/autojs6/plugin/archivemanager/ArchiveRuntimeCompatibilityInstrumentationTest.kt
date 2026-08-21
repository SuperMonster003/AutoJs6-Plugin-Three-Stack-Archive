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
            ZipArchiveAccess.open(source, snapshot.zipCharsetName).use { zipFile ->
                val zipEntry = requireNotNull(zipFile.getEntry(entry.sourceName))
                assertArrayEquals(expected, zipFile.getInputStream(zipEntry).use { it.readBytes() })
            }
        } finally {
            source.delete()
        }
    }
}
