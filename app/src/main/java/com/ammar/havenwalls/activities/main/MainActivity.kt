package com.ammar.havenwalls.activities.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ammar.havenwalls.extensions.trimAll
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.TagSearchMeta
import com.ammar.havenwalls.model.UploaderSearchMeta
import com.ammar.havenwalls.ui.appCurrentDestinationAsState
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.common.bottombar.BottomBarDestination
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBarState
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.havenwalls.ui.common.navigation.rememberTwoPaneNavController
import com.ammar.havenwalls.ui.common.searchedit.SaveAsDialog
import com.ammar.havenwalls.ui.common.searchedit.SavedSearchesDialog
import com.ammar.havenwalls.ui.destinations.WallhavenApiKeyDialogDestination
import com.ammar.havenwalls.ui.home.HomeScreenNavArgs
import com.ammar.havenwalls.ui.navArgs
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ammar.havenwalls.ui.wallpaper.WallpaperViewModel
import com.ramcosta.composedestinations.navigation.navigate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var twoPaneController: TwoPaneNavigation.Controller

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
            val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

            twoPaneController = rememberTwoPaneNavController(
                initialPaneMode = if (isExpanded) Mode.TWO_PANE else Mode.SINGLE_PANE
            )
            val pane1NavController = twoPaneController.pane1NavHostController
            val isTwoPaneMode = twoPaneController.paneMode.value == Mode.TWO_PANE
            val viewModel: MainActivityViewModel = hiltViewModel()
            val wallpaperViewModel: WallpaperViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentDestination by pane1NavController.appCurrentDestinationAsState()
            val currentBackStackEntry by pane1NavController.currentBackStackEntryAsState()
            val rootDestinations = remember {
                BottomBarDestination.values().map { it.direction.route }
            }
            val showBackButton = remember(currentBackStackEntry, rootDestinations) {
                if (currentDestination?.baseRoute == "home_screen") {
                    val navArgs: HomeScreenNavArgs? = currentBackStackEntry?.navArgs()
                    return@remember navArgs?.search != null
                }
                currentDestination?.route !in rootDestinations
            }
            val systemBarsController = LocalSystemBarsController.current
            val systemBarsState by systemBarsController.state
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

            LaunchedEffect(searchBarControllerState.search) {
                viewModel.setSearchBarSearch(searchBarControllerState.search)
            }

            LaunchedEffect(useNavRail) {
                bottomBarController.update { it.copy(isRail = useNavRail) }
            }

            HavenWallsTheme(
                statusBarVisible = systemBarsState.statusBarVisible,
                statusBarColor = systemBarsState.statusBarColor,
                navigationBarVisible = systemBarsState.navigationBarVisible,
                navigationBarColor = systemBarsState.navigationBarColor,
                lightStatusBars = systemBarsState.lightStatusBars,
                lightNavigationBars = systemBarsState.lightNavigationBars,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainActivityContent(
                        currentDestination = currentDestination,
                        showBackButton = showBackButton,
                        useNavRail = useNavRail,
                        useDockedSearchBar = isTwoPaneMode,
                        globalErrors = uiState.globalErrors,
                        searchBarVisible = searchBarControllerState.visible,
                        searchBarActive = uiState.searchBarActive,
                        searchBarSearch = uiState.searchBarSearch,
                        searchBarQuery = searchBarQuery,
                        searchBarSuggestions = uiState.searchBarSuggestions,
                        showSearchBarFilters = uiState.showSearchBarFilters,
                        searchBarDeleteSuggestion = uiState.searchBarDeleteSuggestion,
                        searchBarOverflowIcon = searchBarControllerState.overflowIcon,
                        onSearchBarQueryChange = viewModel::onSearchBarQueryChange,
                        onBackClick = { pane1NavController.navigateUp() },
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
                            doSearch(viewModel, searchBarControllerState, search)
                        },
                        onSearchBarSuggestionClick = {
                            doSearch(viewModel, searchBarControllerState, it.value)
                        },
                        onSearchBarSuggestionInsert = { viewModel.setSearchBarSearch(it.value) },
                        onSearchBarSuggestionDeleteRequest = {
                            viewModel.setShowSearchBarSuggestionDeleteRequest(it.value)
                        },
                        onSearchBarActiveChange = { active ->
                            viewModel.setSearchBarActive(active)
                            viewModel.setShowSearchBarFilters(false)
                            if (!isTwoPaneMode) {
                                systemBarsController.update {
                                    it.copy(
                                        statusBarColor = if (active) statusBarSemiTransparentColor else Color.Unspecified,
                                    )
                                }
                                bottomBarController.update { it.copy(visible = !active) }
                            }
                            searchBarControllerState.onActiveChange(active)
                        },
                        onSearchBarShowFiltersChange = viewModel::setShowSearchBarFilters,
                        onSearchBarFiltersChange = {
                            viewModel.setSearchBarSearch(
                                uiState.searchBarSearch.copy(filters = it)
                            )
                        },
                        onDeleteSearchBarSuggestionConfirmClick = {
                            uiState.searchBarDeleteSuggestion?.run { viewModel.deleteSearch(this) }
                        },
                        onDeleteSearchBarSuggestionDismissRequest = {
                            viewModel.setShowSearchBarSuggestionDeleteRequest(null)
                        },
                        onFixWallHavenApiKeyClick = {
                            pane1NavController.navigate(WallhavenApiKeyDialogDestination)
                        },
                        onDismissGlobalError = viewModel::dismissGlobalError,
                        onBottomBarSizeChanged = { size ->
                            bottomBarController.update { it.copy(size = size) }
                        },
                        onBottomBarItemClick = {
                            pane1NavController.navigate(it.route) {
                                launchSingleTop = true
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
                            twoPaneController = twoPaneController,
                            contentPadding = it,
                            mainActivityViewModel = viewModel,
                            wallpaperViewModel = wallpaperViewModel,
                            applyContentPadding = uiState.applyScaffoldPadding,
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
                                doSearch(viewModel, searchBarControllerState, it.search)
                                viewModel.showSavedSearches(false)
                                viewModel.setSearchBarActive(false)
                            },
                            onDismissRequest = { viewModel.showSavedSearches(false) }
                        )
                    }
                }
            }
        }
    }

    private fun doSearch(
        viewModel: MainActivityViewModel,
        searchBarControllerState: MainSearchBarState,
        search: Search,
    ) {
        viewModel.onSearch(search)
        searchBarControllerState.onSearch(search)
    }
}
