package com.ammar.havenwalls.activities.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import com.ammar.havenwalls.extensions.trimAll
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.TagSearchMeta
import com.ammar.havenwalls.model.UploaderSearchMeta
import com.ammar.havenwalls.ui.NavGraphs
import com.ammar.havenwalls.ui.appCurrentDestinationAsState
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.common.bottombar.BottomBarDestination
import com.ammar.havenwalls.ui.common.bottombar.BottomBarState
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.destinations.WallhavenApiKeyDialogDestination
import com.ammar.havenwalls.ui.startAppDestination
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.utils.startDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberAnimatedNavController()
            // val topAppBarState = rememberTopAppBarState()
            // val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            //     topAppBarState,
            // )
            // val fabController = rememberFABController()
            // val currentBackStackEntry by navController.currentBackStackEntryAsState()
            // LaunchedEffect(currentBackStackEntry) {
            //     // reveal TopAppBar on changing screens
            //     scrollBehavior.state.heightOffset = 0f
            // }
            val viewModel: MainActivityViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentDestination = navController.appCurrentDestinationAsState().value
                ?: NavGraphs.root.startAppDestination
            val rootDestinations = remember {
                BottomBarDestination.values().map { it.direction.startDestination }
            }
            val showBackButton = remember(currentDestination, rootDestinations) {
                currentDestination !in rootDestinations
            }
            val systemBarsController = LocalSystemBarsController.current
            val systemBarsState by systemBarsController.state
            val bottomBarController = LocalBottomBarController.current
            val searchBarController = LocalMainSearchBarController.current
            val searchBarControllerState by searchBarController.state

            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact

            val doSearch = remember {
                fun(s: Search) {
                    viewModel.onSearch(s)
                    searchBarControllerState.onSearch(s)
                }
            }
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
                            doSearch(search)
                        },
                        onSearchBarSuggestionClick = { doSearch(it.value) },
                        onSearchBarSuggestionInsert = { viewModel.setSearchBarSearch(it.value) },
                        onSearchBarSuggestionDeleteRequest = {
                            viewModel.setShowSearchBarSuggestionDeleteRequest(it.value)
                        },
                        onSearchBarActiveChange = { active ->
                            viewModel.setSearchBarActive(active)
                            viewModel.setShowSearchBarFilters(false)
                            systemBarsController.update {
                                it.copy(
                                    statusBarColor = if (active) statusBarSemiTransparentColor else Color.Unspecified,
                                )
                            }
                            bottomBarController.update { BottomBarState(visible = !active) }
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
                            navController.navigate(WallhavenApiKeyDialogDestination)
                        },
                        onDismissGlobalError = viewModel::dismissGlobalError,
                        onBottomBarSizeChanged = { size ->
                            bottomBarController.update { it.copy(size = size) }
                        },
                        onBottomBarItemClick = {
                            navController.navigate(it) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        MainNavigation(
                            navController = navController,
                            contentPadding = it,
                            mainActivityViewModel = viewModel,
                            applyContentPadding = uiState.applyScaffoldPadding,
                        )
                    }
                }
            }
        }
    }
}
