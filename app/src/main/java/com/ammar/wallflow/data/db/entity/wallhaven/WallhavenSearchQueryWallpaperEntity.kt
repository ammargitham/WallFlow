package com.ammar.wallflow.data.db.entity.wallhaven

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.ammar.wallflow.data.db.entity.WallhavenSearchQueryEntity

@Entity(
    tableName = "wallhaven_search_query_wallpapers",
    primaryKeys = [
        "search_query_id",
        "wallpaper_id",
    ],
    foreignKeys = [
        ForeignKey(
            entity = WallhavenSearchQueryEntity::class,
            parentColumns = ["id"],
            childColumns = ["search_query_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WallhavenWallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaper_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["wallpaper_id"]), // need extra to remove warning
    ],
)
data class WallhavenSearchQueryWallpaperEntity(
    @ColumnInfo(name = "search_query_id") val searchQueryId: Long,
    @ColumnInfo(name = "wallpaper_id") val wallpaperId: Long,
)
