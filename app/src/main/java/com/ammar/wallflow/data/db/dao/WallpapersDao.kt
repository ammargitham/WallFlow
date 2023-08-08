package com.ammar.wallflow.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.WallpaperEntity
import com.ammar.wallflow.data.db.entity.WallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.WallpaperWithUploaderAndTags

@Dao
interface WallpapersDao {
    @Query("SELECT * FROM wallpapers")
    suspend fun getAll(): List<WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE wallhaven_id = :wallhavenId")
    suspend fun getByWallhavenId(wallhavenId: String): WallpaperEntity?

    @Transaction
    @Query("SELECT * FROM wallpapers WHERE wallhaven_id = :wallhavenId")
    suspend fun getWithUploaderAndTagsByWallhavenId(wallhavenId: String): WallpaperWithUploaderAndTags?

    @Query("SELECT * FROM wallpapers WHERE wallhaven_id IN (:wallhavenIds)")
    suspend fun getByWallhavenIds(wallhavenIds: List<String>): List<WallpaperEntity>

    @Query(
        """
            SELECT *
            FROM wallpapers
            WHERE id IN (
                SELECT sqw.wallpaper_id
                FROM search_query_wallpapers sqw
                WHERE sqw.search_query_id = (
                    SELECT sq.id
                    FROM search_query sq
                    WHERE query_string = :queryString
                )
            )
        """,
    )
    fun pagingSource(queryString: String): PagingSource<Int, WallpaperEntity>

    @Query("SELECT COUNT(1) FROM wallpapers")
    suspend fun count(): Int

    @Query("DELETE FROM wallpapers")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg wallpaper: WallpaperEntity): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(wallpapers: Collection<WallpaperEntity>): List<Long>

    @Update
    suspend fun update(vararg wallpaper: WallpaperEntity)

    @Upsert
    suspend fun upsert(vararg wallpaper: WallpaperEntity): List<Long>

    @Query(
        """
            SELECT
            *
            FROM wallpapers
            WHERE EXISTS (
                SELECT 1
                FROM search_query_wallpapers
                WHERE wallpaper_id = wallpapers.id
                AND search_query_id = :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM search_query_wallpapers
                WHERE wallpaper_id = wallpapers.id
                AND search_query_id <> :searchQueryId
            );
        """,
    )
    suspend fun getAllUniqueToSearchQueryId(searchQueryId: Long): List<WallpaperEntity>

    @Query(
        """
            DELETE
            FROM wallpapers
            WHERE EXISTS (
                SELECT 1
                FROM search_query_wallpapers
                WHERE wallpaper_id = wallpapers.id
                AND search_query_id = :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM search_query_wallpapers
                WHERE wallpaper_id = wallpapers.id
                AND search_query_id <> :searchQueryId
            );
        """,
    )
    suspend fun deleteAllUniqueToSearchQueryId(searchQueryId: Long)

    @Insert
    suspend fun insertWallpaperTagMappings(vararg wallpaperTag: WallpaperTagsEntity)

    @Insert
    suspend fun insertWallpaperTagMappings(wallpaperTags: Collection<WallpaperTagsEntity>)

    @Query("DELETE FROM wallpaper_tags WHERE wallpaper_id = :wallpaperId")
    suspend fun deleteWallpaperTagMappings(wallpaperId: Long)
}
