package com.ammar.wallflow.ui.screens.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.WallhavenSearch
import com.ammar.wallflow.model.WallhavenTagSearchMeta
import com.ammar.wallflow.model.WallhavenUploaderSearchMeta
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.ui.common.BottomBarAwareHorizontalTwoPane
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.screens.destinations.WallpaperScreenDestination
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ammar.wallflow.utils.DownloadStatus
import com.ammar.wallflow.utils.applyWallpaper
import com.ammar.wallflow.utils.shareWallpaper
import com.ammar.wallflow.utils.shareWallpaperUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Destination
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
    viewerViewModel: WallpaperViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.favoriteWallpapers.collectAsLazyPagingItems()
    val gridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val systemController = LocalSystemController.current
    val bottomBarController = LocalBottomBarController.current
    val searchBarController = LocalMainSearchBarController.current
    val systemState by systemController.state

    LaunchedEffect(Unit) {
        systemController.resetBarsState()
        bottomBarController.update { it.copy(visible = true) }
        searchBarController.update { it.copy(visible = false) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets),
    ) {
        FavoritesScreenContent(
            modifier = Modifier.fillMaxSize(),
            isExpanded = systemState.isExpanded,
            gridState = gridState,
            wallpapers = wallpapers,
            favorites = uiState.favorites,
            blurSketchy = uiState.blurSketchy,
            blurNsfw = uiState.blurNsfw,
            selectedWallpaper = uiState.selectedWallpaper,
            showSelection = systemState.isExpanded,
            layoutPreferences = uiState.layoutPreferences,
            fullWallpaper = viewerUiState.wallpaper,
            fullWallpaperActionsVisible = viewerUiState.actionsVisible,
            fullWallpaperDownloadStatus = viewerUiState.downloadStatus,
            fullWallpaperLoading = viewerUiState.loading,
            showFullWallpaperInfo = viewerUiState.showInfo,
            onWallpaperClick = onWallpaperClick,
            onWallpaperFavoriteClick = viewModel::toggleFavorite,
            onFullWallpaperTransform = viewerViewModel::onWallpaperTransform,
            onFullWallpaperTap = viewerViewModel::onWallpaperTap,
            onFullWallpaperInfoClick = viewerViewModel::showInfo,
            onFullWallpaperInfoDismiss = { viewerViewModel.showInfo(false) },
            onFullWallpaperShareLinkClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@FavoritesScreenContent
                shareWallpaperUrl(context, wallpaper)
            },
            onFullWallpaperShareImageClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@FavoritesScreenContent
                shareWallpaper(context, viewerViewModel, wallpaper)
            },
            onFullWallpaperApplyWallpaperClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@FavoritesScreenContent
                applyWallpaper(context, viewerViewModel, wallpaper)
            },
            onFullWallpaperFullScreenClick = {
                viewerUiState.wallpaper?.run {
                    navController.navigate(
                        WallpaperScreenDestination(
                            source = source,
                            thumbData = thumbData,
                            wallpaperId = id,
                        ),
                    )
                }
            },
            onFullWallpaperTagClick = {
                val search = WallhavenSearch(
                    query = "id:${it.id}",
                    meta = WallhavenTagSearchMeta(it),
                )
                if (searchBarController.state.value.search == search) {
                    return@FavoritesScreenContent
                }
                navController.search(search)
            },
            onFullWallpaperUploaderClick = {
                val search = WallhavenSearch(
                    query = "@${it.username}",
                    meta = WallhavenUploaderSearchMeta(wallhavenUploader = it),
                )
                if (searchBarController.state.value.search == search) {
                    return@FavoritesScreenContent
                }
                navController.search(search)
            },
            onFullWallpaperDownloadPermissionsGranted = viewerViewModel::download,
        )
    }
}

@Composable
private fun FavoritesScreenContent(
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showSelection: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    fullWallpaper: Wallpaper? = null,
    fullWallpaperActionsVisible: Boolean = true,
    fullWallpaperDownloadStatus: DownloadStatus? = null,
    fullWallpaperLoading: Boolean = false,
    showFullWallpaperInfo: Boolean = false,
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onFullWallpaperTransform: () -> Unit = {},
    onFullWallpaperTap: () -> Unit = {},
    onFullWallpaperInfoClick: () -> Unit = {},
    onFullWallpaperInfoDismiss: () -> Unit = {},
    onFullWallpaperShareLinkClick: () -> Unit = {},
    onFullWallpaperShareImageClick: () -> Unit = {},
    onFullWallpaperApplyWallpaperClick: () -> Unit = {},
    onFullWallpaperFullScreenClick: () -> Unit = {},
    onFullWallpaperTagClick: (WallhavenTag) -> Unit = {},
    onFullWallpaperUploaderClick: (WallhavenUploader) -> Unit = {},
    onFullWallpaperDownloadPermissionsGranted: () -> Unit = {},
) {
    if (isExpanded) {
        BottomBarAwareHorizontalTwoPane(
            modifier = modifier,
            first = {
                WallpaperStaggeredGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = contentPadding,
                    wallpapers = wallpapers,
                    favorites = favorites,
                    blurSketchy = blurSketchy,
                    blurNsfw = blurNsfw,
                    selectedWallpaper = selectedWallpaper,
                    showSelection = showSelection,
                    gridType = layoutPreferences.gridType,
                    gridColType = layoutPreferences.gridColType,
                    gridColCount = layoutPreferences.gridColCount,
                    gridColMinWidthPct = layoutPreferences.gridColMinWidthPct,
                    roundedCorners = layoutPreferences.roundedCorners,
                    onWallpaperClick = onWallpaperClick,
                    onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                )
            },
            second = {
                WallpaperViewer(
                    wallpaper = fullWallpaper,
                    actionsVisible = fullWallpaperActionsVisible,
                    downloadStatus = fullWallpaperDownloadStatus,
                    loading = fullWallpaperLoading,
                    thumbData = selectedWallpaper?.thumbData,
                    showFullScreenAction = true,
                    showInfo = showFullWallpaperInfo,
                    onWallpaperTransform = onFullWallpaperTransform,
                    onWallpaperTap = onFullWallpaperTap,
                    onInfoClick = onFullWallpaperInfoClick,
                    onInfoDismiss = onFullWallpaperInfoDismiss,
                    onShareLinkClick = onFullWallpaperShareLinkClick,
                    onShareImageClick = onFullWallpaperShareImageClick,
                    onApplyWallpaperClick = onFullWallpaperApplyWallpaperClick,
                    onFullScreenClick = onFullWallpaperFullScreenClick,
                    onTagClick = onFullWallpaperTagClick,
                    onUploaderClick = onFullWallpaperUploaderClick,
                    onDownloadPermissionsGranted = onFullWallpaperDownloadPermissionsGranted,
                )
            },
        )
    } else {
        WallpaperStaggeredGrid(
            modifier = modifier,
            state = gridState,
            contentPadding = contentPadding,
            wallpapers = wallpapers,
            favorites = favorites,
            blurSketchy = blurSketchy,
            blurNsfw = blurNsfw,
            selectedWallpaper = selectedWallpaper,
            showSelection = showSelection,
            gridType = layoutPreferences.gridType,
            gridColType = layoutPreferences.gridColType,
            gridColCount = layoutPreferences.gridColCount,
            gridColMinWidthPct = layoutPreferences.gridColMinWidthPct,
            roundedCorners = layoutPreferences.roundedCorners,
            onWallpaperClick = onWallpaperClick,
            onWallpaperFavoriteClick = onWallpaperFavoriteClick,
        )
    }
}
