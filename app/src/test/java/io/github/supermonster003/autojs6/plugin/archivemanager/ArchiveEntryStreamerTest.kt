package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry

class ArchiveEntryStreamerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `streams one scanned entry and verifies its bytes`() {
        val expected = "preview content".toByteArray()
        val source = writeZip(
            temporaryFolder.newFile("preview.zip"),
            FixtureEntry("docs/preview.txt", expected, ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        val output = ByteArrayOutputStream()

        ArchiveEntryStreamer(source, snapshot).stream(snapshot.entries.single(), output)

        assertArrayEquals(expected, output.toByteArray())
    }

    @Test
    fun `rejects entry data that no longer matches the scanned CRC`() {
        val source = writeZip(
            temporaryFolder.newFile("changed.zip"),
            FixtureEntry("changed.txt", "original".toByteArray(), ZipEntry.STORED),
        )
        val snapshot = ArchiveScanner().scan(source)
        patchFirstStoredEntryData(source)
        check(source.setLastModified(snapshot.sourceLastModifiedMillis))

        expectArchiveFailure<ArchiveExtractionException>(ArchiveFailureCode.CRC_MISMATCH) {
            ArchiveEntryStreamer(source, snapshot).stream(
                snapshot.entries.single(),
                ByteArrayOutputStream(),
            )
        }
    }
}
