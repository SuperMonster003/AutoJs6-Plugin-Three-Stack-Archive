package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.InputType
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
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
import com.google.android.material.snackbar.Snackbar
import io.github.supermonster003.autojs6.plugin.three.stack.archive.databinding.ActivityArchiveManagerBinding
import io.github.supermonster003.autojs6.plugin.three.stack.archive.databinding.DialogArchiveOutputConflictBinding
import io.github.supermonster003.autojs6.plugin.three.stack.archive.databinding.DialogArchiveResourceBudgetBinding
import io.github.supermonster003.autojs6.plugin.three.stack.archive.databinding.DialogExtractionDestinationBinding
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

open class ArchiveManagerActivity : ConfiguredActivity() {

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
    private var pendingConflictPolicy: ArchiveExtractionConflictPolicy? = null
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
    private var selectedConflictPolicy = ArchiveExtractionConflictPolicy.ASK
    private var lastFailureDiagnostic: ArchiveFailureDiagnostic? = null
    private var usesCompactHeader = false
    private var usesUltraCompactLayout = false
    private var isCompactSearchExpanded = false
    private var renderedSelectionCount = 0
    private var pendingAddDirectory: String? = null
    private var pendingImportParentPath: String? = null
    private var hostSessionClosed = false
    private var targetReplacementHistory: HostTargetReplacementHistory? = null

    private val outputTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri == null) {
            pendingExtractionPaths = emptySet()
            pendingSkipUnsafePaths = false
            pendingAllowResourceBudgetOverride = false
            pendingConflictPolicy = null
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

    private val addFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val parentPath = pendingAddDirectory
        pendingAddDirectory = null
        if (uris.isNotEmpty() && parentPath != null) {
            startAddingFiles(parentPath, uris.distinct())
        }
    }

    private val addFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        val parentPath = pendingImportParentPath
        pendingImportParentPath = null
        if (treeUri != null && parentPath != null) {
            startAddingDirectoryTree(parentPath, treeUri)
        }
    }

    internal open fun resolveRequest(): ArchiveOpenRequest? = ArchiveIntentPolicy.resolve(intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        restoreInstanceState(savedInstanceState)

        val resolvedRequest = resolveRequest()
        if (resolvedRequest == null) {
            Toast.makeText(this, R.string.error_permission_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        request = resolvedRequest
        usesCompactHeader = shouldUseCompactHeader(resources.configuration)
        usesUltraCompactLayout = shouldUseUltraCompactLayout(resources.configuration)
        setupViews()
        val recoveredOutputs = runCatching {
            resolvedRequest.hostSession?.abortIncompleteOutputs() ?: 0
        }.getOrDefault(0)
        if (recoveredOutputs > 0) {
            showMessage(getString(R.string.text_interrupted_outputs_recovered, recoveredOutputs))
        }
        updateHeaderPresentation()
        loadArchive(resolvedRequest)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        usesCompactHeader = shouldUseCompactHeader(newConfig)
        usesUltraCompactLayout = shouldUseUltraCompactLayout(newConfig)
        isCompactSearchExpanded = false
        updateHeaderPresentation()
        updateResourceBudgetInput()
        updateConflictPolicyInput()
        binding.root.post { binding.root.requestApplyInsets() }
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
        pendingConflictPolicy?.let {
            outState.putString(STATE_PENDING_CONFLICT_POLICY, it.name)
        }
        outState.putBoolean(STATE_DIRECT_ACTION_HANDLED, directActionHandled)
        outState.putString(STATE_RESOURCE_BUDGET_PROFILE, selectedResourceBudgetProfile.name)
        outState.putString(STATE_CONFLICT_POLICY, selectedConflictPolicy.name)
        outState.putInt(STATE_CUSTOM_BUDGET_ENTRIES, customResourceBudget.maxEntries)
        outState.putInt(STATE_CUSTOM_BUDGET_PATH_LENGTH, customResourceBudget.maxPathLength)
        outState.putInt(STATE_CUSTOM_BUDGET_DEPTH, customResourceBudget.maxDepth)
        outState.putLong(STATE_CUSTOM_BUDGET_SINGLE_SIZE, customResourceBudget.maxSingleUncompressedBytes)
        outState.putLong(STATE_CUSTOM_BUDGET_TOTAL_SIZE, customResourceBudget.maxTotalUncompressedBytes)
        outState.putLong(STATE_CUSTOM_BUDGET_RATIO, customResourceBudget.maxCompressionRatio)
        pendingAddDirectory?.let { outState.putString(STATE_PENDING_ADD_DIRECTORY, it) }
        pendingImportParentPath?.let {
            outState.putString(STATE_PENDING_IMPORT_PARENT_PATH, it)
        }
        selectedFilenameCharsetName?.let {
            outState.putString(STATE_FILENAME_CHARSET, it)
        }
    }

    override fun onDestroy() {
        clearSelectedPassword()
        if (::binding.isInitialized) binding.archivePassword.text?.clear()
        val closeSessionAfterCleanup = !isChangingConfigurations
        val activeOperation = operationJob
        if (activeOperation?.isActive == true) {
            activeOperation.invokeOnCompletion {
                clearStagedArchive()
                if (closeSessionAfterCleanup) closeHostSession()
            }
        } else {
            clearStagedArchive()
            if (closeSessionAfterCleanup) closeHostSession()
        }
        super.onDestroy()
    }

    private fun setupViews() = with(binding) {
        toolbar.setNavigationOnClickListener { handleBack() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionArchiveInformation -> {
                    showArchiveInformationDialog()
                    true
                }
                R.id.actionRestorePreviousArchiveVersion -> {
                    showRestorePreviousArchiveVersionDialog()
                    true
                }
                else -> false
            }
        }
        toolbar.menu.findItem(R.id.actionArchiveInformation).isEnabled = false
        toolbar.menu.findItem(R.id.actionRestorePreviousArchiveVersion).isVisible = false
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
        searchInput.doAfterTextChanged {
            updateSearchPresentation()
            renderEntries()
        }
        upButton.setOnClickListener {
            currentDirectory = currentDirectory.substringBeforeLast('/', ArchivePathPolicy.ROOT_PATH)
            renderEntries()
        }
        selectAllButton.setOnClickListener { selectAllVisibleEntries() }
        addFilesButton.setOnClickListener {
            if (!canManageArchive()) return@setOnClickListener
            pendingAddDirectory = currentDirectory
            addFilesLauncher.launch(arrayOf("*/*"))
        }
        addFolderButton.setOnClickListener {
            if (!canManageArchive()) return@setOnClickListener
            pendingImportParentPath = currentDirectory
            addFolderLauncher.launch(null)
        }
        newFolderButton.setOnClickListener { showNewFolderDialog() }
        renameButton.setOnClickListener { showRenameDialog() }
        deleteButton.setOnClickListener { startDeleteSelection() }
        manageActionsButton.setOnClickListener {
            androidx.appcompat.widget.PopupMenu(this@ArchiveManagerActivity, manageActionsButton).apply {
                val actions = listOf(addFilesButton, addFolderButton, newFolderButton, renameButton, deleteButton)
                actions.filter { it.isVisible }.forEach { button ->
                    menu.add(0, button.id, 0, button.text).isEnabled = button.isEnabled
                }
                setOnMenuItemClickListener { item ->
                    actions.firstOrNull { it.id == item.itemId }?.performClick() ?: false
                }
                show()
            }
        }
        extractButton.setOnClickListener { showExtractionScopeDialog() }
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
        extractionConflictPolicyInput.setAdapter(
            ArrayAdapter(
                this@ArchiveManagerActivity,
                android.R.layout.simple_list_item_1,
                conflictPolicies().map(::conflictPolicyLabel),
            ),
        )
        extractionConflictPolicyInput.setOnItemClickListener { _, _, position, _ ->
            conflictPolicies().getOrNull(position)?.let(::selectConflictPolicy)
        }
        extractionConflictPolicyInput.setOnClickListener {
            extractionConflictPolicyInput.showDropDown()
        }
        compactBudgetButton.setOnClickListener { showCompactResourceBudgetDialog() }
        compactConflictButton.setOnClickListener { showCompactConflictPolicyDialog() }
        compactEncodingButton.setOnClickListener { showCompactFilenameEncodingDialog() }
        compactSearchButton.setOnClickListener { toggleCompactSearch() }
        updateConflictPolicyInput()
        applyPasswordButton.setOnClickListener { applyPassword() }
        archivePassword.setOnEditorActionListener { _, _, _ ->
            applyPassword()
            true
        }
        diagnosticCopyButton.setOnClickListener { copyLastDiagnostic() }
        filenameEncodingLayout.isVisible = false
        passwordControls.isVisible = false
        diagnosticCopyButton.isVisible = false
        updateSelectionPresentation(0)

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
        if (usesUltraCompactLayout && isCompactSearchExpanded) {
            isCompactSearchExpanded = false
            updateSearchPresentation()
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
        updateHeaderPresentation()
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
                            if (snapshot != null || !isBusy) return@postUiUpdate
                            updateArchiveSummary(
                                getString(
                                    R.string.text_loading_progress,
                                    formatBytes(copied),
                                    formatBytes(request.reportedSize),
                                ),
                            )
                        }
                    }.also { stagedArchive = it }
                }
                configureFilenameEncoding(snapshot = null)
                val (scanned, scannedIndex) = scanStagedArchive(staged)
                applyScannedArchive(scanned, scannedIndex)
                refreshTargetReplacementHistory()
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
            fun scan(source: ArchiveReadSource): ArchiveSnapshot = ArchiveScanner().scan(
                source = source,
                options = readerOptions,
            ) { ensureActive() }
            val scanned = try {
                scan(staged.source)
            } catch (_: ArchiveLocalFileRequiredException) {
                val cached = ArchiveCacheStager.materialize(
                    staged = staged,
                    cacheDirectory = cacheDir,
                    onProgress = {},
                )
                replaceStagedArchive(staged, cached)
                scan(cached.source)
            }
            scanned.let {
                scanned to ArchiveIndex(
                    scanned,
                    getString(R.string.text_unsafe_paths_folder),
                )
            }
        } finally {
            readerOptions.clearPassword()
        }
    }

    private fun replaceStagedArchive(expected: StagedArchive, replacement: StagedArchive) {
        val replaced = synchronized(this) {
            if (stagedArchive !== expected) {
                false
            } else {
                stagedArchive = replacement
                true
            }
        }
        if (!replaced) {
            replacement.close()
            throw CancellationException("Archive input changed during materialization")
        }
        expected.close()
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
        updateArchiveSummary(
            resources.getQuantityString(
                R.plurals.text_archive_summary,
                scanned.entries.size,
                scanned.entries.size,
                formatBytes(scanned.totalUncompressedBytes),
            ),
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
            updateFilenameEncodingPresentation()
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
        updateFilenameEncodingPresentation()
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
                0,
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
            binding.upButton.isVisible = !usesCompactHeader && directory.isNotEmpty()
            binding.message.isVisible = result.rows.isEmpty() || lastFailureDiagnostic != null
            if (result.rows.isEmpty() && lastFailureDiagnostic == null) {
                binding.message.text = getString(R.string.text_no_entries)
            }
            binding.entryList.isVisible = result.rows.isNotEmpty()
            adapter.submitList(result.rows)
            updateSelectionPresentation(result.selectionCount)
            binding.extractButton.isEnabled = true
            updateManagementPresentation()
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

    private fun canManageArchive(): Boolean {
        return managementStatus()?.isWritable == true
    }

    private fun managementStatus(): ArchiveManagementStatus? {
        val archive = snapshot ?: return null
        val openRequest = request ?: return null
        if (!ThreeStackArchivePlugin.canModifyFileName(openRequest.displayName)) {
            return ArchiveManagementStatus(null, ArchiveManagementReadOnlyReason.FORMAT_NOT_SUPPORTED)
        }
        return ArchiveEngine.DEFAULT.managementStatus(
            snapshot = archive,
            requestedAction = openRequest.requestedAction,
            hasHostReplacementSession = openRequest.hostSession != null,
        )
    }

    private suspend fun refreshTargetReplacementHistory(): HostTargetReplacementHistory? {
        val openRequest = request
        val query = if (
            openRequest?.requestedAction == ArchiveRequestedAction.MANAGE &&
            openRequest.hostSession != null
        ) {
            withContext(Dispatchers.IO) {
                try {
                    true to openRequest.hostSession.queryTargetReplacement(openRequest.targetId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false to null
                }
            }
        } else {
            true to null
        }
        if (query.first) targetReplacementHistory = query.second
        updateTargetReplacementAction()
        return targetReplacementHistory
    }

    private fun updateTargetReplacementAction() {
        if (!::binding.isInitialized) return
        val available = targetReplacementHistory?.isAvailable == true
        binding.toolbar.menu.findItem(R.id.actionRestorePreviousArchiveVersion)?.apply {
            isVisible = available
            isEnabled = available && !isBusy && snapshot != null
        }
    }

    internal fun showArchiveInformationDialog(): androidx.appcompat.app.AlertDialog? {
        if (isBusy) return null
        val archive = snapshot ?: return null
        val status = managementStatus() ?: return null
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_archive_information)
            .setMessage(archiveInformationMessage(archive, status))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    internal fun showRestorePreviousArchiveVersionDialog(): androidx.appcompat.app.AlertDialog? {
        if (isBusy || snapshot == null) return null
        val history = targetReplacementHistory?.takeIf(HostTargetReplacementHistory::isAvailable)
            ?: return null
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_restore_previous_archive_version)
            .setMessage(
                getString(
                    R.string.dialog_message_restore_previous_archive_version,
                    formatBytes(history.previousSize),
                    formatDate(history.createdAt),
                ),
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_restore_previous_archive_version) { _, _ ->
                startRestorePreviousArchiveVersion(history)
            }
            .show()
    }

    private fun archiveInformationMessage(
        archive: ArchiveSnapshot,
        status: ArchiveManagementStatus,
    ): String {
        val fileCount = archive.entries.count { !it.isDirectory }
        val directoryCount = archive.entries.size - fileCount
        val managementText = status.readOnlyReason?.let { reason ->
            getString(
                R.string.text_archive_management_read_only,
                archiveManagementReadOnlyReason(reason),
            )
        } ?: getString(R.string.text_archive_management_available)
        val paragraphs = mutableListOf(
            listOf(
                getString(R.string.text_archive_information_format, archive.format.displayName),
                getString(
                    R.string.text_archive_information_archive_size,
                    formatBytes(archive.sourceLength),
                ),
                getString(
                    R.string.text_archive_information_contents,
                    archive.entries.size,
                    fileCount,
                    directoryCount,
                    formatBytes(archive.totalUncompressedBytes),
                ),
                getString(R.string.text_archive_management_status, managementText),
            ).joinToString(separator = "\n"),
        )
        status.capabilities?.let { capabilities ->
            val operations = ArchiveOperation.entries
                .filter(capabilities::supports)
                .map(::archiveMutationOperationLabel)
                .joinToString(separator = ", ")
            paragraphs += getString(R.string.text_archive_management_operations, operations)
            if (capabilities.strategy == ArchiveMutationStrategy.FULL_REWRITE) {
                paragraphs += getString(R.string.text_archive_mutation_full_rewrite)
            }
            metadataEffectParagraph(capabilities.metadataEffects)?.let(paragraphs::add)
            paragraphs += getString(R.string.text_archive_information_safety)
        }
        targetReplacementHistory?.let { history ->
            paragraphs += targetReplacementHistoryMessage(history)
        }
        return paragraphs.joinToString(separator = "\n\n")
    }

    private fun targetReplacementHistoryMessage(history: HostTargetReplacementHistory): String =
        when (history.state) {
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_AVAILABLE -> getString(
                R.string.text_previous_archive_version_available,
                formatBytes(history.previousSize),
                formatDate(history.createdAt),
            )
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED ->
                getString(R.string.text_previous_archive_version_restored)
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_STALE ->
                getString(R.string.text_previous_archive_version_stale)
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RECOVERY_REQUIRED,
            ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_UNKNOWN,
            -> getString(R.string.text_previous_archive_version_recovery_required)
            else -> error("Unexpected target replacement history state")
        }

    private fun archiveManagementReadOnlyReason(reason: ArchiveManagementReadOnlyReason): String =
        getString(
            when (reason) {
                ArchiveManagementReadOnlyReason.CURRENT_SESSION_READ_ONLY ->
                    R.string.text_archive_read_only_current_session
                ArchiveManagementReadOnlyReason.HOST_REPLACEMENT_UNAVAILABLE ->
                    R.string.text_archive_read_only_host_replacement
                ArchiveManagementReadOnlyReason.FORMAT_NOT_SUPPORTED ->
                    R.string.text_archive_read_only_format
                ArchiveManagementReadOnlyReason.MULTI_VOLUME_ARCHIVE ->
                    R.string.text_archive_read_only_multi_volume
                ArchiveManagementReadOnlyReason.UNSAFE_ENTRY_PATH ->
                    R.string.text_archive_read_only_unsafe_path
                ArchiveManagementReadOnlyReason.PASSWORD_REQUIRED ->
                    R.string.text_archive_read_only_password
                ArchiveManagementReadOnlyReason.UNSUPPORTED_ENTRY_METHOD ->
                    R.string.text_archive_read_only_unsupported_method
                ArchiveManagementReadOnlyReason.BACKEND_VARIANT_READ_ONLY ->
                    R.string.text_archive_read_only_backend_variant
            },
        )

    private fun archiveMutationOperationLabel(operation: ArchiveOperation): String = getString(
        when (operation) {
            ArchiveOperation.ADD -> R.string.text_archive_mutation_operation_add
            ArchiveOperation.DELETE -> R.string.text_archive_mutation_operation_delete
            ArchiveOperation.RENAME -> R.string.text_archive_mutation_operation_rename
            ArchiveOperation.DETECT,
            ArchiveOperation.LIST,
            ArchiveOperation.PREVIEW,
            ArchiveOperation.OPEN,
            ArchiveOperation.EXTRACT,
            ArchiveOperation.CREATE,
            -> error("Non-mutation operation cannot be presented as an archive change")
        },
    )

    private fun metadataEffectParagraph(
        effects: Set<ArchiveMutationMetadataEffect>,
    ): String? {
        if (effects.isEmpty()) return null
        val lines = ArchiveMutationMetadataEffect.entries
            .filter(effects::contains)
            .map { effect -> "- ${getString(metadataEffectString(effect))}" }
        return buildString {
            append(getString(R.string.text_archive_metadata_effects))
            lines.forEach { line ->
                append('\n')
                append(line)
            }
        }
    }

    private fun metadataEffectString(effect: ArchiveMutationMetadataEffect): Int = when (effect) {
        ArchiveMutationMetadataEffect.ARCHIVE_COMMENT_DROPPED ->
            R.string.text_archive_metadata_archive_comment_dropped
        ArchiveMutationMetadataEffect.ENTRY_COMMENTS_DROPPED ->
            R.string.text_archive_metadata_entry_comments_dropped
        ArchiveMutationMetadataEffect.EXTRA_FIELDS_NORMALIZED ->
            R.string.text_archive_metadata_extra_fields_normalized
        ArchiveMutationMetadataEffect.UNIX_ATTRIBUTES_DROPPED ->
            R.string.text_archive_metadata_unix_attributes_dropped
        ArchiveMutationMetadataEffect.COMPRESSION_SETTINGS_NORMALIZED ->
            R.string.text_archive_metadata_compression_normalized
        ArchiveMutationMetadataEffect.ENCRYPTION_SETTINGS_NORMALIZED ->
            R.string.text_archive_metadata_encryption_normalized
    }

    private fun showNewFolderDialog() {
        if (!canManageArchive() || isBusy) return
        showArchiveEntryNameDialog(
            title = getString(R.string.dialog_title_new_archive_folder),
            initialValue = "",
        ) { displayName ->
            startArchiveMutation(
                ArchiveMutationRequest.AddDirectory(
                    parentPath = currentDirectory,
                    displayName = displayName,
                ),
            )
        }
    }

    private fun showRenameDialog() {
        if (!canManageArchive() || isBusy) return
        val path = selectedPaths.singleOrNull()
        val node = path?.let { index?.node(it) }
        if (path == null || node == null || path.isEmpty()) {
            showMessage(getString(R.string.error_rename_single_selection))
            return
        }
        showArchiveEntryNameDialog(
            title = getString(R.string.dialog_title_rename_archive_entry),
            initialValue = node.name,
        ) { displayName ->
            startArchiveMutation(
                ArchiveMutationRequest.Rename(
                    path = path,
                    newDisplayName = displayName,
                ),
            )
        }
    }

    private fun startDeleteSelection() {
        if (!canManageArchive() || isBusy) return
        val paths = selectedPaths.toSet()
        if (paths.isEmpty()) {
            showMessage(getString(R.string.error_no_selection))
            return
        }
        startArchiveMutation(ArchiveMutationRequest.Delete(paths))
    }

    private fun showArchiveEntryNameDialog(
        title: String,
        initialValue: String,
        onAccepted: (String) -> Unit,
    ) {
        val input = EditText(this).apply {
            hint = getString(R.string.text_archive_entry_name)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
            setText(initialValue)
            setSelection(text.length)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(input)
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setPositiveButton(R.string.dialog_button_continue, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val displayName = input.text?.toString().orEmpty()
                if (ArchiveIntentPolicy.validateDisplayName(displayName) == null) {
                    input.error = getString(R.string.error_zip_entry_name_invalid)
                    return@setOnClickListener
                }
                dialog.dismiss()
                onAccepted(displayName)
            }
        }
        dialog.show()
    }

    private fun startAddingFiles(parentPath: String, uris: List<Uri>) {
        startArchiveMutation {
            val files = withContext(Dispatchers.IO) {
                uris.map(::resolveAddedFile)
            }
            ArchiveMutationRequest.AddFiles(parentPath, files)
        }
    }

    private fun startAddingDirectoryTree(parentPath: String, treeUri: Uri) {
        val archive = snapshot ?: return
        startArchiveMutation {
            binding.message.text = getString(R.string.text_scanning_folder)
            binding.message.isVisible = true
            val remainingEntries = archive.structureLimits.maxEntries - archive.entries.size
            if (remainingEntries <= 0) {
                throw ArchiveValidationException(
                    code = ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
                    message = "The archive has no remaining entry capacity",
                    format = archive.format,
                    stage = ArchiveFailureStage.INPUT,
                )
            }
            val entries = withContext(Dispatchers.IO) {
                SafDirectoryTreeImporter(
                    contentResolver = contentResolver,
                    format = archive.format,
                    limits = archive.structureLimits,
                    maxEntries = minOf(
                        remainingEntries,
                        SafDirectoryTreeImporter.MAX_IMPORT_ENTRIES,
                    ),
                ).scan(treeUri) { ensureActive() }
            }
            ArchiveMutationRequest.AddTree(parentPath, entries)
        }
    }

    private fun resolveAddedFile(uri: Uri): ArchiveMutationAddedFile {
        if (!uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)) {
            throw IOException("Selected file does not use a content URI")
        }
        var displayName: String? = null
        var size = ArchiveIntentPolicy.SIZE_UNKNOWN
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) {
                    displayName = cursor.getString(nameColumn)
                }
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    size = cursor.getLong(sizeColumn).takeIf { it >= 0L }
                        ?: ArchiveIntentPolicy.SIZE_UNKNOWN
                }
            }
        }
        val validatedName = ArchiveIntentPolicy.validateDisplayName(displayName)
            ?: throw ArchiveValidationException(
                code = ArchiveFailureCode.INVALID_DESTINATION_NAME,
                message = "Selected file has an invalid display name",
                format = snapshot?.format,
            )
        val lastModified = runCatching {
            contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null,
                null,
                null,
            )?.use { cursor ->
                val column = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (cursor.moveToFirst() && column >= 0 && !cursor.isNull(column)) {
                    cursor.getLong(column).takeIf { it >= 0L }
                } else {
                    null
                }
            }
        }.getOrNull() ?: -1L
        return ArchiveMutationAddedFile(
            displayName = validatedName,
            size = size,
            lastModified = lastModified,
        ) {
            contentResolver.openInputStream(uri)
                ?: throw IOException("Selected file cannot be opened")
        }
    }

    private suspend fun confirmArchiveMutation(prepared: PreparedArchiveMutation): Boolean =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val dialog = createArchiveMutationPreflightDialog(
                    prepared = prepared,
                    onConfirmed = {
                        if (continuation.isActive) continuation.resume(true)
                    },
                    onCancelled = {
                        if (continuation.isActive) continuation.resume(false)
                    },
                )
                continuation.invokeOnCancellation {
                    runOnUiThread {
                        if (dialog.isShowing) dialog.dismiss()
                    }
                }
                dialog.show()
            }
        }

    internal fun createArchiveMutationPreflightDialog(
        prepared: PreparedArchiveMutation,
        onConfirmed: () -> Unit,
        onCancelled: () -> Unit,
    ): androidx.appcompat.app.AlertDialog {
        var completed = false
        fun complete(confirmed: Boolean) {
            if (completed) return
            completed = true
            if (confirmed) onConfirmed() else onCancelled()
        }
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_archive_mutation_preflight)
            .setMessage(archiveMutationPreflightMessage(prepared))
            .setNegativeButton(R.string.dialog_button_cancel) { _, _ -> complete(false) }
            .setPositiveButton(R.string.dialog_button_continue) { _, _ -> complete(true) }
            .create()
            .also { dialog ->
                dialog.setOnCancelListener { complete(false) }
                dialog.setOnDismissListener { complete(false) }
            }
    }

    internal fun archiveMutationPreflightMessage(prepared: PreparedArchiveMutation): String {
        val work = prepared.workEstimate
        val contentWork = if (work.unknownContentFileCount == 0) {
            getString(
                R.string.text_archive_mutation_preflight_known_work,
                formatBytes(work.knownContentBytesToRead),
            )
        } else {
            getString(
                R.string.text_archive_mutation_preflight_unknown_work,
                formatBytes(work.knownContentBytesToRead),
                work.unknownContentFileCount,
            )
        }
        val paragraphs = mutableListOf(
            listOf(
                getString(
                    R.string.text_archive_mutation_preflight_change,
                    archiveMutationOperationLabel(prepared.operation),
                ),
                getString(
                    R.string.text_archive_information_archive_size,
                    formatBytes(work.sourceArchiveBytes),
                ),
                getString(
                    R.string.text_archive_mutation_preflight_result,
                    work.resultEntryCount,
                    work.resultFileCount,
                    work.resultDirectoryCount,
                ),
                contentWork,
            ).joinToString(separator = "\n"),
            getString(R.string.text_archive_mutation_output_size_unknown),
        )
        if (prepared.metadataEffects.isNotEmpty()) {
            metadataEffectParagraph(prepared.metadataEffects)?.let(paragraphs::add)
        }
        paragraphs += getString(R.string.text_archive_mutation_full_rewrite)
        paragraphs += getString(R.string.text_archive_information_safety)
        return paragraphs.joinToString(separator = "\n\n")
    }

    private fun startArchiveMutation(request: ArchiveMutationRequest) {
        startArchiveMutation { request }
    }

    private fun startArchiveMutation(
        requestProvider: suspend () -> ArchiveMutationRequest,
    ) {
        if (!canManageArchive() || isBusy) return
        val staged = stagedArchive ?: return
        val archive = snapshot ?: return
        val openRequest = request ?: return
        val hostSession = openRequest.hostSession ?: return
        setBusy(true, getString(R.string.text_preparing_archive_changes), cancellable = true)
        operationJob = lifecycleScope.launch {
            var committed: HostOutputTransaction? = null
            try {
                val mutationRequest = requestProvider()
                val provider = ArchiveEngine.DEFAULT.createMutationProvider(
                    archive.format,
                    hostSession,
                    cacheDir,
                )
                val prepared = withContext(Dispatchers.IO) {
                    provider.prepare(archive, mutationRequest)
                }
                if (!confirmArchiveMutation(prepared)) {
                    setBusy(false)
                    renderEntries()
                    return@launch
                }
                setBusy(
                    true,
                    getString(R.string.text_preparing_archive_changes),
                    cancellable = true,
                )
                committed = withContext(Dispatchers.IO) {
                    provider.execute(
                        source = staged.source,
                        snapshot = archive,
                        targetId = openRequest.targetId,
                        displayName = openRequest.displayName,
                        prepared = prepared,
                        checkCancelled = { ensureActive() },
                        progress = ArchiveMutationProgressListener(::renderMutationProgress),
                    )
                }
                targetReplacementHistory = committed.replacementHistory
                reloadArchiveAfterReplacement(
                    previousRequest = openRequest,
                    reportedSize = committed.size ?: ArchiveIntentPolicy.SIZE_UNKNOWN,
                )
                val history = refreshTargetReplacementHistory()
                showArchiveChangesComplete(history)
            } catch (cancelled: CancellationException) {
                if (committed == null) {
                    showMessage(getString(R.string.text_cancelled))
                } else if (!isFinishing && !isDestroyed) {
                    showMessage(getString(R.string.error_archive_refresh_failed))
                }
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                showFailure(
                    error = error,
                    headline = getString(
                        if (committed == null) {
                            R.string.error_archive_mutation_failed
                        } else {
                            R.string.error_archive_refresh_failed
                        },
                    ),
                    formatHint = archive.format,
                    stageHint = if (committed == null) {
                        ArchiveFailureStage.OUTPUT
                    } else {
                        ArchiveFailureStage.INPUT
                    },
                )
                setBusy(false)
                if (snapshot != null) renderEntries()
            }
        }
    }

    private suspend fun reloadArchiveAfterReplacement(
        previousRequest: ArchiveOpenRequest,
        reportedSize: Long,
    ) {
        val previousStaged = synchronized(this) {
            val value = stagedArchive
            stagedArchive = null
            value
        }
        snapshot?.readerOptions?.clearPassword()
        snapshot = null
        index = null
        previousStaged?.close()
        selectedPaths.clear()
        val updatedRequest = previousRequest.copy(
            reportedSize = reportedSize,
        )
        request = updatedRequest
        val replacement = withContext(Dispatchers.IO) {
            ArchiveCacheStager.stage(
                contentResolver = contentResolver,
                source = updatedRequest.archiveUri,
                cacheDirectory = cacheDir,
                reportedSize = updatedRequest.reportedSize,
            ) { copied ->
                postUiUpdate {
                    updateArchiveSummary(
                        getString(
                            R.string.text_loading_progress,
                            formatBytes(copied),
                            formatBytes(updatedRequest.reportedSize),
                        ),
                    )
                }
            }.also { stagedArchive = it }
        }
        val (scanned, scannedIndex) = scanStagedArchive(replacement)
        applyScannedArchive(scanned, scannedIndex)
    }

    private fun showArchiveChangesComplete(history: HostTargetReplacementHistory?) {
        Snackbar.make(
            binding.root,
            R.string.text_archive_changes_complete,
            Snackbar.LENGTH_LONG,
        ).apply {
            history?.takeIf(HostTargetReplacementHistory::isAvailable)?.let { available ->
                setAction(R.string.action_restore_previous_archive_version) {
                    showRestorePreviousArchiveVersionDialogFor(available)
                }
            }
        }.show()
    }

    private fun showRestorePreviousArchiveVersionDialogFor(
        history: HostTargetReplacementHistory,
    ) {
        if (targetReplacementHistory?.id != history.id) return
        showRestorePreviousArchiveVersionDialog()
    }

    private fun startRestorePreviousArchiveVersion(history: HostTargetReplacementHistory) {
        if (isBusy || targetReplacementHistory?.id != history.id) return
        val openRequest = request ?: return
        val hostSession = openRequest.hostSession ?: return
        setBusy(
            true,
            getString(R.string.text_restoring_previous_archive_version),
            cancellable = false,
        )
        operationJob = lifecycleScope.launch {
            var restored: HostTargetReplacementHistory? = null
            try {
                restored = withContext(Dispatchers.IO) {
                    hostSession.undoTargetReplacement(openRequest.targetId, history.id)
                }
                targetReplacementHistory = restored
                reloadRestoredArchiveVersion(
                    openRequest = openRequest,
                    reportedSize = restored.restoredSize ?: ArchiveIntentPolicy.SIZE_UNKNOWN,
                )
            } catch (cancelled: CancellationException) {
                setBusy(false)
                throw cancelled
            } catch (_: Throwable) {
                if (restored == null) {
                    val refreshed = refreshTargetReplacementHistory()
                    if (
                        refreshed?.state ==
                        ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_RESTORED
                    ) {
                        try {
                            reloadRestoredArchiveVersion(
                                openRequest = openRequest,
                                reportedSize = ArchiveIntentPolicy.SIZE_UNKNOWN,
                            )
                            return@launch
                        } catch (cancelled: CancellationException) {
                            setBusy(false)
                            throw cancelled
                        } catch (_: Throwable) {
                            showMessage(getString(R.string.error_archive_restore_refresh_failed))
                        }
                    } else {
                        showMessage(
                            getString(
                                if (
                                    refreshed?.state ==
                                    ExplorerActionHostSessionValues.TARGET_REPLACEMENT_UNDO_STATE_STALE
                                ) {
                                    R.string.error_previous_archive_version_changed
                                } else {
                                    R.string.error_previous_archive_version_restore_failed
                                },
                            ),
                        )
                    }
                } else {
                    showMessage(getString(R.string.error_archive_restore_refresh_failed))
                }
                setBusy(false)
                if (snapshot != null) renderEntries()
            }
        }
    }

    private suspend fun reloadRestoredArchiveVersion(
        openRequest: ArchiveOpenRequest,
        reportedSize: Long,
    ) {
        reloadArchiveAfterReplacement(
            previousRequest = openRequest,
            reportedSize = reportedSize,
        )
        refreshTargetReplacementHistory()
        Snackbar.make(
            binding.root,
            R.string.text_previous_archive_version_restore_complete,
            Snackbar.LENGTH_LONG,
        ).show()
    }

    private fun renderMutationProgress(update: ArchiveMutationProgress) {
        postUiUpdate {
            val text = when (update.phase) {
                ArchiveMutationPhase.PREPARING ->
                    getString(R.string.text_preparing_archive_changes)
                ArchiveMutationPhase.WRITING -> getString(
                    R.string.text_writing_archive_changes,
                    (update.completedEntries + 1).coerceAtMost(update.totalEntries),
                    update.totalEntries,
                    update.currentPath.orEmpty(),
                )
                ArchiveMutationPhase.VERIFYING ->
                    getString(R.string.text_verifying_archive_changes)
                ArchiveMutationPhase.COMMITTING ->
                    getString(R.string.text_committing_archive_changes)
            }
            binding.message.text = text
            binding.message.isVisible = true
        }
    }

    private fun updateManagementPresentation() = with(binding) {
        val visible = canManageArchive()
        manageActionsButton.isVisible = visible && !isBusy
        manageActionsButton.isEnabled = visible && !isBusy
        addFilesButton.isVisible = visible
        addFolderButton.isVisible = visible
        newFolderButton.isVisible = visible
        renameButton.isVisible = visible
        deleteButton.isVisible = visible
        addFilesButton.isEnabled = visible && !isBusy
        addFolderButton.isEnabled = visible && !isBusy
        newFolderButton.isEnabled = visible && !isBusy
        renameButton.isEnabled = visible && !isBusy && selectedPaths.size == 1
        deleteButton.isEnabled = visible && !isBusy && selectedPaths.isNotEmpty()
    }

    private fun conflictPolicies(): List<ArchiveExtractionConflictPolicy> =
        ArchiveExtractionConflictPolicy.entries

    private fun conflictPolicyLabel(policy: ArchiveExtractionConflictPolicy): String = getString(
        when (policy) {
            ArchiveExtractionConflictPolicy.ASK -> R.string.text_conflict_policy_ask
            ArchiveExtractionConflictPolicy.SKIP -> R.string.text_conflict_policy_skip
            ArchiveExtractionConflictPolicy.OVERWRITE -> R.string.text_conflict_policy_overwrite
            ArchiveExtractionConflictPolicy.AUTO_RENAME -> R.string.text_conflict_policy_auto_rename
        },
    )

    internal fun selectConflictPolicy(policy: ArchiveExtractionConflictPolicy) {
        if (isBusy) return
        selectedConflictPolicy = policy
        updateConflictPolicyInput()
    }

    private fun updateConflictPolicyInput() {
        val label = conflictPolicyLabel(selectedConflictPolicy)
        val details = getString(
            when (selectedConflictPolicy) {
                ArchiveExtractionConflictPolicy.ASK -> R.string.text_conflict_policy_ask_details
                ArchiveExtractionConflictPolicy.SKIP -> R.string.text_conflict_policy_skip_details
                ArchiveExtractionConflictPolicy.OVERWRITE ->
                    R.string.text_conflict_policy_overwrite_details
                ArchiveExtractionConflictPolicy.AUTO_RENAME ->
                    R.string.text_conflict_policy_auto_rename_details
            },
        )
        binding.extractionConflictPolicyInput.setText(label, false)
        binding.extractionConflictPolicyInput.contentDescription = "$label. $details"
        binding.extractionConflictPolicyLayout.helperText = details.takeUnless { usesCompactHeader }
        binding.compactConflictButton.text =
            "${getText(R.string.text_compact_conflict_policy)}: $label"
        binding.compactConflictButton.contentDescription =
            "${getText(R.string.text_extraction_conflict_policy)}. $label. $details"
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
        val label = resourceBudgetProfileLabel(selectedResourceBudgetProfile)
        val details = if (
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
        binding.extractionBudgetInput.setText(label, false)
        binding.extractionBudgetInput.contentDescription = "$label. $details"
        binding.extractionBudgetLayout.helperText = details.takeUnless { usesCompactHeader }
        binding.compactBudgetButton.text =
            "${getText(R.string.text_compact_extraction_budget)}: $label"
        binding.compactBudgetButton.contentDescription =
            "${getText(R.string.text_extraction_budget)}. $label. $details"
    }

    private fun shouldUseCompactHeader(configuration: Configuration): Boolean =
        configuration.screenHeightDp < COMPACT_HEADER_MIN_HEIGHT_DP

    private fun shouldUseUltraCompactLayout(configuration: Configuration): Boolean =
        configuration.screenHeightDp < ULTRA_COMPACT_HEADER_MIN_HEIGHT_DP

    private fun updateHeaderPresentation() = with(binding) {
        archiveName.isVisible = !usesCompactHeader
        archiveSummary.isVisible = !usesCompactHeader
        archiveHeader.isVisible = !usesCompactHeader
        compactOptions.isVisible = true
        extractionBudgetLayout.isVisible = false
        extractionConflictPolicyLayout.isVisible = false
        currentPath.isVisible = !usesUltraCompactLayout
        selectedCount.isVisible = !usesUltraCompactLayout
        upButton.isVisible = !usesCompactHeader && currentDirectory.isNotEmpty()
        updateFilenameEncodingPresentation()
        updateSearchPresentation()
        toolbar.title = if (usesCompactHeader) {
            archiveName.text.takeIf { it.isNotBlank() } ?: getText(R.string.text_archive_workspace)
        } else {
            getText(R.string.text_archive_workspace)
        }
        updateToolbarSubtitle()
    }

    private fun updateSearchPresentation() = with(binding) {
        searchLayout.isVisible = !usesUltraCompactLayout || isCompactSearchExpanded
        compactSearchButton.isVisible = usesUltraCompactLayout
        val query = searchInput.text?.toString().orEmpty().trim()
        compactSearchButton.text = if (query.isEmpty()) {
            getText(R.string.text_compact_search)
        } else {
            "${getText(R.string.text_compact_search)}: $query"
        }
        compactSearchButton.contentDescription = compactSearchButton.text
    }

    private fun toggleCompactSearch() {
        if (!usesUltraCompactLayout) return
        isCompactSearchExpanded = !isCompactSearchExpanded
        updateSearchPresentation()
        if (isCompactSearchExpanded) {
            binding.searchInput.requestFocus()
            binding.searchInput.post {
                getSystemService<InputMethodManager>()?.showSoftInput(
                    binding.searchInput,
                    0,
                )
            }
        } else {
            binding.searchInput.clearFocus()
            getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(
                binding.searchInput.windowToken,
                0,
            )
        }
    }

    private fun updateFilenameEncodingPresentation() = with(binding) {
        val isAvailable = filenameCharsetChoices.isNotEmpty()
        filenameEncodingLayout.isVisible = false
        compactEncodingButton.isVisible = isAvailable
        if (!isAvailable) return@with
        val selected = filenameCharsetChoices.firstOrNull {
            it.charsetName == selectedFilenameCharsetName
        } ?: filenameCharsetChoices.first()
        compactEncodingButton.text =
            "${getText(R.string.text_compact_filename_encoding)}: ${selected.label}"
        compactEncodingButton.contentDescription = compactEncodingButton.text
        filenameEncodingLayout.isEnabled = !isBusy
        compactEncodingButton.isEnabled = !isBusy
    }

    internal fun showCompactResourceBudgetDialog(): androidx.appcompat.app.AlertDialog {
        val profiles = resourceBudgetProfiles()
        val labels = profiles.map(::resourceBudgetProfileLabel).toTypedArray()
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.text_extraction_budget)
            .setSingleChoiceItems(
                labels,
                profiles.indexOf(selectedResourceBudgetProfile),
            ) { dialog, which ->
                dialog.dismiss()
                profiles.getOrNull(which)?.let(::selectResourceBudgetProfile)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    internal fun showCompactConflictPolicyDialog(): androidx.appcompat.app.AlertDialog {
        val policies = conflictPolicies()
        val labels = policies.map(::conflictPolicyLabel).toTypedArray()
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.text_extraction_conflict_policy)
            .setSingleChoiceItems(
                labels,
                policies.indexOf(selectedConflictPolicy),
            ) { dialog, which ->
                dialog.dismiss()
                policies.getOrNull(which)?.let(::selectConflictPolicy)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    internal fun showCompactFilenameEncodingDialog(): androidx.appcompat.app.AlertDialog? {
        if (filenameCharsetChoices.isEmpty()) return null
        val labels = filenameCharsetChoices.map(FilenameCharsetChoice::label).toTypedArray()
        val selectedIndex = filenameCharsetChoices.indexOfFirst {
            it.charsetName == selectedFilenameCharsetName
        }.coerceAtLeast(0)
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.text_filename_encoding)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                dialog.dismiss()
                filenameCharsetChoices.getOrNull(which)?.let(::selectFilenameCharset)
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    private fun updateArchiveSummary(summary: CharSequence) {
        binding.archiveSummary.text = summary
        updateToolbarSubtitle()
    }

    private fun updateSelectionPresentation(selectionCount: Int) {
        renderedSelectionCount = selectionCount
        binding.selectedCount.text = getString(R.string.text_selected_count, selectionCount)
        binding.selectedCount.isVisible = !usesUltraCompactLayout
        updateToolbarSubtitle()
    }

    private fun updateToolbarSubtitle() = with(binding) {
        if (usesCompactHeader && usesUltraCompactLayout) {
            val contextLabel = if (renderedSelectionCount > 0) {
                selectedCount.text
            } else {
                currentPath.text.takeIf { it.isNotBlank() }
                    ?: archiveSummary.text.takeIf { it.isNotBlank() }
            }
            val archiveTitle = archiveName.text.takeIf { it.isNotBlank() }
                ?: getText(R.string.app_name)
            toolbar.title = listOfNotNull(archiveTitle, contextLabel)
                .joinToString(separator = "  ")
            toolbar.subtitle = null
        } else {
            toolbar.subtitle = when {
                !usesCompactHeader -> null
                else -> archiveSummary.text.takeIf { it.isNotBlank() }
            }
        }
        toolbar.contentDescription = listOfNotNull(
            toolbar.title?.takeIf { it.isNotBlank() },
            archiveSummary.text.takeIf { it.isNotBlank() },
            currentPath.text.takeIf { it.isNotBlank() },
            selectedCount.text.takeIf { it.isNotBlank() },
        ).joinToString(separator = ". ")
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

    internal fun showExtractionScopeDialog(): androidx.appcompat.app.AlertDialog? {
        val archive = snapshot ?: return null
        val archiveIndex = index ?: return null
        val options = ArchiveExtractionScopeResolver.options(
            currentDirectory = currentDirectory,
            selectedPaths = selectedPaths,
            currentDirectoryCanExtract = !archive.isIsolatedPath(currentDirectory),
        )
        val currentPath = "/${archiveIndex.displayPath(currentDirectory)}"
        val labels = options.map { option ->
            when (option.scope) {
                ArchiveExtractionScope.ENTIRE_ARCHIVE ->
                    getString(R.string.text_extraction_scope_entire_archive)
                ArchiveExtractionScope.CURRENT_DIRECTORY -> getString(
                    R.string.text_extraction_scope_current_directory,
                    ArchivePathPolicy.unsafeSourceNameForDisplay(currentPath),
                )
                ArchiveExtractionScope.CURRENT_SELECTION ->
                    getString(R.string.text_extraction_scope_current_selection)
            }
        }.toTypedArray()
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_extraction_scope)
            .setItems(labels) { _, position ->
                options.getOrNull(position)?.let { option ->
                    chooseExtractionDestination(option.requestedPaths)
                }
            }
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .show()
    }

    private fun chooseExtractionDestination(
        requestedPaths: Collection<String>,
        skipUnsafePathsConfirmed: Boolean = false,
        resourceBudgetConfirmed: Boolean = false,
    ) {
        val archive = snapshot
        val archiveIndex = index
        val extractionPaths = requestedPaths.toCollection(LinkedHashSet())
        if (archive == null || archiveIndex == null || extractionPaths.isEmpty()) {
            Toast.makeText(this, R.string.error_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        pendingExtractionPaths = extractionPaths
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
                        requestedPaths = extractionPaths,
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
                        requestedPaths = extractionPaths,
                        skipUnsafePathsConfirmed = skipUnsafePathsConfirmed,
                        resourceBudgetConfirmed = true,
                    )
                }
                .show()
            return
        }
        pendingSkipUnsafePaths = selection.skippedUnsafeEntries.isNotEmpty()
        pendingAllowResourceBudgetOverride = budgetAssessment.exceedsBudget
        pendingConflictPolicy = selectedConflictPolicy
        showExtractionDestinationDialog()
    }

    internal fun showExtractionDestinationDialog(): androidx.appcompat.app.AlertDialog? {
        val openRequest = request ?: return null
        if (!canUseHostExtractionDestination()) {
            outputTreeLauncher.launch(null)
            return null
        }
        val destinationBinding = DialogExtractionDestinationBinding.inflate(layoutInflater)
        destinationBinding.destinationMessage.text = getString(
            R.string.dialog_message_extraction_destination,
            ArchiveExtractionNaming.rootName(openRequest.displayName, snapshot?.format ?: ArchiveFormat.ZIP),
        )
        destinationBinding.currentFolderButton.text = getString(
            R.string.text_extraction_destination_current_folder,
            openRequest.parentDisplayPath,
        )
        destinationBinding.chooseFolderButton.text =
            getString(R.string.text_extraction_destination_choose_folder)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_extraction_destination)
            .setView(destinationBinding.root)
            .setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
                clearPendingExtraction()
            }
            .create()
        destinationBinding.currentFolderButton.setOnClickListener {
            dialog.dismiss()
            extractToHost()
        }
        destinationBinding.chooseFolderButton.setOnClickListener {
            dialog.dismiss()
            outputTreeLauncher.launch(null)
        }
        dialog.setOnCancelListener { clearPendingExtraction() }
        dialog.show()
        return dialog
    }

    internal fun canUseHostExtractionDestination(): Boolean = request?.let { openRequest ->
        openRequest.hostSession != null &&
            openRequest.requestedAction == ArchiveRequestedAction.EXTRACT_TO
    } == true

    private fun clearPendingExtraction() {
        pendingExtractionPaths = emptySet()
        pendingSkipUnsafePaths = false
        pendingAllowResourceBudgetOverride = false
        pendingConflictPolicy = null
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
        chooseExtractionDestination(setOf(ArchivePathPolicy.ROOT_PATH))
    }

    private fun resumeRequestedActionAfterScan() {
        pendingOutputTreeUri?.let { treeUri ->
            pendingOutputTreeUri = null
            extractTo(treeUri)
        } ?: if (pendingExtractionPaths.isNotEmpty()) {
            chooseExtractionDestination(
                requestedPaths = pendingExtractionPaths,
                skipUnsafePathsConfirmed = pendingSkipUnsafePaths,
                resourceBudgetConfirmed = pendingAllowResourceBudgetOverride,
            )
        } else {
            request?.let(::startDirectExtractionIfRequested)
        }
    }

    private fun extractTo(treeUri: Uri) = startExtraction {
        SafArchiveOutputWriter(contentResolver, treeUri)
    }

    private fun extractToHost() {
        val hostSession = request?.hostSession ?: run {
            outputTreeLauncher.launch(null)
            return
        }
        startExtraction { HostArchiveOutputWriter(hostSession) }
    }

    private fun startExtraction(
        writerFactory: () -> ArchiveOutputWriter,
    ) {
        val staged = stagedArchive ?: return
        val archive = snapshot ?: return
        val paths = pendingExtractionPaths.takeIf(Set<String>::isNotEmpty) ?: return
        val skipUnsafePaths = pendingSkipUnsafePaths
        val allowResourceBudgetOverride = pendingAllowResourceBudgetOverride
        val conflictPolicy = pendingConflictPolicy ?: selectedConflictPolicy
        val resourceBudget = selectedResourceBudget()
        pendingExtractionPaths = emptySet()
        pendingSkipUnsafePaths = false
        pendingAllowResourceBudgetOverride = false
        pendingConflictPolicy = null
        setBusy(true, getString(R.string.text_preparing_extraction), cancellable = true)
        operationJob = lifecycleScope.launch {
            var reportedResidualOutputs = emptyList<ArchiveOutputLocation>()
            try {
                val rootName = ArchiveExtractionNaming.rootName(
                    displayName = request?.displayName.orEmpty(),
                    format = archive.format,
                )
                val result = withContext(Dispatchers.IO) {
                    val progressTracker = ExtractionProgressTracker()
                    var lastReportedBytes = -PROGRESS_REPORT_BYTES
                    var lastCompletedEntries = -1
                    var lastReportedElapsedMillis = -PROGRESS_REPORT_INTERVAL_MILLIS
                    ArchiveExtractor(resourceBudget = resourceBudget).extractToWriter(
                        source = staged.source,
                        snapshot = archive,
                        selectedPaths = paths,
                        rootName = rootName,
                        writer = writerFactory(),
                        skipUnsafePaths = skipUnsafePaths,
                        allowResourceBudgetOverride = allowResourceBudgetOverride,
                        conflictPolicy = conflictPolicy,
                        conflictResolver = ArchiveExtractionConflictResolver { conflict ->
                            resolveExtractionConflict(conflict)
                        },
                        progress = ArchiveProgressListener { update ->
                            val metrics = progressTracker.update(update)
                            when (update.phase) {
                                ExtractionPhase.CLEANING_UP -> postUiUpdate {
                                    renderExtractionCleanup()
                                }
                                ExtractionPhase.CLEANUP_FAILED -> {
                                    reportedResidualOutputs = update.residualOutputs
                                    postUiUpdate {
                                        renderExtractionCleanupFailure(update.residualOutputs)
                                    }
                                }
                                ExtractionPhase.PREPARING -> postUiUpdate {
                                    renderExtractionPreparing()
                                }
                                ExtractionPhase.COMMITTING -> postUiUpdate {
                                    renderExtractionCommitting()
                                }
                                ExtractionPhase.EXTRACTING -> if (
                                    update.bytesWritten - lastReportedBytes >= PROGRESS_REPORT_BYTES ||
                                    update.completedEntries == update.totalEntries ||
                                    update.completedEntries - lastCompletedEntries >= PROGRESS_REPORT_ENTRIES ||
                                    metrics.elapsedMillis - lastReportedElapsedMillis >=
                                    PROGRESS_REPORT_INTERVAL_MILLIS
                                ) {
                                    lastReportedBytes = update.bytesWritten
                                    lastCompletedEntries = update.completedEntries
                                    lastReportedElapsedMillis = metrics.elapsedMillis
                                    postUiUpdate {
                                        renderExtractionProgress(update, metrics)
                                    }
                                }
                                else -> Unit
                            }
                        },
                    )
                }
                val completedHeadline = resources.getQuantityString(
                    R.plurals.text_extraction_complete,
                    result.filesExtracted,
                    result.filesExtracted,
                    result.root.displayName,
                )
                val conflictCount = result.entriesSkipped +
                    result.entriesOverwritten +
                    result.entriesAutoRenamed
                val completedSummary = if (conflictCount > 0) {
                    "$completedHeadline\n${getString(
                        R.string.text_extraction_conflict_summary,
                        result.entriesSkipped,
                        result.entriesOverwritten,
                        result.entriesAutoRenamed,
                    )}"
                } else {
                    completedHeadline
                }
                val completedMessage = "$completedSummary\n${getString(
                    R.string.text_extraction_output_path,
                    ArchivePathPolicy.unsafeSourceNameForDisplay(result.root.identifier),
                )}"
                showMessage(completedMessage)
                Toast.makeText(this@ArchiveManagerActivity, completedMessage, Toast.LENGTH_LONG).show()
                setBusy(false)
                renderEntries()
            } catch (cancelled: CancellationException) {
                val cleanupMessage = cleanupResidualMessage(
                    reportedResidualOutputs + cancelled.residualArchiveOutputs(),
                )
                val message = cleanupMessage ?: getString(R.string.text_cancelled)
                showMessage(message)
                if (cleanupMessage != null) {
                    Toast.makeText(
                        this@ArchiveManagerActivity,
                        R.string.error_cleanup_failed,
                        Toast.LENGTH_LONG,
                    ).show()
                }
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                showFailure(
                    error = error,
                    headline = headlineForExtractionFailure(error),
                    formatHint = archive.format,
                    stageHint = ArchiveFailureStage.ENTRY_DATA,
                    residualOutputs = reportedResidualOutputs,
                )
                setBusy(false)
                renderEntries()
            }
        }
    }

    internal suspend fun resolveExtractionConflict(
        conflict: ArchiveExtractionConflict,
    ): ArchiveExtractionConflictResolution = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val dialog = createExtractionConflictDialog(
                conflict = conflict,
                onResolution = { resolution ->
                    if (continuation.isActive) continuation.resume(resolution)
                },
                onCancelled = {
                    if (continuation.isActive) {
                        continuation.cancel(
                            CancellationException("Output conflict decision cancelled"),
                        )
                    }
                },
            )
            continuation.invokeOnCancellation {
                runOnUiThread {
                    if (dialog.isShowing) dialog.dismiss()
                }
            }
            dialog.show()
        }
    }

    internal fun createExtractionConflictDialog(
        conflict: ArchiveExtractionConflict,
        onResolution: (ArchiveExtractionConflictResolution) -> Unit,
        onCancelled: () -> Unit,
    ): androidx.appcompat.app.AlertDialog {
        val conflictBinding = DialogArchiveOutputConflictBinding.inflate(layoutInflater)
        val existingType = getString(
            if (conflict.existingIsDirectory) {
                R.string.text_item_type_directory
            } else {
                R.string.text_item_type_file
            },
        )
        val incomingType = getString(
            if (conflict.incomingIsDirectory) {
                R.string.text_item_type_directory
            } else {
                R.string.text_item_type_file
            },
        )
        conflictBinding.conflictMessage.text = getString(
            R.string.dialog_message_output_conflict,
            ArchivePathPolicy.unsafeSourceNameForDisplay(conflict.archivePath),
            ArchivePathPolicy.unsafeSourceNameForDisplay(conflict.existingDisplayName),
            existingType,
            ArchivePathPolicy.unsafeSourceNameForDisplay(conflict.requestedDisplayName),
            incomingType,
        )
        conflictBinding.conflictOverwrite.isEnabled = conflict.canOverwrite
        conflictBinding.overwriteUnavailable.isVisible = !conflict.canOverwrite

        var completed = false
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_output_conflict)
            .setView(conflictBinding.root)
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .setPositiveButton(R.string.dialog_button_continue, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
                dialog.cancel()
            }
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val decision = when (conflictBinding.conflictDecision.checkedRadioButtonId) {
                    R.id.conflictSkip -> ArchiveExtractionConflictDecision.SKIP
                    R.id.conflictOverwrite -> ArchiveExtractionConflictDecision.OVERWRITE
                    else -> ArchiveExtractionConflictDecision.AUTO_RENAME
                }
                if (
                    decision == ArchiveExtractionConflictDecision.OVERWRITE &&
                    !conflict.canOverwrite
                ) {
                    return@setOnClickListener
                }
                completed = true
                onResolution(
                    ArchiveExtractionConflictResolution(
                        decision = decision,
                        applyToAll = conflictBinding.applyToAll.isChecked,
                    ),
                )
                dialog.dismiss()
            }
        }
        dialog.setOnCancelListener {
            if (!completed) {
                completed = true
                onCancelled()
            }
        }
        dialog.setOnDismissListener {
            if (!completed) {
                completed = true
                onCancelled()
            }
        }
        return dialog
    }

    private fun setBusy(busy: Boolean, status: String? = null, cancellable: Boolean = false) {
        isBusy = busy
        if (busy) {
            renderGeneration++
            renderJob?.cancel()
        }
        binding.progress.isVisible = busy
        if (!busy) setProgressIndicatorIndeterminate(true)
        binding.cancelButton.isVisible = busy && cancellable
        binding.selectAllButton.isVisible = !busy
        binding.cancelButton.isEnabled = busy && cancellable
        binding.searchInput.isEnabled = !busy
        binding.extractionBudgetLayout.isEnabled = !busy
        binding.extractionConflictPolicyLayout.isEnabled = !busy
        binding.filenameEncodingLayout.isEnabled = !busy
        binding.compactBudgetButton.isEnabled = !busy
        binding.compactConflictButton.isEnabled = !busy
        binding.compactEncodingButton.isEnabled = !busy
        binding.archivePasswordLayout.isEnabled = !busy
        binding.archivePassword.isEnabled = !busy
        binding.applyPasswordButton.isEnabled = !busy
        binding.toolbar.menu.findItem(R.id.actionArchiveInformation).isEnabled =
            !busy && snapshot != null
        updateTargetReplacementAction()
        binding.upButton.isEnabled = !busy && currentDirectory.isNotEmpty()
        binding.selectAllButton.isEnabled = !busy && snapshot != null
        binding.extractButton.isEnabled = !busy && snapshot != null
        updateManagementPresentation()
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
        residualOutputs: List<ArchiveOutputLocation> = emptyList(),
    ) {
        val diagnostic = ArchiveFailureDiagnostic.from(
            error = error,
            formatHint = formatHint,
            stageHint = stageHint,
            archiveDisplayName = request?.displayName,
        )
        lastFailureDiagnostic = diagnostic
        val diagnosticMessage = getString(
            R.string.error_diagnostic_details,
            headline,
            diagnostic.format?.displayName ?: getString(R.string.text_unknown),
            failureStageLabel(diagnostic.stage),
            diagnostic.code?.name ?: getString(R.string.text_unknown),
            failureReason(diagnostic),
        )
        binding.message.text = cleanupResidualMessage(
            residualOutputs + error.residualArchiveOutputs(),
        )?.let { cleanup ->
            "$diagnosticMessage\n\n$cleanup"
        } ?: diagnosticMessage
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
        staged?.close()
    }

    @Synchronized
    private fun clearSelectedPassword() {
        selectedPassword?.fill('\u0000')
        selectedPassword = null
    }

    @Synchronized
    private fun closeHostSession() {
        if (hostSessionClosed) return
        hostSessionClosed = true
        runCatching { request?.hostSession?.close() }
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
        pendingAddDirectory = savedInstanceState.getString(STATE_PENDING_ADD_DIRECTORY)
        pendingImportParentPath = savedInstanceState.getString(STATE_PENDING_IMPORT_PARENT_PATH)
        pendingSkipUnsafePaths = savedInstanceState.getBoolean(
            STATE_PENDING_SKIP_UNSAFE_PATHS,
            false,
        )
        pendingAllowResourceBudgetOverride = savedInstanceState.getBoolean(
            STATE_PENDING_RESOURCE_BUDGET_OVERRIDE,
            false,
        )
        pendingConflictPolicy = savedInstanceState.getString(STATE_PENDING_CONFLICT_POLICY)
            ?.let { stored ->
                ArchiveExtractionConflictPolicy.entries.firstOrNull { it.name == stored }
            }
        directActionHandled = savedInstanceState.getBoolean(STATE_DIRECT_ACTION_HANDLED, false)
        selectedResourceBudgetProfile = savedInstanceState
            .getString(STATE_RESOURCE_BUDGET_PROFILE)
            ?.let { stored ->
                ArchiveResourceBudgetProfile.entries.firstOrNull { it.name == stored }
            }
            ?: ArchiveResourceBudgetProfile.COMPATIBLE
        selectedConflictPolicy = savedInstanceState.getString(STATE_CONFLICT_POLICY)
            ?.let { stored ->
                ArchiveExtractionConflictPolicy.entries.firstOrNull { it.name == stored }
            }
            ?: ArchiveExtractionConflictPolicy.ASK
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
            ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
            ArchiveFailureCode.OUTPUT_FAILURE,
            -> getString(R.string.error_cannot_create_output)
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
                ArchiveFailureCode.OUTPUT_CONFLICT_CONFIRMATION_REQUIRED,
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

    private fun formatBytes(bytes: Long): String = when {
        bytes < 0L -> getString(R.string.text_unknown)
        else -> Formatter.formatFileSize(this, bytes)
    }

    private fun renderExtractionPreparing() {
        if (!isBusy) return
        setProgressIndicatorIndeterminate(true)
        binding.message.text = getString(R.string.text_preparing_extraction)
        binding.cancelButton.isEnabled = true
    }

    private fun renderExtractionCommitting() {
        if (!isBusy) return
        setProgressIndicatorIndeterminate(true)
        binding.message.text = getString(R.string.text_committing_extraction)
        binding.cancelButton.isEnabled = false
    }

    private fun renderExtractionProgress(
        update: ExtractionProgress,
        metrics: ExtractionProgressMetrics,
    ) {
        if (!isBusy) return
        metrics.fraction?.let { fraction ->
            setProgressIndicatorIndeterminate(false)
            binding.progress.max = EXTRACTION_PROGRESS_MAX
            binding.progress.setProgressCompat(
                (fraction * EXTRACTION_PROGRESS_MAX).roundToInt()
                    .coerceIn(0, EXTRACTION_PROGRESS_MAX),
                true,
            )
        } ?: setProgressIndicatorIndeterminate(true)

        binding.message.text = buildList {
            add(
                getString(
                    R.string.text_extracting,
                    update.completedEntries,
                    update.totalEntries,
                ),
            )
            update.currentPath?.let { path ->
                add(
                    getString(
                        R.string.text_extraction_current_item,
                        ArchivePathPolicy.unsafeSourceNameForDisplay(path),
                    ),
                )
            }
            add(
                getString(
                    R.string.text_extraction_progress_bytes,
                    formatBytes(update.bytesWritten),
                    formatBytes(update.totalBytes),
                ),
            )
            metrics.bytesPerSecond?.let { rate ->
                val formattedRate = formatBytes(rate)
                add(
                    metrics.estimatedRemainingMillis?.let { remaining ->
                        getString(
                            R.string.text_extraction_rate_eta,
                            formattedRate,
                            formatElapsedDuration(remaining),
                        )
                    } ?: getString(R.string.text_extraction_rate, formattedRate),
                )
            }
        }.joinToString("\n")
        binding.cancelButton.isEnabled = true
    }

    private fun renderExtractionCleanup() {
        if (!isBusy) return
        setProgressIndicatorIndeterminate(true)
        binding.message.text = getString(R.string.text_cleaning_up)
        binding.cancelButton.isEnabled = false
    }

    private fun renderExtractionCleanupFailure(outputs: List<ArchiveOutputLocation>) {
        if (!isBusy) return
        setProgressIndicatorIndeterminate(true)
        binding.message.text = cleanupResidualMessage(outputs)
            ?: getString(R.string.error_cleanup_failed)
        binding.cancelButton.isEnabled = false
    }

    private fun setProgressIndicatorIndeterminate(indeterminate: Boolean) {
        if (binding.progress.isIndeterminate == indeterminate) return
        val wasVisible = binding.progress.isVisible
        if (wasVisible) binding.progress.isVisible = false
        binding.progress.isIndeterminate = indeterminate
        if (wasVisible) binding.progress.isVisible = true
    }

    private fun formatElapsedDuration(durationMillis: Long): String {
        val seconds = durationMillis / MILLIS_PER_SECOND +
            if (durationMillis % MILLIS_PER_SECOND == 0L) 0L else 1L
        return DateUtils.formatElapsedTime(seconds.coerceAtLeast(1L))
    }

    private fun cleanupResidualMessage(outputs: Collection<ArchiveOutputLocation>): String? {
        val uniqueOutputs = outputs.distinctBy(ArchiveOutputLocation::identifier)
        if (uniqueOutputs.isEmpty()) return null
        val locations = uniqueOutputs.joinToString("\n") { output ->
            val name = ArchivePathPolicy.unsafeSourceNameForDisplay(output.displayName)
            val identifier = ArchivePathPolicy.unsafeSourceNameForDisplay(output.identifier)
            "- $name\n  $identifier"
        }
        return getString(R.string.error_cleanup_residual_outputs, locations)
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
        const val COMPACT_HEADER_MIN_HEIGHT_DP = 600
        const val ULTRA_COMPACT_HEADER_MIN_HEIGHT_DP = 400
        const val MIB = 1_024L * 1_024L
        const val GIB = 1_024L * MIB
        const val MAX_MIB_VALUE = Long.MAX_VALUE / MIB
        const val MAX_FILENAME_CHARSET_LENGTH = 64
        const val MAX_SAVED_STATE_CHARS = 128 * 1024
        const val MAX_SAVED_STATE_PATHS = 2_048
        const val EXTRACTION_PROGRESS_MAX = 1_000
        const val MILLIS_PER_SECOND = 1_000L
        const val PROGRESS_REPORT_BYTES = 8L * 1024L * 1024L
        const val PROGRESS_REPORT_ENTRIES = 64
        const val PROGRESS_REPORT_INTERVAL_MILLIS = 250L
        const val RENDER_CANCELLATION_INTERVAL = 64
        const val SEARCH_DEBOUNCE_MILLIS = 150L
        const val STATE_CURRENT_DIRECTORY = "current_directory"
        const val STATE_CONFLICT_POLICY = "conflict_policy"
        const val STATE_DIRECT_ACTION_HANDLED = "direct_action_handled"
        const val STATE_FILENAME_CHARSET = "filename_charset"
        const val STATE_PENDING_EXTRACTION_PATHS = "pending_extraction_paths"
        const val STATE_PENDING_ADD_DIRECTORY = "pending_add_directory"
        const val STATE_PENDING_IMPORT_PARENT_PATH = "pending_import_parent_path"
        const val STATE_PENDING_OUTPUT_TREE_URI = "pending_output_tree_uri"
        const val STATE_PENDING_CONFLICT_POLICY = "pending_conflict_policy"
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
