package com.ammar.wallflow.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.model.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface LightDarkDao {
    @Query("SELECT * FROM light_dark ORDER BY updated_on DESC")
    fun observeAll(): Flow<List<LightDarkEntity>>

    @Query("SELECT * FROM light_dark ORDER BY updated_on DESC")
    suspend fun getAll(): List<LightDarkEntity>

    @Query("SELECT * FROM light_dark WHERE source_id = :sourceId AND source = :source")
    suspend fun getBySourceIdAndSource(sourceId: String, source: Source): LightDarkEntity?

    @Query("SELECT * FROM light_dark WHERE typeFlags in (:typeFlags) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomByTypeFlag(typeFlags: Set<Int>): LightDarkEntity?

    @Query("SELECT * FROM light_dark ORDER BY updated_on DESC")
    fun pagingSource(): PagingSource<Int, LightDarkEntity>

    @Query(
        "SELECT typeFlags FROM light_dark WHERE source_id = :sourceId AND source = :source",
    )
    fun observeTypeFlags(
        sourceId: String,
        source: Source,
    ): Flow<Int?>

    @Query("SELECT COUNT(*) FROM light_dark")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(lightDarkEntity: LightDarkEntity)

    @Query("DELETE FROM light_dark WHERE source_id = :sourceId AND source = :source")
    suspend fun deleteBySourceIdAndSource(sourceId: String, source: Source)

    @Insert
    suspend fun insertAll(lightDarkEntities: Collection<LightDarkEntity>)
}
