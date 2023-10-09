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
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.network.model.wallhaven.toEntity
import com.ammar.wallflow.data.network.model.wallhaven.toWallpaperEntity
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.wallhaven.DefaultWallhavenRepository
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.backup.BackupOptions
import com.ammar.wallflow.model.backup.BackupV1
import com.ammar.wallflow.model.search.toEntity
import com.ammar.wallflow.workers.FakeLocalWallpapersRepository
import com.ammar.wallflow.workers.FakeWallhavenNetworkDataSource
import com.ammar.wallflow.workers.TestClock
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreUtilsTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val mockDb = Room.inMemoryDatabaseBuilder(
        context = ApplicationProvider.getApplicationContext(),
        klass = AppDatabase::class.java,
    ).build()
    private val wallpapersDao = mockDb.wallhavenWallpapersDao()
    private val tagsDao = mockDb.wallhavenTagsDao()
    private val uploadersDao = mockDb.wallhavenUploadersDao()
    private val favoriteDao = mockDb.favoriteDao()
    private val savedSearchDao = mockDb.wallhavenSavedSearchDao()
    private val random = Random(1000)
    private val clock = TestClock(now = Instant.fromEpochMilliseconds(1694954538))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        mockDb.clearAllTables()
    }

    private suspend fun initDb() {
        val networkWallhavenWallpapers = MockFactory.generateNetworkWallpapers(
            random = random,
            clock = clock,
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
        wallpapersDao.insert(wallpaperEntities)

        val wallpaperMap = wallpapersDao.getAll().associateBy { it.wallhavenId }
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
        wallpapersDao.insertWallpaperTagMappings(wallpaperTagEntities)

        // favorites 4 wallpapers
        val favoriteWallpaperEntities = wallpaperEntities.shuffled().take(4)
        val favoriteEntities = favoriteWallpaperEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                favoritedOn = clock.now(),
            )
        }
        favoriteDao.insertAll(favoriteEntities)

        val savedSearches = MockFactory.generateWallhavenSavedSearches(random = random)
        savedSearchDao.upsert(savedSearches.map { it.toEntity() })
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
            val json = getBackupV1Json(
                options = BackupOptions(
                    settings = true,
                    favorites = true,
                    savedSearches = true,
                    file = Uri.EMPTY,
                ),
                appPreferencesRepository = appPreferencesRepository,
                favoriteDao = favoriteDao,
                wallpapersDao = wallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)
            val dbWallpapers = wallpapersDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            val decoded = Json.decodeFromString<BackupV1>(json)
            assertEquals(
                appPreferencesRepository.appPreferencesFlow.firstOrNull(),
                decoded.preferences,
            )
            assertEquals(
                dbFavorites.sortedBy { it.id },
                decoded.favorites?.sortedBy { it.id },
            )
            val favoritedWallhavenIds = dbFavorites.map { it.sourceId }
            assertEquals(
                dbWallpapers
                    .filter { it.wallhavenId in favoritedWallhavenIds }
                    .sortedBy { it.id },
                decoded.wallhaven?.wallpapers?.sortedBy { it.id },
            )
            assertEquals(
                dbSavedSearches.sortedBy { it.id },
                decoded.wallhaven?.savedSearches?.sortedBy { it.id },
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
            val oldFavWallhavenIds = oldDbFavorites.map { it.sourceId }
            val oldDbFavWallpapersWithUploaderAndTags = wallpapersDao.getAllWithUploaderAndTags()
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
                wallpapersDao = wallpapersDao,
                savedSearchDao = savedSearchDao,
            )
            assertNotNull(json)
            // clear all tables and preferences
            mockDb.clearAllTables()
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
                    appDatabase = mockDb,
                    wallHavenNetwork = FakeWallhavenNetworkDataSource(),
                    ioDispatcher = testDispatcher,
                ),
                favoritesRepository = FavoritesRepository(
                    favoriteDao = favoriteDao,
                    wallpapersDao = wallpapersDao,
                    localWallpapersRepository = FakeLocalWallpapersRepository(),
                    ioDispatcher = testDispatcher,
                ),
                wallpapersDao = wallpapersDao,
                uploadersDao = uploadersDao,
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            val dbWallpapers = wallpapersDao.getAll()
            val dbUploaders = uploadersDao.getAll()
            val dbTags = tagsDao.getAll()
            val dbFavorites = favoriteDao.getAll()
            val dbSavedSearches = savedSearchDao.getAll()
            assertEquals(oldPreferences, preferences)
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
    fun shouldRestoreBackupWithChangedPrefs() = runTest(testDispatcher) {
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
                wallpapersDao = wallpapersDao,
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
                    appDatabase = mockDb,
                    wallHavenNetwork = FakeWallhavenNetworkDataSource(),
                    ioDispatcher = testDispatcher,
                ),
                favoritesRepository = FavoritesRepository(
                    favoriteDao = favoriteDao,
                    wallpapersDao = wallpapersDao,
                    localWallpapersRepository = FakeLocalWallpapersRepository(),
                    ioDispatcher = testDispatcher,
                ),
                wallpapersDao = wallpapersDao,
                uploadersDao = uploadersDao,
            )
            val preferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
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
                wallpapersDao = wallpapersDao,
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
                    appDatabase = mockDb,
                    wallHavenNetwork = FakeWallhavenNetworkDataSource(),
                    ioDispatcher = testDispatcher,
                ),
                favoritesRepository = FavoritesRepository(
                    favoriteDao = favoriteDao,
                    wallpapersDao = wallpapersDao,
                    localWallpapersRepository = FakeLocalWallpapersRepository(),
                    ioDispatcher = testDispatcher,
                ),
                wallpapersDao = wallpapersDao,
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
            val oldDbFavWallpapersWithUploaderAndTags = wallpapersDao.getAllWithUploaderAndTags()
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
                wallpapersDao = wallpapersDao,
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
                    appDatabase = mockDb,
                    wallHavenNetwork = FakeWallhavenNetworkDataSource(),
                    ioDispatcher = testDispatcher,
                ),
                favoritesRepository = FavoritesRepository(
                    favoriteDao = favoriteDao,
                    wallpapersDao = wallpapersDao,
                    localWallpapersRepository = FakeLocalWallpapersRepository(),
                    ioDispatcher = testDispatcher,
                ),
                wallpapersDao = wallpapersDao,
                uploadersDao = uploadersDao,
            )
            val dbFavorites = favoriteDao.getAll()
            val dbFavWallhavenIds = dbFavorites.map { it.sourceId }
            val dbWallsWithTagsAndUploaders = wallpapersDao.getAllWithUploaderAndTagsByWallhavenIds(
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

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
