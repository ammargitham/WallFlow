package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.WallhavenSavedSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM wallhaven_saved_searches ORDER BY name")
    fun observeAll(): Flow<List<WallhavenSavedSearchEntity>>

    @Query("SELECT * FROM wallhaven_saved_searches ORDER BY name")
    suspend fun getAll(): List<WallhavenSavedSearchEntity>

    @Query("SELECT * FROM wallhaven_saved_searches WHERE id = :id")
    suspend fun getById(id: Long): WallhavenSavedSearchEntity?

    @Query("SELECT * FROM wallhaven_saved_searches WHERE name = :name")
    suspend fun getByName(name: String): WallhavenSavedSearchEntity?

    @Query("SELECT * FROM wallhaven_saved_searches WHERE name in (:names)")
    suspend fun getAllByNames(names: Collection<String>): List<WallhavenSavedSearchEntity>

    @Upsert
    suspend fun upsert(savedSearch: WallhavenSavedSearchEntity)

    @Upsert
    suspend fun upsert(savedSearchDaos: Collection<WallhavenSavedSearchEntity>)

    @Query("DELETE FROM wallhaven_saved_searches WHERE name = :name")
    suspend fun deleteByName(name: String)
}
