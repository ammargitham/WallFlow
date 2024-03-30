package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.AutoWallpaperHistory
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import kotlinx.datetime.Instant

@Entity(
    tableName = "auto_wallpaper_history",
)
data class AutoWallpaperHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val source: Source,
    @ColumnInfo(
        name = "source_choice",
        defaultValue = "SAVED_SEARCH",
    )
    val sourceChoice: SourceChoice,
    @ColumnInfo(name = "set_on") val setOn: Instant,
    var targets: Set<WallpaperTarget>? = null,
)

fun AutoWallpaperHistoryEntity.toModel() = AutoWallpaperHistory(
    sourceId = sourceId,
    source = source,
    sourceChoice = sourceChoice,
    setOn = setOn,
    targets = targets,
)
