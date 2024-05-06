package com.ammar.wallflow.ui.screens.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.extensions.rememberLazyStaggeredGridState
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.ui.common.BottomBarAwareHorizontalTwoPane
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.utils.DownloadStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun CollectionsScreenContent(
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    viewedList: ImmutableList<Viewed> = persistentListOf(),
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showSelection: Boolean = false,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    emptyContent: (LazyStaggeredGridScope.() -> Unit)? = null,
    fullWallpaper: Wallpaper? = null,
    fullWallpaperActionsVisible: Boolean = true,
    fullWallpaperDownloadStatus: DownloadStatus? = null,
    fullWallpaperLoading: Boolean = false,
    showFullWallpaperInfo: Boolean = false,
    isFullWallpaperFavorite: Boolean = false,
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
    onFullWallpaperLightDarkTypeFlagsChange: (Int) -> Unit = {},
) {
    CollectionsScreenContent(
        modifier = modifier,
        isExpanded = isExpanded,
        listContent = {
            Feed(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                wallpapers = wallpapers,
                favorites = favorites,
                viewedList = viewedList,
                viewedWallpapersLook = viewedWallpapersLook,
                blurSketchy = blurSketchy,
                blurNsfw = blurNsfw,
                selectedWallpaper = selectedWallpaper,
                showSelection = showSelection,
                layoutPreferences = layoutPreferences,
                lightDarkList = lightDarkList,
                header = header,
                emptyContent = emptyContent,
                onWallpaperClick = onWallpaperClick,
                onWallpaperFavoriteClick = onWallpaperFavoriteClick,
            )
        },
        detailContent = {
            WallpaperViewer(
                wallpaper = fullWallpaper,
                actionsVisible = fullWallpaperActionsVisible,
                downloadStatus = fullWallpaperDownloadStatus,
                loading = fullWallpaperLoading,
                thumbData = selectedWallpaper?.thumbData,
                isExpanded = true,
                showInfo = showFullWallpaperInfo,
                isFavorite = isFullWallpaperFavorite,
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
                onFavoriteToggle = {
                    if (fullWallpaper != null) {
                        onWallpaperFavoriteClick(fullWallpaper)
                    }
                },
                onLightDarkTypeFlagsChange = onFullWallpaperLightDarkTypeFlagsChange,
            )
        },
    )
}

@Composable
private fun CollectionsScreenContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    listContent: @Composable () -> Unit = {},
    detailContent: @Composable () -> Unit = {},
) {
    val listSaveableStateHolder = rememberSaveableStateHolder()
    val list = remember {
        movableContentOf {
            listSaveableStateHolder.SaveableStateProvider(0) {
                listContent()
            }
        }
    }

    Box(
        modifier = modifier,
    ) {
        if (isExpanded) {
            BottomBarAwareHorizontalTwoPane(
                modifier = Modifier.fillMaxSize(),
                first = list,
                second = detailContent,
                splitFraction = 0.5f,
            )
        } else {
            list()
        }
    }
}

@Composable
private fun Feed(
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    viewedList: ImmutableList<Viewed> = persistentListOf(),
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    showSelection: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    emptyContent: (LazyStaggeredGridScope.() -> Unit)? = null,
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
) {
    val gridState = wallpapers.rememberLazyStaggeredGridState()
    WallpaperStaggeredGrid(
        modifier = modifier.testTag("collections:feed"),
        state = gridState,
        contentPadding = contentPadding,
        wallpapers = wallpapers,
        favorites = favorites,
        viewedList = viewedList,
        viewedWallpapersLook = viewedWallpapersLook,
        blurSketchy = blurSketchy,
        blurNsfw = blurNsfw,
        header = header,
        emptyContent = emptyContent,
        selectedWallpaper = selectedWallpaper,
        showSelection = showSelection,
        gridType = layoutPreferences.gridType,
        gridColType = layoutPreferences.gridColType,
        gridColCount = layoutPreferences.gridColCount,
        gridColMinWidthPct = layoutPreferences.gridColMinWidthPct,
        roundedCorners = layoutPreferences.roundedCorners,
        lightDarkList = lightDarkList,
        onWallpaperClick = onWallpaperClick,
        onWallpaperFavoriteClick = onWallpaperFavoriteClick,
    )
}
