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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.supermonster003.autojs6.plugin.archivemanager.databinding.ActivityArchiveManagerBinding
import io.github.supermonster003.autojs6.plugin.archivemanager.databinding.DialogArchiveResourceBudgetBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
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
    private var pendingSkipUnsafePaths = false
    private var pendingAllowResourceBudgetOverride = false
    private var pendingOutputTreeUri: Uri? = null
    private var operationJob: Job? = null
    private var renderJob: Job? = null
    private var renderGeneration = 0
    private var isBusy = false
    private var directActionHandled = false
    private var selectedFilenameCharsetName: String? = null
    private var selectedPassword: CharArray? = null
    private var filenameCharsetChoices: List<FilenameCharsetChoice> = emptyList()
    private var selectedResourceBudgetProfile = ArchiveResourceBudgetProfile.COMPATIBLE
    private var customResourceBudget = ArchiveResourceBudget.COMPATIBLE
    private var lastFailureDiagnostic: ArchiveFailureDiagnostic? = null

    private val outputTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri == null) {
            pendingExtractionPaths = emptySet()
            pendingSkipUnsafePaths = false
            pendingAllowResourceBudgetOverride = false
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
        outState.putBoolean(STATE_PENDING_SKIP_UNSAFE_PATHS, pendingSkipUnsafePaths)
        outState.putBoolean(
            STATE_PENDING_RESOURCE_BUDGET_OVERRIDE,
            pendingAllowResourceBudgetOverride,
        )
        outState.putBoolean(STATE_DIRECT_ACTION_HANDLED, directActionHandled)
        outState.putString(STATE_RESOURCE_BUDGET_PROFILE, selectedResourceBudgetProfile.name)
        outState.putInt(STATE_CUSTOM_BUDGET_ENTRIES, customResourceBudget.maxEntries)
        outState.putInt(STATE_CUSTOM_BUDGET_PATH_LENGTH, customResourceBudget.maxPathLength)
        outState.putInt(STATE_CUSTOM_BUDGET_DEPTH, customResourceBudget.maxDepth)
        outState.putLong(STATE_CUSTOM_BUDGET_SINGLE_SIZE, customResourceBudget.maxSingleUncompressedBytes)
        outState.putLong(STATE_CUSTOM_BUDGET_TOTAL_SIZE, customResourceBudget.maxTotalUncompressedBytes)
        outState.putLong(STATE_CUSTOM_BUDGET_RATIO, customResourceBudget.maxCompressionRatio)
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
                pendingSkipUnsafePaths = false
                pendingAllowResourceBudgetOverride = false
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
        extractionBudgetInput.setAdapter(
            ArrayAdapter(
                this@ArchiveManagerActivity,
                android.R.layout.simple_list_item_1,
                resourceBudgetProfiles().map(::resourceBudgetProfileLabel),
            ),
        )
        extractionBudgetInput.setOnItemClickListener { _, _, position, _ ->
            resourceBudgetProfiles().getOrNull(position)?.let(::selectResourceBudgetProfile)
        }
        extractionBudgetInput.setOnClickListener { extractionBudgetInput.showDropDown() }
        updateResourceBudgetInput()
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
                val passwordFailure = configurePasswordFailure(error)
                if (stagedArchive == null || (passwordFailure && snapshot == null)) {
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
                if (passwordFailure) focusArchivePassword()
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
                scanned to ArchiveIndex(
                    scanned,
                    getString(R.string.text_unsafe_paths_folder),
                )
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
                val passwordFailure = configurePasswordFailure(error)
                if (!passwordFailure) configurePassword(snapshot)
                val diagnostic = ArchiveFailureDiagnostic.from(
                    error = error,
                    formatHint = snapshot?.format ?: ArchiveFormat.SEVEN_Z,
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

    private fun configurePasswordFailure(error: Throwable): Boolean {
        val diagnostic = ArchiveFailureDiagnostic.from(
            error = error,
            formatHint = snapshot?.format ?: ArchiveFormat.SEVEN_Z,
            stageHint = ArchiveFailureStage.PASSWORD,
        )
        if (diagnostic.code !in setOf(
                ArchiveFailureCode.PASSWORD_REQUIRED,
                ArchiveFailureCode.WRONG_PASSWORD,
            )
        ) {
            return false
        }
        with(binding) {
            passwordControls.isVisible = true
            archivePasswordLayout.helperText = getString(
                R.string.text_password_needed_for_encrypted_entries,
            )
            archivePasswordLayout.error = if (
                diagnostic.code == ArchiveFailureCode.WRONG_PASSWORD
            ) {
                getString(R.string.error_password_incorrect)
            } else {
                null
            }
            archivePasswordLayout.isEnabled = !isBusy
            archivePassword.isEnabled = !isBusy
            applyPasswordButton.isEnabled = !isBusy
        }
        return true
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
                    node.toRow(query.isNotEmpty(), selected, archiveIndex)
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
            binding.currentPath.text = if (query.isEmpty()) {
                "/${archiveIndex.displayPath(directory)}"
            } else {
                "/"
            }
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
        archiveIndex: ArchiveIndex,
    ): ArchiveEntryRow {
        val archiveEntry = entry
        val isUnsafePath = snapshot?.isIsolatedPath(path) == true ||
            archiveEntry?.isOutputPathSafe == false
        val details = if (isDirectory) {
            buildString {
                append(
                    resources.getQuantityString(
                        R.plurals.text_folder_details,
                        descendantFileCount,
                        descendantFileCount,
                        formatBytes(uncompressedSize),
                    ),
                )
                if (isUnsafePath) {
                    append('\n')
                    append(getString(R.string.text_unsafe_path_read_only))
                }
            }
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
                if (isUnsafePath) {
                    append('\n')
                    append(getString(R.string.text_unsafe_path_read_only))
                }
                if (archiveEntry.isEncrypted) {
                    append('\n')
                    append(
                        getString(
                            if (archiveEntry.capabilities.canExtract) {
                                R.string.text_encrypted_entry_unlocked
                            } else {
                                R.string.text_encrypted_entry
                            },
                            encryptionMethodLabel(archiveEntry.encryptionMethod),
                        ),
                    )
                } else if (!archiveEntry.capabilities.canExtract) {
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
            displayName = if (showFullPath) archiveIndex.displayPath(path) else name,
            details = details,
            isDirectory = isDirectory,
            isBlocked = isUnsafePath || archiveEntry?.canExtract == false,
            isSelected = path in selected,
        )
    }

    private fun selectAllVisibleEntries() {
        pendingSkipUnsafePaths = false
        pendingAllowResourceBudgetOverride = false
        adapter.currentList.forEach { row ->
            if (!row.isBlocked) selectedPaths += row.path
        }
        renderEntries()
    }

    private fun resourceBudgetProfiles(): List<ArchiveResourceBudgetProfile> =
        ArchiveResourceBudgetProfile.entries

    private fun resourceBudgetProfileLabel(profile: ArchiveResourceBudgetProfile): String = getString(
        when (profile) {
            ArchiveResourceBudgetProfile.COMPATIBLE -> R.string.text_budget_profile_compatible
            ArchiveResourceBudgetProfile.STRICT -> R.string.text_budget_profile_strict
            ArchiveResourceBudgetProfile.CUSTOM -> R.string.text_budget_profile_custom
        },
    )

    private fun selectedResourceBudget(): ArchiveResourceBudget = when (selectedResourceBudgetProfile) {
        ArchiveResourceBudgetProfile.COMPATIBLE -> ArchiveResourceBudget.COMPATIBLE
        ArchiveResourceBudgetProfile.STRICT -> ArchiveResourceBudget.STRICT
        ArchiveResourceBudgetProfile.CUSTOM -> customResourceBudget
    }

    private fun selectResourceBudgetProfile(profile: ArchiveResourceBudgetProfile) {
        if (profile == ArchiveResourceBudgetProfile.CUSTOM) {
            showCustomResourceBudgetDialog()
            return
        }
        selectedResourceBudgetProfile = profile
        pendingAllowResourceBudgetOverride = false
        updateResourceBudgetInput()
    }

    private fun updateResourceBudgetInput() {
        binding.extractionBudgetInput.setText(
            resourceBudgetProfileLabel(selectedResourceBudgetProfile),
            false,
        )
        binding.extractionBudgetLayout.helperText = if (
            selectedResourceBudgetProfile == ArchiveResourceBudgetProfile.CUSTOM
        ) {
            getString(R.string.text_budget_profile_custom_details)
        } else {
            val budget = selectedResourceBudget()
            getString(
                R.string.text_budget_profile_details,
                formatWholeNumber(budget.maxEntries.toLong()),
                formatWholeNumber(budget.maxPathLength.toLong()),
                formatWholeNumber(budget.maxDepth.toLong()),
                formatWholeNumber(budget.maxSingleUncompressedBytes / GIB),
                formatWholeNumber(budget.maxTotalUncompressedBytes / GIB),
                formatWholeNumber(budget.maxCompressionRatio),
            )
        }
    }

    private fun showCustomResourceBudgetDialog() {
        val customBinding = DialogArchiveResourceBudgetBinding.inflate(layoutInflater)
        val hardLimits = ArchiveStructureLimits.DEFAULT
        customBinding.maxEntries.setText(
            customStructuralInput(customResourceBudget.maxEntries, hardLimits.maxPathNodes),
        )
        customBinding.maxPathLength.setText(
            customStructuralInput(customResourceBudget.maxPathLength, hardLimits.maxPathLength),
        )
        customBinding.maxDepth.setText(
            customStructuralInput(customResourceBudget.maxDepth, hardLimits.maxDepth),
        )
        customBinding.maxSingleSize.setText(customMibInput(customResourceBudget.maxSingleUncompressedBytes))
        customBinding.maxTotalSize.setText(customMibInput(customResourceBudget.maxTotalUncompressedBytes))
        customBinding.maxCompressionRatio.setText(customUnlimitedInput(customResourceBudget.maxCompressionRatio))

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_custom_budget)
            .setView(customBinding.root)
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setPositiveButton(R.string.dialog_button_save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val budget = readCustomResourceBudget(customBinding) ?: return@setOnClickListener
                customResourceBudget = budget
                selectedResourceBudgetProfile = ArchiveResourceBudgetProfile.CUSTOM
                pendingAllowResourceBudgetOverride = false
                updateResourceBudgetInput()
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { updateResourceBudgetInput() }
        dialog.show()
    }

    private fun readCustomResourceBudget(
        customBinding: DialogArchiveResourceBudgetBinding,
    ): ArchiveResourceBudget? {
        val fields = listOf(
            customBinding.maxEntriesLayout to customBinding.maxEntries.text,
            customBinding.maxPathLengthLayout to customBinding.maxPathLength.text,
            customBinding.maxDepthLayout to customBinding.maxDepth.text,
            customBinding.maxSingleSizeLayout to customBinding.maxSingleSize.text,
            customBinding.maxTotalSizeLayout to customBinding.maxTotalSize.text,
            customBinding.maxCompressionRatioLayout to customBinding.maxCompressionRatio.text,
        )
        fields.forEach { (layout, _) -> layout.error = null }
        val parsed = fields.map { (layout, value) ->
            value?.toString()?.trim()?.toLongOrNull()
                ?.takeIf { it >= 0L }
                .also { if (it == null) layout.error = getString(R.string.error_custom_budget_invalid) }
        }
        if (parsed.any { it == null }) return null
        val values = parsed.filterNotNull()
        val hardLimits = ArchiveStructureLimits.DEFAULT
        val structuralMaximums = listOf(
            hardLimits.maxPathNodes.toLong(),
            hardLimits.maxPathLength.toLong(),
            hardLimits.maxDepth.toLong(),
        )
        var structuralInvalid = false
        values.take(3).forEachIndexed { index, value ->
            if (value > structuralMaximums[index]) {
                fields[index].first.error = getString(R.string.error_custom_budget_invalid)
                structuralInvalid = true
            }
        }
        val singleMib = values[3]
        val totalMib = values[4]
        if (singleMib > MAX_MIB_VALUE) {
            customBinding.maxSingleSizeLayout.error = getString(R.string.error_custom_budget_invalid)
        }
        if (totalMib > MAX_MIB_VALUE) {
            customBinding.maxTotalSizeLayout.error = getString(R.string.error_custom_budget_invalid)
        }
        if (structuralInvalid || singleMib > MAX_MIB_VALUE || totalMib > MAX_MIB_VALUE) return null

        val maxSingleBytes = unlimitedOrMib(singleMib)
        val maxTotalBytes = unlimitedOrMib(totalMib)
        if (maxTotalBytes < maxSingleBytes) {
            customBinding.maxTotalSizeLayout.error = getString(
                R.string.error_custom_budget_total_less_single,
            )
            return null
        }
        return ArchiveResourceBudget(
            maxEntries = structuralBudgetValue(values[0], hardLimits.maxPathNodes),
            maxPathLength = structuralBudgetValue(values[1], hardLimits.maxPathLength),
            maxDepth = structuralBudgetValue(values[2], hardLimits.maxDepth),
            maxSingleUncompressedBytes = maxSingleBytes,
            maxTotalUncompressedBytes = maxTotalBytes,
            maxCompressionRatio = values[5].takeUnless { it == 0L } ?: Long.MAX_VALUE,
        )
    }

    private fun resourceBudgetConfirmationMessage(
        assessment: ArchiveResourceBudgetAssessment,
    ): String = buildString {
        append(
            getString(
                R.string.text_resource_budget_summary,
                formatBytes(assessment.estimatedOutputBytes),
                assessment.totalEntries,
            ),
        )
        append("\n\n")
        assessment.violations.forEach { violation ->
            append("- ")
            append(resourceBudgetViolationLabel(violation))
            append('\n')
        }
        append('\n')
        append(getString(R.string.text_resource_budget_warning))
    }

    private fun resourceBudgetViolationLabel(violation: ArchiveResourceBudgetViolation): String =
        when (violation.kind) {
            ArchiveResourceBudgetViolationKind.ENTRY_COUNT -> getString(
                R.string.text_budget_violation_entry_count,
                formatWholeNumber(violation.actual),
                formatWholeNumber(violation.limit),
            )
            ArchiveResourceBudgetViolationKind.PATH_LENGTH -> getString(
                R.string.text_budget_violation_path_length,
                formatWholeNumber(violation.actual),
                formatWholeNumber(violation.limit),
            )
            ArchiveResourceBudgetViolationKind.DEPTH -> getString(
                R.string.text_budget_violation_path_depth,
                formatWholeNumber(violation.actual),
                formatWholeNumber(violation.limit),
            )
            ArchiveResourceBudgetViolationKind.SINGLE_UNCOMPRESSED_SIZE -> getString(
                R.string.text_budget_violation_single_size,
                formatBytes(violation.actual),
                formatBytes(violation.limit),
            )
            ArchiveResourceBudgetViolationKind.TOTAL_UNCOMPRESSED_SIZE -> getString(
                R.string.text_budget_violation_total_size,
                formatBytes(violation.actual),
                formatBytes(violation.limit),
            )
            ArchiveResourceBudgetViolationKind.COMPRESSION_RATIO -> getString(
                R.string.text_budget_violation_compression_ratio,
                formatWholeNumber(violation.actual),
                formatWholeNumber(violation.limit),
            )
        }

    private fun customStructuralInput(value: Int, hardMaximum: Int): String =
        if (value == hardMaximum) "0" else value.toString()

    private fun customMibInput(value: Long): String =
        if (value == Long.MAX_VALUE) "0" else (value / MIB).toString()

    private fun customUnlimitedInput(value: Long): String =
        if (value == Long.MAX_VALUE) "0" else value.toString()

    private fun structuralBudgetValue(value: Long, hardMaximum: Int): Int =
        if (value == 0L) hardMaximum else value.toInt()

    private fun unlimitedOrMib(value: Long): Long =
        if (value == 0L) Long.MAX_VALUE else value * MIB

    private fun formatWholeNumber(value: Long): String =
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

    private fun chooseExtractionDestination(
        skipUnsafePathsConfirmed: Boolean = false,
        resourceBudgetConfirmed: Boolean = false,
    ) {
        val archive = snapshot
        val archiveIndex = index
        if (archive == null || archiveIndex == null || selectedPaths.isEmpty()) {
            Toast.makeText(this, R.string.error_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        pendingExtractionPaths = selectedPaths.toSet()
        val selection = try {
            ArchiveSelection.resolve(archive, pendingExtractionPaths, archiveIndex)
        } catch (error: Throwable) {
            pendingExtractionPaths = emptySet()
            showFailure(
                error = error,
                headline = headlineForExtractionFailure(error),
                formatHint = archive.format,
                stageHint = ArchiveFailureStage.ENTRY_DATA,
            )
            return
        }
        if (selection.totalEntries == 0 && selection.skippedUnsafeEntries.isNotEmpty()) {
            pendingExtractionPaths = emptySet()
            pendingSkipUnsafePaths = false
            pendingAllowResourceBudgetOverride = false
            showMessage(getString(R.string.error_no_safe_entries))
            return
        }
        val passwordRequired = selection.files.any {
            it.isEncrypted && !archive.readerOptions.hasPassword
        }
        if (passwordRequired) {
            binding.archivePasswordLayout.error = getString(R.string.error_password_required)
            showMessage(getString(R.string.error_password_required))
            focusArchivePassword()
            return
        }
        if (selection.skippedUnsafeEntries.isNotEmpty() && !skipUnsafePathsConfirmed) {
            pendingSkipUnsafePaths = false
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_unsafe_paths)
                .setMessage(
                    getString(
                        R.string.dialog_message_skip_unsafe_paths,
                        selection.skippedUnsafeEntries.size,
                    ),
                )
                .setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
                    pendingExtractionPaths = emptySet()
                    pendingSkipUnsafePaths = false
                    pendingAllowResourceBudgetOverride = false
                }
                .setPositiveButton(R.string.dialog_button_skip_unsafe) { _, _ ->
                    chooseExtractionDestination(
                        skipUnsafePathsConfirmed = true,
                        resourceBudgetConfirmed = resourceBudgetConfirmed,
                    )
                }
                .show()
            return
        }
        val budget = selectedResourceBudget()
        val budgetAssessment = ArchiveResourceBudgetEvaluator.assess(selection, budget)
        if (budgetAssessment.exceedsBudget && !resourceBudgetConfirmed) {
            pendingAllowResourceBudgetOverride = false
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_resource_budget)
                .setMessage(resourceBudgetConfirmationMessage(budgetAssessment))
                .setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
                    pendingExtractionPaths = emptySet()
                    pendingSkipUnsafePaths = false
                    pendingAllowResourceBudgetOverride = false
                }
                .setPositiveButton(R.string.dialog_button_continue_anyway) { _, _ ->
                    chooseExtractionDestination(
                        skipUnsafePathsConfirmed = skipUnsafePathsConfirmed,
                        resourceBudgetConfirmed = true,
                    )
                }
                .show()
            return
        }
        pendingSkipUnsafePaths = selection.skippedUnsafeEntries.isNotEmpty()
        pendingAllowResourceBudgetOverride = budgetAssessment.exceedsBudget
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
        pendingSkipUnsafePaths = false
        pendingAllowResourceBudgetOverride = false
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
        val skipUnsafePaths = pendingSkipUnsafePaths
        val allowResourceBudgetOverride = pendingAllowResourceBudgetOverride
        val resourceBudget = selectedResourceBudget()
        pendingExtractionPaths = emptySet()
        pendingSkipUnsafePaths = false
        pendingAllowResourceBudgetOverride = false
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
                    ArchiveExtractor(contentResolver, resourceBudget).extract(
                        source = staged.file,
                        snapshot = archive,
                        selectedPaths = paths,
                        treeUri = treeUri,
                        rootName = rootName,
                        skipUnsafePaths = skipUnsafePaths,
                        allowResourceBudgetOverride = allowResourceBudgetOverride,
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
        binding.extractionBudgetLayout.isEnabled = !busy
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
        val diagnostic = ArchiveFailureDiagnostic.from(
            error = error,
            formatHint = formatHint,
            stageHint = stageHint,
            archiveDisplayName = request?.displayName,
        )
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
        pendingSkipUnsafePaths = savedInstanceState.getBoolean(
            STATE_PENDING_SKIP_UNSAFE_PATHS,
            false,
        )
        pendingAllowResourceBudgetOverride = savedInstanceState.getBoolean(
            STATE_PENDING_RESOURCE_BUDGET_OVERRIDE,
            false,
        )
        directActionHandled = savedInstanceState.getBoolean(STATE_DIRECT_ACTION_HANDLED, false)
        selectedResourceBudgetProfile = savedInstanceState
            .getString(STATE_RESOURCE_BUDGET_PROFILE)
            ?.let { stored ->
                ArchiveResourceBudgetProfile.entries.firstOrNull { it.name == stored }
            }
            ?: ArchiveResourceBudgetProfile.COMPATIBLE
        customResourceBudget = restoredCustomResourceBudget(savedInstanceState)
        selectedFilenameCharsetName = savedInstanceState.getString(STATE_FILENAME_CHARSET)
            ?.take(MAX_FILENAME_CHARSET_LENGTH)
    }

    private fun restoredCustomResourceBudget(savedInstanceState: Bundle): ArchiveResourceBudget {
        val requiredKeys = listOf(
            STATE_CUSTOM_BUDGET_ENTRIES,
            STATE_CUSTOM_BUDGET_PATH_LENGTH,
            STATE_CUSTOM_BUDGET_DEPTH,
            STATE_CUSTOM_BUDGET_SINGLE_SIZE,
            STATE_CUSTOM_BUDGET_TOTAL_SIZE,
            STATE_CUSTOM_BUDGET_RATIO,
        )
        if (requiredKeys.any { !savedInstanceState.containsKey(it) }) {
            return ArchiveResourceBudget.COMPATIBLE
        }
        val restored = runCatching {
            ArchiveResourceBudget(
                maxEntries = savedInstanceState.getInt(STATE_CUSTOM_BUDGET_ENTRIES),
                maxPathLength = savedInstanceState.getInt(STATE_CUSTOM_BUDGET_PATH_LENGTH),
                maxDepth = savedInstanceState.getInt(STATE_CUSTOM_BUDGET_DEPTH),
                maxSingleUncompressedBytes = savedInstanceState.getLong(STATE_CUSTOM_BUDGET_SINGLE_SIZE),
                maxTotalUncompressedBytes = savedInstanceState.getLong(STATE_CUSTOM_BUDGET_TOTAL_SIZE),
                maxCompressionRatio = savedInstanceState.getLong(STATE_CUSTOM_BUDGET_RATIO),
            )
        }.getOrNull() ?: return ArchiveResourceBudget.COMPATIBLE
        val hardLimits = ArchiveStructureLimits.DEFAULT
        return restored.takeIf {
            it.maxEntries <= hardLimits.maxPathNodes &&
                it.maxPathLength <= hardLimits.maxPathLength &&
                it.maxDepth <= hardLimits.maxDepth
        } ?: ArchiveResourceBudget.COMPATIBLE
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
            ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
            -> getString(R.string.error_archive_limit)
            ArchiveFailureCode.PASSWORD_REQUIRED -> getString(R.string.error_password_required)
            ArchiveFailureCode.WRONG_PASSWORD -> getString(R.string.error_password_incorrect)
            ArchiveFailureCode.MISSING_VOLUME -> getString(R.string.error_archive_missing_volumes)
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

    private fun failureReason(diagnostic: ArchiveFailureDiagnostic): String {
        if (diagnostic.code == ArchiveFailureCode.MISSING_VOLUME) {
            val displayName = request?.displayName ?: getString(R.string.text_unknown)
            val splitArchive = diagnostic.splitArchiveInfo
            return getString(
                R.string.error_reason_missing_volumes,
                splitArchive?.requiredVolumeSummary(displayName) ?: "*.z01, *.z02, ...",
                splitArchive?.finalVolumeName(displayName) ?: displayName,
            )
        }
        return getString(
            when (diagnostic.code) {
                ArchiveFailureCode.CACHE_SPACE_UNAVAILABLE -> R.string.error_reason_cache_space
                ArchiveFailureCode.INVALID_SIGNATURE -> R.string.error_reason_invalid_signature
                ArchiveFailureCode.UNSUPPORTED_FILENAME_CHARSET ->
                    R.string.error_reason_filename_encoding
                ArchiveFailureCode.PASSWORD_REQUIRED,
                ArchiveFailureCode.WRONG_PASSWORD,
                -> R.string.error_reason_password
                ArchiveFailureCode.INVALID_PATH,
                ArchiveFailureCode.UNSAFE_PATH_CONFIRMATION_REQUIRED,
                ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
                ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
                ArchiveFailureCode.DUPLICATE_PATH,
                ArchiveFailureCode.FILE_DIRECTORY_CONFLICT,
                -> R.string.error_reason_path
                ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                ArchiveFailureCode.RESOURCE_BUDGET_CONFIRMATION_REQUIRED,
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
    }

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
        const val MIB = 1_024L * 1_024L
        const val GIB = 1_024L * MIB
        const val MAX_MIB_VALUE = Long.MAX_VALUE / MIB
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
        const val STATE_PENDING_RESOURCE_BUDGET_OVERRIDE = "pending_resource_budget_override"
        const val STATE_PENDING_SKIP_UNSAFE_PATHS = "pending_skip_unsafe_paths"
        const val STATE_RESOURCE_BUDGET_PROFILE = "resource_budget_profile"
        const val STATE_CUSTOM_BUDGET_ENTRIES = "custom_budget_entries"
        const val STATE_CUSTOM_BUDGET_PATH_LENGTH = "custom_budget_path_length"
        const val STATE_CUSTOM_BUDGET_DEPTH = "custom_budget_depth"
        const val STATE_CUSTOM_BUDGET_SINGLE_SIZE = "custom_budget_single_size"
        const val STATE_CUSTOM_BUDGET_TOTAL_SIZE = "custom_budget_total_size"
        const val STATE_CUSTOM_BUDGET_RATIO = "custom_budget_ratio"
        const val STATE_SELECTED_PATHS = "selected_paths"
    }
}
