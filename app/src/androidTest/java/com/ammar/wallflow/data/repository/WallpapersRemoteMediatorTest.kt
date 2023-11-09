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
import com.ammar.wallflow.data.db.dao.search.SearchQueryDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMeta
import com.ammar.wallflow.data.network.model.wallhaven.StringNetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.retrofit.RetrofitWallhavenNetwork
import com.ammar.wallflow.data.network.retrofit.reddit.RetrofitRedditNetwork
import com.ammar.wallflow.extensions.randomList
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Before
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
    private val wallhavenNetworkDataSource = RetrofitWallhavenNetwork(fakeWallhavenNetworkApi)
    private lateinit var fakeDb: AppDatabase
    private lateinit var searchQueryDao: SearchQueryDao
    private lateinit var wallhavenWallpapersDao: WallhavenWallpapersDao
    private lateinit var wallhavenSearchQueryWallpapersDao: WallhavenSearchQueryWallpapersDao
    private lateinit var redditWallpapersDao: RedditWallpapersDao
    private lateinit var redditSearchQueryWallpapersDao: RedditSearchQueryWallpapersDao
    private val fakeRedditNetworkApi = FakeRedditNetworkApi()
    private val redditNetworkDataSource = RetrofitRedditNetwork(fakeRedditNetworkApi)

    @Before
    fun setUp() {
        fakeDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        searchQueryDao = fakeDb.searchQueryDao()
        wallhavenWallpapersDao = fakeDb.wallhavenWallpapersDao()
        wallhavenSearchQueryWallpapersDao = fakeDb.wallhavenSearchQueryWallpapersDao()
        redditWallpapersDao = fakeDb.redditWallpapersDao()
        redditSearchQueryWallpapersDao = fakeDb.redditSearchQueryWallpapersDao()
    }

    @After
    fun tearDown() {
        fakeDb.clearAllTables()
        fakeDb.close()
        fakeWallhavenNetworkApi.failureMsg = null
        fakeWallhavenNetworkApi.clearFakeData()

        fakeRedditNetworkApi.failureMsg = null
        fakeRedditNetworkApi.clearFakeData()
    }

    @Test
    fun refreshLoadReturnsSuccessResultWhenMoreDataIsPresent() = runTest {
        val query = "test"
        val search = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf(query)),
        )
        val mockNetworkWallpapers = MockFactory.generateNetworkWallhavenWallpapers(20)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search.getApiQueryString(),
            networkWallhavenWallpapers = mockNetworkWallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 198,
            ),
        )

        val remoteMediator = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search,
            fakeDb,
            wallhavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        val wallpaperCount = wallhavenWallpapersDao.count()
        val searchQueryCount = searchQueryDao.count()

        assertEquals(20, wallpaperCount)
        assertEquals(1, searchQueryCount)

        val searchQueryEntity = searchQueryDao.getBySearchQuery(
            search.toJson(),
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
        val search = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf("test")),
        )
        val remoteMediator = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search,
            fakeDb,
            wallhavenNetworkDataSource,
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
        val search = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf("test")),
        )
        val remoteMediator = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search,
            fakeDb,
            wallhavenNetworkDataSource,
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
        val search = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf("test")),
        )
        var mockNetworkWallpapers = MockFactory.generateNetworkWallhavenWallpapers(20)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search.getApiQueryString(),
            networkWallhavenWallpapers = mockNetworkWallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 198,
            ),
        )
        val remoteMediator = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search,
            fakeDb,
            wallhavenNetworkDataSource,
        )
        val pagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Success }
        val searchQueryEntity = searchQueryDao.getBySearchQuery(
            search.toJson(),
        )
        assertNotNull(searchQueryEntity)
        var wallpaperCount = wallhavenWallpapersDao.count()
        var searchQueryCount = searchQueryDao.count()
        val prevWallpaperIds = wallhavenWallpapersDao.getAllWallhavenIds()

        assertEquals(20, wallpaperCount)
        assertEquals(1, searchQueryCount)
        val lastUpdated = searchQueryEntity.lastUpdatedOn
        // refresh again
        fakeWallhavenNetworkApi.clearFakeData()
        mockNetworkWallpapers = MockFactory.generateNetworkWallhavenWallpapers(20)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search.getApiQueryString(),
            networkWallhavenWallpapers = mockNetworkWallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 198,
            ),
        )
        val refreshResult = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { refreshResult is MediatorResult.Success }
        val refreshSearchQueryEntity = searchQueryDao.getBySearchQuery(
            search.toJson(),
        )
        assertNotNull(refreshSearchQueryEntity)
        val refreshLastUpdated = refreshSearchQueryEntity.lastUpdatedOn
        assertTrue { refreshLastUpdated > lastUpdated }
        wallpaperCount = wallhavenWallpapersDao.count()
        searchQueryCount = searchQueryDao.count()
        val newWallpaperIds = wallhavenWallpapersDao.getAllWallhavenIds()

        assertEquals(20, wallpaperCount)
        assertEquals(1, searchQueryCount)
        assertNotEquals(prevWallpaperIds, newWallpaperIds)
    }

    @Test
    fun refreshLoadSameQueryUpdateWallpapers() = runTest {
        val queryStr = "test"
        val search = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf(queryStr)),
        )

        val queryWallpapers1 = MockFactory.generateNetworkWallhavenWallpapers(20)
        val queryWallpaperWallhavenIds = queryWallpapers1.map { it.id }
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search.getApiQueryString(),
            networkWallhavenWallpapers = queryWallpapers1,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 200,
            ),
        )
        val remoteMediator = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search,
            fakeDb,
            wallhavenNetworkDataSource,
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

        val queryWallpapers2 = MockFactory.generateNetworkWallhavenWallpapers(10)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search.getApiQueryString(),
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
        val search1 = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf(queryStr1)),
        )
        val queryStr2 = "test2"
        val search2 = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf(queryStr2)),
        )

        val query1Wallpapers = MockFactory.generateNetworkWallhavenWallpapers(20)
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search1.getApiQueryString(),
            networkWallhavenWallpapers = query1Wallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 10,
                per_page = 20,
                total = 200,
            ),
        )
        val remoteMediator1 = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search1,
            fakeDb,
            wallhavenNetworkDataSource,
        )
        val pagingState1 = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator1.load(LoadType.REFRESH, pagingState1)

        val query2Wallpapers =
            query1Wallpapers.randomList(5) + MockFactory.generateNetworkWallhavenWallpaper(
                idNumber = 21,
            )
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = search2.getApiQueryString(),
            networkWallhavenWallpapers = query2Wallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = 1,
                per_page = 20,
                total = 6,
            ),
        )
        val remoteMediator2 = WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
            search2,
            fakeDb,
            wallhavenNetworkDataSource,
        )
        val pagingState2 = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator2.load(LoadType.REFRESH, pagingState2)

        val wallpaperCount = wallhavenWallpapersDao.count()
        val searchQueryCount = searchQueryDao.count()

        assertEquals(21, wallpaperCount)
        assertEquals(2, searchQueryCount)

        val searchQueryEntity1 = searchQueryDao.getBySearchQuery(
            search1.toJson(),
        )
        assertNotNull(searchQueryEntity1)
        val searchQueryEntity2 = searchQueryDao.getBySearchQuery(
            search2.toJson(),
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

    @Test
    fun refreshLoadReturnsSuccessResultWhenMoreDataIsPresentForReddit() = runTest {
        val query = "test"
        val search = RedditSearch(
            query = query,
            filters = RedditFilters(
                subreddits = setOf("test1"),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        val fakeRedditPosts = MockFactory.generateNetworkRedditPosts(20)
        fakeRedditNetworkApi.setPostsForQuery(
            query = "self:no $query",
            networkRedditPosts = fakeRedditPosts,
            after = "after",
        )
        val totalImages = fakeRedditPosts
            .filter { !it.is_video }
            .fold(0) { prev, post ->
                if (post.is_gallery) {
                    prev + (post.gallery_data?.items?.size ?: 0)
                } else {
                    prev + 1
                }
            }

        val remoteMediator = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        val wallpaperCount = redditWallpapersDao.count()
        val searchQueryCount = searchQueryDao.count()

        assertEquals(totalImages, wallpaperCount)
        assertEquals(1, searchQueryCount)

        val searchQueryEntity = searchQueryDao.getBySearchQuery(json.encodeToString(search))
        assertNotNull(searchQueryEntity)

        val queryWallpaperEntities = redditSearchQueryWallpapersDao.getBySearchQueryId(
            searchQueryEntity.id,
        )
        assertEquals(totalImages, queryWallpaperEntities.size)

        assertTrue { result is MediatorResult.Success }
        assertFalse { (result as MediatorResult.Success).endOfPaginationReached }
    }

    @Test
    fun refreshLoadSuccessAndEndOfPaginationWhenNoMoreDataForReddit() = runTest {
        val search = RedditSearch(
            query = "test",
            filters = RedditFilters(
                subreddits = setOf("test1"),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        val remoteMediator = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState = PagingState<Int, RedditWallpaperEntity>(
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
    fun refreshLoadReturnsErrorResultWhenErrorOccursForReddit() = runTest {
        fakeRedditNetworkApi.failureMsg = "Throw test failure"
        val search = RedditSearch(
            query = "test",
            filters = RedditFilters(
                subreddits = setOf("test1"),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        val remoteMediator = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Error }
    }

    @Test
    fun refreshLoadUpdateRemoteKeyLastUpdatedForReddit() = runTest {
        val search = RedditSearch(
            query = "test",
            filters = RedditFilters(
                subreddits = setOf("test1"),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        val remoteMediator = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        val result = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { result is MediatorResult.Success }
        val searchQueryEntity = searchQueryDao.getBySearchQuery(json.encodeToString(search))
        assertNotNull(searchQueryEntity)
        val lastUpdated = searchQueryEntity.lastUpdatedOn
        // refresh again
        val refreshResult = remoteMediator.load(LoadType.REFRESH, pagingState)
        assertTrue { refreshResult is MediatorResult.Success }
        val refreshSearchQueryEntity = searchQueryDao.getBySearchQuery(json.encodeToString(search))
        assertNotNull(refreshSearchQueryEntity)
        val refreshLastUpdated = refreshSearchQueryEntity.lastUpdatedOn
        assertTrue { refreshLastUpdated > lastUpdated }
    }

    @Test
    fun refreshLoadSameQueryUpdateWallpapersForReddit() = runTest {
        val query = "test"
        val search = RedditSearch(
            query = query,
            filters = RedditFilters(
                subreddits = setOf("test1"),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        val fakeRedditPosts1 = MockFactory.generateNetworkRedditPosts(20)
        fakeRedditNetworkApi.setPostsForQuery(
            query = "self:no $query",
            networkRedditPosts = fakeRedditPosts1,
            after = "after",
        )
        val totalImages1 = fakeRedditPosts1
            .filter { !it.is_video }
            .fold(0) { prev, post ->
                if (post.is_gallery) {
                    prev + (post.gallery_data?.items?.size ?: 0)
                } else {
                    prev + 1
                }
            }
        val ids1 = LongRange(1, totalImages1.toLong()).toList()

        val remoteMediator = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )

        remoteMediator.load(LoadType.REFRESH, pagingState)

        var wallpaperCount = redditWallpapersDao.count()
        assertEquals(totalImages1, wallpaperCount)

        val dbWallpaperIds = redditWallpapersDao.getAllIds()

        assertEquals(ids1.sorted(), dbWallpaperIds.sorted())

        val fakeRedditPosts2 = MockFactory.generateNetworkRedditPosts(10)
        fakeRedditNetworkApi.setPostsForQuery(
            query = "self:no $query",
            networkRedditPosts = fakeRedditPosts2,
            after = null,
        )
        val totalImages2 = fakeRedditPosts2
            .filter { !it.is_video }
            .fold(0) { prev, post ->
                if (post.is_gallery) {
                    prev + (post.gallery_data?.items?.size ?: 0)
                } else {
                    prev + 1
                }
            }

        remoteMediator.load(LoadType.REFRESH, pagingState)

        wallpaperCount = redditWallpapersDao.count()
        assertEquals(totalImages2, wallpaperCount)
    }

    @Test
    fun refreshLoadMultipleQueriesForReddit() = runTest {
        val queryStr1 = "test1"
        val search1 = RedditSearch(
            query = queryStr1,
            filters = RedditFilters(
                subreddits = setOf(queryStr1),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        val queryStr2 = "test2"
        val search2 = RedditSearch(
            query = queryStr2,
            filters = RedditFilters(
                subreddits = setOf(queryStr2),
                includeNsfw = false,
                sort = RedditSort.TOP,
                timeRange = RedditTimeRange.ALL,
            ),
        )

        val fakeRedditPosts1 = MockFactory.generateNetworkRedditPosts(20)
        fakeRedditNetworkApi.setPostsForQuery(
            query = "self:no $queryStr1",
            networkRedditPosts = fakeRedditPosts1,
            after = "after",
        )
        val totalImages1 = fakeRedditPosts1
            .filter { !it.is_video }
            .fold(0) { prev, post ->
                if (post.is_gallery) {
                    prev + (post.gallery_data?.items?.size ?: 0)
                } else {
                    prev + 1
                }
            }

        val remoteMediator1 = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search1,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState1 = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator1.load(LoadType.REFRESH, pagingState1)

        assertEquals(totalImages1, redditWallpapersDao.count())

        val fakeRedditPosts2 = fakeRedditPosts1.randomList(5) +
            MockFactory.generateRedditImagePost(
                postId = 21,
            )
        fakeRedditNetworkApi.setPostsForQuery(
            query = "self:no $queryStr2",
            networkRedditPosts = fakeRedditPosts2,
            after = "after1",
        )
        val totalImages2 = fakeRedditPosts2
            .filter { !it.is_video }
            .fold(0) { prev, post ->
                if (post.is_gallery) {
                    prev + (post.gallery_data?.items?.size ?: 0)
                } else {
                    prev + 1
                }
            }
        val remoteMediator2 = WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
            search2,
            fakeDb,
            redditNetworkDataSource,
        )
        val pagingState2 = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            0,
        )
        remoteMediator2.load(LoadType.REFRESH, pagingState2)

        val wallpaperCount = redditWallpapersDao.count()
        val searchQueryCount = searchQueryDao.count()

        assertEquals(totalImages1 + 1, wallpaperCount)
        assertEquals(2, searchQueryCount)

        val searchQueryEntity1 = searchQueryDao.getBySearchQuery(
            json.encodeToString(search1),
        )
        assertNotNull(searchQueryEntity1)
        val searchQueryEntity2 = searchQueryDao.getBySearchQuery(
            json.encodeToString(search2),
        )
        assertNotNull(searchQueryEntity2)

        val query1WallpaperEntities = redditSearchQueryWallpapersDao.getBySearchQueryId(
            searchQueryEntity1.id,
        )
        assertEquals(totalImages1, query1WallpaperEntities.size)

        val query2WallpaperEntities = redditSearchQueryWallpapersDao.getBySearchQueryId(
            searchQueryEntity2.id,
        )
        assertEquals(totalImages2, query2WallpaperEntities.size)
    }
}
