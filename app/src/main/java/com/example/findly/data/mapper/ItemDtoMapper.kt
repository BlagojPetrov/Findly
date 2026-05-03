package com.example.findly.data.mapper

import com.example.findly.data.remote.firestore.dto.ItemDto
import com.example.findly.domain.model.Category
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType

fun ItemDto.toDomain(): Item = Item(
    id              = id,
    type            = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.LOST),
    title           = title,
    description     = description,
    category        = runCatching { Category.valueOf(category) }.getOrDefault(Category.OTHER),
    imageUrl        = imageUrl,
    locationName    = locationName,
    userId          = userId,
    userDisplayName = userDisplayName,
    userPhotoUrl    = userPhotoUrl,
    timestamp       = timestamp?.toDate()?.time ?: System.currentTimeMillis()
)

fun Item.toDto(): ItemDto = ItemDto(
    id              = id,
    type            = type.name,
    title           = title,
    description     = description,
    category        = category.name,
    imageUrl        = imageUrl,
    locationName    = locationName,
    userId          = userId,
    userDisplayName = userDisplayName,
    userPhotoUrl    = userPhotoUrl,
    keywords        = extractKeywords(title, description)
)

private fun extractKeywords(title: String, description: String): List<String> {
    val stopWords = setOf(
        // English
        "the", "a", "an", "and", "or", "but", "in", "on", "at",
        "to", "for", "of", "with", "is", "it", "this", "that", "was",

        // Macedonian
        "и", "или", "но", "во", "на", "со", "се", "ова", "тоа", "беше"
    )

    return (title + " " + description)
        .lowercase()
        .replace(Regex("[^\\p{L}0-9 ]"), "")
        .split(" ")
        .filter { it.length > 2 && it !in stopWords }
        .distinct()
        .take(20)
}