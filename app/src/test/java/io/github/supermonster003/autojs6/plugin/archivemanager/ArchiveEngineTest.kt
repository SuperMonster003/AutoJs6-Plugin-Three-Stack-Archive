package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.tukaani.xz.LZMA2Options
import java.io.ByteArrayOutputStream
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
        assertTrue(capabilities.supports(ArchiveOperation.ADD))
        assertTrue(capabilities.supports(ArchiveOperation.DELETE))
        assertTrue(capabilities.supports(ArchiveOperation.RENAME))
        assertEquals(ArchiveOptionMode.OPTIONAL, capabilities.password)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
        assertEquals(ArchiveOptionMode.OPTIONAL, capabilities.splitVolumes)
        assertTrue(capabilities.canReadSplitVolumes)
        assertEquals((0..9).toList(), capabilities.compressionLevels)
        assertTrue(Charset.forName("GB18030").name() in capabilities.filenameCharsetNames)
        assertTrue(Charset.forName("IBM437").name() in capabilities.filenameCharsetNames)
        assertEquals(
            listOf(
                ArchiveFormat.ZIP,
                ArchiveFormat.SEVEN_Z,
                ArchiveFormat.RAR,
                ArchiveFormat.TAR,
                ArchiveFormat.TAR_GZIP,
                ArchiveFormat.TAR_XZ,
                ArchiveFormat.TAR_BZIP2,
                ArchiveFormat.TAR_ZSTD,
            ),
            engine.readableFormats,
        )
        assertEquals(
            engine.readableFormats.filterNot { it == ArchiveFormat.RAR },
            engine.creatableFormats,
        )
        assertEquals(
            engine.readableFormats.flatMap(ArchiveFormat::catalogExtensions).toSet(),
            ArchiveManagerPlugin.EXTENSIONS.toSet(),
        )
        assertEquals(
            engine.readableFormats.flatMap(ArchiveFormat::mimeTypes).toSet(),
            ArchiveManagerPlugin.MIME_TYPES.toSet(),
        )
        assertEquals(
            engine.readableFormats.flatMap(ArchiveFormat::catalogFileNameSuffixes).toSet() +
                NumberedArchiveVolumePolicy.fileNameSuffixes.toSet(),
            ArchiveManagerPlugin.FILE_NAME_SUFFIXES.toSet(),
        )
        assertFalse(ArchiveFormatLimitation.PASSWORD_UNAVAILABLE in capabilities.limitations)
        assertFalse(ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE in capabilities.limitations)
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
            engine.mutationFormats,
        )
        assertEquals(
            listOf("7z", "tar", "tbz2", "tgz", "txz", "tzst", "zip"),
            ArchiveManagerPlugin.MANAGE_EXTENSIONS.toList(),
        )
        assertEquals(
            listOf("tar.bz2", "tar.gz", "tar.xz", "tar.zst"),
            ArchiveManagerPlugin.MANAGE_FILE_NAME_SUFFIXES.toList(),
        )
        val mutation = requireNotNull(engine.mutationCapabilities(ArchiveFormat.ZIP))
        assertEquals(
            setOf(ArchiveOperation.ADD, ArchiveOperation.DELETE, ArchiveOperation.RENAME),
            mutation.operations,
        )
        assertEquals(ArchiveMutationStrategy.FULL_REWRITE, mutation.strategy)
        assertEquals(8, mutation.minimumHostProtocolVersion)
        assertTrue(
            ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED in mutation.metadataEffects,
        )
        assertTrue(
            ArchiveMutationMetadataEffect.UNIX_ATTRIBUTES_DROPPED in mutation.metadataEffects,
        )
    }

    @Test
    fun `ordinary and compressed tar wrappers expose rewrite mutation`() {
        val engine = ArchiveEngine.DEFAULT

        listOf(
            ArchiveFormat.TAR,
            ArchiveFormat.TAR_GZIP,
            ArchiveFormat.TAR_XZ,
            ArchiveFormat.TAR_BZIP2,
            ArchiveFormat.TAR_ZSTD,
        ).forEach { format ->
            val capabilities = engine.capabilities(format)
            val canMutate = format == ArchiveFormat.TAR ||
                format == ArchiveFormat.TAR_GZIP ||
                format == ArchiveFormat.TAR_XZ ||
                format == ArchiveFormat.TAR_BZIP2 ||
                format == ArchiveFormat.TAR_ZSTD
            assertTrue(capabilities.supports(ArchiveOperation.DETECT))
            assertTrue(capabilities.supports(ArchiveOperation.LIST))
            assertTrue(capabilities.supports(ArchiveOperation.PREVIEW))
            assertTrue(capabilities.supports(ArchiveOperation.OPEN))
            assertTrue(capabilities.supports(ArchiveOperation.EXTRACT))
            assertTrue(capabilities.supports(ArchiveOperation.CREATE))
            assertEquals(canMutate, capabilities.supports(ArchiveOperation.ADD))
            assertEquals(canMutate, capabilities.supports(ArchiveOperation.DELETE))
            assertEquals(canMutate, capabilities.supports(ArchiveOperation.RENAME))
            assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.password)
            assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
            assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.splitVolumes)
            assertFalse(capabilities.canReadSplitVolumes)
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
                    ArchiveFormat.RAR,
                    -> error("Non-TAR format is outside this assertion")
                },
                capabilities.compressionLevels,
            )
            assertTrue(capabilities.filenameCharsetNames.isEmpty())
            assertTrue(ArchiveFormatLimitation.PASSWORD_UNAVAILABLE in capabilities.limitations)
            assertTrue(ArchiveFormatLimitation.MUTATION_REQUIRES_REWRITE in capabilities.limitations)
            assertEquals(
                when (format) {
                    ArchiveFormat.TAR -> TAR_MUTATION_CAPABILITIES
                    ArchiveFormat.TAR_GZIP -> TAR_GZIP_MUTATION_CAPABILITIES
                    ArchiveFormat.TAR_XZ -> TAR_XZ_MUTATION_CAPABILITIES
                    ArchiveFormat.TAR_BZIP2 -> TAR_BZIP2_MUTATION_CAPABILITIES
                    ArchiveFormat.TAR_ZSTD -> TAR_ZSTD_MUTATION_CAPABILITIES
                    else -> null
                },
                engine.mutationCapabilities(format),
            )
        }
        assertEquals(
            setOf(ArchiveOperation.ADD, ArchiveOperation.DELETE, ArchiveOperation.RENAME),
            TAR_MUTATION_CAPABILITIES.operations,
        )
        assertEquals(ArchiveMutationStrategy.FULL_REWRITE, TAR_MUTATION_CAPABILITIES.strategy)
        assertEquals(8, TAR_MUTATION_CAPABILITIES.minimumHostProtocolVersion)
        assertEquals(
            setOf(
                ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED,
                ArchiveMutationMetadataEffect.UNIX_ATTRIBUTES_DROPPED,
            ),
            TAR_MUTATION_CAPABILITIES.metadataEffects,
        )
        assertEquals(
            TAR_MUTATION_CAPABILITIES.metadataEffects +
                ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
            TAR_GZIP_MUTATION_CAPABILITIES.metadataEffects,
        )
        assertEquals(
            TAR_MUTATION_CAPABILITIES.metadataEffects +
                ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
            TAR_XZ_MUTATION_CAPABILITIES.metadataEffects,
        )
        assertEquals(
            TAR_MUTATION_CAPABILITIES.metadataEffects +
                ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
            TAR_BZIP2_MUTATION_CAPABILITIES.metadataEffects,
        )
        assertEquals(
            TAR_MUTATION_CAPABILITIES.metadataEffects +
                ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED,
            TAR_ZSTD_MUTATION_CAPABILITIES.metadataEffects,
        )
    }

    @Test
    fun `xz mutation preset stays within its explicit encoder memory budget`() {
        val options = LZMA2Options(TAR_XZ_MUTATION_COMPRESSION_LEVEL)

        assertEquals(4, tarMutationCompressionLevel(ArchiveFormat.TAR_XZ))
        assertEquals(4 * 1_024 * 1_024, options.dictSize)
        assertEquals(48_058, options.encoderMemoryUsage)
        assertTrue(options.encoderMemoryUsage <= TAR_XZ_MUTATION_MAX_ENCODER_MEMORY_KIB)
    }

    @Test
    fun `bzip2 mutation preset stays within its explicit worst case encoder memory budget`() {
        val estimatedBytes = estimateTarBzip2MutationEncoderMemoryBytes(
            TAR_BZIP2_MUTATION_COMPRESSION_LEVEL,
        )

        assertEquals(6, tarMutationCompressionLevel(ArchiveFormat.TAR_BZIP2))
        assertEquals(8_324_288L, estimatedBytes)
        assertTrue(estimatedBytes <= TAR_BZIP2_MUTATION_MAX_ENCODER_MEMORY_BYTES)
    }

    @Test
    fun `zstd mutation preset stays within its audited single threaded encoder memory budget`() {
        val estimatedBytes = estimateTarZstdMutationEncoderMemoryBytes()

        assertEquals(3, tarMutationCompressionLevel(ArchiveFormat.TAR_ZSTD))
        assertEquals(20, TAR_ZSTD_MUTATION_WINDOW_LOG)
        assertEquals(2_746_400L, estimatedBytes)
        assertTrue(estimatedBytes <= TAR_ZSTD_MUTATION_MAX_ENCODER_MEMORY_BYTES)

        val encoded = ByteArrayOutputStream().also { target ->
            TarArchiveCompression.openOutput(
                format = ArchiveFormat.TAR_ZSTD,
                output = target,
                compressionLevel = TAR_ZSTD_MUTATION_COMPRESSION_LEVEL,
                zstdWindowLog = TAR_ZSTD_MUTATION_WINDOW_LOG,
            ).use { it.write(ByteArray(2 * 1_024 * 1_024)) }
        }.toByteArray()
        val magic = byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte())
        assertArrayEquals(magic, encoded.copyOf(magic.size))
        assertTrue(encoded[4].toInt() and 0x04 != 0)
        assertEquals(0x50, encoded[5].toInt() and 0xFF)
    }

    @Test
    fun `7z capabilities expose dynamic non-solid rewrite without overclaiming`() {
        val capabilities = ArchiveEngine.DEFAULT.capabilities(ArchiveFormat.SEVEN_Z)

        assertTrue(capabilities.supports(ArchiveOperation.DETECT))
        assertTrue(capabilities.supports(ArchiveOperation.LIST))
        assertTrue(capabilities.supports(ArchiveOperation.PREVIEW))
        assertTrue(capabilities.supports(ArchiveOperation.OPEN))
        assertTrue(capabilities.supports(ArchiveOperation.EXTRACT))
        assertTrue(capabilities.supports(ArchiveOperation.CREATE))
        assertTrue(capabilities.supports(ArchiveOperation.ADD))
        assertTrue(capabilities.supports(ArchiveOperation.DELETE))
        assertTrue(capabilities.supports(ArchiveOperation.RENAME))
        assertEquals(
            SEVEN_Z_MUTATION_CAPABILITIES,
            ArchiveEngine.DEFAULT.mutationCapabilities(ArchiveFormat.SEVEN_Z),
        )
        assertEquals(ArchiveOptionMode.OPTIONAL, capabilities.password)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.filenameEncryption)
        assertEquals(ArchiveOptionMode.UNSUPPORTED, capabilities.splitVolumes)
        assertTrue(capabilities.canReadSplitVolumes)
        assertEquals((0..9).toList(), capabilities.compressionLevels)
        assertTrue(capabilities.filenameCharsetNames.isEmpty())
        assertTrue(ArchiveFormatLimitation.ENTRY_METHOD_DEPENDENT in capabilities.limitations)
        assertTrue(
            ArchiveFormatLimitation.SOLID_CREATION_UNAVAILABLE in capabilities.limitations,
        )
        assertTrue(
            ArchiveFormatLimitation.FILENAME_ENCRYPTION_UNAVAILABLE in capabilities.limitations,
        )
        assertTrue(
            ArchiveFormatLimitation.SPLIT_CREATION_UNAVAILABLE in capabilities.limitations,
        )
        assertFalse(
            ArchiveFormatLimitation.SPLIT_VOLUMES_UNAVAILABLE in capabilities.limitations,
        )
        assertEquals("application/x-7z-compressed", ArchiveFormat.SEVEN_Z.primaryMimeType)
        assertEquals(setOf("7z"), ArchiveFormat.SEVEN_Z.extensions)
        assertTrue(ArchiveFormat.SEVEN_Z.matchesFileName("ARCHIVE.7Z"))
    }

    @Test
    fun `7z mutation preset stays within explicit encoder and decoder memory budgets`() {
        val options = LZMA2Options(SEVEN_Z_MUTATION_COMPRESSION_LEVEL)

        assertEquals(3, sevenZMutationCompressionLevel())
        assertEquals(4 * 1_024 * 1_024, options.dictSize)
        assertEquals(31_410, options.encoderMemoryUsage)
        assertTrue(options.encoderMemoryUsage <= SEVEN_Z_MUTATION_MAX_ENCODER_MEMORY_KIB)
        assertEquals(64 * 1_024, SEVEN_Z_MUTATION_MAX_DECODER_MEMORY_KIB)
    }

    @Test
    fun `compressed tar formats publish exact compound suffixes without generic stream leaves`() {
        assertEquals("application/x-tar", ArchiveFormat.TAR.primaryMimeType)
        assertEquals("application/x-compressed-tar", ArchiveFormat.TAR_GZIP.primaryMimeType)
        assertEquals("application/x-xz-compressed-tar", ArchiveFormat.TAR_XZ.primaryMimeType)
        assertEquals("application/x-bzip2-compressed-tar", ArchiveFormat.TAR_BZIP2.primaryMimeType)
        assertEquals("application/x-zstd-compressed-tar", ArchiveFormat.TAR_ZSTD.primaryMimeType)
        assertEquals(setOf("tar.gz", "tgz"), ArchiveFormat.TAR_GZIP.extensions)
        assertEquals(setOf("tgz"), ArchiveFormat.TAR_GZIP.catalogExtensions)
        assertEquals(setOf("tar.gz"), ArchiveFormat.TAR_GZIP.catalogFileNameSuffixes)
        assertEquals(setOf("tar.xz", "txz"), ArchiveFormat.TAR_XZ.extensions)
        assertEquals(setOf("txz"), ArchiveFormat.TAR_XZ.catalogExtensions)
        assertEquals(setOf("tar.xz"), ArchiveFormat.TAR_XZ.catalogFileNameSuffixes)
        assertEquals(setOf("tar.bz2", "tbz2"), ArchiveFormat.TAR_BZIP2.extensions)
        assertEquals(setOf("tbz2"), ArchiveFormat.TAR_BZIP2.catalogExtensions)
        assertEquals(setOf("tar.bz2"), ArchiveFormat.TAR_BZIP2.catalogFileNameSuffixes)
        assertEquals(setOf("tar.zst", "tzst"), ArchiveFormat.TAR_ZSTD.extensions)
        assertEquals(setOf("tzst"), ArchiveFormat.TAR_ZSTD.catalogExtensions)
        assertEquals(setOf("tar.zst"), ArchiveFormat.TAR_ZSTD.catalogFileNameSuffixes)
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
            assertTrue(file.capabilities.canDelete)
            assertTrue(file.capabilities.canRename)
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
