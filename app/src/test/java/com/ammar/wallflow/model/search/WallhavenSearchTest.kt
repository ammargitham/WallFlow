package com.ammar.wallflow.model.search

import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.utils.TestClock
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import org.junit.Test

class WallhavenSearchTest {
    @Test
    fun `should convert WallhavenSearch to qs`() {
        var search = WallhavenSearch(
            query = "test",
            filters = WallhavenFilters(includedTags = setOf("test")),
            meta = null,
        )
        var searchStr = "{\"query\":\"test\",\"filters\":{\"includedTags\":[\"test\"]}}"
        assertEquals(
            searchStr,
            search.toQueryString(),
        )

        val createdAt = TestClock(now = Instant.fromEpochSeconds(1696860179))
        search = WallhavenSearch(
            query = "test",
            filters = WallhavenFilters(includedTags = setOf("test")),
            meta = WallhavenTagSearchMeta(
                tag = WallhavenTag(
                    id = 1,
                    name = "test",
                    alias = emptyList(),
                    categoryId = 1,
                    category = "test_cat",
                    purity = Purity.SFW,
                    createdAt = createdAt.now(),
                ),
            ),
        )
        searchStr = "{\"query\":\"test\",\"filters\":{\"includedTags\":[\"test\"]}," +
            "\"meta\":{\"type\":\"WallhavenTagSearchMeta\"," +
            "\"tag\":{\"id\":1,\"name\":\"test\",\"alias\":[],\"categoryId\":1," +
            "\"category\":\"test_cat\",\"purity\":\"SFW\",\"createdAt\":\"2023-10-09T14:02:59Z\"}}}"
        assertEquals(
            searchStr,
            search.toQueryString(),
        )

        search = WallhavenSearch(
            query = "",
            filters = WallhavenFilters(includedTags = setOf("test")),
            meta = null,
        )
        // backwards compat
        searchStr = "includedTags=test&excludedTags=&username=&tagId=&wallpaperId=" +
            "&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=date_added&order=desc" +
            "&topRange=1M&atleast=&resolutions=&ratios=&colors=&seed="
        assertEquals(
            searchStr,
            search.toQueryString(
                backwardsCompat = true,
            ),
        )
    }

    @Test
    fun `should convert qs to WallhavenSearch`() {
        var searchStr = "{\"query\":\"test\",\"filters\":{\"includedTags\":[\"test\"]}}"
        var search = WallhavenSearch(
            query = "test",
            filters = WallhavenFilters(includedTags = setOf("test")),
            meta = null,
        )
        assertEquals(
            search,
            WallhavenSearch.fromQueryString(searchStr),
        )

        val createdAt = TestClock(now = Instant.fromEpochSeconds(1696860179))
        searchStr = "{\"query\":\"test\",\"filters\":{\"includedTags\":[\"test\"]}," +
            "\"meta\":{\"type\":\"WallhavenTagSearchMeta\"," +
            "\"tag\":{\"id\":1,\"name\":\"test\",\"alias\":[],\"categoryId\":1," +
            "\"category\":\"test_cat\",\"purity\":\"SFW\",\"createdAt\":\"2023-10-09T14:02:59Z\"}}}"
        search = WallhavenSearch(
            query = "test",
            filters = WallhavenFilters(includedTags = setOf("test")),
            meta = WallhavenTagSearchMeta(
                tag = WallhavenTag(
                    id = 1,
                    name = "test",
                    alias = emptyList(),
                    categoryId = 1,
                    category = "test_cat",
                    purity = Purity.SFW,
                    createdAt = createdAt.now(),
                ),
            ),
        )
        assertEquals(
            search,
            WallhavenSearch.fromQueryString(searchStr),
        )

        // backwards compat
        searchStr = "includedTags=test&excludedTags=&username=&tagId=&wallpaperId=" +
            "&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=date_added&order=desc" +
            "&topRange=1M&atleast=&resolutions=&ratios=&colors=&seed="
        search = WallhavenSearch(
            query = "",
            filters = WallhavenFilters(includedTags = setOf("test")),
            meta = null,
        )
        assertEquals(
            search,
            WallhavenSearch.fromQueryString(searchStr),
        )
    }

    @Test
    fun `getApiQueryString should match prev WallhavenFilters#getQString`() {
        val search = WallhavenSearch(
            query = "test",
            filters = WallhavenFilters(
                includedTags = setOf("test2"),
                excludedTags = setOf("test3"),
                username = "user",
                tagId = 100,
                wallpaperId = "new_wall",
            ),
        )
        val expectedQuery = "+test2 +test -test3 @user id:100 like:new_wall"
        assertEquals(
            expectedQuery,
            search.getApiQueryString(),
        )
    }

    @Test
    fun `convert tags, tagId, etc to ApiQueryString`() {
        var search = WallhavenSearch(
            filters = WallhavenFilters(
                includedTags = setOf("i1", "i2"),
                excludedTags = setOf("e1", "e2"),
            ),
        )
        assertEquals("+i1 +i2 -e1 -e2", search.getApiQueryString())

        search = WallhavenSearch(
            filters = WallhavenFilters(
                includedTags = setOf("i 1", "i 2"),
                excludedTags = setOf("e 1", "e 2"),
            ),
        )
        assertEquals("+\"i 1\" +\"i 2\" -\"e 1\" -\"e 2\"", search.getApiQueryString())

        search = WallhavenSearch(
            filters = WallhavenFilters(
                includedTags = setOf("i1", "i2"),
                excludedTags = setOf("e1", "e2"),
                username = "test",
                tagId = 12L,
                wallpaperId = "xx1234xx",
            ),
        )
        assertEquals("+i1 +i2 -e1 -e2 @test id:12 like:xx1234xx", search.getApiQueryString())
    }

    @Test
    fun `migrate queryString created by WallhavenFilters to WallhavenSearch`() {
        val filtersStr = "includedTags=test&excludedTags=&username=&tagId=&wallpaperId=" +
            "&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=date_added&order=desc" +
            "&topRange=1M&atleast=&resolutions=&ratios=&colors=&seed="
        val expectedStr = "{\"filters\":{\"includedTags\":[\"test\"]}}"
        assertEquals(
            expectedStr,
            migrateWallhavenFiltersQSToWallhavenSearchJson(filtersStr),
        )
    }
}
