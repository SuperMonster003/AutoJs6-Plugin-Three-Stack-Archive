package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Opt-in regression checks for the locally supplied archive-test-2026 corpus.
 * The samples remain outside the repository and their payloads are only read as inert archive data.
 */
class ExternalArchiveCompatibilitySampleTest {

    @Test
    fun `all supplied corpus files retain their pinned identities`() {
        val directory = externalCorpusDirectory()

        assertEquals(EXPECTED_SAMPLE_NAMES, coveredSampleNames())
        EXPECTED_SAMPLES.forEach { (name, expectedSha256) ->
            val source = requiredSample(directory, name)
            assertEquals("$name SHA-256", expectedSha256, source.sha256())
        }
    }

    @Test
    fun `plain archives enter the production reader and expose verified entry data`() {
        val directory = externalCorpusDirectory()

        PLAIN_ARCHIVES.forEach { fixture ->
            val source = verifiedSample(directory, fixture.name)
            val snapshot = ArchiveScanner().scan(source)

            assertEquals("${fixture.name} format", fixture.format, snapshot.format)
            assertTrue("${fixture.name} entries", snapshot.entries.isNotEmpty())
            streamOneEntry(source, snapshot)
        }
    }

    @Test
    fun `encrypted archives use typed password recovery and the supplied password`() {
        val directory = externalCorpusDirectory()

        ENCRYPTED_ARCHIVES.forEach { fixture ->
            val source = verifiedSample(directory, fixture.name)
            val withoutPassword = runCatching { ArchiveScanner().scan(source) }
            withoutPassword.fold(
                onSuccess = { snapshot ->
                    assertEquals("${fixture.name} format", fixture.format, snapshot.format)
                    assertTrue(snapshot.entries.isNotEmpty())
                    assertTrue(snapshot.entries.all(ArchiveEntry::isEncrypted))
                    assertTrue(snapshot.entries.none(ArchiveEntry::canExtract))
                },
                onFailure = { failure ->
                    val error = failure as? ArchiveValidationException
                        ?: throw AssertionError("${fixture.name} untyped password failure", failure)
                    assertEquals("${fixture.name} password code", ArchiveFailureCode.PASSWORD_REQUIRED, error.code)
                    assertEquals("${fixture.name} format", fixture.format, error.format)
                    assertEquals("${fixture.name} password stage", ArchiveFailureStage.PASSWORD, error.stage)
                },
            )

            val wrong = expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.WRONG_PASSWORD,
            ) {
                ArchiveScanner().scan(
                    source,
                    ArchiveReaderOptions(password = "wrong-password".toCharArray()),
                )
            }
            assertEquals("${fixture.name} wrong-password format", fixture.format, wrong.format)
            assertEquals(ArchiveFailureStage.PASSWORD, wrong.stage)

            val snapshot = ArchiveScanner().scan(
                source,
                ArchiveReaderOptions(password = CORPUS_PASSWORD.toCharArray()),
            )
            try {
                assertEquals("${fixture.name} recovered format", fixture.format, snapshot.format)
                assertTrue(snapshot.entries.isNotEmpty())
                streamOneEntry(source, snapshot)
            } finally {
                snapshot.readerOptions.clearPassword()
            }
        }
    }

    @Test
    fun `complete WinRAR volume set lists and streams reconstructed entry data`() {
        val directory = externalCorpusDirectory()
        val primary = verifiedSample(directory, WINRAR_SPLIT_PRIMARY)
        val companion = verifiedSample(directory, WINRAR_SPLIT_COMPANION)
        val source = VolumeAwareArchiveReadSource(
            source = primary.asArchiveReadSource(),
            displayName = primary.name,
            volumeSet = LocalArchiveVolumeSet(
                directory,
                mapOf(collisionKey(companion.name) to companion.asArchiveReadSource()),
            ),
        )

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.RAR, snapshot.format)
        assertTrue(snapshot.entries.isNotEmpty())
        streamOneEntry(source, snapshot)
    }

    @Test
    fun `standalone MT Manager compressor streams are not reported as tar archives`() {
        val directory = externalCorpusDirectory()

        STANDALONE_STREAMS.forEach { name ->
            val source = verifiedSample(directory, name)
            val error = expectArchiveFailure<ArchiveValidationException>(
                ArchiveFailureCode.INVALID_SIGNATURE,
            ) {
                ArchiveScanner().scan(source)
            }

            assertEquals("$name detected format", null, error.format)
            assertEquals("$name failure stage", ArchiveFailureStage.FORMAT_DETECTION, error.stage)
        }
    }

    private fun externalCorpusDirectory(): File {
        val path = System.getenv(CORPUS_DIRECTORY_ENV)
        assumeTrue("Set $CORPUS_DIRECTORY_ENV to run this external corpus", !path.isNullOrBlank())
        return File(requireNotNull(path)).also { directory ->
            assumeTrue("External corpus directory is unavailable: $directory", directory.isDirectory)
        }
    }

    private fun verifiedSample(directory: File, name: String): File =
        requiredSample(directory, name).also { source ->
            assertEquals("$name SHA-256", requireNotNull(EXPECTED_SAMPLES[name]), source.sha256())
        }

    private fun requiredSample(directory: File, name: String): File = File(directory, name).also {
        assertTrue("External corpus sample is unavailable: $it", it.isFile)
    }

    private fun streamOneEntry(source: File, snapshot: ArchiveSnapshot) {
        streamOneEntry(source.asArchiveReadSource(), snapshot)
    }

    private fun streamOneEntry(source: ArchiveReadSource, snapshot: ArchiveSnapshot) {
        val entry = snapshot.entries.firstOrNull { candidate -> candidate.canOpen }
            ?: throw AssertionError("${snapshot.format} sample has no readable file entry")
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(entry, output)
        assertEquals(entry.uncompressedSize, output.size().toLong())
    }

    private fun coveredSampleNames(): Set<String> = buildSet {
        addAll(PLAIN_ARCHIVES.map(ExternalFixture::name))
        addAll(ENCRYPTED_ARCHIVES.map(ExternalFixture::name))
        add(WINRAR_SPLIT_PRIMARY)
        add(WINRAR_SPLIT_COMPANION)
        addAll(STANDALONE_STREAMS)
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private data class ExternalFixture(
        val name: String,
        val format: ArchiveFormat,
    )

    private companion object {
        const val CORPUS_DIRECTORY_ENV = "ARCHIVE_EXTERNAL_CORPUS_DIRECTORY"
        const val CORPUS_PASSWORD = "archive-test-2026"
        const val WINRAR_SPLIT_PRIMARY = "2-winrar.part01.rar"
        const val WINRAR_SPLIT_COMPANION = "2-winrar.part02.rar"

        val PLAIN_ARCHIVES = listOf(
            ExternalFixture("1-bandizip.zip", ArchiveFormat.ZIP),
            ExternalFixture("2-winrar-rar4.rar", ArchiveFormat.RAR),
            ExternalFixture("2-winrar-rar5.rar", ArchiveFormat.RAR),
            ExternalFixture("2-winrar.zip", ArchiveFormat.ZIP),
            ExternalFixture("4-mt-manager.7z", ArchiveFormat.SEVEN_Z),
            ExternalFixture("4-mt-manager.tar", ArchiveFormat.TAR),
            ExternalFixture("4-mt-manager.zip", ArchiveFormat.ZIP),
            ExternalFixture("5-删除这台电脑6个文件夹.zip", ArchiveFormat.ZIP),
        )
        val ENCRYPTED_ARCHIVES = listOf(
            ExternalFixture("2-winrar-rar5-pw.rar", ArchiveFormat.RAR),
            ExternalFixture("4-mt-manager-pw.7z", ArchiveFormat.SEVEN_Z),
        )
        val STANDALONE_STREAMS = setOf(
            "4-mt-manager.gz",
            "4-mt-manager.xz",
        )
        val EXPECTED_SAMPLES = linkedMapOf(
            "1-bandizip.zip" to "1b9b3dd42d0d26f9b172d95c9804a2c4e86b2c052cc4a846421de3fb4dc17218",
            "2-winrar-rar4.rar" to "d0186f1b6e34d7c1362c01b25a7b901e8c2bcec358efe157d73fb91004326051",
            "2-winrar-rar5-pw.rar" to "0bbfabf74ab3d1d45d901b52f9fda81911050d83338e03fc3356950b62761b7b",
            "2-winrar-rar5.rar" to "f0d97e455c4f9215af95a1ed0c26c2fe1d693a1b3da09ab2711071676d9c59ba",
            WINRAR_SPLIT_PRIMARY to "f8edc1cecca1b0a7bafa907ed6176004f84cd56916430e53600322592c0d0047",
            WINRAR_SPLIT_COMPANION to "7ffd9f978f192053f69b549ed86655dcc2a492067cdd8e9412453e2df85e64dd",
            "2-winrar.zip" to "ecee2d4c96219fe7c3b5d0f20639d6681675ec84380ba3e1fe5dba6e6a0ddc1c",
            "4-mt-manager-pw.7z" to "426f0c772c53e158d1572bbb4ff7138d0081e2a78c5e8fece90561ad6756c88a",
            "4-mt-manager.7z" to "bb10bfc36c8bc46ee8a25195ec3d87c2991986d655835c05b3d2bb00134c9ee6",
            "4-mt-manager.gz" to "5e6e040d5ec14d1517cda8d388ee948ef217b668b9b748d300343f0c7f13c151",
            "4-mt-manager.tar" to "360bf01e99cd90b589920e6d4d71329f70a42843cc6794d946c5f1d5bc8853fd",
            "4-mt-manager.xz" to "fb3bab091af4eadb75d851e2d1f710ca2df9160ec41fd76da08515924fcd2675",
            "4-mt-manager.zip" to "83d2c542143de6b1fecdff58240f6994cf57a7646fbc1d594831eb766ce61041",
            "5-删除这台电脑6个文件夹.zip" to "815a13dabbd4b049a22e2fc95939a96ed96a2bda79a37f8182bd3ab1f4d02ae3",
        )
        val EXPECTED_SAMPLE_NAMES = EXPECTED_SAMPLES.keys.toSet()
    }
}
