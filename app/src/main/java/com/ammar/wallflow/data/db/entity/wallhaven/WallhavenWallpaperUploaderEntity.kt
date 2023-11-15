package com.ammar.wallflow.data.db.entity.wallhaven

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "wallhaven_wallpaper_uploaders",
    primaryKeys = ["wallpaper_id", "uploader_id"],
    indices = [
        Index("wallpaper_id"),
        Index("uploader_id"),
    ],
)
data class WallhavenWallpaperUploaderEntity(
    @ColumnInfo(name = "wallpaper_id") val wallpaperId: Long,
    @ColumnInfo(name = "uploader_id") val uploaderId: Long,
)
