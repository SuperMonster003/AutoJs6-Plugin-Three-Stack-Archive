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
    fun `Bandizip 7 sample preserves legacy Windows separators and unicode names`() {
        val source = copyFixture(BANDIZIP_ZIP_FIXTURE)

        assertEquals(EXPECTED_BANDIZIP_ZIP_SHA256, source.sha256())
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
        assertEquals(listOf("ascii.txt", "目录/文件.txt"), snapshot.entries.map(ArchiveEntry::path))
        assertFixtureText(snapshot, source, "目录/文件.txt")
    }

    @Test
    fun `WinRAR 6 ZIP sample preserves unicode names and content`() {
        val source = copyFixture(WINRAR_ZIP_FIXTURE)

        assertEquals(EXPECTED_WINRAR_ZIP_SHA256, source.sha256())
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
        assertEquals(listOf("ascii.txt", "文件.txt"), snapshot.entries.map(ArchiveEntry::path))
        assertFixtureText(snapshot, source, "文件.txt")
    }

    @Test
    fun `WinRAR plain RAR4 and RAR5 samples preserve unicode names and content`() {
        listOf(
            WINRAR_RAR4_FIXTURE to EXPECTED_WINRAR_RAR4_SHA256,
            WINRAR_RAR5_FIXTURE to EXPECTED_WINRAR_RAR5_SHA256,
        ).forEach { (fixture, expectedSha256) ->
            val source = copyFixture(fixture)

            assertEquals(expectedSha256, source.sha256())
            val snapshot = ArchiveScanner().scan(source)

            assertEquals("$fixture format", ArchiveFormat.RAR, snapshot.format)
            assertEquals(
                "$fixture paths",
                listOf("ascii.txt", "文件.txt"),
                snapshot.entries.map(ArchiveEntry::path),
            )
            assertTrue(snapshot.entries.all(ArchiveEntry::canExtract))
            assertFixtureText(snapshot, source, "文件.txt")
        }
    }

    @Test
    fun `WinRAR encrypted RAR5 sample recovers through typed password failures`() {
        val source = copyFixture(WINRAR_RAR5_AES_FIXTURE)
        assertEquals(EXPECTED_WINRAR_RAR5_AES_SHA256, source.sha256())

        val withoutPassword = ArchiveScanner().scan(source)
        assertEquals(ArchiveFormat.RAR, withoutPassword.format)
        assertTrue(withoutPassword.entries.all(ArchiveEntry::isEncrypted))
        assertTrue(withoutPassword.entries.none(ArchiveEntry::canExtract))

        val wrong = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.WRONG_PASSWORD,
        ) {
            ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(password = "wrong-password".toCharArray()),
            )
        }
        assertEquals(ArchiveFormat.RAR, wrong.format)
        assertEquals(ArchiveFailureStage.PASSWORD, wrong.stage)

        val snapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray()),
        )
        try {
            assertTrue(snapshot.entries.all(ArchiveEntry::canExtract))
            assertTrue(snapshot.entries.all { it.crc32 == null })
            assertFixtureText(snapshot, source, "文件.txt")
        } finally {
            snapshot.readerOptions.clearPassword()
        }
    }

    @Test
    fun `WinRAR header encrypted RAR5 sample requires a password before listing`() {
        val source = copyFixture(WINRAR_RAR5_HEADER_AES_FIXTURE)
        assertEquals(EXPECTED_WINRAR_RAR5_HEADER_AES_SHA256, source.sha256())

        val required = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.PASSWORD_REQUIRED,
        ) {
            ArchiveScanner().scan(source)
        }
        assertEquals(ArchiveFormat.RAR, required.format)
        assertEquals(ArchiveFailureStage.PASSWORD, required.stage)

        val wrong = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.WRONG_PASSWORD,
        ) {
            ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(password = "wrong-password".toCharArray()),
            )
        }
        assertEquals(ArchiveFormat.RAR, wrong.format)

        val snapshot = ArchiveScanner().scan(
            source,
            ArchiveReaderOptions(password = FIXTURE_PASSWORD.toCharArray()),
        )
        try {
            assertEquals(listOf("ascii.txt", "文件.txt"), snapshot.entries.map(ArchiveEntry::path))
            assertFixtureText(snapshot, source, "文件.txt")
        } finally {
            snapshot.readerOptions.clearPassword()
        }
    }

    @Test
    fun `WinRAR split set exposes first-volume metadata without claiming extraction`() {
        val source = copyFixture(WINRAR_RAR5_SPLIT_FIRST_FIXTURE)
        assertEquals(EXPECTED_WINRAR_RAR5_SPLIT_FIRST_SHA256, source.sha256())

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.RAR, snapshot.format)
        assertTrue(snapshot.entries.isNotEmpty())
        assertTrue(snapshot.entries.none(ArchiveEntry::canExtract))
        assertTrue(snapshot.entries.all { entry ->
            ArchiveEntryLimitation.MISSING_VOLUME in entry.capabilities.limitations
        })
        ArchiveEngine.DEFAULT.openReader(source, ArchiveFormat.RAR).use { reader ->
            expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.MISSING_VOLUME) {
                reader.openEntry(reader.entries.first())
            }
        }
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
    fun `external compressed tar samples preserve unicode names and content`() {
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
            CompressedTarFixture(
                name = TAR_BZIP2_FIXTURE,
                sha256 = EXPECTED_7ZIP_TAR_BZIP2_SHA256,
                format = ArchiveFormat.TAR_BZIP2,
            ),
            CompressedTarFixture(
                name = TAR_ZSTD_FIXTURE,
                sha256 = EXPECTED_BSDTAR_TAR_ZSTD_SHA256,
                format = ArchiveFormat.TAR_ZSTD,
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

    private fun assertFixtureText(snapshot: ArchiveSnapshot, source: File, path: String) {
        val output = java.io.ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(snapshot.entries.single { it.path == path }, output)
        assertTrue(output.toString(Charsets.UTF_8.name()).contains("UTF-8 文件名"))
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val EXPECTED_7ZIP_SHA256 =
            "af0b0186ec1605f5f2b640816b85586336b1e3d9b46f85fbedc238ae042c9c0c"
        const val EXPECTED_BANDIZIP_ZIP_SHA256 =
            "61ef38cc532dc9112a85e427a02fd5cc1e9d6b9ec1830039311359ac5b165b0c"
        const val EXPECTED_WINRAR_ZIP_SHA256 =
            "487938029f4282fdfe731d790592f18c6f5ebc93aff7c6309dee2cf5fa419e00"
        const val EXPECTED_WINRAR_RAR4_SHA256 =
            "a12fc3e2f14a946ecb983efb3a4c808909ef22f9e150d0d2ace61e0dd95047ea"
        const val EXPECTED_WINRAR_RAR5_SHA256 =
            "c009be6f80980f6bba3856125f5e107d93fbd30fcaa87b22d0d6a78c67026599"
        const val EXPECTED_WINRAR_RAR5_AES_SHA256 =
            "05cdd95a68d8a5a1afac7b9961d0323e7a1a9fa6224418f388ba0cba918d7f7f"
        const val EXPECTED_WINRAR_RAR5_HEADER_AES_SHA256 =
            "d27f8ddc47eca0777cdbe00819bb69f740f60b1dc1d757af5e6656b779177007"
        const val EXPECTED_WINRAR_RAR5_SPLIT_FIRST_SHA256 =
            "404bc4db3a217e9d7eaaa03ac483fd6502289680bfc2af404bc74d572116bae4"
        const val EXPECTED_7ZIP_TAR_SHA256 =
            "771eaf4fc2bef962e4bed64ee109d51d6ef4e25ec95357e5343c882dbb59e403"
        const val EXPECTED_7ZIP_TAR_GZIP_SHA256 =
            "a0a6719169340f5ec6a2147a37e3320a352ec5332951340007c4a96e0e115a9c"
        const val EXPECTED_7ZIP_TAR_XZ_SHA256 =
            "9d929770fddbcce1e114e38e9198fd6b3827a29cbf8a54763859574e05319a22"
        const val EXPECTED_7ZIP_TAR_BZIP2_SHA256 =
            "fd1b55270cda192d065c4d9ba352b64baaf94f52d4442ac362308113ac4aebae"
        const val EXPECTED_BSDTAR_TAR_ZSTD_SHA256 =
            "de581c580bd873817ab3cf5d2311c623ded577cfaa7e0096eb7d6c4c0db3655a"
        const val AES_FIXTURE = "7zip-22-aes256-unicode.zip"
        const val ZIP_CRYPTO_FIXTURE = "7zip-22-zipcrypto-unicode.zip"
        const val BANDIZIP_ZIP_FIXTURE = "bandizip-7.46-deflate-unicode.zip"
        const val WINRAR_ZIP_FIXTURE = "winrar-6.10-deflate-unicode.zip"
        const val WINRAR_RAR4_FIXTURE = "winrar-6.10-rar4-unicode.rar"
        const val WINRAR_RAR5_FIXTURE = "winrar-6.10-rar5-unicode.rar"
        const val WINRAR_RAR5_AES_FIXTURE = "winrar-6.10-rar5-aes-unicode.rar"
        const val WINRAR_RAR5_HEADER_AES_FIXTURE =
            "winrar-6.10-rar5-header-aes-unicode.rar"
        const val WINRAR_RAR5_SPLIT_FIRST_FIXTURE = "winrar-6.10-store-split.part1.rar"
        const val TAR_FIXTURE = "7zip-22-ustar-unicode.tar"
        const val TAR_GZIP_FIXTURE = "7zip-22-ustar-unicode.tar.gz"
        const val TAR_XZ_FIXTURE = "7zip-22-ustar-unicode.tar.xz"
        const val TAR_BZIP2_FIXTURE = "7zip-22-ustar-unicode.tar.bz2"
        const val TAR_ZSTD_FIXTURE = "bsdtar-3.8.4-ustar-unicode.tar.zst"
        const val FIXTURE_PASSWORD = "ArchiveManager-Test-2026"
    }

    private data class CompressedTarFixture(
        val name: String,
        val sha256: String,
        val format: ArchiveFormat,
    )
}
