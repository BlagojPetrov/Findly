package com.example.findly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.findly.data.local.dao.SavedItemDao
import com.example.findly.data.local.entity.SavedItemEntity

@Database(
    entities = [SavedItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FindlyDatabase : RoomDatabase() {

    abstract fun savedItemDao(): SavedItemDao

    companion object {
        @Volatile
        private var INSTANCE: FindlyDatabase? = null

        fun getInstance(context: Context): FindlyDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FindlyDatabase::class.java,
                    "findly_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}