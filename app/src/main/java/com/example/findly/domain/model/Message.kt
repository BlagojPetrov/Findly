package com.example.findly.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderDisplayName: String,
    val senderPhotoUrl: String?,
    val text: String,
    val timestamp: Long,
    val read: Boolean
)