package com.ammar.wallflow.ui.wallpaper

import android.content.Intent
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
import com.ammar.wallflow.activities.setwallpaper.SetWallpaperActivity
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getUriForFile
import com.ammar.wallflow.extensions.parseMimeType
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.extensions.share
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.TagSearchMeta
import com.ammar.wallflow.model.UploaderSearchMeta
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewerViewModel
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = wallpaperScreenLocalDeepLinkUriPattern),
        DeepLink(uriPattern = wallpaperScreenExternalDeepLinkUriPattern),
        DeepLink(uriPattern = wallpaperScreenExternalShortDeepLinkUriPattern),
    ],
)
@Composable
fun WallpaperScreen(
    navController: NavController,
    wallpaperId: String,
    thumbUrl: String?,
    viewModel: WallpaperViewModel = hiltViewModel(),
    viewerViewModel: WallpaperViewerViewModel = hiltViewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewerUiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
    val sheetColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        BottomSheetDefaults.Elevation,
    )
    // TODO: Use Color.Transparent for nav bar
    // fully transparent nav bar will require setting some extra flags,
    // so setting alpha 0.01 as current workaround
    val navigationBarColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f)
    // val navigationBarColor = Color.Transparent
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val context = LocalContext.current

    LaunchedEffect(wallpaperId, thumbUrl) {
        viewerViewModel.setWallpaperId(wallpaperId, thumbUrl)
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            systemController.update {
                it.copy(
                    statusBarColor = Color.Transparent,
                    lightStatusBars = false,
                    navigationBarColor = navigationBarColor,
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
                navigationBarColor = if (viewerUiState.showInfo) sheetColor else navigationBarColor,
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
            thumbUrl = viewerUiState.thumbUrl,
            showInfo = viewerUiState.showInfo,
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
            onShareLinkClick = { viewerUiState.wallpaper?.run { context.share(url) } },
            onShareImageClick = {
                val wallpaper = viewerUiState.wallpaper ?: return@WallpaperViewer
                viewerViewModel.downloadForSharing {
                    if (it == null) return@downloadForSharing
                    context.share(
                        uri = context.getUriForFile(it),
                        type = wallpaper.fileType.ifBlank { parseMimeType(wallpaper.path) },
                        title = wallpaper.path.getFileNameFromUrl(),
                        grantTempPermission = true,
                    )
                }
            },
            onApplyWallpaperClick = {
                viewerViewModel.downloadForSharing {
                    val file = it ?: return@downloadForSharing
                    context.startActivity(
                        Intent().apply {
                            setClass(context, SetWallpaperActivity::class.java)
                            putExtra(
                                SetWallpaperActivity.EXTRA_URI,
                                context.getUriForFile(file),
                            )
                        },
                    )
                }
            },
            onTagClick = {
                val search = Search(
                    query = "id:${it.id}",
                    meta = TagSearchMeta(tag = it),
                )
                if (searchBarController.state.value.search == search) {
                    return@WallpaperViewer
                }
                navController.search(search)
            },
            onUploaderClick = {
                val search = Search(
                    query = "@${it.username}",
                    meta = UploaderSearchMeta(uploader = it),
                )
                if (searchBarController.state.value.search == search) {
                    return@WallpaperViewer
                }
                navController.search(search)
            },
            onDownloadPermissionsGranted = viewerViewModel::download,
        )

        TopBar(
            navController = navController,
            visible = uiState.systemBarsVisible,
            gradientBg = true,
            showBackButton = true,
        )
    }
}
