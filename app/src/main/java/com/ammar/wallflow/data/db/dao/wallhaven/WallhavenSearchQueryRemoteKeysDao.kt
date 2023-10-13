package com.ammar.wallflow.data.db.dao.wallhaven

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryRemoteKeyEntity

@Dao
interface WallhavenSearchQueryRemoteKeysDao {
    @Query(
        "SELECT * FROM search_query_remote_keys WHERE search_query_id = :searchQueryId",
    )
    suspend fun getBySearchQueryId(searchQueryId: Long): WallhavenSearchQueryRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(remoteKey: WallhavenSearchQueryRemoteKeyEntity)

    @Upsert
    suspend fun upsert(searchQueryRemoteKeyEntity: WallhavenSearchQueryRemoteKeyEntity)

    @Query("DELETE FROM search_query_remote_keys WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
