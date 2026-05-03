package com.example.findly.domain.repository

import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun getItemsFeed(): Flow<List<Item>>
    fun getItemsByType(type: ItemType): Flow<List<Item>>
    fun searchItems(query: String): Flow<List<Item>>
    suspend fun getItemById(id: String): Item?
    suspend fun createItem(item: Item)
    fun getUserItems(userId: String): Flow<List<Item>>

    suspend fun deleteItem(itemId: String)
}