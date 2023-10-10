package com.ammar.wallflow.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryEntity
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallhavenSearchQueryDaoTest {
    private val fakeDb = Room.inMemoryDatabaseBuilder(
        context = ApplicationProvider.getApplicationContext(),
        klass = AppDatabase::class.java,
    ).build()

    @After
    fun tearDown() {
        fakeDb.clearAllTables()
    }

    @Test
    fun shouldBeBackwardsCompatibleWithWallhavenFiltersToQueryString() = runTest {
        // in initial versions of WallFlow, WallhavenSearchQueryEntity used to save the result
        // of WallhavenFilters#toQueryString. Now we save WallhavenSearch#toQueryString.

        // prev WallhavenFilters#toSearchQuery result
        var filterQueryStringResult = "includedTags=test&excludedTags=test2&username=&tagId=" +
            "&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=date_added" +
            "&order=desc&topRange=1M&atleast=&resolutions=&ratios=&colors=&seed="
        var entity = WallhavenSearchQueryEntity(
            id = 0,
            queryString = filterQueryStringResult,
            lastUpdatedOn = Clock.System.now(),
        )
        val dao = fakeDb.wallhavenSearchQueryDao()
        dao.upsert(entity)

        // now we search using WallhavenSearch#toQueryString
        var search = WallhavenSearch(
            filters = WallhavenFilters(
                includedTags = setOf("test"),
                excludedTags = setOf("test2"),
            ),
        )
        var foundEntity = dao.getBySearchQuery(
            search.toQueryString(
                backwardsCompat = true,
            ),
        )
        assertNotNull(foundEntity)
        assertEquals(
            search,
            WallhavenSearch.fromQueryString(foundEntity.queryString),
        )

        filterQueryStringResult = "includedTags=test%2Ctest2&excludedTags=test3&username=&tagId=" +
            "&wallpaperId=&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=date_added" +
            "&order=desc&topRange=1M&atleast=&resolutions=&ratios=&colors=&seed="
        entity = WallhavenSearchQueryEntity(
            id = 0,
            queryString = filterQueryStringResult,
            lastUpdatedOn = Clock.System.now(),
        )
        dao.upsert(entity)
        // above filterQueryString was made using a WallhavenSearch with query = "test"
        // we lose the query information when using backwards compat mode
        search = WallhavenSearch(
            query = "",
            filters = WallhavenFilters(
                includedTags = setOf("test", "test2"),
                excludedTags = setOf("test3"),
            ),
        )
        foundEntity = dao.getBySearchQuery(
            search.toQueryString(
                backwardsCompat = true,
            ),
        )
        assertNotNull(foundEntity)
        assertEquals(
            search,
            WallhavenSearch.fromQueryString(foundEntity.queryString),
        )
    }
}
