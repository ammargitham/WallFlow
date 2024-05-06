package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice

@Dao
interface AutoWallpaperHistoryDao {
    @Query("SELECT * FROM auto_wallpaper_history ORDER BY set_on")
    suspend fun getAll(): List<AutoWallpaperHistoryEntity>

    @Query("SELECT * FROM auto_wallpaper_history WHERE source = :source ORDER BY set_on")
    suspend fun getAllBySource(source: Source): List<AutoWallpaperHistoryEntity>

    @Query(
        """
        SELECT *
        FROM auto_wallpaper_history
        WHERE source_choice = :sourceChoice
        ORDER BY set_on
        """,
    )
    suspend fun getAllBySourceChoice(sourceChoice: SourceChoice): List<AutoWallpaperHistoryEntity>

    @Query(
        """
        SELECT source_id
        FROM auto_wallpaper_history
        WHERE source_choice = :sourceChoice
        ORDER BY set_on
        """,
    )
    suspend fun getAllSourceIdsBySourceChoice(sourceChoice: SourceChoice): List<String>

    @Query(
        """
        SELECT source_id
        FROM auto_wallpaper_history
        WHERE
            source_choice = :sourceChoice
            AND source_id NOT IN (:excludedSourceIds)
        ORDER BY set_on
        LIMIT 1
        """,
    )
    suspend fun getOldestSetOnSourceIdBySourceChoiceAndSourceIdNotIn(
        sourceChoice: SourceChoice,
        excludedSourceIds: Collection<String>,
    ): String?

    @Query("SELECT * FROM auto_wallpaper_history WHERE source_id = :sourceId AND source = :source")
    suspend fun getBySourceId(
        sourceId: String,
        source: Source,
    ): AutoWallpaperHistoryEntity?

    @Upsert
    suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity)

    @Query(
        """
        DELETE FROM auto_wallpaper_history
        WHERE
            source_choice = :sourceChoice
            AND source_id in (:sourceIds)
        """,
    )
    suspend fun deleteBySourceIdsAndSourceChoice(
        sourceIds: Collection<String>,
        sourceChoice: SourceChoice,
    )
}
