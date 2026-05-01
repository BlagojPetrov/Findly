package com.example.findly.data.repository

import com.example.findly.data.local.dao.SavedItemDao
import com.example.findly.data.mapper.toDomain
import com.example.findly.data.mapper.toSavedEntity
import com.example.findly.domain.model.Item
import com.example.findly.domain.repository.SavedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedRepositoryImpl(
    private val dao: SavedItemDao
) : SavedRepository {

    override fun getSavedItems(userId: String): Flow<List<Item>> =
        dao.getAllSavedItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun isItemSaved(itemId: String, userId: String): Flow<Boolean> =
        dao.isItemSaved(itemId, userId)

    override suspend fun saveItem(item: Item, userId: String) =
        dao.saveItem(item.toSavedEntity(userId))

    override suspend fun removeSavedItem(itemId: String, userId: String) =
        dao.removeSavedItem(itemId, userId)
}