package com.ammar.havenwalls.data.db.entity

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.ammar.havenwalls.data.common.Purity
import com.ammar.havenwalls.data.common.Resolution
import com.ammar.havenwalls.model.Thumbs
import com.ammar.havenwalls.model.Wallpaper
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals

class WallpaperEntityTest {
    @Test
    fun convert_WallpaperEntity_to_Wallpaper() {
        val wallpaperEntity = WallpaperEntity(
            id = 0,
            wallhavenId = "85k6eo",
            url = "https://wallhaven.cc/w/85k6eo",
            shortUrl = "https://whvn.cc/85k6eo",
            uploaderId = null,
            views = 444,
            favorites = 43,
            source = "",
            purity = Purity.SKETCHY,
            category = "people",
            dimensionX = 1429,
            dimensionY = 1031,
            fileSize = 1159446,
            fileType = "image/png",
            createdAt = Instant.parse("2023-04-26T02:41:32Z"),
            colors = listOf("#996633", "#999999", "#cc6633", "#e7d8b1", "#cccccc"),
            path = "https://w.wallhaven.cc/full/85/wallhaven-85k6eo.png",
            thumbs = ThumbsEntity(
                large = "https://th.wallhaven.cc/lg/85/85k6eo.jpg",
                original = "https://th.wallhaven.cc/orig/85/85k6eo.jpg",
                small = "https://th.wallhaven.cc/small/85/85k6eo.jpg"
            ),
        )
        val expected = Wallpaper(
            id = "85k6eo",
            url = "https://wallhaven.cc/w/85k6eo",
            shortUrl = "https://whvn.cc/85k6eo",
            uploader = null,
            views = 444,
            favorites = 43,
            source = "",
            purity = Purity.SKETCHY,
            category = "people",
            resolution = Resolution(1429, 1031),
            fileSize = 1159446,
            fileType = "image/png",
            createdAt = Instant.parse("2023-04-26T02:41:32Z"),
            colors = listOf(
                "#996633",
                "#999999",
                "#cc6633",
                "#e7d8b1",
                "#cccccc"
            ).map { Color(it.toColorInt()) },
            path = "https://w.wallhaven.cc/full/85/wallhaven-85k6eo.png",
            thumbs = Thumbs(
                large = "https://th.wallhaven.cc/lg/85/85k6eo.jpg",
                original = "https://th.wallhaven.cc/orig/85/85k6eo.jpg",
                small = "https://th.wallhaven.cc/small/85/85k6eo.jpg"
            ),
            tags = null,
        )
        assertEquals(
            expected,
            wallpaperEntity.asWallpaper(),
        )
    }
}
