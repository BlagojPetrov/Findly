package com.example.findly.domain.repository

import com.example.findly.domain.model.Item
import kotlinx.coroutines.flow.Flow

interface SavedRepository {
    fun getSavedItems(userId: String): Flow<List<Item>>
    fun isItemSaved(itemId: String, userId: String): Flow<Boolean>
    suspend fun saveItem(item: Item, userId: String)
    suspend fun removeSavedItem(itemId: String, userId: String)
}