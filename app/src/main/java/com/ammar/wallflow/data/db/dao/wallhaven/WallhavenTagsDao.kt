package com.ammar.wallflow.data.db.dao.wallhaven

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallhavenTagsDao {
    @Query("SELECT * FROM wallhaven_tags")
    fun observeAll(): Flow<List<WallhavenTagEntity>>

    @Query("SELECT * FROM wallhaven_tags")
    suspend fun getAll(): List<WallhavenTagEntity>

    @Query("SELECT id FROM wallhaven_tags WHERE name IN (:names)")
    suspend fun getIdsByNames(names: Collection<String>): List<Long>

    @Query("SELECT * FROM wallhaven_tags WHERE name IN (:names)")
    suspend fun getByNames(names: Collection<String>): List<WallhavenTagEntity>

    @Query("SELECT * FROM wallhaven_tags WHERE wallhaven_id IN (:wallhavenIds)")
    suspend fun getByWallhavenIds(wallhavenIds: Collection<Long>): List<WallhavenTagEntity>

    @Query("SELECT COUNT(1) FROM wallhaven_tags")
    suspend fun count(): Long

    @Query("DELETE FROM wallhaven_tags")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg tag: WallhavenTagEntity): List<Long>

    @Insert
    suspend fun insert(tags: Collection<WallhavenTagEntity>): List<Long>

    @Update
    suspend fun update(tags: Collection<WallhavenTagEntity>)
}
