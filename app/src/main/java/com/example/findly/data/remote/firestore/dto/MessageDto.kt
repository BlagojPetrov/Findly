package com.example.findly.data.remote.firestore.dto

import com.google.firebase.firestore.DocumentId

data class MessageDto(
    @DocumentId
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderDisplayName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
)