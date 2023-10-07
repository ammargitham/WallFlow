package com.ammar.wallflow.data.network

import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaperResponse
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.model.search.WallhavenSearchQuery
import org.jsoup.nodes.Document

interface WallhavenNetworkDataSource {
    suspend fun search(
        searchQuery: WallhavenSearchQuery,
        page: Int? = null,
    ): NetworkWallhavenWallpapersResponse

    suspend fun wallpaper(wallpaperWallhavenId: String): NetworkWallhavenWallpaperResponse

    suspend fun popularTags(): Document?
}
