package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.AutoWallpaperHistory
import kotlinx.datetime.Instant

@Entity(
    tableName = "auto_wallpaper_history",
    indices = [
        Index(
            value = ["wallhaven_id"],
            unique = true,
        )
    ]
)
data class AutoWallpaperHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "wallhaven_id") val wallhavenId: String,
    @ColumnInfo(name = "set_on") val setOn: Instant,
)

fun AutoWallpaperHistoryEntity.toModel() = AutoWallpaperHistory(
    wallhavenId = wallhavenId,
    setOn = setOn,
)
