package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.WallhavenSearchQueryEntity
import kotlinx.datetime.Instant

@Dao
interface WallhavenSearchQueryDao {
    @Query("SELECT * FROM wallhaven_search_query WHERE id = :id")
    suspend fun getById(id: Long): WallhavenSearchQueryEntity?

    @Query("SELECT * FROM wallhaven_search_query")
    suspend fun getAll(): List<WallhavenSearchQueryEntity>

    @Query("SELECT id FROM wallhaven_search_query WHERE last_updated_on < :instant")
    suspend fun getAllIdsOlderThan(instant: Instant): List<Long>

    @Query(
        """
        SELECT *
        FROM wallhaven_search_query
        WHERE query_string = :queryString
        """,
    )
    suspend fun getBySearchQuery(queryString: String): WallhavenSearchQueryEntity?

    @Query("SELECT COUNT(1) FROM wallhaven_search_query")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(vararg searchQuery: WallhavenSearchQueryEntity): List<Long>

    @Upsert
    suspend fun upsert(searchQueries: Collection<WallhavenSearchQueryEntity>): List<Long>

    @Query("DELETE FROM wallhaven_search_query WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wallhaven_search_query")
    suspend fun deleteAll()
}
