package com.ammar.wallflow.data.db.dao.wallhaven

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryWallpaperEntity

@Dao
interface WallhavenSearchQueryWallpapersDao {
    @Query("SELECT * FROM wallhaven_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun getBySearchQueryId(searchQueryId: Long): List<WallhavenSearchQueryWallpaperEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg searchQueryWallpaper: WallhavenSearchQueryWallpaperEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchQueryWallpapers: Collection<WallhavenSearchQueryWallpaperEntity>)

    @Query("DELETE FROM wallhaven_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
