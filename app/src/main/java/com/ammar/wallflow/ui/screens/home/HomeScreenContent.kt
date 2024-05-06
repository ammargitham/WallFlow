package com.ammar.wallflow.ui.screens.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.extensions.rememberLazyStaggeredGridState
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper1
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper2
import com.ammar.wallflow.ui.common.BottomBarAwareHorizontalTwoPane
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.screens.home.composables.header
import com.ammar.wallflow.ui.screens.home.composables.wallhavenHeader
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.utils.DownloadStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    wallpapers: LazyPagingItems<Wallpaper>,
    nestedScrollConnectionGetter: () -> NestedScrollConnection,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    isExpanded: Boolean = false,
    isMedium: Boolean = false,
    favorites: ImmutableList<Favorite> = persistentListOf(),
    viewedList: ImmutableList<Viewed> = persistentListOf(),
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    showFAB: Boolean = true,
    isHome: Boolean = true,
    fullWallpaper: Wallpaper? = null,
    fullWallpaperActionsVisible: Boolean = true,
    fullWallpaperDownloadStatus: DownloadStatus? = null,
    fullWallpaperLoading: Boolean = false,
    showFullWallpaperInfo: Boolean = false,
    isFullWallpaperFavorite: Boolean = false,
    fullWallpaperLightDarkTypeFlags: Int = LightDarkType.UNSPECIFIED,
    searchBar: @Composable () -> Unit = {},
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    refreshIndicator: @Composable (BoxScope.() -> Unit) = {},
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onFABClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
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
    onFullWallpaperLightDarkTypeFlagsChange: (Int) -> Unit = {},
) {
    HomeScreenContent(
        modifier = modifier,
        isExpanded = isExpanded,
        listContent = {
            Feed(
                wallpapers = wallpapers,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnectionGetter()),
                contentPadding = contentPadding,
                layoutPreferences = layoutPreferences,
                favorites = favorites,
                viewedList = viewedList,
                viewedWallpapersLook = viewedWallpapersLook,
                lightDarkList = lightDarkList,
                blurSketchy = blurSketchy,
                blurNsfw = blurNsfw,
                showSelection = isExpanded,
                selectedWallpaper = selectedWallpaper,
                isHome = isHome,
                showFAB = showFAB,
                isMedium = isMedium,
                searchBar = searchBar,
                header = header,
                refreshState = refreshState,
                refreshIndicator = refreshIndicator,
                onWallpaperClick = onWallpaperClick,
                onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                onFABClick = onFABClick,
                onRefresh = onRefresh,
            )
        },
        detailContent = {
            WallpaperViewer(
                wallpaper = fullWallpaper,
                actionsVisible = fullWallpaperActionsVisible,
                downloadStatus = fullWallpaperDownloadStatus,
                loading = fullWallpaperLoading,
                thumbData = selectedWallpaper?.thumbData,
                isExpanded = isExpanded,
                showInfo = showFullWallpaperInfo,
                isFavorite = isFullWallpaperFavorite,
                lightDarkTypeFlags = fullWallpaperLightDarkTypeFlags,
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

@OptIn(ExperimentalMaterial3Api::class)
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
    isHome: Boolean = true,
    showFAB: Boolean = true,
    isMedium: Boolean = false,
    searchBar: @Composable () -> Unit = {},
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    refreshIndicator: @Composable (BoxScope.() -> Unit) = {},
    onWallpaperClick: (Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (Wallpaper) -> Unit = {},
    onFABClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val gridState = wallpapers.rememberLazyStaggeredGridState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = if (isMedium) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFAB,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 },
                ),
            ) {
                val isFabExpanded by remember(gridState) {
                    derivedStateOf { gridState.firstVisibleItemIndex == 0 }
                }

                val testTag = "home:${if (isHome) "home" else "search"}-filters"

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
                    text = {
                        Text(
                            modifier = Modifier.testTag(testTag),
                            text = stringResource(
                                if (isHome) {
                                    R.string.home_filters
                                } else {
                                    R.string.search_filters
                                },
                            ),
                        )
                    },
                )
            }
        },
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = refreshState,
            isRefreshing = wallpapers.loadState.refresh == LoadState.Loading,
            onRefresh = onRefresh,
            indicator = refreshIndicator,
        ) {
            WallpaperStaggeredGrid(
                modifier = Modifier
                    .windowInsetsPadding(topWindowInsets)
                    .testTag("home:feed")
                    .padding(it),
                state = gridState,
                contentPadding = contentPadding,
                wallpapers = wallpapers,
                favorites = favorites,
                viewedList = viewedList,
                viewedWallpapersLook = viewedWallpapersLook,
                lightDarkList = lightDarkList,
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
            searchBar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            HomeScreenContent(
                header = {
                    header(
                        sources = persistentListOf(OnlineSource.WALLHAVEN),
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
