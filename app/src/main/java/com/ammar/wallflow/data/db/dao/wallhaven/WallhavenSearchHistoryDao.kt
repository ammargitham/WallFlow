package com.ammar.wallflow.data.db.dao.wallhaven

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallhavenSearchHistoryDao {
    @Query("SELECT * FROM wallhaven_search_history ORDER BY last_updated_on DESC")
    fun getAll(): Flow<List<WallhavenSearchHistoryEntity>>

    @Query("SELECT * FROM wallhaven_search_history WHERE `query` = :query")
    suspend fun getByQuery(query: String): WallhavenSearchHistoryEntity?

    @Upsert
    suspend fun upsert(searchHistory: WallhavenSearchHistoryEntity)

    @Query("DELETE FROM wallhaven_search_history WHERE `query` = :query")
    suspend fun deleteByQuery(query: String)
}