package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.WallhavenSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM wallhaven_search_history ORDER BY last_updated_on DESC")
    fun getAll(): Flow<List<WallhavenSearchHistoryEntity>>

    @Query("SELECT * FROM wallhaven_search_history WHERE `query` = :query")
    suspend fun getByQuery(query: String): WallhavenSearchHistoryEntity?

    @Upsert
    suspend fun upsert(searchHistory: WallhavenSearchHistoryEntity)

    @Query("DELETE FROM wallhaven_search_history WHERE `query` = :query")
    suspend fun deleteByQuery(query: String)
}
