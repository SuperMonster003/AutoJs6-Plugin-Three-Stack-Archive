package io.github.supermonster003.autojs6.plugin.archivemanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.Formatter
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.supermonster003.autojs6.plugin.archivemanager.databinding.ActivityArchiveManagerBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

class ArchiveManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArchiveManagerBinding
    private lateinit var adapter: ArchiveEntryAdapter

    private var request: ArchiveOpenRequest? = null
    @Volatile
    private var stagedArchive: StagedArchive? = null
    private var snapshot: ArchiveSnapshot? = null
    private var index: ArchiveIndex? = null
    private var currentDirectory = ArchivePathPolicy.ROOT_PATH
    private val selectedPaths = linkedSetOf<String>()
    private var pendingExtractionPaths: Set<String> = emptySet()
    private var pendingOutputTreeUri: Uri? = null
    private var operationJob: Job? = null
    private var renderJob: Job? = null
    private var renderGeneration = 0
    private var isBusy = false
    private var directActionHandled = false
    private var selectedFilenameCharsetName: String? = null
    private var selectedPassword: CharArray? = null
    private var filenameCharsetChoices: List<FilenameCharsetChoice> = emptyList()
    private var lastFailureDiagnostic: ArchiveFailureDiagnostic? = null

    private val outputTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri == null) {
            pendingExtractionPaths = emptySet()
            if (
                request?.requestedAction == ArchiveRequestedAction.EXTRACT_TO &&
                selectedPaths == setOf(ArchivePathPolicy.ROOT_PATH)
            ) {
                selectedPaths.clear()
                renderEntries()
            }
        } else if (stagedArchive != null && snapshot != null) {
            extractTo(treeUri)
        } else {
            pendingOutputTreeUri = treeUri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        restoreInstanceState(savedInstanceState)

        val resolvedRequest = ArchiveIntentPolicy.resolve(intent)
        if (resolvedRequest == null) {
            Toast.makeText(this, R.string.error_permission_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        request = resolvedRequest
        setupViews()
        loadArchive(resolvedRequest)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CURRENT_DIRECTORY, currentDirectory)
        outState.putBoundedStringList(STATE_SELECTED_PATHS, selectedPaths)
        val pendingSaved = outState.putBoundedStringList(
            STATE_PENDING_EXTRACTION_PATHS,
            pendingExtractionPaths,
        )
        if (pendingSaved) {
            pendingOutputTreeUri?.let {
                outState.putString(STATE_PENDING_OUTPUT_TREE_URI, it.toString())
            }
        }
        outState.putBoolean(STATE_DIRECT_ACTION_HANDLED, directActionHandled)
        selectedFilenameCharsetName?.let {
            outState.putString(STATE_FILENAME_CHARSET, it)
        }
    }

    override fun onDestroy() {
        clearSelectedPassword()
        if (::binding.isInitialized) binding.archivePassword.text?.clear()
        val activeOperation = operationJob
        if (activeOperation?.isActive == true) {
            activeOperation.invokeOnCompletion { clearStagedArchive() }
        } else {
            clearStagedArchive()
        }
        super.onDestroy()
    }

    private fun setupViews() = with(binding) {
        toolbar.setNavigationOnClickListener { handleBack() }
        adapter = ArchiveEntryAdapter(
            onOpenDirectory = { row ->
                currentDirectory = row.path
                searchInput.setText("")
                renderEntries()
            },
            onSelectionChanged = { row, checked ->
                if (checked) selectedPaths += row.path else selectedPaths -= row.path
                renderEntries()
            },
        )
        entryList.layoutManager = LinearLayoutManager(this@ArchiveManagerActivity)
        entryList.adapter = adapter
        searchInput.doAfterTextChanged { renderEntries() }
        upButton.setOnClickListener {
            currentDirectory = currentDirectory.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            renderEntries()
        }
        selectAllButton.setOnClickListener { selectAllVisibleEntries() }
        extractButton.setOnClickListener { chooseExtractionDestination() }
        cancelButton.setOnClickListener { operationJob?.cancel() }
        filenameEncodingInput.setOnItemClickListener { _, _, position, _ ->
            filenameCharsetChoices.getOrNull(position)?.let(::selectFilenameCharset)
        }
        filenameEncodingInput.setOnClickListener { filenameEncodingInput.showDropDown() }
        applyPasswordButton.setOnClickListener { applyPassword() }
        archivePassword.setOnEditorActionListener { _, _, _ ->
            applyPassword()
            true
        }
        diagnosticCopyButton.setOnClickListener { copyLastDiagnostic() }
        filenameEncodingLayout.isVisible = false
        passwordControls.isVisible = false
        diagnosticCopyButton.isVisible = false
        selectedCount.text = getString(R.string.text_selected_count, 0)

        onBackPressedDispatcher.addCallback(
            this@ArchiveManagerActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBack()
            },
        )
    }

    private fun handleBack() {
        if (isBusy) {
            operationJob?.cancel()
            return
        }
        if (binding.searchInput.text?.isNotEmpty() == true) {
            binding.searchInput.setText("")
            return
        }
        if (currentDirectory.isNotEmpty()) {
            currentDirectory = currentDirectory.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            renderEntries()
            return
        }
        finish()
    }

    private fun loadArchive(request: ArchiveOpenRequest) {
        binding.archiveName.text = request.displayName
        setBusy(true, getString(R.string.text_loading_archive), cancellable = true)
        operationJob = lifecycleScope.launch {
            try {
                val staged = withContext(Dispatchers.IO) {
                    ArchiveCacheStager.stage(
                        contentResolver = contentResolver,
                        source = request.archiveUri,
                        cacheDirectory = cacheDir,
                        reportedSize = request.reportedSize,
                    ) { copied ->
                        postUiUpdate {
                            binding.archiveSummary.text = getString(
                                R.string.text_loading_progress,
                                formatBytes(copied),
                                formatBytes(request.reportedSize),
                            )
                        }
                    }.also { stagedArchive = it }
                }
                configureFilenameEncoding(snapshot = null)
                val (scanned, scannedIndex) = scanStagedArchive(staged)
                applyScannedArchive(scanned, scannedIndex)
                resumeRequestedActionAfterScan()
            } catch (cancelled: CancellationException) {
                clearStagedArchive()
                showMessage(getString(R.string.text_cancelled))
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                if (stagedArchive == null) {
                    binding.filenameEncodingLayout.isVisible = false
                } else {
                    configureFilenameEncoding(snapshot)
                }
                showFailure(
                    error = error,
                    headline = headlineForLoadFailure(error),
                    stageHint = if (stagedArchive == null) {
                        ArchiveFailureStage.INPUT
                    } else {
                        ArchiveFailureStage.INDEX
                    },
                )
                setBusy(false)
            }
        }
    }

    private suspend fun scanStagedArchive(
        staged: StagedArchive,
    ): Pair<ArchiveSnapshot, ArchiveIndex> = withContext(Dispatchers.IO) {
        val readerOptions = ArchiveReaderOptions(
            filenameCharsetName = selectedFilenameCharsetName,
            password = selectedPassword,
        )
        try {
            ArchiveScanner().scan(
                source = staged.file,
                options = readerOptions,
            ) { ensureActive() }.let { scanned ->
                scanned to ArchiveIndex(scanned)
            }
        } finally {
            readerOptions.clearPassword()
        }
    }

    private fun applyScannedArchive(scanned: ArchiveSnapshot, scannedIndex: ArchiveIndex) {
        snapshot?.readerOptions?.clearPassword()
        snapshot = scanned
        index = scannedIndex
        val currentNode = scannedIndex.node(currentDirectory)
        if (currentDirectory.isNotEmpty() && currentNode?.isDirectory != true) {
            currentDirectory = ArchivePathPolicy.ROOT_PATH
        }
        selectedPaths.retainAll { scannedIndex.node(it) != null }
        binding.archiveSummary.text = resources.getQuantityString(
            R.plurals.text_archive_summary,
            scanned.entries.size,
            scanned.entries.size,
            formatBytes(scanned.totalUncompressedBytes),
        )
        lastFailureDiagnostic = null
        binding.diagnosticCopyButton.isVisible = false
        configureFilenameEncoding(scanned)
        configurePassword(scanned)
        setBusy(false)
        renderEntries()
    }

    private fun selectFilenameCharset(choice: FilenameCharsetChoice) {
        if (choice.charsetName == selectedFilenameCharsetName || isBusy) return
        val staged = stagedArchive ?: return
        val previous = selectedFilenameCharsetName
        selectedFilenameCharsetName = choice.charsetName
        setBusy(true, getString(R.string.text_reindexing_archive), cancellable = true)
        operationJob = lifecycleScope.launch {
            try {
                val (scanned, scannedIndex) = scanStagedArchive(staged)
                applyScannedArchive(scanned, scannedIndex)
                resumeRequestedActionAfterScan()
            } catch (cancelled: CancellationException) {
                selectedFilenameCharsetName = previous
                configureFilenameEncoding(snapshot)
                showMessage(getString(R.string.text_cancelled))
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                if (snapshot != null) selectedFilenameCharsetName = previous
                configureFilenameEncoding(snapshot)
                showFailure(
                    error = error,
                    headline = headlineForLoadFailure(error),
                    stageHint = ArchiveFailureStage.INDEX,
                )
                setBusy(false)
                renderEntries()
            }
        }
    }

    private fun configureFilenameEncoding(snapshot: ArchiveSnapshot?) {
        val format = snapshot?.format ?: ArchiveFormat.ZIP
        val supported = ArchiveEngine.DEFAULT.capabilities(format).filenameCharsetNames
        if (supported.isEmpty()) {
            filenameCharsetChoices = emptyList()
            binding.filenameEncodingLayout.isVisible = false
            return
        }
        if (selectedFilenameCharsetName != null && selectedFilenameCharsetName !in supported) {
            selectedFilenameCharsetName = null
        }
        val detected = snapshot?.readerOptions?.filenameCharsetName
            ?.takeIf { selectedFilenameCharsetName == null }
        val automaticLabel = detected?.let {
            getString(R.string.text_filename_encoding_automatic_detected, it)
        } ?: getString(R.string.text_filename_encoding_automatic)
        filenameCharsetChoices = buildList {
            add(FilenameCharsetChoice(automaticLabel, null))
            supported.forEach { charsetName ->
                add(FilenameCharsetChoice(charsetName, charsetName))
            }
        }
        binding.filenameEncodingInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                filenameCharsetChoices.map(FilenameCharsetChoice::label),
            ),
        )
        val selected = filenameCharsetChoices.first {
            it.charsetName == selectedFilenameCharsetName
        }
        binding.filenameEncodingInput.setText(selected.label, false)
        binding.filenameEncodingLayout.isVisible = true
        binding.filenameEncodingLayout.isEnabled = !isBusy
    }

    private fun applyPassword() {
        if (isBusy) return
        val staged = stagedArchive ?: return
        val candidate = binding.archivePassword.text?.let { editable ->
            CharArray(editable.length) { index -> editable[index] }
        }?.takeIf(CharArray::isNotEmpty)
        if (candidate == null) {
            binding.archivePasswordLayout.error = getString(R.string.error_password_required)
            focusArchivePassword()
            return
        }
        val previous = selectedPassword
        selectedPassword = candidate
        binding.archivePasswordLayout.error = null
        setBusy(true, getString(R.string.text_applying_password), cancellable = true)
        operationJob = lifecycleScope.launch {
            try {
                val (scanned, scannedIndex) = scanStagedArchive(staged)
                applyScannedArchive(scanned, scannedIndex)
                previous?.fill('\u0000')
                resumeRequestedActionAfterScan()
            } catch (cancelled: CancellationException) {
                selectedPassword?.fill('\u0000')
                selectedPassword = previous
                configurePassword(snapshot)
                showMessage(getString(R.string.text_cancelled))
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                selectedPassword?.fill('\u0000')
                selectedPassword = previous
                configurePassword(snapshot)
                val diagnostic = ArchiveFailureDiagnostic.from(
                    error = error,
                    formatHint = snapshot?.format ?: ArchiveFormat.ZIP,
                    stageHint = ArchiveFailureStage.PASSWORD,
                )
                if (diagnostic.code == ArchiveFailureCode.WRONG_PASSWORD) {
                    binding.archivePasswordLayout.error = getString(R.string.error_password_incorrect)
                }
                showFailure(
                    error = error,
                    headline = headlineForLoadFailure(error),
                    stageHint = ArchiveFailureStage.PASSWORD,
                )
                setBusy(false)
                renderEntries()
                focusArchivePassword()
            }
        }
    }

    private fun configurePassword(snapshot: ArchiveSnapshot?) = with(binding) {
        val hasEncryptedEntries = snapshot?.entries?.any(ArchiveEntry::isEncrypted) == true
        passwordControls.isVisible = hasEncryptedEntries
        if (!hasEncryptedEntries) {
            clearSelectedPassword()
            archivePassword.text?.clear()
            archivePasswordLayout.error = null
            return@with
        }
        archivePasswordLayout.helperText = getString(
            if (snapshot.readerOptions.hasPassword) {
                R.string.text_password_applied
            } else {
                R.string.text_password_needed_for_encrypted_entries
            },
        )
        archivePasswordLayout.isEnabled = !isBusy
        archivePassword.isEnabled = !isBusy
        applyPasswordButton.isEnabled = !isBusy
    }

    private fun focusArchivePassword() {
        binding.archivePassword.requestFocus()
        binding.archivePassword.post {
            getSystemService<InputMethodManager>()?.showSoftInput(
                binding.archivePassword,
                InputMethodManager.SHOW_IMPLICIT,
            )
        }
    }

    private fun renderEntries() {
        val archiveIndex = index ?: return
        val archive = snapshot ?: return
        val query = binding.searchInput.text?.toString().orEmpty().trim()
        val directory = currentDirectory
        val selected = selectedPaths.toSet()
        val generation = ++renderGeneration
        renderJob?.cancel()
        renderJob = lifecycleScope.launch {
            if (query.isNotEmpty()) delay(SEARCH_DEBOUNCE_MILLIS)
            val result = withContext(Dispatchers.Default) {
                val nodes = if (query.isEmpty()) {
                    archiveIndex.children(directory)
                } else {
                    archiveIndex.search(query)
                }
                val rows = nodes.mapIndexed { position, node ->
                    if (position % RENDER_CANCELLATION_INTERVAL == 0) ensureActive()
                    node.toRow(query.isNotEmpty(), selected)
                }
                val selectionCount = if (selected.isEmpty()) {
                    0
                } else {
                    runCatching {
                        ArchiveSelection.resolve(archive, selected, archiveIndex).totalEntries
                    }.getOrDefault(selected.size)
                }
                RenderResult(rows, selectionCount)
            }
            if (generation != renderGeneration || isBusy) return@launch
            binding.currentPath.text = if (query.isEmpty()) "/$directory" else "/"
            binding.upButton.isEnabled = query.isEmpty() && directory.isNotEmpty()
            binding.message.isVisible = result.rows.isEmpty() || lastFailureDiagnostic != null
            if (result.rows.isEmpty() && lastFailureDiagnostic == null) {
                binding.message.text = getString(R.string.text_no_entries)
            }
            binding.entryList.isVisible = result.rows.isNotEmpty()
            adapter.submitList(result.rows)
            binding.selectedCount.text = getString(
                R.string.text_selected_count,
                result.selectionCount,
            )
            binding.extractButton.isEnabled = result.selectionCount > 0
        }
    }

    private fun ArchiveNode.toRow(
        showFullPath: Boolean,
        selected: Set<String>,
    ): ArchiveEntryRow {
        val archiveEntry = entry
        val details = if (isDirectory) {
            resources.getQuantityString(
                R.plurals.text_folder_details,
                descendantFileCount,
                descendantFileCount,
                formatBytes(uncompressedSize),
            )
        } else if (archiveEntry != null) {
            buildString {
                if (snapshot?.format?.isTarFamily == true) {
                    append(formatBytes(archiveEntry.uncompressedSize))
                } else {
                    append(
                        getString(
                            R.string.text_size_details,
                            formatBytes(archiveEntry.uncompressedSize),
                            formatBytes(archiveEntry.compressedSize),
                            archiveEntry.crc32?.let { String.format(Locale.ROOT, "%08X", it) }
                                ?: getString(R.string.text_unknown),
                        ),
                    )
                }
                archiveEntry.modifiedTimeMillis?.let { modified ->
                    append('\n')
                    append(getString(R.string.text_modified, formatDate(modified)))
                }
                if (archiveEntry.isEncrypted) {
                    append('\n')
                    append(
                        getString(
                            if (archiveEntry.canExtract) {
                                R.string.text_encrypted_entry_unlocked
                            } else {
                                R.string.text_encrypted_entry
                            },
                            encryptionMethodLabel(archiveEntry.encryptionMethod),
                        ),
                    )
                } else if (!archiveEntry.canExtract) {
                    append('\n')
                    append(
                        getString(
                            if (
                                ArchiveEntryLimitation.UNSUPPORTED_ENTRY_TYPE in
                                archiveEntry.capabilities.limitations
                            ) {
                                R.string.text_unsupported_entry_type
                            } else {
                                R.string.text_unsupported_compression_method
                            },
                            archiveEntry.compressionMethodId,
                        ),
                    )
                }
            }
        } else {
            getString(R.string.text_unknown)
        }
        return ArchiveEntryRow(
            path = path,
            displayName = if (showFullPath) path else name,
            details = details,
            isDirectory = isDirectory,
            isBlocked = archiveEntry?.canExtract == false,
            isSelected = path in selected,
        )
    }

    private fun selectAllVisibleEntries() {
        adapter.currentList.forEach { row ->
            if (!row.isBlocked) selectedPaths += row.path
        }
        renderEntries()
    }

    private fun chooseExtractionDestination() {
        val archive = snapshot
        val archiveIndex = index
        if (archive == null || archiveIndex == null || selectedPaths.isEmpty()) {
            Toast.makeText(this, R.string.error_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        pendingExtractionPaths = selectedPaths.toSet()
        val passwordRequired = runCatching {
            ArchiveSelection.resolve(archive, pendingExtractionPaths, archiveIndex).files.any {
                it.isEncrypted && !archive.readerOptions.hasPassword
            }
        }.getOrDefault(false)
        if (passwordRequired) {
            binding.archivePasswordLayout.error = getString(R.string.error_password_required)
            showMessage(getString(R.string.error_password_required))
            focusArchivePassword()
            return
        }
        outputTreeLauncher.launch(null)
    }

    private fun startDirectExtractionIfRequested(resolvedRequest: ArchiveOpenRequest) {
        if (
            resolvedRequest.requestedAction != ArchiveRequestedAction.EXTRACT_TO ||
            directActionHandled
        ) {
            return
        }
        directActionHandled = true
        selectedPaths.clear()
        selectedPaths += ArchivePathPolicy.ROOT_PATH
        renderEntries()
        chooseExtractionDestination()
    }

    private fun resumeRequestedActionAfterScan() {
        pendingOutputTreeUri?.let { treeUri ->
            pendingOutputTreeUri = null
            extractTo(treeUri)
        } ?: if (pendingExtractionPaths.isNotEmpty()) {
            chooseExtractionDestination()
        } else {
            request?.let(::startDirectExtractionIfRequested)
        }
    }

    private fun extractTo(treeUri: Uri) {
        val staged = stagedArchive ?: return
        val archive = snapshot ?: return
        val paths = pendingExtractionPaths.takeIf(Set<String>::isNotEmpty) ?: return
        pendingExtractionPaths = emptySet()
        setBusy(true, getString(R.string.text_extracting, 0, 0), cancellable = true)
        operationJob = lifecycleScope.launch {
            try {
                val rootName = extractionRootName(
                    displayName = request?.displayName.orEmpty(),
                    format = archive.format,
                )
                val result = withContext(Dispatchers.IO) {
                    var lastReportedBytes = -PROGRESS_REPORT_BYTES
                    var lastCompletedEntries = -1
                    ArchiveExtractor(contentResolver, archive.limits).extract(
                        source = staged.file,
                        snapshot = archive,
                        selectedPaths = paths,
                        treeUri = treeUri,
                        rootName = rootName,
                        progress = ArchiveProgressListener { update ->
                            when (update.phase) {
                                ExtractionPhase.CLEANING_UP -> postUiUpdate {
                                    binding.message.text = getString(R.string.text_cleaning_up)
                                    binding.cancelButton.isEnabled = false
                                }
                                ExtractionPhase.EXTRACTING -> if (
                                    update.bytesWritten - lastReportedBytes >= PROGRESS_REPORT_BYTES ||
                                    update.completedEntries == update.totalEntries ||
                                    update.completedEntries - lastCompletedEntries >= PROGRESS_REPORT_ENTRIES
                                ) {
                                    lastReportedBytes = update.bytesWritten
                                    lastCompletedEntries = update.completedEntries
                                    postUiUpdate {
                                        binding.message.text = getString(
                                            R.string.text_extracting,
                                            update.completedEntries,
                                            update.totalEntries,
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        },
                    )
                }
                val completedMessage = resources.getQuantityString(
                    R.plurals.text_extraction_complete,
                    result.filesExtracted,
                    result.filesExtracted,
                    result.root.displayName,
                )
                showMessage(completedMessage)
                Toast.makeText(this@ArchiveManagerActivity, completedMessage, Toast.LENGTH_LONG).show()
                setBusy(false)
                renderEntries()
            } catch (cancelled: CancellationException) {
                val message = if (cancelled.suppressed.isNotEmpty()) {
                    getString(R.string.error_cleanup_failed)
                } else {
                    getString(R.string.text_cancelled)
                }
                showMessage(message)
                if (cancelled.suppressed.isNotEmpty()) {
                    Toast.makeText(this@ArchiveManagerActivity, message, Toast.LENGTH_LONG).show()
                }
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                showFailure(
                    error = error,
                    headline = headlineForExtractionFailure(error),
                    formatHint = archive.format,
                    stageHint = ArchiveFailureStage.ENTRY_DATA,
                )
                setBusy(false)
                renderEntries()
            }
        }
    }

    private fun setBusy(busy: Boolean, status: String? = null, cancellable: Boolean = false) {
        isBusy = busy
        if (busy) {
            renderGeneration++
            renderJob?.cancel()
        }
        binding.progress.isVisible = busy
        binding.cancelButton.isVisible = busy && cancellable
        binding.cancelButton.isEnabled = busy && cancellable
        binding.searchInput.isEnabled = !busy
        binding.filenameEncodingLayout.isEnabled = !busy
        binding.archivePasswordLayout.isEnabled = !busy
        binding.archivePassword.isEnabled = !busy
        binding.applyPasswordButton.isEnabled = !busy
        binding.upButton.isEnabled = !busy && currentDirectory.isNotEmpty()
        binding.selectAllButton.isEnabled = !busy && snapshot != null
        binding.extractButton.isEnabled = !busy && snapshot != null && selectedPaths.isNotEmpty()
        if (busy) binding.diagnosticCopyButton.isVisible = false
        if (status != null) {
            binding.message.text = status
            binding.message.isVisible = true
        } else if (busy) {
            binding.message.isVisible = false
        }
    }

    private fun showMessage(message: String) {
        lastFailureDiagnostic = null
        binding.message.text = message
        binding.message.isVisible = true
        binding.diagnosticCopyButton.isVisible = false
    }

    private fun showFailure(
        error: Throwable,
        headline: String,
        formatHint: ArchiveFormat? = snapshot?.format,
        stageHint: ArchiveFailureStage,
    ) {
        val diagnostic = ArchiveFailureDiagnostic.from(error, formatHint, stageHint)
        lastFailureDiagnostic = diagnostic
        binding.message.text = getString(
            R.string.error_diagnostic_details,
            headline,
            diagnostic.format?.displayName ?: getString(R.string.text_unknown),
            failureStageLabel(diagnostic.stage),
            diagnostic.code?.name ?: getString(R.string.text_unknown),
            failureReason(diagnostic),
        )
        binding.message.isVisible = true
        binding.diagnosticCopyButton.isVisible =
            applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun copyLastDiagnostic() {
        val diagnostic = lastFailureDiagnostic ?: return
        getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.text_archive_diagnostic),
                diagnostic.debugReport(),
            ),
        )
        Toast.makeText(this, R.string.text_diagnostic_copied, Toast.LENGTH_SHORT).show()
    }

    @Synchronized
    private fun clearStagedArchive() {
        snapshot?.readerOptions?.clearPassword()
        snapshot = null
        clearSelectedPassword()
        val staged = stagedArchive
        stagedArchive = null
        staged?.delete()
    }

    @Synchronized
    private fun clearSelectedPassword() {
        selectedPassword?.fill('\u0000')
        selectedPassword = null
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        currentDirectory = savedInstanceState.getString(STATE_CURRENT_DIRECTORY)
            ?: ArchivePathPolicy.ROOT_PATH
        selectedPaths += savedInstanceState.getStringArrayList(STATE_SELECTED_PATHS).orEmpty()
        pendingExtractionPaths = savedInstanceState
            .getStringArrayList(STATE_PENDING_EXTRACTION_PATHS)
            .orEmpty()
            .toSet()
        pendingOutputTreeUri = savedInstanceState.getString(STATE_PENDING_OUTPUT_TREE_URI)
            ?.let(Uri::parse)
        directActionHandled = savedInstanceState.getBoolean(STATE_DIRECT_ACTION_HANDLED, false)
        selectedFilenameCharsetName = savedInstanceState.getString(STATE_FILENAME_CHARSET)
            ?.take(MAX_FILENAME_CHARSET_LENGTH)
    }

    private fun Bundle.putBoundedStringList(key: String, values: Collection<String>): Boolean {
        if (values.size > MAX_SAVED_STATE_PATHS || values.sumOf(String::length) > MAX_SAVED_STATE_CHARS) {
            return false
        }
        putStringArrayList(key, ArrayList(values))
        return true
    }

    private data class RenderResult(
        val rows: List<ArchiveEntryRow>,
        val selectionCount: Int,
    )

    private data class FilenameCharsetChoice(
        val label: String,
        val charsetName: String?,
    )

    private fun postUiUpdate(update: () -> Unit) {
        if (isFinishing || isDestroyed) return
        binding.root.post {
            if (!isFinishing && !isDestroyed) update()
        }
    }

    private fun headlineForLoadFailure(error: Throwable): String = when (error) {
        is ArchiveInputLimitException -> getString(R.string.error_archive_limit)
        is ArchiveException -> when (error.code) {
            ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE,
            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
            ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
            ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
            ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
            -> getString(R.string.error_archive_limit)
            ArchiveFailureCode.PASSWORD_REQUIRED -> getString(R.string.error_password_required)
            ArchiveFailureCode.WRONG_PASSWORD -> getString(R.string.error_password_incorrect)
            else -> getString(R.string.error_archive_invalid)
        }
        else -> getString(R.string.error_cannot_open_archive)
    }

    private fun headlineForExtractionFailure(error: Throwable): String = when (error) {
        is ArchiveException -> when (error.code) {
            ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
            -> getString(R.string.error_archive_limit)
            ArchiveFailureCode.OUTPUT_FAILURE -> getString(R.string.error_cannot_create_output)
            ArchiveFailureCode.PASSWORD_REQUIRED -> getString(R.string.error_password_required)
            ArchiveFailureCode.WRONG_PASSWORD -> getString(R.string.error_password_incorrect)
            else -> getString(R.string.error_extraction_failed)
        }
        else -> getString(R.string.error_extraction_failed)
    }

    private fun failureStageLabel(stage: ArchiveFailureStage): String = getString(
        when (stage) {
            ArchiveFailureStage.INPUT -> R.string.text_failure_stage_input
            ArchiveFailureStage.FORMAT_DETECTION -> R.string.text_failure_stage_format_detection
            ArchiveFailureStage.INDEX -> R.string.text_failure_stage_index
            ArchiveFailureStage.PASSWORD -> R.string.text_failure_stage_password
            ArchiveFailureStage.ENTRY_DATA -> R.string.text_failure_stage_entry_data
            ArchiveFailureStage.OUTPUT -> R.string.text_failure_stage_output
            ArchiveFailureStage.CLEANUP -> R.string.text_failure_stage_cleanup
        },
    )

    private fun failureReason(diagnostic: ArchiveFailureDiagnostic): String = getString(
        when (diagnostic.code) {
            ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE -> R.string.error_reason_cache_space
            ArchiveFailureCode.INVALID_SIGNATURE -> R.string.error_reason_invalid_signature
            ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET ->
                R.string.error_reason_filename_encoding
            ArchiveFailureCode.PASSWORD_REQUIRED,
            ArchiveFailureCode.WRONG_PASSWORD,
            -> R.string.error_reason_password
            ArchiveFailureCode.INVALID_PATH,
            ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
            ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
            ArchiveFailureCode.DUPLICATE_PATH,
            ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
            -> R.string.error_reason_path
            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
            ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
            -> R.string.error_reason_limit
            ArchiveFailureCode.UNSUPPORTED_METHOD -> R.string.error_reason_compression_method
            ArchiveFailureCode.SIZE_MISMATCH,
            ArchiveFailureCode.CRC_MISMATCH,
            -> R.string.error_reason_integrity
            ArchiveFailureCode.INVALID_DESTINATION_NAME,
            ArchiveFailureCode.OUTPUT_FAILURE,
            -> R.string.error_reason_output
            else -> when (diagnostic.stage) {
                ArchiveFailureStage.INPUT -> R.string.error_reason_input
                ArchiveFailureStage.FORMAT_DETECTION -> R.string.error_reason_invalid_signature
                ArchiveFailureStage.INDEX -> R.string.error_reason_index
                ArchiveFailureStage.PASSWORD -> R.string.error_reason_password
                ArchiveFailureStage.ENTRY_DATA -> R.string.error_reason_entry_data
                ArchiveFailureStage.OUTPUT -> R.string.error_reason_output
                ArchiveFailureStage.CLEANUP -> R.string.error_reason_cleanup
            }
        },
    )

    private fun extractionRootName(
        displayName: String,
        format: ArchiveFormat,
    ): String {
        val candidate = (format.baseNameWithoutArchiveExtension(displayName)
            ?: displayName.substringBeforeLast('.', displayName))
            .ifBlank { DEFAULT_EXTRACTION_ROOT }
            .take(MAX_EXTRACTION_ROOT_LENGTH)
        return runCatching { ArchivePathPolicy.validateDestinationRootName(candidate) }
            .getOrDefault(DEFAULT_EXTRACTION_ROOT)
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 0L -> getString(R.string.text_unknown)
        else -> Formatter.formatFileSize(this, bytes)
    }

    private fun formatDate(timeMillis: Long): String {
        val date = Date(timeMillis)
        return DateFormat.getMediumDateFormat(this).format(date) + " " +
            DateFormat.getTimeFormat(this).format(date)
    }

    private fun encryptionMethodLabel(method: ArchiveEncryptionMethod?): String = getString(
        when (method) {
            ArchiveEncryptionMethod.AES -> R.string.text_encryption_method_aes
            ArchiveEncryptionMethod.ZIP_CRYPTO -> R.string.text_encryption_method_zip_crypto
            ArchiveEncryptionMethod.OTHER,
            null,
            -> R.string.text_encryption_method_other
        },
    )

    private companion object {
        const val DEFAULT_EXTRACTION_ROOT = "archive"
        const val MAX_EXTRACTION_ROOT_LENGTH = 120
        const val MAX_FILENAME_CHARSET_LENGTH = 64
        const val MAX_SAVED_STATE_CHARS = 128 * 1024
        const val MAX_SAVED_STATE_PATHS = 2_048
        const val PROGRESS_REPORT_BYTES = 8L * 1024L * 1024L
        const val PROGRESS_REPORT_ENTRIES = 64
        const val RENDER_CANCELLATION_INTERVAL = 64
        const val SEARCH_DEBOUNCE_MILLIS = 150L
        const val STATE_CURRENT_DIRECTORY = "current_directory"
        const val STATE_DIRECT_ACTION_HANDLED = "direct_action_handled"
        const val STATE_FILENAME_CHARSET = "filename_charset"
        const val STATE_PENDING_EXTRACTION_PATHS = "pending_extraction_paths"
        const val STATE_PENDING_OUTPUT_TREE_URI = "pending_output_tree_uri"
        const val STATE_SELECTED_PATHS = "selected_paths"
    }
}
