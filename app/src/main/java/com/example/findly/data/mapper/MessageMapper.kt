package com.example.findly.data.mapper

import com.example.findly.data.remote.firestore.dto.ConversationDto
import com.example.findly.data.remote.firestore.dto.MessageDto
import com.example.findly.domain.model.Conversation
import com.example.findly.domain.model.Message

fun MessageDto.toDomain(): Message {
    return Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderDisplayName = senderDisplayName,
        senderPhotoUrl = senderPhotoUrl,
        text = text,
        timestamp = timestamp,
        read = read
    )
}

fun ConversationDto.toDomain(currentUserId: String): Conversation {
    val otherUserId = participantIds.first { it != currentUserId }
    return Conversation(
        id = id,
        itemId = itemId,
        itemTitle = itemTitle,
        otherUserId = otherUserId,
        otherUserDisplayName = participantNames[otherUserId] ?: "",
        otherUserPhotoUrl = participantPhotoUrls[otherUserId],
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount[currentUserId] ?: 0
    )
}