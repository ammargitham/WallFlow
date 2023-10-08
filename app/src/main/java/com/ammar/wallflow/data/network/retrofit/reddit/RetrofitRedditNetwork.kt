package com.ammar.wallflow.data.network.retrofit.reddit

import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.model.search.RedditSearchQuery

class RetrofitRedditNetwork(
    private val redditNetworkApi: RedditNetworkApi,
) : RedditNetworkDataSource {
    override suspend fun search(
        searchQuery: RedditSearchQuery,
        after: String?,
    ) = with(searchQuery) {
        if (subreddit.isBlank()) {
            throw IllegalArgumentException("subreddit cannot be empty")
        }
        redditNetworkApi.search(
            query = "self%3Ano ${query ?: ""}",
            subreddit = subreddit,
            includeNsfw = if (includeNsfw) {
                "on"
            } else {
                "off"
            },
            sort = sort.value,
            timeRange = timeRange.value,
            after = after,
        )
    }
}
