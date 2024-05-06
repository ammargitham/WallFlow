package com.ammar.wallflow.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.insertRedditEntities
import com.ammar.wallflow.insertWallhavenEntities
import com.ammar.wallflow.model.AutoWallpaperHistory
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import com.ammar.wallflow.workers.FakeLocalWallpapersRepository
import kotlin.random.Random
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var favoriteRepository: FavoritesRepository
    private lateinit var tagsDao: WallhavenTagsDao
    private lateinit var uploadersDao: WallhavenUploadersDao
    private lateinit var wallhavenWallpapersDao: WallhavenWallpapersDao
    private lateinit var redditWallpapersDao: RedditWallpapersDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var autoWallpaperHistoryRepository: AutoWallpaperHistoryRepository

    private val testDispatcher = StandardTestDispatcher()
    private val random = Random(1000)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
        ).build()
        favoriteDao = db.favoriteDao()
        wallhavenWallpapersDao = db.wallhavenWallpapersDao()
        redditWallpapersDao = db.redditWallpapersDao()
        tagsDao = db.wallhavenTagsDao()
        uploadersDao = db.wallhavenUploadersDao()
        autoWallpaperHistoryRepository = AutoWallpaperHistoryRepository(
            autoWallpaperHistoryDao = db.autoWallpaperHistoryDao(),
            ioDispatcher = testDispatcher,
        )
        favoriteRepository = FavoritesRepository(
            favoriteDao = favoriteDao,
            wallhavenWallpapersDao = wallhavenWallpapersDao,
            redditWallpapersDao = redditWallpapersDao,
            localWallpapersRepository = FakeLocalWallpapersRepository(),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        db.clearAllTables()
        db.close()
    }

    @Test
    fun shouldGetFreshWallpaperOnEveryCall() = runTest(testDispatcher) {
        initDb()
        var prevWallpaper: Wallpaper? = null
        repeat(11) {
            // we insert 10 light dark wallpapers, so we will get 10 fresh wallpapers
            val wallpaper = favoriteRepository.getFirstFresh(
                context = context,
                excluding = emptyList(),
            )
            if (it == 10) {
                // should be null after 10 calls
                assertNull(wallpaper)
                return@repeat
            }
            if (wallpaper == null) {
                throw NullPointerException()
            }
            // update auto wallpaper history
            autoWallpaperHistoryRepository.addHistory(
                AutoWallpaperHistory(
                    sourceId = wallpaper.id,
                    source = wallpaper.source,
                    sourceChoice = SourceChoice.LIGHT_DARK,
                    setOn = Clock.System.now(),
                    targets = setOf(WallpaperTarget.HOME),
                ),
            )
            assertNotEquals(wallpaper, prevWallpaper)
            prevWallpaper = wallpaper
        }
    }

    @Test
    fun shouldGetOldestSetWallpaper() = runTest(testDispatcher) {
        initDb()

        // oldest set wallpaper should be null
        var wallpaper = favoriteRepository.getByOldestSetOn(
            context = context,
            excluding = emptyList(),
        )
        assertNull(wallpaper)

        // exhaust all fresh wallpapers
        repeat(10) {
            favoriteRepository.getFirstFresh(
                context = context,
                excluding = emptyList(),
            )?.also {
                autoWallpaperHistoryRepository.addHistory(
                    AutoWallpaperHistory(
                        sourceId = it.id,
                        source = it.source,
                        sourceChoice = SourceChoice.FAVORITES,
                        setOn = Clock.System.now(),
                        targets = setOf(WallpaperTarget.HOME),
                    ),
                )
            } ?: throw NullPointerException()
        }

        // next fresh should be null
        wallpaper = favoriteRepository.getFirstFresh(
            context = context,
            excluding = emptyList(),
        )
        assertNull(wallpaper)

        // oldest should never be null now and should not repeat
        var prevWallpaper: Wallpaper? = null
        repeat(20) {
            wallpaper = favoriteRepository.getByOldestSetOn(
                context = context,
                excluding = emptyList(),
            )
            assertNotNull(wallpaper)
            wallpaper?.let {
                autoWallpaperHistoryRepository.addHistory(
                    AutoWallpaperHistory(
                        sourceId = it.id,
                        source = it.source,
                        sourceChoice = SourceChoice.FAVORITES,
                        setOn = Clock.System.now(),
                        targets = setOf(WallpaperTarget.HOME),
                    ),
                )
            }
            assertNotEquals(wallpaper, prevWallpaper)
            prevWallpaper = wallpaper
        }
    }

    private suspend fun initDb() {
        val wallhavenEntities = insertWallhavenEntities(
            count = 15,
            random = random,
            clock = Clock.System,
            tagsDao = tagsDao,
            uploadersDao = uploadersDao,
            wallhavenWallpapersDao = wallhavenWallpapersDao,
        )
        val redditEntities = insertRedditEntities(
            count = 15,
            random = random,
            redditWallpapersDao = redditWallpapersDao,
        )

        // mark light/dark 5 wallhaven and reddit wallpapers
        val favoriteWallhavenEntities = wallhavenEntities.shuffled().take(5)
        val favoriteRedditEntities = redditEntities.shuffled().take(5)
        val favoriteEntities = favoriteWallhavenEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.WALLHAVEN,
                sourceId = it.wallhavenId,
                favoritedOn = Clock.System.now(),
            )
        } + favoriteRedditEntities.map {
            FavoriteEntity(
                id = 0,
                source = Source.REDDIT,
                sourceId = it.redditId,
                favoritedOn = Clock.System.now(),
            )
        }
        favoriteDao.insertAll(favoriteEntities)
    }
}
