package com.ammar.wallflow.ui.screens.home

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ammar.wallflow.ui.screens.main.RootNavControllerWrapper
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Ignore("Incomplete test")
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    // private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
    //     scope = this,
    //     produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
    // )

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun initialTest() = runTest(testDispatcher) {
        // val dataStore = dataStore()
        // val viewModel = getViewModel(
        //     dataStore = dataStore,
        //     coroutineDispatcher = testDispatcher,
        // )
        composeTestRule.setContent {
            WallFlowTheme {
                HomeScreen(
                    navController = TestNavHostController(context),
                    rootNavControllerWrapper = RootNavControllerWrapper(
                        navController = TestNavHostController(context),
                    ),
                )
            }
        }
    }

    // private fun getViewModel(
    //     dataStore: DataStore<Preferences>,
    //     coroutineDispatcher: CoroutineDispatcher,
    // ): HomeViewModel {
    //     val wallhavenWallpapersDao = FakeWallhavenWallpapersDao()
    //     val redditWallpapersDao = FakeRedditWallpapersDao()
    //     val localWallpapersRepository = FakeLocalWallpapersRepository()
    //     return HomeViewModel(
    //         wallhavenRepository = object : WallhavenRepository {
    //             override fun wallpapersPager(
    //                 search: WallhavenSearch,
    //                 pageSize: Int,
    //                 prefetchDistance: Int,
    //                 initialLoadSize: Int,
    //             ): Flow<PagingData<Wallpaper>> {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override fun popularTags(): Flow<Resource<List<WallhavenTag>>> {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override suspend fun refreshPopularTags() {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override fun wallpaper(
    //                 wallpaperWallhavenId: String,
    //             ): Flow<Resource<WallhavenWallpaper?>> {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override suspend fun insertTagEntities(
    //                 tags: Collection<WallhavenTagEntity>,
    //             ) {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override suspend fun insertUploaderEntities(
    //                 uploaders: Collection<WallhavenUploaderEntity>,
    //             ) {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override suspend fun insertWallpaperEntities(
    //                 entities: Collection<WallhavenWallpaperEntity>,
    //             ) {
    //                 TODO("Not yet implemented")
    //             }
    //         },
    //         redditRepository = object : RedditRepository {
    //             override fun wallpapersPager(
    //                 search: RedditSearch,
    //                 pageSize: Int,
    //                 prefetchDistance: Int,
    //                 initialLoadSize: Int,
    //             ): Flow<PagingData<Wallpaper>> {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override fun wallpaper(wallpaperId: String): Flow<Resource<RedditWallpaper?>> {
    //                 TODO("Not yet implemented")
    //             }
    //
    //             override suspend fun insertWallpaperEntities(
    //                 entities: Collection<RedditWallpaperEntity>,
    //             ) {
    //                 TODO("Not yet implemented")
    //             }
    //         },
    //         appPreferencesRepository = AppPreferencesRepository(
    //             context = context,
    //             dataStore = dataStore,
    //             ioDispatcher = coroutineDispatcher,
    //         ),
    //         savedSearchRepository = SavedSearchRepository(
    //             savedSearchDao = FakeSavedSearchDao(),
    //             ioDispatcher = coroutineDispatcher,
    //         ),
    //         favoritesRepository = FavoritesRepository(
    //             favoriteDao = FakeFavoriteDao(),
    //             wallhavenWallpapersDao = wallhavenWallpapersDao,
    //             redditWallpapersDao = redditWallpapersDao,
    //             localWallpapersRepository = localWallpapersRepository,
    //             ioDispatcher = coroutineDispatcher,
    //         ),
    //         savedStateHandle = SavedStateHandle(),
    //         viewedRepository = ViewedRepository(
    //             viewedDao = FakeViewedDao(),
    //             ioDispatcher = coroutineDispatcher,
    //         ),
    //         lightDarkRepository = LightDarkRepository(
    //             lightDarkDao = FakeLightDarkDao(),
    //             wallhavenWallpapersDao = wallhavenWallpapersDao,
    //             redditWallpapersDao = redditWallpapersDao,
    //             localWallpapersRepository = localWallpapersRepository,
    //             ioDispatcher = coroutineDispatcher,
    //         ),
    //     )
    // }

    companion object {
        // private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
