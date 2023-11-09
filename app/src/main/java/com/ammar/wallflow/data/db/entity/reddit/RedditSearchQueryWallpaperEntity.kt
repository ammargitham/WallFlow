package com.ammar.wallflow.data.db.entity.reddit

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.ammar.wallflow.data.db.entity.search.SearchQueryEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity

@Entity(
    tableName = "reddit_search_query_wallpapers",
    primaryKeys = [
        "search_query_id",
        "wallpaper_id",
    ],
    foreignKeys = [
        ForeignKey(
            entity = SearchQueryEntity::class,
            parentColumns = ["id"],
            childColumns = ["search_query_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RedditWallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaper_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["wallpaper_id"]), // need extra to remove warning
    ],
)
data class RedditSearchQueryWallpaperEntity(
    @ColumnInfo(name = "search_query_id") val searchQueryId: Long,
    @ColumnInfo(name = "wallpaper_id") val wallpaperId: Long,
    val order: Int? = null,
)
