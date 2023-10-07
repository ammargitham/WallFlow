package com.ammar.wallflow.data.network.model.wallhaven

import kotlinx.serialization.Serializable

@Serializable
data class NetworkWallhavenWallpapersResponse(
    val data: List<NetworkWallhavenWallpaper>,
    val meta: NetworkWallhavenMeta? = null,
)

@Serializable
data class NetworkWallhavenWallpaperResponse(
    val data: NetworkWallhavenWallpaper,
)
