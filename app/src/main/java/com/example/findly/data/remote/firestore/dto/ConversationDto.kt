package com.example.findly.data.remote.firestore.dto

import com.google.firebase.firestore.DocumentId

data class ConversationDto(
    @DocumentId
    val id: String = "",
    val itemId: String = "",
    val itemTitle: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotoUrls: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Map<String, Int> = emptyMap()
)