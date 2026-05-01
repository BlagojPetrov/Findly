package com.example.findly.data.local.dao

import androidx.room.*
import com.example.findly.data.local.entity.SavedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {

    @Query("SELECT * FROM saved_items WHERE userId = :userId ORDER BY savedAt DESC")
    fun getAllSavedItems(userId: String): Flow<List<SavedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveItem(item: SavedItemEntity)

    @Query("DELETE FROM saved_items WHERE id = :itemId AND userId = :userId")
    suspend fun removeSavedItem(itemId: String, userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_items WHERE id = :itemId AND userId = :userId)")
    fun isItemSaved(itemId: String, userId: String): Flow<Boolean>
}