package com.ammar.wallflow.ui.screens.home

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.TestNavHostController
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.reddit.RedditRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.reddit.RedditWallpaper
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.workers.FakeFavoriteDao
import com.ammar.wallflow.workers.FakeLocalWallpapersRepository
import com.ammar.wallflow.workers.FakeRedditWallpapersDao
import com.ammar.wallflow.workers.FakeSavedSearchDao
import com.ammar.wallflow.workers.FakeWallhavenWallpapersDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = this,
        produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
    )

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun initialTest() = runTest(testDispatcher) {
        val dataStore = dataStore()
        val viewModel = getViewModel(
            dataStore = dataStore,
            coroutineDispatcher = testDispatcher,
        )
        composeTestRule.setContent {
            WallFlowTheme {
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {}
                }
                HomeScreen(
                    navController = TestNavHostController(context),
                    nestedScrollConnectionGetter = { nestedScrollConnection },
                )
            }
        }
    }

    private fun getViewModel(
        dataStore: DataStore<Preferences>,
        coroutineDispatcher: CoroutineDispatcher,
    ) = HomeViewModel(
        wallhavenRepository = object : WallhavenRepository {
            override fun wallpapersPager(
                search: WallhavenSearch,
                pageSize: Int,
                prefetchDistance: Int,
                initialLoadSize: Int,
            ): Flow<PagingData<Wallpaper>> {
                TODO("Not yet implemented")
            }

            override fun popularTags(): Flow<Resource<List<WallhavenTag>>> {
                TODO("Not yet implemented")
            }

            override suspend fun refreshPopularTags() {
                TODO("Not yet implemented")
            }

            override fun wallpaper(wallpaperWallhavenId: String): Flow<Resource<WallhavenWallpaper?>> {
                TODO("Not yet implemented")
            }

            override suspend fun insertTagEntities(tags: Collection<WallhavenTagEntity>) {
                TODO("Not yet implemented")
            }

            override suspend fun insertUploaderEntities(uploaders: Collection<WallhavenUploaderEntity>) {
                TODO("Not yet implemented")
            }

            override suspend fun insertWallpaperEntities(entities: Collection<WallhavenWallpaperEntity>) {
                TODO("Not yet implemented")
            }
        },
        redditRepository = object : RedditRepository {
            override fun wallpapersPager(
                search: RedditSearch,
                pageSize: Int,
                prefetchDistance: Int,
                initialLoadSize: Int,
            ): Flow<PagingData<Wallpaper>> {
                TODO("Not yet implemented")
            }

            override fun wallpaper(wallpaperId: String): Flow<Resource<RedditWallpaper?>> {
                TODO("Not yet implemented")
            }

            override suspend fun insertWallpaperEntities(entities: Collection<RedditWallpaperEntity>) {
                TODO("Not yet implemented")
            }
        },
        appPreferencesRepository = AppPreferencesRepository(
            dataStore = dataStore,
            ioDispatcher = coroutineDispatcher,
        ),
        savedSearchRepository = SavedSearchRepository(
            savedSearchDao = FakeSavedSearchDao(),
            ioDispatcher = coroutineDispatcher,
        ),
        favoritesRepository = FavoritesRepository(
            favoriteDao = FakeFavoriteDao(),
            wallhavenWallpapersDao = FakeWallhavenWallpapersDao(),
            redditWallpapersDao = FakeRedditWallpapersDao(),
            localWallpapersRepository = FakeLocalWallpapersRepository(),
            ioDispatcher = coroutineDispatcher,
        ),
        savedStateHandle = SavedStateHandle(),
    )

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
