package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity

@Dao
interface AutoWallpaperHistoryDao {
    @Query("SELECT * FROM auto_wallpaper_history ORDER BY set_on")
    suspend fun getAll(): List<AutoWallpaperHistoryEntity>

    @Query("SELECT * FROM auto_wallpaper_history WHERE wallhaven_id = :wallhavenId")
    suspend fun getByWallhavenId(wallhavenId: String): AutoWallpaperHistoryEntity?

    @Upsert
    suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity)
}
