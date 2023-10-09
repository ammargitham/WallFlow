package com.ammar.wallflow.model.search

import com.ammar.wallflow.extensions.fromQueryString
import com.ammar.wallflow.extensions.toQueryString
import kotlinx.serialization.Serializable

@Serializable
data class RedditSearch(
    val query: String = "",
    val subreddits: Set<String>,
    val includeNsfw: Boolean,
    val sort: RedditSort,
    val timeRange: RedditTimeRange,
) {
    fun toQueryString() = mapOf(
        "query" to query,
        "subreddits" to subreddits.joinToString(","),
        "includeNsfw" to includeNsfw.toString(),
        "sort" to sort.value,
        "timeRange" to timeRange.value,
    ).toQueryString()

    companion object {
        fun fromQueryString(string: String?): RedditSearch? {
            if (string == null) {
                return null
            }
            val map = string.fromQueryString()
            return RedditSearch(
                query = map["query"] ?: "",
                subreddits = map["subreddits"]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: emptySet(),
                includeNsfw = map["includeNsfw"]?.toBoolean() ?: false,
                sort = map["sort"]?.let {
                    RedditSort.fromValue(it)
                } ?: RedditSort.RELEVANCE,
                timeRange = map["timeRange"]?.let {
                    RedditTimeRange.fromValue(it)
                } ?: RedditTimeRange.ALL,
            )
        }
    }
}

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
