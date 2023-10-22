package com.ammar.wallflow.data.network.model.wallhaven

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.model.serializers.InstantSerializer
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class NetworkWallhavenWallpaper(
    val id: String,
    val url: String,
    val short_url: String,
    val uploader: NetworkWallhavenUploader? = null,
    val views: Int,
    val favorites: Int,
    val source: String,
    val purity: String,
    val category: String,
    val dimension_x: Int,
    val dimension_y: Int,
    val resolution: String,
    val ratio: Float,
    val file_size: Long,
    val file_type: String,
    @Serializable(InstantSerializer::class)
    val created_at: Instant,
    val colors: List<String>,
    val path: String,
    val thumbs: NetworkWallhavenThumbs,
    val tags: List<NetworkWallhavenTag>? = null,
)

fun NetworkWallhavenWallpaper.toWallhavenWallpaper() = WallhavenWallpaper(
    id = id,
    url = url,
    shortUrl = short_url,
    views = views,
    uploader = uploader?.toWallhavenUploader(),
    favorites = favorites,
    wallhavenSource = source,
    purity = Purity.fromName(purity),
    category = category,
    resolution = IntSize(dimension_x, dimension_y),
    fileSize = file_size,
    mimeType = file_type,
    createdAt = created_at,
    colors = colors.map { Color(it.toColorInt()) },
    data = path,
    thumbData = thumbs.original,
    tags = tags?.map { it.toWallhavenTag() },
)

fun NetworkWallhavenWallpaper.toWallpaperEntity(
    id: Long = 0,
    uploaderId: Long? = null,
) = WallhavenWallpaperEntity(
    id = id,
    wallhavenId = this.id,
    url = url,
    uploaderId = uploaderId,
    shortUrl = short_url,
    views = views,
    favorites = favorites,
    source = source,
    purity = Purity.fromName(purity),
    category = category,
    dimensionX = dimension_x,
    dimensionY = dimension_y,
    fileSize = file_size,
    fileType = file_type,
    createdAt = created_at,
    colors = colors,
    path = path,
    thumbs = thumbs.asThumbsEntity(),
)
