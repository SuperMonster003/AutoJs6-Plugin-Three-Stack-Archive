package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

class NumberedArchiveVolumeSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `only zip and seven z first volume names are recognized`() {
        assertEquals(
            ArchiveFormat.ZIP,
            NumberedArchiveVolumePolicy.inspectFirstVolume("backup.zip.001")?.format,
        )
        assertEquals(
            ArchiveFormat.SEVEN_Z,
            NumberedArchiveVolumePolicy.inspectFirstVolume("BACKUP.7Z.001")?.format,
        )
        assertNull(NumberedArchiveVolumePolicy.inspectFirstVolume("backup.001"))
        assertNull(NumberedArchiveVolumePolicy.inspectFirstVolume("backup.zip.01"))
        assertNull(NumberedArchiveVolumePolicy.inspectFirstVolume("backup.zip.002"))
        assertNull(NumberedArchiveVolumePolicy.inspectFirstVolume(".zip.001"))
    }

    @Test
    fun `seven zip generated numbered zip and seven z volumes scan and stream exactly`() {
        listOf(
            FixtureSet(ZIP_FIXTURE_STEM, ArchiveFormat.ZIP, 10_155L),
            FixtureSet(SEVEN_Z_FIXTURE_STEM, ArchiveFormat.SEVEN_Z, 10_151L),
        ).forEach { fixture ->
            val resolution = copyAndResolve(fixture.stem)
            val snapshot = ArchiveScanner().scan(resolution.source)
            try {
                assertEquals(fixture.format, snapshot.format)
                assertEquals(fixture.totalSize, snapshot.sourceLength)
                assertEquals(2, snapshot.volumeIdentities.size)
                val entry = snapshot.entries.single()
                assertEquals(FIXTURE_ENTRY_NAME, entry.path)
                assertFalse(entry.capabilities.canDelete)
                assertFalse(entry.capabilities.canRename)

                val output = ByteArrayOutputStream()
                ArchiveEntryStreamer(resolution.source, snapshot).stream(entry, output)
                assertEquals(FIXTURE_PAYLOAD_SHA256, output.toByteArray().sha256())
            } finally {
                snapshot.readerOptions.clearPassword()
            }
        }
    }

    @Test
    fun `a gap in an authorized numbered volume set reports the exact missing name`() {
        val root = temporaryFolder.newFolder("missing-middle")
        val primaryName = "$ZIP_FIXTURE_STEM.001"
        val thirdName = "$ZIP_FIXTURE_STEM.003"
        val primary = copyFixtureTo(root, primaryName)
        val third = copyFixtureTo(root, thirdName)
        val volumeSet = LocalArchiveVolumeSet(
            localDirectory = root,
            sourcesByCollisionKey = mapOf(
                collisionKey(third.name) to third.asArchiveReadSource(),
            ),
        )
        val info = requireNotNull(NumberedArchiveVolumePolicy.inspectFirstVolume(primary.name))

        val error = expectFailure<ArchiveValidationException> {
            info.resolve(primary.asArchiveReadSource(), volumeSet)
        }

        assertEquals(ArchiveFailureCode.MISSING_VOLUME, error.code)
        assertEquals(ArchiveFormat.ZIP, error.format)
        assertTrue(error.message.orEmpty().contains("$ZIP_FIXTURE_STEM.002"))
    }

    private fun copyAndResolve(stem: String): NumberedArchiveVolumeResolution {
        val root = temporaryFolder.newFolder(stem.replace('.', '-'))
        val names = (1..3).map { index ->
            "$stem.${index.toString().padStart(3, '0')}"
        }
        val primary = copyFixtureTo(root, names.first())
        val companions = names.drop(1).associate { name ->
            val file = copyFixtureTo(root, name)
            collisionKey(name) to file.asArchiveReadSource()
        }
        val volumeSet = LocalArchiveVolumeSet(root, companions)
        val info = requireNotNull(NumberedArchiveVolumePolicy.inspectFirstVolume(primary.name))
        return info.resolve(primary.asArchiveReadSource(), volumeSet)
    }

    private fun copyFixtureTo(directory: File, name: String): File = File(directory, name).also {
        val resource = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("archive-fixtures/$name"),
        )
        resource.use { input -> it.outputStream().use(input::copyTo) }
    }

    private inline fun <reified T : Throwable> expectFailure(block: () -> Unit): T {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return error
            throw AssertionError(
                "Expected ${T::class.java.name}, but received ${error.javaClass.name}",
                error,
            )
        }
        throw AssertionError("Expected ${T::class.java.name}")
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private data class FixtureSet(
        val stem: String,
        val format: ArchiveFormat,
        val totalSize: Long,
    )

    private companion object {
        const val ZIP_FIXTURE_STEM = "7zip-22-store-numbered.zip"
        const val SEVEN_Z_FIXTURE_STEM = "7zip-22-copy-numbered.7z"
        const val FIXTURE_ENTRY_NAME = "numbered-volume-payload.md"
        const val FIXTURE_PAYLOAD_SHA256 =
            "e42d8009b2338ca40cbf92c34b50fefc96b4ebafa6edeb2ed18156504b41a385"
    }
}
