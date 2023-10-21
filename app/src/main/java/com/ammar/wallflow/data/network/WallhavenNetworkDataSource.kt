package com.ammar.wallflow.data.network

import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaperResponse
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.model.search.WallhavenSearch
import org.jsoup.nodes.Document

interface WallhavenNetworkDataSource : OnlineSourceNetworkDataSource {
    suspend fun search(
        search: WallhavenSearch,
        page: Int?,
    ): NetworkWallhavenWallpapersResponse

    suspend fun wallpaper(wallpaperWallhavenId: String): NetworkWallhavenWallpaperResponse

    suspend fun popularTags(): Document?
}
