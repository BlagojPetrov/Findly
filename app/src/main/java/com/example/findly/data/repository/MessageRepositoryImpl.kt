package com.example.findly.data.repository

import com.example.findly.data.mapper.toDomain
import com.example.findly.data.remote.firestore.FirestoreMessageSource
import com.example.findly.data.remote.firestore.dto.ConversationDto
import com.example.findly.data.remote.firestore.dto.MessageDto
import com.example.findly.domain.model.Conversation
import com.example.findly.domain.model.Message
import com.example.findly.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val firestoreMessageSource: FirestoreMessageSource
) : MessageRepository {

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            firestoreMessageSource.deleteConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeConversations(userId: String): Flow<List<Conversation>> {
        return firestoreMessageSource.observeConversations(userId).map { list ->
            list.map { it.toDomain(userId) }
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return firestoreMessageSource.observeMessages(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderDisplayName: String,
        senderPhotoUrl: String?,
        text: String
    ): Result<Unit> {
        return try {
            val dto = MessageDto(
                conversationId = conversationId,
                senderId = senderId,
                senderDisplayName = senderDisplayName,
                senderPhotoUrl = senderPhotoUrl,
                text = text,
                timestamp = System.currentTimeMillis(),
                read = false
            )
            firestoreMessageSource.sendMessage(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrCreateConversation(
        itemId: String,
        itemTitle: String,
        currentUserId: String,
        currentUserDisplayName: String,
        otherUserId: String,
        otherUserDisplayName: String,
        otherUserPhotoUrl: String?
    ): Result<String> {
        return try {
            val dto = ConversationDto(
                itemId = itemId,
                itemTitle = itemTitle,
                participantIds = listOf(currentUserId, otherUserId),
                participantNames = mapOf(
                    currentUserId to currentUserDisplayName,
                    otherUserId to otherUserDisplayName
                ),
                participantPhotoUrls = buildMap {
                    if (otherUserPhotoUrl != null) put(otherUserId, otherUserPhotoUrl)
                },
                lastMessage = "",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = mapOf(
                    currentUserId to 0,
                    otherUserId to 0
                )
            )
            val conversationId = firestoreMessageSource.getOrCreateConversation(dto)
            Result.success(conversationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsRead(
        conversationId: String,
        userId: String
    ): Result<Unit> {
        return try {
            firestoreMessageSource.markMessagesAsRead(conversationId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}