package com.ammar.wallflow.data.db.dao.search

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.search.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY last_updated_on DESC")
    fun getAll(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE `query` = :query")
    suspend fun getByQuery(query: String): SearchHistoryEntity?

    @Upsert
    suspend fun upsert(searchHistory: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteByQuery(query: String)
}
