package com.ammar.wallflow.model.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RedditFilters")
data class RedditFilters(
    val subreddits: Set<String>,
    val includeNsfw: Boolean,
    val sort: RedditSort,
    val timeRange: RedditTimeRange,
) : Filters()

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
