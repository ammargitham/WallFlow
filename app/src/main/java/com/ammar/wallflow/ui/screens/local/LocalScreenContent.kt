package com.ammar.wallflow.ui.screens.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.ui.common.BottomBarAwareHorizontalTwoPane
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun LocalScreenContent(
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    folders: ImmutableList<LocalDirectory> = persistentListOf(),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    viewedList: ImmutableList<Viewed> = persistentListOf(),
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    selectedWallpaper: Wallpaper? = null,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    fullWallpaper: Wallpaper? = null,
    fullWallpaperActionsVisible: Boolean = true,
    fullWallpaperLoading: Boolean = false,
    showFullWallpaperInfo: Boolean = false,
    isFullWallpaperFavorite: Boolean = false,
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onFABClick: () -> Unit = {},
    onFullWallpaperTransform: () -> Unit = {},
    onFullWallpaperTap: () -> Unit = {},
    onFullWallpaperInfoClick: () -> Unit = {},
    onFullWallpaperInfoDismiss: () -> Unit = {},
    onFullWallpaperShareImageClick: () -> Unit = {},
    onFullWallpaperApplyWallpaperClick: () -> Unit = {},
    onFullWallpaperFullScreenClick: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
    onFullWallpaperLightDarkTypeFlagsChange: (Int) -> Unit = {},
) {
    LocalScreenContent(
        modifier = modifier,
        isExpanded = isExpanded,
        listContent = {
            Feed(
                modifier = Modifier.fillMaxSize(),
                gridState = gridState,
                contentPadding = contentPadding,
                wallpapers = wallpapers,
                folders = folders,
                favorites = favorites,
                viewedList = viewedList,
                viewedWallpapersLook = viewedWallpapersLook,
                lightDarkList = lightDarkList,
                selectedWallpaper = selectedWallpaper,
                showSelection = isExpanded,
                layoutPreferences = layoutPreferences,
                onWallpaperClick = onWallpaperClick,
                onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                onFABClick = onFABClick,
                onAddFolderClick = onAddFolderClick,
            )
        },
        detailContent = {
            WallpaperViewer(
                wallpaper = fullWallpaper,
                actionsVisible = fullWallpaperActionsVisible,
                downloadStatus = null,
                loading = fullWallpaperLoading,
                thumbData = selectedWallpaper?.thumbData,
                isExpanded = true,
                showInfo = showFullWallpaperInfo,
                isFavorite = isFullWallpaperFavorite,
                onWallpaperTransform = onFullWallpaperTransform,
                onWallpaperTap = onFullWallpaperTap,
                onInfoClick = onFullWallpaperInfoClick,
                onInfoDismiss = onFullWallpaperInfoDismiss,
                onShareLinkClick = {},
                onShareImageClick = onFullWallpaperShareImageClick,
                onApplyWallpaperClick = onFullWallpaperApplyWallpaperClick,
                onFullScreenClick = onFullWallpaperFullScreenClick,
                onDownloadPermissionsGranted = {},
                onUploaderClick = {},
                onTagClick = {},
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
private fun LocalScreenContent(
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
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    folders: ImmutableList<LocalDirectory> = persistentListOf(),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    viewedList: ImmutableList<Viewed> = persistentListOf(),
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    showSelection: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onFABClick: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
) {
    val expandedFab by remember(gridState.firstVisibleItemIndex) {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 }
    }

    Box(
        modifier = modifier,
    ) {
        if (folders.isNotEmpty()) {
            WallpaperStaggeredGrid(
                state = gridState,
                contentPadding = contentPadding,
                wallpapers = wallpapers,
                favorites = favorites,
                viewedList = viewedList,
                viewedWallpapersLook = viewedWallpapersLook,
                lightDarkList = lightDarkList,
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
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(contentPadding)
                    .offset(x = (-16).dp, y = (-16).dp),
                onClick = onFABClick,
                expanded = expandedFab,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_folder_cog_24),
                        contentDescription = stringResource(R.string.manage),
                    )
                },
                text = { Text(text = stringResource(R.string.manage)) },
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.no_local_dirs),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    onClick = onAddFolderClick,
                ) {
                    Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_dir),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.add_dir))
                }
            }
        }
    }
}
