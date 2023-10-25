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
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.model.reddit.toWallpaperEntities
import com.ammar.wallflow.data.network.model.wallhaven.toEntity
import com.ammar.wallflow.data.network.model.wallhaven.toWallpaperEntity
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.reddit.DefaultRedditRepository
import com.ammar.wallflow.data.repository.wallhaven.DefaultWallhavenRepository
import com.ammar.wallflow.json
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Source
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
        val wallhavenEntitiesWithoutUploaders = wallhavenEntities.filter { it.uploaderId == null }
        val wallhavenEntitiesWithUploaders = wallhavenEntities.filter { it.uploaderId != null }
        val favoriteWallhavenEntities = wallhavenEntitiesWithoutUploaders.shuffled().take(2) +
            wallhavenEntitiesWithUploaders.shuffled().take(2)
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

        val savedSearches = MockFactory.generateWallhavenSavedSearches(random = random) +
            MockFactory.generateRedditSavedSearches(random = random)
        savedSearchDao.upsert(savedSearches.map { it.toEntity() })
    }

    private suspend fun insertWallhavenEntities(
        allWallhavenUploadersNull: Boolean = false,
    ): List<WallhavenWallpaperEntity> {
        val networkWallhavenWallpapers = MockFactory.generateNetworkWallhavenWallpapers(
            random = random,
            clock = clock,
            allUploadersNull = allWallhavenUploadersNull,
        )
        val networkUploaders = networkWallhavenWallpapers.mapNotNull { it.uploader }
        val networkTags = networkWallhavenWallpapers.flatMap { it.tags ?: emptyList() }
        tagsDao.insert(networkTags.map { it.toEntity() })
        // need wallhaven tag id to wallhaven wallpaper id mapping
        val wallpaperTagsMap = networkWallhavenWallpapers.associate {
            it.id to it.tags?.map { t -> t.id }
        }
        val dbTagMap = tagsDao.getAll().associateBy { it.wallhavenId }

        uploadersDao.insert(networkUploaders.map { it.toEntity() })

        val dbUploaderUsernameMap = uploadersDao.getAll().associateBy { it.username }
        val whIdToUploaderUsernameMap = networkWallhavenWallpapers.associate {
            it.id to it.uploader?.username
        }
        val wallpaperEntities = networkWallhavenWallpapers.map {
            val uploaderUsername = whIdToUploaderUsernameMap[it.id]
            val uploaderId = dbUploaderUsernameMap[uploaderUsername]?.id
            it.toWallpaperEntity(
                uploaderId = uploaderId,
            )
        }
        wallhavenWallpapersDao.insert(wallpaperEntities)

        val wallpaperMap = wallhavenWallpapersDao.getAll().associateBy { it.wallhavenId }
        val wallpaperTagEntities = wallpaperMap.flatMap {
            val whTagIds = wallpaperTagsMap[it.key] ?: emptyList()
            val tagDbIds = whTagIds.mapNotNull { tId -> dbTagMap[tId]?.id }
            tagDbIds.map { tDbId ->
                WallhavenWallpaperTagsEntity(
                    wallpaperId = it.value.id,
                    tagId = tDbId,
                )
            }
        }
        wallhavenWallpapersDao.insertWallpaperTagMappings(wallpaperTagEntities)
        return wallpaperEntities
    }

    private suspend fun insertRedditEntities(): List<RedditWallpaperEntity> {
        val networkRedditPosts = MockFactory.generateNetworkRedditPosts(
            random = random,
        )
        val wallpaperEntities = networkRedditPosts.flatMap {
            it.toWallpaperEntities()
        }
        redditWallpapersDao.insert(wallpaperEntities)
        return wallpaperEntities
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

    @Test
    fun shouldCreateCorrectBackupJson() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb()
            val jsonStr = getBackupV1Json(
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(jsonStr)
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
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

            val favoritedWallhavenIds = dbFavorites
                .filter { it.source == Source.WALLHAVEN }
                .map { it.sourceId }
            assertEquals(
                dbWallhavenWallpapers
                    .filter { it.wallhavenId in favoritedWallhavenIds }
                    .sortedBy { it.id },
                decoded.wallhaven?.wallpapers?.sortedBy { it.id },
            )

            val favoritedRedditIds = dbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            assertEquals(
                dbRedditWallpapers
                    .filter { it.redditId in favoritedRedditIds }
                    .sortedBy { it.id },
                decoded.reddit?.wallpapers?.sortedBy { it.id },
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

    @Test
    fun shouldRestoreBackup() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val appPreferencesRepository = dataStore.appPreferencesRepository
        try {
            initDb()
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
            val oldDbWallhavenUploaders = oldDbFavWallpapersWithUploaderAndTags.mapNotNull {
                it.uploader
            }
            val oldDbWallhavenTags = oldDbFavWallpapersWithUploaderAndTags.flatMap {
                it.tags ?: emptyList()
            }
            val oldFavRedditIds = oldDbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val oldDbRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldFavRedditIds
            }
            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)
            // clear all tables and preferences
            fakeDb.clearAllTables()
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                context = context,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
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
                uploadersDao = uploadersDao,
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbUploaders = uploadersDao.getAll()
            val dbTags = tagsDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            assertEquals(oldPreferences, preferences)
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallhavenWallpapers
                    .map { it.copy(id = 0, uploaderId = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallhavenWallpapers
                    .map { it.copy(id = 0, uploaderId = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbWallhavenTags
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbTags
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbWallhavenUploaders
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
            val oldPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val json = getBackupV1Json(
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                context = context,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
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
                uploadersDao = uploadersDao,
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
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                context = context,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
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
                uploadersDao = uploadersDao,
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
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)

            // change prefs again without backup
            appPreferencesRepository.updateBlurSketchy(false)
            appPreferencesRepository.updateBlurSketchy(false)

            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                context = context,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
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
                uploadersDao = uploadersDao,
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
            val oldDbFavWallpapersWithUploaderAndTags =
                wallhavenWallpapersDao.getAllWithUploaderAndTags()
                    .filter { it.wallpaper.wallhavenId in oldFavWallhavenIds }
            val oldDbWallpapers = oldDbFavWallpapersWithUploaderAndTags.map { it.wallpaper }
            val oldDbUploaders = oldDbFavWallpapersWithUploaderAndTags.mapNotNull { it.uploader }
            val oldDbTags = oldDbFavWallpapersWithUploaderAndTags.flatMap {
                it.tags ?: emptyList()
            }
            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)

            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                context = context,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
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
                uploadersDao = uploadersDao,
            )
            val dbFavorites = favoriteDao.getAll()
            val dbFavWallhavenIds = dbFavorites.map { it.sourceId }
            val dbWallsWithTagsAndUploaders =
                wallhavenWallpapersDao.getAllWithUploaderAndTagsByWallhavenIds(
                    dbFavWallhavenIds,
                )
            val dbWallpapers = dbWallsWithTagsAndUploaders.map { it.wallpaper }
            val dbUploaders = dbWallsWithTagsAndUploaders.mapNotNull { it.uploader }
            val dbTags = dbWallsWithTagsAndUploaders.flatMap { it.tags ?: emptyList() }
            val dbSavedSearches = savedSearchDao.getAll()
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallpapers
                    .map { it.copy(id = 0, uploaderId = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallpapers
                    .map { it.copy(id = 0, uploaderId = 0) }
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
            val oldDbWallhavenUploaders = oldDbFavWallpapersWithUploaderAndTags.mapNotNull {
                it.uploader
            }
            val oldDbWallhavenTags = oldDbFavWallpapersWithUploaderAndTags.flatMap {
                it.tags ?: emptyList()
            }
            val oldFavRedditIds = oldDbFavorites
                .filter { it.source == Source.REDDIT }
                .map { it.sourceId }
            val oldDbRedditWallpapers = redditWallpapersDao.getAll().filter {
                it.redditId in oldFavRedditIds
            }
            val oldDbSavedSearches = savedSearchDao.getAll()
            val json = getBackupV1Json(
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallhavenWallpapersDao = wallhavenWallpapersDao,
                redditWallpapersDao = redditWallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)
            // clear all tables and preferences
            fakeDb.clearAllTables()
            dataStore.clear()
            // perform restore
            val backup = readBackupJson(json)
            restoreBackup(
                context = context,
                backup = backup,
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
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
                uploadersDao = uploadersDao,
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val dbWallhavenWallpapers = wallhavenWallpapersDao.getAll()
            val dbUploaders = uploadersDao.getAll()
            val dbTags = tagsDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbRedditWallpapers = redditWallpapersDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            assertEquals(oldPreferences, preferences)
            // need to reset db ids before comparing
            assertEquals(
                oldDbWallhavenWallpapers
                    .map { it.copy(id = 0, uploaderId = 0) }
                    .sortedBy { it.wallhavenId },
                dbWallhavenWallpapers
                    .map { it.copy(id = 0, uploaderId = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbWallhavenTags
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
                dbTags
                    .map { it.copy(id = 0) }
                    .sortedBy { it.wallhavenId },
            )
            assertEquals(
                oldDbWallhavenUploaders
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
    }

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
