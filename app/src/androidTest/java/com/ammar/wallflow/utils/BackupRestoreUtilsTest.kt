package com.ammar.wallflow.utils

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.MockFactory
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.dao.ViewedDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.data.repository.reddit.DefaultRedditRepository
import com.ammar.wallflow.data.repository.wallhaven.DefaultWallhavenRepository
import com.ammar.wallflow.insertRedditEntities as actualInsertRedditEntities
import com.ammar.wallflow.insertWallhavenEntities as actualInsertWallhavenEntities
import com.ammar.wallflow.json
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.backup.Backup
import com.ammar.wallflow.model.backup.BackupOptions
import com.ammar.wallflow.model.backup.BackupV1
import com.ammar.wallflow.model.search.Filters
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenTagSearchMeta
import com.ammar.wallflow.model.search.toEntity
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.workers.FakeLocalWallpapersRepository
import com.ammar.wallflow.workers.FakeRedditNetworkDataSource
import com.ammar.wallflow.workers.FakeWallhavenNetworkDataSource
import com.ammar.wallflow.workers.TestClock
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.buffer
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreUtilsTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDb: AppDatabase
    private lateinit var wallhavenWallpapersDao: WallhavenWallpapersDao
    private lateinit var redditWallpapersDao: RedditWallpapersDao
    private lateinit var tagsDao: WallhavenTagsDao
    private lateinit var uploadersDao: WallhavenUploadersDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var savedSearchDao: SavedSearchDao
    private lateinit var viewedDao: ViewedDao
    private lateinit var lightDarkDao: LightDarkDao
    private val random = Random(1000)
    private val clock = TestClock(now = Instant.fromEpochMilliseconds(1694954538))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDb = Room.inMemoryDatabaseBuilder(
            context = ApplicationProvider.getApplicationContext(),
            klass = AppDatabase::class.java,
        ).build()
        wallhavenWallpapersDao = fakeDb.wallhavenWallpapersDao()
        redditWallpapersDao = fakeDb.redditWallpapersDao()
        tagsDao = fakeDb.wallhavenTagsDao()
        uploadersDao = fakeDb.wallhavenUploadersDao()
        favoriteDao = fakeDb.favoriteDao()
        savedSearchDao = fakeDb.savedSearchDao()
        viewedDao = fakeDb.viewedDao()
        lightDarkDao = fakeDb.lightDarkDao()
    }

    @After
    fun tearDown() {
        fakeDb.clearAllTables()
        fakeDb.close()
    }

    private suspend fun initDb(
        allWallhavenUploadersNull: Boolean = false,
    ) {
        val wallhavenEntities = insertWallhavenEntities(
            allWallhavenUploadersNull = allWallhavenUploadersNull,
        )
        val redditEntities = insertRedditEntities()

        // favorites 4 wallhaven and reddit wallpapers
        val favoriteWallhavenEntities = wallhavenEntities.shuffled().take(4)
        val favoriteRedditEntities = redditEntities.shuffled().take(4)
        val favoriteEntities = favoriteWallhavenEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                favoritedOn = clock.now(),
            )
        } + favoriteRedditEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                favoritedOn = clock.now(),
            )
        }
        favoriteDao.insertAll(favoriteEntities)

        // mark light/dark 4 wallhaven and reddit wallpapers
        val possibleLightDarkTypes = setOf(
            LightDarkType.LIGHT,
            LightDarkType.DARK,
            LightDarkType.EXTRA_DIM,
        )
        val lightDarkWallhavenEntities = wallhavenEntities.shuffled().take(4)
        val lightDarkRedditEntities = redditEntities.shuffled().take(4)
        val lightDarkEntities = lightDarkWallhavenEntities.map {
            LightDarkEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                typeFlags = possibleLightDarkTypes.random(),
                updatedOn = clock.now(),
            )
        } + lightDarkRedditEntities.map {
            LightDarkEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                typeFlags = possibleLightDarkTypes.random(),
                updatedOn = clock.now(),
            )
        }
        lightDarkDao.insertAll(lightDarkEntities)

        // view 2 wallhaven and 2 reddit wallpapers
        val viewedWallhavenEntities = wallhavenEntities.shuffled().take(2)
        val viewedRedditEntities = redditEntities.shuffled().take(2)
        val viewedEntities = viewedWallhavenEntities.map {
            ViewedEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                lastViewedOn = clock.now(),
            )
        } + viewedRedditEntities.map {
            ViewedEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                lastViewedOn = clock.now(),
            )
        }
        viewedDao.insertAll(viewedEntities)

        val savedSearches = MockFactory.generateWallhavenSavedSearches(random = random) +
            MockFactory.generateRedditSavedSearches(random = random)
        savedSearchDao.upsert(savedSearches.map { it.toEntity() })
    }

    private suspend fun insertWallhavenEntities(
        allWallhavenUploadersNull: Boolean = false,
        count: Int = 10,
    ) = actualInsertWallhavenEntities(
        random = random,
        clock = clock,
        tagsDao = tagsDao,
        uploadersDao = uploadersDao,
        wallhavenWallpapersDao = wallhavenWallpapersDao,
        allWallhavenUploadersNull = allWallhavenUploadersNull,
        count = count,
    )

    private suspend fun insertRedditEntities(
        count: Int = 10,
    ) = actualInsertRedditEntities(
        random = random,
        redditWallpapersDao = redditWallpapersDao,
        count = count,
    )

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = this.backgroundScope,
        produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
    )

    private suspend fun DataStore<Preferences>.clear() = this.edit { it.clear() }

    private val DataStore<Preferences>.appPreferencesRepository
        get() = AppPreferencesRepository(
            context = context,
            dataStore = this,
            favoritesRepository = FavoritesRepository(
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                localWallpapersRepository = FakeLocalWallpapersRepository(),
                ioDispatcher = testDispatcher,
            ),
            ioDispatcher = testDispatcher,
        )

    @Test
    fun shouldCreateCorrectBackupJson() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb()
            val jsonStr = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(jsonStr)
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbLightDark = lightDarkDao.getAll()
            val dbViewed = viewedDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            val dbSavedSearchesMap = dbSavedSearches.groupBy {
                when (json.decodeFromString<Filters>(it.filters)) {
                    is WallhavenFilters -> OnlineSource.WALLHAVEN
                    is RedditFilters -> OnlineSource.REDDIT
                }
            }
            val decoded = json.decodeFromString<BackupV1>(jsonStr)
            assertEquals(
                appPreferencesRepository.appPreferencesFlow.firstOrNull(),
                decoded.preferences,
            )
            assertEquals(
                dbFavorites.sortedBy { it.id },
                decoded.favorites?.sortedBy { it.id },
            )
            assertEquals(
                dbLightDark.sortedBy { it.id },
                decoded.lightDark?.sortedBy { it.id },
            )
            assertEquals(
                dbViewed.sortedBy { it.id },
                decoded.viewed?.sortedBy { it.id },
            )

            val decodedWallhavenEntities = decoded.wallhaven
                ?.wallpapers
                ?.sortedBy { it.id }
                ?: emptyList()
            val decodedRedditEntities = decoded.reddit
                ?.wallpapers
                ?.sortedBy { it.id }
                ?: emptyList()

            val favoritedWallhavenIds = dbFavorites
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            assertTrue(
                decodedWallhavenEntities.containsAll(
                    dbWallhavenWallpapers
                        .filter { it.wallhavenId in favoritedWallhavenIds }
                        .sortedBy { it.id },
                ),
            )

            val favoritedRedditIds = dbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            assertTrue(
                decodedRedditEntities.containsAll(
                    dbRedditWallpapers
                        .filter { it.redditId in favoritedRedditIds }
                        .sortedBy { it.id },
                ),
            )

            val lightDarkWallhavenIds = dbLightDark
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            assertTrue(
                decodedWallhavenEntities.containsAll(
                    dbWallhavenWallpapers
                        .filter { it.wallhavenId in lightDarkWallhavenIds }
                        .sortedBy { it.id },
                ),
            )

            val lightDarkRedditIds = dbLightDark
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            assertTrue(
                decodedRedditEntities.containsAll(
                    dbRedditWallpapers
                        .filter { it.redditId in lightDarkRedditIds }
                        .sortedBy { it.id },
                ),
            )

            assertEquals(
                dbSavedSearchesMap[OnlineSource.WALLHAVEN]?.sortedBy { it.id },
                decoded.wallhaven?.savedSearches?.sortedBy { it.id },
            )

            assertEquals(
                dbSavedSearchesMap[OnlineSource.REDDIT]?.sortedBy { it.id },
                decoded.reddit?.savedSearches?.sortedBy { it.id },
            )
        } finally {
            dataStore.clear()
        }
    }

    private suspend fun getBackupV1Json(
        appPreferencesRepository: AppPreferencesRepository,
        options: BackupOptions,
    ) = getBackupV1Json(
        options = options,
        appPreferencesRepository = appPreferencesRepository,
        favoriteDao = favoriteDao,
        wallhavenWallpapersDao = wallhavenWallpapersDao,
        redditWallpapersDao = redditWallpapersDao,
        savedSearchDao = savedSearchDao,
        viewedDao = viewedDao,
        lightDarkDao = lightDarkDao,
    )

    @Test
    fun shouldRestoreBackup() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb()
            val oldPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val oldDbViewed = viewedDao.getAll()

            val oldDbFavorites = favoriteDao.getAll()
            val oldFavWallhavenIds = oldDbFavorites
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            val oldDbFavWallpapersWithUploaderAndTags =
                wallhavenWallpapersDao.getAllWithUploaderAndTags()
                    .filter { it.wallpaper.wallhavenId in oldFavWallhavenIds }
            val oldDbFavWallhavenWallpapers = oldDbFavWallpapersWithUploaderAndTags.map {
                it.wallpaper
            }
            val oldFavRedditIds = oldDbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val oldDbFavRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldFavRedditIds
            }

            val oldDbLightDark = lightDarkDao.getAll()
            val oldLightDarkWallhavenIds = oldDbLightDark
                .filter { it.source == Source.WALLHAVEN }
                .filter { it.sourceId !in oldFavWallhavenIds }
                .map { it.sourceId }
            val oldDbLightDarkWallpapersWithUploaderAndTags =
                wallhavenWallpapersDao.getAllWithUploaderAndTags()
                    .filter { it.wallpaper.wallhavenId in oldLightDarkWallhavenIds }
            val oldDbLightDarkWallhavenWallpapers = oldDbLightDarkWallpapersWithUploaderAndTags
                .map {
                    it.wallpaper
                }
            val oldLightDarkRedditIds = oldDbLightDark
                .filter { it.source == Source.REDDIT }
                .filter { it.sourceId !in oldFavRedditIds }
                .map { it.sourceId }
            val oldDbLightDarkRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldLightDarkRedditIds
            }

            val oldDbWallhavenWallpapers = oldDbFavWallhavenWallpapers +
                oldDbLightDarkWallhavenWallpapers
            val oldDbRedditWallpapers = oldDbFavRedditWallpapers +
                oldDbLightDarkRedditWallpapers

            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)
            // clear all tables and preferences
            fakeDb.clearAllTables()
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                appPreferencesRepository = appPreferencesRepository,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbLightDark = lightDarkDao.getAll()
            val dbViewed = viewedDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            assertEquals(oldPreferences, preferences)
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallhavenWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallhavenWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbLightDark
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbLightDark
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbViewed
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbViewed
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbRedditWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.redditId },
                dbRedditWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.redditId },
            )
            assertEquals(
                oldDbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
                dbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
            )
        } finally {
            dataStore.clear()
        }
    }

    private suspend fun restoreBackup(
        appPreferencesRepository: AppPreferencesRepository,
        backup: Backup,
        options: BackupOptions,
    ) = restoreBackup(
        context = context,
        backup = backup,
        options = options,
        appPreferencesRepository = appPreferencesRepository,
        savedSearchRepository = SavedSearchRepository(
            savedSearchDao = savedSearchDao,
            ioDispatcher = testDispatcher,
        ),
        wallhavenRepository = DefaultWallhavenRepository(
            appDatabase = fakeDb,
            wallHavenNetwork = FakeWallhavenNetworkDataSource(),
            ioDispatcher = testDispatcher,
        ),
        redditRepository = DefaultRedditRepository(
            appDatabase = fakeDb,
            dataSource = FakeRedditNetworkDataSource(),
            ioDispatcher = testDispatcher,
        ),
        favoritesRepository = FavoritesRepository(
            favoriteDao = favoriteDao,
            wallhavenWallpapersDao = wallhavenWallpapersDao,
            redditWallpapersDao = redditWallpapersDao,
            localWallpapersRepository = FakeLocalWallpapersRepository(),
            ioDispatcher = testDispatcher,
        ),
        wallhavenWallpapersDao = wallhavenWallpapersDao,
        redditWallpapersDao = redditWallpapersDao,
        viewedRepository = ViewedRepository(
            viewedDao = viewedDao,
            ioDispatcher = testDispatcher,
        ),
        lightDarkRepository = LightDarkRepository(
            lightDarkDao = lightDarkDao,
            wallhavenWallpapersDao = wallhavenWallpapersDao,
            redditWallpapersDao = redditWallpapersDao,
            localWallpapersRepository = FakeLocalWallpapersRepository(),
            ioDispatcher = testDispatcher,
        ),
    )

    @Test
    fun shouldRestoreBackupWithChangedPrefs() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            // change prefs
            appPreferencesRepository.updateBlurNsfw(true)
            appPreferencesRepository.updateBlurSketchy(true)
            appPreferencesRepository.updateWallhavenApiKey("test")
            appPreferencesRepository.updateHomeWallhavenSearch(
                WallhavenSearch(
                    query = "test",
                    filters = WallhavenFilters(),
                    meta = WallhavenTagSearchMeta(
                        tag = WallhavenTag(
                            id = 1,
                            name = "test_tag",
                            alias = emptyList(),
                            categoryId = 1,
                            category = "people",
                            purity = Purity.SFW,
                            createdAt = Clock.System.now(),
                        ),
                    ),
                ),
            )
            appPreferencesRepository.updateHomeRedditSearch(
                RedditSearch(
                    query = "test",
                    filters = RedditFilters(
                        subreddits = setOf("test", "test2"),
                        includeNsfw = false,
                        sort = RedditSort.RELEVANCE,
                        timeRange = RedditTimeRange.ALL,
                    ),
                ),
            )
            appPreferencesRepository.updateHomeSources(
                mapOf(
                    OnlineSource.WALLHAVEN to true,
                    OnlineSource.REDDIT to false,
                ),
            )
            appPreferencesRepository.updateWriteTagsToExif(true)
            appPreferencesRepository.updateTagsWriteType(ExifWriteType.OVERWRITE)
            val oldPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            assertEquals(oldPreferences, preferences)
            assertTrue(preferences?.homeSources?.size == 2)
        } finally {
            dataStore.clear()
        }
    }

    @Test
    fun shouldRestoreBackupWithAutoWallpaperPrefs() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb()
            // change prefs
            val autoWallSavedSearch = savedSearchDao.getAll().first()
            appPreferencesRepository.updateAutoWallpaperPrefs(
                AutoWallpaperPreferences(
                    enabled = true,
                    savedSearchEnabled = true,
                    savedSearchIds = setOf(autoWallSavedSearch.id),
                ),
            )
            val oldPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                appPreferencesRepository = appPreferencesRepository,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            assertNotNull(preferences)
            val savedSearchId = preferences.autoWallpaperPreferences.savedSearchIds.first()
            val restoredAutoWallSavedSearch = savedSearchDao.getById(savedSearchId)
            assertEquals(
                autoWallSavedSearch.copy(id = 0),
                restoredAutoWallSavedSearch?.copy(id = 0),
            )
            assertEquals(oldPreferences, preferences)
        } finally {
            dataStore.clear()
        }
    }

    @Test
    fun shouldRestoreBackupWithChangedPrefsWithoutClear() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            // change prefs
            appPreferencesRepository.updateBlurNsfw(true)
            appPreferencesRepository.updateBlurSketchy(true)
            appPreferencesRepository.updateWallhavenApiKey("test")
            val oldPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)

            // change prefs again without backup
            appPreferencesRepository.updateBlurSketchy(false)
            appPreferencesRepository.updateBlurSketchy(false)

            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                appPreferencesRepository = appPreferencesRepository,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            assertEquals(oldPreferences, preferences)
        } finally {
            dataStore.clear()
        }
    }

    @Test
    fun shouldRestoreBackupEntitiesWithoutClear() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb()
            val oldDbFavorites = favoriteDao.getAll()
            val oldFavWallhavenIds = oldDbFavorites.map { it.sourceId }
            val oldDbFavWallpapersWithUploaderAndTags = wallhavenWallpapersDao
                .getAllWithUploaderAndTags()
                .filter {
                    it.wallpaper.wallhavenId in oldFavWallhavenIds
                }
            val oldDbFavWallpapers = oldDbFavWallpapersWithUploaderAndTags.map { it.wallpaper }
            val oldDbFavUploaders = oldDbFavWallpapersWithUploaderAndTags.mapNotNull { it.uploader }
            val oldDbFavTags = oldDbFavWallpapersWithUploaderAndTags.flatMap {
                it.tags ?: emptyList()
            }

            val oldDbLightDark = lightDarkDao.getAll()
            val oldLightDarkWallhavenIds = oldDbLightDark.map { it.sourceId }
            val oldDbLightDarkWallpapersWithUploaderAndTags = wallhavenWallpapersDao
                .getAllWithUploaderAndTags()
                .filter {
                    it.wallpaper.wallhavenId in oldLightDarkWallhavenIds
                }
            val oldDbLightDarkWallpapers = oldDbLightDarkWallpapersWithUploaderAndTags.map {
                it.wallpaper
            }
            val oldDbLightDarkUploaders = oldDbLightDarkWallpapersWithUploaderAndTags.mapNotNull {
                it.uploader
            }
            val oldDbLightDarkTags = oldDbLightDarkWallpapersWithUploaderAndTags.flatMap {
                it.tags ?: emptyList()
            }

            val oldDbWallpapers = oldDbFavWallpapers + oldDbLightDarkWallpapers
            val oldDbUploaders = oldDbFavUploaders + oldDbLightDarkUploaders
            val oldDbTags = oldDbFavTags + oldDbLightDarkTags

            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)

            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                appPreferencesRepository = appPreferencesRepository,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            val dbFavorites = favoriteDao.getAll()
            val dbFavWallhavenIds = dbFavorites.map { it.sourceId }
            val dbFavWallsWithTagsAndUploaders = wallhavenWallpapersDao
                .getAllWithUploaderAndTagsByWallhavenIds(
                    dbFavWallhavenIds,
                )
            val dbFavWallpapers = dbFavWallsWithTagsAndUploaders.map { it.wallpaper }
            val dbFavUploaders = dbFavWallsWithTagsAndUploaders.mapNotNull { it.uploader }
            val dbFavTags = dbFavWallsWithTagsAndUploaders.flatMap { it.tags ?: emptyList() }

            val dbLightDark = lightDarkDao.getAll()
            val dbLightDarkWallhavenIds = dbLightDark.map { it.sourceId }
            val dbLightDarkWallsWithTagsAndUploaders = wallhavenWallpapersDao
                .getAllWithUploaderAndTagsByWallhavenIds(
                    dbLightDarkWallhavenIds,
                )
            val dbLightDarkWallpapers = dbLightDarkWallsWithTagsAndUploaders.map { it.wallpaper }
            val dbLightDarkUploaders = dbLightDarkWallsWithTagsAndUploaders.mapNotNull {
                it.uploader
            }
            val dbLightDarkTags = dbLightDarkWallsWithTagsAndUploaders.flatMap {
                it.tags ?: emptyList()
            }

            val dbWallpapers = dbFavWallpapers + dbLightDarkWallpapers
            val dbUploaders = dbFavUploaders + dbLightDarkUploaders
            val dbTags = dbFavTags + dbLightDarkTags

            val dbSavedSearches = savedSearchDao.getAll()
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbTags
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbTags
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbUploaders
                    .map { it.copy(id = 0) }
                    .sortedBy { it.username },
                dbUploaders
                    .map { it.copy(id = 0) }
                    .sortedBy { it.username },
            )
            assertEquals(
                oldDbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
                dbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
            )
        } finally {
            dataStore.clear()
        }
    }

    @Test
    fun shouldRestoreBackupWhenFavUploadersEmpty() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb(allWallhavenUploadersNull = true)
            val oldPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val oldDbFavorites = favoriteDao.getAll()
            val oldFavWallhavenIds = oldDbFavorites
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            val oldDbFavWallpapersWithUploaderAndTags =
                wallhavenWallpapersDao.getAllWithUploaderAndTags()
                    .filter { it.wallpaper.wallhavenId in oldFavWallhavenIds }
            val oldDbWallhavenWallpapers = oldDbFavWallpapersWithUploaderAndTags.map {
                it.wallpaper
            }
            val oldFavRedditIds = oldDbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val oldDbRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldFavRedditIds
            }
            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)
            // clear all tables and preferences
            fakeDb.clearAllTables()
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                appPreferencesRepository = appPreferencesRepository,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            assertEquals(oldPreferences, preferences)
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallhavenWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallhavenWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbRedditWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.redditId },
                dbRedditWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.redditId },
            )
            assertEquals(
                oldDbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
                dbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
            )
        } finally {
            dataStore.clear()
        }
    }

    @Test
    fun shouldMigrateAppPrefsV1ToV2() = runTest(testDispatcher) {
        // read v1 json file
        val inputStream = this.javaClass.classLoader?.getResourceAsStream(
            "wallflow_backup_v1.json",
        ) ?: throw RuntimeException("Missing json file!")
        val jsonString = inputStream.source().buffer().use { source ->
            source.readUtf8()
        }
        // read json
        val backup = readBackupJson(jsonString)
        assertNotNull(backup)
        assertTrue(backup is BackupV1)
        assertNotNull(backup.preferences)
        assertNotNull(backup.favorites)
        assertNotNull(backup.wallhaven)
        assertEquals(2, backup.preferences?.version)
        assertEquals("nature", backup.preferences?.homeWallhavenSearch?.query)
        assertNotNull(backup.preferences?.autoWallpaperPreferences)
        assertNotNull(backup.preferences?.autoWallpaperPreferences?.savedSearchIds)
        assertEquals(backup.preferences?.autoWallpaperPreferences?.savedSearchIds?.size, 1)
        assertNull(backup.viewed)
    }

    private suspend fun initDbWith1000Records() {
        val count = 1000
        val wallhavenEntities = insertWallhavenEntities(count = count)
        val redditEntities = insertRedditEntities(count = count)

        val favoriteEntities = wallhavenEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                favoritedOn = clock.now(),
            )
        } + redditEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                favoritedOn = clock.now(),
            )
        }
        favoriteDao.insertAll(favoriteEntities)

        val possibleLightDarkTypes = setOf(
            LightDarkType.LIGHT,
            LightDarkType.DARK,
            LightDarkType.EXTRA_DIM,
        )
        val lightDarkEntities = wallhavenEntities.map {
            LightDarkEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                typeFlags = possibleLightDarkTypes.random(),
                updatedOn = clock.now(),
            )
        } + redditEntities.map {
            LightDarkEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                typeFlags = possibleLightDarkTypes.random(),
                updatedOn = clock.now(),
            )
        }
        lightDarkDao.insertAll(lightDarkEntities)

        val viewedEntities = wallhavenEntities.map {
            ViewedEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                lastViewedOn = clock.now(),
            )
        } + redditEntities.map {
            ViewedEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                lastViewedOn = clock.now(),
            )
        }
        viewedDao.insertAll(viewedEntities)

        val savedSearches = MockFactory.generateWallhavenSavedSearches(
            random = random,
            size = 1000,
        ) + MockFactory.generateRedditSavedSearches(random = random)
        savedSearchDao.upsert(savedSearches.map { it.toEntity() })
    }

    @Test
    fun shouldCreateBackupJsonWith1000Records() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDbWith1000Records()
            val jsonStr = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = false,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(jsonStr)
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbLightDark = lightDarkDao.getAll()
            val dbViewed = viewedDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            val dbSavedSearchesMap = dbSavedSearches.groupBy {
                when (json.decodeFromString<Filters>(it.filters)) {
                    is WallhavenFilters -> OnlineSource.WALLHAVEN
                    is RedditFilters -> OnlineSource.REDDIT
                }
            }
            val decoded = json.decodeFromString<BackupV1>(jsonStr)

            assertEquals(
                dbFavorites.sortedBy { it.id },
                decoded.favorites?.sortedBy { it.id },
            )
            assertEquals(
                dbLightDark.sortedBy { it.id },
                decoded.lightDark?.sortedBy { it.id },
            )
            assertEquals(
                dbViewed.sortedBy { it.id },
                decoded.viewed?.sortedBy { it.id },
            )

            val decodedWallhavenEntities = decoded.wallhaven
                ?.wallpapers
                ?.sortedBy { it.id }
                ?: emptyList()
            val decodedRedditEntities = decoded.reddit
                ?.wallpapers
                ?.sortedBy { it.id }
                ?: emptyList()

            assertTrue(decodedWallhavenEntities.isNotEmpty())
            assertTrue(decodedRedditEntities.isNotEmpty())

            val favoritedWallhavenIds = dbFavorites
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            val favoriteWallhavenEntities = dbWallhavenWallpapers.filter {
                it.wallhavenId in favoritedWallhavenIds
            }
            assertTrue(favoriteWallhavenEntities.isNotEmpty())
            assertTrue(
                decodedWallhavenEntities.containsAll(
                    favoriteWallhavenEntities
                        .sortedBy { it.id },
                ),
            )

            val favoritedRedditIds = dbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val favoriteRedditEntities = dbRedditWallpapers.filter {
                it.redditId in favoritedRedditIds
            }
            assertTrue(favoriteRedditEntities.isNotEmpty())
            assertTrue(
                decodedRedditEntities.containsAll(
                    favoriteRedditEntities.sortedBy { it.id },
                ),
            )

            val lightDarkWallhavenIds = dbLightDark
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            val lightDarkWallhavenEntities = dbWallhavenWallpapers.filter {
                it.wallhavenId in lightDarkWallhavenIds
            }
            assertTrue(lightDarkWallhavenEntities.isNotEmpty())
            assertTrue(
                decodedWallhavenEntities.containsAll(
                    lightDarkWallhavenEntities
                        .sortedBy { it.id },
                ),
            )

            val lightDarkRedditIds = dbLightDark
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val lightDarkRedditEntities = dbRedditWallpapers.filter {
                it.redditId in lightDarkRedditIds
            }
            assertTrue(lightDarkRedditEntities.isNotEmpty())
            assertTrue(
                decodedRedditEntities.containsAll(
                    lightDarkRedditEntities.sortedBy { it.id },
                ),
            )

            val decodedWallhavenSavedSearches = decoded.wallhaven?.savedSearches
            assertTrue(decodedWallhavenSavedSearches?.isNotEmpty() == true)
            assertEquals(
                dbSavedSearchesMap[OnlineSource.WALLHAVEN]?.sortedBy { it.id },
                decodedWallhavenSavedSearches?.sortedBy { it.id },
            )

            val decodedRedditSavedSearches = decoded.reddit?.savedSearches
            assertTrue(decodedRedditSavedSearches?.isNotEmpty() == true)
            assertEquals(
                dbSavedSearchesMap[OnlineSource.REDDIT]?.sortedBy { it.id },
                decodedRedditSavedSearches?.sortedBy { it.id },
            )
        } finally {
            dataStore.clear()
        }
    }

    @Test
    fun shouldRestoreBackupWith1000Records() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDbWith1000Records()
            val oldDbViewed = viewedDao.getAll()

            val oldDbFavorites = favoriteDao.getAll()
            val oldFavWallhavenIds = oldDbFavorites
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            val oldDbFavWallpapersWithUploaderAndTags =
                wallhavenWallpapersDao.getAllWithUploaderAndTags()
                    .filter { it.wallpaper.wallhavenId in oldFavWallhavenIds }
            val oldDbFavWallhavenWallpapers = oldDbFavWallpapersWithUploaderAndTags.map {
                it.wallpaper
            }
            val oldFavRedditIds = oldDbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val oldDbFavRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldFavRedditIds
            }

            val oldDbLightDark = lightDarkDao.getAll()
            val oldLightDarkWallhavenIds = oldDbLightDark
                .filter { it.source == Source.WALLHAVEN }
                .filter { it.sourceId !in oldFavWallhavenIds }
                .map { it.sourceId }
            val oldDbLightDarkWallpapersWithUploaderAndTags =
                wallhavenWallpapersDao.getAllWithUploaderAndTags()
                    .filter { it.wallpaper.wallhavenId in oldLightDarkWallhavenIds }
            val oldDbLightDarkWallhavenWallpapers = oldDbLightDarkWallpapersWithUploaderAndTags
                .map {
                    it.wallpaper
                }
            val oldLightDarkRedditIds = oldDbLightDark
                .filter { it.source == Source.REDDIT }
                .filter { it.sourceId !in oldFavRedditIds }
                .map { it.sourceId }
            val oldDbLightDarkRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldLightDarkRedditIds
            }

            val oldDbWallhavenWallpapers = oldDbFavWallhavenWallpapers +
                oldDbLightDarkWallhavenWallpapers
            val oldDbRedditWallpapers = oldDbFavRedditWallpapers +
                oldDbLightDarkRedditWallpapers

            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                appPreferencesRepository = appPreferencesRepository,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            assertNotNull(json)
            // clear all tables and preferences
            fakeDb.clearAllTables()
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                appPreferencesRepository = appPreferencesRepository,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    viewed = true,
                    lightDark = true,
                    file = Uri.EMPTY,
                ),
            )
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbLightDark = lightDarkDao.getAll()
            val dbViewed = viewedDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallhavenWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallhavenWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbFavorites
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbLightDark
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbLightDark
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbViewed
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
                dbViewed
                    .map { it.copy(id = 0) }
                    .sortedBy { it.sourceId },
            )
            assertEquals(
                oldDbRedditWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.redditId },
                dbRedditWallpapers
                    .map { it.copy(id = 0) }
                    .sortedBy { it.redditId },
            )
            assertEquals(
                oldDbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
                dbSavedSearches
                    .map { it.copy(id = 0) }
                    .sortedBy { it.name },
            )
        } finally {
            dataStore.clear()
        }
    }

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
