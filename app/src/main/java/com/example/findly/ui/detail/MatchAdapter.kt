package com.example.findly.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.findly.R
import com.example.findly.databinding.ItemMatchCardBinding
import com.example.findly.domain.model.MatchedItem
import kotlin.math.roundToInt

class MatchAdapter(
    private val onClick: (MatchedItem) -> Unit
) : ListAdapter<MatchedItem, MatchAdapter.MatchViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MatchViewHolder(
        private val binding: ItemMatchCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(matchedItem: MatchedItem) {
            val item = matchedItem.item
            val scorePercent = (matchedItem.score * 100).roundToInt()

            binding.textViewMatchTitle.text = item.title
            binding.textViewMatchLocation.text = item.locationName
                ?: binding.root.context.getString(R.string.location_unknown)
            binding.textViewMatchScore.text = "$scorePercent% match"

            binding.imageViewMatchPhoto.load(item.imageUrl) {
                placeholder(R.drawable.ic_category)
                error(R.drawable.ic_category)
                transformations(RoundedCornersTransformation(12f))
            }

            binding.root.setOnClickListener { onClick(matchedItem) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MatchedItem>() {
        override fun areItemsTheSame(oldItem: MatchedItem, newItem: MatchedItem): Boolean {
            return oldItem.item.id == newItem.item.id
        }

        override fun areContentsTheSame(oldItem: MatchedItem, newItem: MatchedItem): Boolean {
            return oldItem == newItem
        }
    }
}