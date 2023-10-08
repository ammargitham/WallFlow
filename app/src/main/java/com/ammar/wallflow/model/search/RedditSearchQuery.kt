package com.ammar.wallflow.model.search

data class RedditSearchQuery(
    val query: String? = null,
    val subreddit: String,
    val includeNsfw: Boolean,
    val sort: RedditSort,
    val timeRange: RedditTimeRange,
)

enum class RedditSort(val value: String) {
    RELEVANCE("relevance"),
    NEW("new"),
    TOP("top"),
    COMMENTS("comments"),
}

enum class RedditTimeRange(val value: String) {
    ALL("all"),
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
}
