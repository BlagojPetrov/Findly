package com.example.findly.domain.model

data class Item(
    val id: String,
    val type: ItemType,
    val title: String,
    val description: String,
    val category: Category,
    val imageUrl: String?,
    val locationName: String?,
    val userId: String,
    val userDisplayName: String,
    val userPhotoUrl: String? = null,
    val timestamp: Long
)

enum class ItemType { LOST, FOUND }

enum class Category {
    ELECTRONICS,
    CLOTHING,
    DOCUMENTS,
    ACCESSORIES,
    KEYS,
    WALLET,
    PETS,
    OTHER
}