package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.SearchQueryEntity
import kotlinx.datetime.Instant

@Dao
interface SearchQueryDao {
    @Query("SELECT * FROM search_query WHERE id = :id")
    suspend fun getById(id: Long): SearchQueryEntity?

    @Query("SELECT * FROM search_query")
    suspend fun getAll(): List<SearchQueryEntity>

    @Query("SELECT id FROM search_query WHERE last_updated_on < :instant")
    suspend fun getAllIdsOlderThan(instant: Instant): List<Long>

    @Query(
        """
        SELECT *
        FROM search_query
        WHERE query_string = :queryString
        """,
    )
    suspend fun getBySearchQuery(queryString: String): SearchQueryEntity?

    @Query("SELECT COUNT(1) FROM search_query")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(vararg searchQuery: SearchQueryEntity): List<Long>

    @Upsert
    suspend fun upsert(searchQueries: Collection<SearchQueryEntity>): List<Long>

    @Query("DELETE FROM search_query WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM search_query")
    suspend fun deleteAll()
}
