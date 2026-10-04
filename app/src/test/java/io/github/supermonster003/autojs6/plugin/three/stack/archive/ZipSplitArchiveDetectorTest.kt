package io.github.supermonster003.autojs6.plugin.three.stack.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ZipSplitArchiveDetectorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `detects the final volume and derives standard missing volume names`() {
        val source = copyFixture(FINAL_VOLUME_FIXTURE, "资料.zip")

        val info = requireNotNull(ZipSplitArchiveDetector.inspect(source))

        assertEquals(ZipSplitSegmentKind.FINAL_VOLUME, info.segmentKind)
        assertEquals(1, info.lastDiskNumber)
        assertEquals(2, info.totalVolumeCount)
        assertEquals("资料.z01", info.requiredVolumeSummary("资料.zip"))
        assertEquals("资料.zip", info.finalVolumeName("资料.zip"))
    }

    @Test
    fun `detects a first standard volume from its leading split signature`() {
        val source = copyFixture(FIRST_VOLUME_FIXTURE, "资料.z01")

        val info = requireNotNull(ZipSplitArchiveDetector.inspect(source))

        assertEquals(ZipSplitSegmentKind.FIRST_VOLUME, info.segmentKind)
        assertNull(info.lastDiskNumber)
        assertNull(info.totalVolumeCount)
        assertEquals("资料.zip", info.finalVolumeName("资料.z01"))
        assertEquals("*.z01, *.z02, ...", info.requiredVolumeSummary("资料.z01"))
    }

    @Test
    fun `ordinary zip remains outside split archive diagnostics`() {
        val source = writeZip(
            temporaryFolder.newFile("ordinary.zip"),
            FixtureEntry("plain.txt", "plain".toByteArray()),
        )

        assertNull(ZipSplitArchiveDetector.inspect(source))
        assertEquals("plain.txt", ArchiveScanner().scan(source).entries.single().path)
    }

    @Test
    fun `scanner classifies a lone final volume as missing volumes instead of malformed`() {
        val source = copyFixture(FINAL_VOLUME_FIXTURE, "source.archive")

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MISSING_VOLUME,
        ) {
            ArchiveScanner().scan(source)
        }

        assertEquals(ArchiveFormat.ZIP, error.format)
        assertEquals(ArchiveFailureStage.INDEX, error.stage)
        val splitFailure = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<ZipSplitArchiveException>()
            .firstOrNull()
        assertEquals(1, splitFailure?.info?.lastDiskNumber)
    }

    @Test
    fun `scanner recognizes a first volume marker even without a zip filename`() {
        val source = copyFixture(FIRST_VOLUME_FIXTURE, "source.archive")

        val error = expectArchiveFailure<ArchiveValidationException>(
            ArchiveFailureCode.MISSING_VOLUME,
        ) {
            ArchiveScanner().scan(source)
        }

        assertEquals(ArchiveFormat.ZIP, error.format)
        assertEquals(ArchiveFailureStage.INDEX, error.stage)
        val splitFailure = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<ZipSplitArchiveException>()
            .firstOrNull()
        assertEquals(ZipSplitSegmentKind.FIRST_VOLUME, splitFailure?.info?.segmentKind)
    }

    @Test
    fun `volume summaries are bounded even when metadata declares many disks`() {
        val info = ZipSplitArchiveInfo(
            segmentKind = ZipSplitSegmentKind.FINAL_VOLUME,
            lastDiskNumber = 50_000,
        )

        val summary = info.requiredVolumeSummary("large.zip")

        assertEquals(
            "large.z01, large.z02, large.z03, large.z04, large.z05, large.z06, " +
                "large.z07, large.z08 (+49992)",
            summary,
        )
        assertTrue(summary.length < 160)
    }

    @Test
    fun `committed split fixtures retain fixed hashes`() {
        assertEquals(
            FIRST_VOLUME_SHA256,
            fixtureBytes(FIRST_VOLUME_FIXTURE).sha256(),
        )
        assertEquals(
            FINAL_VOLUME_SHA256,
            fixtureBytes(FINAL_VOLUME_FIXTURE).sha256(),
        )
    }

    private fun copyFixture(name: String, targetName: String): File =
        temporaryFolder.newFile(targetName).apply { writeBytes(fixtureBytes(name)) }

    private fun fixtureBytes(name: String): ByteArray = requireNotNull(
        javaClass.classLoader?.getResourceAsStream("archive-fixtures/$name"),
    ).use { it.readBytes() }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private companion object {
        const val FIRST_VOLUME_FIXTURE = "zip4j-2.11.5-store-split.z01"
        const val FINAL_VOLUME_FIXTURE = "zip4j-2.11.5-store-split.zip"
        const val FIRST_VOLUME_SHA256 =
            "3acda0b2528818e2838b5525b187c354d2a4a9a4c17953f7d69106b2f60e94cd"
        const val FINAL_VOLUME_SHA256 =
            "37453a6abdd3c13493a68beea151cd9382f41abfebde98b7d8826301064eff4e"
    }
}
