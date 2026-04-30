package com.example.findly.data.mapper

import com.example.findly.data.local.entity.SavedItemEntity
import com.example.findly.domain.model.Category
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType

fun Item.toSavedEntity(): SavedItemEntity = SavedItemEntity(
    id              = id,
    type            = type.name,
    title           = title,
    description     = description,
    category        = category.name,
    imageUrl        = imageUrl,
    locationName    = locationName,
    userId          = userId,
    userDisplayName = userDisplayName,
    timestamp       = timestamp
)

fun SavedItemEntity.toDomain(): Item = Item(
    id              = id,
    type            = ItemType.valueOf(type),
    title           = title,
    description     = description,
    category        = Category.valueOf(category),
    imageUrl        = imageUrl,
    locationName    = locationName,
    userId          = userId,
    userDisplayName = userDisplayName,
    timestamp       = timestamp
)