package com.ammar.wallflow.data.network

import com.ammar.wallflow.data.network.model.NetworkResponse
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenSearchQuery
import org.jsoup.nodes.Document

interface WallhavenNetworkDataSource {
    suspend fun search(
        searchQuery: WallhavenSearchQuery,
        page: Int? = null,
    ): NetworkResponse<List<NetworkWallhavenWallpaper>>

    suspend fun wallpaper(wallpaperWallhavenId: String): NetworkResponse<NetworkWallhavenWallpaper>

    suspend fun popularTags(): Document?
}
