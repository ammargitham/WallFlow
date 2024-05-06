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
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites ORDER BY favorited_on DESC")
    fun pagingSource(): PagingSource<Int, FavoriteEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM favorites WHERE source_id = :sourceId AND source = :source)",
    )
    suspend fun exists(
        sourceId: String,
        source: Source,
    ): Boolean

    @Query(
        "SELECT EXISTS(SELECT 1 FROM favorites WHERE source_id = :sourceId AND source = :source)",
    )
    fun observeExists(
        sourceId: String,
        source: Source,
    ): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getCount(): Int

    @Query("SELECT * FROM favorites WHERE source_id = :sourceId AND source = :source")
    suspend fun getBySourceIdAndType(
        sourceId: String,
        source: Source,
    ): FavoriteEntity?

    @Query("SELECT * FROM favorites ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): FavoriteEntity?

    @Query(
        """
        SELECT * FROM favorites
        WHERE id NOT IN (
            SELECT DISTINCT f.id
            FROM auto_wallpaper_history awh JOIN favorites f
                    ON awh.source = f.source AND  awh.source_id = f.source_id
        )
        AND id NOT IN (:excludingIds)
        ORDER BY favorited_on
        LIMIT 1
        """,
    )
    suspend fun getFirstFreshExcludingIds(
        excludingIds: Collection<Long>,
    ): FavoriteEntity?

    @Query(
        """
        SELECT f.*
        FROM favorites f INNER JOIN (
            SELECT awh.* FROM auto_wallpaper_history awh
            INNER JOIN (
                SELECT id, source, source_id, max(set_on) max_value
                FROM auto_wallpaper_history
                GROUP BY source, source_id
            ) t on t.id = awh.id
        ) awh
        WHERE awh.source = f.source
            AND awh.source_id = f.source_id
            AND f.id NOT IN (:excludingIds)
        ORDER BY awh.set_on
        LIMIT 1
        """,
    )
    suspend fun getByOldestSetOnAndIdsNotIn(
        excludingIds: Collection<Long>,
    ): FavoriteEntity?

    @Insert
    suspend fun insertAll(favoriteEntities: Collection<FavoriteEntity>)

    @Upsert
    suspend fun upsert(favoriteEntity: FavoriteEntity)

    @Query(
        """
        DELETE FROM favorites
        WHERE
            source_id = :sourceId
            AND source = :source
        """,
    )
    suspend fun deleteBySourceIdAndSource(
        sourceId: String,
        source: Source,
    )

    @Query(
        """
        DELETE FROM favorites
        WHERE
            source_id in (:sourceIds)
            AND source = :source
        """,
    )
    suspend fun deleteBySourceIdsAndSource(
        sourceIds: Collection<String>,
        source: Source,
    )

    @Query(
        """
            SELECT id
            FROM favorites
            WHERE
                source_id in (:sourceIds)
                AND source = :source
        """,
    )
    suspend fun getIdsBySourceIdsAndSource(
        sourceIds: Collection<String>,
        source: Source,
    ): List<Long>

    @Query(
        """
            SELECT COUNT(*)
            FROM favorites
            WHERE id NOT IN (:ids)
        """,
    )
    suspend fun getCountWhereIdsNotIn(ids: Collection<Long>): Int
}
