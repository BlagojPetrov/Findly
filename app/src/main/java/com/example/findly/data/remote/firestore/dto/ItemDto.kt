package com.example.findly.data.remote.firestore.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ItemDto(
    @DocumentId
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String? = null,
    val locationName: String? = null,
    val userId: String = "",
    val userDisplayName: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val status: String = "ACTIVE",
    val keywords: List<String> = emptyList()
)