package com.example.findly.data.repository

import com.example.findly.data.mapper.toDomain
import com.example.findly.data.mapper.toDto
import com.example.findly.data.remote.firestore.FirestoreItemSource
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.example.findly.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreItemRepository(
    private val source: FirestoreItemSource = FirestoreItemSource()
) : ItemRepository {

    override fun getItemsFeed(): Flow<List<Item>> =
        source.observeItems().map { dtos -> dtos.map { it.toDomain() } }

    override fun getItemsByType(type: ItemType): Flow<List<Item>> =
        source.observeItemsByType(type).map { dtos -> dtos.map { it.toDomain() } }

    override fun searchItems(query: String): Flow<List<Item>> =
        source.searchItems(query).map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getItemById(id: String): Item? =
        source.getItemById(id)?.toDomain()

    override suspend fun createItem(item: Item) {
        source.createItem(item.toDto())
    }

    override fun getUserItems(userId: String): Flow<List<Item>> =
        source.observeUserItems(userId).map { dtos -> dtos.map { it.toDomain() } }
}