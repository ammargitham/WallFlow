package com.ammar.wallflow.data.db.dao.search

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
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

    @Query("SELECT * FROM saved_searches WHERE name IN (:names)")
    suspend fun getAllByNames(names: Collection<String>): List<SavedSearchEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_searches WHERE id = :id)")
    suspend fun exists(id: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM saved_searches WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM saved_searches
            WHERE
                id != :id
                AND name = :name
        )
    """,
    )
    suspend fun existsExcludingId(id: Long, name: String): Boolean

    @Upsert
    suspend fun upsert(savedSearch: SavedSearchEntity)

    @Upsert
    suspend fun upsert(savedSearchDaos: Collection<SavedSearchEntity>)

    @Query("DELETE FROM saved_searches WHERE name = :name")
    suspend fun deleteByName(name: String)
}
