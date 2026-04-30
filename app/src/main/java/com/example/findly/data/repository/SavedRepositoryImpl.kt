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

    override fun getSavedItems(): Flow<List<Item>> =
        dao.getAllSavedItems().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun isItemSaved(itemId: String): Flow<Boolean> =
        dao.isItemSaved(itemId)

    override suspend fun saveItem(item: Item) =
        dao.saveItem(item.toSavedEntity())

    override suspend fun removeSavedItem(itemId: String) =
        dao.removeSavedItem(itemId)
}