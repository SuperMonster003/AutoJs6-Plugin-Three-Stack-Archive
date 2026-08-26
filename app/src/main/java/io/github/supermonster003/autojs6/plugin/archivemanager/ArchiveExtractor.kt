package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.CRC32

internal class ArchiveExtractor @JvmOverloads constructor(
    private val contentResolver: ContentResolver? = null,
    private val resourceBudget: ArchiveResourceBudget = ArchiveResourceBudget.COMPATIBLE,
    private val engine: ArchiveEngine = ArchiveEngine.DEFAULT,
) {

    suspend fun extract(
        source: File,
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        treeUri: Uri,
        rootName: String,
        skipUnsafePaths: Boolean = false,
        allowResourceBudgetOverride: Boolean = false,
        conflictPolicy: ArchiveExtractionConflictPolicy = ArchiveExtractionConflictPolicy.ASK,
        conflictResolver: ArchiveExtractionConflictResolver = ArchiveExtractionConflictResolver.NONE,
        progress: ArchiveProgressListener = ArchiveProgressListener.NONE,
    ): ExtractionResult = extract(
        source = source.asArchiveReadSource(),
        snapshot = snapshot,
        selectedPaths = selectedPaths,
        treeUri = treeUri,
        rootName = rootName,
        skipUnsafePaths = skipUnsafePaths,
        allowResourceBudgetOverride = allowResourceBudgetOverride,
        conflictPolicy = conflictPolicy,
        conflictResolver = conflictResolver,
        progress = progress,
    )

    suspend fun extract(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        treeUri: Uri,
        rootName: String,
        skipUnsafePaths: Boolean = false,
        allowResourceBudgetOverride: Boolean = false,
        conflictPolicy: ArchiveExtractionConflictPolicy = ArchiveExtractionConflictPolicy.ASK,
        conflictResolver: ArchiveExtractionConflictResolver = ArchiveExtractionConflictResolver.NONE,
        progress: ArchiveProgressListener = ArchiveProgressListener.NONE,
    ): ExtractionResult {
        val resolver = contentResolver ?: throw IllegalStateException(
            "ArchiveExtractor requires a ContentResolver for SAF extraction",
        )
        val writer = try {
            SafArchiveOutputWriter(resolver, treeUri)
        } catch (error: RuntimeException) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.OUTPUT_FAILURE,
                "Extraction destination is invalid",
                error,
                snapshot.format,
            )
        }
        return extractToWriter(
            source = source,
            snapshot = snapshot,
            selectedPaths = selectedPaths,
            rootName = rootName,
            writer = writer,
            skipUnsafePaths = skipUnsafePaths,
            allowResourceBudgetOverride = allowResourceBudgetOverride,
            conflictPolicy = conflictPolicy,
            conflictResolver = conflictResolver,
            progress = progress,
        )
    }

    suspend fun extractToWriter(
        source: File,
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        rootName: String,
        writer: ArchiveOutputWriter,
        skipUnsafePaths: Boolean = false,
        allowResourceBudgetOverride: Boolean = false,
        conflictPolicy: ArchiveExtractionConflictPolicy = ArchiveExtractionConflictPolicy.ASK,
        conflictResolver: ArchiveExtractionConflictResolver = ArchiveExtractionConflictResolver.NONE,
        progress: ArchiveProgressListener = ArchiveProgressListener.NONE,
    ): ExtractionResult = extractToWriter(
        source = source.asArchiveReadSource(),
        snapshot = snapshot,
        selectedPaths = selectedPaths,
        rootName = rootName,
        writer = writer,
        skipUnsafePaths = skipUnsafePaths,
        allowResourceBudgetOverride = allowResourceBudgetOverride,
        conflictPolicy = conflictPolicy,
        conflictResolver = conflictResolver,
        progress = progress,
    )

    suspend fun extractToWriter(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        selectedPaths: Collection<String>,
        rootName: String,
        writer: ArchiveOutputWriter,
        skipUnsafePaths: Boolean = false,
        allowResourceBudgetOverride: Boolean = false,
        conflictPolicy: ArchiveExtractionConflictPolicy = ArchiveExtractionConflictPolicy.ASK,
        conflictResolver: ArchiveExtractionConflictResolver = ArchiveExtractionConflictResolver.NONE,
        progress: ArchiveProgressListener = ArchiveProgressListener.NONE,
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val extractionContext = currentCoroutineContext()
        val structureLimits = snapshot.structureLimits
        val safeRootName = ArchivePathPolicy.validateDestinationRootName(rootName, structureLimits)
        validateSnapshot(source, snapshot, structureLimits)
        val selection = ArchiveSelection.resolve(snapshot, selectedPaths)
        val budgetAssessment = ArchiveResourceBudgetEvaluator.assess(selection, resourceBudget)
        validateSelection(
            selection = selection,
            budgetAssessment = budgetAssessment,
            format = snapshot.format,
            passwordProvided = snapshot.readerOptions.hasPassword,
            skipUnsafePaths = skipUnsafePaths,
            allowResourceBudgetOverride = allowResourceBudgetOverride,
        )
        val enforcedBudget = if (allowResourceBudgetOverride) {
            resourceBudget.expandedToInclude(budgetAssessment)
        } else {
            resourceBudget
        }
        preflightDirectoryMetadata(source, snapshot) {
            extractionContext.ensureActive()
        }

        progress.onProgress(
            ExtractionProgress(
                phase = ExtractionPhase.PREPARING,
                currentPath = null,
                completedEntries = 0,
                totalEntries = selection.totalEntries,
                bytesWritten = 0L,
                totalBytes = selection.totalUncompressedBytes,
            ),
        )
        currentCoroutineContext().ensureActive()

        var root: ArchiveOutputWriter.Node? = null
        var committed = false
        try {
            root = writer.createRoot(safeRootName)
            currentCoroutineContext().ensureActive()
            val conflictController = ArchiveExtractionConflictController(
                policy = conflictPolicy,
                resolver = conflictResolver,
            )
            val directories = HashMap<String, PlannedDirectory?>()
            directories[ArchivePathPolicy.ROOT_PATH] = PlannedDirectory(root, "")
            val activeDirectories = ArrayList<String>(selection.directories.size)
            val plannedFiles = ArrayList<PlannedFile>(selection.files.size)
            var directoriesCreated = 0
            var entriesSkipped = 0
            var entriesOverwritten = 0
            var entriesAutoRenamed = 0

            selection.directories.forEach { directoryPath ->
                currentCoroutineContext().ensureActive()
                val parentPath = parentPath(directoryPath)
                if (!directories.containsKey(parentPath)) {
                    extractionFailure("Extraction directory parent is missing")
                }
                val parent = directories[parentPath]
                if (parent == null) {
                    directories[directoryPath] = null
                    entriesSkipped++
                    return@forEach
                }
                val allocation = allocateOutputNode(
                    writer = writer,
                    parent = parent,
                    archivePath = directoryPath,
                    requestedDisplayName = displayName(directoryPath),
                    incomingIsDirectory = true,
                    structureLimits = structureLimits,
                    conflictController = conflictController,
                )
                when (allocation.decision) {
                    ArchiveExtractionConflictDecision.SKIP -> entriesSkipped++
                    ArchiveExtractionConflictDecision.OVERWRITE -> entriesOverwritten++
                    ArchiveExtractionConflictDecision.AUTO_RENAME -> entriesAutoRenamed++
                    null -> Unit
                }
                val directory = allocation.node?.let { node ->
                    PlannedDirectory(
                        node = node,
                        relativePath = childPath(parent.relativePath, node.location.displayName),
                    )
                }
                directories[directoryPath] = directory
                if (directory != null) {
                    activeDirectories += directoryPath
                    if (allocation.created) directoriesCreated++
                }
            }

            selection.files.forEach { entry ->
                currentCoroutineContext().ensureActive()
                val parentPath = parentPath(entry.path)
                if (!directories.containsKey(parentPath)) {
                    extractionFailure("Extraction file parent is missing")
                }
                val parent = directories[parentPath]
                if (parent == null) {
                    entriesSkipped++
                    return@forEach
                }
                val allocation = allocateOutputNode(
                    writer = writer,
                    parent = parent,
                    archivePath = entry.path,
                    requestedDisplayName = entry.displayName,
                    incomingIsDirectory = false,
                    structureLimits = structureLimits,
                    conflictController = conflictController,
                )
                when (allocation.decision) {
                    ArchiveExtractionConflictDecision.SKIP -> entriesSkipped++
                    ArchiveExtractionConflictDecision.OVERWRITE -> entriesOverwritten++
                    ArchiveExtractionConflictDecision.AUTO_RENAME -> entriesAutoRenamed++
                    null -> Unit
                }
                allocation.node?.let { plannedFiles += PlannedFile(entry, it) }
            }

            val totalEntries = activeDirectories.size + plannedFiles.size
            val totalBytes = plannedFiles.fold(0L) { total, planned ->
                checkedAdd(total, planned.entry.uncompressedSize, Long.MAX_VALUE) {
                    ArchiveFailureCode.MALFORMED_ARCHIVE
                }
            }
            var completedEntries = 0
            var bytesWritten = 0L
            activeDirectories.forEach { directoryPath ->
                completedEntries++
                progress.onProgress(
                    ExtractionProgress(
                        phase = ExtractionPhase.EXTRACTING,
                        currentPath = directoryPath,
                        completedEntries = completedEntries,
                        totalEntries = totalEntries,
                        bytesWritten = bytesWritten,
                        totalBytes = totalBytes,
                    ),
                )
            }

            engine.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                plannedFiles.forEach { planned ->
                    currentCoroutineContext().ensureActive()
                    val entry = planned.entry
                    val liveEntry = reader.entryAt(entry.ordinal)
                        ?: changed("Selected archive entry no longer exists")
                    validateCentralEntry(
                        liveEntry = liveEntry,
                        snapshotEntry = entry,
                        requireExtractable = true,
                        passwordProvided = snapshot.readerOptions.hasPassword,
                        format = snapshot.format,
                    )
                    val measurement = writer.openFile(planned.node).use { rawOutput ->
                        reader.openEntry(liveEntry).use { rawInput ->
                            copyEntry(
                                input = BufferedInputStream(rawInput),
                                output = BufferedOutputStream(rawOutput),
                                entry = entry,
                                compressedSize = liveEntry.compressedSize,
                                totalBeforeEntry = bytesWritten,
                                totalExpected = totalBytes,
                                completedEntries = completedEntries,
                                totalEntries = totalEntries,
                                budget = enforcedBudget,
                                progress = progress,
                            )
                        }
                    }
                    bytesWritten = checkedAdd(
                        bytesWritten,
                        measurement.bytes,
                        enforcedBudget.maxTotalUncompressedBytes,
                    ) {
                        ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED
                    }
                    validateMeasurement(entry, liveEntry, measurement, enforcedBudget)
                    completedEntries++
                    progress.onProgress(
                        ExtractionProgress(
                            phase = ExtractionPhase.EXTRACTING,
                            currentPath = entry.path,
                            completedEntries = completedEntries,
                            totalEntries = totalEntries,
                            bytesWritten = bytesWritten,
                            totalBytes = totalBytes,
                        ),
                    )
                }
            }

            currentCoroutineContext().ensureActive()
            verifySourceIdentity(source, snapshot)
            progress.onProgress(
                ExtractionProgress(
                    phase = ExtractionPhase.COMMITTING,
                    currentPath = null,
                    completedEntries = totalEntries,
                    totalEntries = totalEntries,
                    bytesWritten = bytesWritten,
                    totalBytes = totalBytes,
                ),
            )
            val committedRoot = writer.commitRoot(root)
            committed = true
            val result = ExtractionResult(
                root = committedRoot.location,
                filesExtracted = plannedFiles.size,
                directoriesCreated = directoriesCreated,
                bytesWritten = bytesWritten,
                entriesSkipped = entriesSkipped,
                entriesOverwritten = entriesOverwritten,
                entriesAutoRenamed = entriesAutoRenamed,
            )
            progress.onProgress(
                ExtractionProgress(
                    phase = ExtractionPhase.COMPLETED,
                    currentPath = null,
                    completedEntries = totalEntries,
                    totalEntries = totalEntries,
                    bytesWritten = bytesWritten,
                    totalBytes = totalBytes,
                ),
            )
            result
        } catch (error: Throwable) {
            val mapped = mapExtractionFailure(error)
            root?.takeUnless { committed }?.let { createdRoot ->
                val cleanupFailure = withContext(NonCancellable) {
                    val notificationFailure = runCatching {
                        progress.onProgress(
                            ExtractionProgress(
                                phase = ExtractionPhase.CLEANING_UP,
                                currentPath = null,
                                completedEntries = 0,
                                totalEntries = selection.totalEntries,
                                bytesWritten = 0L,
                                totalBytes = selection.totalUncompressedBytes,
                            ),
                        )
                    }.exceptionOrNull()
                    val deletionFailure = runCatching {
                        writer.deleteRoot(createdRoot)
                    }.exceptionOrNull()
                    deletionFailure?.let { failure ->
                        ArchiveCleanupException(createdRoot.location, failure).also { cleanup ->
                            notificationFailure?.let(cleanup::addSuppressed)
                            runCatching {
                                progress.onProgress(
                                    ExtractionProgress(
                                        phase = ExtractionPhase.CLEANUP_FAILED,
                                        currentPath = null,
                                        completedEntries = 0,
                                        totalEntries = selection.totalEntries,
                                        bytesWritten = 0L,
                                        totalBytes = selection.totalUncompressedBytes,
                                        residualOutputs = listOf(createdRoot.location),
                                    ),
                                )
                            }.exceptionOrNull()?.let(cleanup::addSuppressed)
                        }
                    } ?: notificationFailure
                }
                cleanupFailure?.let(mapped::addSuppressed)
            }
            throw mapped
        }
    }

    private suspend fun allocateOutputNode(
        writer: ArchiveOutputWriter,
        parent: PlannedDirectory,
        archivePath: String,
        requestedDisplayName: String,
        incomingIsDirectory: Boolean,
        structureLimits: ArchiveStructureLimits,
        conflictController: ArchiveExtractionConflictController,
    ): OutputAllocation {
        validateOutputPath(
            parentPath = parent.relativePath,
            displayName = requestedDisplayName,
            isDirectory = incomingIsDirectory,
            structureLimits = structureLimits,
        )
        val existing = writer.findChild(parent.node, requestedDisplayName)
        if (existing == null) {
            return OutputAllocation(
                node = createOutputNode(
                    writer = writer,
                    parent = parent.node,
                    displayName = requestedDisplayName,
                    isDirectory = incomingIsDirectory,
                ),
                created = true,
            )
        }

        val conflict = ArchiveExtractionConflict(
            archivePath = archivePath,
            requestedDisplayName = requestedDisplayName,
            existingDisplayName = existing.location.displayName,
            incomingIsDirectory = incomingIsDirectory,
            existingIsDirectory = existing.isDirectory,
            canOverwrite = writer.canOverwrite(existing, incomingIsDirectory),
        )
        return when (val decision = conflictController.resolve(conflict)) {
            ArchiveExtractionConflictDecision.SKIP -> OutputAllocation(
                node = null,
                created = false,
                decision = decision,
            )
            ArchiveExtractionConflictDecision.OVERWRITE -> OutputAllocation(
                node = existing,
                created = false,
                decision = decision,
            )
            ArchiveExtractionConflictDecision.AUTO_RENAME -> OutputAllocation(
                node = createAutoRenamedOutputNode(
                    writer = writer,
                    parent = parent,
                    requestedDisplayName = requestedDisplayName,
                    incomingIsDirectory = incomingIsDirectory,
                    structureLimits = structureLimits,
                ),
                created = true,
                decision = decision,
            )
        }
    }

    private fun createAutoRenamedOutputNode(
        writer: ArchiveOutputWriter,
        parent: PlannedDirectory,
        requestedDisplayName: String,
        incomingIsDirectory: Boolean,
        structureLimits: ArchiveStructureLimits,
    ): ArchiveOutputWriter.Node {
        val separatorLength = if (parent.relativePath.isEmpty()) 0 else 1
        val maximumNameLength = structureLimits.restrictedToHardLimits().maxPathLength -
            parent.relativePath.length - separatorLength
        for (suffix in 2..MAX_AUTO_RENAME_ATTEMPTS) {
            val candidate = numberedDisplayName(
                requested = requestedDisplayName,
                suffix = suffix,
                isDirectory = incomingIsDirectory,
                maximumLength = maximumNameLength,
            )
            validateOutputPath(
                parentPath = parent.relativePath,
                displayName = candidate,
                isDirectory = incomingIsDirectory,
                structureLimits = structureLimits,
            )
            if (writer.findChild(parent.node, candidate) == null) {
                return createOutputNode(
                    writer = writer,
                    parent = parent.node,
                    displayName = candidate,
                    isDirectory = incomingIsDirectory,
                )
            }
        }
        throw ArchiveExtractionException(
            ArchiveFailureCode.OUTPUT_FAILURE,
            "Cannot allocate a unique extraction entry name",
        )
    }

    private fun createOutputNode(
        writer: ArchiveOutputWriter,
        parent: ArchiveOutputWriter.Node,
        displayName: String,
        isDirectory: Boolean,
    ): ArchiveOutputWriter.Node = if (isDirectory) {
        writer.createDirectory(parent, displayName)
    } else {
        writer.createFile(parent, displayName)
    }

    private fun validateOutputPath(
        parentPath: String,
        displayName: String,
        isDirectory: Boolean,
        structureLimits: ArchiveStructureLimits,
    ) {
        ArchivePathPolicy.validateEntryPath(
            sourceName = childPath(parentPath, displayName),
            isDirectory = isDirectory,
            limits = structureLimits,
        )
    }

    private fun numberedDisplayName(
        requested: String,
        suffix: Int,
        isDirectory: Boolean,
        maximumLength: Int,
    ): String {
        val marker = " ($suffix)"
        if (maximumLength <= marker.length) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
                "An auto-renamed extraction path would exceed the hard path limit",
            )
        }
        val extensionIndex = if (isDirectory) {
            -1
        } else {
            requested.lastIndexOf('.').takeIf { it in 1 until requested.lastIndex } ?: -1
        }
        var stem = if (extensionIndex >= 0) requested.substring(0, extensionIndex) else requested
        var extension = if (extensionIndex >= 0) requested.substring(extensionIndex) else ""
        if (maximumLength - marker.length - extension.length <= 0) {
            stem = requested
            extension = ""
        }
        val stemLimit = maximumLength - marker.length - extension.length
        val truncatedStem = takeWithoutSplittingSurrogate(stem, stemLimit)
        return "$truncatedStem$marker$extension"
    }

    private fun takeWithoutSplittingSurrogate(value: String, maximumLength: Int): String {
        var end = minOf(value.length, maximumLength.coerceAtLeast(0))
        if (
            end in 1 until value.length &&
            value[end - 1].isHighSurrogate() &&
            value[end].isLowSurrogate()
        ) {
            end--
        }
        return value.substring(0, end)
    }

    private suspend fun copyEntry(
        input: BufferedInputStream,
        output: BufferedOutputStream,
        entry: ArchiveEntry,
        compressedSize: Long,
        totalBeforeEntry: Long,
        totalExpected: Long,
        completedEntries: Int,
        totalEntries: Int,
        budget: ArchiveResourceBudget,
        progress: ArchiveProgressListener,
    ): EntryMeasurement {
        val crc32 = CRC32()
        val buffer = ByteArray(BUFFER_SIZE)
        var entryBytes = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            entryBytes = checkedAdd(
                entryBytes,
                read.toLong(),
                budget.maxSingleUncompressedBytes,
            ) {
                ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED
            }
            val totalBytes = checkedAdd(
                totalBeforeEntry,
                entryBytes,
                budget.maxTotalUncompressedBytes,
            ) {
                ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED
            }
            validateCompressionRatio(entryBytes, compressedSize, budget)
            output.write(buffer, 0, read)
            crc32.update(buffer, 0, read)
            progress.onProgress(
                ExtractionProgress(
                    phase = ExtractionPhase.EXTRACTING,
                    currentPath = entry.path,
                    completedEntries = completedEntries,
                    totalEntries = totalEntries,
                    bytesWritten = totalBytes,
                    totalBytes = totalExpected,
                ),
            )
        }
        output.flush()
        return EntryMeasurement(entryBytes, crc32.value)
    }

    private fun validateSnapshot(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        structureLimits: ArchiveStructureLimits,
    ) {
        if (!source.isRegularFile) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SOURCE_NOT_FILE,
                "Archive source is not a regular file",
            )
        }
        verifySourceIdentity(source, snapshot)
        if (!structureLimits.isWithinHardLimits()) {
            changed("Archive snapshot exceeds the hard structure limits")
        }
        if (snapshot.entries.size > structureLimits.maxEntries) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                "Archive snapshot exceeds the hard entry limit",
            )
        }
        var total = 0L
        snapshot.isolatedPathRoot?.let { isolatedRoot ->
            val validatedRoot = ArchivePathPolicy.validateEntryPath(
                isolatedRoot,
                isDirectory = true,
                limits = structureLimits,
            )
            if (validatedRoot.path != isolatedRoot || '/' in isolatedRoot) {
                changed("Archive snapshot isolation root is inconsistent")
            }
        }
        val containsIsolatedEntries = snapshot.entries.any {
            it.pathStatus == ArchiveEntryPathStatus.UNSAFE_ISOLATED
        }
        if ((snapshot.isolatedPathRoot != null) != containsIsolatedEntries) {
            changed("Archive snapshot isolation metadata is inconsistent")
        }
        snapshot.entries.forEach { entry ->
            validateSnapshotPath(entry, snapshot, structureLimits)
            if (entry.uncompressedSize < 0L) {
                throw ArchiveExtractionException(
                    ArchiveFailureCode.MALFORMED_ARCHIVE,
                    "Archive snapshot contains invalid size metadata",
                )
            }
            total = checkedAdd(total, entry.uncompressedSize, Long.MAX_VALUE) {
                ArchiveFailureCode.MALFORMED_ARCHIVE
            }
        }
        if (total != snapshot.totalUncompressedBytes) {
            changed("Archive snapshot total size is inconsistent")
        }
    }

    private fun validateSelection(
        selection: ResolvedArchiveSelection,
        budgetAssessment: ArchiveResourceBudgetAssessment,
        format: ArchiveFormat,
        passwordProvided: Boolean,
        skipUnsafePaths: Boolean,
        allowResourceBudgetOverride: Boolean,
    ) {
        if (selection.skippedUnsafeEntries.isNotEmpty() && !skipUnsafePaths) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
                "Unsafe archive paths require explicit skip confirmation",
                format = format,
            )
        }
        if (selection.totalEntries == 0 && selection.skippedUnsafeEntries.isNotEmpty()) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.EMPTY_SELECTION,
                "The selection contains no safely extractable entries",
                format = format,
            )
        }
        var total = 0L
        selection.files.forEach { entry ->
            if (!entry.canExtract) {
                throw ArchiveExtractionException(
                    entry.unavailableExtractionCode(passwordProvided),
                    "Selected archive entry data is unavailable to this backend",
                    format = format,
                )
            }
            total = checkedAdd(total, entry.uncompressedSize, Long.MAX_VALUE) {
                ArchiveFailureCode.MALFORMED_ARCHIVE
            }
        }
        if (total != selection.totalUncompressedBytes) {
            changed("Archive selection size is inconsistent")
        }
        if (!allowResourceBudgetOverride && budgetAssessment.exceedsBudget) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
                "Archive selection exceeds the configured resource budget",
                format = format,
            )
        }
    }

    private fun validateSnapshotPath(
        entry: ArchiveEntry,
        snapshot: ArchiveSnapshot,
        structureLimits: ArchiveStructureLimits,
    ) {
        when (entry.pathStatus) {
            ArchiveEntryPathStatus.SAFE -> {
                if (snapshot.isIsolatedPath(entry.path)) {
                    changed("Archive snapshot safe path overlaps the isolation root")
                }
                val validated = ArchivePathPolicy.validateEntryPath(
                    entry.sourceName,
                    entry.isDirectory,
                    structureLimits,
                )
                if (validated.path != entry.path || validated.displayName != entry.displayName) {
                    changed("Archive snapshot path metadata is inconsistent")
                }
            }
            ArchiveEntryPathStatus.UNSAFE_ISOLATED -> {
                val isolatedRoot = snapshot.isolatedPathRoot
                    ?: changed("Archive snapshot isolation root is missing")
                val expected = ArchivePathPolicy.isolatedEntryPath(
                    isolatedRoot,
                    entry.ordinal,
                    entry.isDirectory,
                    structureLimits,
                )
                if (
                    entry.path != expected.path ||
                    entry.displayName != ArchivePathPolicy.unsafeSourceNameForDisplay(entry.sourceName) ||
                    entry.canExtract
                ) {
                    changed("Archive snapshot isolated path metadata is inconsistent")
                }
                try {
                    ArchivePathPolicy.validateEntryPath(
                        entry.sourceName,
                        entry.isDirectory,
                        structureLimits,
                    )
                    changed("Archive snapshot isolated a safe source path")
                } catch (error: ArchiveValidationException) {
                    if (error.code != ArchiveFailureCode.INVALID_PATH) throw error
                }
            }
        }
    }

    private fun preflightDirectoryMetadata(
        source: ArchiveReadSource,
        snapshot: ArchiveSnapshot,
        cancellationCheck: () -> Unit,
    ) {
        try {
            engine.openReader(
                source = source,
                format = snapshot.format,
                options = snapshot.readerOptions,
            ).use { reader ->
                var ordinal = 0
                reader.entries.forEach { liveEntry ->
                    if (ordinal % PREFLIGHT_CANCELLATION_CHECK_INTERVAL == 0) cancellationCheck()
                    if (ordinal >= snapshot.entries.size) changed("Archive entry count changed")
                    validateCentralEntry(
                        liveEntry = liveEntry,
                        snapshotEntry = snapshot.entries[ordinal],
                        passwordProvided = snapshot.readerOptions.hasPassword,
                        format = snapshot.format,
                    )
                    ordinal++
                }
                if (ordinal != snapshot.entries.size) changed("Archive entry count changed")
            }
        } catch (error: ArchiveException) {
            throw error
        } catch (error: IOException) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SOURCE_CHANGED,
                "Archive cannot be reopened for extraction",
                error,
            )
        }
    }

    private fun validateCentralEntry(
        liveEntry: ArchiveReaderEntry,
        snapshotEntry: ArchiveEntry,
        requireExtractable: Boolean = false,
        passwordProvided: Boolean,
        format: ArchiveFormat,
    ) {
        if (requireExtractable && !liveEntry.capabilities.canExtract) {
            throw ArchiveExtractionException(
                liveEntry.unavailableExtractionCode(passwordProvided),
                "Archive entry data is unavailable to this backend",
                format = format,
            )
        }
        val same = liveEntry.name == snapshotEntry.sourceName &&
            liveEntry.isDirectory == snapshotEntry.isDirectory &&
            liveEntry.compressionMethod == snapshotEntry.compressionMethod &&
            liveEntry.compressionMethodId == snapshotEntry.compressionMethodId &&
            liveEntry.capabilities == snapshotEntry.capabilities &&
            liveEntry.isEncrypted == snapshotEntry.isEncrypted &&
            liveEntry.encryptionMethod == snapshotEntry.encryptionMethod &&
            liveEntry.size == snapshotEntry.uncompressedSize &&
            liveEntry.compressedSize == snapshotEntry.compressedSize &&
            liveEntry.crc == snapshotEntry.crc32
        if (!same) changed("Archive directory metadata changed")
    }

    private fun validateMeasurement(
        entry: ArchiveEntry,
        liveEntry: ArchiveReaderEntry,
        measurement: EntryMeasurement,
        budget: ArchiveResourceBudget,
    ) {
        if (measurement.bytes != entry.uncompressedSize || measurement.bytes != liveEntry.size) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.SIZE_MISMATCH,
                "Extracted entry size does not match the scanned size",
            )
        }
        if (entry.crc32 != null && measurement.crc32 != entry.crc32) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.CRC_MISMATCH,
                "Extracted entry CRC does not match the scanned CRC",
            )
        }
        validateCompressionRatio(measurement.bytes, liveEntry.compressedSize, budget)
    }

    private fun validateCompressionRatio(
        uncompressedSize: Long,
        compressedSize: Long,
        budget: ArchiveResourceBudget,
    ) {
        if (uncompressedSize == 0L || compressedSize < 0L) return
        if (compressedSize == 0L && budget.maxCompressionRatio == Long.MAX_VALUE) return
        val threshold = if (compressedSize <= 0L) {
            0L
        } else if (compressedSize > Long.MAX_VALUE / budget.maxCompressionRatio) {
            Long.MAX_VALUE
        } else {
            compressedSize * budget.maxCompressionRatio
        }
        if (uncompressedSize > threshold) {
            throw ArchiveExtractionException(
                ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
                "Archive entry exceeds the compression-ratio limit",
            )
        }
    }

    private fun ArchiveEntry.unavailableExtractionCode(passwordProvided: Boolean): ArchiveFailureCode =
        when {
            ArchiveEntryLimitation.MISSING_VOLUME in capabilities.limitations ->
                ArchiveFailureCode.MISSING_VOLUME
            isEncrypted && !passwordProvided -> ArchiveFailureCode.PASSWORD_REQUIRED
            else -> ArchiveFailureCode.UNSUPPORTED_METHOD
        }

    private fun ArchiveReaderEntry.unavailableExtractionCode(passwordProvided: Boolean): ArchiveFailureCode =
        when {
            ArchiveEntryLimitation.MISSING_VOLUME in capabilities.limitations ->
                ArchiveFailureCode.MISSING_VOLUME
            isEncrypted && !passwordProvided -> ArchiveFailureCode.PASSWORD_REQUIRED
            else -> ArchiveFailureCode.UNSUPPORTED_METHOD
        }

    private fun verifySourceIdentity(source: ArchiveReadSource, snapshot: ArchiveSnapshot) {
        val identity = try {
            source.identity()
        } catch (_: Exception) {
            changed("Archive source cannot be inspected after scanning")
        }
        if (identity.length != snapshot.sourceLength ||
            identity.lastModifiedMillis != snapshot.sourceLastModifiedMillis
        ) {
            changed("Archive source changed after scanning")
        }
    }

    private fun checkedAdd(
        current: Long,
        increment: Long,
        limit: Long,
        code: () -> ArchiveFailureCode,
    ): Long {
        if (increment < 0L || current > limit - increment) {
            throw ArchiveExtractionException(code(), "Archive extraction limit exceeded")
        }
        return current + increment
    }

    private fun parentPath(path: String): String = path.substringBeforeLast('/', "")

    private fun displayName(path: String): String = path.substringAfterLast('/')

    private fun childPath(parentPath: String, displayName: String): String =
        if (parentPath.isEmpty()) displayName else "$parentPath/$displayName"

    private fun changed(message: String): Nothing = throw ArchiveExtractionException(
        ArchiveFailureCode.SOURCE_CHANGED,
        message,
    )

    private fun extractionFailure(message: String): Nothing = throw ArchiveExtractionException(
        ArchiveFailureCode.OUTPUT_FAILURE,
        message,
    )

    private fun mapExtractionFailure(error: Throwable): Throwable = when (error) {
        is CancellationException -> error
        is ArchiveException -> error
        else -> ArchiveExtractionException(
            ArchiveFailureCode.OUTPUT_FAILURE,
            "Archive extraction failed",
            error,
        )
    }

    private data class EntryMeasurement(
        val bytes: Long,
        val crc32: Long,
    )

    private data class PlannedDirectory(
        val node: ArchiveOutputWriter.Node,
        val relativePath: String,
    )

    private data class PlannedFile(
        val entry: ArchiveEntry,
        val node: ArchiveOutputWriter.Node,
    )

    private data class OutputAllocation(
        val node: ArchiveOutputWriter.Node?,
        val created: Boolean,
        val decision: ArchiveExtractionConflictDecision? = null,
    )

    private companion object {
        const val BUFFER_SIZE = 32 * 1_024
        const val MAX_AUTO_RENAME_ATTEMPTS = 10_000
        const val PREFLIGHT_CANCELLATION_CHECK_INTERVAL = 64
    }
}
