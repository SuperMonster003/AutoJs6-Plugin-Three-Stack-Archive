package io.github.supermonster003.autojs6.plugin.archivemanager

import android.os.Bundle
import android.text.format.Formatter
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var unavailableNameDialog: androidx.appcompat.app.AlertDialog? = null
    private var sessionClosed = false
    private var terminalFailureMessage: String? = null

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
        terminalFailureMessage = savedInstanceState?.getString(STATE_TERMINAL_FAILURE)

        val resolvedRequest = ArchiveCompressionIntentPolicy.resolve(intent)
        if (resolvedRequest == null) {
            Toast.makeText(this, R.string.error_compression_request_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        request = resolvedRequest
        setupViews(resolvedRequest)
        terminalFailureMessage?.let { message ->
            closeHostSession()
            renderTerminalFailure(message)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CONFLICT_POLICY, selectedConflictPolicy.name)
        outState.putBoolean(STATE_SEPARATE_ARCHIVES, createSeparateArchives)
        terminalFailureMessage?.let { outState.putString(STATE_TERMINAL_FAILURE, it) }
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
        renderCreationModeControls()

        createButton.setOnClickListener { createArchive() }
        cancelButton.setOnClickListener {
            if (operationJob?.isActive == true) {
                operationJob?.cancel()
            } else {
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
        if (operationJob?.isActive == true) {
            operationJob?.cancel()
        } else {
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
        val outputDisplayName = if (separateArchives) {
            ArchiveCreationPlanner.separateOutputDisplayNames(
                targetDisplayNames = resolvedRequest.targets.map(ArchiveCompressionTarget::displayName),
                parentDisplayPath = resolvedRequest.parentDisplayPath,
                fallbackStem = getString(R.string.text_default_archive_name),
                format = creationFormat,
            ).first()
        } else {
            ArchiveCompressionPolicy.normalizeOutputDisplayName(
                binding.outputName.text?.toString(),
                creationFormat,
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
            ),
            separateArchives = separateArchives,
            fallbackStem = getString(R.string.text_default_archive_name),
        )
        setBusy(true, getString(R.string.text_preparing_compression))
        val attemptId = ++creationAttemptId

        operationJob = lifecycleScope.launch {
            var unavailableName: ArchiveOutputNameUnavailableException? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val cancellationContext = currentCoroutineContext()
                    var lastUiUpdateNanos = 0L
                    var lastUiPhase: ArchiveCreationPhase? = null
                    ArchiveCreationBatchExecutor.execute(
                        writer = archiveEngine.createWriter(
                            creationFormat,
                            resolvedRequest.hostSession,
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
                val message = if (plan.separateArchives) {
                    getString(
                        R.string.text_separate_archives_created,
                        result.outputs.size,
                        result.filesCompressed,
                        result.directoriesAdded,
                    )
                } else {
                    val output = result.outputs.single()
                    resources.getQuantityString(
                        R.plurals.text_archive_created,
                        output.filesCompressed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        output.outputDisplayName,
                        output.filesCompressed,
                    )
                }
                Toast.makeText(this@CreateArchiveActivity, message, Toast.LENGTH_LONG).show()
                closeHostSession()
                finish()
            } catch (error: ArchiveCreationPartialFailureException) {
                if (!isFinishing && !isDestroyed) renderPartialCreationFailure(error)
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

    private fun selectConflictPolicy(policy: ArchiveCreationConflictPolicy) {
        selectedConflictPolicy = policy
        if (!createSeparateArchives) renderSelectedConflictPolicy()
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
        val splitVolumesAvailable = capabilities.splitVolumes != ArchiveOptionMode.UNSUPPORTED
        splitVolumeLayout.helperText = if (splitVolumesAvailable) {
            null
        } else {
            getString(R.string.text_split_unavailable_for_format, formatLabel(format))
        }
        splitVolumeLayout.isEnabled = splitVolumesAvailable
        splitVolume.isEnabled = splitVolumesAvailable
    }

    private fun formatLabel(format: ArchiveFormat): String = when (format) {
        ArchiveFormat.ZIP -> getString(R.string.text_format_zip)
        ArchiveFormat.SEVEN_Z,
        ArchiveFormat.TAR,
        ArchiveFormat.TAR_GZIP,
        ArchiveFormat.TAR_XZ,
        ArchiveFormat.TAR_BZIP2,
        ArchiveFormat.TAR_ZSTD,
        -> format.displayName
    }

    private fun setBusy(busy: Boolean, message: String) = with(binding) {
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
        val splitVolumesAvailable = capabilities.splitVolumes != ArchiveOptionMode.UNSUPPORTED
        splitVolumeLayout.isEnabled = !busy && splitVolumesAvailable
        splitVolume.isEnabled = !busy && splitVolumesAvailable
        renderCreationModeControls(busy)
        createButton.isEnabled = !busy
        cancelButton.isEnabled = true
        cancelButton.text = getString(R.string.dialog_button_cancel)
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
        const val STATE_CONFLICT_POLICY = "creation_conflict_policy"
        const val STATE_SEPARATE_ARCHIVES = "separate_archives"
        const val STATE_TERMINAL_FAILURE = "terminal_creation_failure"
    }
}
