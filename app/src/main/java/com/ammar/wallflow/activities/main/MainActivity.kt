package com.ammar.wallflow.activities.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.extensions.toPxF
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.TagSearchMeta
import com.ammar.wallflow.model.UploaderSearchMeta
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.SearchBar
import com.ammar.wallflow.ui.common.bottombar.BottomBarDestination
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarController
import com.ammar.wallflow.ui.common.searchedit.SaveAsDialog
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.screens.destinations.HomeScreenDestination
import com.ammar.wallflow.ui.screens.destinations.WallhavenApiKeyDialogDestination
import com.ammar.wallflow.ui.screens.home.HomeScreenNavArgs
import com.ammar.wallflow.ui.screens.navArgs
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.utils.startDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
            val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

            val navController = rememberNavController()
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
                        is TagSearchMeta, is UploaderSearchMeta -> {
                            if (uiState.searchBarActive) uiState.searchBarSearch.query else ""
                        }
                        else -> uiState.searchBarSearch.query
                    }
                }
            }
            val statusBarSemiTransparentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
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

            LaunchedEffect(searchBarControllerState.search) {
                viewModel.setSearchBarSearch(searchBarControllerState.search)
            }

            LaunchedEffect(useNavRail) {
                bottomBarController.update { it.copy(isRail = useNavRail) }
            }

            LaunchedEffect(isExpanded) {
                systemController.update { it.copy(isExpanded = isExpanded) }
            }

            WallFlowTheme(
                darkTheme = when (uiState.theme) {
                    Theme.SYSTEM -> isSystemInDarkTheme()
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                },
                statusBarVisible = systemState.statusBarVisible,
                statusBarColor = systemState.statusBarColor,
                navigationBarVisible = systemState.navigationBarVisible,
                navigationBarColor = systemState.navigationBarColor,
                lightStatusBars = systemState.lightStatusBars,
                lightNavigationBars = systemState.lightNavigationBars,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size -> systemController.update { it.copy(size = size) } },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainActivityContent(
                        currentDestination = currentDestination,
                        showBackButton = showBackButton,
                        useNavRail = useNavRail,
                        useDockedSearchBar = isExpanded,
                        globalErrors = uiState.globalErrors,
                        bottomBarVisible = bottomBarController.state.value.visible,
                        bottomBarSize = bottomBarController.state.value.size,
                        searchBarOffsetHeightPx = searchBarOffsetHeightPx,
                        searchBarVisible = searchBarControllerState.visible,
                        searchBarActive = uiState.searchBarActive,
                        searchBarSearch = uiState.searchBarSearch,
                        searchBarQuery = searchBarQuery,
                        searchBarSuggestions = uiState.searchBarSuggestions,
                        showSearchBarFilters = uiState.showSearchBarFilters,
                        searchBarDeleteSuggestion = uiState.searchBarDeleteSuggestion,
                        searchBarOverflowIcon = searchBarControllerState.overflowIcon,
                        searchBarShowNSFW = uiState.searchBarShowNSFW,
                        searchBarShowQuery = searchBarControllerState.showQuery,
                        onSearchBarQueryChange = viewModel::onSearchBarQueryChange,
                        onBackClick = { navController.navigateUp() },
                        onSearchBarSearch = {
                            if (it.isBlank()) {
                                return@MainActivityContent
                            }
                            val search = if (it.trimAll() == searchBarQuery) {
                                // keep current search data if query hasn't changed
                                // this allows to keep meta data if only filters were changed
                                uiState.searchBarSearch.copy(
                                    filters = uiState.searchBarSearch.filters,
                                )
                            } else {
                                Search(
                                    query = it,
                                    filters = uiState.searchBarSearch.filters,
                                )
                            }
                            doSearch(
                                viewModel = viewModel,
                                navController = navController,
                                searchBarController = searchBarController,
                                search = search,
                            )
                        },
                        onSearchBarSuggestionClick = {
                            doSearch(
                                viewModel = viewModel,
                                navController = navController,
                                searchBarController = searchBarController,
                                search = it.value,
                            )
                        },
                        onSearchBarSuggestionInsert = { viewModel.setSearchBarSearch(it.value) },
                        onSearchBarSuggestionDeleteRequest = {
                            viewModel.setShowSearchBarSuggestionDeleteRequest(it.value)
                        },
                        onSearchBarActiveChange = { active ->
                            viewModel.setSearchBarActive(active)
                            viewModel.setShowSearchBarFilters(false)
                            if (!isExpanded) {
                                systemController.update {
                                    it.copy(
                                        statusBarColor = if (active) {
                                            statusBarSemiTransparentColor
                                        } else {
                                            Color.Unspecified
                                        },
                                    )
                                }
                                bottomBarController.update { it.copy(visible = !active) }
                            }
                            searchBarControllerState.onActiveChange(active)
                        },
                        onSearchBarShowFiltersChange = viewModel::setShowSearchBarFilters,
                        onSearchBarFiltersChange = {
                            viewModel.setSearchBarSearch(
                                uiState.searchBarSearch.copy(filters = it),
                            )
                        },
                        onDeleteSearchBarSuggestionConfirmClick = {
                            uiState.searchBarDeleteSuggestion?.run { viewModel.deleteSearch(this) }
                        },
                        onDeleteSearchBarSuggestionDismissRequest = {
                            viewModel.setShowSearchBarSuggestionDeleteRequest(null)
                        },
                        onFixWallHavenApiKeyClick = {
                            navController.navigate(WallhavenApiKeyDialogDestination)
                        },
                        onDismissGlobalError = viewModel::dismissGlobalError,
                        onBottomBarSizeChanged = { size ->
                            bottomBarController.update { it.copy(size = size) }
                        },
                        onBottomBarItemClick = {
                            navController.navigate(it) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSearchBarSaveAsClick = {
                            viewModel.showSaveSearchAsDialog(
                                uiState.searchBarSearch.copy(
                                    query = searchBarQuery,
                                ),
                            )
                        },
                        onSearchBarLoadClick = viewModel::showSavedSearches,
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
}
