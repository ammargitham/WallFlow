package com.ammar.wallflow.data.network.model.reddit

import com.ammar.wallflow.data.network.model.OnlineSourceWallpapersNetworkResponse
import kotlinx.serialization.Serializable

@Serializable
data class NetworkRedditSearchResponse(
    val data: NetworkRedditData,
) : OnlineSourceWallpapersNetworkResponse
