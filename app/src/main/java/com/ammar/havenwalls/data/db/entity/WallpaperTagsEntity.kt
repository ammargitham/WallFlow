package com.ammar.havenwalls.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "wallpaper_tags",
    primaryKeys = ["wallpaper_id", "tag_id"],
    indices = [
        Index("wallpaper_id"),
        Index("tag_id"),
    ]
)
data class WallpaperTagsEntity(
    @ColumnInfo(name = "wallpaper_id") val wallpaperId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)
