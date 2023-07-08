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
import com.ammar.wallflow.data.db.database.AppDatabase
import com.ammar.wallflow.data.db.entity.WallpaperEntity
import com.ammar.wallflow.data.network.model.NetworkMeta
import com.ammar.wallflow.data.network.model.NetworkWallpaper
import com.ammar.wallflow.data.network.model.StringNetworkMetaQuery
import com.ammar.wallflow.data.network.model.asWallpaperEntity
import com.ammar.wallflow.data.network.retrofit.RetrofitWallHavenNetwork
import com.ammar.wallflow.data.repository.MockFactory
import com.ammar.wallflow.data.repository.MockWallHavenNetworkApi
import com.ammar.wallflow.data.repository.WallpapersRemoteMediator
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getTempDir
import com.ammar.wallflow.extensions.getTempFile
import com.ammar.wallflow.model.SearchQuery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import java.io.File
import java.util.UUID
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
class CleanupWorkerTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var tempDir: File
    private val mockNetworkApi = MockWallHavenNetworkApi()
    private val wallHavenNetworkDataSource = RetrofitWallHavenNetwork(mockNetworkApi)
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

        mockkStatic("com.ammar.havenwalls.extensions.Context_extKt")

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
        mockNetworkApi.failureMsg = null
        mockNetworkApi.clearMockData()
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
        val wallpapersDao = db.wallpapersDao()
        assertEquals(20, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())

        // we cache 5 wallpapers in temp dir
        val wallpapers = wallpapersDao.getAll()
        val cachedCount = 5
        val randomWallpapers = wallpapers.shuffled().take(cachedCount)
        createTempFiles(randomWallpapers, clock.now())

        assertEquals(cachedCount, tempDir.listFiles()?.size)

        // forward time by 2 days
        clock.plus(2.days)
        var worker = getWorker(clock = clock)
        worker.doWork()

        // nothing should be deleted
        assertEquals(20, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())
        assertEquals(cachedCount, tempDir.listFiles()?.size)

        // forward time by 6 days
        clock.plus(6.days)
        worker = getWorker(clock = clock)
        worker.doWork()

        // everything should be deleted
        assertEquals(0, wallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesUniqueWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallpapersDao = db.wallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        simulateSearch(clock, query1)
        assertEquals(20, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())

        val query2 = "test2"
        simulateSearch(clock, query2)
        assertEquals(40, wallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 10 wallpapers in temp dir
        val wallpapers = wallpapersDao.getAll()
        val cachedCount = 10
        val randomWallpapers = wallpapers.shuffled().take(cachedCount)
        createTempFiles(randomWallpapers, clock.now())

        assertEquals(cachedCount, tempDir.listFiles()?.size)

        // forward time by 8 days
        clock.plus(8.days)
        val worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesOnDifferentDatesUniqueWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallpapersDao = db.wallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val mockNetworkWallpapers1 = MockFactory.generateNetworkWallpapers(20)
        simulateSearch(clock, query1, mockNetworkWallpapers = mockNetworkWallpapers1)
        assertEquals(20, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())

        val cachedCount = 5

        // we cache 5 wallpapers from mockNetworkWallpapers1
        val wallHavenIds1 = mockNetworkWallpapers1.map { it.id }
        val wallpapers1 = wallpapersDao.getByWallhavenIds(wallHavenIds1)
        val randomWallpapers1 = wallpapers1.shuffled().take(cachedCount)
        createTempFiles(randomWallpapers1, clock.now())

        assertEquals(cachedCount, tempDir.listFiles()?.size)

        clock.plus(2.days)
        val query2 = "test2"
        val mockNetworkWallpapers2 = MockFactory.generateNetworkWallpapers(20)
        simulateSearch(clock, query2, mockNetworkWallpapers = mockNetworkWallpapers2)
        assertEquals(40, wallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 5 wallpapers from mockNetworkWallpapers2
        val wallHavenIds2 = mockNetworkWallpapers2.map { it.id }
        val wallpapers2 = wallpapersDao.getByWallhavenIds(wallHavenIds2)
        val randomWallpapers2 = wallpapers2.shuffled().take(cachedCount)
        createTempFiles(randomWallpapers2, clock.now())

        assertEquals(cachedCount * 2, tempDir.listFiles()?.size)

        // forward time by 6 days
        clock.plus(6.days)

        // worker should only delete searchQuery1 and related cache
        var worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(20, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())
        assertEquals(cachedCount, tempDir.listFiles()?.size)
        assertEquals(wallpapers2.sortedBy { it.id }, wallpapersDao.getAll().sortedBy { it.id })
        assertEquals(
            randomWallpapers2.map { it.path.getFileNameFromUrl() }.sorted(),
            tempDir.listFiles()?.map { it.name }?.sorted(),
        )

        // forward time by 2 days
        clock.plus(2.days)

        // worker should delete everything
        worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesOnSamesDatesCommonWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallpapersDao = db.wallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val query1NewCount = 20
        val mockNetworkWallpapers1 = MockFactory.generateNetworkWallpapers(query1NewCount)
        simulateSearch(clock, query1, mockNetworkWallpapers = mockNetworkWallpapers1)
        assertEquals(query1NewCount, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())

        // we cache 5 wallpapers from mockNetworkWallpapers1
        val cachedCount1 = 5
        val wallHavenIds1 = mockNetworkWallpapers1.map { it.id }
        val wallpapers1 = wallpapersDao.getByWallhavenIds(wallHavenIds1)
        val randomWallpapers1 = wallpapers1.shuffled().take(cachedCount1)
        createTempFiles(randomWallpapers1, clock.now())

        assertEquals(cachedCount1, tempDir.listFiles()?.size)

        // For query2, we will create a mix of the following wallpapers:
        // 1. 10 new wallpapers
        // 2. 3 from query1 temp file cached wallpapers
        // 3. 7 from query1 wallpapers which are not cached

        // 10 new wallpapers
        val query2NewCount = 10
        val newQuery2Wallpapers = MockFactory.generateNetworkWallpapers(query2NewCount)
        val mockNetworkWallpapers2 = newQuery2Wallpapers.toMutableList()
        // take 3 from cached
        val query1CachedWallHavenIds = randomWallpapers1.shuffled().take(3).map { it.wallhavenId }
        val cached = mockNetworkWallpapers1.filter { it.id in query1CachedWallHavenIds }
        mockNetworkWallpapers2 += cached
        // take 7 from non-cached
        mockNetworkWallpapers2 += (mockNetworkWallpapers1 - cached.toSet()).shuffled().take(7)

        val query2 = "test2"
        simulateSearch(clock, query2, mockNetworkWallpapers = mockNetworkWallpapers2)
        assertEquals(query1NewCount + query2NewCount, wallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 3 wallpapers from new query2 wallpapers
        val newCacheCount2 = 3
        val wallHavenIds2 = newQuery2Wallpapers.map { it.id }
        val wallpapers2 = wallpapersDao.getByWallhavenIds(wallHavenIds2)
        val randomWallpapers2 = wallpapers2.shuffled().take(newCacheCount2).toMutableList()
        // 1 from prev cached wallpapers
        randomWallpapers2 += wallpapersDao.getByWallhavenIds(
            query1CachedWallHavenIds.shuffled().take(1)
        )
        createTempFiles(randomWallpapers2, clock.now())

        assertEquals(cachedCount1 + newCacheCount2, tempDir.listFiles()?.size)

        // forward time by 8 days
        clock.plus(8.days)

        // worker should delete everything
        val worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorker2QueriesOnDifferentDatesCommonWalls() = runTest(testDispatcher) {
        val searchQueryDao = db.searchQueryDao()
        val wallpapersDao = db.wallpapersDao()

        val clock = TestClock(Clock.System.now())
        val query1 = "test1"
        val query1NewCount = 20
        val mockNetworkWallpapers1 = MockFactory.generateNetworkWallpapers(query1NewCount)
        simulateSearch(clock, query1, mockNetworkWallpapers = mockNetworkWallpapers1)
        assertEquals(query1NewCount, wallpapersDao.count())
        assertEquals(1, searchQueryDao.count())

        // we cache 5 wallpapers from mockNetworkWallpapers1
        val cachedCount1 = 5
        val wallHavenIds1 = mockNetworkWallpapers1.map { it.id }
        val wallpapers1 = wallpapersDao.getByWallhavenIds(wallHavenIds1)
        val randomWallpapers1 = wallpapers1.shuffled().take(cachedCount1)
        createTempFiles(randomWallpapers1, clock.now())

        assertEquals(cachedCount1, tempDir.listFiles()?.size)

        // For query2, we will create a mix of the following wallpapers:
        // 1. 10 new wallpapers
        // 2. 3 from query1 temp file cached wallpapers
        // 3. 7 from query1 wallpapers which are not cached

        // 10 new wallpapers
        val query2NewCount = 10
        val newQuery2Wallpapers = MockFactory.generateNetworkWallpapers(query2NewCount)
        val mockNetworkWallpapers2 = newQuery2Wallpapers.toMutableList()
        // take 3 from cached
        val query1CachedWallHavenIds = randomWallpapers1.shuffled().take(3).map { it.wallhavenId }
        val cached = mockNetworkWallpapers1.filter { it.id in query1CachedWallHavenIds }
        mockNetworkWallpapers2 += cached
        // take 7 from non-cached
        val nonCached = (mockNetworkWallpapers1 - cached.toSet()).shuffled().take(7)
        mockNetworkWallpapers2 += nonCached
        // forward time by 2.days
        clock.plus(2.days)
        val query2 = "test2"
        simulateSearch(clock, query2, mockNetworkWallpapers = mockNetworkWallpapers2)
        assertEquals(query1NewCount + query2NewCount, wallpapersDao.count())
        assertEquals(2, searchQueryDao.count())

        // we cache 3 wallpapers from new query2 wallpapers
        val newCacheCount2 = 3
        val wallHavenIds2 = newQuery2Wallpapers.map { it.id }
        val wallpapers2 = wallpapersDao.getByWallhavenIds(wallHavenIds2)
        val randomWallpapers2 = wallpapers2.shuffled().take(newCacheCount2).toMutableList()
        // 1 from prev cached wallpapers
        val updatedLastModifiedWallpaper = wallpapersDao.getByWallhavenIds(
            query1CachedWallHavenIds.shuffled().take(1)
        )
        randomWallpapers2 += updatedLastModifiedWallpaper
        createTempFiles(randomWallpapers2, clock.now())

        assertEquals(cachedCount1 + newCacheCount2, tempDir.listFiles()?.size)

        val allWallpapers = wallpapersDao.getAll()

        // worker should only delete query1 and cache/data unique to it
        // here wallpapers unique to query1 are:
        // mockNetworkWallpapers1 - (10 wallpapers taken from query1 wallpapers used in query2)
        val expectedDeletedNetworkWallpapers = mockNetworkWallpapers1
            .minus(cached.toSet())
            .minus(nonCached.toSet())
        val expectedDeletedWallhavenIds = expectedDeletedNetworkWallpapers.map { it.id }
        val expectedDeletedWallpapers = allWallpapers.filter {
            it.wallhavenId in expectedDeletedWallhavenIds
        }
        val expectedRemainingWallpapers = allWallpapers - expectedDeletedWallpapers.toSet()

        // worker will also delete temp files older than 7 days
        // which means all cached query1 files will be deleted except the 1 with updated
        // last modified time
        val expectedDeletedWallpaperFileNames = randomWallpapers1.map {
            it.path.getFileNameFromUrl()
        } - updatedLastModifiedWallpaper.map {
            it.path.getFileNameFromUrl()
        }.toSet()
        val expectedRemainingCachedFileNames = tempDir.listFiles()?.filter {
            it.name !in expectedDeletedWallpaperFileNames
        } ?: emptyList()

        // forward time by 6 days
        clock.plus(6.days)

        var worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(
            expectedRemainingWallpapers.sortedBy { it.id },
            wallpapersDao.getAll().sortedBy { it.id },
        )
        assertEquals(1, searchQueryDao.count())
        assertEquals(expectedRemainingCachedFileNames.sorted(), tempDir.listFiles()?.sorted())

        // forward time by 2 days
        clock.plus(2.days)

        // worker should delete everything
        worker = getWorker(clock = clock)
        worker.doWork()
        assertEquals(0, wallpapersDao.count())
        assertEquals(0, searchQueryDao.count())
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Test
    fun testCleanupWorkerOldTempFiles() = runTest(testDispatcher) {
        // create temp files
        val wallpapers = MockFactory.generateNetworkWallpapers(10).map {
            it.asWallpaperEntity()
        }
        val now = Clock.System.now()
        createTempFiles(wallpapers, now)
        val clock = TestClock(now)
        // forward time by 2 days
        clock.plus(2.days)
        assertEquals(10, tempDir.listFiles()?.size)

        // forward time by 6 days
        clock.plus(6.days)
        val worker = getWorker(clock = clock)
        worker.doWork()
        // everything should be deleted
        assertEquals(0, tempDir.listFiles()?.size)
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalPagingApi::class)
    private suspend fun simulateSearch(
        clock: Clock,
        query: String,
        perPage: Int = 20,
        totalResultsCount: Int = 198,
        mockNetworkWallpapers: List<NetworkWallpaper> = MockFactory.generateNetworkWallpapers(20),
    ) {
        val searchQuery = SearchQuery(includedTags = setOf(query))
        mockNetworkApi.setWallpapersForQuery(
            query = searchQuery.getQString(),
            networkWallpapers = mockNetworkWallpapers,
            meta = NetworkMeta(
                query = StringNetworkMetaQuery(""),
                current_page = 1,
                last_page = totalResultsCount / perPage + 1,
                per_page = perPage,
                total = totalResultsCount,
            )
        )
        val remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            db,
            wallHavenNetworkDataSource,
            clock,
        )
        val pagingState = PagingState<Int, WallpaperEntity>(
            listOf(),
            null,
            PagingConfig(10),
            10
        )
        remoteMediator.load(LoadType.REFRESH, pagingState)
    }

    private fun createTempFiles(
        wallpapers: List<WallpaperEntity>,
        now: Instant,
    ) {
        val fileNames = wallpapers.map { it.path.getFileNameFromUrl() }
        fileNames.forEach {
            val tempFile = context.getTempFile(it)
            tempFile.createNewFile()
            tempFile.setLastModified(now.toEpochMilliseconds())
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
