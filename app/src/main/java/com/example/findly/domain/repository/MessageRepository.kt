package com.example.findly.domain.repository

import com.example.findly.domain.model.Conversation
import com.example.findly.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeConversations(userId: String): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderDisplayName: String,
        senderPhotoUrl: String?,
        text: String
    ): Result<Unit>
    suspend fun getOrCreateConversation(
        itemId: String,
        itemTitle: String,
        currentUserId: String,
        currentUserDisplayName: String,
        otherUserId: String,
        otherUserDisplayName: String,
        otherUserPhotoUrl: String?
    ): Result<String>
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Result<Unit>
}