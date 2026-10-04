package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.lingala.zip4j.io.outputstream.ZipOutputStream as Zip4jOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionKeys
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerArchiveOperationKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveSessionValues
import org.autojs.plugin.explorer.api.IExplorerArchiveOperationCallback
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ExplorerArchiveExtractionInstrumentationTest {

    @Test
    fun extractsAnOpaqueDirectorySelectionIntoTheHostOutputTree() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-extract-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-extract-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "source.zip")
        val expected = "native host extraction".encodeToByteArray()
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("folder/"))
            output.closeEntry()
            output.putNextEntry(ZipEntry("folder/nested.txt"))
            output.write(expected)
            output.closeEntry()
            output.putNextEntry(ZipEntry("other.txt"))
            output.write("not selected".encodeToByteArray())
            output.closeEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "native.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        val folder = session.listChildren(
            "root",
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
            .orEmpty()
            .single { it.getString(ExplorerArchiveSessionKeys.NAME) == "folder" }
        val operationId = UUID.randomUUID().toString()
        val terminal = CountDownLatch(1)
        val completed = AtomicReference<Bundle?>()
        val failed = AtomicReference<Bundle?>()
        val callback = object : IExplorerArchiveOperationCallback.Stub() {
            override fun onProgress(update: Bundle) = Unit

            override fun onCompleted(result: Bundle) {
                completed.set(result)
                terminal.countDown()
            }

            override fun onFailed(failure: Bundle) {
                failed.set(failure)
                terminal.countDown()
            }
        }
        val host = DirectoryOutputHost(outputDirectory)
        session.extractEntries(
            Bundle().apply {
                putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                putStringArrayList(
                    ExplorerArchiveOperationKeys.ENTRY_IDS,
                    arrayListOf(requireNotNull(folder.getString(ExplorerArchiveSessionKeys.ID))),
                )
            },
            host,
            callback,
        )

        assertTrue("Timed out waiting for archive extraction", terminal.await(30, TimeUnit.SECONDS))
        assertNull(failed.get())
        val result = requireNotNull(completed.get())
        assertEquals(operationId, result.getString(ExplorerArchiveOperationKeys.OPERATION_ID))
        assertEquals("native", result.getString(ExplorerArchiveOperationKeys.OUTPUT_DISPLAY_NAME))
        assertEquals(1, result.getInt(ExplorerArchiveOperationKeys.FILES_EXTRACTED))
        assertArrayEquals(expected, File(host.root, "folder/nested.txt").readBytes())
        assertTrue(!File(host.root, "other.txt").exists())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    @Test
    fun cancellationBeforeOutputCreationReportsCancelledAndPublishesNothing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-cancel-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-cancel-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "source.zip")
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("payload.txt"))
            output.write(ByteArray(32 * 1_024) { it.toByte() })
            output.closeEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "cancel.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        val entry = session.listChildren(
            "root",
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
            .orEmpty()
            .single { it.getString(ExplorerArchiveSessionKeys.NAME) == "payload.txt" }
        val operationId = UUID.randomUUID().toString()
        val terminal = CountDownLatch(1)
        val cancellationRequested = AtomicBoolean(false)
        val completed = AtomicReference<Bundle?>()
        val failed = AtomicReference<Bundle?>()
        val callback = object : IExplorerArchiveOperationCallback.Stub() {
            override fun onProgress(update: Bundle) {
                if (cancellationRequested.compareAndSet(false, true)) {
                    session.cancelExtraction(operationId)
                }
            }

            override fun onCompleted(result: Bundle) {
                completed.set(result)
                terminal.countDown()
            }

            override fun onFailed(failure: Bundle) {
                failed.set(failure)
                terminal.countDown()
            }
        }
        val host = DirectoryOutputHost(outputDirectory, "cancel")
        session.extractEntries(
            Bundle().apply {
                putString(ExplorerArchiveOperationKeys.OPERATION_ID, operationId)
                putStringArrayList(
                    ExplorerArchiveOperationKeys.ENTRY_IDS,
                    arrayListOf(requireNotNull(entry.getString(ExplorerArchiveSessionKeys.ID))),
                )
            },
            host,
            callback,
        )

        assertTrue("Timed out waiting for archive cancellation", terminal.await(30, TimeUnit.SECONDS))
        assertNull(completed.get())
        val failure = requireNotNull(failed.get())
        assertEquals(operationId, failure.getString(ExplorerArchiveOperationKeys.OPERATION_ID))
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_ERROR_CANCELLED,
            failure.getInt(ExplorerArchiveOperationKeys.ERROR_CODE),
        )
        assertTrue(!host.root.exists())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    @Test
    fun encryptedEntryRetriesReportTypedInteractionsAndClearRequestPasswords() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-password-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-password-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "encrypted.zip")
        val expected = "encrypted native host extraction".encodeToByteArray()
        writeEncryptedZip(archive, "secret.txt", expected, PASSWORD)

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "encrypted.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        val entry = session.listChildren(
            "root",
            0,
            ExplorerActionProtocol.MAX_ARCHIVE_PAGE_SIZE,
        ).getParcelableArrayList<Bundle>(ExplorerArchiveSessionKeys.ITEMS)
            .orEmpty()
            .single { it.getString(ExplorerArchiveSessionKeys.NAME) == "secret.txt" }
        assertTrue(entry.getBoolean(ExplorerArchiveSessionKeys.CAN_EXTRACT))
        val entryId = requireNotNull(entry.getString(ExplorerArchiveSessionKeys.ID))

        val missingHost = DirectoryOutputHost(outputDirectory, "encrypted")
        val missing = runExtractionAttempt(session, entryId, missingHost)
        assertNull(missing.completed)
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_ERROR_INTERACTION_REQUIRED,
            missing.failed?.getInt(ExplorerArchiveOperationKeys.ERROR_CODE),
        )
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_PASSWORD_REQUIRED,
            missing.failed?.getInt(ExplorerArchiveOperationKeys.INTERACTION_KIND),
        )
        assertTrue(!missingHost.root.exists())

        awaitSessionOperationCleanup()
        val wrongPassword = "wrong-password".toCharArray()
        val wrongHost = DirectoryOutputHost(outputDirectory, "encrypted")
        val wrong = runExtractionAttempt(session, entryId, wrongHost, wrongPassword)
        assertTrue(wrongPassword.all { it == '\u0000' })
        assertNull(wrong.completed)
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_WRONG_PASSWORD,
            wrong.failed?.getInt(ExplorerArchiveOperationKeys.INTERACTION_KIND),
        )
        assertTrue(!wrongHost.root.exists())

        awaitSessionOperationCleanup()
        val correctPassword = PASSWORD.toCharArray()
        val correctHost = DirectoryOutputHost(outputDirectory, "encrypted")
        val correct = runExtractionAttempt(session, entryId, correctHost, correctPassword)
        assertTrue(correctPassword.all { it == '\u0000' })
        assertNull(correct.failed)
        assertArrayEquals(expected, File(correctHost.root, "secret.txt").readBytes())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    @Test
    fun unsafePathsRequireTypedSkipConfirmationBeforeSafeEntriesAreWritten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-unsafe-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-unsafe-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "unsafe.zip")
        val expected = "safe entry".encodeToByteArray()
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("safe.txt"))
            output.write(expected)
            output.closeEntry()
            output.putNextEntry(ZipEntry("../outside.txt"))
            output.write("must not escape".encodeToByteArray())
            output.closeEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "unsafe.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            onClosed = {},
        )
        val blockedHost = DirectoryOutputHost(outputDirectory, "unsafe")
        val blocked = runExtractionAttempt(session, "root", blockedHost)
        assertNull(blocked.completed)
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_UNSAFE_PATHS,
            blocked.failed?.getInt(ExplorerArchiveOperationKeys.INTERACTION_KIND),
        )
        assertTrue(!blockedHost.root.exists())

        awaitSessionOperationCleanup()
        val confirmedHost = DirectoryOutputHost(outputDirectory, "unsafe")
        val confirmed = runExtractionAttempt(
            session = session,
            entryId = "root",
            host = confirmedHost,
            skipUnsafePaths = true,
        )
        assertNull(confirmed.failed)
        assertArrayEquals(expected, File(confirmedHost.root, "safe.txt").readBytes())
        assertTrue(!File(outputDirectory, "outside.txt").exists())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    @Test
    fun resourceBudgetRequiresTypedConfirmationBeforeExpandedExtraction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDirectory = File(context.cacheDir, "archive-session-budget-source-${UUID.randomUUID()}")
        val outputDirectory = File(context.cacheDir, "archive-session-budget-output-${UUID.randomUUID()}")
        assertTrue(sourceDirectory.mkdirs())
        assertTrue(outputDirectory.mkdirs())
        val archive = File(sourceDirectory, "budget.zip")
        val expected = ByteArray(64 * 1_024) { index -> (index % 4).toByte() }
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            output.putNextEntry(ZipEntry("payload.bin"))
            output.write(expected)
            output.closeEntry()
        }

        val session = ExplorerArchiveSession(
            ownerUid = Process.myUid(),
            displayName = "budget.zip",
            stagedArchive = StagedArchive(archive.asArchiveReadSource(), archive.length()),
            snapshot = ArchiveScanner().scan(archive),
            extractionResourceBudget = ArchiveResourceBudget.COMPATIBLE.copy(
                maxCompressionRatio = 1L,
            ),
            onClosed = {},
        )
        val blockedHost = DirectoryOutputHost(outputDirectory, "budget")
        val blocked = runExtractionAttempt(session, "root", blockedHost)
        assertNull(blocked.completed)
        assertEquals(
            ExplorerArchiveSessionValues.EXTRACTION_INTERACTION_RESOURCE_BUDGET,
            blocked.failed?.getInt(ExplorerArchiveOperationKeys.INTERACTION_KIND),
        )
        assertTrue(!blockedHost.root.exists())

        awaitSessionOperationCleanup()
        val confirmedHost = DirectoryOutputHost(outputDirectory, "budget")
        val confirmed = runExtractionAttempt(
            session = session,
            entryId = "root",
            host = confirmedHost,
            allowResourceBudgetOverride = true,
        )
        assertNull(confirmed.failed)
        assertArrayEquals(expected, File(confirmedHost.root, "payload.bin").readBytes())

        session.close()
        assertTrue(!archive.exists())
        outputDirectory.deleteRecursively()
    }

    private fun runExtractionAttempt(
        session: ExplorerArchiveSession,
        entryId: String,
        host: DirectoryOutputHost,
        password: CharArray? = null,
        skipUnsafePaths: Boolean = false,
        allowResourceBudgetOverride: Boolean = false,
    ): ExtractionAttemptResult {
        val terminal = CountDownLatch(1)
        val completed = AtomicReference<Bundle?>()
        val failed = AtomicReference<Bundle?>()
        val callback = object : IExplorerArchiveOperationCallback.Stub() {
            override fun onProgress(update: Bundle) = Unit

            override fun onCompleted(result: Bundle) {
                completed.set(result)
                terminal.countDown()
            }

            override fun onFailed(failure: Bundle) {
                failed.set(failure)
                terminal.countDown()
            }
        }
        val request = Bundle().apply {
            putString(ExplorerArchiveOperationKeys.OPERATION_ID, UUID.randomUUID().toString())
            putStringArrayList(ExplorerArchiveOperationKeys.ENTRY_IDS, arrayListOf(entryId))
            password?.let { putCharArray(ExplorerArchiveOperationKeys.PASSWORD, it) }
            putBoolean(ExplorerArchiveOperationKeys.SKIP_UNSAFE_PATHS, skipUnsafePaths)
            putBoolean(
                ExplorerArchiveOperationKeys.ALLOW_RESOURCE_BUDGET_OVERRIDE,
                allowResourceBudgetOverride,
            )
        }
        session.extractEntries(request, host, callback)
        assertTrue(!request.containsKey(ExplorerArchiveOperationKeys.PASSWORD))
        assertTrue("Timed out waiting for archive extraction", terminal.await(30, TimeUnit.SECONDS))
        return ExtractionAttemptResult(completed.get(), failed.get())
    }

    private fun writeEncryptedZip(
        target: File,
        entryName: String,
        payload: ByteArray,
        password: String,
    ) {
        val passwordChars = password.toCharArray()
        try {
            Zip4jOutputStream(FileOutputStream(target), passwordChars).use { output ->
                output.putNextEntry(
                    ZipParameters().apply {
                        fileNameInZip = entryName
                        compressionMethod = CompressionMethod.DEFLATE
                        isEncryptFiles = true
                        encryptionMethod = EncryptionMethod.AES
                        aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    },
                )
                output.write(payload)
                output.closeEntry()
            }
        } finally {
            passwordChars.fill('\u0000')
        }
    }

    private fun awaitSessionOperationCleanup() {
        Thread.sleep(50L)
    }

    private data class ExtractionAttemptResult(
        val completed: Bundle?,
        val failed: Bundle?,
    )

    private class DirectoryOutputHost(
        private val parent: File,
        rootName: String = "native",
    ) : UnusedTestExplorerActionHostSession() {
        private val transactionId = UUID.randomUUID().toString()
        private var state = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
        val root = File(parent, rootName)

        override fun prepareOutputTree(displayName: String, conflictPolicy: Int): Bundle {
            assertEquals(root.name, displayName)
            assertEquals(ExplorerActionHostSessionValues.OUTPUT_CONFLICT_AUTO_RENAME, conflictPolicy)
            check(root.mkdir())
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED
            return outputBundle(state)
        }

        override fun createOutputDirectory(transactionId: String, relativePath: String) {
            requireActive(transactionId)
            check(File(root, relativePath).mkdirs())
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING
        }

        override fun openOutputFile(
            transactionId: String,
            relativePath: String,
        ): ParcelFileDescriptor {
            requireActive(transactionId)
            val target = File(root, relativePath)
            check(target.parentFile?.isDirectory == true)
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING
            return ParcelFileDescriptor.open(
                target,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun commitOutput(transactionId: String): Bundle {
            requireActive(transactionId)
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED
            return outputBundle(state)
        }

        override fun queryOutput(transactionId: String): Bundle {
            require(transactionId == this.transactionId)
            return outputBundle(state)
        }

        override fun abortOutput(transactionId: String) {
            require(transactionId == this.transactionId)
            root.deleteRecursively()
            state = ExplorerActionHostSessionValues.OUTPUT_STATE_ABORTED
        }

        override fun close() {
            if (state != ExplorerActionHostSessionValues.OUTPUT_STATE_COMMITTED) {
                root.deleteRecursively()
            }
        }

        private fun requireActive(value: String) {
            require(value == transactionId)
            check(
                state == ExplorerActionHostSessionValues.OUTPUT_STATE_PREPARED ||
                    state == ExplorerActionHostSessionValues.OUTPUT_STATE_WRITING,
            )
        }

        private fun outputBundle(outputState: Int): Bundle = Bundle().apply {
            putString(ExplorerActionHostSessionKeys.OUTPUT_TRANSACTION_ID, transactionId)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_NAME, root.name)
            putString(ExplorerActionHostSessionKeys.OUTPUT_DISPLAY_PATH, root.absolutePath)
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_KIND,
                ExplorerActionHostSessionValues.OUTPUT_KIND_DIRECTORY_TREE,
            )
            putInt(ExplorerActionHostSessionKeys.OUTPUT_STATE, outputState)
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_FILE_COUNT,
                root.walkTopDown().count(File::isFile),
            )
            putInt(
                ExplorerActionHostSessionKeys.OUTPUT_DIRECTORY_COUNT,
                root.walkTopDown().count(File::isDirectory).coerceAtLeast(1) - 1,
            )
            putLong(
                ExplorerActionHostSessionKeys.OUTPUT_BYTES,
                root.walkTopDown().filter(File::isFile).sumOf(File::length),
            )
        }
    }

    private companion object {
        const val PASSWORD = "archive-test-2026"
    }
}
