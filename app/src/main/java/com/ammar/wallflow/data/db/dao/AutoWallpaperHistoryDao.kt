package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.model.Source

@Dao
interface AutoWallpaperHistoryDao {
    @Query("SELECT * FROM auto_wallpaper_history ORDER BY set_on")
    suspend fun getAll(): List<AutoWallpaperHistoryEntity>

    @Query("SELECT * FROM auto_wallpaper_history WHERE source = :source ORDER BY set_on")
    suspend fun getAllBySource(source: Source): List<AutoWallpaperHistoryEntity>

    @Query("SELECT * FROM auto_wallpaper_history WHERE source_id = :sourceId AND source = :source")
    suspend fun getBySourceId(
        sourceId: String,
        source: Source,
    ): AutoWallpaperHistoryEntity?

    @Upsert
    suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity)
}
