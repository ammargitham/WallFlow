package com.ammar.wallflow.data.repository

import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMeta
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaper
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.data.network.retrofit.api.WallhavenNetworkApi
import java.io.IOException

class FakeWallhavenNetworkApi : WallhavenNetworkApi {
    var failureMsg: String? = null
    private val wallpaperMap = mutableMapOf<String, List<NetworkWallhavenWallpaper>>()
    private val metaMap = mutableMapOf<String, NetworkWallhavenMeta?>()

    override suspend fun search(
        query: String?,
        categories: String?,
        purity: String?,
        sorting: String?,
        order: String?,
        topRange: String?,
        atleast: String?,
        resolutions: String?,
        colors: String?,
        ratios: String?,
        page: Int?,
        seed: String?,
    ): NetworkWallhavenWallpapersResponse {
        failureMsg?.run { throw IOException(this) }
        return NetworkWallhavenWallpapersResponse(
            data = wallpaperMap.getOrDefault(query, emptyList()),
            meta = metaMap[query],
        )
    }

    override suspend fun popularTags() = throw RuntimeException()

    override suspend fun wallpaper(id: String) = throw RuntimeException()

    fun setWallpapersForQuery(
        query: String,
        networkWallhavenWallpapers: List<NetworkWallhavenWallpaper>,
        meta: NetworkWallhavenMeta? = null,
    ) {
        wallpaperMap[query] = networkWallhavenWallpapers
        metaMap[query] = meta
    }

    fun clearMockData() {
        wallpaperMap.clear()
        metaMap.clear()
    }
}
