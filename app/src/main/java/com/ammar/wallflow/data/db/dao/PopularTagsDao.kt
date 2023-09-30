package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ammar.wallflow.data.db.entity.WallhavenPopularTagEntity
import com.ammar.wallflow.data.db.entity.WallhavenPopularTagWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface PopularTagsDao {
    @Query("SELECT * FROM wallhaven_popular_tags")
    fun observeAll(): Flow<List<WallhavenPopularTagEntity>>

    @Query("SELECT * FROM wallhaven_popular_tags")
    suspend fun getAll(): List<WallhavenPopularTagEntity>

    @Transaction
    @Query("SELECT * FROM wallhaven_popular_tags")
    suspend fun getAllWithDetails(): List<WallhavenPopularTagWithDetails>

    @Query("SELECT COUNT(1) FROM wallhaven_popular_tags")
    suspend fun count(): Long

    @Query("DELETE FROM wallhaven_popular_tags")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg popularTag: WallhavenPopularTagEntity)

    @Insert
    suspend fun insert(popularTags: Collection<WallhavenPopularTagEntity>)
}
