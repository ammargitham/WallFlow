package com.ammar.wallflow.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.extensions.findActivity
import com.ammar.wallflow.extensions.rememberLazyStaggeredGridState
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.model.MenuItem
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.SearchSaver
import com.ammar.wallflow.model.Tag
import com.ammar.wallflow.model.TagSearchMeta
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallpaper1
import com.ammar.wallflow.model.wallpaper2
import com.ammar.wallflow.ui.appCurrentDestinationAsState
import com.ammar.wallflow.ui.common.LocalSystemBarsController
import com.ammar.wallflow.ui.common.SearchBar
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.BottomBarController
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarState
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.wallflow.ui.common.searchedit.EditSearchModalBottomSheet
import com.ammar.wallflow.ui.common.searchedit.SaveAsDialog
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.destinations.SettingsScreenDestination
import com.ammar.wallflow.ui.destinations.WallpaperScreenDestination
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.ui.wallpaper.WallpaperScreenNavArgs
import com.ammar.wallflow.ui.wallpaper.WallpaperViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
@RootNavGraph(start = true)
@Destination(
    navArgsDelegate = HomeScreenNavArgs::class,
)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    twoPaneController: TwoPaneNavigation.Controller,
    wallpaperViewModel: WallpaperViewModel,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    val gridState = wallpapers.rememberLazyStaggeredGridState()
    val refreshing = wallpapers.loadState.refresh == LoadState.Loading
    val expandedFab by remember(gridState.firstVisibleItemIndex) {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 }
    }
    val refreshState = rememberPullRefreshState(
        // refreshing = uiState.wallpapersLoading,
        refreshing = false,
        onRefresh = {
            wallpapers.refresh()
            viewModel.refresh()
        },
    )
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemBarsController = LocalSystemBarsController.current
    val density = LocalDensity.current
    val bottomWindowInsets = bottomWindowInsets
    val navigationBarsInsets = WindowInsets.navigationBars
    val (startPadding, bottomPadding) = remember(
        bottomBarController.state,
        density,
        bottomWindowInsets.getBottom(density),
        navigationBarsInsets.getBottom(density),
    ) {
        getStartBottomPadding(
            density,
            bottomBarController,
            bottomWindowInsets,
            navigationBarsInsets
        )
    }
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context.findActivity())
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val currentPane2Destination by twoPaneController.pane2NavHostController.appCurrentDestinationAsState()
    val isTwoPaneMode = twoPaneController.paneMode.value == Mode.TWO_PANE

    LaunchedEffect(refreshing) {
        viewModel.setWallpapersLoading(refreshing)
    }

    LaunchedEffect(Unit) {
        systemBarsController.reset()
        bottomBarController.update { it.copy(visible = true) }
    }

    LaunchedEffect(uiState.search, uiState.isHome) {
        searchBarController.update {
            MainSearchBarState(
                visible = true,
                overflowIcon = if (uiState.isHome) {
                    {
                        SearchBarOverflowMenu(
                            items = listOf(
                                MenuItem(
                                    text = stringResource(R.string.settings),
                                    value = "settings",
                                )
                            ),
                            onItemClick = {
                                if (it.value == "settings") {
                                    twoPaneController.navigate(SettingsScreenDestination) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                } else null,
                search = uiState.search,
                onSearch = {
                    if (uiState.search == it) return@MainSearchBarState
                    twoPaneController.pane1NavHostController.search(it)
                }
            )
        }
    }

    LaunchedEffect(uiState.selectedWallpaper) {
        val navArgs = WallpaperScreenNavArgs(
            wallpaperId = uiState.selectedWallpaper?.id,
            thumbUrl = uiState.selectedWallpaper?.thumbs?.original,
        )
        if (!isExpanded) {
            return@LaunchedEffect
        }
        twoPaneController.setPaneMode(Mode.TWO_PANE)
        if (currentPane2Destination is WallpaperScreenDestination) {
            wallpaperViewModel.setWallpaperId(
                wallpaperId = navArgs.wallpaperId,
                thumbUrl = navArgs.thumbUrl,
            )
            return@LaunchedEffect
        }
        twoPaneController.navigatePane2(
            WallpaperScreenDestination(navArgs)
        )
    }

    val onWallpaperClick: (wallpaper: Wallpaper) -> Unit = remember(isTwoPaneMode) {
        {
            if (isTwoPaneMode) {
                viewModel.setSelectedWallpaper(it)

            } else {
                // navigate to wallpaper screen
                twoPaneController.navigatePane1(
                    WallpaperScreenDestination(
                        wallpaperId = it.id,
                        thumbUrl = it.thumbs.original,
                    )
                )
            }
        }
    }

    val onTagClick: (tag: Tag) -> Unit = remember {
        {
            twoPaneController.pane1NavHostController.search(
                Search(
                    query = "id:${it.id}",
                    meta = TagSearchMeta(it),
                )
            )
        }
    }

    val onFilterFABClick = remember { { viewModel.showFilters(true) } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets)
            .padding(top = SearchBar.Defaults.height)
            .pullRefresh(state = refreshState),
    ) {
        HomeScreenContent(
            modifier = Modifier.fillMaxSize(),
            gridState = gridState,
            contentPadding = PaddingValues(
                start = startPadding + 8.dp,
                end = 8.dp,
                bottom = bottomPadding + 8.dp,
            ),
            tags = if (uiState.isHome) uiState.tags else persistentListOf(),
            isTagsLoading = uiState.areTagsLoading,
            wallpapers = wallpapers,
            blurSketchy = uiState.blurSketchy,
            blurNsfw = uiState.blurNsfw,
            selectedWallpaper = uiState.selectedWallpaper,
            showSelection = isTwoPaneMode,
            layoutPreferences = uiState.layoutPreferences,
            onWallpaperClick = onWallpaperClick,
            onTagClick = onTagClick
        )

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            // refreshing = uiState.wallpapersLoading,
            refreshing = false,
            state = refreshState,
        )

        if (uiState.isHome) {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-16).dp - bottomPadding),
                onClick = onFilterFABClick,
                expanded = expandedFab,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_filter_alt_24),
                        contentDescription = stringResource(R.string.filters),
                    )
                },
                text = { Text(text = stringResource(R.string.filters)) },
            )
        }

        if (uiState.showFilters) {
            val state = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()
            var localSearch by rememberSaveable(
                uiState.search,
                stateSaver = SearchSaver,
            ) { mutableStateOf(uiState.search) }

            EditSearchModalBottomSheet(
                state = state,
                search = localSearch,
                header = {
                    HomeFiltersBottomSheetHeader(
                        modifier = Modifier.padding(
                            start = 22.dp,
                            end = 22.dp,
                            bottom = 16.dp,
                        ),
                        saveEnabled = localSearch != uiState.search,
                        onSaveClick = {
                            viewModel.updateHomeSearch(localSearch)
                            scope.launch { state.hide() }.invokeOnCompletion {
                                if (!state.isVisible) {
                                    viewModel.showFilters(false)
                                }
                            }
                        },
                        onSaveAsClick = { viewModel.showSaveSearchAsDialog(localSearch) },
                        onLoadClick = viewModel::showSavedSearches,
                    )
                },
                showNSFW = uiState.showNSFW,
                onChange = { localSearch = it },
                onDismissRequest = { viewModel.showFilters(false) }
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
                    viewModel.updateHomeSearch(it.search)
                    viewModel.showSavedSearches(false)
                },
                onDismissRequest = { viewModel.showSavedSearches(false) }
            )
        }
    }
}

private fun getStartBottomPadding(
    density: Density,
    bottomBarController: BottomBarController,
    bottomWindowInsets: WindowInsets,
    navigationBarsInsets: WindowInsets,
): Pair<Dp, Dp> = with(density) {
    val bottomBarState by bottomBarController.state
    val startPadding = if (bottomBarState.isRail) bottomBarState.size.width.toDp() else 0.dp
    val bottomInsetsPadding = if (bottomBarState.isRail) {
        bottomWindowInsets.getBottom(density).toDp()
    } else 0.dp
    val bottomNavPadding = if (bottomBarState.isRail) 0.dp else {
        navigationBarsInsets.getBottom(density).toDp()
    }
    val bottomBarPadding = if (bottomBarState.isRail) 0.dp else {
        bottomBarState.size.height.toDp()
    }
    val bottomPadding = bottomInsetsPadding + bottomBarPadding + bottomNavPadding
    Pair(startPadding, bottomPadding)
}

@Composable
internal fun HomeScreenContent(
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    tags: ImmutableList<Tag> = persistentListOf(),
    isTagsLoading: Boolean = false,
    wallpapers: LazyPagingItems<Wallpaper>,
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showSelection: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onTagClick: (tag: Tag) -> Unit = {},
) {
    WallpaperStaggeredGrid(
        modifier = modifier,
        state = gridState,
        contentPadding = contentPadding,
        wallpapers = wallpapers,
        blurSketchy = blurSketchy,
        blurNsfw = blurNsfw,
        header = {
            if (tags.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    PopularTagsRow(
                        tags = tags,
                        loading = isTagsLoading,
                        onTagClick = onTagClick,
                    )
                }
            }
        },
        selectedWallpaper = selectedWallpaper,
        showSelection = showSelection,
        gridType = layoutPreferences.gridType,
        gridColType = layoutPreferences.gridColType,
        gridColCount = layoutPreferences.gridColCount,
        gridColMinWidthPct = layoutPreferences.gridColMinWidthPct,
        roundedCorners = layoutPreferences.roundedCorners,
        onWallpaperClick = onWallpaperClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    WallFlowTheme {
        val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
        val pagingItems = wallpapers.collectAsLazyPagingItems()
        HomeScreenContent(tags = persistentListOf(), wallpapers = pagingItems)
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun PortraitPreview() {
    WallFlowTheme {
        val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
        val pagingItems = wallpapers.collectAsLazyPagingItems()
        HomeScreenContent(tags = persistentListOf(), wallpapers = pagingItems)
    }
}
