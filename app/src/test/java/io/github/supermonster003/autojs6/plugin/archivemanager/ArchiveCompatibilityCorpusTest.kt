package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
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
    }
}
