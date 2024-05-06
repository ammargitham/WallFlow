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

    @Query("SELECT * FROM light_dark WHERE typeFlags IN (:typeFlags) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomByTypeFlag(typeFlags: Set<Int>): LightDarkEntity?

    @Query(
        """
        SELECT * FROM light_dark
        WHERE id NOT IN (
            SELECT DISTINCT ld.id
            FROM auto_wallpaper_history awh JOIN light_dark ld
                    ON awh.source = ld.source AND  awh.source_id = ld.source_id
        )
        AND typeFlags IN (:typeFlags)
        AND id NOT IN (:excludingIds)
        ORDER BY updated_on
        LIMIT 1
        """,
    )
    suspend fun getFirstFreshByTypeFlagsAndIdNotIn(
        typeFlags: Set<Int>,
        excludingIds: Collection<Long>,
    ): LightDarkEntity?

    @Query(
        """
        SELECT ld.*
        FROM light_dark ld INNER JOIN (
            SELECT awh.* FROM auto_wallpaper_history awh
            INNER JOIN (
                SELECT id, source, source_id, max(set_on) max_value
                FROM auto_wallpaper_history
                GROUP BY source, source_id
            ) t on t.id = awh.id
        ) awh
        WHERE awh.source = ld.source
            AND  awh.source_id = ld.source_id
            AND ld.typeFlags IN (:typeFlags)
            AND ld.id NOT IN (:excludingIds)
        ORDER BY awh.set_on
        LIMIT 1
        """,
    )
    suspend fun getByOldestSetOnAndTypeFlagsAndIdsNotId(
        typeFlags: Set<Int>,
        excludingIds: Collection<Long>,
    ): LightDarkEntity?

    @Query(
        """
            SELECT id
            FROM light_dark
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
            FROM light_dark
            WHERE
                typeFlags IN (:typeFlags)
                AND id NOT IN (:ids)
        """,
    )
    suspend fun getCountWhereTypeFlagsAndIdsNotIn(
        typeFlags: Set<Int>,
        ids: Collection<Long>,
    ): Int

    // @Query(
    //     """
    //     select awh.* from auto_wallpaper_history awh
    //     inner join (
    //         select id, source, source_id, max(set_on) max_value
    //         from auto_wallpaper_history
    //         group by source, source_id
    //     ) t on t.id = awh.id
    //     order by set_on
    //     """
    // )
    // suspend fun getAllInHistoryByTypeFlags(): List<AutoWallpaperHistoryEntity>

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
