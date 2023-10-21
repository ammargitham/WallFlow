package com.ammar.wallflow.workers

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ammar.wallflow.MockFactory
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.OnlineSourceWallpaperEntity
import com.ammar.wallflow.data.db.entity.reddit.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditPost
import com.ammar.wallflow.data.network.model.reddit.toWallpaperEntities
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMeta
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaper
import com.ammar.wallflow.data.network.model.wallhaven.StringNetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.model.wallhaven.toWallpaperEntity
import com.ammar.wallflow.data.network.retrofit.RetrofitWallhavenNetwork
import com.ammar.wallflow.data.network.retrofit.reddit.RetrofitRedditNetwork
import com.ammar.wallflow.data.repository.FakeRedditNetworkApi
import com.ammar.wallflow.data.repository.FakeWallhavenNetworkApi
import com.ammar.wallflow.data.repository.WallpapersRemoteMediator
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getTempDir
import com.ammar.wallflow.extensions.getTempFile
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CleanupWorkerTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var tempDir: File
    private val fakeWallhavenNetworkApi = FakeWallhavenNetworkApi()
    private val wallHavenNetworkDataSource = RetrofitWallhavenNetwork(fakeWallhavenNetworkApi)
    private val fakeRedditNetworkApi = FakeRedditNetworkApi()
    private val redditNetworkDataSource = RetrofitRedditNetwork(fakeRedditNetworkApi)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).build()
        val actualTempDir = context.getTempDir()
        tempDir = File(actualTempDir, "test")
        tempDir.mkdirs()

        mockkStatic("com.ammar.wallflow.extensions.Context_extKt")

        // make sure every call to ctx.tempDir() gets the test temp dir
        every { context.getTempDir() } returns tempDir

        // make sure every call to ctx.getTempFile(fileName) gets the file from the test dir
        val fileNameSlot = slot<String>()
        every {
            context.getTempFile(capture(fileNameSlot))
        } answers {
            File(tempDir, fileNameSlot.captured)
        }
    }

    @After
    fun tearDown() {
        db.clearAllTables()
        db.close()
        fakeWallhavenNetworkApi.failureMsg = null
        fakeWallhavenNetworkApi.clearFakeData()
        fakeRedditNetworkApi.failureMsg = null
        fakeRedditNetworkApi.clearFakeData()
        tempDir.listFiles()?.forEach {
            println(it.lastModified())
            it.delete()
        }
        tempDir.delete()
        unmockkAll()
    }

    @Test
    fun testCleanupWorker1Query() = runTest(testDispatcher) {
        val clock = TestClock(Clock.System.now())
        val query = "test"
        simulateSearch(clock, query)
        val searchQueryDao = db.searchQueryDao()
        val wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        val redditWallpapersDao = db.redditWallpapersDao()
        assertEquals(20, wallhavenWallpapersDao.count())
        assertEquals(20, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 5 wallhaven and reddit wallpapers in temp dir
        val wallhavenWallpapers = wallhavenWallpapersDao.getAll()
        val redditWallpapers = redditWallpapersDao.getAll()
        val cachedCount = 5
        val randomWallhavenWallpapers = wallhavenWallpapers.shuffled().take(cachedCount)
        val randomRedditWallpapers = redditWallpapers.shuffled().take(cachedCount)
        createTempFiles(
            randomWallhavenWallpapers + randomRedditWallpapers,
            clock.now(),
        )

        assertEquals(cachedCount * 2, tempDir.listFiles()?.size)

        // forward time by 2 days
        clock.plus(2.days)
        var worker = getWorker(clock = clock)
        worker.doWork()

        // nothing should be deleted
        assertEquals(20, wallhavenWallpapersDao.count())
        assertEquals(20, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())
        assertEquals(cachedCount * 2, tempDir.listFiles()?.size)

        // forward time by 6 days
        clock.plus(6.days)
        worker = getWorker(clock = clock)
        worker.doWork()

        // everything should be deleted
        assertEquals(0, wallhavenWallpapersDao.count())
        assertEquals(0, redditWallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesUniqueWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        val redditWallpapersDao = db.redditWallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        simulateSearch(clock, query1)
        assertEquals(20, wallhavenWallpapersDao.count())
        assertEquals(20, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        val query2 = "test2"
        simulateSearch(clock, query2)
        assertEquals(40, wallhavenWallpapersDao.count())
        assertEquals(40, redditWallpapersDao.count())
        assertEquals(4, searchQueryDao.count())

        // we cache 10 wallpapers in temp dir
        val wallhavenWallpapers = wallhavenWallpapersDao.getAll()
        val redditWallpapers = redditWallpapersDao.getAll()
        val cachedCount = 10
        val randomWallhavenWallpapers = wallhavenWallpapers.shuffled().take(cachedCount)
        val randomRedditWallpapers = redditWallpapers.shuffled().take(cachedCount)
        createTempFiles(randomWallhavenWallpapers + randomRedditWallpapers, clock.now())

        assertEquals(cachedCount * 2, tempDir.listFiles()?.size)

        // forward time by 8 days
        clock.plus(8.days)
        val worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallhavenWallpapersDao.count())
        assertEquals(0, redditWallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesOnDifferentDatesUniqueWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        val redditWallpapersDao = db.redditWallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val fakeNetworkWallhavenWallpapers1 = MockFactory.generateNetworkWallhavenWallpapers(
            size = 20,
        )
        val fakeNetworkRedditPosts1 = MockFactory.generateNetworkRedditPosts(
            size = 20,
            singleImageOnly = true,
        )
        simulateSearch(
            clock,
            query1,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers1,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts1,
        )
        assertEquals(20, wallhavenWallpapersDao.count())
        assertEquals(20, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        val cachedCount = 5

        // we cache 5 wallpapers from fakeNetworkWallhavenWallpapers1 and fakeNetworkRedditPosts1
        val wallHavenIds1 = fakeNetworkWallhavenWallpapers1.map { it.id }
        val wallhavenWallpapers1 = wallhavenWallpapersDao.getByWallhavenIds(wallHavenIds1)
        val randomWallhavenWallpapers1 = wallhavenWallpapers1.shuffled().take(cachedCount)
        val redditIds = getRedditIds(fakeNetworkRedditPosts1)
        val redditWallpapers1 = redditWallpapersDao.getByRedditIds(redditIds)
        val randomRedditWallpapers1 = redditWallpapers1.shuffled().take(cachedCount)
        createTempFiles(
            randomWallhavenWallpapers1 + randomRedditWallpapers1,
            clock.now(),
        )
        assertEquals(cachedCount * 2, tempDir.listFiles()?.size)

        clock.plus(2.days)
        val query2 = "test2"
        val fakeNetworkWallpapers2 = MockFactory.generateNetworkWallhavenWallpapers(20)
        val fakeNetworkRedditPosts2 = MockFactory.generateNetworkRedditPosts(
            size = 20,
            singleImageOnly = true,
        )
        simulateSearch(
            clock,
            query2,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallpapers2,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts2,
        )
        assertEquals(40, wallhavenWallpapersDao.count())
        assertEquals(40, redditWallpapersDao.count())
        assertEquals(4, searchQueryDao.count())

        // we cache 5 wallpapers from fakeNetworkWallpapers2 and fakeNetworkRedditPosts2
        val wallHavenIds2 = fakeNetworkWallpapers2.map { it.id }
        val wallhavenWallpapers2 = wallhavenWallpapersDao.getByWallhavenIds(wallHavenIds2)
        val randomWallhavenWallpapers2 = wallhavenWallpapers2.shuffled().take(cachedCount)
        val redditIds2 = getRedditIds(fakeNetworkRedditPosts2)
        val redditWallpapers2 = redditWallpapersDao.getByRedditIds(redditIds2)
        val randomRedditWallpapers2 = redditWallpapers2.shuffled().take(cachedCount)
        createTempFiles(
            randomWallhavenWallpapers2 + randomRedditWallpapers2,
            clock.now(),
        )
        assertEquals(cachedCount * 4, tempDir.listFiles()?.size)

        // forward time by 6 days
        clock.plus(6.days)

        // worker should only delete searchQuery1 and related cache
        var worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(20, wallhavenWallpapersDao.count())
        assertEquals(20, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())
        assertEquals(cachedCount * 2, tempDir.listFiles()?.size)
        assertEquals(
            wallhavenWallpapers2.sortedBy { it.id },
            wallhavenWallpapersDao.getAll().sortedBy { it.id },
        )
        assertTrue(
            tempDir.listFiles()?.map { it.name }?.containsAll(
                randomWallhavenWallpapers2.map { it.path.getFileNameFromUrl() },
            ) ?: false,
        )
        assertEquals(
            redditWallpapers2.sortedBy { it.id },
            redditWallpapersDao.getAll().sortedBy { it.id },
        )
        assertTrue(
            tempDir.listFiles()?.map { it.name }?.containsAll(
                randomRedditWallpapers2.map { it.url.getFileNameFromUrl() },
            ) ?: false,
        )

        // forward time by 2 days
        clock.plus(2.days)

        // worker should delete everything
        worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallhavenWallpapersDao.count())
        assertEquals(0, redditWallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesOnSamesDatesCommonWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        val redditWallpapersDao = db.redditWallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val query1NewCount = 20
        val fakeNetworkWallhavenWallpapers1 = MockFactory.generateNetworkWallhavenWallpapers(
            query1NewCount,
        )
        val fakeNetworkRedditPosts1 = MockFactory.generateNetworkRedditPosts(
            query1NewCount,
            singleImageOnly = true,
        )
        simulateSearch(
            clock,
            query1,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers1,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts1,
        )
        assertEquals(query1NewCount, wallhavenWallpapersDao.count())
        assertEquals(query1NewCount, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 5 wallpapers from fakeNetworkWallhavenWallpapers1 and fakeNetworkRedditPosts1
        val cachedCount1 = 5
        val wallhavenIds1 = fakeNetworkWallhavenWallpapers1.map { it.id }
        val wallhavenWallpapers1 = wallhavenWallpapersDao.getByWallhavenIds(wallhavenIds1)
        val randomWallhavenWallpapers1 = wallhavenWallpapers1.shuffled().take(cachedCount1)
        val redditIds1 = getRedditIds(fakeNetworkRedditPosts1)
        val redditWallpapers1 = redditWallpapersDao.getByRedditIds(redditIds1)
        val randomRedditWallpapers1 = redditWallpapers1.shuffled().take(cachedCount1)
        createTempFiles(
            randomWallhavenWallpapers1 + randomRedditWallpapers1,
            clock.now(),
        )

        assertEquals(cachedCount1 * 2, tempDir.listFiles()?.size)

        // For query2, we will create a mix of the following wallpapers:
        // 1. 10 new wallpapers
        // 2. 3 from query1 temp file cached wallpapers
        // 3. 7 from query1 wallpapers which are not cached

        // 10 new wallpapers
        val query2NewCount = 10
        val newQuery2WallhavenWallpapers = MockFactory.generateNetworkWallhavenWallpapers(
            query2NewCount,
        )
        val fakeNetworkWallhavenWallpapers2 = newQuery2WallhavenWallpapers.toMutableList()
        // take 3 from cached
        val query1CachedWallhavenIds = randomWallhavenWallpapers1
            .shuffled()
            .take(3)
            .map { it.wallhavenId }
        val cachedWallhavenWallpapers = fakeNetworkWallhavenWallpapers1.filter {
            it.id in query1CachedWallhavenIds
        }
        fakeNetworkWallhavenWallpapers2 += cachedWallhavenWallpapers
        // take 7 from non-cached
        fakeNetworkWallhavenWallpapers2 +=
            (fakeNetworkWallhavenWallpapers1 - cachedWallhavenWallpapers.toSet())
                .shuffled()
                .take(7)
        val newQuery2RedditPosts = MockFactory.generateNetworkRedditPosts(
            query2NewCount,
            singleImageOnly = true,
        )
        val fakeNetworkRedditPosts2 = newQuery2RedditPosts.toMutableList()
        // take 3 from cached
        val query1CachedRedditIds = randomRedditWallpapers1
            .shuffled()
            .take(3)
            .map { it.redditId }
        val cachedRedditPosts = fakeNetworkRedditPosts2.filter {
            it.id in query1CachedRedditIds
        }
        fakeNetworkRedditPosts2 += cachedRedditPosts
        // take 7 from non-cached
        fakeNetworkRedditPosts2 +=
            (fakeNetworkRedditPosts1 - cachedRedditPosts.toSet())
                .shuffled()
                .take(7)

        val query2 = "test2"
        simulateSearch(
            clock,
            query2,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers2,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts2,
        )
        assertEquals(query1NewCount + query2NewCount, wallhavenWallpapersDao.count())
        assertEquals(query1NewCount + query2NewCount, redditWallpapersDao.count())
        assertEquals(4, searchQueryDao.count())

        // we cache 3 wallpapers from new query2 wallpapers
        val newCacheCount2 = 3
        val wallhavenIds2 = newQuery2WallhavenWallpapers.map { it.id }
        val wallhavenWallpapers2 = wallhavenWallpapersDao.getByWallhavenIds(wallhavenIds2)
        val randomWallhavenWallpapers2 = wallhavenWallpapers2
            .shuffled()
            .take(newCacheCount2)
            .toMutableList()
        // 1 from prev cached wallpapers
        randomWallhavenWallpapers2 += wallhavenWallpapersDao.getByWallhavenIds(
            query1CachedWallhavenIds.shuffled().take(1),
        )
        val redditIds2 = getRedditIds(newQuery2RedditPosts)
        val redditWallpapers2 = redditWallpapersDao.getByRedditIds(redditIds2)
        val randomRedditWallpapers2 = redditWallpapers2
            .shuffled()
            .take(newCacheCount2)
            .toMutableList()
        // 1 from prev cached wallpapers
        randomRedditWallpapers2 += redditWallpapersDao.getByRedditIds(
            query1CachedRedditIds.shuffled().take(1),
        )
        createTempFiles(
            randomWallhavenWallpapers2 + randomRedditWallpapers2,
            clock.now(),
        )

        assertEquals(
            (cachedCount1 + newCacheCount2) * 2,
            tempDir.listFiles()?.size,
        )

        // forward time by 8 days
        clock.plus(8.days)

        // worker should delete everything
        val worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallhavenWallpapersDao.count())
        assertEquals(0, redditWallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesOnDifferentDatesCommonWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        val redditWallpapersDao = db.redditWallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val query1NewCount = 20
        val fakeNetworkWallhavenWallpapers1 = MockFactory.generateNetworkWallhavenWallpapers(
            query1NewCount,
        )
        val fakeNetworkRedditPosts1 = MockFactory.generateNetworkRedditPosts(
            query1NewCount,
            singleImageOnly = true,
        )
        simulateSearch(
            clock,
            query1,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers1,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts1,
        )
        assertEquals(query1NewCount, wallhavenWallpapersDao.count())
        assertEquals(query1NewCount, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 5 wallpapers from fakeNetworkWallhavenWallpapers1 and fakeNetworkRedditPosts1
        val cachedCount1 = 5
        val wallhavenIds1 = fakeNetworkWallhavenWallpapers1.map { it.id }
        val wallhavenWallpapers1 = wallhavenWallpapersDao.getByWallhavenIds(wallhavenIds1)
        val randomWallhavenWallpapers1 = wallhavenWallpapers1.shuffled().take(cachedCount1)
        val redditIds1 = getRedditIds(fakeNetworkRedditPosts1)
        val redditWallpapers1 = redditWallpapersDao.getByRedditIds(redditIds1)
        val randomRedditWallpapers1 = redditWallpapers1.shuffled().take(cachedCount1)
        createTempFiles(
            randomWallhavenWallpapers1 + randomRedditWallpapers1,
            clock.now(),
        )

        assertEquals(cachedCount1 * 2, tempDir.listFiles()?.size)

        // For query2, we will create a mix of the following wallpapers:
        // 1. 10 new wallpapers
        // 2. 3 from query1 temp file cached wallpapers
        // 3. 7 from query1 wallpapers which are not cached

        // 10 new wallpapers
        val query2NewCount = 10
        val newQuery2WallhavenWallpapers = MockFactory.generateNetworkWallhavenWallpapers(
            query2NewCount,
        )
        val fakeNetworkWallhavenWallpapers2 = newQuery2WallhavenWallpapers.toMutableList()
        // take 3 from cached
        val query1CachedWallhavenIds = randomWallhavenWallpapers1
            .shuffled()
            .take(3)
            .map { it.wallhavenId }
        val cachedWallhavenWallpapers = fakeNetworkWallhavenWallpapers1.filter {
            it.id in query1CachedWallhavenIds
        }
        fakeNetworkWallhavenWallpapers2 += cachedWallhavenWallpapers
        // take 7 from non-cached
        val nonCachedWallhavenWallpapers =
            (fakeNetworkWallhavenWallpapers1 - cachedWallhavenWallpapers.toSet())
                .shuffled()
                .take(7)
        fakeNetworkWallhavenWallpapers2 += nonCachedWallhavenWallpapers
        val newQuery2RedditPosts = MockFactory.generateNetworkRedditPosts(
            query2NewCount,
            singleImageOnly = true,
        )
        val fakeNetworkRedditPosts2 = newQuery2RedditPosts.toMutableList()
        // take 3 from cached
        val query1CachedRedditIds = randomRedditWallpapers1
            .shuffled()
            .take(3)
            .map { it.redditId }
        val cachedRedditPosts = fakeNetworkRedditPosts1.filter {
            val redditIds = getRedditIds(listOf(it))
            redditIds.first() in query1CachedRedditIds
        }
        fakeNetworkRedditPosts2 += cachedRedditPosts
        // take 7 from non-cached
        val nonCachedRedditPosts = (fakeNetworkRedditPosts1 - cachedRedditPosts.toSet())
            .shuffled()
            .take(7)
        fakeNetworkRedditPosts2 += nonCachedRedditPosts
        // forward time by 2.days
        clock.plus(2.days)
        val query2 = "test2"
        simulateSearch(
            clock,
            query2,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers2,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts2,
        )
        assertEquals(query1NewCount + query2NewCount, wallhavenWallpapersDao.count())
        assertEquals(query1NewCount + query2NewCount, redditWallpapersDao.count())
        assertEquals(4, searchQueryDao.count())

        // we cache 3 wallpapers from new query2 wallpapers
        val newCacheCount2 = 3
        val wallhavenIds2 = newQuery2WallhavenWallpapers.map { it.id }
        val wallhavenWallpapers2 = wallhavenWallpapersDao.getByWallhavenIds(wallhavenIds2)
        val randomWallhavenWallpapers2 = wallhavenWallpapers2
            .shuffled()
            .take(newCacheCount2)
            .toMutableList()
        // 1 from prev cached wallpapers
        val updatedLastModifiedWallhavenWallpaper = wallhavenWallpapersDao.getByWallhavenIds(
            query1CachedWallhavenIds.shuffled().take(1),
        )
        randomWallhavenWallpapers2 += updatedLastModifiedWallhavenWallpaper
        val redditIds2 = getRedditIds(newQuery2RedditPosts)
        val redditWallpapers2 = redditWallpapersDao.getByRedditIds(redditIds2)
        val randomRedditWallpapers2 = redditWallpapers2
            .shuffled()
            .take(newCacheCount2)
            .toMutableList()
        // 1 from prev cached wallpapers
        val updatedLastModifiedRedditWallpaper = redditWallpapersDao.getByRedditIds(
            query1CachedRedditIds.shuffled().take(1),
        )
        randomRedditWallpapers2 += updatedLastModifiedRedditWallpaper
        createTempFiles(
            randomWallhavenWallpapers2 + randomRedditWallpapers2,
            clock.now(),
        )

        assertEquals(
            (cachedCount1 + newCacheCount2) * 2,
            tempDir.listFiles()?.size,
        )

        val allWallhavenWallpapers = wallhavenWallpapersDao.getAll()
        val allRedditWallpapers = redditWallpapersDao.getAll()

        // worker should only delete query1 and cache/data unique to it
        // here wallpapers unique to query1 are:
        // mockNetworkWallpapers1 - (10 wallpapers taken from query1 wallpapers used in query2)
        val expectedDeletedNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers1
            .minus(cachedWallhavenWallpapers.toSet())
            .minus(nonCachedWallhavenWallpapers.toSet())
        val expectedDeletedWallhavenIds = expectedDeletedNetworkWallhavenWallpapers.map { it.id }
        val expectedDeletedWallhavenWallpapers = allWallhavenWallpapers.filter {
            it.wallhavenId in expectedDeletedWallhavenIds
        }
        val expectedRemainingWallhavenWallpapers = allWallhavenWallpapers -
            expectedDeletedWallhavenWallpapers.toSet()

        val expectedDeletedNetworkRedditPosts = fakeNetworkRedditPosts1
            .minus(cachedRedditPosts.toSet())
            .minus(nonCachedRedditPosts.toSet())
        val expectedDeletedRedditIds = getRedditIds(expectedDeletedNetworkRedditPosts)
        val expectedDeletedRedditWallpapers = allRedditWallpapers.filter {
            it.redditId in expectedDeletedRedditIds
        }
        val expectedRemainingRedditWallpapers = allRedditWallpapers -
            expectedDeletedRedditWallpapers.toSet()

        // worker will also delete temp files older than 7 days
        // which means all cached query1 files will be deleted except the 1 with updated
        // last modified time
        val expectedDeletedWallpaperFileNames = randomWallhavenWallpapers1.map {
            it.path.getFileNameFromUrl()
        } + randomRedditWallpapers1.map {
            it.url.getFileNameFromUrl()
        } - updatedLastModifiedWallhavenWallpaper.map {
            it.path.getFileNameFromUrl()
        }.toSet() - updatedLastModifiedRedditWallpaper.map {
            it.url.getFileNameFromUrl()
        }.toSet()
        val expectedRemainingCachedFileNames = tempDir.listFiles()?.filter {
            it.name !in expectedDeletedWallpaperFileNames
        } ?: emptyList()

        // forward time by 6 days
        clock.plus(6.days)

        var worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(
            expectedRemainingWallhavenWallpapers.sortedBy { it.id },
            wallhavenWallpapersDao.getAll().sortedBy { it.id },
        )
        assertEquals(
            expectedRemainingRedditWallpapers.sortedBy { it.id },
            redditWallpapersDao.getAll().sortedBy { it.id },
        )
        assertEquals(2, searchQueryDao.count())
        assertEquals(expectedRemainingCachedFileNames.sorted(), tempDir.listFiles()?.sorted())

        // forward time by 2 days
        clock.plus(2.days)

        // worker should delete everything
        worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallhavenWallpapersDao.count())
        assertEquals(0, redditWallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorkerOldTempFiles() = runTest(testDispatcher) {
        // create temp files
        val wallhavenWallpapers = MockFactory.generateNetworkWallhavenWallpapers(10).map {
            it.toWallpaperEntity()
        }
        val redditWallpapers = MockFactory.generateNetworkRedditPosts(
            10,
            singleImageOnly = true,
        ).flatMap {
            it.toWallpaperEntities()
        }
        val now = Clock.System.now()
        createTempFiles(
            wallhavenWallpapers + redditWallpapers,
            now,
        )
        val clock = TestClock(now)
        // forward time by 2 days
        clock.plus(2.days)
        assertEquals(20, tempDir.listFiles()?.size)

        // forward time by 6 days
        clock.plus(6.days)
        val worker = getWorker(clock = clock)
        worker.doWork()
        // everything should be deleted
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker1QueriesWithFavoriteWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        val redditWallpapersDao = db.redditWallpapersDao()
        val favoriteDao = db.favoriteDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val query1NewCount = 20
        val fakeNetworkWallhavenWallpapers1 = MockFactory.generateNetworkWallhavenWallpapers(
            query1NewCount,
        )
        val fakeNetworkRedditPosts1 = MockFactory.generateNetworkRedditPosts(
            query1NewCount,
            singleImageOnly = true,
        )
        simulateSearch(
            clock,
            query1,
            fakeNetworkWallhavenWallpapers = fakeNetworkWallhavenWallpapers1,
            fakeNetworkRedditPosts = fakeNetworkRedditPosts1,
        )
        assertEquals(query1NewCount, wallhavenWallpapersDao.count())
        assertEquals(query1NewCount, redditWallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 5 wallpapers from fakeNetworkWallhavenWallpapers1 + fakeNetworkRedditPosts1
        val cachedCount1 = 5
        val wallhavenIds1 = fakeNetworkWallhavenWallpapers1.map { it.id }
        val wallhavenWallpapers1 = wallhavenWallpapersDao.getByWallhavenIds(wallhavenIds1)
        val randomWallhavenWallpapers1 = wallhavenWallpapers1.shuffled().take(cachedCount1)
        val redditIds1 = getRedditIds(fakeNetworkRedditPosts1)
        val redditWallpapers1 = redditWallpapersDao.getByRedditIds(redditIds1)
        val randomRedditWallpapers1 = redditWallpapers1.shuffled().take(cachedCount1)
        createTempFiles(
            randomWallhavenWallpapers1 + randomRedditWallpapers1,
            clock.now(),
        )

        assertEquals(cachedCount1 * 2, tempDir.listFiles()?.size)

        // We favorite 2 random wallpapers from non-cached wallpapers
        val query1CachedWallhavenIds = randomWallhavenWallpapers1.map { it.wallhavenId }
        val query1CachedRedditIds = randomRedditWallpapers1.map { it.redditId }
        val favoriteEntities = fakeNetworkWallhavenWallpapers1
            .filter {
                it.id !in query1CachedWallhavenIds
            }
            .take(2)
            .map {
                FavoriteEntity(
                    id = 0,
                    sourceId = it.id,
                    source = Source.WALLHAVEN,
                    favoritedOn = clock.now(),
                )
            } + fakeNetworkRedditPosts1
            .filter {
                val redditIds = getRedditIds(listOf(it))
                redditIds.first() !in query1CachedRedditIds
            }
            .take(2)
            .map {
                FavoriteEntity(
                    id = 0,
                    sourceId = getRedditIds(listOf(it)).first(),
                    source = Source.REDDIT,
                    favoritedOn = clock.now(),
                )
            }
        favoriteDao.insertAll(favoriteEntities)

        // forward time by 8 days
        clock.plus(8.days)

        // worker should not delete everything
        val worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(2, wallhavenWallpapersDao.count())
        assertEquals(2, redditWallpapersDao.count())

        val remainingWallhavenWallpapers = wallhavenWallpapersDao.getAll()
        assertEquals(
            remainingWallhavenWallpapers
                .map { it.wallhavenId }
                .sorted(),
            favoriteEntities
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
                .sorted(),
        )
        val remainingRedditWallpapers = redditWallpapersDao.getAll()
        assertEquals(
            remainingRedditWallpapers
                .map { it.redditId }
                .sorted(),
            favoriteEntities
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
                .sorted(),
        )
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalPagingApi::class)
    private suspend fun simulateSearch(
        clock: Clock,
        query: String,
        perPage: Int = 20,
        totalResultsCount: Int = 198,
        fakeNetworkWallhavenWallpapers: List<NetworkWallhavenWallpaper> =
            MockFactory.generateNetworkWallhavenWallpapers(20),
        fakeNetworkRedditPosts: List<NetworkRedditPost> =
            MockFactory.generateNetworkRedditPosts(20, singleImageOnly = true),
    ) {
        val wallhavenSearch = WallhavenSearch(
            filters = WallhavenFilters(includedTags = setOf(query)),
        )
        fakeWallhavenNetworkApi.setWallpapersForQuery(
            query = wallhavenSearch.getApiQueryString(),
            networkWallhavenWallpapers = fakeNetworkWallhavenWallpapers,
            meta = NetworkWallhavenMeta(
                query = StringNetworkWallhavenMetaQuery(""),
                current_page = 1,
                last_page = totalResultsCount / perPage + 1,
                per_page = perPage,
                total = totalResultsCount,
            ),
        )
        val wallhavenRemoteMediator =
            WallpapersRemoteMediator<WallhavenSearch, WallhavenWallpaperEntity>(
                wallhavenSearch,
                db,
                wallHavenNetworkDataSource,
                clock,
            )
        val wallhavenPagingState = PagingState<Int, WallhavenWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        wallhavenRemoteMediator.load(LoadType.REFRESH, wallhavenPagingState)

        val redditSearch = RedditSearch(
            query = query,
            filters = RedditFilters(
                subreddits = setOf("test"),
                includeNsfw = false,
                sort = RedditSort.RELEVANCE,
                timeRange = RedditTimeRange.ALL,
            ),
        )
        fakeRedditNetworkApi.setPostsForQuery(
            query = "self:no $query",
            networkRedditPosts = fakeNetworkRedditPosts,
            after = "after",
        )
        val redditRemoteMediator =
            WallpapersRemoteMediator<RedditSearch, RedditWallpaperEntity>(
                redditSearch,
                db,
                redditNetworkDataSource,
                clock,
            )
        val redditPagingState = PagingState<Int, RedditWallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10,
        )
        redditRemoteMediator.load(LoadType.REFRESH, redditPagingState)
    }

    private fun createTempFiles(
        wallpapers: List<OnlineSourceWallpaperEntity>,
        now: Instant,
    ) {
        val fileNames = wallpapers.map {
            when (it) {
                is WallhavenWallpaperEntity -> it.path.getFileNameFromUrl()
                is RedditWallpaperEntity -> it.url.getFileNameFromUrl()
                else -> throw RuntimeException()
            }
        }
        fileNames.forEach {
            val tempFile = context.getTempFile(it)
            tempFile.createNewFile()
            tempFile.setLastModified(now.toEpochMilliseconds())
        }
    }

    private fun getRedditIds(redditPosts: List<NetworkRedditPost>) = redditPosts
        .filter { !it.is_video }
        .flatMap {
            if (it.is_gallery) {
                it.media_metadata?.keys?.toList() ?: emptyList()
            } else {
                val id = it.preview?.images?.first()?.id ?: return@flatMap emptyList()
                listOf(id)
            }
        }

    private fun getWorker(
        clock: Clock = TestClock(Clock.System.now()),
    ): CleanupWorker {
        val workTaskExecutor = InstantWorkTaskExecutor()
        return CleanupWorker(
            context = context,
            params = WorkerParameters(
                UUID.randomUUID(),
                Data.EMPTY,
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                0,
                workTaskExecutor.serialTaskExecutor,
                workTaskExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                TestProgressUpdater(),
                TestForegroundUpdater(),
            ),
            clock = clock,
            appDatabase = db,
            ioDispatcher = testDispatcher,
        )
    }
}
