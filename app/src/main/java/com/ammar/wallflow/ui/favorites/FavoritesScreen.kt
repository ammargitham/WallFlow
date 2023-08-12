package com.ammar.wallflow.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.extensions.findActivity
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.ui.appCurrentDestinationAsState
import com.ammar.wallflow.ui.common.LocalSystemBarsController
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.destinations.WallpaperScreenDestination
import com.ammar.wallflow.ui.wallpaper.WallpaperScreenNavArgs
import com.ammar.wallflow.ui.wallpaper.WallpaperViewModel
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Destination
@Composable
fun FavoritesScreen(
    twoPaneController: TwoPaneNavigation.Controller,
    wallpaperViewModel: WallpaperViewModel,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.favoriteWallpapers.collectAsLazyPagingItems()
    val gridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val systemBarsController = LocalSystemBarsController.current
    val bottomBarController = LocalBottomBarController.current
    val searchBarsController = LocalMainSearchBarController.current

    val windowSizeClass = calculateWindowSizeClass(context.findActivity())
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val currentPane2Destination by twoPaneController.pane2NavHostController
        .appCurrentDestinationAsState()
    val isTwoPaneMode = twoPaneController.paneMode.value == TwoPaneNavigation.Mode.TWO_PANE

    LaunchedEffect(Unit) {
        systemBarsController.reset()
        bottomBarController.update { it.copy(visible = true) }
        searchBarsController.update { it.copy(visible = false) }
    }

    LaunchedEffect(uiState.selectedWallpaper) {
        val navArgs = WallpaperScreenNavArgs(
            wallpaperId = uiState.selectedWallpaper?.id,
            thumbUrl = uiState.selectedWallpaper?.thumbs?.original,
        )
        if (!isExpanded) {
            return@LaunchedEffect
        }
        twoPaneController.setPaneMode(TwoPaneNavigation.Mode.TWO_PANE)
        if (currentPane2Destination is WallpaperScreenDestination) {
            wallpaperViewModel.setWallpaperId(
                wallpaperId = navArgs.wallpaperId,
                thumbUrl = navArgs.thumbUrl,
            )
            return@LaunchedEffect
        }
        twoPaneController.navigatePane2(
            WallpaperScreenDestination(navArgs),
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
            gridState = gridState,
            // contentPadding = PaddingValues(
            //     start = startPadding + 8.dp,
            //     end = 8.dp,
            //     bottom = bottomPadding + 8.dp,
            // ),
            wallpapers = wallpapers,
            favorites = uiState.favorites,
            blurSketchy = uiState.blurSketchy,
            blurNsfw = uiState.blurNsfw,
            selectedWallpaper = uiState.selectedWallpaper,
            showSelection = isTwoPaneMode,
            layoutPreferences = uiState.layoutPreferences,
            onWallpaperClick = onWallpaperClick,
            onWallpaperFavoriteClick = viewModel::toggleFavorite,
        )
    }
}

@Composable
private fun FavoritesScreenContent(
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    wallpapers: LazyPagingItems<Wallpaper>,
    favorites: ImmutableList<Favorite> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showSelection: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
) {
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
