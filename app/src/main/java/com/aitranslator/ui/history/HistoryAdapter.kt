package com.aitranslator.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aitranslator.data.model.HistoryItem
import com.aitranslator.databinding.ItemHistoryBinding
import com.aitranslator.utils.toFormattedDate

/**
 * RecyclerView adapter for the translation history list.
 *
 * Uses [ListAdapter] with [DiffUtil] for efficient, animated list updates.
 *
 * @param onDeleteClick Callback invoked when the delete icon is tapped.
 * @param onCopyClick   Callback invoked when the copy icon is tapped.
 */
class HistoryAdapter(
    private val onDeleteClick: (HistoryItem) -> Unit,
    private val onCopyClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a [HistoryItem] to this row's views.
         */
        fun bind(item: HistoryItem) {
            binding.apply {
                tvOriginalText.text = item.originalText
                tvTranslatedText.text = item.translatedText
                tvLanguagePair.text = "${item.sourceLanguage}  →  ${item.targetLanguage}"
                tvTimestamp.text = item.timestamp.toFormattedDate()

                // Delete this history entry
                btnDelete.setOnClickListener { onDeleteClick(item) }

                // Copy translated text to clipboard
                btnCopy.setOnClickListener { onCopyClick(item) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // DiffUtil callback
    // -------------------------------------------------------------------------

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
            oldItem == newItem
    }
}
