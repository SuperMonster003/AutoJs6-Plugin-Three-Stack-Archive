package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArchiveFormatCompatibilityMatrixTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `android package aliases are structurally verified ZIP archives`() {
        listOf("apk", "apks", "xapk", "apkm", "apkz", "aab").forEach { extension ->
            val source = copyFixture("7zip-22-deflate-unicode.zip", "package.${extension.uppercase()}")
            val detected = ArchiveEngine.DEFAULT.probe(source)
            assertEquals(ArchiveFormat.ZIP, detected.format)
            assertTrue(detected.structurallyVerified)
            assertTrue(ArchiveFormat.ZIP.matchesFileName(source.name))
            assertTrue(ThreeStackArchivePlugin.EXTENSIONS.contains(extension))
            assertTrue(!ThreeStackArchivePlugin.MANAGE_EXTENSIONS.contains(extension))
            assertTrue(!ThreeStackArchivePlugin.canModifyFileName(source.name))
            assertTrue(ArchiveScanner().scan(source).entries.isNotEmpty())
        }
    }

    @Test
    fun `every readable format is detected with canonical missing and misleading extensions`() {
        val fixtures = detectionFixtures()

        assertEquals(ArchiveEngine.DEFAULT.readableFormats.toSet(), fixtures.map { it.format }.toSet())
        fixtures.forEach { fixture ->
            listOf(
                NameCase(fixture.resourceName, expectedNameMatch = true),
                NameCase("${fixture.format.id}-extensionless", expectedNameMatch = false),
                NameCase(fixture.misleadingName, expectedNameMatch = false),
            ).forEach { nameCase ->
                val source = copyFixture(fixture.resourceName, nameCase.displayName)
                val detected = ArchiveEngine.DEFAULT.probe(source)

                assertEquals("${fixture.format} from ${nameCase.displayName}", fixture.format, detected.format)
                assertTrue("${fixture.format} should be structurally verified", detected.structurallyVerified)
                assertEquals(
                    "${fixture.format} name match for ${nameCase.displayName}",
                    nameCase.expectedNameMatch,
                    fixture.format.matchesFileName(nameCase.displayName),
                )
                if (!nameCase.expectedNameMatch && '.' in nameCase.displayName) {
                    val misleadingFormats = ArchiveEngine.DEFAULT.readableFormats.filter { format ->
                        format.matchesFileName(nameCase.displayName)
                    }
                    assertEquals("misleading suffix for ${nameCase.displayName}", 1, misleadingFormats.size)
                    assertTrue(fixture.format !in misleadingFormats)
                }
            }
        }
    }

    @Test
    fun `every readable format reports signature preserving damage at the index stage`() {
        val fixtures = detectionFixtures()

        assertEquals(ArchiveEngine.DEFAULT.readableFormats.toSet(), fixtures.map { it.format }.toSet())
        fixtures.forEach { fixture ->
            val source = copyFixture(
                fixture.resourceName,
                "damaged-${fixture.format.id}.data",
            )
            source.writeBytes(signaturePreservingDamage(fixture.format, source.readBytes()))

            val error = try {
                ArchiveScanner().scan(source)
                throw AssertionError("${fixture.format} damaged sample was accepted")
            } catch (failure: ArchiveValidationException) {
                failure
            }

            assertEquals(
                "${fixture.format} damaged code",
                ArchiveFailureCode.MALFORMED_ARCHIVE,
                error.code,
            )
            assertEquals("${fixture.format} damaged format", fixture.format, error.format)
            assertEquals("${fixture.format} damaged stage", ArchiveFailureStage.INDEX, error.stage)
        }
    }

    private fun detectionFixtures(): List<DetectionFixture> = listOf(
        DetectionFixture(
            format = ArchiveFormat.ZIP,
            resourceName = "windows-explorer-11-deflate-unicode.zip",
            misleadingName = "zip-content.rar",
        ),
        DetectionFixture(
            format = ArchiveFormat.SEVEN_Z,
            resourceName = "7zip-22-lzma2-solid-unicode.7z",
            misleadingName = "7z-content.zip",
        ),
        DetectionFixture(
            format = ArchiveFormat.RAR,
            resourceName = "winrar-6.10-rar5-unicode.rar",
            misleadingName = "rar-content.7z",
        ),
        DetectionFixture(
            format = ArchiveFormat.TAR,
            resourceName = "7zip-22-ustar-unicode.tar",
            misleadingName = "tar-content.zip",
        ),
        DetectionFixture(
            format = ArchiveFormat.TAR_GZIP,
            resourceName = "7zip-22-ustar-unicode.tar.gz",
            misleadingName = "tar-gzip-content.tar.xz",
        ),
        DetectionFixture(
            format = ArchiveFormat.TAR_XZ,
            resourceName = "7zip-22-ustar-unicode.tar.xz",
            misleadingName = "tar-xz-content.tar.bz2",
        ),
        DetectionFixture(
            format = ArchiveFormat.TAR_BZIP2,
            resourceName = "7zip-22-ustar-unicode.tar.bz2",
            misleadingName = "tar-bzip2-content.tar.zst",
        ),
        DetectionFixture(
            format = ArchiveFormat.TAR_ZSTD,
            resourceName = "bsdtar-3.8.4-ustar-unicode.tar.zst",
            misleadingName = "tar-zstd-content.tar.gz",
        ),
    )

    private fun signaturePreservingDamage(format: ArchiveFormat, bytes: ByteArray): ByteArray =
        when (format) {
            ArchiveFormat.ZIP -> bytes.copyOf(bytes.size - ZIP_TRAILER_BYTES_TO_REMOVE)
            ArchiveFormat.SEVEN_Z -> bytes.copyOf(SEVEN_Z_TRUNCATED_BYTES)
            ArchiveFormat.RAR -> bytes.copyOf(RAR5_SIGNATURE_BYTES)
            ArchiveFormat.TAR -> bytes.clone().apply {
                this[TAR_SECOND_HEADER_OFFSET] =
                    (this[TAR_SECOND_HEADER_OFFSET].toInt() xor 0x01).toByte()
            }
            ArchiveFormat.TAR_GZIP -> bytes.copyOf(GZIP_HEADER_BYTES)
            ArchiveFormat.TAR_XZ -> bytes.copyOf(XZ_HEADER_BYTES)
            ArchiveFormat.TAR_BZIP2 -> bytes.copyOf(BZIP2_HEADER_BYTES)
            ArchiveFormat.TAR_ZSTD -> bytes.copyOf(ZSTD_HEADER_BYTES)
        }

    private fun copyFixture(resourceName: String, targetName: String): File {
        val target = temporaryFolder.newFile(targetName)
        val resource = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("archive-fixtures/$resourceName"),
        )
        resource.use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    private data class DetectionFixture(
        val format: ArchiveFormat,
        val resourceName: String,
        val misleadingName: String,
    )

    private data class NameCase(
        val displayName: String,
        val expectedNameMatch: Boolean,
    )

    private companion object {
        const val ZIP_TRAILER_BYTES_TO_REMOVE = 12
        const val SEVEN_Z_TRUNCATED_BYTES = 20
        const val RAR5_SIGNATURE_BYTES = 8
        const val TAR_SECOND_HEADER_OFFSET = 1_024
        const val GZIP_HEADER_BYTES = 4
        const val XZ_HEADER_BYTES = 6
        const val BZIP2_HEADER_BYTES = 4
        const val ZSTD_HEADER_BYTES = 4
    }
}
