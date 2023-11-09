package com.ammar.wallflow.data.db.dao.wallpaper

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryWallpaperEntity

@Dao
interface WallhavenSearchQueryWallpapersDao : SearchQueryWallpapersDao {
    @Query("SELECT * FROM wallhaven_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun getBySearchQueryId(searchQueryId: Long): List<WallhavenSearchQueryWallpaperEntity>

    @Query(
        """
            SELECT MAX(COALESCE(wsqw.`order`, ww.ROWID))
            FROM wallhaven_wallpapers ww
            INNER JOIN wallhaven_search_query_wallpapers wsqw
                ON ww.id = wsqw.wallpaper_id
            WHERE wsqw.search_query_id = :searchQueryId
        """,
    )
    suspend fun getMaxOrderBySearchQueryId(searchQueryId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg searchQueryWallpaper: WallhavenSearchQueryWallpaperEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchQueryWallpapers: Collection<WallhavenSearchQueryWallpaperEntity>)

    @Query("DELETE FROM wallhaven_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
