package com.ammar.wallflow.data.db.entity.wallpaper

import androidx.compose.ui.unit.IntSize
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.reddit.RedditWallpaper
import kotlinx.serialization.Serializable

@Entity(
    tableName = "reddit_wallpapers",
    indices = [
        Index(
            value = ["reddit_id"],
            unique = true,
        ),
    ],
)
@Serializable
data class RedditWallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "reddit_id") val redditId: String,
    val subreddit: String,
    @ColumnInfo(name = "post_id") val postId: String,
    @ColumnInfo(name = "post_title") val postTitle: String,
    @ColumnInfo(name = "post_url") val postUrl: String,
    val purity: Purity,
    val url: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    val width: Int,
    val height: Int,
    val author: String,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "gallery_pos") val galleryPosition: Int? = null,
) : OnlineSourceWallpaperEntity

fun RedditWallpaperEntity.toWallpaper() = RedditWallpaper(
    id = redditId,
    data = url,
    resolution = IntSize(width, height),
    mimeType = mimeType,
    thumbData = thumbnailUrl,
    purity = purity,
    subreddit = subreddit,
    postId = postId,
    postTitle = postTitle,
    postUrl = postUrl,
    author = author,
    galleryPosition = galleryPosition,
)
