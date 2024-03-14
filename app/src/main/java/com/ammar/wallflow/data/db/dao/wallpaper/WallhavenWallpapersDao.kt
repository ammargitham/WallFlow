package com.ammar.wallflow.data.db.dao.wallpaper

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperUploaderEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallpaperWithUploaderAndTags
import com.ammar.wallflow.utils.safeGetAll

@Dao
interface WallhavenWallpapersDao : WallpapersDao {
    @Query("SELECT * FROM wallhaven_wallpapers")
    suspend fun getAll(): List<WallhavenWallpaperEntity>

    @Query("SELECT * FROM wallhaven_wallpapers WHERE wallhaven_id = :wallhavenId")
    suspend fun getByWallhavenId(wallhavenId: String): WallhavenWallpaperEntity?

    @Transaction
    @Query("SELECT * FROM wallhaven_wallpapers WHERE wallhaven_id = :wallhavenId")
    suspend fun getWithUploaderAndTagsByWallhavenId(
        wallhavenId: String,
    ): WallpaperWithUploaderAndTags?

    @Transaction
    @Query("SELECT * FROM wallhaven_wallpapers WHERE wallhaven_id IN (:wallhavenIds)")
    suspend fun getAllWithUploaderAndTagsByWallhavenIds(
        wallhavenIds: Collection<String>,
    ): List<WallpaperWithUploaderAndTags>

    @Transaction
    @Query("SELECT * FROM wallhaven_wallpapers")
    suspend fun getAllWithUploaderAndTags(): List<WallpaperWithUploaderAndTags>

    @Query("SELECT * FROM wallhaven_wallpapers WHERE wallhaven_id IN (:wallhavenIds)")
    suspend fun getByWallhavenIds(wallhavenIds: List<String>): List<WallhavenWallpaperEntity>

    @Query(
        """
            SELECT ww.*
            FROM wallhaven_wallpapers ww
            INNER JOIN wallhaven_search_query_wallpapers wsqw
                ON ww.id = wsqw.wallpaper_id
            WHERE wsqw.search_query_id = (
                SELECT sq.id
                FROM search_query sq
                WHERE query_string = :queryString
            )
            ORDER BY COALESCE(wsqw.`order`, ww.ROWID)
        """,
    )
    fun pagingSource(queryString: String): PagingSource<Int, WallhavenWallpaperEntity>

    @Query("SELECT COUNT(1) FROM wallhaven_wallpapers")
    suspend fun count(): Int

    @Query("DELETE FROM wallhaven_wallpapers")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg wallpaper: WallhavenWallpaperEntity): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(wallpapers: Collection<WallhavenWallpaperEntity>): List<Long>

    @Update
    suspend fun update(vararg wallpaper: WallhavenWallpaperEntity)

    @Upsert
    suspend fun upsert(vararg wallpaper: WallhavenWallpaperEntity): List<Long>

    @Upsert
    suspend fun upsert(wallpapers: Collection<WallhavenWallpaperEntity>)

    @Query(
        """
            SELECT
            *
            FROM wallhaven_wallpapers
            WHERE EXISTS (
                SELECT 1
                FROM wallhaven_search_query_wallpapers
                WHERE wallpaper_id = wallhaven_wallpapers.id
                AND search_query_id = :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM wallhaven_search_query_wallpapers
                WHERE wallpaper_id = wallhaven_wallpapers.id
                AND search_query_id <> :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM favorites
                WHERE source_id = wallhaven_wallpapers.wallhaven_id
                AND source = 'WALLHAVEN'
            )
            AND NOT EXISTS (
                SELECT 1
                FROM light_dark
                WHERE source_id = wallhaven_wallpapers.wallhaven_id
                AND source = 'WALLHAVEN'
            );
        """,
    )
    suspend fun getAllUniqueToSearchQueryId(searchQueryId: Long): List<WallhavenWallpaperEntity>

    @Query("SELECT * FROM wallhaven_wallpapers WHERE wallhaven_id IN (:wallhavenIds)")
    suspend fun getAllByWallhavenIdsUpTo999Items(
        wallhavenIds: Collection<String>,
    ): List<WallhavenWallpaperEntity>

    @Transaction
    suspend fun getAllByWallhavenIds(wallhavenIds: Collection<String>) = safeGetAll(
        wallhavenIds,
        ::getAllByWallhavenIdsUpTo999Items,
    )

    @Query("SELECT wallhaven_id FROM wallhaven_wallpapers")
    suspend fun getAllWallhavenIds(): List<String>

    @Query(
        """
            DELETE
            FROM wallhaven_wallpapers
            WHERE EXISTS (
                SELECT 1
                FROM wallhaven_search_query_wallpapers
                WHERE wallpaper_id = wallhaven_wallpapers.id
                AND search_query_id = :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM wallhaven_search_query_wallpapers
                WHERE wallpaper_id = wallhaven_wallpapers.id
                AND search_query_id <> :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM favorites
                WHERE source_id = wallhaven_wallpapers.wallhaven_id
                AND source = 'WALLHAVEN'
            )
            AND NOT EXISTS (
                SELECT 1
                FROM light_dark
                WHERE source_id = wallhaven_wallpapers.wallhaven_id
                AND source = 'WALLHAVEN'
            );
        """,
    )
    suspend fun deleteAllUniqueToSearchQueryId(searchQueryId: Long)

    @Upsert
    suspend fun upsertWallpaperUploaderMappings(
        vararg wallpaperUploader: WallhavenWallpaperUploaderEntity,
    )

    @Insert
    suspend fun insertWallpaperTagMappings(vararg wallpaperTag: WallhavenWallpaperTagsEntity)

    @Insert
    suspend fun insertWallpaperTagMappings(wallpaperTags: Collection<WallhavenWallpaperTagsEntity>)

    @Query("DELETE FROM wallhaven_wallpaper_tags WHERE wallpaper_id = :wallpaperId")
    suspend fun deleteWallpaperTagMappings(wallpaperId: Long)
}
