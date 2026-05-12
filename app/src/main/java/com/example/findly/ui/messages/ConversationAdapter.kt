package com.example.findly.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.findly.R
import com.example.findly.databinding.ItemConversationBinding
import com.example.findly.domain.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onClick: (Conversation) -> Unit,
    private val onLongClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.textViewUserName.text = conversation.otherUserDisplayName
            binding.textViewItemTitle.text = conversation.itemTitle
            binding.textViewLastMessage.text = conversation.lastMessage
            binding.textViewTimestamp.text = formatTimestamp(conversation.lastMessageTimestamp)

            binding.textViewUnreadCount.isVisible = conversation.unreadCount > 0
            binding.textViewUnreadCount.text = conversation.unreadCount.toString()

            binding.imageViewAvatar.load(conversation.otherUserPhotoUrl) {
                placeholder(R.drawable.ic_person)
                error(R.drawable.ic_person)
                transformations(CircleCropTransformation())
            }

            binding.root.setOnClickListener { onClick(conversation) }
            binding.root.setOnLongClickListener {
                onLongClick(conversation)
                true
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val oneDayMillis = 24 * 60 * 60 * 1000
            return if (diff < oneDayMillis) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            } else {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}