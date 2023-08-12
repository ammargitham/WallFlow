package com.ammar.wallflow.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.model.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY favorited_on DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY favorited_on DESC")
    fun pagingSource(): PagingSource<Int, FavoriteEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM favorites WHERE source_id = :sourceId AND source = :source)",
    )
    suspend fun exists(
        sourceId: String,
        source: Source,
    ): Boolean

    @Query("SELECT * FROM favorites WHERE source_id = :sourceId AND source = :source")
    suspend fun getBySourceIdAndType(
        sourceId: String,
        source: Source,
    ): FavoriteEntity?

    @Insert
    suspend fun insertAll(favoriteEntities: Collection<FavoriteEntity>)

    @Upsert
    suspend fun upsert(favoriteEntity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE source_id = :sourceId AND source = :source")
    suspend fun deleteBySourceIdAndType(
        sourceId: String,
        source: Source,
    )
}
