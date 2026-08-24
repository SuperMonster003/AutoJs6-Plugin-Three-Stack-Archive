package io.github.supermonster003.autojs6.plugin.archivemanager

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipCentralDirectoryNameReaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `reads legacy bytes after a preamble and before trailing data`() {
        val source = temporaryFolder.newFile("legacy-with-padding.zip")
        val charset = Charset.forName("GB18030")
        ZipOutputStream(FileOutputStream(source), charset).use { output ->
            output.putNextEntry(ZipEntry("目录/文件.txt"))
            output.write("payload".toByteArray())
            output.closeEntry()
        }
        Files.write(
            source.toPath(),
            "MZ-preamble".toByteArray() + source.readBytes() + "trailing-data".toByteArray(),
        )

        val rawNames = requireNotNull(ZipCentralDirectoryNameReader.readNonUtf8Names(source))

        assertEquals(listOf("目录/文件.txt"), rawNames.map { it.toString(charset) })
        assertEquals(
            charset,
            ZipArchiveAccess.detectRawFilenameCharset(rawNames, Locale.CHINA),
        )
    }

    @Test
    fun `recognizes UTF-8 bytes when an old writer omitted the UTF-8 flag`() {
        val source = temporaryFolder.newFile("utf8-without-language-flag.zip")
        ZipOutputStream(FileOutputStream(source)).use { output ->
            output.putNextEntry(ZipEntry("目录/hello.txt"))
            output.write("payload".toByteArray())
            output.closeEntry()
        }
        val bytes = source.readBytes()
        clearCentralDirectoryUtf8Flags(bytes)
        source.writeBytes(bytes)

        val rawNames = requireNotNull(ZipCentralDirectoryNameReader.readNonUtf8Names(source))

        assertEquals(listOf("目录/hello.txt"), rawNames.map { it.toString(Charsets.UTF_8) })
        assertEquals(
            Charsets.UTF_8,
            ZipArchiveAccess.detectRawFilenameCharset(rawNames, Locale.CHINA),
        )
    }

    private fun clearCentralDirectoryUtf8Flags(bytes: ByteArray) {
        var offset = 0
        var headers = 0
        while (offset <= bytes.size - Int.SIZE_BYTES) {
            if (
                bytes[offset] == 0x50.toByte() &&
                bytes[offset + 1] == 0x4B.toByte() &&
                bytes[offset + 2] == 0x01.toByte() &&
                bytes[offset + 3] == 0x02.toByte()
            ) {
                bytes[offset + CENTRAL_DIRECTORY_FLAGS_HIGH_BYTE_OFFSET] =
                    (bytes[offset + CENTRAL_DIRECTORY_FLAGS_HIGH_BYTE_OFFSET].toInt() and 0xF7).toByte()
                headers++
            }
            offset++
        }
        assertEquals(1, headers)
    }

    private companion object {
        const val CENTRAL_DIRECTORY_FLAGS_HIGH_BYTE_OFFSET = 9
    }
}
