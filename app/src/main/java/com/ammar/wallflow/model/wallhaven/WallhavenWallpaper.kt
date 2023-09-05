@file:UseSerializers(ColorSerializer::class, IntSizeSerializer::class)

package com.ammar.wallflow.model.wallhaven

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.serializers.ColorSerializer
import com.ammar.wallflow.model.serializers.IntSizeSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.UseSerializers

@Stable
@kotlinx.serialization.Serializable
data class WallhavenWallpaper(
    val id: String,
    val url: String,
    val shortUrl: String,
    val uploader: WallhavenUploader?,
    val views: Int,
    val favorites: Int,
    val source: String,
    val purity: Purity,
    val category: String,
    val resolution: IntSize,
    val fileSize: Long,
    val fileType: String,
    val createdAt: Instant,
    val colors: List<Color>,
    val path: String,
    val thumbs: WallhavenThumbs,
    val tags: List<WallhavenTag>?,
)

@kotlinx.serialization.Serializable
data class WallhavenThumbs(
    val large: String,
    val original: String,
    val small: String,
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
    source = "https://example.com/source1",
    purity = Purity.SFW,
    category = "landscape",
    resolution = IntSize(1920, 1080),
    fileSize = 1000000,
    fileType = "jpg",
    createdAt = Clock.System.now(),
    colors = listOf(
        Color("#e64d19".toColorInt()),
        Color("#575c36".toColorInt()),
    ),
    path = "wallpapers/wallpaper1",
    thumbs = WallhavenThumbs(
        large = "https://example.com/wallpaper1/large",
        original = "https://example.com/wallpaper1/original",
        small = "https://example.com/wallpaper1/small",
    ),
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
    source = "https://example.com/source2",
    purity = Purity.NSFW,
    category = "portrait",
    resolution = IntSize(1080, 1920),
    fileSize = 500000,
    fileType = "png",
    createdAt = Clock.System.now(),
    colors = listOf(
        Color("#09c081".toColorInt()),
        Color("#2ba4b5".toColorInt()),
    ),
    path = "wallpapers/wallpaper2",
    thumbs = WallhavenThumbs(
        large = "https://example.com/wallpaper2/large",
        original = "https://example.com/wallpaper2/original",
        small = "https://example.com/wallpaper2/small",
    ),
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
