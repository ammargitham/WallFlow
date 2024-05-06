package com.ammar.wallflow.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.R
import com.ammar.wallflow.destinations.WallpaperScreenDestination
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.extensions.toPxF
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.SearchSaver
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenTagSearchMeta
import com.ammar.wallflow.model.search.WallhavenUploaderSearchMeta
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.navigation.AppNavGraphs
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.SearchBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBar
import com.ammar.wallflow.ui.common.rememberAdaptiveBottomSheetState
import com.ammar.wallflow.ui.common.searchedit.EditSearchModalBottomSheet
import com.ammar.wallflow.ui.common.searchedit.SaveAsDialog
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.screens.home.composables.FiltersBottomSheetHeader
import com.ammar.wallflow.ui.screens.home.composables.ManageSourcesDialog
import com.ammar.wallflow.ui.screens.home.composables.RedditInitDialog
import com.ammar.wallflow.ui.screens.home.composables.header
import com.ammar.wallflow.ui.screens.home.composables.wallhavenHeader
import com.ammar.wallflow.ui.screens.main.RootNavControllerWrapper
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ammar.wallflow.utils.applyWallpaper
import com.ammar.wallflow.utils.getStartBottomPadding
import com.ammar.wallflow.utils.shareWallpaper
import com.ammar.wallflow.utils.shareWallpaperUrl
import com.ramcosta.composedestinations.annotation.Destination
import kotlin.math.roundToInt
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<AppNavGraphs.HomeNavGraph>(
    start = true,
    navArgs = HomeScreenNavArgs::class,
)
@Composable
fun HomeScreen(
    navController: NavController,
    rootNavControllerWrapper: RootNavControllerWrapper,
) {
    val rootNavController = rootNavControllerWrapper.navController
    val viewModel: HomeViewModel = hiltViewModel()
    val viewerViewModel: WallpaperViewerViewModel = hiltViewModel()
    val searchBarViewModel: SearchBarViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val searchBarUiState by searchBarViewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    val refreshState = rememberPullToRefreshState()
    val showRefreshingIndicator = wallpapers.loadState.refresh == LoadState.Loading &&
        wallpapers.itemCount > 0
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
    val bottomBarState by bottomBarController.state

    val searchBarQuery by remember {
        derivedStateOf {
            when (searchBarUiState.search.meta) {
                is WallhavenTagSearchMeta, is WallhavenUploaderSearchMeta -> {
                    if (searchBarUiState.active) {
                        searchBarUiState.search.query
                    } else {
                        ""
                    }
                }
                else -> searchBarUiState.search.query
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
        searchBarViewModel.setSearch(search)
        searchBarViewModel.setSource(uiState.selectedSource)
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
                rootNavController.navigate(
                    WallpaperScreenDestination(
                        source = it.source,
                        wallpaperId = it.id,
                        thumbData = it.thumbData,
                    ).route,
                )
            }
        }
    }

    val onTagClick: (wallhavenTag: WallhavenTag) -> Unit = remember(
        searchBarUiState.search,
        uiState.prevMainWallhavenSearch,
    ) {
        fn@{
            val prevSearch = uiState.prevMainWallhavenSearch
                ?: MainSearchBar.Defaults.wallhavenSearch
            val search = prevSearch.copy(
                query = "id:${it.id}",
                meta = WallhavenTagSearchMeta(it),
            )
            if (searchBarUiState.search == search) {
                return@fn
            }
            navController.search(search)
        }
    }

    val onUploaderClick: (WallhavenUploader) -> Unit = remember(
        searchBarUiState.search,
        uiState.prevMainWallhavenSearch,
    ) {
        fn@{
            val prevSearch = uiState.prevMainWallhavenSearch
                ?: MainSearchBar.Defaults.wallhavenSearch
            val search = prevSearch.copy(
                query = "@${it.username}",
                meta = WallhavenUploaderSearchMeta(uploader = it),
            )
            if (searchBarUiState.search == search) {
                return@fn
            }
            navController.search(search)
        }
    }

    val onFilterFABClick = remember { { viewModel.showFilters(true) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("Home Screen"),
    ) {
        HomeScreenContent(
            modifier = Modifier.fillMaxSize(),
            nestedScrollConnectionGetter = { nestedScrollConnection },
            isExpanded = systemState.isExpanded,
            isMedium = systemState.isMedium,
            contentPadding = PaddingValues(
                start = if (systemState.isExpanded) 0.dp else 8.dp,
                end = if (systemState.isExpanded) 0.dp else 8.dp,
                top = SearchBar.Defaults.height,
                bottom = bottomPadding + 8.dp,
            ),
            wallpapers = wallpapers,
            searchBar = {
                HomeSearch(
                    modifier = Modifier.offset {
                        IntOffset(x = 0, y = searchBarOffsetHeightPx.roundToInt())
                    },
                    active = searchBarUiState.active,
                    useDocked = systemState.isExpanded || bottomBarState.isRail,
                    useFullWidth = systemState.isExpanded,
                    search = searchBarUiState.search,
                    query = searchBarQuery,
                    suggestions = searchBarUiState.suggestions,
                    showQuery = if (uiState.isHome) {
                        searchBarUiState.active
                    } else {
                        true
                    },
                    onQueryChange = searchBarViewModel::setQuery,
                    onBackClick = if (!uiState.isHome) {
                        { navController.navigateUp() }
                    } else {
                        null
                    },
                    onSearch = {
                        doSearch(
                            mainSearch = uiState.mainSearch,
                            search = it,
                            searchBarViewModel = searchBarViewModel,
                            navController = navController,
                        )
                    },
                    onSearchChange = searchBarViewModel::setSearch,
                    onSearchDeleteRequest = searchBarViewModel::setSearchToDelete,
                    onActiveChange = { active ->
                        searchBarViewModel.setActive(active)
                        if (systemState.isExpanded || bottomBarState.isRail) {
                            return@HomeSearch
                        }
                        systemController.update {
                            it.copy(
                                statusBarColor = if (active) {
                                    Color.Transparent
                                } else {
                                    Color.Unspecified
                                },
                            )
                        }
                        bottomBarController.update {
                            it.copy(visible = !active)
                        }
                    },
                    onSaveAsClick = {
                        val searchBarSearch = searchBarUiState.search
                        val query = searchBarSearch.query
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
                    onLoadClick = {
                        viewModel.showSavedSearches(
                            show = true,
                            isFromSearchBar = true,
                        )
                    },
                )
            },
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
            refreshState = refreshState,
            refreshIndicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = SearchBar.Defaults.height - 8.dp),
                    state = refreshState,
                    isRefreshing = showRefreshingIndicator,
                )
            },
            showFAB = !searchBarUiState.active,
            favorites = uiState.favorites,
            viewedList = uiState.viewedList,
            viewedWallpapersLook = uiState.viewedWallpapersLook,
            lightDarkList = uiState.lightDarkList,
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
            onRefresh = {
                wallpapers.refresh()
                viewModel.refresh()
            },
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
                    rootNavController.navigate(
                        WallpaperScreenDestination(
                            source = source,
                            wallpaperId = id,
                            thumbData = thumbData,
                        ).route,
                    )
                }
            },
            onFullWallpaperUploaderClick = onUploaderClick,
            onFullWallpaperDownloadPermissionsGranted = viewerViewModel::download,
            onFullWallpaperLightDarkTypeFlagsChange = viewerViewModel::updateLightDarkTypeFlags,
        )
    }

    if (uiState.showFilters) {
        val state = rememberAdaptiveBottomSheetState(
            bottomSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = uiState.selectedSource == OnlineSource.REDDIT,
            ),
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
                if (uiState.showSavedSearchesForSearchBar) {
                    doSearch(
                        mainSearch = uiState.mainSearch,
                        navController = navController,
                        searchBarViewModel = searchBarViewModel,
                        search = it.search,
                    )
                    viewModel.showSavedSearches(false)
                    searchBarViewModel.setActive(false)
                } else {
                    viewModel.updateHomeSearch(it.search)
                    viewModel.showSavedSearches(false)
                }
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

    searchBarUiState.searchToDelete?.run {
        AlertDialog(
            title = { Text(text = this.query) },
            text = { Text(text = stringResource(R.string.delete_suggestion_dialog_text)) },
            confirmButton = {
                TextButton(onClick = searchBarViewModel::onConfirmDeleteSearch) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = searchBarViewModel::onCancelDeleteSearch) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            onDismissRequest = searchBarViewModel::onCancelDeleteSearch,
        )
    }
}

private fun doSearch(
    mainSearch: Search?,
    search: Search,
    searchBarViewModel: SearchBarViewModel,
    navController: NavController,
) {
    if (mainSearch == search) {
        return
    }
    searchBarViewModel.onSearch(search)
    navController.search(search)
}
