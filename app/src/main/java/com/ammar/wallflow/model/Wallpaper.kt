@file:UseSerializers(ColorSerializer::class, IntSizeSerializer::class)

package com.ammar.wallflow.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.model.serializers.ColorSerializer
import com.ammar.wallflow.model.serializers.IntSizeSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.UseSerializers

@kotlinx.serialization.Serializable
data class Wallpaper(
    val id: String,
    val url: String,
    val shortUrl: String,
    val uploader: Uploader?,
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
    val thumbs: Thumbs,
    val tags: List<Tag>?,
)

@kotlinx.serialization.Serializable
data class Thumbs(
    val large: String,
    val original: String,
    val small: String,
)

// Examples
val wallpaper1 = Wallpaper(
    id = "1",
    url = "https://example.com/wallpaper1",
    shortUrl = "https://example.com/w1",
    uploader = Uploader(
        username = "uploader1",
        group = "User",
        avatar = Avatar(
            large = "https://example.com/large1",
            medium = "https://example.com/medium1",
            small = "https://example.com/small1",
            tiny = "https://example.com/tiny1",
        )
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
    thumbs = Thumbs(
        large = "https://example.com/wallpaper1/large",
        original = "https://example.com/wallpaper1/original",
        small = "https://example.com/wallpaper1/small"
    ),
    tags = listOf(
        Tag(
            id = 1,
            name = "tag1",
            alias = listOf("tag1"),
            categoryId = 1,
            category = "category1",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
        Tag(
            id = 2,
            name = "tag2",
            alias = listOf("tag2"),
            categoryId = 2,
            category = "category2",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
    )
)

val wallpaper2 = Wallpaper(
    id = "2",
    url = "https://example.com/wallpaper2",
    shortUrl = "https://example.com/w2",
    uploader = Uploader(
        username = "uploader2",
        group = "User",
        avatar = Avatar(
            large = "https://example.com/large2",
            medium = "https://example.com/medium2",
            small = "https://example.com/small2",
            tiny = "https://example.com/tiny2",
        )
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
    thumbs = Thumbs(
        large = "https://example.com/wallpaper2/large",
        original = "https://example.com/wallpaper2/original",
        small = "https://example.com/wallpaper2/small"
    ),
    tags = listOf(
        Tag(
            id = 3,
            name = "tag3",
            alias = listOf("tag3"),
            categoryId = 3,
            category = "category3",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
        Tag(
            id = 4,
            name = "tag4",
            alias = listOf("tag4"),
            categoryId = 4,
            category = "category4",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        ),
    )
)
