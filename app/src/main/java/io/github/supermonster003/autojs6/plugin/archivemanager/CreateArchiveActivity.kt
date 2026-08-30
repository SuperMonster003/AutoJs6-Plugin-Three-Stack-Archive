package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.supermonster003.autojs6.plugin.archivemanager.databinding.ActivityCreateArchiveBinding
import org.autojs.plugin.explorer.api.ExplorerActionHostSessionValues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit

class CreateArchiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateArchiveBinding
    private var request: ArchiveCompressionRequest? = null
    private var operationJob: Job? = null
    private var creationAttemptId = 0L
    private val archiveEngine = ArchiveEngine.DEFAULT
    private var selectedFormat = ArchiveFormat.ZIP
    private var compressionLevel = ArchiveCompressionPolicy.DEFAULT_COMPRESSION_LEVEL
    private var selectedConflictPolicy = ArchiveCreationConflictPolicy.AUTO_RENAME
    private var createSeparateArchives = false
    private var moveSourcesToTrash = false
    private var operationCancellable = true
    private var selectedSplitVolumeSizeMiB: Long? = null
    private var splitVolumeChoices: List<SplitVolumeChoice> = emptyList()
    private var renderingSplitVolume = false
    private var unavailableNameDialog: androidx.appcompat.app.AlertDialog? = null
    private var sessionClosed = false
    private var terminalFailureMessage: String? = null
    private var completedArchiveMessage: String? = null
    private var completedRestoreMessage: String? = null
    private var activeTargetTrashBatchId: String? = null
    private var activeTargetTrashBatch: HostTargetTrashBatch? = null
    private var targetTrashBatches: List<HostTargetTrashBatch> = emptyList()
    private var targetTrashHistoryMenuItem: MenuItem? = null
    private var targetTrashHistoryRefreshGeneration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateArchiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedConflictPolicy = savedInstanceState
            ?.getString(STATE_CONFLICT_POLICY)
            ?.let { stored ->
                ArchiveCreationConflictPolicy.entries.firstOrNull { it.name == stored }
            }
            ?: ArchiveCreationConflictPolicy.AUTO_RENAME
        createSeparateArchives = savedInstanceState?.getBoolean(STATE_SEPARATE_ARCHIVES) == true
        moveSourcesToTrash = savedInstanceState?.getBoolean(STATE_MOVE_SOURCES_TO_TRASH) == true
        selectedSplitVolumeSizeMiB = savedInstanceState
            ?.takeIf { it.containsKey(STATE_SPLIT_VOLUME_SIZE_MIB) }
            ?.getLong(STATE_SPLIT_VOLUME_SIZE_MIB)
            ?.takeIf { sizeMiB ->
                sizeMiB in ArchiveSplitVolumePolicy.MIN_SIZE_MIB..
                    ArchiveSplitVolumePolicy.MAX_SIZE_MIB
        }
        terminalFailureMessage = savedInstanceState?.getString(STATE_TERMINAL_FAILURE)
        completedArchiveMessage = savedInstanceState?.getString(STATE_COMPLETED_ARCHIVE_MESSAGE)
        completedRestoreMessage = savedInstanceState?.getString(STATE_COMPLETED_RESTORE_MESSAGE)
        activeTargetTrashBatchId = savedInstanceState?.getString(STATE_TARGET_TRASH_BATCH_ID)

        val resolvedRequest = ArchiveCompressionIntentPolicy.resolve(intent)
        if (resolvedRequest == null) {
            Toast.makeText(this, R.string.error_compression_request_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        request = resolvedRequest
        setupViews(resolvedRequest)
        when {
            terminalFailureMessage != null -> {
                closeHostSession()
                renderTerminalFailure(requireNotNull(terminalFailureMessage))
            }
            completedArchiveMessage != null -> {
                renderCreationCompleted()
                loadActiveTargetTrashBatch()
                refreshTargetTrashHistory()
            }
            else -> refreshTargetTrashHistory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CONFLICT_POLICY, selectedConflictPolicy.name)
        outState.putBoolean(STATE_SEPARATE_ARCHIVES, createSeparateArchives)
        outState.putBoolean(STATE_MOVE_SOURCES_TO_TRASH, moveSourcesToTrash)
        selectedSplitVolumeSizeMiB?.let {
            outState.putLong(STATE_SPLIT_VOLUME_SIZE_MIB, it)
        }
        terminalFailureMessage?.let { outState.putString(STATE_TERMINAL_FAILURE, it) }
        completedArchiveMessage?.let {
            outState.putString(STATE_COMPLETED_ARCHIVE_MESSAGE, it)
        }
        completedRestoreMessage?.let {
            outState.putString(STATE_COMPLETED_RESTORE_MESSAGE, it)
        }
        activeTargetTrashBatchId?.let {
            outState.putString(STATE_TARGET_TRASH_BATCH_ID, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.password.text?.clear()
            binding.passwordConfirmation.text?.clear()
        }
        if (isFinishing) {
            val activeJob = operationJob
            activeJob?.cancel()
            if (activeJob?.isActive == true) {
                activeJob.invokeOnCompletion { closeHostSession() }
            } else {
                closeHostSession()
            }
        }
        super.onDestroy()
    }

    private fun setupViews(request: ArchiveCompressionRequest) = with(binding) {
        toolbar.setNavigationOnClickListener { handleBack() }
        targetTrashHistoryMenuItem = toolbar.menu.add(
            Menu.NONE,
            MENU_TARGET_TRASH_HISTORY,
            Menu.NONE,
            R.string.action_source_recovery_history,
        ).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            isVisible = false
        }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId != MENU_TARGET_TRASH_HISTORY) return@setOnMenuItemClickListener false
            showTargetTrashHistory()
            true
        }
        selectionSummary.text = resources.getQuantityString(
            R.plurals.text_compression_selection_summary,
            request.targets.size,
            request.targets.size,
        )
        outputPath.text = request.parentDisplayPath
        val creatableFormats = archiveEngine.creatableFormats
        check(creatableFormats.isNotEmpty()) { "No archive writer is available" }
        selectedFormat = creatableFormats.first()
        outputName.setText(
            ArchiveCompressionPolicy.defaultOutputDisplayName(
                targetDisplayNames = request.targets.map(ArchiveCompressionTarget::displayName),
                parentDisplayPath = request.parentDisplayPath,
                fallbackStem = getString(R.string.text_default_archive_name),
                format = selectedFormat,
            ),
        )

        val formatLabels = creatableFormats.map(::formatLabel)
        format.setAdapter(
            ArrayAdapter(
                this@CreateArchiveActivity,
                android.R.layout.simple_list_item_1,
                formatLabels,
            ),
        )
        format.setText(formatLabels.first(), false)
        format.setOnClickListener { format.showDropDown() }
        compressionLevel.setOnClickListener { compressionLevel.showDropDown() }

        val conflictPolicies = ArchiveCreationConflictPolicy.entries
        creationConflictPolicy.setAdapter(
            ArrayAdapter(
                this@CreateArchiveActivity,
                android.R.layout.simple_list_item_1,
                conflictPolicies.map(::conflictPolicyLabel),
            ),
        )
        selectConflictPolicy(selectedConflictPolicy)
        creationConflictPolicy.setOnClickListener { creationConflictPolicy.showDropDown() }
        creationConflictPolicy.setOnItemClickListener { _, _, position, _ ->
            selectConflictPolicy(conflictPolicies[position])
        }

        val levels = listOf(
            CompressionLevelChoice(R.string.text_compression_level_none, 0),
            CompressionLevelChoice(R.string.text_compression_level_fast, 1),
            CompressionLevelChoice(R.string.text_compression_level_normal, 6),
            CompressionLevelChoice(R.string.text_compression_level_maximum, 9),
        )
        setupSplitVolumeInput()
        configureFormat(selectedFormat, levels)
        format.setOnItemClickListener { _, _, position, _ ->
            val previousFormat = selectedFormat
            selectedFormat = creatableFormats[position]
            ArchiveCompressionPolicy.outputDisplayNameForFormat(
                value = outputName.text?.toString(),
                previousFormat = previousFormat,
                nextFormat = selectedFormat,
            )?.let(outputName::setText)
            configureFormat(selectedFormat, levels)
            renderCreationModeControls()
        }

        createSeparateArchives = createSeparateArchives && request.targets.size > 1
        separateArchives.isChecked = createSeparateArchives
        separateArchives.setOnCheckedChangeListener { _, checked ->
            createSeparateArchives = checked && request.targets.size > 1
            renderCreationModeControls()
        }
        moveSourcesToTrash.isChecked = this@CreateArchiveActivity.moveSourcesToTrash
        moveSourcesToTrash.setOnCheckedChangeListener { _, checked ->
            this@CreateArchiveActivity.moveSourcesToTrash = checked
        }
        renderCreationModeControls()

        createButton.setOnClickListener {
            if (completedArchiveMessage == null) {
                createArchive()
            } else {
                activeTargetTrashBatch?.takeIf { batch -> batch.canUndo }?.let {
                    restoreTargetTrashBatch(it)
                }
            }
        }
        cancelButton.setOnClickListener {
            if (completedArchiveMessage != null && operationJob?.isActive != true) {
                finish()
            } else if (operationJob?.isActive == true && operationCancellable) {
                operationJob?.cancel()
            } else if (operationJob?.isActive != true) {
                finish()
            }
        }
        outputName.setOnEditorActionListener { _, _, _ ->
            password.requestFocus()
            true
        }
        password.setOnEditorActionListener { _, _, _ ->
            passwordConfirmation.requestFocus()
            true
        }
        password.doAfterTextChanged { passwordConfirmationLayout.error = null }
        passwordConfirmation.doAfterTextChanged { passwordConfirmationLayout.error = null }
        passwordConfirmation.setOnEditorActionListener { _, _, _ ->
            createArchive()
            true
        }

        onBackPressedDispatcher.addCallback(
            this@CreateArchiveActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBack()
            },
        )
    }

    private fun handleBack() {
        if (completedArchiveMessage != null && operationJob?.isActive != true) {
            finish()
        } else if (operationJob?.isActive == true && operationCancellable) {
            operationJob?.cancel()
        } else if (operationJob?.isActive != true) {
            finish()
        }
    }

    private fun createArchive(
        conflictPolicy: ArchiveCreationConflictPolicy = selectedConflictPolicy,
    ) {
        if (terminalFailureMessage != null) return
        val resolvedRequest = request ?: return
        if (operationJob?.isActive == true) return
        val creationFormat = selectedFormat
        val separateArchives = createSeparateArchives && resolvedRequest.targets.size > 1
        val trashSourcesAfterCreation = moveSourcesToTrash
        val splitSelection = resolveSplitVolumeSelection(creationFormat)
        if (!splitSelection.valid) {
            binding.splitVolumeLayout.error = getString(
                R.string.error_split_volume_invalid,
                ArchiveSplitVolumePolicy.MIN_SIZE_MIB,
                ArchiveSplitVolumePolicy.MAX_SIZE_MIB,
            )
            binding.splitVolume.requestFocus()
            return
        }
        val splitVolumeSizeBytes = splitSelection.sizeBytes
        val outputDisplayName = if (separateArchives) {
            ArchiveCreationPlanner.separateOutputDisplayNames(
                targetDisplayNames = resolvedRequest.targets.map(ArchiveCompressionTarget::displayName),
                parentDisplayPath = resolvedRequest.parentDisplayPath,
                fallbackStem = getString(R.string.text_default_archive_name),
                format = creationFormat,
                splitVolumeSizeBytes = splitVolumeSizeBytes,
            ).first()
        } else {
            ArchiveCompressionPolicy.normalizeCreationOutputDisplayName(
                value = binding.outputName.text?.toString(),
                format = creationFormat,
                splitVolumeSizeBytes = splitVolumeSizeBytes,
            )
        }
        if (outputDisplayName == null) {
            binding.outputNameLayout.error = getString(R.string.error_archive_file_name_invalid)
            binding.outputName.requestFocus()
            return
        }
        binding.outputNameLayout.error = null
        if (!separateArchives) binding.outputName.setText(outputDisplayName)
        val password = binding.password.text?.let { editable ->
            CharArray(editable.length) { index -> editable[index] }
        } ?: CharArray(0)
        val passwordConfirmation = binding.passwordConfirmation.text?.let { editable ->
            CharArray(editable.length) { index -> editable[index] }
        } ?: CharArray(0)
        val passwordAvailable = archiveEngine.capabilities(selectedFormat).password !=
            ArchiveOptionMode.UNSUPPORTED
        if (
            passwordAvailable &&
            !ArchiveCompressionPolicy.passwordConfirmationMatches(password, passwordConfirmation)
        ) {
            password.fill('\u0000')
            passwordConfirmation.fill('\u0000')
            binding.passwordConfirmationLayout.error = getString(
                R.string.error_password_confirmation_mismatch,
            )
            binding.passwordConfirmation.requestFocus()
            return
        }
        binding.passwordConfirmationLayout.error = null
        passwordConfirmation.fill('\u0000')
        val passwordForCreation = password.takeIf { passwordAvailable && it.isNotEmpty() }
        currentFocus?.let { focused ->
            getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(focused.windowToken, 0)
        }
        val plan = ArchiveCreationPlanner.plan(
            request = resolvedRequest,
            format = creationFormat,
            options = ArchiveCreationOptions(
                outputDisplayName = outputDisplayName,
                compressionLevel = compressionLevel,
                password = passwordForCreation,
                conflictPolicy = if (separateArchives) {
                    ArchiveCreationConflictPolicy.AUTO_RENAME
                } else {
                    conflictPolicy
                },
                splitVolumeSizeBytes = splitVolumeSizeBytes,
            ),
            separateArchives = separateArchives,
            fallbackStem = getString(R.string.text_default_archive_name),
        )
        setBusy(true, getString(R.string.text_preparing_compression))
        val attemptId = ++creationAttemptId

        operationJob = lifecycleScope.launch {
            var unavailableName: ArchiveOutputNameUnavailableException? = null
            var acceptCreationProgress = true
            try {
                val result = try {
                    withContext(Dispatchers.IO) {
                        val cancellationContext = currentCoroutineContext()
                        var lastUiUpdateNanos = 0L
                        var lastUiPhase: ArchiveCreationPhase? = null
                        ArchiveCreationBatchExecutor.execute(
                            writer = archiveEngine.createWriter(
                                creationFormat,
                                resolvedRequest.hostSession,
                                cacheDir,
                            ),
                            plan = plan,
                            checkCancelled = { cancellationContext.ensureActive() },
                            progress = ArchiveCreationBatchProgressListener { update ->
                                val now = System.nanoTime()
                                val phaseChanged = update.creation.phase != lastUiPhase
                                if (
                                    phaseChanged ||
                                    now - lastUiUpdateNanos >= UI_PROGRESS_INTERVAL_NANOS
                                ) {
                                    lastUiUpdateNanos = now
                                    lastUiPhase = update.creation.phase
                                    runOnUiThread {
                                        if (
                                            acceptCreationProgress &&
                                            !isFinishing &&
                                            !isDestroyed &&
                                            attemptId == creationAttemptId &&
                                            operationJob?.isActive == true &&
                                            terminalFailureMessage == null
                                        ) {
                                            renderCreationBatchProgress(update)
                                        }
                                    }
                                }
                            },
                        )
                    }
                } finally {
                    acceptCreationProgress = false
                }
                val targetTrashResult = if (trashSourcesAfterCreation) {
                    setBusy(
                        busy = true,
                        message = getString(R.string.text_moving_sources_to_trash),
                        cancellable = false,
                    )
                    if (result.committedOutputTransactionIds.isEmpty()) {
                        HostTargetTrashResult(
                            state = ExplorerActionHostSessionValues.TARGET_TRASH_STATE_FAILED,
                            trashItemIds = emptyList(),
                            movedCount = 0,
                            recoveryCount = 0,
                        )
                    } else withContext(NonCancellable + Dispatchers.IO) {
                        try {
                            ExplorerActionHostSessionClient(resolvedRequest.hostSession)
                                .moveTargetsToTrash(
                                    targetIds = resolvedRequest.targets.map(ArchiveCompressionTarget::id),
                                    outputTransactionIds = result.committedOutputTransactionIds,
                                )
                        } catch (_: Exception) {
                            HostTargetTrashResult(
                                state = ExplorerActionHostSessionValues.TARGET_TRASH_STATE_UNKNOWN,
                                trashItemIds = emptyList(),
                                movedCount = 0,
                                recoveryCount = 0,
                            )
                        }
                    }
                } else {
                    null
                }
                val archiveMessage = if (plan.separateArchives) {
                    if (
                        splitVolumeSizeBytes == null ||
                        result.physicalOutputsCreated == result.outputs.size.toLong()
                    ) {
                        getString(
                            R.string.text_separate_archives_created,
                            result.outputs.size,
                            result.filesCompressed,
                            result.directoriesAdded,
                        )
                    } else {
                        getString(
                            R.string.text_separate_split_archives_created,
                            result.outputs.size,
                            result.physicalOutputsCreated,
                            result.filesCompressed,
                        )
                    }
                } else {
                    val output = result.outputs.single()
                    if (splitVolumeSizeBytes == null || output.createdOutputs.size == 1) {
                        resources.getQuantityString(
                            R.plurals.text_archive_created,
                            output.filesCompressed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                            output.outputDisplayName,
                            output.filesCompressed,
                        )
                    } else {
                        getString(
                            R.string.text_split_archive_created,
                            output.outputDisplayName,
                            output.createdOutputs.size,
                            output.filesCompressed,
                        )
                    }
                }
                val message = appendTargetTrashResult(archiveMessage, targetTrashResult)
                val targetTrashBatchId = targetTrashResult?.batchId
                if (targetTrashBatchId == null) {
                    Toast.makeText(this@CreateArchiveActivity, message, Toast.LENGTH_LONG).show()
                    closeHostSession()
                    finish()
                } else {
                    completedArchiveMessage = message
                    completedRestoreMessage = null
                    activeTargetTrashBatchId = targetTrashBatchId
                    activeTargetTrashBatch = withContext(Dispatchers.IO) {
                        runCatching {
                            ExplorerActionHostSessionClient(resolvedRequest.hostSession)
                                .queryTargetTrashBatch(targetTrashBatchId)
                        }.getOrNull()
                    }
                    renderCreationCompleted()
                    refreshTargetTrashHistory()
                }
            } catch (error: ArchiveCreationPartialFailureException) {
                if (!isFinishing && !isDestroyed) renderPartialCreationFailure(error)
            } catch (error: ArchiveCreationPartialOutputException) {
                if (!isFinishing && !isDestroyed) renderPartialOutputFailure(error)
            } catch (error: ArchiveCreationOutputGroupRollbackException) {
                if (!isFinishing && !isDestroyed) renderOutputGroupCleanupFailure(error)
            } catch (error: ArchiveCreationCacheCleanupException) {
                if (!isFinishing && !isDestroyed) {
                    val message = getString(R.string.error_split_compression_cache_cleanup)
                    terminalFailureMessage = message
                    closeHostSession()
                    renderTerminalFailure(message)
                }
            } catch (_: CancellationException) {
                if (!isFinishing && !isDestroyed) {
                    setBusy(false, getString(R.string.text_cancelled))
                }
            } catch (error: ArchiveOutputNameUnavailableException) {
                if (!isFinishing && !isDestroyed) {
                    unavailableName = error
                    setBusy(false, getString(R.string.dialog_title_archive_name_unavailable))
                }
            } catch (error: ArchiveCreationRollbackException) {
                if (!isFinishing && !isDestroyed) {
                    val message = getString(
                        R.string.error_compression_cleanup_unconfirmed,
                        ArchivePathPolicy.unsafeSourceNameForDisplay(
                            error.pendingOutputDisplayPath,
                        ),
                        userFacingReason(error.operationFailure),
                    )
                    terminalFailureMessage = message
                    closeHostSession()
                    renderTerminalFailure(message)
                }
            } catch (error: Throwable) {
                if (!isFinishing && !isDestroyed) {
                    setBusy(
                        false,
                        getString(
                            R.string.error_with_reason,
                            getString(R.string.error_compression_failed),
                            userFacingReason(error),
                        ),
                    )
                }
            } finally {
                password.fill('\u0000')
                if (attemptId == creationAttemptId) {
                    operationJob = null
                }
            }
            unavailableName?.let(::showUnavailableNameDialog)
        }
    }

    internal fun renderCreationProgress(update: ArchiveCreationProgress) {
        binding.status.text = creationProgressText(update)
    }

    internal fun renderCreationBatchProgress(update: ArchiveCreationBatchProgress) {
        val progressText = creationProgressText(update.creation)
        binding.status.text = if (update.totalArchives == 1) {
            progressText
        } else {
            getString(
                R.string.text_separate_archive_progress,
                update.archiveIndex,
                update.totalArchives,
                ArchivePathPolicy.unsafeSourceNameForDisplay(
                    update.requestedOutputDisplayName,
                ),
                progressText,
            )
        }
    }

    private fun creationProgressText(update: ArchiveCreationProgress): String =
        when (update.phase) {
            ArchiveCreationPhase.SCANNING -> renderScanningProgress(update)
            ArchiveCreationPhase.COMPRESSING -> renderCompressingProgress(update)
            ArchiveCreationPhase.VERIFYING -> getString(R.string.text_verifying_archive)
            ArchiveCreationPhase.COMMITTING -> getString(R.string.text_committing_archive)
        }

    private fun renderScanningProgress(update: ArchiveCreationProgress): String {
        val currentEntry = update.currentEntry ?: return getString(
            R.string.text_scanning_compression_sources,
        )
        val filesForRules = update.completedFiles.toQuantityRuleInt()
        return resources.getQuantityString(
            R.plurals.text_scanning_compression_progress,
            filesForRules,
            ArchivePathPolicy.unsafeSourceNameForDisplay(currentEntry),
            update.completedFiles,
            update.completedDirectories,
        )
    }

    private fun renderCompressingProgress(update: ArchiveCreationProgress): String {
        val currentEntry = update.currentEntry ?: return getString(
            R.string.text_preparing_compression,
        )
        val safeCurrentEntry = ArchivePathPolicy.unsafeSourceNameForDisplay(currentEntry)
        val bytesRead = Formatter.formatShortFileSize(this, update.sourceBytesRead)
        return if (update.unknownSizeFiles == 0L) {
            resources.getQuantityString(
                R.plurals.text_compressing_progress,
                update.totalFiles.toQuantityRuleInt(),
                safeCurrentEntry,
                update.completedFiles,
                update.totalFiles,
                bytesRead,
                Formatter.formatShortFileSize(this, update.knownSourceBytes),
            )
        } else {
            resources.getQuantityString(
                R.plurals.text_compressing_progress_unknown_sizes,
                update.unknownSizeFiles.toQuantityRuleInt(),
                safeCurrentEntry,
                update.completedFiles,
                update.totalFiles,
                bytesRead,
                update.unknownSizeFiles,
            )
        }
    }

    private fun Long.toQuantityRuleInt(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    internal fun renderPartialCreationFailure(error: ArchiveCreationPartialFailureException) {
        val operationFailure = error.operationFailure
        val message = when (operationFailure) {
            is CancellationException -> getString(
                R.string.error_separate_compression_partially_cancelled,
                error.completedOutputs.size,
                error.totalOutputs,
            )
            is ArchiveCreationPartialOutputException -> buildString {
                append(
                    getString(
                        R.string.error_separate_compression_completed_before_split_failure,
                        error.completedOutputs.size,
                        error.totalOutputs,
                    ),
                )
                append("\n\n")
                append(partialOutputFailureMessage(operationFailure))
            }
            is ArchiveCreationOutputGroupRollbackException -> buildString {
                append(
                    getString(
                        R.string.error_separate_compression_completed_before_split_failure,
                        error.completedOutputs.size,
                        error.totalOutputs,
                    ),
                )
                append("\n\n")
                append(outputGroupCleanupFailureMessage(operationFailure))
            }
            is ArchiveCreationRollbackException -> getString(
                R.string.error_separate_compression_cleanup_unconfirmed,
                error.completedOutputs.size,
                error.totalOutputs,
                ArchivePathPolicy.unsafeSourceNameForDisplay(
                    operationFailure.pendingOutputDisplayPath,
                ),
                userFacingReason(operationFailure.operationFailure),
            )
            else -> {
                val failedName = if (operationFailure is ArchiveCreationOutputException) {
                    operationFailure.outputDisplayName
                } else {
                    error.failedRequestedOutputDisplayName
                }
                getString(
                    R.string.error_separate_compression_partial_failure,
                    error.completedOutputs.size,
                    error.totalOutputs,
                    ArchivePathPolicy.unsafeSourceNameForDisplay(failedName),
                    userFacingReason(operationFailure),
                )
            }
        }
        terminalFailureMessage = message
        closeHostSession()
        renderTerminalFailure(message)
    }

    internal fun renderPartialOutputFailure(error: ArchiveCreationPartialOutputException) {
        val message = partialOutputFailureMessage(error)
        terminalFailureMessage = message
        closeHostSession()
        renderTerminalFailure(message)
    }

    private fun partialOutputFailureMessage(error: ArchiveCreationPartialOutputException): String =
        getString(
            R.string.error_split_compression_partial_failure,
            error.committedOutputs.size,
            error.totalOutputs,
            ArchivePathPolicy.unsafeSourceNameForDisplay(error.failedOutputDisplayName),
            userFacingReason(error.operationFailure),
        )

    internal fun renderOutputGroupCleanupFailure(
        error: ArchiveCreationOutputGroupRollbackException,
    ) {
        val message = outputGroupCleanupFailureMessage(error)
        terminalFailureMessage = message
        closeHostSession()
        renderTerminalFailure(message)
    }

    private fun outputGroupCleanupFailureMessage(
        error: ArchiveCreationOutputGroupRollbackException,
    ): String = getString(
            R.string.error_split_compression_cleanup_unconfirmed,
            error.committedOutputs.size,
            error.totalOutputs,
            error.residualOutputs.size,
            ArchivePathPolicy.unsafeSourceNameForDisplay(error.residualOutputs.first().displayPath),
            userFacingReason(error.operationFailure),
        )

    private fun selectConflictPolicy(policy: ArchiveCreationConflictPolicy) {
        selectedConflictPolicy = policy
        if (!createSeparateArchives) renderSelectedConflictPolicy()
    }

    private fun setupSplitVolumeInput() = with(binding) {
        splitVolumeChoices = buildList {
            add(SplitVolumeChoice(getString(R.string.text_none), null))
            ArchiveSplitVolumePolicy.presetSizesMiB.forEach { sizeMiB ->
                add(
                    SplitVolumeChoice(
                        label = getString(R.string.text_split_volume_mib, sizeMiB),
                        sizeMiB = sizeMiB,
                    ),
                )
            }
        }
        splitVolume.setAdapter(
            ArrayAdapter(
                this@CreateArchiveActivity,
                android.R.layout.simple_list_item_1,
                splitVolumeChoices.map(SplitVolumeChoice::label),
            ),
        )
        splitVolume.setOnClickListener {
            if (splitVolume.isEnabled) splitVolume.showDropDown()
        }
        splitVolume.setOnItemClickListener { _, _, position, _ ->
            selectedSplitVolumeSizeMiB = splitVolumeChoices[position].sizeMiB
            splitVolumeLayout.error = null
            fitOutputNameForSplitSelection()
            renderCreationModeControls()
        }
        splitVolume.doAfterTextChanged { editable ->
            if (renderingSplitVolume) return@doAfterTextChanged
            splitVolumeLayout.error = null
            val parsed = parseSplitVolumeSelection(editable?.toString().orEmpty())
            if (parsed.valid) {
                selectedSplitVolumeSizeMiB = parsed.sizeMiB
                fitOutputNameForSplitSelection()
                renderCreationModeControls()
            }
        }
    }

    private fun resolveSplitVolumeSelection(format: ArchiveFormat): SplitVolumeSelection {
        if (archiveEngine.capabilities(format).splitVolumes == ArchiveOptionMode.UNSUPPORTED) {
            return SplitVolumeSelection(valid = true, sizeBytes = null)
        }
        val parsed = parseSplitVolumeSelection(binding.splitVolume.text?.toString().orEmpty())
        if (!parsed.valid) return SplitVolumeSelection(valid = false, sizeBytes = null)
        selectedSplitVolumeSizeMiB = parsed.sizeMiB
        return SplitVolumeSelection(
            valid = true,
            sizeBytes = parsed.sizeMiB?.let(ArchiveSplitVolumePolicy::bytesFromMiB),
        )
    }

    private fun parseSplitVolumeSelection(value: String): ParsedSplitVolumeSelection {
        val normalized = value.trim()
        splitVolumeChoices.firstOrNull { choice -> choice.label == normalized }?.let { choice ->
            return ParsedSplitVolumeSelection(valid = true, sizeMiB = choice.sizeMiB)
        }
        val customSizeMiB = ArchiveSplitVolumePolicy.parseCustomSizeMiB(normalized)
        return ParsedSplitVolumeSelection(
            valid = customSizeMiB != null,
            sizeMiB = customSizeMiB,
        )
    }

    private fun renderSplitVolumeControl(
        format: ArchiveFormat,
        busy: Boolean = operationJob?.isActive == true,
    ) = with(binding) {
        val available = archiveEngine.capabilities(format).splitVolumes !=
            ArchiveOptionMode.UNSUPPORTED
        splitVolumeLayout.helperText = if (available) {
            getString(
                R.string.text_split_volume_helper,
                ArchiveSplitVolumePolicy.MIN_SIZE_MIB,
                ArchiveSplitVolumePolicy.MAX_SIZE_MIB,
            )
        } else {
            getString(R.string.text_split_unavailable_for_format, formatLabel(format))
        }
        splitVolumeLayout.error = null
        splitVolumeLayout.isEnabled = available && !busy
        splitVolume.isEnabled = available && !busy

        val displayedValue = if (!available || selectedSplitVolumeSizeMiB == null) {
            splitVolumeChoices.first().label
        } else {
            val selectedSizeMiB = requireNotNull(selectedSplitVolumeSizeMiB)
            splitVolumeChoices.firstOrNull { choice -> choice.sizeMiB == selectedSizeMiB }
                ?.label
                ?: selectedSizeMiB.toString()
        }
        renderingSplitVolume = true
        try {
            splitVolume.setText(displayedValue, false)
        } finally {
            renderingSplitVolume = false
        }
        if (available) fitOutputNameForSplitSelection()
    }

    private fun fitOutputNameForSplitSelection() {
        if (
            selectedFormat != ArchiveFormat.ZIP ||
            selectedSplitVolumeSizeMiB == null ||
            !::binding.isInitialized
        ) {
            return
        }
        ArchiveCompressionPolicy.fitSplitZipOutputDisplayName(
            binding.outputName.text?.toString(),
        )?.let { fittedName ->
            if (fittedName != binding.outputName.text?.toString()) {
                binding.outputName.setText(fittedName)
            }
        }
    }

    private fun renderSelectedConflictPolicy() = with(binding) {
        val policy = if (createSeparateArchives) {
            ArchiveCreationConflictPolicy.AUTO_RENAME
        } else {
            selectedConflictPolicy
        }
        creationConflictPolicy.setText(conflictPolicyLabel(policy), false)
        creationConflictPolicyDetails.setText(
            if (createSeparateArchives) {
                R.string.text_separate_archive_conflict_note
            } else {
                when (policy) {
                    ArchiveCreationConflictPolicy.AUTO_RENAME ->
                        R.string.text_creation_conflict_policy_auto_rename_details
                    ArchiveCreationConflictPolicy.ASK ->
                        R.string.text_creation_conflict_policy_ask_details
                }
            },
        )
    }

    private fun renderCreationModeControls(
        busy: Boolean = operationJob?.isActive == true,
    ) = with(binding) {
        val resolvedRequest = request
        val supportsSeparateArchives = (resolvedRequest?.targets?.size ?: 0) > 1
        if (!supportsSeparateArchives && createSeparateArchives) {
            createSeparateArchives = false
            separateArchives.isChecked = false
        }
        separateArchives.isEnabled = !busy && supportsSeparateArchives
        moveSourcesToTrash.isEnabled = !busy
        outputNameLayout.isVisible = !createSeparateArchives
        outputNameLayout.isEnabled = !busy && !createSeparateArchives
        creationConflictPolicyLayout.isEnabled = !busy && !createSeparateArchives
        creationConflictPolicy.isEnabled = !busy && !createSeparateArchives
        separateArchivesPreview.isVisible = createSeparateArchives
        if (createSeparateArchives && resolvedRequest != null) {
            val names = ArchiveCreationPlanner.separateOutputDisplayNames(
                targetDisplayNames = resolvedRequest.targets.map(
                    ArchiveCompressionTarget::displayName,
                ),
                parentDisplayPath = resolvedRequest.parentDisplayPath,
                fallbackStem = getString(R.string.text_default_archive_name),
                format = selectedFormat,
                splitVolumeSizeBytes = selectedSplitVolumeSizeMiB
                    ?.takeIf { archiveEngine.capabilities(selectedFormat).splitVolumes !=
                        ArchiveOptionMode.UNSUPPORTED }
                    ?.let(ArchiveSplitVolumePolicy::bytesFromMiB),
            )
            val shownNames = names.take(MAX_SEPARATE_ARCHIVE_PREVIEW_NAMES)
            separateArchivesPreview.text = buildList {
                add(getString(R.string.text_separate_archive_preview_count, names.size))
                shownNames.forEach { name ->
                    add("- ${ArchivePathPolicy.unsafeSourceNameForDisplay(name)}")
                }
                val remaining = names.size - shownNames.size
                if (remaining > 0) {
                    add(getString(R.string.text_separate_archive_preview_more, remaining))
                }
                add(getString(R.string.text_separate_archive_conflict_note))
            }.joinToString("\n")
        }
        renderSelectedConflictPolicy()
    }

    private fun conflictPolicyLabel(policy: ArchiveCreationConflictPolicy): String = getString(
        when (policy) {
            ArchiveCreationConflictPolicy.AUTO_RENAME -> R.string.text_conflict_policy_auto_rename
            ArchiveCreationConflictPolicy.ASK -> R.string.text_conflict_policy_ask
        },
    )

    private fun showUnavailableNameDialog(error: ArchiveOutputNameUnavailableException) {
        if (isFinishing || isDestroyed) return
        val dialog = createUnavailableNameDialog(error)
        unavailableNameDialog = dialog
        dialog.setOnDismissListener {
            if (unavailableNameDialog === dialog) unavailableNameDialog = null
        }
        dialog.show()
    }

    internal fun createUnavailableNameDialog(
        error: ArchiveOutputNameUnavailableException,
    ): androidx.appcompat.app.AlertDialog {
        val safeName = ArchivePathPolicy.unsafeSourceNameForDisplay(error.requestedDisplayName)
        val safePath = ArchivePathPolicy.unsafeSourceNameForDisplay(
            request?.parentDisplayPath.orEmpty(),
        )
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_archive_name_unavailable)
            .setMessage(
                getString(
                    R.string.dialog_message_archive_name_unavailable,
                    safeName,
                    safePath,
                ),
            )
            .setNegativeButton(R.string.dialog_button_edit_name) { _, _ ->
                binding.outputName.requestFocus()
                binding.outputName.setSelection(binding.outputName.text?.length ?: 0)
            }
            .setNeutralButton(R.string.dialog_button_cancel, null)
            .setPositiveButton(R.string.dialog_button_use_numbered_name) { _, _ ->
                createArchive(ArchiveCreationConflictPolicy.AUTO_RENAME)
            }
            .create()
    }

    internal fun currentUnavailableNameDialog(): androidx.appcompat.app.AlertDialog? =
        unavailableNameDialog

    private fun configureFormat(
        format: ArchiveFormat,
        choices: List<CompressionLevelChoice>,
    ) = with(binding) {
        val capabilities = archiveEngine.capabilities(format)
        val supportedChoices = choices.filter { it.level in capabilities.compressionLevels }
        check(supportedChoices.isNotEmpty()) { "${format.displayName} has no compression levels" }
        val levelLabels = supportedChoices.map { getString(it.labelResource) }
        compressionLevel.setAdapter(
            ArrayAdapter(
                this@CreateArchiveActivity,
                android.R.layout.simple_list_item_1,
                levelLabels,
            ),
        )
        val selectedIndex = supportedChoices.indexOfFirst {
            it.level == this@CreateArchiveActivity.compressionLevel
        }.takeIf { it >= 0 } ?: supportedChoices.indexOfFirst {
            it.level == ArchiveCompressionPolicy.DEFAULT_COMPRESSION_LEVEL
        }.coerceAtLeast(0)
        this@CreateArchiveActivity.compressionLevel = supportedChoices[selectedIndex].level
        compressionLevel.setText(levelLabels[selectedIndex], false)
        compressionLevel.isEnabled = supportedChoices.size > 1
        compressionLevel.setOnItemClickListener { _, _, position, _ ->
            this@CreateArchiveActivity.compressionLevel = supportedChoices[position].level
        }

        val passwordAvailable = capabilities.password != ArchiveOptionMode.UNSUPPORTED
        if (!passwordAvailable) {
            password.text?.clear()
            passwordConfirmation.text?.clear()
            passwordConfirmationLayout.error = null
        }
        passwordLayout.helperText = if (passwordAvailable) {
            getString(R.string.text_password_encryption_note)
        } else {
            getString(R.string.text_encryption_unavailable_for_format, formatLabel(format))
        }
        passwordLayout.isEnabled = passwordAvailable
        password.isEnabled = passwordAvailable
        passwordConfirmationLayout.isEnabled = passwordAvailable
        passwordConfirmation.isEnabled = passwordAvailable
        encryptFileNames.isChecked = capabilities.filenameEncryption == ArchiveOptionMode.REQUIRED
        encryptFileNames.isEnabled = capabilities.filenameEncryption == ArchiveOptionMode.OPTIONAL
        renderSplitVolumeControl(format)
    }

    private fun formatLabel(format: ArchiveFormat): String = when (format) {
        ArchiveFormat.ZIP -> getString(R.string.text_format_zip)
        ArchiveFormat.SEVEN_Z,
        ArchiveFormat.RAR,
        ArchiveFormat.TAR,
        ArchiveFormat.TAR_GZIP,
        ArchiveFormat.TAR_XZ,
        ArchiveFormat.TAR_BZIP2,
        ArchiveFormat.TAR_ZSTD,
        -> format.displayName
    }

    private fun loadActiveTargetTrashBatch() {
        val batchId = activeTargetTrashBatchId ?: return
        val resolvedRequest = request ?: return
        lifecycleScope.launch {
            val batch = withContext(Dispatchers.IO) {
                runCatching {
                    ExplorerActionHostSessionClient(resolvedRequest.hostSession)
                        .queryTargetTrashBatch(batchId)
                }.getOrNull()
            }
            if (
                !sessionClosed &&
                activeTargetTrashBatchId == batchId &&
                completedArchiveMessage != null
            ) {
                activeTargetTrashBatch = batch
                renderCreationCompleted()
            }
        }
    }

    private fun refreshTargetTrashHistory() {
        if (sessionClosed) return
        val resolvedRequest = request ?: return
        val generation = ++targetTrashHistoryRefreshGeneration
        lifecycleScope.launch {
            val batches = withContext(Dispatchers.IO) {
                runCatching {
                    ExplorerActionHostSessionClient(resolvedRequest.hostSession)
                        .listTargetTrashBatches()
                }.getOrDefault(emptyList())
            }
            if (sessionClosed || generation != targetTrashHistoryRefreshGeneration) return@launch
            targetTrashBatches = batches
            if (completedArchiveMessage != null) {
                activeTargetTrashBatchId?.let { batchId ->
                    batches.firstOrNull { batch -> batch.id == batchId }?.let { batch ->
                        activeTargetTrashBatch = batch
                        renderCreationCompleted()
                    }
                }
            }
            targetTrashHistoryMenuItem?.apply {
                isVisible = batches.isNotEmpty()
                isEnabled = operationJob?.isActive != true
            }
        }
    }

    private fun showTargetTrashHistory() {
        val batches = targetTrashBatches
        if (batches.isEmpty() || isFinishing || isDestroyed) return
        val labels = batches.map(::targetTrashHistoryLabel).toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_source_recovery_history)
            .setItems(labels) { dialog, index ->
                dialog.dismiss()
                showTargetTrashBatchDetails(batches[index])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun targetTrashHistoryLabel(batch: HostTargetTrashBatch): String {
        val names = buildString {
            append(batch.items.take(MAX_HISTORY_PREVIEW_NAMES).joinToString(", ") { item ->
                ArchivePathPolicy.unsafeSourceNameForDisplay(item.displayName)
            })
            val remaining = batch.items.size - MAX_HISTORY_PREVIEW_NAMES
            if (remaining > 0) {
                append(" ")
                append(
                    resources.getQuantityString(
                        R.plurals.text_more_source_items,
                        remaining,
                        remaining,
                    ),
                )
            }
        }
        return getString(
            R.string.text_source_recovery_history_item,
            names,
            targetTrashBatchStateLabel(batch.state),
            formatTargetTrashHistoryDate(batch.createdAt),
        )
    }

    private fun showTargetTrashBatchDetails(batch: HostTargetTrashBatch) {
        val displayedItems = batch.items.take(MAX_HISTORY_DETAIL_NAMES)
            .joinToString("\n") { item ->
                "- ${ArchivePathPolicy.unsafeSourceNameForDisplay(item.displayName)}"
            }
        val remaining = batch.items.size - MAX_HISTORY_DETAIL_NAMES
        val itemList = if (remaining > 0) {
            "$displayedItems\n- ${
                resources.getQuantityString(
                    R.plurals.text_more_source_items,
                    remaining,
                    remaining,
                )
            }"
        } else {
            displayedItems
        }
        val message = getString(
            R.string.text_source_recovery_details,
            formatTargetTrashHistoryDate(batch.createdAt),
            targetTrashBatchStateLabel(batch.state),
            batch.movedCount,
            batch.restoredCount,
            batch.recoveryCount,
            itemList,
        )
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_source_recovery_details)
            .setMessage(message)
        if (batch.canUndo) {
            builder
                .setPositiveButton(R.string.action_restore_sources) { _, _ ->
                    restoreTargetTrashBatch(batch)
                }
                .setNegativeButton(android.R.string.cancel, null)
        } else {
            builder.setPositiveButton(android.R.string.ok, null)
        }
        builder.show()
    }

    private fun restoreTargetTrashBatch(batch: HostTargetTrashBatch) {
        if (!batch.canUndo || operationJob?.isActive == true || sessionClosed) return
        val resolvedRequest = request ?: return
        val activeCompletionBatch = completedArchiveMessage != null &&
            activeTargetTrashBatchId == batch.id
        setBusy(
            busy = true,
            message = getString(R.string.text_restoring_sources),
            cancellable = false,
        )
        operationJob = lifecycleScope.launch {
            try {
                val restored = withContext(NonCancellable + Dispatchers.IO) {
                    ExplorerActionHostSessionClient(resolvedRequest.hostSession)
                        .undoTargetTrashBatch(batch.id)
                }
                val resultMessage = targetTrashUndoResultMessage(restored)
                if (activeCompletionBatch) {
                    activeTargetTrashBatch = restored
                    completedRestoreMessage = resultMessage
                    renderCreationCompleted()
                } else if (completedArchiveMessage != null) {
                    renderCreationCompleted()
                    Toast.makeText(this@CreateArchiveActivity, resultMessage, Toast.LENGTH_LONG).show()
                } else {
                    setBusy(false, resultMessage)
                }
            } catch (error: Throwable) {
                val message = getString(
                    R.string.error_source_restore_failed,
                    userFacingReason(error),
                )
                if (activeCompletionBatch) {
                    completedRestoreMessage = message
                    renderCreationCompleted()
                    loadActiveTargetTrashBatch()
                } else if (completedArchiveMessage != null) {
                    renderCreationCompleted()
                    Toast.makeText(this@CreateArchiveActivity, message, Toast.LENGTH_LONG).show()
                } else {
                    setBusy(false, message)
                }
            } finally {
                operationJob = null
                refreshTargetTrashHistory()
            }
        }
    }

    private fun targetTrashUndoResultMessage(batch: HostTargetTrashBatch): String {
        val remaining = batch.items.count { item ->
            item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_MOVED ||
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_STALE ||
                item.state == ExplorerActionHostSessionValues.TARGET_TRASH_ITEM_STATE_RECOVERY_REQUIRED
        }
        return buildList {
            if (batch.restoredCount > 0) {
                add(
                    resources.getQuantityString(
                        R.plurals.text_sources_restored_from_trash,
                        batch.restoredCount,
                        batch.restoredCount,
                    ),
                )
            }
            if (remaining > 0) {
                add(
                    resources.getQuantityString(
                        R.plurals.warning_sources_not_restored,
                        remaining,
                        remaining,
                    ),
                )
            }
            if (isEmpty()) add(getString(R.string.text_source_restore_no_change))
        }.joinToString("\n")
    }

    private fun targetTrashBatchStateLabel(state: Int): String = getString(
        when (state) {
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_AVAILABLE ->
                R.string.text_source_recovery_state_available
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_PARTIAL ->
                R.string.text_source_recovery_state_partial
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_RESTORED ->
                R.string.text_source_recovery_state_restored
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_STALE ->
                R.string.text_source_recovery_state_stale
            ExplorerActionHostSessionValues.TARGET_TRASH_BATCH_STATE_RECOVERY_REQUIRED ->
                R.string.text_source_recovery_state_recovery_required
            else -> R.string.text_source_recovery_state_failed
        },
    )

    private fun formatTargetTrashHistoryDate(timestamp: Long): String {
        val date = Date(timestamp)
        return DateFormat.getMediumDateFormat(this).format(date) + " " +
            DateFormat.getTimeFormat(this).format(date)
    }

    private fun renderCreationCompleted() = with(binding) {
        progress.isVisible = false
        status.isVisible = true
        status.text = listOfNotNull(completedArchiveMessage, completedRestoreMessage)
            .joinToString("\n")
        password.text?.clear()
        passwordConfirmation.text?.clear()
        passwordConfirmationLayout.error = null
        outputNameLayout.isEnabled = false
        creationConflictPolicyLayout.isEnabled = false
        creationConflictPolicy.isEnabled = false
        format.isEnabled = false
        compressionLevel.isEnabled = false
        passwordLayout.isEnabled = false
        password.isEnabled = false
        passwordConfirmationLayout.isEnabled = false
        passwordConfirmation.isEnabled = false
        encryptFileNames.isEnabled = false
        splitVolumeLayout.isEnabled = false
        splitVolume.isEnabled = false
        separateArchives.isEnabled = false
        moveSourcesToTrash.isEnabled = false
        createButton.isVisible = activeTargetTrashBatch?.canUndo == true
        createButton.isEnabled = activeTargetTrashBatch?.canUndo == true
        createButton.setText(
            if (completedRestoreMessage == null) {
                R.string.action_restore_sources
            } else {
                R.string.action_retry_source_restore
            },
        )
        cancelButton.isEnabled = true
        cancelButton.setText(R.string.dialog_button_done)
        targetTrashHistoryMenuItem?.isEnabled = true
    }

    private fun setBusy(
        busy: Boolean,
        message: String,
        cancellable: Boolean = true,
    ) = with(binding) {
        operationCancellable = !busy || cancellable
        progress.isVisible = busy
        status.isVisible = true
        status.text = message
        format.isEnabled = !busy
        val capabilities = archiveEngine.capabilities(selectedFormat)
        compressionLevel.isEnabled = !busy && capabilities.compressionLevels.size > 1
        val passwordAvailable = capabilities.password != ArchiveOptionMode.UNSUPPORTED
        passwordLayout.isEnabled = !busy && passwordAvailable
        password.isEnabled = !busy && passwordAvailable
        passwordConfirmationLayout.isEnabled = !busy && passwordAvailable
        passwordConfirmation.isEnabled = !busy && passwordAvailable
        encryptFileNames.isEnabled = !busy &&
            capabilities.filenameEncryption == ArchiveOptionMode.OPTIONAL
        renderSplitVolumeControl(selectedFormat, busy)
        renderCreationModeControls(busy)
        createButton.isEnabled = !busy
        cancelButton.isEnabled = !busy || cancellable
        cancelButton.text = getString(R.string.dialog_button_cancel)
        targetTrashHistoryMenuItem?.isEnabled = !busy
    }

    private fun appendTargetTrashResult(
        archiveMessage: String,
        result: HostTargetTrashResult?,
    ): String {
        if (result == null) return archiveMessage
        val detail = when (result.state) {
            ExplorerActionHostSessionValues.TARGET_TRASH_STATE_COMMITTED -> getString(
                R.string.text_sources_moved_to_trash,
                result.movedCount,
            )
            ExplorerActionHostSessionValues.TARGET_TRASH_STATE_RECOVERY_REQUIRED ->
                getString(R.string.warning_sources_trash_recovery_required)
            ExplorerActionHostSessionValues.TARGET_TRASH_STATE_FAILED ->
                getString(R.string.warning_sources_trash_failed)
            else -> getString(R.string.warning_sources_trash_unknown)
        }
        return "$archiveMessage\n$detail"
    }

    private fun renderTerminalFailure(message: String) = with(binding) {
        setBusy(false, message)
        password.text?.clear()
        passwordConfirmation.text?.clear()
        passwordConfirmationLayout.error = null
        outputNameLayout.isEnabled = false
        creationConflictPolicyLayout.isEnabled = false
        creationConflictPolicy.isEnabled = false
        format.isEnabled = false
        compressionLevel.isEnabled = false
        passwordLayout.isEnabled = false
        password.isEnabled = false
        passwordConfirmationLayout.isEnabled = false
        passwordConfirmation.isEnabled = false
        encryptFileNames.isEnabled = false
        splitVolumeLayout.isEnabled = false
        splitVolume.isEnabled = false
        separateArchives.isEnabled = false
        moveSourcesToTrash.isEnabled = false
        createButton.isEnabled = false
        cancelButton.text = getString(android.R.string.ok)
    }

    private fun userFacingReason(error: Throwable): String {
        val root = generateSequence(error) { it.cause }.last()
        return root.message
            ?.replace(Regex("[\\p{Cc}\\p{Cf}]+"), " ")
            ?.trim()
            ?.take(MAX_ERROR_REASON_LENGTH)
            ?.takeIf(String::isNotBlank)
            ?: root.javaClass.simpleName
    }

    private fun closeHostSession() {
        if (sessionClosed) return
        sessionClosed = true
        runCatching { request?.hostSession?.close() }
    }

    private data class CompressionLevelChoice(
        val labelResource: Int,
        val level: Int,
    )

    private companion object {
        val UI_PROGRESS_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100)
        const val MAX_ERROR_REASON_LENGTH = 500
        const val MAX_SEPARATE_ARCHIVE_PREVIEW_NAMES = 5
        const val MAX_HISTORY_PREVIEW_NAMES = 2
        const val MAX_HISTORY_DETAIL_NAMES = 12
        const val MENU_TARGET_TRASH_HISTORY = 21_001
        const val STATE_CONFLICT_POLICY = "creation_conflict_policy"
        const val STATE_SEPARATE_ARCHIVES = "separate_archives"
        const val STATE_MOVE_SOURCES_TO_TRASH = "move_sources_to_trash"
        const val STATE_SPLIT_VOLUME_SIZE_MIB = "split_volume_size_mib"
        const val STATE_TERMINAL_FAILURE = "terminal_creation_failure"
        const val STATE_COMPLETED_ARCHIVE_MESSAGE = "completed_archive_message"
        const val STATE_COMPLETED_RESTORE_MESSAGE = "completed_restore_message"
        const val STATE_TARGET_TRASH_BATCH_ID = "target_trash_batch_id"
    }

    private data class SplitVolumeChoice(
        val label: String,
        val sizeMiB: Long?,
    )

    private data class SplitVolumeSelection(
        val valid: Boolean,
        val sizeBytes: Long?,
    )

    private data class ParsedSplitVolumeSelection(
        val valid: Boolean,
        val sizeMiB: Long?,
    )
}
