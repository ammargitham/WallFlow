package com.ammar.wallflow.data.db.dao.search

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.utils.safeGetAll
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
    suspend fun getAllByNamesUpTo999Items(names: Collection<String>): List<SavedSearchEntity>

    @Transaction
    suspend fun getAllByNames(names: Collection<String>) = safeGetAll(
        names,
        ::getAllByNamesUpTo999Items,
    )

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
