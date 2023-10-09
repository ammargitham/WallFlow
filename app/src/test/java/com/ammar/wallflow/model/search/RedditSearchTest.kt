package com.ammar.wallflow.model.search

import kotlin.test.assertEquals
import org.junit.Test

class RedditSearchTest {
    @Test
    fun `should convert reddit search to query string`() {
        var search = RedditSearch(
            query = "",
            subreddits = setOf(
                "test",
                "test1",
            ),
            includeNsfw = false,
            sort = RedditSort.RELEVANCE,
            timeRange = RedditTimeRange.ALL,
        )
        assertEquals(
            "query=&subreddits=test%2Ctest1&includeNsfw=false&sort=relevance&timeRange=all",
            search.toQueryString(),
        )
        search = RedditSearch(
            query = "test",
            subreddits = setOf(
                "test",
            ),
            includeNsfw = true,
            sort = RedditSort.COMMENTS,
            timeRange = RedditTimeRange.HOUR,
        )
        assertEquals(
            "query=test&subreddits=test&includeNsfw=true&sort=comments&timeRange=hour",
            search.toQueryString(),
        )
        search = RedditSearch(
            query = "test test1",
            subreddits = setOf(
                "test",
            ),
            includeNsfw = true,
            sort = RedditSort.COMMENTS,
            timeRange = RedditTimeRange.HOUR,
        )
        assertEquals(
            "query=test+test1&subreddits=test&includeNsfw=true&sort=comments&timeRange=hour",
            search.toQueryString(),
        )
    }

    @Test
    fun `should convert query string to reddit search`() {
        var qs = "query=&subreddits=test%2Ctest1&includeNsfw=false&sort=relevance&timeRange=all"
        var expected = RedditSearch(
            query = "",
            subreddits = setOf(
                "test",
                "test1",
            ),
            includeNsfw = false,
            sort = RedditSort.RELEVANCE,
            timeRange = RedditTimeRange.ALL,
        )
        assertEquals(
            expected,
            RedditSearch.fromQueryString(qs),
        )

        qs = "query=test&subreddits=test&includeNsfw=true&sort=comments&timeRange=hour"
        expected = RedditSearch(
            query = "test",
            subreddits = setOf(
                "test",
            ),
            includeNsfw = true,
            sort = RedditSort.COMMENTS,
            timeRange = RedditTimeRange.HOUR,
        )
        assertEquals(
            expected,
            RedditSearch.fromQueryString(qs),
        )

        qs = "query=test+test1&subreddits=test&includeNsfw=true&sort=comments&timeRange=hour"
        expected = RedditSearch(
            query = "test test1",
            subreddits = setOf(
                "test",
            ),
            includeNsfw = true,
            sort = RedditSort.COMMENTS,
            timeRange = RedditTimeRange.HOUR,
        )
        assertEquals(
            expected,
            RedditSearch.fromQueryString(qs),
        )
    }
}
