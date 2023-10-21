package com.ammar.wallflow.data.network.model.reddit

import kotlinx.serialization.Serializable

@Serializable
data class NetworkRedditData(
    val after: String? = null,
    val children: List<NetworkRedditDataChild>,
)

@Serializable
data class NetworkRedditDataChild(
    val data: NetworkRedditPost,
)
