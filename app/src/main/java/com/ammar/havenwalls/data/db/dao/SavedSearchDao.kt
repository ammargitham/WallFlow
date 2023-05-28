package com.ammar.havenwalls.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.havenwalls.data.db.entity.SavedSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY name")
    fun getAll(): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches WHERE id = :id")
    suspend fun getById(id: Long): SavedSearchEntity?

    @Query("SELECT * FROM saved_searches WHERE name = :name")
    suspend fun getByName(name: String): SavedSearchEntity?

    @Upsert
    suspend fun upsert(savedSearchDao: SavedSearchEntity)

    @Query("DELETE FROM saved_searches WHERE name = :name")
    suspend fun deleteByName(name: String)
}
