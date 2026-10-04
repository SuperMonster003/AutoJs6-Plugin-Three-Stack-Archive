package io.github.supermonster003.autojs6.plugin.three.stack.archive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.supermonster003.autojs6.plugin.three.stack.archive.databinding.ItemArchiveEntryBinding

internal data class ArchiveEntryRow(
    val path: String,
    val displayName: String,
    val details: String,
    val isDirectory: Boolean,
    val isBlocked: Boolean,
    val isSelected: Boolean,
)

internal class ArchiveEntryAdapter(
    private val onOpenDirectory: (ArchiveEntryRow) -> Unit,
    private val onSelectionChanged: (ArchiveEntryRow, Boolean) -> Unit,
) : ListAdapter<ArchiveEntryRow, ArchiveEntryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArchiveEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemArchiveEntryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ArchiveEntryRow) = with(binding) {
            selected.setOnCheckedChangeListener(null)
            selected.isEnabled = !row.isBlocked
            selected.isChecked = row.isSelected
            selected.contentDescription = buildString {
                append(root.context.getString(R.string.text_select_entry))
                append(": ")
                append(row.displayName)
            }
            selected.importantForAccessibility = if (row.isDirectory) {
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
            } else {
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            selected.setOnCheckedChangeListener { _, checked ->
                onSelectionChanged(row, checked)
            }
            kind.setImageResource(if (row.isDirectory) R.drawable.ic_entry_folder else R.drawable.ic_entry_file)
            name.text = row.displayName
            details.text = row.details
            root.contentDescription = listOf(row.displayName, row.details)
                .filter(String::isNotBlank)
                .joinToString(", ")
            root.isEnabled = row.isDirectory || !row.isBlocked
            root.isSelected = !row.isDirectory && row.isSelected
            ViewCompat.setAccessibilityDelegate(root, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (row.isDirectory) {
                        info.className = Button::class.java.name
                    } else {
                        info.className = CheckBox::class.java.name
                        info.isCheckable = true
                        info.isChecked = row.isSelected
                    }
                }
            })
            root.alpha = if (row.isBlocked) BLOCKED_ALPHA else 1F
            content.setOnClickListener {
                if (row.isDirectory) {
                    onOpenDirectory(row)
                } else if (!row.isBlocked) {
                    selected.isChecked = !selected.isChecked
                }
            }
            root.setOnClickListener { content.performClick() }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ArchiveEntryRow>() {
        override fun areItemsTheSame(oldItem: ArchiveEntryRow, newItem: ArchiveEntryRow) =
            oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: ArchiveEntryRow, newItem: ArchiveEntryRow) =
            oldItem == newItem
    }

    private companion object {
        const val BLOCKED_ALPHA = 0.45F
    }
}
