package com.ammar.havenwalls.data.network.retrofit

import com.ammar.havenwalls.data.network.WallHavenNetworkDataSource
import com.ammar.havenwalls.data.network.retrofit.api.WallHavenNetworkApi
import com.ammar.havenwalls.extensions.toHexString
import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.model.toCategoryInt
import com.ammar.havenwalls.model.toPurityInt

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
            atleast = atleast?.toString(),
            resolutions = resolutions.joinToString(","),
            colors = colors?.toHexString()?.removePrefix("#"),
            ratios = ratios.joinToString(",") { it.toRatioString() },
            page = page,
            seed = seed,
        )
    }

    override suspend fun wallpaper(wallpaperWallhavenId: String) =
        wallHavenNetworkApi.wallpaper(wallpaperWallhavenId)

    override suspend fun popularTags() = wallHavenNetworkApi.popularTags()
}
