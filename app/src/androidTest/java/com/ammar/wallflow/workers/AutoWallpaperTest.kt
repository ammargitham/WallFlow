package com.ammar.wallflow.workers

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Parcel
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
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
import com.ammar.wallflow.MIME_TYPE_JPEG
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.wallpaper.toWallpaper
import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMeta
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenTag
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenThumbs
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaper
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.data.network.model.wallhaven.StringNetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.model.wallhaven.toWallhavenWallpaper
import com.ammar.wallflow.data.network.model.wallhaven.toWallpaperEntity
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.AutoWallpaperHistoryRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.accessibleFolders
import com.ammar.wallflow.extensions.getTempFile
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.toEntity
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.FAILURE_REASON
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.FailureReason
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.FailureReason.SAVED_SEARCH_NOT_SET
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SUCCESS_NEXT_HOME_WALLPAPER_ID
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SUCCESS_NEXT_LOCK_WALLPAPER_ID
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoWallpaperTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val allTargets = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN)

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
                        workDataOf(FAILURE_REASON to FailureReason.DISABLED.name),
                    ),
                ),
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerNoSavedSearchId() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                ),
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
                        workDataOf(FAILURE_REASON to FailureReason.DISABLED.name),
                    ),
                ),
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerSavedSearchNull() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = true,
                    savedSearchIds = setOf(2),
                ),
            )
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : FakeSavedSearchDao() {
                    override suspend fun getById(id: Long) = null
                },
            )
            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.failure(
                        workDataOf(FAILURE_REASON to SAVED_SEARCH_NOT_SET.name),
                    ),
                ),
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
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = true,
                    savedSearchIds = setOf(1),
                    useObjectDetection = false,
                ),
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = WallhavenSearch(
                    query = "test",
                    filters = WallhavenFilters(),
                ),
            )
            val networkWallpapers = List(30) { testNetworkWallhavenWallpaper }
            val wallpapers = networkWallpapers.map { it.toWallhavenWallpaper() }
            val autoWallpaperHistoryDao = object : FakeAutoWallpaperHistoryDao() {
                private var history = emptyList<AutoWallpaperHistoryEntity>()

                override suspend fun getAll() = history

                override suspend fun getAllBySource(source: Source) = history

                override suspend fun getBySourceId(
                    sourceId: String,
                    source: Source,
                ) = history.find { it.sourceId == sourceId }

                override suspend fun upsert(
                    vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity,
                ) {
                    history = history + autoWallpaperHistoryEntity
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : FakeSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : FakeWallhavenNetworkDataSource() {
                    override suspend fun search(
                        search: WallhavenSearch,
                        page: Int?,
                    ) = NetworkWallhavenWallpapersResponse(
                        data = networkWallpapers,
                        meta = NetworkWallhavenMeta(
                            current_page = 1,
                            last_page = 1,
                            per_page = networkWallpapers.size,
                            total = networkWallpapers.size,
                            query = StringNetworkWallhavenMetaQuery(
                                value = "",
                            ),
                            seed = null,
                        ),
                    )
                },
            )

            every {
                worker["setWallpaper"](
                    wallpapers.first(),
                    allTargets,
                )
            } returns (true to tempFile.toUri())

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            SUCCESS_NEXT_HOME_WALLPAPER_ID to wallpapers.first().id,
                            SUCCESS_NEXT_LOCK_WALLPAPER_ID to wallpapers.first().id,
                        ),
                    ),
                ),
            )
            verify {
                worker["setWallpaper"](
                    wallpapers.first(),
                    allTargets,
                )
            }
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
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = true,
                    savedSearchIds = setOf(1),
                ),
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = WallhavenSearch(
                    query = "test",
                    filters = WallhavenFilters(),
                ),
            )
            val networkWallpapers = List(30) { testNetworkWallhavenWallpaper }
            val wallpapers = networkWallpapers.map { it.toWallhavenWallpaper() }
            val historyWalls = wallpapers.take(4)
            val setOn = Clock.System.now()
            val autoWallpaperHistoryDao = object : FakeAutoWallpaperHistoryDao() {
                private var history = historyWalls.mapIndexed { i, wallpaper ->
                    AutoWallpaperHistoryEntity(
                        id = i.toLong(),
                        sourceId = wallpaper.id,
                        source = Source.WALLHAVEN,
                        sourceChoice = SourceChoice.SAVED_SEARCH,
                        setOn = setOn + i.hours,
                    )
                }

                override suspend fun getAll() = history

                override suspend fun getAllBySource(source: Source) = history

                override suspend fun getBySourceId(
                    sourceId: String,
                    source: Source,
                ) = history.find { it.sourceId == sourceId }

                override suspend fun upsert(
                    vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity,
                ) {
                    history = history + autoWallpaperHistoryEntity
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : FakeSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : FakeWallhavenNetworkDataSource() {
                    override suspend fun search(
                        search: WallhavenSearch,
                        page: Int?,
                    ) = NetworkWallhavenWallpapersResponse(
                        data = networkWallpapers,
                        meta = NetworkWallhavenMeta(
                            current_page = 1,
                            last_page = 1,
                            per_page = networkWallpapers.size,
                            total = networkWallpapers.size,
                            query = StringNetworkWallhavenMetaQuery(
                                value = "",
                            ),
                            seed = null,
                        ),
                    )
                },
            )

            every {
                worker["setWallpaper"](
                    wallpapers[4],
                    allTargets,
                )
            } returns (true to tempFile.toUri())

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            SUCCESS_NEXT_HOME_WALLPAPER_ID to wallpapers[4].id,
                            SUCCESS_NEXT_LOCK_WALLPAPER_ID to wallpapers[4].id,
                        ),
                    ),
                ),
            )
            verify {
                worker["setWallpaper"](
                    wallpapers[4],
                    allTargets,
                )
            }
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
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = true,
                    savedSearchIds = setOf(1),
                ),
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = WallhavenSearch(
                    query = "test",
                    filters = WallhavenFilters(),
                ),
            )
            val networkWallpapers = List(30) { testNetworkWallhavenWallpaper }
            val wallpapers = networkWallpapers.map { it.toWallhavenWallpaper() }
            val historyWalls = wallpapers.map { it }
            val setOn = Clock.System.now()
            val autoWallpaperHistoryDao = object : FakeAutoWallpaperHistoryDao() {
                private var history = historyWalls.mapIndexed { i, wallpaper ->
                    AutoWallpaperHistoryEntity(
                        id = i.toLong(),
                        sourceId = wallpaper.id,
                        source = Source.WALLHAVEN,
                        sourceChoice = SourceChoice.SAVED_SEARCH,
                        setOn = setOn + i.hours,
                    )
                }

                override suspend fun getAll() = history

                override suspend fun getAllBySource(source: Source) = history

                override suspend fun getBySourceId(
                    sourceId: String,
                    source: Source,
                ) = history.find { it.sourceId == sourceId }

                override suspend fun upsert(
                    vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity,
                ) {
                    val existingIds = history.map { it.id }
                    history = history + autoWallpaperHistoryEntity.filter {
                        it.id !in existingIds
                    }
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : FakeSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : FakeWallhavenNetworkDataSource() {
                    override suspend fun search(
                        search: WallhavenSearch,
                        page: Int?,
                    ) = NetworkWallhavenWallpapersResponse(
                        data = networkWallpapers,
                        meta = NetworkWallhavenMeta(
                            current_page = 1,
                            last_page = 1,
                            per_page = networkWallpapers.size,
                            total = networkWallpapers.size,
                            query = StringNetworkWallhavenMetaQuery(
                                value = "",
                            ),
                            seed = null,
                        ),
                    )
                },
            )

            every {
                worker["setWallpaper"](
                    wallpapers[0],
                    allTargets,
                )
            } returns (true to tempFile.toUri())

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            SUCCESS_NEXT_HOME_WALLPAPER_ID to wallpapers[0].id,
                            SUCCESS_NEXT_LOCK_WALLPAPER_ID to wallpapers[0].id,
                        ),
                    ),
                ),
            )
            verify {
                worker["setWallpaper"](
                    wallpapers[0],
                    allTargets,
                )
            }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(historyWalls.count(), updatedHistory.count())
        } catch (e: Exception) {
            Log.e(TAG, "testAutoWallpaperWorkerShouldNotIgnoreHistory: ", e)
        } finally {
            testDataStore.clear()
            tempFile.delete()
        }
    }

    @Test
    fun testAutoWallpaperWorkerNoFavoriteWallpaper() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val favoriteDao = object : FakeFavoriteDao() {
                override suspend fun getFirstFreshExcludingIds(
                    excludingIds: Collection<Long>,
                ) = null

                override suspend fun getByOldestSetOnAndIdsNotIn(
                    excludingIds: Collection<Long>,
                ) = null

                override suspend fun getCount() = 0
            }
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository(
                favoriteDao = favoriteDao,
            )
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = false,
                    favoritesEnabled = true,
                    savedSearchIds = setOf(1),
                    useObjectDetection = false,
                ),
            )
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
            )

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.failure(
                        workDataOf(
                            FAILURE_REASON to FailureReason.DISABLED.name,
                        ),
                    ),
                ),
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerSetsFavoriteWallpaper() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        val tempFile = createTempFile(context, "tmp")
        try {
            val favoriteDao = object : FakeFavoriteDao() {
                override suspend fun getFirstFreshExcludingIds(
                    excludingIds: Collection<Long>,
                ) = FavoriteEntity(
                    id = 1,
                    sourceId = "1",
                    source = Source.WALLHAVEN,
                    favoritedOn = Clock.System.now(),
                )

                override suspend fun getCount() = 1
            }
            val wallpaperEntity = testNetworkWallhavenWallpaper.toWallpaperEntity()
            val wallhavenWallpapersDao = object : FakeWallhavenWallpapersDao() {
                override suspend fun getByWallhavenId(wallhavenId: String) = wallpaperEntity
            }
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository(
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
            )
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = false,
                    favoritesEnabled = true,
                    savedSearchIds = setOf(1),
                    useObjectDetection = false,
                ),
            )
            val autoWallpaperHistoryDao = object : FakeAutoWallpaperHistoryDao() {
                private var history = emptyList<AutoWallpaperHistoryEntity>()

                override suspend fun getAll() = history

                override suspend fun getAllBySource(source: Source) = history

                override suspend fun getBySourceId(
                    sourceId: String,
                    source: Source,
                ) = history.find { it.sourceId == sourceId }

                override suspend fun upsert(
                    vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity,
                ) {
                    history = autoWallpaperHistoryEntity.toList()
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
            )

            every {
                worker["setWallpaper"](
                    wallpaperEntity.toWallpaper(),
                    allTargets,
                )
            } returns (true to tempFile.toUri())

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            SUCCESS_NEXT_HOME_WALLPAPER_ID to wallpaperEntity.wallhavenId,
                            SUCCESS_NEXT_LOCK_WALLPAPER_ID to wallpaperEntity.wallhavenId,
                        ),
                    ),
                ),
            )
            verify {
                worker["setWallpaper"](
                    wallpaperEntity.toWallpaper(),
                    allTargets,
                )
            }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(1, updatedHistory.count())
        } finally {
            testDataStore.clear()
            tempFile.delete()
        }
    }

    @Test
    fun testAutoWallpaperWorkerNoLocalWallpaper() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = false,
                    favoritesEnabled = false,
                    localEnabled = true,
                    savedSearchIds = setOf(1),
                    useObjectDetection = false,
                ),
            )
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = object : FakeFavoriteDao() {
                    override suspend fun getRandom() = null
                },
                localWallpapersRepository = object : FakeLocalWallpapersRepository() {
                    override suspend fun getRandom(
                        context: Context,
                        uris: Collection<Uri>,
                    ) = null
                },
            )

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.failure(
                        workDataOf(
                            FAILURE_REASON to FailureReason.DISABLED.name,
                        ),
                    ),
                ),
            )
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerSetsLocalWallpaper() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        try {
            val uri = Uri.parse("http://example.com")
            val localWallpaper = LocalWallpaper(
                id = uri.toString(),
                data = uri,
                fileSize = 1L,
                resolution = IntSize(1, 1),
                mimeType = MIME_TYPE_JPEG,
                name = "test",
            )
            val localWallpapersRepository = object : FakeLocalWallpapersRepository() {
                override suspend fun getFirstFresh(
                    context: Context,
                    uris: Collection<Uri>,
                    excluding: Collection<Wallpaper>,
                ) = localWallpaper
            }
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository(
                localWallpapersRepository = localWallpapersRepository,
            )
            mockkObject(appPreferencesRepository, recordPrivateCalls = true)
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = false,
                    favoritesEnabled = false,
                    localEnabled = true,
                    savedSearchIds = setOf(1),
                    localDirs = setOf(Uri.EMPTY),
                    useObjectDetection = false,
                ),
            )
            val autoWallpaperHistoryDao = object : FakeAutoWallpaperHistoryDao() {
                private var history = emptyList<AutoWallpaperHistoryEntity>()

                override suspend fun getAll() = history

                override suspend fun getAllBySource(source: Source) = history

                override suspend fun getBySourceId(
                    sourceId: String,
                    source: Source,
                ) = history.find { it.sourceId == sourceId }

                override suspend fun upsert(
                    vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity,
                ) {
                    history = autoWallpaperHistoryEntity.toList()
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                localWallpapersRepository = localWallpapersRepository,
            )
            mockkStatic("com.ammar.wallflow.extensions.Context_extKt")
            val parcel = Parcel.obtain()
            val uriPermission = UriPermission.CREATOR.createFromParcel(
                parcel.apply {
                    writeString(uri.toString())
                    writeInt(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    writeLong(Clock.System.now().epochSeconds)
                },
            )
            parcel.recycle()
            every {
                context.accessibleFolders
            } returns listOf(uriPermission)
            every {
                appPreferencesRepository["getUriIfAccessible"](
                    any<String>(),
                    any<Set<Uri>>(),
                )
            } returns uri
            every {
                worker["setWallpaper"](
                    localWallpaper,
                    allTargets,
                )
            } returns (true to uri)

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            SUCCESS_NEXT_HOME_WALLPAPER_ID to localWallpaper.id,
                            SUCCESS_NEXT_LOCK_WALLPAPER_ID to localWallpaper.id,
                        ),
                    ),
                ),
            )
            verify {
                worker["setWallpaper"](
                    localWallpaper,
                    allTargets,
                )
            }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(1, updatedHistory.count())
        } finally {
            testDataStore.clear()
        }
    }

    @Test
    fun testAutoWallpaperWorkerSetsDifferentWallpapers() = runTest(testDispatcher) {
        val testDataStore = dataStore()
        val tempFile = createTempFile(context, "tmp")
        try {
            val appPreferencesRepository = testDataStore.getAppPreferencesRepository()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = true,
                    lsSavedSearchEnabled = true,
                    savedSearchIds = setOf(1),
                    lsSavedSearchIds = setOf(1),
                    useObjectDetection = false,
                    setDifferentWallpapers = true,
                ),
            )
            val savedSearch = SavedSearch(
                id = 1,
                name = "Test",
                search = WallhavenSearch(
                    query = "test",
                    filters = WallhavenFilters(),
                ),
            )
            val networkWallpapers = List(30) { testNetworkWallhavenWallpaper }
            val wallpapers = networkWallpapers.map { it.toWallhavenWallpaper() }
            val autoWallpaperHistoryDao = object : FakeAutoWallpaperHistoryDao() {
                private var history = emptyList<AutoWallpaperHistoryEntity>()

                override suspend fun getAll() = history

                override suspend fun getAllBySource(source: Source) = history

                override suspend fun getBySourceId(
                    sourceId: String,
                    source: Source,
                ) = history.find { it.sourceId == sourceId }

                override suspend fun upsert(
                    vararg autoWallpaperHistoryEntity: AutoWallpaperHistoryEntity,
                ) {
                    history = history + autoWallpaperHistoryEntity
                }
            }
            val worker = getWorker(
                dataStore = testDataStore,
                appPreferencesRepository = appPreferencesRepository,
                savedSearchDao = object : FakeSavedSearchDao() {
                    override suspend fun getById(id: Long) = savedSearch.toEntity(1)
                },
                autoWallpaperHistoryDao = autoWallpaperHistoryDao,
                wallHavenNetwork = object : FakeWallhavenNetworkDataSource() {
                    override suspend fun search(
                        search: WallhavenSearch,
                        page: Int?,
                    ) = NetworkWallhavenWallpapersResponse(
                        data = networkWallpapers,
                        meta = NetworkWallhavenMeta(
                            current_page = 1,
                            last_page = 1,
                            per_page = networkWallpapers.size,
                            total = networkWallpapers.size,
                            query = StringNetworkWallhavenMetaQuery(
                                value = "",
                            ),
                            seed = null,
                        ),
                    )
                },
            )

            every {
                worker["setWallpaper"](
                    wallpapers[0],
                    setOf(WallpaperTarget.HOME),
                )
            } returns (true to tempFile.toUri())
            every {
                worker["setWallpaper"](
                    wallpapers[1],
                    setOf(WallpaperTarget.LOCKSCREEN),
                )
            } returns (true to tempFile.toUri())

            val result = worker.doWork()
            assertThat(
                result,
                `is`(
                    Result.success(
                        workDataOf(
                            SUCCESS_NEXT_HOME_WALLPAPER_ID to wallpapers[0].id,
                            SUCCESS_NEXT_LOCK_WALLPAPER_ID to wallpapers[1].id,
                        ),
                    ),
                ),
            )
            verify {
                worker["setWallpaper"](
                    wallpapers[0],
                    setOf(WallpaperTarget.HOME),
                )
            }
            verify {
                worker["setWallpaper"](
                    wallpapers[1],
                    setOf(WallpaperTarget.LOCKSCREEN),
                )
            }
            val updatedHistory = autoWallpaperHistoryDao.getAll()
            assertEquals(2, updatedHistory.count())
        } finally {
            testDataStore.clear()
            tempFile.delete()
        }
    }

    private fun getWorker(
        dataStore: DataStore<Preferences>,
        savedSearchDao: SavedSearchDao = FakeSavedSearchDao(),
        autoWallpaperHistoryDao: AutoWallpaperHistoryDao = FakeAutoWallpaperHistoryDao(),
        objectDetectionModelDao: ObjectDetectionModelDao = FakeObjectDetectionModelDao(),
        wallHavenNetwork: WallhavenNetworkDataSource = FakeWallhavenNetworkDataSource(),
        redditNetwork: RedditNetworkDataSource = FakeRedditNetworkDataSource(),
        favoriteDao: FavoriteDao = object : FakeFavoriteDao() {
            override suspend fun getCount() = 0
        },
        wallhavenWallpapersDao: WallhavenWallpapersDao = FakeWallhavenWallpapersDao(),
        redditWallpapersDao: RedditWallpapersDao = FakeRedditWallpapersDao(),
        localWallpapersRepository: LocalWallpapersRepository = FakeLocalWallpapersRepository(),
        lightDarkDao: LightDarkDao = FakeLightDarkDao(),
        appPreferencesRepository: AppPreferencesRepository =
            dataStore.getAppPreferencesRepository(
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                localWallpapersRepository = localWallpapersRepository,
            ),
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
            okHttpClient = fakeOkHttpClient,
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
            redditNetwork = redditNetwork,
            favoritesRepository = FavoritesRepository(
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                localWallpapersRepository = localWallpapersRepository,
                ioDispatcher = testDispatcher,
            ),
            localWallpapersRepository = localWallpapersRepository,
            lightDarkRepository = LightDarkRepository(
                lightDarkDao = lightDarkDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                localWallpapersRepository = localWallpapersRepository,
                ioDispatcher = testDispatcher,
            ),
        ).run {
            spyk(this, recordPrivateCalls = true)
        }
    }

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = this.backgroundScope,
        produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
    )

    private suspend fun DataStore<Preferences>.clear() = this.edit { it.clear() }

    private fun DataStore<Preferences>.getAppPreferencesRepository(
        favoriteDao: FavoriteDao = object : FakeFavoriteDao() {
            override suspend fun getCount() = 0
        },
        wallhavenWallpapersDao: WallhavenWallpapersDao = FakeWallhavenWallpapersDao(),
        redditWallpapersDao: RedditWallpapersDao = FakeRedditWallpapersDao(),
        localWallpapersRepository: LocalWallpapersRepository = FakeLocalWallpapersRepository(),
    ) = AppPreferencesRepository(
        context = context,
        dataStore = this,
        favoritesRepository = FavoritesRepository(
            favoriteDao = favoriteDao,
            wallhavenWallpapersDao = wallhavenWallpapersDao,
            redditWallpapersDao = redditWallpapersDao,
            localWallpapersRepository = localWallpapersRepository,
            ioDispatcher = testDispatcher,
        ),
        ioDispatcher = testDispatcher,
    )

    private fun createTempFile(
        context: Context,
        @Suppress("SameParameterValue") fileName: String,
    ) = context.getTempFile(fileName).also {
        it.parentFile?.mkdirs()
        it.createNewFile()
    }

    private val testNetworkWallhavenWallpaper: NetworkWallhavenWallpaper
        get() {
            val id = Random.nextInt().toString()
            return NetworkWallhavenWallpaper(
                id = id,
                url = "https://example.com/wallpaper_$id",
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
                path = "wallpaper_path_$id",
                thumbs = NetworkWallhavenThumbs(
                    large = "test",
                    original = "test",
                    small = "test",
                ),
                tags = listOf(
                    NetworkWallhavenTag(
                        id = Random.nextLong(),
                        name = "test",
                        alias = "",
                        category_id = 1,
                        category = "test",
                        purity = Purity.SFW.purityName,
                        created_at = Clock.System.now(),
                    ),
                ),
            )
        }

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
