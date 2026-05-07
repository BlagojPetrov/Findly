package com.example.findly.domain.model

data class Conversation(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val otherUserId: String,
    val otherUserDisplayName: String,
    val otherUserPhotoUrl: String?,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
)