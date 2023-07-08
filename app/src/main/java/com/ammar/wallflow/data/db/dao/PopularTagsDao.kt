package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ammar.wallflow.data.db.entity.PopularTagEntity
import com.ammar.wallflow.data.db.entity.PopularTagWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface PopularTagsDao {
    @Query("SELECT * FROM popular_tags")
    fun observeAll(): Flow<List<PopularTagEntity>>

    @Query("SELECT * FROM popular_tags")
    suspend fun getAll(): List<PopularTagEntity>

    @Transaction
    @Query("SELECT * FROM popular_tags")
    suspend fun getAllWithDetails(): List<PopularTagWithDetails>

    @Query("SELECT COUNT(1) FROM popular_tags")
    suspend fun count(): Long

    @Query("DELETE FROM popular_tags")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg popularTag: PopularTagEntity)

    @Insert
    suspend fun insert(popularTags: Collection<PopularTagEntity>)
}
