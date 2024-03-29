package com.ammar.wallflow.data.network.retrofit

import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.retrofit.api.WallhavenNetworkApi
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.toCategoryInt
import com.ammar.wallflow.model.toWallhavenPurityInt

class RetrofitWallhavenNetwork(
    private val wallHavenNetworkApi: WallhavenNetworkApi,
) : WallhavenNetworkDataSource {

    override suspend fun search(
        search: WallhavenSearch,
        page: Int?,
    ) = with(search.filters) {
        wallHavenNetworkApi.search(
            query = search.getApiQueryString(),
            categories = categories.toCategoryInt().toString().padStart(3, '0'),
            purity = purity.toWallhavenPurityInt().toString().padStart(3, '0'),
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

    override suspend fun wallpaper(wallpaperWallhavenId: String) = wallHavenNetworkApi.wallpaper(
        wallpaperWallhavenId,
    )

    override suspend fun popularTags() = wallHavenNetworkApi.popularTags()
}
