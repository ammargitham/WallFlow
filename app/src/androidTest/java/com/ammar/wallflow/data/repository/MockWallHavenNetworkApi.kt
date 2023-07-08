package com.ammar.wallflow.data.repository

import com.ammar.wallflow.data.network.model.NetworkMeta
import com.ammar.wallflow.data.network.model.NetworkResponse
import com.ammar.wallflow.data.network.model.NetworkWallpaper
import com.ammar.wallflow.data.network.retrofit.api.WallHavenNetworkApi
import java.io.IOException
import org.jsoup.nodes.Document

class MockWallHavenNetworkApi : WallHavenNetworkApi {
    var failureMsg: String? = null
    private val wallpaperMap = mutableMapOf<String, List<NetworkWallpaper>>()
    private val metaMap = mutableMapOf<String, NetworkMeta?>()

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
    ): NetworkResponse<List<NetworkWallpaper>> {
        failureMsg?.run { throw IOException(this) }
        return NetworkResponse(
            data = wallpaperMap.getOrDefault(query, emptyList()),
            meta = metaMap[query],
        )
    }

    override suspend fun popularTags(): Document? {
        TODO("Not yet implemented")
    }

    override suspend fun wallpaper(id: String): NetworkResponse<NetworkWallpaper> {
        TODO("Not yet implemented")
    }

    fun setWallpapersForQuery(
        query: String,
        networkWallpapers: List<NetworkWallpaper>,
        meta: NetworkMeta? = null,
    ) {
        wallpaperMap[query] = networkWallpapers
        metaMap[query] = meta
    }

    fun clearMockData() {
        wallpaperMap.clear()
        metaMap.clear()
    }
}
