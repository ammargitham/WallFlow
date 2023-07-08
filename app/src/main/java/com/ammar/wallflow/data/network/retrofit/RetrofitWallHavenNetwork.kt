package com.ammar.wallflow.data.network.retrofit

import com.ammar.wallflow.data.network.WallHavenNetworkDataSource
import com.ammar.wallflow.data.network.retrofit.api.WallHavenNetworkApi
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.toCategoryInt
import com.ammar.wallflow.model.toPurityInt

class RetrofitWallHavenNetwork constructor(
    private val wallHavenNetworkApi: WallHavenNetworkApi,
) : WallHavenNetworkDataSource {

    override suspend fun search(searchQuery: SearchQuery, page: Int?) = with(searchQuery) {
        wallHavenNetworkApi.search(
            query = searchQuery.getQString(),
            categories = categories.toCategoryInt().toString().padStart(3, '0'),
            purity = purity.toPurityInt().toString().padStart(3, '0'),
            sorting = sorting.value,
            order = order.value,
            topRange = topRange.value,
            atleast = atleast?.toString()?.replace(" ", ""),
            resolutions = resolutions.joinToString(",") { "${it.width}x${it.height}" },
            colors = colors?.toHexString()?.removePrefix("#"),
            ratios = ratios.joinToString(",") { it.toRatioString().replace(" ", "") },
            page = page,
            seed = seed,
        )
    }

    override suspend fun wallpaper(wallpaperWallhavenId: String) =
        wallHavenNetworkApi.wallpaper(wallpaperWallhavenId)

    override suspend fun popularTags() = wallHavenNetworkApi.popularTags()
}
