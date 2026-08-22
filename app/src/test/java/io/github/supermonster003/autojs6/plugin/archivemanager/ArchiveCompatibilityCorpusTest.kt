package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ArchiveCompatibilityCorpusTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `7-Zip 22 sample preserves external metadata names and content`() {
        val source = copyFixture("7zip-22-deflate-unicode.zip")

        assertEquals(EXPECTED_7ZIP_SHA256, source.sha256())
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
        assertEquals(listOf("ascii.txt", "文件.txt"), snapshot.entries.map(ArchiveEntry::path))
        assertEquals(
            listOf(ArchiveCompressionMethod.DEFLATED, ArchiveCompressionMethod.STORED),
            snapshot.entries.map(ArchiveEntry::compressionMethod),
        )
        val unicodeEntry = snapshot.entries.last()
        val output = java.io.ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(unicodeEntry, output)
        assertTrue(output.toString(Charsets.UTF_8.name()).contains("UTF-8 文件名"))
    }

    @Test
    fun `7-Zip 22 tar sample preserves unicode names and content`() {
        val source = copyFixture(TAR_FIXTURE)

        assertEquals(EXPECTED_7ZIP_TAR_SHA256, source.sha256())
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.TAR, snapshot.format)
        assertEquals(listOf("ascii.txt", "文件.txt"), snapshot.entries.map(ArchiveEntry::path))
        assertTrue(snapshot.entries.all { it.compressionMethod == ArchiveCompressionMethod.STORED })
        assertTrue(snapshot.entries.all { it.compressionMethodId == "TAR" })
        assertTrue(snapshot.entries.all { it.crc32 == null })
        val unicodeEntry = snapshot.entries.last()
        val output = java.io.ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(unicodeEntry, output)
        assertTrue(output.toString(Charsets.UTF_8.name()).contains("UTF-8 文件名"))
    }

    @Test
    fun `7-Zip 22 compressed tar samples preserve unicode names and content`() {
        listOf(
            CompressedTarFixture(
                name = TAR_GZIP_FIXTURE,
                sha256 = EXPECTED_7ZIP_TAR_GZIP_SHA256,
                format = ArchiveFormat.TAR_GZIP,
            ),
            CompressedTarFixture(
                name = TAR_XZ_FIXTURE,
                sha256 = EXPECTED_7ZIP_TAR_XZ_SHA256,
                format = ArchiveFormat.TAR_XZ,
            ),
        ).forEach { fixture ->
            val source = copyFixture(fixture.name)

            assertEquals(fixture.sha256, source.sha256())
            val snapshot = ArchiveScanner().scan(source)

            assertEquals(fixture.format, snapshot.format)
            assertEquals(listOf("ascii.txt", "文件.txt"), snapshot.entries.map(ArchiveEntry::path))
            assertTrue(snapshot.entries.all { it.compressionMethod == ArchiveCompressionMethod.STORED })
            assertTrue(snapshot.entries.all { it.compressionMethodId == "TAR" })
            assertTrue(snapshot.entries.all { it.crc32 == null })
            val unicodeEntry = snapshot.entries.last()
            val output = java.io.ByteArrayOutputStream()
            ArchiveEntryStreamer(source, snapshot).stream(unicodeEntry, output)
            assertTrue(output.toString(Charsets.UTF_8.name()).contains("UTF-8 文件名"))
        }
    }

    @Test
    fun `truncated external sample reports index damage instead of an unknown failure`() {
        val source = copyFixture("7zip-22-deflate-unicode.zip")
        source.writeBytes(source.readBytes().dropLast(12).toByteArray())

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MALFORMED_ARCHIVE,
        ) {
            ArchiveScanner().scan(source)
        }

        assertEquals(ArchiveFormat.ZIP, error.format)
        assertEquals(ArchiveFailureStage.INDEX, error.stage)
    }

    @Test
    fun `encrypted external samples remain browseable without a password`() {
        listOf(
            AES_FIXTURE to ArchiveEncryptionMethod.AES,
            ZIP_CRYPTO_FIXTURE to ArchiveEncryptionMethod.ZIP_CRYPTO,
        ).forEach { (fixture, encryptionMethod) ->
            val source = copyFixture(fixture)
            val snapshot = ArchiveScanner().scan(source)

            assertEquals(listOf("ascii.txt", "文件.txt"), snapshot.entries.map(ArchiveEntry::path))
            assertFalse(snapshot.readerOptions.hasPassword)
            assertTrue(snapshot.entries.all(ArchiveEntry::isEncrypted))
            assertEquals(
                "$fixture encryption metadata",
                List(snapshot.entries.size) { encryptionMethod },
                snapshot.entries.map(ArchiveEntry::encryptionMethod),
            )
            assertTrue(snapshot.entries.none(ArchiveEntry::canExtract))
            assertTrue(snapshot.entries.all {
                ArchiveEntryLimitation.ENCRYPTED in it.capabilities.limitations
            })
        }
    }

    @Test
    fun `AES and ZipCrypto external samples open with the recorded password`() {
        listOf(
            AES_FIXTURE to ArchiveEncryptionMethod.AES,
            ZIP_CRYPTO_FIXTURE to ArchiveEncryptionMethod.ZIP_CRYPTO,
        ).forEach { (fixture, encryptionMethod) ->
            val source = copyFixture(fixture)
            val snapshot = ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray()),
            )

            assertTrue(snapshot.readerOptions.hasPassword)
            assertEquals(
                "$fixture extraction capabilities",
                List(snapshot.entries.size) { true },
                snapshot.entries.map(ArchiveEntry::canExtract),
            )
            assertEquals(
                "$fixture encryption metadata",
                List(snapshot.entries.size) { encryptionMethod },
                snapshot.entries.map(ArchiveEntry::encryptionMethod),
            )
            val unicodeEntry = snapshot.entries.single { it.path == "文件.txt" }
            val output = java.io.ByteArrayOutputStream()
            ArchiveEntryStreamer(source, snapshot).stream(unicodeEntry, output)
            assertTrue(output.toString(Charsets.UTF_8.name()).contains("UTF-8 文件名"))
        }
    }

    @Test
    fun `encrypted external samples report a stable wrong password failure`() {
        listOf(AES_FIXTURE, ZIP_CRYPTO_FIXTURE).forEach { fixture ->
            val error = expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.WRONG_PASSWORD,
            ) {
                ArchiveScanner().scan(
                    copyFixture(fixture),
                    ArchiveReaderOptions(password = "wrong-password".toCharArray()),
                )
            }

            assertEquals(ArchiveFormat.ZIP, error.format)
            assertEquals(ArchiveFailureStage.PASSWORD, error.stage)
        }
    }

    private fun copyFixture(name: String): File {
        val target = temporaryFolder.newFile(name)
        val resource = requireNotNull(javaClass.classLoader?.getResourceAsStream("archive-fixtures/$name"))
        resource.use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val EXPECTED_7ZIP_SHA256 =
            "af0b0186ec1605f5f2b640816b85586336b1e3d9b46f85fbedc238ae042c9c0c"
        const val EXPECTED_7ZIP_TAR_SHA256 =
            "771eaf4fc2bef962e4bed64ee109d51d6ef4e25ec95357e5343c882dbb59e403"
        const val EXPECTED_7ZIP_TAR_GZIP_SHA256 =
            "a0a6719169340f5ec6a2147a37e3320a352ec5332951340007c4a96e0e115a9c"
        const val EXPECTED_7ZIP_TAR_XZ_SHA256 =
            "9d929770fddbcce1e114e38e9198fd6b3827a29cbf8a54763859574e05319a22"
        const val AES_FIXTURE = "7zip-22-aes256-unicode.zip"
        const val ZIP_CRYPTO_FIXTURE = "7zip-22-zipcrypto-unicode.zip"
        const val TAR_FIXTURE = "7zip-22-ustar-unicode.tar"
        const val TAR_GZIP_FIXTURE = "7zip-22-ustar-unicode.tar.gz"
        const val TAR_XZ_FIXTURE = "7zip-22-ustar-unicode.tar.xz"
        const val FIXTURE_PASSWORD = "ArchiveManager-Test-2026"
    }

    private data class CompressedTarFixture(
        val name: String,
        val sha256: String,
        val format: ArchiveFormat,
    )
}
