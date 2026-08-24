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
    private val archiveEngine = ArchiveEngine.DEFAULT
    private var selectedFormat = ArchiveFormat.ZIP
    private var compressionLevel = ArchiveCompressionPolicy.DEFAULT_COMPRESSION_LEVEL
    private var selectedConflictPolicy = ArchiveCreationConflictPolicy.AUTO_RENAME
    private var unavailableNameDialog: androidx.appcompat.app.AlertDialog? = null
    private var sessionClosed = false

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

        val resolvedRequest = ArchiveCompressionIntentPolicy.resolve(intent)
        if (resolvedRequest == null) {
            Toast.makeText(this, R.string.error_compression_request_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        request = resolvedRequest
        setupViews(resolvedRequest)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CONFLICT_POLICY, selectedConflictPolicy.name)
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
        }

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
        val resolvedRequest = request ?: return
        if (operationJob?.isActive == true) return
        val outputDisplayName = ArchiveCompressionPolicy.normalizeOutputDisplayName(
            binding.outputName.text?.toString(),
            selectedFormat,
        )
        if (outputDisplayName == null) {
            binding.outputNameLayout.error = getString(R.string.error_archive_file_name_invalid)
            binding.outputName.requestFocus()
            return
        }
        binding.outputNameLayout.error = null
        binding.outputName.setText(outputDisplayName)
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
        setBusy(true, getString(R.string.text_preparing_compression))

        operationJob = lifecycleScope.launch {
            var unavailableName: ArchiveOutputNameUnavailableException? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val cancellationContext = currentCoroutineContext()
                    var lastUiUpdateNanos = 0L
                    archiveEngine.createWriter(selectedFormat, resolvedRequest.hostSession).create(
                        request = resolvedRequest,
                        options = ArchiveCreationOptions(
                            outputDisplayName = outputDisplayName,
                            compressionLevel = compressionLevel,
                            password = passwordForCreation,
                            conflictPolicy = conflictPolicy,
                        ),
                        checkCancelled = { cancellationContext.ensureActive() },
                        progress = ArchiveCreationProgressListener { update ->
                            val now = System.nanoTime()
                            if (
                                now - lastUiUpdateNanos >= UI_PROGRESS_INTERVAL_NANOS ||
                                update.completedFiles == 0L
                            ) {
                                lastUiUpdateNanos = now
                                runOnUiThread {
                                    if (!isFinishing && !isDestroyed) {
                                        val completedFilesForRules = update.completedFiles
                                            .coerceAtMost(Int.MAX_VALUE.toLong())
                                            .toInt()
                                        binding.status.text = resources.getQuantityString(
                                            R.plurals.text_compressing_progress,
                                            completedFilesForRules,
                                            update.currentEntry,
                                            update.completedFiles,
                                            Formatter.formatShortFileSize(
                                                this@CreateArchiveActivity,
                                                update.sourceBytesRead,
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
                val message = resources.getQuantityString(
                    R.plurals.text_archive_created,
                    result.filesCompressed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    result.outputDisplayName,
                    result.filesCompressed,
                )
                Toast.makeText(this@CreateArchiveActivity, message, Toast.LENGTH_LONG).show()
                closeHostSession()
                finish()
            } catch (_: CancellationException) {
                if (!isFinishing && !isDestroyed) {
                    setBusy(false, getString(R.string.text_cancelled))
                }
            } catch (error: ArchiveOutputNameUnavailableException) {
                if (!isFinishing && !isDestroyed) {
                    unavailableName = error
                    setBusy(false, getString(R.string.dialog_title_archive_name_unavailable))
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
                operationJob = null
            }
            unavailableName?.let(::showUnavailableNameDialog)
        }
    }

    private fun selectConflictPolicy(policy: ArchiveCreationConflictPolicy) = with(binding) {
        selectedConflictPolicy = policy
        creationConflictPolicy.setText(conflictPolicyLabel(policy), false)
        creationConflictPolicyDetails.setText(
            when (policy) {
                ArchiveCreationConflictPolicy.AUTO_RENAME ->
                    R.string.text_creation_conflict_policy_auto_rename_details
                ArchiveCreationConflictPolicy.ASK ->
                    R.string.text_creation_conflict_policy_ask_details
            },
        )
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
        outputNameLayout.isEnabled = !busy
        creationConflictPolicyLayout.isEnabled = !busy
        creationConflictPolicy.isEnabled = !busy
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
        createButton.isEnabled = !busy
        cancelButton.isEnabled = true
        cancelButton.text = getString(R.string.dialog_button_cancel)
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
        const val STATE_CONFLICT_POLICY = "creation_conflict_policy"
    }
}
