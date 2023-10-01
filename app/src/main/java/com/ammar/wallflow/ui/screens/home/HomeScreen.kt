package com.ammar.wallflow.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.extensions.rememberLazyStaggeredGridState
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.SearchSaver
import com.ammar.wallflow.model.TagSearchMeta
import com.ammar.wallflow.model.UploaderSearchMeta
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.SearchBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBar
import com.ammar.wallflow.ui.common.searchedit.EditSearchModalBottomSheet
import com.ammar.wallflow.ui.common.searchedit.SaveAsDialog
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.screens.destinations.WallpaperScreenDestination
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ammar.wallflow.utils.applyWallpaper
import com.ammar.wallflow.utils.getStartBottomPadding
import com.ammar.wallflow.utils.shareWallpaper
import com.ammar.wallflow.utils.shareWallpaperUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
)
@Destination(
    navArgsDelegate = HomeScreenNavArgs::class,
)
@Composable
fun HomeScreen(
    navController: NavController,
    nestedScrollConnectionGetter: () -> NestedScrollConnection,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    viewerViewModel: WallpaperViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    val gridState = wallpapers.rememberLazyStaggeredGridState()
    val refreshing = wallpapers.loadState.refresh == LoadState.Loading
    val refreshState = rememberPullRefreshState(
        refreshing = false,
        onRefresh = {
            wallpapers.refresh()
            viewModel.refresh()
        },
    )
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val bottomWindowInsets = bottomWindowInsets
    val navigationBarsInsets = WindowInsets.navigationBars
    val bottomPadding = remember(
        bottomBarController.state.value,
        density,
        bottomWindowInsets.getBottom(density),
        navigationBarsInsets.getBottom(density),
    ) {
        getStartBottomPadding(
            density,
            bottomBarController,
            bottomWindowInsets,
            navigationBarsInsets,
        )
    }
    val systemState by systemController.state

    LaunchedEffect(refreshing) {
        viewModel.setWallpapersLoading(refreshing)
    }

    LaunchedEffect(Unit) {
        systemController.resetBarsState()
        bottomBarController.update { it.copy(visible = true) }
    }

    LaunchedEffect(uiState.mainSearch) {
        searchBarController.update {
            it.copy(
                visible = true,
                search = uiState.mainSearch ?: MainSearchBar.Defaults.search,
            )
        }
    }

    val onWallpaperClick: (wallpaper: Wallpaper) -> Unit = remember(systemState.isExpanded) {
        {
            if (systemState.isExpanded) {
                viewModel.setSelectedWallpaper(it)
                viewerViewModel.setWallpaper(
                    source = it.source,
                    wallpaperId = it.id,
                    thumbData = it.thumbData,
                )
            } else {
                // navigate to wallpaper screen
                navController.navigate(
                    WallpaperScreenDestination(
                        source = it.source,
                        wallpaperId = it.id,
                        thumbData = it.thumbData,
                    ),
                )
            }
        }
    }

    val onTagClick: (wallhavenTag: WallhavenTag) -> Unit = remember(
        searchBarController.state.value.search,
    ) {
        fn@{
            val search = Search(
                query = "id:${it.id}",
                meta = TagSearchMeta(it),
            )
            if (searchBarController.state.value.search == search) {
                return@fn
            }
            navController.search(search)
        }
    }

    val onFilterFABClick = remember { { viewModel.showFilters(true) } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets)
            .pullRefresh(state = refreshState),
    ) {
        HomeScreenContent(
            modifier = Modifier.fillMaxSize(),
            nestedScrollConnectionGetter = nestedScrollConnectionGetter,
            isExpanded = systemState.isExpanded,
            gridState = gridState,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = SearchBar.Defaults.height,
                bottom = bottomPadding + 8.dp,
            ),
            wallhavenTags = if (uiState.isHome) uiState.wallhavenTags else persistentListOf(),
            isTagsLoading = uiState.areTagsLoading,
            wallpapers = wallpapers,
            favorites = uiState.favorites,
            blurSketchy = uiState.blurSketchy,
            blurNsfw = uiState.blurNsfw,
            selectedWallpaper = uiState.selectedWallpaper,
            layoutPreferences = uiState.layoutPreferences,
            showFAB = uiState.isHome,
            fullWallpaper = viewerUiState.wallpaper,
            fullWallpaperActionsVisible = viewerUiState.actionsVisible,
            fullWallpaperDownloadStatus = viewerUiState.downloadStatus,
            fullWallpaperLoading = viewerUiState.loading,
            showFullWallpaperInfo = viewerUiState.showInfo,
            onWallpaperClick = onWallpaperClick,
            onWallpaperFavoriteClick = viewModel::toggleFavorite,
            onTagClick = onTagClick,
            onFABClick = onFilterFABClick,
            onFullWallpaperTransform = viewerViewModel::onWallpaperTransform,
            onFullWallpaperTap = viewerViewModel::onWallpaperTap,
            onFullWallpaperInfoClick = viewerViewModel::showInfo,
            onFullWallpaperInfoDismiss = { viewerViewModel.showInfo(false) },
            onFullWallpaperShareLinkClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@HomeScreenContent
                shareWallpaperUrl(context, wallpaper)
            },
            onFullWallpaperShareImageClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@HomeScreenContent
                shareWallpaper(context, viewerViewModel, wallpaper)
            },
            onFullWallpaperApplyWallpaperClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@HomeScreenContent
                applyWallpaper(context, viewerViewModel, wallpaper)
            },
            onFullWallpaperFullScreenClick = {
                viewerUiState.wallpaper?.run {
                    navController.navigate(
                        WallpaperScreenDestination(
                            source = source,
                            wallpaperId = id,
                            thumbData = thumbData,
                        ),
                    )
                }
            },
            onFullWallpaperUploaderClick = {
                val search = Search(
                    query = "@${it.username}",
                    meta = UploaderSearchMeta(wallhavenUploader = it),
                )
                if (searchBarController.state.value.search == search) {
                    return@HomeScreenContent
                }
                navController.search(search)
            },
            onFullWallpaperDownloadPermissionsGranted = viewerViewModel::download,
        )

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = false,
            state = refreshState,
        )

        if (uiState.showFilters) {
            val state = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()
            var localSearch by rememberSaveable(
                uiState.homeSearch,
                stateSaver = SearchSaver,
            ) { mutableStateOf(uiState.homeSearch) }

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
                        saveEnabled = localSearch != uiState.homeSearch,
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
                onDismissRequest = { viewModel.showFilters(false) },
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
                onDismissRequest = { viewModel.showSavedSearches(false) },
            )
        }
    }
}
