package com.ammar.wallflow.ui.screens.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper1
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper2
import com.ammar.wallflow.ui.common.BottomBarAwareHorizontalTwoPane
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.utils.DownloadStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

@Composable
internal fun HomeScreenContent(
    gridState: LazyStaggeredGridState,
    wallpapers: LazyPagingItems<Wallpaper>,
    nestedScrollConnectionGetter: () -> NestedScrollConnection,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    isExpanded: Boolean = false,
    favorites: ImmutableList<Favorite> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    showFAB: Boolean = true,
    fullWallpaper: Wallpaper? = null,
    fullWallpaperActionsVisible: Boolean = true,
    fullWallpaperDownloadStatus: DownloadStatus? = null,
    fullWallpaperLoading: Boolean = false,
    showFullWallpaperInfo: Boolean = false,
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onFABClick: () -> Unit = {},
    onFullWallpaperTransform: () -> Unit = {},
    onFullWallpaperTap: () -> Unit = {},
    onFullWallpaperInfoClick: () -> Unit = {},
    onFullWallpaperInfoDismiss: () -> Unit = {},
    onFullWallpaperShareLinkClick: () -> Unit = {},
    onFullWallpaperShareImageClick: () -> Unit = {},
    onFullWallpaperApplyWallpaperClick: () -> Unit = {},
    onFullWallpaperFullScreenClick: () -> Unit = {},
    onFullWallpaperUploaderClick: (WallhavenUploader) -> Unit = {},
    onFullWallpaperDownloadPermissionsGranted: () -> Unit = {},
) {
    HomeScreenContent(
        modifier = modifier,
        isExpanded = isExpanded,
        listContent = {
            Feed(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnectionGetter()),
                gridState = gridState,
                contentPadding = contentPadding,
                wallpapers = wallpapers,
                favorites = favorites,
                blurSketchy = blurSketchy,
                blurNsfw = blurNsfw,
                header = header,
                selectedWallpaper = selectedWallpaper,
                showSelection = isExpanded,
                layoutPreferences = layoutPreferences,
                showFAB = showFAB,
                onWallpaperClick = onWallpaperClick,
                onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                onFABClick = onFABClick,
            )
        },
        detailContent = {
            WallpaperViewer(
                wallpaper = fullWallpaper,
                actionsVisible = fullWallpaperActionsVisible,
                downloadStatus = fullWallpaperDownloadStatus,
                loading = fullWallpaperLoading,
                thumbData = selectedWallpaper?.thumbData,
                showFullScreenAction = isExpanded,
                showInfo = showFullWallpaperInfo,
                onWallpaperTransform = onFullWallpaperTransform,
                onWallpaperTap = onFullWallpaperTap,
                onInfoClick = onFullWallpaperInfoClick,
                onInfoDismiss = onFullWallpaperInfoDismiss,
                onShareLinkClick = onFullWallpaperShareLinkClick,
                onShareImageClick = onFullWallpaperShareImageClick,
                onApplyWallpaperClick = onFullWallpaperApplyWallpaperClick,
                onFullScreenClick = onFullWallpaperFullScreenClick,
                onDownloadPermissionsGranted = onFullWallpaperDownloadPermissionsGranted,
                onUploaderClick = onFullWallpaperUploaderClick,
                onTagClick = onTagClick,
            )
        },
    )
}

@Composable
private fun HomeScreenContent(
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun Feed(
    gridState: LazyStaggeredGridState,
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    showSelection: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showFAB: Boolean = true,
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onFABClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (showFAB) {
                val isFabExpanded by remember(gridState) {
                    derivedStateOf { gridState.firstVisibleItemIndex == 0 }
                }

                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(contentPadding),
                    onClick = onFABClick,
                    expanded = isFabExpanded,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_filter_alt_24),
                            contentDescription = stringResource(R.string.filters),
                        )
                    },
                    text = { Text(text = stringResource(R.string.filters)) },
                )
            }
        },
    ) {
        WallpaperStaggeredGrid(
            modifier = Modifier.testTag("home:feed"),
            state = gridState,
            contentPadding = contentPadding,
            wallpapers = wallpapers,
            favorites = favorites,
            blurSketchy = blurSketchy,
            blurNsfw = blurNsfw,
            header = header,
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    WallFlowTheme {
        Surface {
            val wallpapers = flowOf(
                PagingData.from(
                    listOf<Wallpaper>(
                        wallhavenWallpaper1,
                        wallhavenWallpaper2,
                    ),
                ),
            )
            val pagingItems = wallpapers.collectAsLazyPagingItems()
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {}
            }
            val gridState = rememberLazyStaggeredGridState()
            HomeScreenContent(
                gridState = gridState,
                header = {
                    header(
                        sourceHeader = {
                            wallhavenHeader(
                                wallhavenTags = List(5) {
                                    WallhavenTag(
                                        id = it.toLong(),
                                        name = "tag$it",
                                        alias = emptyList(),
                                        categoryId = it.toLong(),
                                        category = "",
                                        purity = Purity.SFW,
                                        createdAt = Clock.System.now(),
                                    )
                                }.toImmutableList(),
                            )
                        },
                    )
                },
                wallpapers = pagingItems,
                nestedScrollConnectionGetter = { nestedScrollConnection },
            )
        }
    }
}
