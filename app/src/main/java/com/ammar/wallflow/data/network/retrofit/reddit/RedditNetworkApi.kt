package com.ammar.wallflow.data.network.retrofit.reddit

import com.ammar.wallflow.data.network.model.reddit.NetworkRedditSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RedditNetworkApi {
    @GET("/r/{subreddit}/search.json?restrict_sr=on")
    suspend fun search(
        @Path("subreddit") subreddit: String, // can be multi-reddit
        @Query("q") query: String, // must include self%3Ano (self:no) to filter out text posts
        @Query("include_over_18") includeNsfw: String? = null, // allowed: on, off or null
        @Query("sort") sort: String, // allowed: relevance, new, top, comments
        @Query("t") timeRange: String, // allowed: all, hour, day, week, month, year
        @Query("after") after: String? = null,
    ): NetworkRedditSearchResponse
}
