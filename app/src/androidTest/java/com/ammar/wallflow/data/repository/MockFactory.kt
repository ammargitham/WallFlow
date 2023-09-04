package com.ammar.wallflow.data.repository

import com.ammar.wallflow.data.network.model.NetworkWallhavenThumbs
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaper
import com.ammar.wallflow.extensions.toHexString
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlinx.datetime.Clock

object MockFactory {
    fun generateNetworkWallpapers(size: Int = 10) = List(size) { generateNetworkWallpaper() }

    fun generateNetworkWallpaper(
        idNumber: Int = Random.nextInt().absoluteValue,
    ) = NetworkWallhavenWallpaper(
        id = "wallpaper${idNumber + 1}",
        url = "https://example.com/wallpaper${idNumber + 1}",
        short_url = "https://example.com/w${idNumber + 1}",
        views = 1000,
        favorites = 50,
        source = "https://example.com/source${idNumber + 1}",
        purity = "sfw",
        category = "landscape",
        dimension_x = 1920,
        dimension_y = 1080,
        ratio = 1920f / 1080,
        resolution = "1920 x 1080",
        file_size = 1000000,
        file_type = "jpg",
        created_at = Clock.System.now(),
        colors = List(Random.nextInt(5)) { RandomColors.nextColor().toHexString() },
        path = "wallpapers/wallpaper${idNumber + 1}.jpg",
        thumbs = NetworkWallhavenThumbs(
            large = "https://example.com/wallpaper${idNumber + 1}/large",
            original = "https://example.com/wallpaper${idNumber + 1}/original",
            small = "https://example.com/wallpaper${idNumber + 1}/small",
        ),
    )
}
