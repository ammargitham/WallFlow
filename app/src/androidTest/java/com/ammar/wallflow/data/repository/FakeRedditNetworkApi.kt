package com.ammar.wallflow.data.repository

import com.ammar.wallflow.data.network.model.reddit.NetworkRedditData
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditDataChild
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditPost
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditSearchResponse
import com.ammar.wallflow.data.network.retrofit.reddit.RedditNetworkApi
import java.io.IOException

class FakeRedditNetworkApi : RedditNetworkApi {
    var failureMsg: String? = null
    private val postMap = mutableMapOf<String, List<NetworkRedditPost>>()
    private val afterMap = mutableMapOf<String, String?>()

    override suspend fun search(
        subreddit: String,
        query: String,
        includeNsfw: String?,
        sort: String,
        timeRange: String,
        after: String?,
    ): NetworkRedditSearchResponse {
        failureMsg?.run { throw IOException(this) }
        return NetworkRedditSearchResponse(
            data = NetworkRedditData(
                after = afterMap[query],
                children = postMap.getOrDefault(
                    query,
                    emptyList(),
                ).map {
                    NetworkRedditDataChild(
                        data = it,
                    )
                },
            ),
        )
    }

    fun setPostsForQuery(
        query: String,
        networkRedditPosts: List<NetworkRedditPost>,
        after: String? = null,
    ) {
        postMap[query] = networkRedditPosts
        afterMap[query] = after
    }

    fun clearFakeData() {
        postMap.clear()
        afterMap.clear()
    }
}
