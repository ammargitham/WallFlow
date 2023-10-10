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
        var searchStr = "query=test&filters=includedTags%3Dtest%26excludedTags%3D%26" +
            "username%3D%26tagId%3D%26wallpaperId%3D%26categories%3Danime%252Cgeneral%252C" +
            "people%26purity%3Dsfw%26sorting%3Ddate_added%26order%3Ddesc%26topRange%3D1M%26" +
            "atleast%3D%26resolutions%3D%26ratios%3D%26colors%3D%26seed%3D&meta="
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
        searchStr = "query=test&filters=includedTags%3Dtest%26excludedTags%3D%26username%3D%26" +
            "tagId%3D%26wallpaperId%3D%26categories%3Danime%252Cgeneral%252Cpeople%26" +
            "purity%3Dsfw%26sorting%3Ddate_added%26order%3Ddesc%26topRange%3D1M%26atleast%3D%26" +
            "resolutions%3D%26ratios%3D%26colors%3D%26seed%3D&meta=type%3DWALLHAVEN_TAG%26" +
            "tag%3D%257B%2522id%2522%253A1%252C%2522name%2522%253A%2522test%2522%252C%2522" +
            "alias%2522%253A%255B%255D%252C%2522categoryId%2522%253A1%252C%2522" +
            "category%2522%253A%2522test_cat%2522%252C%2522purity%2522%253A%2522" +
            "SFW%2522%252C%2522createdAt%2522%253A%25222023-10-09T14%253A02%253A59Z%2522%257D"
        assertEquals(
            searchStr,
            search.toQueryString(),
        )
    }

    @Test
    fun `should convert qs to WallhavenSearch`() {
        var searchStr = "query=test&filters=includedTags%3Dtest%26excludedTags%3D%26" +
            "username%3D%26tagId%3D%26wallpaperId%3D%26categories%3Danime%252Cgeneral%252C" +
            "people%26purity%3Dsfw%26sorting%3Ddate_added%26order%3Ddesc%26topRange%3D1M%26" +
            "atleast%3D%26resolutions%3D%26ratios%3D%26colors%3D%26seed%3D&meta="
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
        searchStr = "query=test&filters=includedTags%3Dtest%26excludedTags%3D%26username%3D%26" +
            "tagId%3D%26wallpaperId%3D%26categories%3Danime%252Cgeneral%252Cpeople%26" +
            "purity%3Dsfw%26sorting%3Ddate_added%26order%3Ddesc%26topRange%3D1M%26atleast%3D%26" +
            "resolutions%3D%26ratios%3D%26colors%3D%26seed%3D&meta=type%3DWALLHAVEN_TAG%26" +
            "tag%3D%257B%2522id%2522%253A1%252C%2522name%2522%253A%2522test%2522%252C%2522" +
            "alias%2522%253A%255B%255D%252C%2522categoryId%2522%253A1%252C%2522" +
            "category%2522%253A%2522test_cat%2522%252C%2522purity%2522%253A%2522" +
            "SFW%2522%252C%2522createdAt%2522%253A%25222023-10-09T14%253A02%253A59Z%2522%257D"
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
        val filters = WallhavenFilters(includedTags = setOf("test"))
        searchStr = filters.toQueryString()
        search = WallhavenSearch(
            query = "",
            filters = filters,
            meta = null,
        )
        assertEquals(
            search,
            WallhavenSearch.fromQueryString(searchStr),
        )
    }

    /**
     * For backwards compatibility
     */
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
}
