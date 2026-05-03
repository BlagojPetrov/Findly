package com.example.findly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_items")
data class SavedItemEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val description: String,
    val category: String,
    val imageUrl: String?,
    val locationName: String?,
    val userDisplayName: String,
    val userPhotoUrl: String? = null,
    val timestamp: Long,
    val savedAt: Long = System.currentTimeMillis()
)