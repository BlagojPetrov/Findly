package com.example.findly.data.remote.firestore

import com.example.findly.data.remote.firestore.dto.ConversationDto
import com.example.findly.data.remote.firestore.dto.MessageDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreMessageSource {

    private val db = FirebaseFirestore.getInstance()
    private val conversationsCollection = db.collection("conversations")

    fun observeConversations(userId: String): Flow<List<ConversationDto>> = callbackFlow {
        val listener = conversationsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull {
                    it.toObject(ConversationDto::class.java)
                } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String): Flow<List<MessageDto>> = callbackFlow {
        val listener = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(MessageDto::class.java)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(dto: MessageDto) {
        val doc = conversationsCollection
            .document(dto.conversationId)
            .collection("messages")
            .document()
        val messageWithId = dto.copy(id = doc.id)
        doc.set(messageWithId).await()

        conversationsCollection.document(dto.conversationId)
            .update(
                mapOf(
                    "lastMessage" to dto.text,
                    "lastMessageTimestamp" to dto.timestamp
                )
            ).await()
    }

    suspend fun getOrCreateConversation(dto: ConversationDto): String {
        val existing = conversationsCollection
            .whereEqualTo("itemId", dto.itemId)
            .whereArrayContains("participantIds", dto.participantIds.first())
            .get()
            .await()
            .documents
            .firstOrNull { doc ->
                val participants = doc.get("participantIds") as? List<*>
                participants?.containsAll(dto.participantIds) == true
            }

        if (existing != null) return existing.id

        val doc = conversationsCollection.document()
        val newDto = dto.copy(id = doc.id)
        doc.set(newDto).await()
        return doc.id
    }

    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        val batch = db.batch()

        val unreadMessages = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .whereEqualTo("read", false)
            .whereNotEqualTo("senderId", userId)
            .get()
            .await()

        unreadMessages.documents.forEach { doc ->
            batch.update(doc.reference, "read", true)
        }

        batch.update(
            conversationsCollection.document(conversationId),
            "unreadCount.$userId", 0
        )

        batch.commit().await()
    }
}