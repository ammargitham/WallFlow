package com.ammar.wallflow.data.network

import com.ammar.wallflow.data.network.model.reddit.NetworkRedditSearchResponse
import com.ammar.wallflow.model.search.RedditSearch

interface RedditNetworkDataSource : OnlineSourceNetworkDataSource {
    suspend fun search(
        search: RedditSearch,
        after: String? = null,
    ): NetworkRedditSearchResponse
}
