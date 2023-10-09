package com.ammar.wallflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.MockFactory
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMeta
import com.ammar.wallflow.data.network.model.wallhaven.StringNetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.retrofit.RetrofitWallhavenNetwork
import com.ammar.wallflow.extensions.randomList
import com.ammar.wallflow.model.search.WallhavenFilters
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

// somehow Android Studio complaints @OptIn not required, but it is required
@Suppress("UnnecessaryOptInAnnotation")
@OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalPagingApi::class,
)
@RunWith(AndroidJUnit4::class)
class WallpapersRemoteMediatorTest {
    private val fakeWallhavenNetworkApi = FakeWallhavenNetworkApi()
    private val wallHavenNetworkDataSource = RetrofitWallhavenNetwork(fakeWallhavenNetworkApi)
    private val mockDb = Room.inMemoryDatabaseBuilder(
        context = ApplicationProvider.getApplicationContext(),
        klass = AppDatabase::class.java,
    ).build()
    private val wallhavenWallpapersDao = mockDb.wallhavenWallpapersDao()
    private val wallhavenSearchQueryDao = mockDb.wallhavenSearchQueryDao()
    private val wallhavenSearchQueryWallpapersDao = mockDb.wallhavenSearchQueryWallpapersDao()

    @After
    fun tearDown() {
        mockDb.clearAllTables()
        fakeWallhavenNetworkApi.failureMsg = null
        fakeWallhavenNetworkApi.clearMockData()
    }

    @Test
    fun refreshLoadReturnsSuccessResultWhenMoreDataIsPresent() = runTest {
        val query = "test"
        val searchQuery = WallhavenFilters(includedTags = setOf(query))
        val mockNetworkWallpapers = MockFactory.generateNetworkWallpapers(20)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallhavenWallpapers = mockNetworkWallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 198,
            ),
        )

        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        val wallpaperCount = wallhavenWallpapersDao.count()
        val searchQueryCount = wallhavenSearchQueryDao.count()

        assertEquals(20, wallpaperCount)
        assertEquals(1, searchQueryCount)

        val searchQueryEntity = wallhavenSearchQueryDao.getBySearchQuery(
            searchQuery.toQueryString(),
        )
        assertNotNull(searchQueryEntity)

        val queryWallpaperEntities = wallhavenSearchQueryWallpapersDao.getBySearchQueryId(
            searchQueryEntity.id,
        )
        assertEquals(20, queryWallpaperEntities.size)

        assertTrue { result is MediatorResult.Success }
        assertFalse { (result as MediatorResult.Success).endOfPaginationReached }
    }

    @Test
    fun refreshLoadSuccessAndEndOfPaginationWhenNoMoreData() = runTest {
        val remoteMediator = WallpapersRemoteMediator(
            WallhavenFilters(includedTags = setOf("test")),
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Success }
        assertTrue { (result as MediatorResult.Success).endOfPaginationReached }
    }

    @Test
    fun refreshLoadReturnsErrorResultWhenErrorOccurs() = runTest {
        fakeWallhavenNetworkApi.failureMsg = "Throw test failure"
        val remoteMediator = WallpapersRemoteMediator(
            WallhavenFilters(includedTags = setOf("test")),
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Error }
    }

    @Test
    fun refreshLoadUpdateRemoteKeyLastUpdated() = runTest {
        val searchQuery = WallhavenFilters(includedTags = setOf("test"))
        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Success }
        val searchQueryEntity = wallhavenSearchQueryDao.getBySearchQuery(
            searchQuery.toQueryString(),
        )
        assertNotNull(searchQueryEntity)
        val lastUpdated = searchQueryEntity.lastUpdatedOn
        // refresh again
        val refreshResult = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { refreshResult is MediatorResult.Success }
        val refreshSearchQueryEntity = wallhavenSearchQueryDao.getBySearchQuery(
            searchQuery.toQueryString(),
        )
        assertNotNull(refreshSearchQueryEntity)
        val refreshLastUpdated = refreshSearchQueryEntity.lastUpdatedOn
        assertTrue { refreshLastUpdated > lastUpdated }
    }

    @Test
    fun refreshLoadSameQueryUpdateWallpapers() = runTest {
        val queryStr = "test"
        val searchQuery = WallhavenFilters(includedTags = setOf(queryStr))

        val queryWallpapers1 = MockFactory.generateNetworkWallpapers(20)
        val queryWallpaperWallhavenIds = queryWallpapers1.map { it.id }
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallhavenWallpapers = queryWallpapers1,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 200,
            ),
        )
        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator.load(LoadType.REFRESH, pagingState)

        var wallpaperCount = wallhavenWallpapersDao.count()
        assertEquals(20, wallpaperCount)

        val dbWallpapers = wallhavenWallpapersDao.getAll()
        val dbWallpaperWallhavenIds = dbWallpapers.map { it.wallhavenId }

        assertEquals(queryWallpaperWallhavenIds, dbWallpaperWallhavenIds)

        val queryWallpapers2 = MockFactory.generateNetworkWallpapers(10)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallhavenWallpapers = queryWallpapers2,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 1,
                per_page = 20,
                total = 1,
            ),
        )
        remoteMediator.load(LoadType.REFRESH, pagingState)

        wallpaperCount = wallhavenWallpapersDao.count()
        assertEquals(10, wallpaperCount)
    }

    @Test
    fun refreshLoadMultipleQueries() = runTest {
        val queryStr1 = "test1"
        val searchQuery1 = WallhavenFilters(includedTags = setOf(queryStr1))
        val queryStr2 = "test2"
        val searchQuery2 = WallhavenFilters(includedTags = setOf(queryStr2))

        val query1Wallpapers = MockFactory.generateNetworkWallpapers(20)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = searchQuery1.getQString(),
            networkWallhavenWallpapers = query1Wallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 200,
            ),
        )
        val remoteMediator1 = WallpapersRemoteMediator(
            searchQuery1,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState1 = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator1.load(LoadType.REFRESH, pagingState1)

        val query2Wallpapers =
            query1Wallpapers.randomList(5) + MockFactory.generateNetworkWallpaper(
                idNumber = 21,
            )
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = searchQuery2.getQString(),
            networkWallhavenWallpapers = query2Wallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 1,
                per_page = 20,
                total = 6,
            ),
        )
        val remoteMediator2 = WallpapersRemoteMediator(
            searchQuery2,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState2 = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator2.load(LoadType.REFRESH, pagingState2)

        val wallpaperCount = wallhavenWallpapersDao.count()
        val searchQueryCount = wallhavenSearchQueryDao.count()

        assertEquals(21, wallpaperCount)
        assertEquals(2, searchQueryCount)

        val searchQueryEntity1 = wallhavenSearchQueryDao.getBySearchQuery(
            searchQuery1.toQueryString(),
        )
        assertNotNull(searchQueryEntity1)
        val searchQueryEntity2 = wallhavenSearchQueryDao.getBySearchQuery(
            searchQuery2.toQueryString(),
        )
        assertNotNull(searchQueryEntity2)

        val query1WallpaperEntities = wallhavenSearchQueryWallpapersDao.getBySearchQueryId(
            searchQueryEntity1.id,
        )
        assertEquals(20, query1WallpaperEntities.size)

        val query2WallpaperEntities = wallhavenSearchQueryWallpapersDao.getBySearchQueryId(
            searchQueryEntity2.id,
        )
        assertEquals(6, query2WallpaperEntities.size)
    }
}
