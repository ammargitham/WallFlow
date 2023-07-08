package com.ammar.wallflow.workers

import android.content.Context
import androidx.compose.ui.unit.IntSize
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.SavedSearchDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.SavedSearchEntity
import com.ammar.wallflow.data.network.WallHavenNetworkDataSource
import com.ammar.wallflow.data.network.model.NetworkMeta
import com.ammar.wallflow.data.network.model.NetworkResponse
import com.ammar.wallflow.data.network.model.NetworkTag
import com.ammar.wallflow.data.network.model.NetworkThumbs
import com.ammar.wallflow.data.network.model.NetworkWallpaper
import com.ammar.wallflow.data.network.model.StringNetworkMetaQuery
import com.ammar.wallflow.data.network.model.asWallpaper
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.AutoWallpaperHistoryRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.extensions.getTempFile
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.SavedSearch
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.toEntity
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.FailureReason
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.jsoup.nodes.Document
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
class AutoWallpaperTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    private open class TestSavedSearchDao : SavedSearchDao {
        override fun getAll(): Flow<List<SavedSearchEntity>> {
            throw RuntimeException()
        }

        override suspend fun getById(id: Long): SavedSearchEntity? {
            throw RuntimeException()
        }

        override suspend fun getByName(name: String): SavedSearchEntity? {
            throw RuntimeException()
        }

        override suspend fun upsert(savedSearchDao: SavedSearchEntity) {
            throw RuntimeException()
        }

        override suspend fun deleteByName(name: String) {
            throw RuntimeException()
        }

    }

    private open class TestAutoWallpaperHistoryDao : AutoWallpaperHistoryDao {
        override suspend fun getAll(): List<AutoWallpaperHistoryEntity> {
            throw RuntimeException()
        }

        override suspend fun getByWallhavenId(wallhavenId: String): AutoWallpaperHistoryEntity? {
            throw RuntimeException()
        }

        override suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity) {
            throw RuntimeException()
        }

    }

    private open class TestObjectDetectionModelDao : ObjectDetectionModelDao {
        override fun getAll(): Flow<List<ObjectDetectionModelEntity>> {
            throw RuntimeException()
        }

        override suspend fun getById(id: Long): ObjectDetectionModelEntity? {
            throw RuntimeException()
        }

        override suspend fun getByName(name: String): ObjectDetectionModelEntity? {
            throw RuntimeException()
        }

        override suspend fun nameExists(name: String): Boolean {
            throw RuntimeException()
        }

        override suspend fun nameExistsExcludingId(id: Long, name: String): Boolean {
            throw RuntimeException()
        }

        override suspend fun upsert(vararg objectDetectionModelEntity: ObjectDetectionModelEntity) {
            throw RuntimeException()
        }

        override suspend fun deleteByName(name: String) {
            throw RuntimeException()
        }

        override suspend fun delete(entity: ObjectDetectionModelEntity) {
            throw RuntimeException()
        }

    }

    private open class TestWallHavenNetworkDataSource : WallHavenNetworkDataSource {
        override suspend fun search(
            searchQuery: SearchQuery,
            page: Int?,
        ): NetworkResponse<List<NetworkWallpaper>> {
            throw RuntimeException()
        }

        override suspend fun wallpaper(wallpaperWallhavenId: String): NetworkResponse<NetworkWallpaper> {
            throw RuntimeException()
        }

        override suspend fun popularTags(): Document? {
            throw RuntimeException()
        }

    }

    private val testOkHttpClient = object : OkHttpClient() {
        override fun newCall(request: Request): Call {
            // Overriding to throw error if download requested
            throw IllegalStateException("Test called okhttp client!")
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testAutoWallpaperWorkerInitial() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val worker = getWorker(testDataStore)
            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.failure(
                        workDataOf(AutoWallpaperWorker.FAILURE_REASON to FailureReason.DISABLED.name)
                    )
                )
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerNoSavedSearchId() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val appPreferencesRepository = testDataStore.appPreferencesRepository
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                )
            )
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
            )
            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.failure(
                        workDataOf(AutoWallpaperWorker.FAILURE_REASON to FailureReason.DISABLED.name)
                    )
                )
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerSavedSearchNull() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val appPreferencesRepository = testDataStore.appPreferencesRepository
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchId = 2,
                )
            )
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : TestSavedSearchDao() {
                    override suspend fun getById(id: Long) = null
                },
            )
            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.failure(
                        workDataOf(AutoWallpaperWorker.FAILURE_REASON to FailureReason.SAVED_SEARCH_NOT_SET.name)
                    )
                )
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerSetsFirstWallpaper() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        val tempFile = createTempFile(context, "tmp")
        try {
            val appPreferencesRepository = testDataStore.appPreferencesRepository
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchId = 1,
                    useObjectDetection = false,
                )
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = Search(
                    query = "test",
                    filters = SearchQuery(),
                )
            )
            val networkWallpapers = List(30) { testNetworkWallpaper }
            val wallpapers = networkWallpapers.map { it.asWallpaper() }
            val autoWallpaperHistoryDao = object : TestAutoWallpaperHistoryDao() {
                private var history = emptyList<AutoWallpaperHistoryEntity>()

                override suspend fun getAll() = history

                override suspend fun getByWallhavenId(wallhavenId: String) = history.find {
                    it.wallhavenId == wallhavenId
                }

                override suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity) {
                    history = history + autoWallpaperHistoryEntity
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : TestSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : TestWallHavenNetworkDataSource() {
                    override suspend fun search(
                        searchQuery: SearchQuery,
                        page: Int?,
                    ): NetworkResponse<List<NetworkWallpaper>> {

                        return NetworkResponse(
                            data = networkWallpapers,
                            meta = NetworkMeta(
                                current_page = 1,
                                last_page = 1,
                                per_page = networkWallpapers.size,
                                total = networkWallpapers.size,
                                query = StringNetworkMetaQuery(
                                    value = "",
                                ),
                                seed = null,
                            )
                        )
                    }
                },
            )

            every { worker["setWallpaper"](wallpapers.first()) } returns (true to tempFile)

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            AutoWallpaperWorker.SUCCESS_NEXT_WALLPAPER_ID to wallpapers.first().id,
                        )
                    )
                ),
            )
            verify { worker["setWallpaper"](wallpapers.first()) }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(1, updatedHistory.count())
        } finally {
            testDataStore.clear()
            tempFile.delete()
        }
    }

    @Test
    fun testAutoWallpaperWorkerShouldIgnoreHistory() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        val tempFile = createTempFile(context, "tmp")
        try {
            val appPreferencesRepository = testDataStore.appPreferencesRepository
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchId = 1,
                )
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = Search(
                    query = "test",
                    filters = SearchQuery(),
                )
            )
            val networkWallpapers = List(30) { testNetworkWallpaper }
            val wallpapers = networkWallpapers.map { it.asWallpaper() }
            val historyWalls = wallpapers.take(4)
            val setOn = Clock.System.now()
            val autoWallpaperHistoryDao = object : TestAutoWallpaperHistoryDao() {
                private var history = historyWalls.mapIndexed { i, wallpaper ->
                    AutoWallpaperHistoryEntity(
                        id = i.toLong(),
                        wallhavenId = wallpaper.id,
                        setOn = setOn + i.hours,
                    )
                }

                override suspend fun getAll() = history

                override suspend fun getByWallhavenId(wallhavenId: String) = history.find {
                    it.wallhavenId == wallhavenId
                }

                override suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity) {
                    history = history + autoWallpaperHistoryEntity
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : TestSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : TestWallHavenNetworkDataSource() {
                    override suspend fun search(
                        searchQuery: SearchQuery,
                        page: Int?,
                    ): NetworkResponse<List<NetworkWallpaper>> {

                        return NetworkResponse(
                            data = networkWallpapers,
                            meta = NetworkMeta(
                                current_page = 1,
                                last_page = 1,
                                per_page = networkWallpapers.size,
                                total = networkWallpapers.size,
                                query = StringNetworkMetaQuery(
                                    value = "",
                                ),
                                seed = null,
                            )
                        )
                    }
                },
            )

            every { worker["setWallpaper"](wallpapers[4]) } returns (true to tempFile)

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            AutoWallpaperWorker.SUCCESS_NEXT_WALLPAPER_ID to wallpapers[4].id,
                        )
                    )
                ),
            )
            verify { worker["setWallpaper"](wallpapers[4]) }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(historyWalls.count() + 1, updatedHistory.count())
        } finally {
            testDataStore.clear()
            tempFile.delete()
        }
    }

    @Test
    fun testAutoWallpaperWorkerShouldNotIgnoreHistory() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        val tempFile = createTempFile(context, "tmp")
        try {
            val appPreferencesRepository = testDataStore.appPreferencesRepository
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchId = 1,
                )
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = Search(
                    query = "test",
                    filters = SearchQuery(),
                )
            )
            val networkWallpapers = List(30) { testNetworkWallpaper }
            val wallpapers = networkWallpapers.map { it.asWallpaper() }
            val historyWalls = wallpapers.map { it }
            val setOn = Clock.System.now()
            val autoWallpaperHistoryDao = object : TestAutoWallpaperHistoryDao() {
                private var history = historyWalls.mapIndexed { i, wallpaper ->
                    AutoWallpaperHistoryEntity(
                        id = i.toLong(),
                        wallhavenId = wallpaper.id,
                        setOn = setOn + i.hours,
                    )
                }

                override suspend fun getAll() = history

                override suspend fun getByWallhavenId(wallhavenId: String) = history.find {
                    it.wallhavenId == wallhavenId
                }

                override suspend fun upsert(vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity) {
                    val existingIds = history.map { it.id }
                    history = history + autoWallpaperHistoryEntity.filter {
                        it.id !in existingIds
                    }
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : TestSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : TestWallHavenNetworkDataSource() {
                    override suspend fun search(
                        searchQuery: SearchQuery,
                        page: Int?,
                    ): NetworkResponse<List<NetworkWallpaper>> {

                        return NetworkResponse(
                            data = networkWallpapers,
                            meta = NetworkMeta(
                                current_page = 1,
                                last_page = 1,
                                per_page = networkWallpapers.size,
                                total = networkWallpapers.size,
                                query = StringNetworkMetaQuery(
                                    value = "",
                                ),
                                seed = null,
                            )
                        )
                    }
                },
            )

            every { worker["setWallpaper"](wallpapers[0]) } returns (true to tempFile)

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            AutoWallpaperWorker.SUCCESS_NEXT_WALLPAPER_ID to wallpapers[0].id,
                        )
                    )
                ),
            )
            verify { worker["setWallpaper"](wallpapers[0]) }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(historyWalls.count(), updatedHistory.count())
        } finally {
            testDataStore.clear()
            tempFile.delete()
        }
    }

    private fun getWorker(
        dataStore: DataStore<Preferences>,
        appPreferencesRepository: AppPreferencesRepository = dataStore.appPreferencesRepository,
        savedSearchDao: SavedSearchDao = TestSavedSearchDao(),
        autoWallpaperHistoryDao: AutoWallpaperHistoryDao = TestAutoWallpaperHistoryDao(),
        objectDetectionModelDao: ObjectDetectionModelDao = TestObjectDetectionModelDao(),
        wallHavenNetwork: WallHavenNetworkDataSource = TestWallHavenNetworkDataSource(),
    ): AutoWallpaperWorker {
        val workTaskExecutor = InstantWorkTaskExecutor()
        return AutoWallpaperWorker(
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
            okHttpClient = testOkHttpClient,
            appPreferencesRepository = appPreferencesRepository,
            savedSearchRepository = SavedSearchRepository(
                savedSearchDao = savedSearchDao,
                ioDispatcher = testDispatcher,
            ),
            autoWallpaperHistoryRepository = AutoWallpaperHistoryRepository(
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                ioDispatcher = testDispatcher,
            ),
            objectDetectionModelRepository = ObjectDetectionModelRepository(
                objectDetectionModelDao = objectDetectionModelDao,
                ioDispatcher = testDispatcher,
            ),
            wallHavenNetwork = wallHavenNetwork,
        ).run {
            spyk(this, recordPrivateCalls = true)
        }
    }

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = this,
        produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
    )

    private suspend fun DataStore<Preferences>.clear() = this.edit { it.clear() }

    private val DataStore<Preferences>.appPreferencesRepository
        get() = AppPreferencesRepository(
            dataStore = this,
            ioDispatcher = testDispatcher,
        )

    private fun createTempFile(
        context: Context,
        @Suppress("SameParameterValue") fileName: String,
    ) = context.getTempFile(fileName).also {
        it.parentFile?.mkdirs()
        it.createNewFile()
    }

    private val testNetworkWallpaper: NetworkWallpaper
        get() {
            val id = Random.nextInt().toString()
            return NetworkWallpaper(
                id = id,
                url = "https://example.com/wallpaper_${id}",
                short_url = "short test",
                uploader = null,
                views = Random.nextInt(),
                favorites = Random.nextInt(),
                source = "source",
                purity = Purity.SFW.purityName,
                category = "test",
                dimension_x = 10,
                dimension_y = 10,
                resolution = IntSize(10, 10).toString(),
                ratio = 1f,
                file_size = 100,
                file_type = "jpg",
                created_at = Clock.System.now(),
                colors = emptyList(),
                path = "wallpaper_path_${id}",
                thumbs = NetworkThumbs(
                    large = "test",
                    original = "test",
                    small = "test",
                ),
                tags = listOf(
                    NetworkTag(
                        id = Random.nextLong(),
                        name = "test",
                        alias = "",
                        category_id = 1,
                        category = "test",
                        purity = Purity.SFW.purityName,
                        created_at = Clock.System.now(),
                    )
                ),
            )
        }

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
