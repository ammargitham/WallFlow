package com.ammar.havenwalls.data.network

import com.ammar.havenwalls.data.network.model.NetworkResponse
import com.ammar.havenwalls.data.network.model.NetworkWallpaper
import com.ammar.havenwalls.model.SearchQuery
import org.jsoup.nodes.Document

interface WallHavenNetworkDataSource {
    suspend fun search(
        searchQuery: SearchQuery,
        page: Int? = null,
    ): NetworkResponse<List<NetworkWallpaper>>

    suspend fun wallpaper(wallpaperWallhavenId: String): NetworkResponse<NetworkWallpaper>

    suspend fun popularTags(): Document?
}
