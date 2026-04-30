package com.example.findly.domain.repository

import com.example.findly.domain.model.Item
import kotlinx.coroutines.flow.Flow

interface SavedRepository {
    fun getSavedItems(): Flow<List<Item>>
    fun isItemSaved(itemId: String): Flow<Boolean>
    suspend fun saveItem(item: Item)
    suspend fun removeSavedItem(itemId: String)
}