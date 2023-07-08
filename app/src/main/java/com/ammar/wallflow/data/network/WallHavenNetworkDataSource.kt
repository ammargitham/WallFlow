package com.ammar.wallflow.data.network

import com.ammar.wallflow.data.network.model.NetworkResponse
import com.ammar.wallflow.data.network.model.NetworkWallpaper
import com.ammar.wallflow.model.SearchQuery
import org.jsoup.nodes.Document

interface WallHavenNetworkDataSource {
    suspend fun search(
        searchQuery: SearchQuery,
        page: Int? = null,
    ): NetworkResponse<List<NetworkWallpaper>>

    suspend fun wallpaper(wallpaperWallhavenId: String): NetworkResponse<NetworkWallpaper>

    suspend fun popularTags(): Document?
}
