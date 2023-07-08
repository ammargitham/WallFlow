package com.ammar.wallflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.data.db.database.AppDatabase
import com.ammar.wallflow.data.db.entity.WallpaperEntity
import com.ammar.wallflow.data.network.model.NetworkMeta
import com.ammar.wallflow.data.network.model.StringNetworkMetaQuery
import com.ammar.wallflow.data.network.retrofit.RetrofitWallHavenNetwork
import com.ammar.wallflow.extensions.randomList
import com.ammar.wallflow.model.SearchQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// somehow Android Studio complaints @OptIn not required, but it is required
@Suppress("UnnecessaryOptInAnnotation")
@OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalPagingApi::class,
)
@RunWith(AndroidJUnit4::class)
class WallpapersRemoteMediatorTest {
    private val mockNetworkApi = MockWallHavenNetworkApi()
    private val wallHavenNetworkDataSource = RetrofitWallHavenNetwork(mockNetworkApi)
    private val mockDb = Room.inMemoryDatabaseBuilder(
        context = ApplicationProvider.getApplicationContext(),
        klass = AppDatabase::class.java
    ).build()
    private val wallpapersDao = mockDb.wallpapersDao()
    private val searchQueryDao = mockDb.searchQueryDao()
    private val searchQueryWallpapersDao = mockDb.searchQueryWallpapersDao()

    @After
    fun tearDown() {
        mockDb.clearAllTables()
        mockNetworkApi.failureMsg = null
        mockNetworkApi.clearMockData()
    }

    @Test
    fun refreshLoadReturnsSuccessResultWhenMoreDataIsPresent() = runTest {
        val query = "test"
        val searchQuery = SearchQuery(includedTags = setOf(query))
        val mockNetworkWallpapers = MockFactory.generateNetworkWallpapers(20)
        mockNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallpapers = mockNetworkWallpapers,
            meta = NetworkMeta(
                query = StringNetworkMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 198,
            )
        )

        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        val wallpaperCount = wallpapersDao.count()
        val searchQueryCount = searchQueryDao.count()

        assertEquals(20, wallpaperCount)
        assertEquals(1, searchQueryCount)

        val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
        assertNotNull(searchQueryEntity)

        val queryWallpaperEntities =
            searchQueryWallpapersDao.getBySearchQueryId(searchQueryEntity.id)
        assertEquals(20, queryWallpaperEntities.size)

        assertTrue { result is MediatorResult.Success }
        assertFalse { (result as MediatorResult.Success).endOfPaginationReached }
    }

    @Test
    fun refreshLoadSuccessAndEndOfPaginationWhenNoMoreData() = runTest {
        val remoteMediator = WallpapersRemoteMediator(
            SearchQuery(includedTags = setOf("test")),
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Success }
        assertTrue { (result as MediatorResult.Success).endOfPaginationReached }
    }

    @Test
    fun refreshLoadReturnsErrorResultWhenErrorOccurs() = runTest {
        mockNetworkApi.failureMsg = "Throw test failure"
        val remoteMediator = WallpapersRemoteMediator(
            SearchQuery(includedTags = setOf("test")),
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Error }
    }

    @Test
    fun refreshLoadUpdateRemoteKeyLastUpdated() = runTest {
        val searchQuery = SearchQuery(includedTags = setOf("test"))
        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Success }
        val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
        assertNotNull(searchQueryEntity)
        val lastUpdated = searchQueryEntity.lastUpdatedOn
        // refresh again
        val refreshResult = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { refreshResult is MediatorResult.Success }
        val refreshSearchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
        assertNotNull(refreshSearchQueryEntity)
        val refreshLastUpdated = refreshSearchQueryEntity.lastUpdatedOn
        assertTrue { refreshLastUpdated > lastUpdated }
    }

    @Test
    fun refreshLoadSameQueryUpdateWallpapers() = runTest {
        val queryStr = "test"
        val searchQuery = SearchQuery(includedTags = setOf(queryStr))

        val queryWallpapers1 = MockFactory.generateNetworkWallpapers(20)
        val queryWallpaperWallhavenIds = queryWallpapers1.map { it.id }
        mockNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallpapers = queryWallpapers1,
            meta = NetworkMeta(
                query = StringNetworkMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 200,
            )
        )
        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0
        )
        remoteMediator.load(LoadType.REFRESH, pagingState)

        var wallpaperCount = wallpapersDao.count()
        assertEquals(20, wallpaperCount)

        val dbWallpapers = wallpapersDao.getAll()
        val dbWallpaperWallhavenIds = dbWallpapers.map { it.wallhavenId }

        assertEquals(queryWallpaperWallhavenIds, dbWallpaperWallhavenIds)

        val queryWallpapers2 = MockFactory.generateNetworkWallpapers(10)
        mockNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallpapers = queryWallpapers2,
            meta = NetworkMeta(
                query = StringNetworkMetaQuery(""),
                current_page = 1,
                last_page = 1,
                per_page = 20,
                total = 1,
            )
        )
        remoteMediator.load(LoadType.REFRESH, pagingState)

        wallpaperCount = wallpapersDao.count()
        assertEquals(10, wallpaperCount)
    }

    @Test
    fun refreshLoadMultipleQueries() = runTest {
        val queryStr1 = "test1"
        val searchQuery1 = SearchQuery(includedTags = setOf(queryStr1))
        val queryStr2 = "test2"
        val searchQuery2 = SearchQuery(includedTags = setOf(queryStr2))

        val query1Wallpapers = MockFactory.generateNetworkWallpapers(20)
        mockNetworkApi.setWallpapersForQuery(
            query = searchQuery1.getQString(),
            networkWallpapers = query1Wallpapers,
            meta = NetworkMeta(
                query = StringNetworkMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 200,
            )
        )
        val remoteMediator1 = WallpapersRemoteMediator(
            searchQuery1,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState1 = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0
        )
        remoteMediator1.load(LoadType.REFRESH, pagingState1)

        val query2Wallpapers =
            query1Wallpapers.randomList(5) + MockFactory.generateNetworkWallpaper(21)
        mockNetworkApi.setWallpapersForQuery(
            query = searchQuery2.getQString(),
            networkWallpapers = query2Wallpapers,
            meta = NetworkMeta(
                query = StringNetworkMetaQuery(""),
                current_page = 1,
                last_page = 1,
                per_page = 20,
                total = 6,
            )
        )
        val remoteMediator2 = WallpapersRemoteMediator(
            searchQuery2,
            mockDb,
            wallHavenNetworkDataSource,
        )
        val pagingState2 = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0
        )
        remoteMediator2.load(LoadType.REFRESH, pagingState2)

        val wallpaperCount = wallpapersDao.count()
        val searchQueryCount = searchQueryDao.count()

        assertEquals(21, wallpaperCount)
        assertEquals(2, searchQueryCount)

        val searchQueryEntity1 = searchQueryDao.getBySearchQuery(searchQuery1.toQueryString())
        assertNotNull(searchQueryEntity1)
        val searchQueryEntity2 = searchQueryDao.getBySearchQuery(searchQuery2.toQueryString())
        assertNotNull(searchQueryEntity2)

        val query1WallpaperEntities =
            searchQueryWallpapersDao.getBySearchQueryId(searchQueryEntity1.id)
        assertEquals(20, query1WallpaperEntities.size)

        val query2WallpaperEntities =
            searchQueryWallpapersDao.getBySearchQueryId(searchQueryEntity2.id)
        assertEquals(6, query2WallpaperEntities.size)
    }
}
