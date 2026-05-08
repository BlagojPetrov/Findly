package com.example.findly.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.findly.R
import com.example.findly.databinding.ItemMessageReceivedBinding
import com.example.findly.databinding.ItemMessageSentBinding
import com.example.findly.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val currentUserId: String,
    private val otherUserPhotoUrl: String?
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_SENT = 0
        private const val VIEW_TYPE_RECEIVED = 1

        object DiffCallback : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(getItem(position))
            is ReceivedMessageViewHolder -> {
                val isLastFromSender = position == itemCount - 1 ||
                        getItem(position + 1).senderId == currentUserId
                holder.bind(getItem(position), isLastFromSender)
            }
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textViewMessage.text = message.text
            binding.textViewTimestamp.text = formatTimestamp(message.timestamp)
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, showAvatar: Boolean) {
            binding.textViewMessage.text = message.text
            binding.textViewTimestamp.text = formatTimestamp(message.timestamp)
            binding.textViewSenderName.text = message.senderDisplayName

            if (showAvatar) {
                binding.imageViewAvatar.load(otherUserPhotoUrl) {
                    placeholder(R.drawable.ic_person)
                    error(R.drawable.ic_person)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.imageViewAvatar.setImageDrawable(null)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}