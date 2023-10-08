package com.ammar.wallflow.data.network.model.reddit

data class NetworkRedditData(
    val after: String? = null,
    val children: List<NetworkRedditDataChild>,
)

data class NetworkRedditDataChild(
    val data: NetworkRedditPost,
)
