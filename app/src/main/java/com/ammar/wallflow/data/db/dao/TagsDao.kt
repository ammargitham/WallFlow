package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ammar.wallflow.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagsDao {
    @Query("SELECT * FROM tags")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT id FROM tags WHERE name IN (:names)")
    suspend fun getIdsByNames(names: Collection<String>): List<Long>

    @Query("SELECT * FROM tags WHERE name IN (:names)")
    suspend fun getByNames(names: Collection<String>): List<TagEntity>

    @Query("SELECT * FROM tags WHERE wallhaven_id IN (:wallhavenIds)")
    suspend fun getByWallhavenIds(wallhavenIds: Collection<Long>): List<TagEntity>

    @Query("SELECT COUNT(1) FROM tags")
    suspend fun count(): Long

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vararg tag: TagEntity): List<Long>

    @Insert
    suspend fun insert(tags: Collection<TagEntity>): List<Long>

    @Update
    suspend fun update(tags: Collection<TagEntity>)
}
