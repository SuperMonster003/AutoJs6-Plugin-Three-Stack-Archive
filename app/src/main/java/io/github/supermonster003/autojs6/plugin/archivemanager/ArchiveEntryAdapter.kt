package io.github.supermonster003.autojs6.plugin.archivemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.supermonster003.autojs6.plugin.archivemanager.databinding.ItemArchiveEntryBinding

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
            selected.setOnCheckedChangeListener { _, checked ->
                onSelectionChanged(row, checked)
            }
            kind.text = if (row.isDirectory) FOLDER_SYMBOL else FILE_SYMBOL
            name.text = row.displayName
            details.text = row.details
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
        const val FOLDER_SYMBOL = "\uD83D\uDCC1"
        const val FILE_SYMBOL = "\uD83D\uDCC4"
        const val BLOCKED_ALPHA = 0.45F
    }
}
