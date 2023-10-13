package com.ammar.wallflow.data.db.dao.wallhaven

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.wallhaven.SavedSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY name")
    fun observeAll(): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches ORDER BY name")
    suspend fun getAll(): List<SavedSearchEntity>

    @Query("SELECT * FROM saved_searches WHERE id = :id")
    suspend fun getById(id: Long): SavedSearchEntity?

    @Query("SELECT * FROM saved_searches WHERE name = :name")
    suspend fun getByName(name: String): SavedSearchEntity?

    @Query("SELECT * FROM saved_searches WHERE name in (:names)")
    suspend fun getAllByNames(names: Collection<String>): List<SavedSearchEntity>

    @Upsert
    suspend fun upsert(savedSearch: SavedSearchEntity)

    @Upsert
    suspend fun upsert(savedSearchDaos: Collection<SavedSearchEntity>)

    @Query("DELETE FROM saved_searches WHERE name = :name")
    suspend fun deleteByName(name: String)
}
