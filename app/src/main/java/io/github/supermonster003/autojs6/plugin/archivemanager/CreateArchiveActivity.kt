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
import androidx.lifecycle.lifecycleScope
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
    private var compressionLevel = ArchiveCompressionPolicy.DEFAULT_COMPRESSION_LEVEL
    private var sessionClosed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateArchiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val resolvedRequest = ArchiveCompressionIntentPolicy.resolve(intent)
        if (resolvedRequest == null) {
            Toast.makeText(this, R.string.error_compression_request_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        request = resolvedRequest
        setupViews(resolvedRequest)
    }

    override fun onDestroy() {
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
        outputName.setText(
            ArchiveCompressionPolicy.defaultOutputDisplayName(
                targetDisplayNames = request.targets.map(ArchiveCompressionTarget::displayName),
                parentDisplayPath = request.parentDisplayPath,
                fallbackStem = getString(R.string.text_default_archive_name),
            ),
        )

        format.setAdapter(
            ArrayAdapter(
                this@CreateArchiveActivity,
                android.R.layout.simple_list_item_1,
                listOf(getString(R.string.text_format_zip)),
            ),
        )
        format.setText(getString(R.string.text_format_zip), false)

        val levels = listOf(
            CompressionLevelChoice(R.string.text_compression_level_none, 0),
            CompressionLevelChoice(R.string.text_compression_level_fast, 1),
            CompressionLevelChoice(R.string.text_compression_level_normal, 6),
            CompressionLevelChoice(R.string.text_compression_level_maximum, 9),
        )
        val levelLabels = levels.map { choice -> getString(choice.labelResource) }
        compressionLevel.setAdapter(
            ArrayAdapter(
                this@CreateArchiveActivity,
                android.R.layout.simple_list_item_1,
                levelLabels,
            ),
        )
        val defaultIndex = levels.indexOfFirst {
            it.level == ArchiveCompressionPolicy.DEFAULT_COMPRESSION_LEVEL
        }.coerceAtLeast(0)
        compressionLevel.setText(levelLabels[defaultIndex], false)
        compressionLevel.setOnItemClickListener { _, _, position, _ ->
            this@CreateArchiveActivity.compressionLevel = levels[position].level
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

    private fun createArchive() {
        val resolvedRequest = request ?: return
        if (operationJob?.isActive == true) return
        val outputDisplayName = ArchiveCompressionPolicy.normalizeOutputDisplayName(
            binding.outputName.text?.toString(),
        )
        if (outputDisplayName == null) {
            binding.outputNameLayout.error = getString(R.string.error_archive_file_name_invalid)
            binding.outputName.requestFocus()
            return
        }
        binding.outputNameLayout.error = null
        binding.outputName.setText(outputDisplayName)
        currentFocus?.let { focused ->
            getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(focused.windowToken, 0)
        }
        setBusy(true, getString(R.string.text_preparing_compression))

        operationJob = lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val cancellationContext = currentCoroutineContext()
                    var lastUiUpdateNanos = 0L
                    ZipArchiveCreator(resolvedRequest.hostSession).create(
                        request = resolvedRequest,
                        options = ArchiveCompressionOptions(outputDisplayName, compressionLevel),
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
                operationJob = null
            }
        }
    }

    private fun setBusy(busy: Boolean, message: String) = with(binding) {
        progress.isVisible = busy
        status.isVisible = true
        status.text = message
        outputNameLayout.isEnabled = !busy
        format.isEnabled = !busy
        compressionLevel.isEnabled = !busy
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
    }
}
