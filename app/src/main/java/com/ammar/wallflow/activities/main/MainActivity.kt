package com.ammar.wallflow.activities.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ammar.wallflow.MainDispatcher
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.destinations.HomeScreenDestination
import com.ammar.wallflow.destinations.MainWallhavenApiKeyDialogDestination
import com.ammar.wallflow.destinations.WallpaperScreenDestination
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.extensions.toPxF
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenTagSearchMeta
import com.ammar.wallflow.model.search.WallhavenUploaderSearchMeta
import com.ammar.wallflow.navArgs
import com.ammar.wallflow.navigation.MainNavigation
import com.ammar.wallflow.ui.common.DefaultSystemController
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.SearchBar
import com.ammar.wallflow.ui.common.SystemState
import com.ammar.wallflow.ui.common.bottombar.BottomBarDestination
import com.ammar.wallflow.ui.common.bottombar.BottomBarState
import com.ammar.wallflow.ui.common.bottombar.DefaultBottomBarController
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.DefaultMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBar
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarState
import com.ammar.wallflow.ui.common.searchedit.SaveAsDialog
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.screens.home.HomeScreenNavArgs
import com.ammar.wallflow.ui.theme.EdgeToEdge
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.utils.startDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val wallhavenUriPattern = Regex(
        "https?://(?:whvn|wallhaven).cc(?:/w)?/(?<wallpaperId>\\S+)",
        RegexOption.IGNORE_CASE,
    )
    private lateinit var navController: NavHostController
    private var consumedIntent = false

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            consumedIntent = savedInstanceState.getBoolean(
                SAVED_INSTANCE_STATE_CONSUMED_INTENT,
                false,
            )
        }

        // Fix leak for Android Q
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (isTaskRoot) {
                            finishAfterTransition()
                        }
                    }
                },
            )
        }

        setContent {
            CompositionLocalProvider(
                LocalSystemController provides DefaultSystemController(SystemState()),
                LocalBottomBarController provides DefaultBottomBarController(BottomBarState()),
                LocalMainSearchBarController provides DefaultMainSearchBarController(
                    MainSearchBarState(),
                ),
            ) {
                Content()
            }
        }
    }

    @OptIn(
        ExperimentalMaterial3WindowSizeClassApi::class,
        ExperimentalComposeUiApi::class,
    )
    @Composable
    private fun Content() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
        val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

        navController = rememberNavController()
        val viewModel: MainActivityViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = currentBackStackEntry?.destination
        val rootNavGraphs = remember { BottomBarDestination.entries.map { it.graph } }
        val showBackButton = remember(currentBackStackEntry, rootNavGraphs) {
            if (currentDestination == null) {
                return@remember false
            }
            if (currentDestination.route == HomeScreenDestination.route) {
                val navArgs: HomeScreenNavArgs? = currentBackStackEntry?.navArgs()
                return@remember navArgs?.search != null
            }
            rootNavGraphs.none { it.startDestination.route == currentDestination.route }
        }
        val systemController = LocalSystemController.current
        val systemState by systemController.state
        val bottomBarController = LocalBottomBarController.current
        val searchBarController = LocalMainSearchBarController.current
        val searchBarControllerState by searchBarController.state

        val searchBarQuery by remember {
            derivedStateOf {
                when (uiState.searchBarSearch.meta) {
                    is WallhavenTagSearchMeta, is WallhavenUploaderSearchMeta -> {
                        if (uiState.searchBarActive) uiState.searchBarSearch.query else ""
                    }
                    else -> uiState.searchBarSearch.query
                }
            }
        }

        val searchBarHeightPx = SearchBar.Defaults.height.toPxF()
        var searchBarOffsetHeightPx by remember { mutableFloatStateOf(0f) }
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val delta = available.y
                    val newOffset = searchBarOffsetHeightPx + delta
                    searchBarOffsetHeightPx = newOffset.coerceIn(-searchBarHeightPx, 0f)
                    return Offset.Zero
                }
            }
        }

        LaunchedEffect(Unit) {
            withContext(mainDispatcher) {
                handleIntent(intent ?: return@withContext)
            }
        }

        LaunchedEffect(searchBarControllerState.search) {
            viewModel.setSearchBarSearch(searchBarControllerState.search)
        }

        LaunchedEffect(searchBarControllerState.source) {
            viewModel.setSearchBarSource(searchBarControllerState.source)
        }

        LaunchedEffect(useNavRail) {
            bottomBarController.update { it.copy(isRail = useNavRail) }
        }

        LaunchedEffect(isExpanded) {
            systemController.update { it.copy(isExpanded = isExpanded) }
        }

        val systemInDarkTheme = isSystemInDarkTheme()
        val darkTheme = remember(uiState.theme, systemInDarkTheme) {
            when (uiState.theme) {
                Theme.SYSTEM -> systemInDarkTheme
                Theme.LIGHT -> false
                Theme.DARK -> true
            }
        }

        EdgeToEdge(
            darkTheme = darkTheme,
            statusBarColor = systemState.statusBarColor,
            navigationBarColor = systemState.navigationBarColor,
            statusBarVisible = systemState.statusBarVisible,
            navigationBarVisible = systemState.navigationBarVisible,
            isStatusBarLight = systemState.isStatusBarLight,
        )

        WallFlowTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        systemController.update {
                            it.copy(size = size)
                        }
                    }
                    .semantics { testTagsAsResourceId = true },
                color = MaterialTheme.colorScheme.background,
            ) {
                MainActivityContent(
                    currentDestination = currentDestination,
                    useNavRail = useNavRail,
                    globalErrors = uiState.globalErrors,
                    bottomBarVisible = bottomBarController.state.value.visible,
                    bottomBarSize = bottomBarController.state.value.size,
                    showLocalTab = uiState.showLocalTab,
                    searchBar = {
                        MainSearchBar(
                            modifier = Modifier.offset {
                                IntOffset(x = 0, y = searchBarOffsetHeightPx.roundToInt())
                            },
                            useDocked = isExpanded,
                            visible = searchBarControllerState.visible,
                            active = uiState.searchBarActive,
                            search = uiState.searchBarSearch,
                            query = searchBarQuery,
                            suggestions = uiState.searchBarSuggestions,
                            deleteSuggestion = uiState.searchBarDeleteSuggestion,
                            overflowIcon = searchBarControllerState.overflowIcon,
                            showQuery = searchBarControllerState.showQuery,
                            onQueryChange = viewModel::onSearchBarQueryChange,
                            onBackClick = if (showBackButton) {
                                { navController.navigateUp() }
                            } else {
                                null
                            },
                            onSearch = {
                                if (it.isBlank()) {
                                    return@MainSearchBar
                                }
                                val search = if (it.trimAll() == searchBarQuery) {
                                    // keep current search data if query hasn't changed
                                    // this allows to keep meta data if only filters were changed
                                    val searchBarSearch = uiState.searchBarSearch
                                    val filters = uiState.searchBarSearch.filters
                                    when (searchBarSearch) {
                                        is RedditSearch -> searchBarSearch.copy(
                                            filters = filters as RedditFilters,
                                        )
                                        is WallhavenSearch -> searchBarSearch.copy(
                                            filters = filters as WallhavenFilters,
                                        )
                                    }
                                } else {
                                    WallhavenSearch(
                                        query = it,
                                        filters = uiState.searchBarSearch.filters
                                            as WallhavenFilters,
                                    )
                                }
                                doSearch(
                                    viewModel = viewModel,
                                    navController = navController,
                                    searchBarController = searchBarController,
                                    search = search,
                                )
                            },
                            onSuggestionClick = {
                                doSearch(
                                    viewModel = viewModel,
                                    navController = navController,
                                    searchBarController = searchBarController,
                                    search = it.value,
                                )
                            },
                            onSuggestionInsert = { viewModel.setSearchBarSearch(it.value) },
                            onSuggestionDeleteRequest = {
                                viewModel.setShowSearchBarSuggestionDeleteRequest(it.value)
                            },
                            onActiveChange = { active ->
                                viewModel.setSearchBarActive(active)
                                viewModel.setShowSearchBarFilters(false)
                                if (!isExpanded) {
                                    systemController.update {
                                        it.copy(
                                            statusBarColor = if (active) {
                                                Color.Transparent
                                            } else {
                                                Color.Unspecified
                                            },
                                        )
                                    }
                                }
                                searchBarControllerState.onActiveChange(active)
                            },
                            onDeleteSuggestionConfirmClick = {
                                uiState.searchBarDeleteSuggestion?.run {
                                    viewModel.deleteSearch(this)
                                }
                            },
                            onDeleteSuggestionDismissRequest = {
                                viewModel.setShowSearchBarSuggestionDeleteRequest(null)
                            },
                            onSaveAsClick = {
                                val searchBarSearch = uiState.searchBarSearch
                                val query = uiState.searchBarSearch.query
                                val updated = when (searchBarSearch) {
                                    is RedditSearch -> searchBarSearch.copy(
                                        query = query,
                                    )
                                    is WallhavenSearch -> searchBarSearch.copy(
                                        query = query,
                                    )
                                }
                                viewModel.showSaveSearchAsDialog(updated)
                            },
                            onLoadClick = viewModel::showSavedSearches,
                        )
                    },
                    onFixWallHavenApiKeyClick = {
                        navController.navigate(MainWallhavenApiKeyDialogDestination)
                    },
                    onDismissGlobalError = viewModel::dismissGlobalError,
                    onBottomBarSizeChanged = { size ->
                        bottomBarController.update { it.copy(size = size) }
                    },
                    onBottomBarItemClick = {
                        navController.navigate(it.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                ) {
                    MainNavigation(
                        navController = navController,
                        contentPadding = it,
                        applyContentPadding = systemState.applyScaffoldPadding,
                        nestedScrollConnection = nestedScrollConnection,
                    )
                }

                uiState.saveSearchAsSearch?.run {
                    SaveAsDialog(
                        checkNameExists = viewModel::checkSavedSearchNameExists,
                        onSave = {
                            viewModel.saveSearchAs(it, this)
                            viewModel.showSaveSearchAsDialog(null)
                        },
                        onDismissRequest = { viewModel.showSaveSearchAsDialog(null) },
                    )
                }

                if (uiState.showSavedSearchesDialog) {
                    SavedSearchesDialog(
                        savedSearches = uiState.savedSearches,
                        onSelect = {
                            doSearch(
                                viewModel = viewModel,
                                navController = navController,
                                searchBarController = searchBarController,
                                search = it.search,
                            )
                            viewModel.showSavedSearches(false)
                            viewModel.setSearchBarActive(false)
                        },
                        onDismissRequest = { viewModel.showSavedSearches(false) },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_INSTANCE_STATE_CONSUMED_INTENT, true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (consumedIntent) {
            return
        }
        val handled = navController.handleDeepLink(intent)
        if (handled) {
            return
        }
        val result = wallhavenUriPattern.matchEntire(intent.data?.toString() ?: "") ?: return
        val wallpaperId = result.groups["wallpaperId"]?.value ?: return
        navController.navigate(
            WallpaperScreenDestination(
                source = Source.WALLHAVEN,
                wallpaperId = wallpaperId,
                thumbData = null,
            ),
        )
    }

    private fun doSearch(
        viewModel: MainActivityViewModel,
        navController: NavHostController,
        searchBarController: MainSearchBarController,
        search: Search,
    ) {
        if (searchBarController.state.value.search == search) {
            return
        }
        viewModel.onSearch(search)
        navController.search(search)
    }

    companion object {
        private const val SAVED_INSTANCE_STATE_CONSUMED_INTENT =
            "SAVED_INSTANCE_STATE_CONSUMED_INTENT"
    }
}
