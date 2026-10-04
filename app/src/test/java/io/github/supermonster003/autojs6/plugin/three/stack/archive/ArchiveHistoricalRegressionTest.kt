package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ArchiveHistoricalRegressionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `legacy 2015 ZIP remains browseable with automatically decoded Chinese names`() {
        val source = copyRegressionFixture(LEGACY_GBK_FIXTURE)

        assertEquals(EXPECTED_LEGACY_GBK_SHA256, source.sha256())
        val snapshot = ArchiveScanner().scan(source)

        assertEquals(ArchiveFormat.ZIP, snapshot.format)
        assertEquals(3, snapshot.entries.size)
        assertTrue(snapshot.entries.first().isDirectory)
        assertEquals("!删除这台电脑6个文件夹", snapshot.entries.first().path)
        assertEquals(
            listOf("删除文件夹.reg", "恢复文件夹.reg"),
            snapshot.entries.drop(1).map(ArchiveEntry::displayName),
        )
        assertTrue(snapshot.entries.all { '\uFFFD' !in it.path })
        assertTrue(snapshot.entries.drop(1).all(ArchiveEntry::canOpen))
        assertTrue(snapshot.entries.drop(1).all(ArchiveEntry::canExtract))
    }

    private fun copyRegressionFixture(name: String): File {
        val target = temporaryFolder.newFile(name)
        val resource = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("regression-fixtures/$name"),
        )
        resource.use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val LEGACY_GBK_FIXTURE = "legacy-2015-gbk-filenames.zip"
        const val EXPECTED_LEGACY_GBK_SHA256 =
            "815a13dabbd4b049a22e2fc95939a96ed96a2bda79a37f8182bd3ab1f4d02ae3"
    }
}
