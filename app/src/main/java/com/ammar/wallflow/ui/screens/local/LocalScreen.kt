package com.ammar.wallflow.ui.screens.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.destinations.WallpaperScreenDestination
import com.ammar.wallflow.extensions.rememberLazyStaggeredGridState
import com.ammar.wallflow.extensions.safeLaunch
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.navigation.AppNavGraphs.LocalNavGraph
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.screens.main.RootNavControllerWrapper
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ammar.wallflow.utils.applyWallpaper
import com.ammar.wallflow.utils.getStartBottomPadding
import com.ammar.wallflow.utils.shareWallpaper
import com.ramcosta.composedestinations.annotation.Destination

@Destination<LocalNavGraph>(
    start = true,
)
@Composable
fun LocalScreen(
    rootNavControllerWrapper: RootNavControllerWrapper,
    viewModel: LocalScreenViewModel = hiltViewModel(),
    viewerViewModel: WallpaperViewerViewModel = hiltViewModel(),
) {
    val rootNavController = rootNavControllerWrapper.navController
    val uiState by viewModel.uiState.collectAsState()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    val systemController = LocalSystemController.current
    val bottomBarController = LocalBottomBarController.current
    val bottomWindowInsets = bottomWindowInsets
    val gridState = wallpapers.rememberLazyStaggeredGridState()
    val navigationBarsInsets = WindowInsets.navigationBars
    val density = LocalDensity.current
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
    val context = LocalContext.current
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) {
        if (it == null) {
            return@rememberLauncherForActivityResult
        }
        viewModel.addLocalDir(it)
    }

    LaunchedEffect(Unit) {
        systemController.resetBarsState()
        bottomBarController.update { it.copy(visible = true) }
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

    val onAddFolderClick: () -> Unit = remember(openDocumentTreeLauncher) {
        {
            openDocumentTreeLauncher.safeLaunch(context, null)
        }
    }

    LocalScreenContent(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets),
        wallpapers = wallpapers,
        folders = uiState.folders,
        isExpanded = systemState.isExpanded,
        contentPadding = PaddingValues(
            start = if (systemState.isExpanded) 0.dp else 8.dp,
            end = if (systemState.isExpanded) 0.dp else 8.dp,
            top = 8.dp,
            bottom = bottomPadding + 8.dp,
        ),
        gridState = gridState,
        favorites = uiState.favorites,
        viewedList = uiState.viewedList,
        viewedWallpapersLook = uiState.viewedWallpapersLook,
        lightDarkList = uiState.lightDarkList,
        selectedWallpaper = uiState.selectedWallpaper,
        layoutPreferences = uiState.layoutPreferences,
        fullWallpaper = viewerUiState.wallpaper,
        fullWallpaperActionsVisible = viewerUiState.actionsVisible,
        fullWallpaperLoading = viewerUiState.loading,
        showFullWallpaperInfo = viewerUiState.showInfo,
        isFullWallpaperFavorite = viewerUiState.isFavorite,
        onWallpaperClick = onWallpaperClick,
        onWallpaperFavoriteClick = viewModel::toggleFavorite,
        onFullWallpaperTransform = viewerViewModel::onWallpaperTransform,
        onFullWallpaperTap = viewerViewModel::onWallpaperTap,
        onFullWallpaperInfoClick = viewerViewModel::showInfo,
        onFullWallpaperInfoDismiss = { viewerViewModel.showInfo(false) },
        onFullWallpaperShareImageClick = {
            val wallpaper = viewerUiState.wallpaper ?: return@LocalScreenContent
            shareWallpaper(context, viewerViewModel, wallpaper)
        },
        onFullWallpaperApplyWallpaperClick = {
            val wallpaper = viewerUiState.wallpaper ?: return@LocalScreenContent
            applyWallpaper(context, viewerViewModel, wallpaper)
        },
        onFullWallpaperFullScreenClick = {
            viewerUiState.wallpaper?.run {
                rootNavController.navigate(
                    WallpaperScreenDestination(
                        source = source,
                        thumbData = thumbData,
                        wallpaperId = id,
                    ).route,
                )
            }
        },
        onFABClick = { viewModel.showManageFoldersSheet(true) },
        onAddFolderClick = onAddFolderClick,
        onFullWallpaperLightDarkTypeFlagsChange = viewerViewModel::updateLightDarkTypeFlags,
    )

    if (uiState.showManageFoldersSheet) {
        ManageFoldersBottomSheet(
            folders = uiState.folders,
            sort = uiState.sort,
            onDismissRequest = { viewModel.showManageFoldersSheet(false) },
            onAddFolderClick = onAddFolderClick,
            onRemoveClick = { viewModel.showRemoveConfirmDialog(it.uri) },
            onSortChange = viewModel::updateSort,
        )
    }

    if (uiState.showRemoveConfirmDialog) {
        RemoveDirectoryConfirmDialog(
            onConfirmClick = { viewModel.removeLocalDirConfirmed() },
            onDismissRequest = { viewModel.showRemoveConfirmDialog(null) },
        )
    }
}
