package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "wallhaven_wallpaper_tags",
    primaryKeys = ["wallpaper_id", "tag_id"],
    indices = [
        Index("wallpaper_id"),
        Index("tag_id"),
    ],
)
data class WallhavenWallpaperTagsEntity(
    @ColumnInfo(name = "wallpaper_id") val wallpaperId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)
