package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipEntry

class ArchiveEngineTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `zip capabilities describe only implemented operations`() {
        val engine = ArchiveEngine.DEFAULT
        val capabilities = engine.capabilities(ArchiveFormat.ZIP)

        assertTrue(capabilities.supports(ArchiveOperation.DETECT))
        assertTrue(capabilities.supports(ArchiveOperation.LIST))
        assertTrue(capabilities.supports(ArchiveOperation.PREVIEW))
        assertTrue(capabilities.supports(ArchiveOperation.OPEN))
        assertTrue(capabilities.supports(ArchiveOperation.EXTRACT))
        assertTrue(capabilities.supports(ArchiveOperation.CREATE))
        assertFalse(capabilities.supports(ArchiveOperation.ADD))
        assertFalse(capabilities.supports(ArchiveOperation.DELETE))
        assertFalse(capabilities.supports(ArchiveOperation.RENAME))
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.password)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.splitVolumes)
        assertEquals((0..9).toList(), capabilities.compressionLevels)
        assertEquals(listOf(ArchiveFormat.ZIP), engine.readableFormats)
        assertEquals(listOf(ArchiveFormat.ZIP), engine.creatableFormats)
        assertEquals(ArchiveFormat.ZIP.extensions, ArchiveManagerPlugin.EXTENSIONS.toSet())
        assertEquals(ArchiveFormat.ZIP.mimeTypes, ArchiveManagerPlugin.MIME_TYPES.toSet())
    }

    @Test
    fun `probe detects the real zip format without relying on the file extension`() {
        val source = writeZip(
            temporaryFolder.newFile("renamed.bin"),
            FixtureEntry("payload.txt", "payload".toByteArray(), ZipEntry.STORED),
        )

        val detected = ArchiveEngine.DEFAULT.probe(source)

        assertEquals(ArchiveFormat.ZIP, detected.format)
        assertTrue(detected.structurallyVerified)
        assertTrue(detected.capabilities.canList)
    }

    @Test
    fun `reader exposes generic metadata capabilities and entry data`() {
        val expected = "engine payload".toByteArray()
        val source = writeZip(
            temporaryFolder.newFile("reader.zip"),
            FixtureEntry("folder/", method = ZipEntry.STORED),
            FixtureEntry("folder/payload.txt", expected, ZipEntry.DEFLATED),
        )

        ArchiveEngine.DEFAULT.openReader(source).use { reader ->
            assertEquals(ArchiveFormat.ZIP, reader.format)
            assertEquals(2, reader.entries.size)
            val directory = reader.entries.first()
            val file = reader.entries.last()
            assertFalse(directory.capabilities.canOpen)
            assertTrue(directory.capabilities.canExtract)
            assertTrue(
                ArchiveEntryLimitation.DIRECTORY_HAS_NO_DATA in directory.capabilities.limitations,
            )
            assertTrue(file.capabilities.canOpen)
            assertTrue(file.capabilities.canExtract)
            assertFalse(file.capabilities.canDelete)
            assertEquals(ArchiveCompressionMethod.DEFLATED, file.compressionMethod)
            assertEquals("8", file.compressionMethodId)
            assertArrayEquals(expected, reader.openEntry(file).use { it.readBytes() })
        }
    }

    @Test
    fun `probe reports an unrecognized structure instead of trusting a zip suffix`() {
        val source = temporaryFolder.newFile("not-an-archive.zip").apply {
            writeText("plain text")
        }

        expectArchiveFailure<ArchiveValidationException>(ArchiveFailureCode.INVALID_SIGNATURE) {
            ArchiveEngine.DEFAULT.probe(source)
        }
    }
}
