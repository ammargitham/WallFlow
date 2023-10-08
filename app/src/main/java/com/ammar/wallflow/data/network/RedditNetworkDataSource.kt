package com.ammar.wallflow.data.network

import com.ammar.wallflow.data.network.model.reddit.NetworkRedditSearchResponse
import com.ammar.wallflow.model.search.RedditSearchQuery

interface RedditNetworkDataSource {
    suspend fun search(
        searchQuery: RedditSearchQuery,
        after: String? = null,
    ): NetworkRedditSearchResponse
}
