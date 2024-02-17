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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.SearchSaver
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenTagSearchMeta
import com.ammar.wallflow.model.search.WallhavenUploaderSearchMeta
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
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
import com.ammar.wallflow.ui.screens.home.composables.FiltersBottomSheetHeader
import com.ammar.wallflow.ui.screens.home.composables.ManageSourcesDialog
import com.ammar.wallflow.ui.screens.home.composables.RedditInitDialog
import com.ammar.wallflow.ui.screens.home.composables.header
import com.ammar.wallflow.ui.screens.home.composables.wallhavenHeader
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ammar.wallflow.utils.applyWallpaper
import com.ammar.wallflow.utils.getStartBottomPadding
import com.ammar.wallflow.utils.shareWallpaper
import com.ammar.wallflow.utils.shareWallpaperUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.collections.immutable.toImmutableList
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
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val viewerViewModel: WallpaperViewerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    // TODO: Replace with M3 PullToRefresh
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
    val bottomPadding = getStartBottomPadding(
        density,
        bottomBarController,
        bottomWindowInsets,
        navigationBarsInsets,
    )
    val systemState by systemController.state
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        systemController.resetBarsState()
        bottomBarController.update { it.copy(visible = true) }
    }

    LaunchedEffect(uiState.mainSearch, uiState.selectedSource) {
        val search = uiState.mainSearch
            ?: when (uiState.selectedSource) {
                OnlineSource.WALLHAVEN -> uiState.prevMainWallhavenSearch?.copy(
                    query = "",
                    meta = null,
                ) ?: MainSearchBar.Defaults.wallhavenSearch
                OnlineSource.REDDIT -> uiState.prevMainRedditSearch?.copy(
                    query = "",
                    meta = null,
                ) ?: MainSearchBar.Defaults.redditSearch(
                    subreddits = uiState.reddit.subreddits,
                )
            }
        searchBarController.update {
            it.copy(
                visible = true,
                search = search,
                source = uiState.selectedSource,
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
        uiState.prevMainWallhavenSearch,
    ) {
        fn@{
            val prevSearch = uiState.prevMainWallhavenSearch
                ?: MainSearchBar.Defaults.wallhavenSearch
            val search = prevSearch.copy(
                query = "id:${it.id}",
                meta = WallhavenTagSearchMeta(it),
            )
            if (searchBarController.state.value.search == search) {
                return@fn
            }
            navController.search(search)
        }
    }

    val onUploaderClick: (WallhavenUploader) -> Unit = remember(
        searchBarController.state.value.search,
        uiState.prevMainWallhavenSearch,
    ) {
        fn@{
            val prevSearch = uiState.prevMainWallhavenSearch
                ?: MainSearchBar.Defaults.wallhavenSearch
            val search = prevSearch.copy(
                query = "@${it.username}",
                meta = WallhavenUploaderSearchMeta(uploader = it),
            )
            if (searchBarController.state.value.search == search) {
                return@fn
            }
            navController.search(search)
        }
    }

    val onFilterFABClick = remember { { viewModel.showFilters(true) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets)
            .pullRefresh(state = refreshState),
    ) {
        HomeScreenContent(
            modifier = Modifier.fillMaxSize(),
            nestedScrollConnectionGetter = nestedScrollConnectionGetter,
            isExpanded = systemState.isExpanded,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = SearchBar.Defaults.height,
                bottom = bottomPadding + 8.dp,
            ),
            wallpapers = wallpapers,
            header = if (uiState.isHome) {
                {
                    header(
                        sources = uiState.sources
                            .filter { it.value }
                            .keys
                            .toImmutableList(),
                        selectedSource = uiState.selectedSource,
                        sourceHeader = when (uiState.selectedSource) {
                            OnlineSource.WALLHAVEN -> {
                                {
                                    wallhavenHeader(
                                        wallhavenTags = uiState.wallhaven.wallhavenTags,
                                        isTagsLoading = uiState.wallhaven.areTagsLoading,
                                        onTagClick = onTagClick,
                                        onTagLongClick = {
                                            clipboardManager.setText(
                                                AnnotatedString("#${it.name}"),
                                            )
                                            context.toast(context.getString(R.string.tag_copied))
                                        },
                                    )
                                }
                            }
                            OnlineSource.REDDIT -> null
                        },
                        onSourceClick = viewModel::changeSource,
                        onManageSourcesClick = {
                            viewModel.showManageSourcesDialog(true)
                        },
                    )
                }
            } else {
                null
            },
            favorites = uiState.favorites,
            viewedList = uiState.viewedList,
            viewedWallpapersLook = uiState.viewedWallpapersLook,
            blurSketchy = uiState.blurSketchy,
            blurNsfw = uiState.blurNsfw,
            selectedWallpaper = uiState.selectedWallpaper,
            layoutPreferences = uiState.layoutPreferences,
            fullWallpaper = viewerUiState.wallpaper,
            fullWallpaperActionsVisible = viewerUiState.actionsVisible,
            fullWallpaperDownloadStatus = viewerUiState.downloadStatus,
            fullWallpaperLoading = viewerUiState.loading,
            showFullWallpaperInfo = viewerUiState.showInfo,
            isFullWallpaperFavorite = viewerUiState.isFavorite,
            isHome = uiState.isHome,
            fullWallpaperLightDarkTypeFlags = viewerUiState.lightDarkTypeFlags,
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
            onFullWallpaperUploaderClick = onUploaderClick,
            onFullWallpaperDownloadPermissionsGranted = viewerViewModel::download,
        )

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = false,
            state = refreshState,
        )
    }

    if (uiState.showFilters) {
        val state = rememberModalBottomSheetState(
            skipPartiallyExpanded = uiState.selectedSource == OnlineSource.REDDIT,
        )
        val scope = rememberCoroutineScope()
        val initialSearch = if (uiState.isHome) {
            uiState.homeSearch
        } else {
            uiState.mainSearch ?: when (uiState.selectedSource) {
                OnlineSource.WALLHAVEN -> MainSearchBar.Defaults.wallhavenSearch
                OnlineSource.REDDIT -> MainSearchBar.Defaults.redditSearch(
                    subreddits = uiState.reddit.subreddits,
                )
            }
        }
        var localSearch by rememberSaveable(initialSearch, stateSaver = SearchSaver) {
            mutableStateOf(initialSearch)
        }
        var hasError by rememberSaveable {
            mutableStateOf(false)
        }

        val testTag = "home:${
            if (uiState.isHome) {
                "home"
            } else {
                "search"
            }
        }-${
            when (localSearch) {
                is RedditSearch -> "reddit"
                is WallhavenSearch -> "wallhaven"
            }
        }-filters"

        EditSearchModalBottomSheet(
            modifier = Modifier
                .semantics { contentDescription = testTag }
                .testTag(testTag),
            state = state,
            search = localSearch,
            header = {
                FiltersBottomSheetHeader(
                    modifier = Modifier.padding(
                        start = 22.dp,
                        end = 22.dp,
                        bottom = 16.dp,
                    ),
                    title = stringResource(
                        if (uiState.isHome) {
                            R.string.home_filters
                        } else {
                            R.string.search_filters
                        },
                    ),
                    source = when (localSearch) {
                        is WallhavenSearch -> OnlineSource.WALLHAVEN
                        is RedditSearch -> OnlineSource.REDDIT
                    },
                    saveEnabled = !hasError && localSearch != initialSearch,
                    onSaveClick = {
                        scope.launch { state.hide() }.invokeOnCompletion {
                            if (!state.isVisible) {
                                viewModel.showFilters(false)
                                if (uiState.isHome) {
                                    viewModel.updateHomeSearch(localSearch)
                                } else {
                                    navController.search(localSearch)
                                }
                            }
                        }
                    },
                    onSaveAsClick = { viewModel.showSaveSearchAsDialog(localSearch) },
                    onLoadClick = viewModel::showSavedSearches,
                )
            },
            showQueryField = uiState.isHome,
            showNSFW = uiState.showNSFW,
            onChange = { localSearch = it },
            onErrorStateChange = { hasError = it },
            onDismissRequest = { viewModel.showFilters(false) },
        )
    }

    if (uiState.showSaveAsDialog) {
        SaveAsDialog(
            checkNameExists = viewModel::checkSavedSearchNameExists,
            onSave = {
                val search = uiState.saveSearchAsSearch ?: return@SaveAsDialog
                viewModel.saveSearchAs(it, search)
                viewModel.showSaveSearchAsDialog(null)
            },
            onDismissRequest = { viewModel.showSaveSearchAsDialog(null) },
        )
    }

    if (uiState.showSavedSearchesDialog) {
        SavedSearchesDialog(
            savedSearches = uiState.savedSearches.filter {
                when (uiState.selectedSource) {
                    OnlineSource.WALLHAVEN -> it.search is WallhavenSearch
                    OnlineSource.REDDIT -> it.search is RedditSearch
                }
            },
            onSelect = {
                viewModel.updateHomeSearch(it.search)
                viewModel.showSavedSearches(false)
            },
            onDismissRequest = { viewModel.showSavedSearches(false) },
        )
    }

    if (uiState.manageSourcesState.showDialog) {
        ManageSourcesDialog(
            currentSources = uiState.manageSourcesState.currentSources,
            saveEnabled = uiState.manageSourcesState.saveEnabled,
            onAddSourceClick = viewModel::addSource,
            onVisibilityChange = { source, visible ->
                val updated = uiState.manageSourcesState.currentSources.toMutableMap()
                updated[source] = visible
                viewModel.updateManageSourcesDialogSources(updated)
            },
            onSaveClick = viewModel::saveManageSources,
            onDismissRequest = { viewModel.showManageSourcesDialog(false) },
        )
    }

    if (uiState.showRedditInitDialog) {
        RedditInitDialog(
            onSaveClick = { viewModel.updateRedditConfigAndCloseDialog(it) },
            onDismissRequest = { viewModel.showRedditInitDialog(false) },
        )
    }
}
