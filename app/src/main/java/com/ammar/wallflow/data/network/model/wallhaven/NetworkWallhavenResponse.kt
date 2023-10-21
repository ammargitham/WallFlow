package com.ammar.wallflow.data.network.model.wallhaven

import com.ammar.wallflow.data.network.model.OnlineSourceWallpapersNetworkResponse
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWallhavenWallpapersResponse(
    val data: List<NetworkWallhavenWallpaper>,
    val meta: NetworkWallhavenMeta? = null,
) : OnlineSourceWallpapersNetworkResponse

@Serializable
data class NetworkWallhavenWallpaperResponse(
    val data: NetworkWallhavenWallpaper,
)
