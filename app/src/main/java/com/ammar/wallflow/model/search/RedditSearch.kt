package com.ammar.wallflow.model.search

import kotlinx.serialization.Serializable

@Serializable
data class RedditSearch(
    val query: String = "",
    val subreddits: Set<String>,
    val includeNsfw: Boolean,
    val sort: RedditSort,
    val timeRange: RedditTimeRange,
)

enum class RedditSort(val value: String) {
    RELEVANCE("relevance"),
    NEW("new"),
    TOP("top"),
    COMMENTS("comments"),
    ;

    companion object {
        fun fromValue(value: String) = when (value) {
            "relevance" -> RELEVANCE
            "new" -> NEW
            "top" -> TOP
            "comments" -> COMMENTS
            else -> null
        }
    }
}

enum class RedditTimeRange(val value: String) {
    ALL("all"),
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ;

    companion object {
        fun fromValue(value: String) = when (value) {
            "all" -> ALL
            "hour" -> HOUR
            "day" -> DAY
            "week" -> WEEK
            "month" -> MONTH
            "year" -> YEAR
            else -> null
        }
    }
}
