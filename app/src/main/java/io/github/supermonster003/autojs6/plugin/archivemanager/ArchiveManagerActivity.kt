package io.github.supermonster003.autojs6.plugin.archivemanager

import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    }

    override fun onDestroy() {
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
                val (scanned, scannedIndex) = withContext(Dispatchers.IO) {
                    ArchiveScanner().scan(staged.file) { ensureActive() }.let { scanned ->
                        scanned to ArchiveIndex(scanned)
                    }
                }
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
                setBusy(false)
                renderEntries()
                pendingOutputTreeUri?.let { treeUri ->
                    pendingOutputTreeUri = null
                    extractTo(treeUri)
                } ?: startDirectExtractionIfRequested(resolvedRequest = request)
            } catch (cancelled: CancellationException) {
                clearStagedArchive()
                showMessage(getString(R.string.text_cancelled))
                setBusy(false)
                throw cancelled
            } catch (error: Throwable) {
                clearStagedArchive()
                showMessage(messageForLoadFailure(error))
                setBusy(false)
            }
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
            binding.message.isVisible = result.rows.isEmpty()
            if (result.rows.isEmpty()) binding.message.text = getString(R.string.text_no_entries)
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
                append(
                    getString(
                        R.string.text_size_details,
                        formatBytes(archiveEntry.uncompressedSize),
                        formatBytes(archiveEntry.compressedSize),
                        archiveEntry.crc32?.let { String.format(Locale.ROOT, "%08X", it) }
                            ?: getString(R.string.text_unknown),
                    ),
                )
                archiveEntry.modifiedTimeMillis?.let { modified ->
                    append('\n')
                    append(getString(R.string.text_modified, formatDate(modified)))
                }
                if (!archiveEntry.canExtract) {
                    append('\n')
                    append(
                        if (archiveEntry.isEncrypted) {
                            getString(R.string.text_encrypted_entry)
                        } else {
                            getString(
                                R.string.text_unsupported_compression_method,
                                archiveEntry.compressionMethodId,
                            )
                        },
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
        if (snapshot == null || selectedPaths.isEmpty()) {
            Toast.makeText(this, R.string.error_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        pendingExtractionPaths = selectedPaths.toSet()
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

    private fun extractTo(treeUri: Uri) {
        val staged = stagedArchive ?: return
        val archive = snapshot ?: return
        val paths = pendingExtractionPaths.takeIf(Set<String>::isNotEmpty) ?: return
        pendingExtractionPaths = emptySet()
        setBusy(true, getString(R.string.text_extracting, 0, 0), cancellable = true)
        operationJob = lifecycleScope.launch {
            try {
                val rootName = extractionRootName(request?.displayName.orEmpty())
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
                showMessage(messageForExtractionFailure(error))
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
        binding.upButton.isEnabled = !busy && currentDirectory.isNotEmpty()
        binding.selectAllButton.isEnabled = !busy && snapshot != null
        binding.extractButton.isEnabled = !busy && snapshot != null && selectedPaths.isNotEmpty()
        if (status != null) {
            binding.message.text = status
            binding.message.isVisible = true
        } else if (busy) {
            binding.message.isVisible = false
        }
    }

    private fun showMessage(message: String) {
        binding.message.text = message
        binding.message.isVisible = true
    }

    @Synchronized
    private fun clearStagedArchive() {
        val staged = stagedArchive
        stagedArchive = null
        staged?.delete()
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

    private fun postUiUpdate(update: () -> Unit) {
        if (isFinishing || isDestroyed) return
        binding.root.post {
            if (!isFinishing && !isDestroyed) update()
        }
    }

    private fun messageForLoadFailure(error: Throwable): String = withFailureReason(error, when (error) {
        is ArchiveInputLimitException -> getString(R.string.error_archive_limit)
        is ArchiveException -> when (error.code) {
            ArchiveFailureCode.ENTRY_LIMIT_EXCEEDED,
            ArchiveFailureCode.PATH_LIMIT_EXCEEDED,
            ArchiveFailureCode.DEPTH_LIMIT_EXCEEDED,
            ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
            -> getString(R.string.error_archive_limit)
            else -> getString(R.string.error_archive_invalid)
        }
        else -> getString(R.string.error_cannot_open_archive)
    })

    private fun messageForExtractionFailure(error: Throwable): String = withFailureReason(error, when (error) {
        is ArchiveException -> when (error.code) {
            ArchiveFailureCode.SINGLE_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.TOTAL_SIZE_LIMIT_EXCEEDED,
            ArchiveFailureCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
            -> getString(R.string.error_archive_limit)
            ArchiveFailureCode.OUTPUT_FAILURE -> getString(R.string.error_cannot_create_output)
            else -> getString(R.string.error_extraction_failed)
        }
        else -> getString(R.string.error_extraction_failed)
    })

    private fun withFailureReason(error: Throwable, headline: String): String {
        val reason = error.message?.trim()?.takeIf(String::isNotEmpty) ?: return headline
        return getString(R.string.error_with_reason, headline, reason.take(MAX_ERROR_REASON_LENGTH))
    }

    private fun extractionRootName(displayName: String): String {
        val candidate = displayName.substringBeforeLast('.', displayName)
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

    private companion object {
        const val DEFAULT_EXTRACTION_ROOT = "archive"
        const val MAX_EXTRACTION_ROOT_LENGTH = 120
        const val MAX_ERROR_REASON_LENGTH = 512
        const val MAX_SAVED_STATE_CHARS = 128 * 1024
        const val MAX_SAVED_STATE_PATHS = 2_048
        const val PROGRESS_REPORT_BYTES = 8L * 1024L * 1024L
        const val PROGRESS_REPORT_ENTRIES = 64
        const val RENDER_CANCELLATION_INTERVAL = 64
        const val SEARCH_DEBOUNCE_MILLIS = 150L
        const val STATE_CURRENT_DIRECTORY = "current_directory"
        const val STATE_DIRECT_ACTION_HANDLED = "direct_action_handled"
        const val STATE_PENDING_EXTRACTION_PATHS = "pending_extraction_paths"
        const val STATE_PENDING_OUTPUT_TREE_URI = "pending_output_tree_uri"
        const val STATE_SELECTED_PATHS = "selected_paths"
    }
}
