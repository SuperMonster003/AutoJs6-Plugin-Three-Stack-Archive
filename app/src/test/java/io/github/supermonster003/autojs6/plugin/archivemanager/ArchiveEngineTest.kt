package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.Charset
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
        assertEquals(ArchiveOptionMode.OPTIONAL, capabilities.password)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.splitVolumes)
        assertEquals((0..9).toList(), capabilities.compressionLevels)
        assertTrue(Charset.forName("GB18030").name() in capabilities.filenameCharsetNames)
        assertTrue(Charset.forName("IBM437").name() in capabilities.filenameCharsetNames)
        assertEquals(
            listOf(
                ArchiveFormat.ZIP,
                ArchiveFormat.SEVEN_Z,
                ArchiveFormat.TAR,
                ArchiveFormat.TAR_GZIP,
                ArchiveFormat.TAR_XZ,
                ArchiveFormat.TAR_BZIP2,
                ArchiveFormat.TAR_ZSTD,
            ),
            engine.readableFormats,
        )
        assertEquals(engine.readableFormats, engine.creatableFormats)
        assertEquals(
            engine.readableFormats.flatMap(ArchiveFormat::catalogExtensions).toSet(),
            ArchiveManagerPlugin.EXTENSIONS.toSet(),
        )
        assertEquals(
            engine.readableFormats.flatMap(ArchiveFormat::mimeTypes).toSet(),
            ArchiveManagerPlugin.MIME_TYPES.toSet(),
        )
        assertFalse(ArchiveFormatLimitation.PASSWORD_UNAVAILABLE in capabilities.limitations)
    }

    @Test
    fun `tar capabilities expose creation without claiming mutation or encryption`() {
        val engine = ArchiveEngine.DEFAULT

        listOf(
            ArchiveFormat.TAR,
            ArchiveFormat.TAR_GZIP,
            ArchiveFormat.TAR_XZ,
            ArchiveFormat.TAR_BZIP2,
            ArchiveFormat.TAR_ZSTD,
        ).forEach { format ->
            val capabilities = engine.capabilities(format)
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
            assertEquals(
                when (format) {
                    ArchiveFormat.TAR -> listOf(0)
                    ArchiveFormat.TAR_GZIP -> (0..9).toList()
                    ArchiveFormat.TAR_XZ,
                    ArchiveFormat.TAR_BZIP2,
                    ArchiveFormat.TAR_ZSTD,
                    -> (1..9).toList()
                    ArchiveFormat.ZIP,
                    ArchiveFormat.SEVEN_Z,
                    -> error("Non-TAR format is outside this assertion")
                },
                capabilities.compressionLevels,
            )
            assertTrue(capabilities.filenameCharsetNames.isEmpty())
            assertTrue(ArchiveFormatLimitation.PASSWORD_UNAVAILABLE in capabilities.limitations)
            assertTrue(ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE in capabilities.limitations)
        }
    }

    @Test
    fun `7z capabilities expose solid reading and non-solid creation without overclaiming`() {
        val capabilities = ArchiveEngine.DEFAULT.capabilities(ArchiveFormat.SEVEN_Z)

        assertTrue(capabilities.supports(ArchiveOperation.DETECT))
        assertTrue(capabilities.supports(ArchiveOperation.LIST))
        assertTrue(capabilities.supports(ArchiveOperation.PREVIEW))
        assertTrue(capabilities.supports(ArchiveOperation.OPEN))
        assertTrue(capabilities.supports(ArchiveOperation.EXTRACT))
        assertTrue(capabilities.supports(ArchiveOperation.CREATE))
        assertFalse(capabilities.supports(ArchiveOperation.ADD))
        assertFalse(capabilities.supports(ArchiveOperation.DELETE))
        assertFalse(capabilities.supports(ArchiveOperation.RENAME))
        assertEquals(ArchiveOptionMode.OPTIONAL, capabilities.password)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.splitVolumes)
        assertEquals((0..9).toList(), capabilities.compressionLevels)
        assertTrue(capabilities.filenameCharsetNames.isEmpty())
        assertTrue(ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT in capabilities.limitations)
        assertTrue(
            ArchiveFormatLimitation.SOLID_CREATION_UNAVAILABLE in capabilities.limitations,
        )
        assertTrue(
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE in capabilities.limitations,
        )
        assertEquals("application/x-7z-compressed", ArchiveFormat.SEVEN_Z.primaryMimeType)
        assertEquals(setOf("7z"), ArchiveFormat.SEVEN_Z.extensions)
        assertTrue(ArchiveFormat.SEVEN_Z.matchesFileName("ARCHIVE.7Z"))
    }

    @Test
    fun `compressed tar formats retain compound suffixes and publish host leaf extensions`() {
        assertEquals("application/x-tar", ArchiveFormat.TAR.primaryMimeType)
        assertEquals("application/x-compressed-tar", ArchiveFormat.TAR_GZIP.primaryMimeType)
        assertEquals("application/x-xz-compressed-tar", ArchiveFormat.TAR_XZ.primaryMimeType)
        assertEquals("application/x-bzip2-compressed-tar", ArchiveFormat.TAR_BZIP2.primaryMimeType)
        assertEquals("application/x-zstd-compressed-tar", ArchiveFormat.TAR_ZSTD.primaryMimeType)
        assertEquals(setOf("tar.gz", "tgz"), ArchiveFormat.TAR_GZIP.extensions)
        assertEquals(setOf("gz", "tgz"), ArchiveFormat.TAR_GZIP.catalogExtensions)
        assertEquals(setOf("tar.xz", "txz"), ArchiveFormat.TAR_XZ.extensions)
        assertEquals(setOf("xz", "txz"), ArchiveFormat.TAR_XZ.catalogExtensions)
        assertEquals(setOf("tar.bz2", "tbz2"), ArchiveFormat.TAR_BZIP2.extensions)
        assertEquals(setOf("bz2", "tbz2"), ArchiveFormat.TAR_BZIP2.catalogExtensions)
        assertEquals(setOf("tar.zst", "tzst"), ArchiveFormat.TAR_ZSTD.extensions)
        assertEquals(setOf("zst", "tzst"), ArchiveFormat.TAR_ZSTD.catalogExtensions)
        assertTrue(ArchiveFormat.TAR_GZIP.matchesFileName("ARCHIVE.TAR.GZ"))
        assertTrue(ArchiveFormat.TAR_XZ.matchesFileName("archive.txz"))
        assertTrue(ArchiveFormat.TAR_BZIP2.matchesFileName("archive.TAR.BZ2"))
        assertTrue(ArchiveFormat.TAR_ZSTD.matchesFileName("archive.tzst"))
        assertFalse(ArchiveFormat.TAR_GZIP.matchesFileName("standalone.gz"))
        assertFalse(ArchiveFormat.TAR_XZ.matchesFileName("standalone.xz"))
        assertFalse(ArchiveFormat.TAR_BZIP2.matchesFileName("standalone.bz2"))
        assertFalse(ArchiveFormat.TAR_ZSTD.matchesFileName("standalone.zst"))
        assertEquals("archive", ArchiveFormat.TAR_GZIP.baseNameWithoutArchiveExtension("archive.TAR.GZ"))
        assertEquals("archive", ArchiveFormat.TAR_XZ.baseNameWithoutArchiveExtension("archive.txz"))
        assertEquals("archive", ArchiveFormat.TAR_BZIP2.baseNameWithoutArchiveExtension("archive.tar.bz2"))
        assertEquals("archive", ArchiveFormat.TAR_ZSTD.baseNameWithoutArchiveExtension("archive.TZST"))
        assertEquals(null, ArchiveFormat.TAR_ZSTD.baseNameWithoutArchiveExtension("archive.bin"))
    }

    @Test
    fun `reader options never expose a password in diagnostics`() {
        val password = "do-not-expose-this-password"
        val options = ArchiveReaderOptions(
            filenameCharsetName = "UTF-8",
            password = password.toCharArray(),
        )

        assertTrue(options.hasPassword)
        assertFalse(options.toString().contains(password))
        assertTrue(options.toString().contains("hasPassword=true"))
        options.clearPassword()
        assertFalse(options.hasPassword)
    }

    @Test
    fun `reader close clears its password copy without mutating caller options`() {
        val source = writeZip(
            temporaryFolder.newFile("reader-password-lifecycle.zip"),
            FixtureEntry("payload.txt", "payload".toByteArray(), ZipEntry.STORED),
        )
        val callerOptions = ArchiveReaderOptions(password = "transient-password".toCharArray())
        val reader = ArchiveEngine.DEFAULT.openReader(source, options = callerOptions)
        val readerOptions = reader.options

        assertTrue(callerOptions.hasPassword)
        assertTrue(readerOptions.hasPassword)

        reader.close()

        assertTrue(callerOptions.hasPassword)
        assertFalse(readerOptions.hasPassword)
        callerOptions.clearPassword()
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

    @Test
    fun `signature probe finds a valid zip after a long executable style preamble`() {
        val source = writeZip(
            temporaryFolder.newFile("long-preamble.zip"),
            FixtureEntry("payload.txt", "payload".toByteArray(), ZipEntry.STORED),
        )
        source.writeBytes(ByteArray(150_000) { 0x4D } + source.readBytes())

        assertTrue(ZipArchiveAccess.hasZipSignature(source))
        assertEquals(ArchiveFormat.ZIP, ArchiveEngine.DEFAULT.probe(source).format)
    }
}
