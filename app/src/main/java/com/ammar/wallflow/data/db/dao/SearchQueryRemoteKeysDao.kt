package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.SearchQueryRemoteKeyEntity

@Dao
interface SearchQueryRemoteKeysDao {
    @Query("SELECT * FROM search_query_remote_keys WHERE search_query_id = :searchQueryId")
    suspend fun getBySearchQueryId(searchQueryId: Long): SearchQueryRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(remoteKey: SearchQueryRemoteKeyEntity)

    @Upsert
    suspend fun upsert(searchQueryRemoteKeyEntity: SearchQueryRemoteKeyEntity)

    @Query("DELETE FROM search_query_remote_keys WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
