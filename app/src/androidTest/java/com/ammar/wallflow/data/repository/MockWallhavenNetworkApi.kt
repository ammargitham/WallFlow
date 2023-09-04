package com.ammar.wallflow.data.repository

import com.ammar.wallflow.data.network.model.NetworkResponse
import com.ammar.wallflow.data.network.model.NetworkWallhavenMeta
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaper
import com.ammar.wallflow.data.network.retrofit.api.WallhavenNetworkApi
import java.io.IOException
import org.jsoup.nodes.Document

class MockWallhavenNetworkApi : WallhavenNetworkApi {
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
    ): NetworkResponse<List<NetworkWallhavenWallpaper>> {
        failureMsg?.run { throw IOException(this) }
        return NetworkResponse(
            data = wallpaperMap.getOrDefault(query, emptyList()),
            meta = metaMap[query],
        )
    }

    override suspend fun popularTags(): Document? {
        TODO("Not yet implemented")
    }

    override suspend fun wallpaper(id: String): NetworkResponse<NetworkWallhavenWallpaper> {
        TODO("Not yet implemented")
    }

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
