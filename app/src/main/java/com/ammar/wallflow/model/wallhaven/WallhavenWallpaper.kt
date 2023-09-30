@file:UseSerializers(ColorSerializer::class, IntSizeSerializer::class)

package com.ammar.wallflow.model.wallhaven

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.data.db.entity.wallhaven.ThumbsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.serializers.ColorSerializer
import com.ammar.wallflow.model.serializers.IntSizeSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.UseSerializers

@Stable
data class WallhavenWallpaper(
    override val source: Source = Source.WALLHAVEN,
    override val id: String,
    override val resolution: IntSize,
    override val fileSize: Long,
    override val data: String,
    override val mimeType: String,
    override val thumbData: String,
    override val purity: Purity,
    val url: String,
    val shortUrl: String,
    val uploader: WallhavenUploader?,
    val views: Int,
    val favorites: Int,
    val wallhavenSource: String,
    val category: String,
    val createdAt: Instant,
    val colors: List<Color>,
    val tags: List<WallhavenTag>?,
) : DownloadableWallpaper()

fun WallhavenWallpaper.toEntity(
    id: Long = 0L,
    uploaderId: Long = 0L,
) = WallhavenWallpaperEntity(
    id = id,
    wallhavenId = this.id,
    url = url,
    shortUrl = shortUrl,
    uploaderId = uploaderId,
    views = views,
    favorites = favorites,
    source = wallhavenSource,
    purity = purity,
    category = category,
    dimensionX = resolution.width,
    dimensionY = resolution.height,
    fileSize = fileSize,
    fileType = mimeType,
    createdAt = createdAt,
    colors = colors.map { it.toHexString() },
    path = data,
    thumbs = ThumbsEntity(
        large = "",
        original = thumbData,
        small = "",
    ),
)

// Examples
val wallhavenWallpaper1 = WallhavenWallpaper(
    id = "1",
    url = "https://example.com/wallpaper1",
    shortUrl = "https://example.com/w1",
    uploader = WallhavenUploader(
        username = "uploader1",
        group = "User",
        avatar = WallhavenAvatar(
            large = "https://example.com/large1",
            medium = "https://example.com/medium1",
            small = "https://example.com/small1",
            tiny = "https://example.com/tiny1",
        ),
    ),
    views = 1000,
    favorites = 50,
    wallhavenSource = "https://example.com/source1",
    purity = Purity.SFW,
    category = "landscape",
    resolution = IntSize(1920, 1080),
    fileSize = 1000000,
    mimeType = "jpg",
    createdAt = Clock.System.now(),
    colors = listOf(
        Color("#e64d19".toColorInt()),
        Color("#575c36".toColorInt()),
    ),
    data = "wallpapers/wallpaper1",
    thumbData = "https://example.com/wallpaper1/original",
    tags = listOf(
        WallhavenTag(
            id = 1,
            name = "tag1",
            alias = listOf("tag1"),
            categoryId = 1,
            category = "category1",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
        WallhavenTag(
            id = 2,
            name = "tag2",
            alias = listOf("tag2"),
            categoryId = 2,
            category = "category2",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
    ),
)

val wallhavenWallpaper2 = WallhavenWallpaper(
    id = "2",
    url = "https://example.com/wallpaper2",
    shortUrl = "https://example.com/w2",
    uploader = WallhavenUploader(
        username = "uploader2",
        group = "User",
        avatar = WallhavenAvatar(
            large = "https://example.com/large2",
            medium = "https://example.com/medium2",
            small = "https://example.com/small2",
            tiny = "https://example.com/tiny2",
        ),
    ),
    views = 500,
    favorites = 20,
    wallhavenSource = "https://example.com/source2",
    purity = Purity.NSFW,
    category = "portrait",
    resolution = IntSize(1080, 1920),
    fileSize = 500000,
    mimeType = "png",
    createdAt = Clock.System.now(),
    colors = listOf(
        Color("#09c081".toColorInt()),
        Color("#2ba4b5".toColorInt()),
    ),
    data = "wallpapers/wallpaper2",
    thumbData = "https://example.com/wallpaper2/original",
    tags = listOf(
        WallhavenTag(
            id = 3,
            name = "tag3",
            alias = listOf("tag3"),
            categoryId = 3,
            category = "category3",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
        WallhavenTag(
            id = 4,
            name = "tag4",
            alias = listOf("tag4"),
            categoryId = 4,
            category = "category4",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
    ),
)
