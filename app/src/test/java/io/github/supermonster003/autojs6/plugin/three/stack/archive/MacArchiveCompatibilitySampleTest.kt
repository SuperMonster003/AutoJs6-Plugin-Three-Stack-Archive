package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer

/**
 * Opt-in regression checks for externally supplied macOS samples that are not redistributed.
 * Set the environment variables documented in the fixture manifest to execute these tests.
 */
class MacArchiveCompatibilitySampleTest {

    @Test
    fun `macOS Archive Utility local and central header variance remains browseable`() {
        val source = externalSample(HELLO_DOLLY_ENV)
        assertEquals(HELLO_DOLLY_SHA256, source.sha256())

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
        // All names are ASCII and carry no UTF-8 flag, so ZIP's IBM437 fallback is lossless.
        assertEquals("IBM437", snapshot.readerOptions.filenameCharsetName)
        assertEquals(
            listOf(
                "hello-dolly",
                "__MACOSX/._hello-dolly",
                "hello-dolly/hello.php",
                "__MACOSX/hello-dolly/._hello.php",
                "hello-dolly/readme.txt",
                "__MACOSX/hello-dolly/._readme.txt",
            ),
            snapshot.entries.map(ArchiveEntry::path),
        )
        val readme = snapshot.entries.single { entry -> entry.path == "hello-dolly/readme.txt" }
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(readme, output)
        assertEquals(624, output.size())
    }

    @Test
    fun `macOS UTF-8 names without language flags preserve emoji and NFD paths`() {
        val source = externalSample(ZIPKIREI_ENV)
        assertEquals(ZIPKIREI_SHA256, source.sha256())

        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
        assertEquals("UTF-8", snapshot.readerOptions.filenameCharsetName)
        assertEquals(43, snapshot.entries.size)
        assertTrue(snapshot.entries.any { entry -> entry.path == "emoji/\uD83D\uDE3B.txt" })
        assertTrue(snapshot.entries.any { entry -> entry.path == "中文/貓.txt" })
        val nfdJapanesePath = "日本語/ハ\u309Aンタ\u3099.txt"
        assertTrue(snapshot.entries.any { entry -> entry.path == nfdJapanesePath })
        assertTrue(Normalizer.isNormalized(nfdJapanesePath, Normalizer.Form.NFD))
        assertTrue(!Normalizer.isNormalized(nfdJapanesePath, Normalizer.Form.NFC))
        val englishEntry = snapshot.entries.single { entry -> entry.path == "English/cat.txt" }
        val output = ByteArrayOutputStream()
        ArchiveEntryStreamer(source, snapshot).stream(englishEntry, output)
        assertTrue(output.size() > 0)
    }

    private fun externalSample(environmentName: String): File {
        val path = System.getenv(environmentName)
        assumeTrue("Set $environmentName to run this external sample test", !path.isNullOrBlank())
        return File(requireNotNull(path)).also { sample ->
            assumeTrue("External sample is unavailable: $sample", sample.isFile)
        }
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val HELLO_DOLLY_ENV = "ARCHIVE_MAC_HELLO_DOLLY_SAMPLE"
        const val ZIPKIREI_ENV = "ARCHIVE_MAC_ZIPKIREI_SAMPLE"
        const val HELLO_DOLLY_SHA256 =
            "df3cfbc607582bcdd3e529800ac5559c90d9ca0c4c24179a1178f69a5d6a9dc4"
        const val ZIPKIREI_SHA256 =
            "676c57107bdc3c70c0e93eaadad5ccdfff4e1829aff23b7045f9c1c7d7b8f9e5"
    }
}
