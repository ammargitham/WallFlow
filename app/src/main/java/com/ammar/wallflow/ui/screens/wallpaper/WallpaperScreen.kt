package com.ammar.wallflow.ui.screens.wallpaper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenTagSearchMeta
import com.ammar.wallflow.model.search.WallhavenUploaderSearchMeta
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ammar.wallflow.utils.applyWallpaper
import com.ammar.wallflow.utils.shareWallpaper
import com.ammar.wallflow.utils.shareWallpaperUrl
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = wallpaperScreenLocalDeepLinkUriPattern),
    ],
)
@Composable
fun WallpaperScreen(
    navController: NavController,
    source: Source,
    wallpaperId: String,
    thumbData: String?,
    viewModel: WallpaperViewModel = hiltViewModel(),
    viewerViewModel: WallpaperViewerViewModel = hiltViewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val sheetColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        BottomSheetDefaults.Elevation,
    )
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val context = LocalContext.current

    LaunchedEffect(wallpaperId, thumbData) {
        viewerViewModel.setWallpaper(source, wallpaperId, thumbData)
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            systemController.update {
                it.copy(
                    statusBarColor = Color.Transparent,
                    navigationBarColor = Color.Transparent,
                )
            }
        }
    }

    DisposableEffect(Unit) {
        searchBarController.update { it.copy(visible = false) }
        bottomBarController.update { it.copy(visible = false) }
        systemController.update { it.copy(applyScaffoldPadding = false) }

        onDispose {
            systemController.update { it.copy(applyScaffoldPadding = true) }
        }
    }

    LaunchedEffect(uiState.systemBarsVisible, viewerUiState.showInfo) {
        systemController.update {
            it.copy(
                statusBarVisible = uiState.systemBarsVisible,
                navigationBarVisible = uiState.systemBarsVisible,
                navigationBarColor = if (viewerUiState.showInfo) {
                    sheetColor
                } else {
                    Color.Transparent
                },
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        WallpaperViewer(
            wallpaper = viewerUiState.wallpaper,
            actionsVisible = viewerUiState.actionsVisible,
            downloadStatus = viewerUiState.downloadStatus,
            loading = viewerUiState.loading,
            thumbData = viewerUiState.thumbData,
            showInfo = viewerUiState.showInfo,
            isFavorite = viewerUiState.isFavorite,
            onWallpaperTransform = {
                viewModel.onWallpaperTransform()
                viewerViewModel.onWallpaperTransform()
            },
            onWallpaperTap = {
                viewModel.onWallpaperTap()
                viewerViewModel.onWallpaperTap()
            },
            onInfoClick = viewerViewModel::showInfo,
            onInfoDismiss = { viewerViewModel.showInfo(false) },
            onShareLinkClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@WallpaperViewer
                shareWallpaperUrl(context, wallpaper)
            },
            onShareImageClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@WallpaperViewer
                shareWallpaper(context, viewerViewModel, wallpaper)
            },
            onApplyWallpaperClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@WallpaperViewer
                applyWallpaper(context, viewerViewModel, wallpaper)
            },
            onTagClick = {
                val search = WallhavenSearch(
                    query = "id:${it.id}",
                    meta = WallhavenTagSearchMeta(tag = it),
                )
                if (searchBarController.state.value.search == search) {
                    return@WallpaperViewer
                }
                navController.search(search)
            },
            onUploaderClick = {
                val search = WallhavenSearch(
                    query = "@${it.username}",
                    meta = WallhavenUploaderSearchMeta(uploader = it),
                )
                if (searchBarController.state.value.search == search) {
                    return@WallpaperViewer
                }
                navController.search(search)
            },
            onDownloadPermissionsGranted = viewerViewModel::download,
            onFavoriteToggle = { viewerViewModel.toggleFavorite() },
        )

        TopBar(
            navController = navController,
            visible = uiState.systemBarsVisible,
            gradientBg = true,
            showBackButton = true,
        )
    }
}
