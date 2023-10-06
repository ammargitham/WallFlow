package com.ammar.wallflow

import com.ammar.wallflow.data.network.model.NetworkWallhavenTag
import com.ammar.wallflow.data.network.model.NetworkWallhavenThumbs
import com.ammar.wallflow.data.network.model.NetworkWallhavenUploader
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaper
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.model.SavedSearch
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.wallhaven.WallhavenSearchQuery
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlinx.datetime.Clock

object MockFactory {
    fun generateNetworkWallpapers(
        size: Int = 10,
        random: Random = Random,
        clock: Clock = Clock.System,
    ) = List(size) {
        generateNetworkWallpaper(
            random = random,
            clock = clock,
        )
    }

    fun generateNetworkWallpaper(
        random: Random = Random,
        clock: Clock = Clock.System,
        idNumber: Int = random.nextInt().absoluteValue,
    ): NetworkWallhavenWallpaper {
        val id = idNumber + 1
        return NetworkWallhavenWallpaper(
            id = "wallpaper$id",
            url = "https://example.com/wallpaper$id",
            short_url = "https://example.com/w$id",
            views = 1000,
            favorites = 50,
            source = "https://example.com/source$id",
            purity = "sfw",
            category = "landscape",
            dimension_x = 1920,
            dimension_y = 1080,
            ratio = 1920f / 1080,
            resolution = "1920 x 1080",
            file_size = 1000000,
            file_type = "jpg",
            created_at = clock.now(),
            colors = List(random.nextInt(5)) { RandomColors.nextColor(random).toHexString() },
            path = "wallpapers/wallpaper$id.jpg",
            thumbs = NetworkWallhavenThumbs(
                large = "https://example.com/wallpaper$id/large",
                original = "https://example.com/wallpaper$id/original",
                small = "https://example.com/wallpaper$id/small",
            ),
            uploader = NetworkWallhavenUploader(
                username = "uploader$id",
                group = "test",
                avatar = mapOf(
                    "large" to "https://example.com/uploader$id/large",
                    "medium" to "https://example.com/uploader$id/medium",
                    "small" to "https://example.com/uploader$id/small",
                    "tiny" to "https://example.com/uploader$id/tiny",
                ),
            ),
            tags = List(random.nextInt(5)) {
                NetworkWallhavenTag(
                    id = (it + id).toLong(),
                    name = "tag_${it + id}",
                    alias = "",
                    category_id = (it + id).toLong(),
                    category = "",
                    purity = "sfw",
                    created_at = clock.now(),
                )
            },
        )
    }

    fun generateWallhavenSavedSearches(
        size: Int = 10,
        random: Random = Random,
    ) = List(size) {
        generateWallhavenSavedSearch(
            random = random,
        )
    }

    private fun generateWallhavenSavedSearch(
        random: Random = Random,
        idNumber: Int = random.nextInt().absoluteValue,
    ): SavedSearch {
        val id = idNumber + 1
        return SavedSearch(
            id = id.toLong(),
            name = "saved_search_$id",
            search = Search(
                query = "test_q_$id",
                filters = WallhavenSearchQuery(),
            ),
        )
    }
}
