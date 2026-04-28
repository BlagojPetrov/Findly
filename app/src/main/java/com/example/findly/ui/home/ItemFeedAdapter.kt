package com.example.findly.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.findly.R
import com.example.findly.databinding.ItemFeedCardBinding
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ItemFeedAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, ItemFeedAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemFeedCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(
        private val binding: ItemFeedCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.tvLocation.text = item.locationName ?: binding.root.context.getString(R.string.location_unknown)
            binding.tvTimestamp.text = item.timestamp.toRelativeTime()

            // Type chip
            binding.chipType.text = when (item.type) {
                ItemType.LOST  -> binding.root.context.getString(R.string.type_lost)
                ItemType.FOUND -> binding.root.context.getString(R.string.type_found)
            }
            binding.chipType.setChipBackgroundColorResource(
                when (item.type) {
                    ItemType.LOST  -> R.color.chip_lost_background
                    ItemType.FOUND -> R.color.chip_found_background
                }
            )
            binding.chipType.setTextColor(
                binding.root.context.getColor(
                    when (item.type) {
                        ItemType.LOST  -> R.color.chip_lost_text
                        ItemType.FOUND -> R.color.chip_found_text
                    }
                )
            )

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun Long.toRelativeTime(): String {
            val now = System.currentTimeMillis()
            val diff = now - this
            return when {
                diff < TimeUnit.MINUTES.toMillis(1)  -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                diff < TimeUnit.DAYS.toMillis(1)     -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}