package com.ammar.wallflow.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ammar.wallflow.model.Tag
import com.ammar.wallflow.model.Uploader
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallpaper1
import com.ammar.wallflow.model.wallpaper2
import com.ammar.wallflow.ui.common.BottomBarAwareHorizontalTwoPane
import com.ammar.wallflow.ui.common.WallpaperStaggeredGrid
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.ui.wallpaperviewer.WallpaperViewer
import com.ammar.wallflow.utils.DownloadStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun HomeScreenContent(
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    isExpanded: Boolean = false,
    favorites: ImmutableList<Favorite> = persistentListOf(),
    tags: ImmutableList<Tag> = persistentListOf(),
    isTagsLoading: Boolean = false,
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
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onTagClick: (tag: Tag) -> Unit = {},
    onFABClick: () -> Unit = {},
    onFullWallpaperTransform: () -> Unit = {},
    onFullWallpaperTap: () -> Unit = {},
    onFullWallpaperInfoClick: () -> Unit = {},
    onFullWallpaperInfoDismiss: () -> Unit = {},
    onFullWallpaperShareLinkClick: () -> Unit = {},
    onFullWallpaperShareImageClick: () -> Unit = {},
    onFullWallpaperApplyWallpaperClick: () -> Unit = {},
    onFullWallpaperFullScreenClick: () -> Unit = {},
    onFullWallpaperUploaderClick: (Uploader) -> Unit = {},
    onFullWallpaperDownloadPermissionsGranted: () -> Unit = {},
) {
    if (isExpanded) {
        BottomBarAwareHorizontalTwoPane(
            modifier = modifier,
            first = {
                Feed(
                    modifier = Modifier.fillMaxSize(),
                    gridState = gridState,
                    contentPadding = contentPadding,
                    wallpapers = wallpapers,
                    favorites = favorites,
                    blurSketchy = blurSketchy,
                    blurNsfw = blurNsfw,
                    tags = tags,
                    isTagsLoading = isTagsLoading,
                    onTagClick = onTagClick,
                    selectedWallpaper = selectedWallpaper,
                    showSelection = true,
                    layoutPreferences = layoutPreferences,
                    showFAB = showFAB,
                    onWallpaperClick = onWallpaperClick,
                    onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                    onFABClick = onFABClick,
                )
            },
            second = {
                WallpaperViewer(
                    wallpaper = fullWallpaper,
                    actionsVisible = fullWallpaperActionsVisible,
                    downloadStatus = fullWallpaperDownloadStatus,
                    loading = fullWallpaperLoading,
                    thumbUrl = selectedWallpaper?.thumbs?.original,
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
                    onDownloadPermissionsGranted = onFullWallpaperDownloadPermissionsGranted,
                    onUploaderClick = onFullWallpaperUploaderClick,
                    onTagClick = onTagClick,
                )
            },
        )
    } else {
        Feed(
            modifier = modifier,
            gridState = gridState,
            contentPadding = contentPadding,
            wallpapers = wallpapers,
            favorites = favorites,
            blurSketchy = blurSketchy,
            blurNsfw = blurNsfw,
            tags = tags,
            isTagsLoading = isTagsLoading,
            onTagClick = onTagClick,
            selectedWallpaper = selectedWallpaper,
            showSelection = false,
            layoutPreferences = layoutPreferences,
            showFAB = showFAB,
            onWallpaperClick = onWallpaperClick,
            onWallpaperFavoriteClick = onWallpaperFavoriteClick,
            onFABClick = onFABClick,
        )
    }
}

@Composable
private fun Feed(
    wallpapers: LazyPagingItems<Wallpaper>,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    layoutPreferences: LayoutPreferences = LayoutPreferences(),
    favorites: ImmutableList<Favorite> = persistentListOf(),
    tags: ImmutableList<Tag> = persistentListOf(),
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    isTagsLoading: Boolean = false,
    showSelection: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showFAB: Boolean = true,
    onTagClick: (tag: Tag) -> Unit = {},
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
    onFABClick: () -> Unit = {},
) {
    val expandedFab by remember(gridState.firstVisibleItemIndex) {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 }
    }

    Box(
        modifier = modifier,
    ) {
        WallpaperStaggeredGrid(
            state = gridState,
            contentPadding = contentPadding,
            wallpapers = wallpapers,
            favorites = favorites,
            blurSketchy = blurSketchy,
            blurNsfw = blurNsfw,
            header = {
                if (tags.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        PopularTagsRow(
                            tags = tags,
                            loading = isTagsLoading,
                            onTagClick = onTagClick,
                        )
                    }
                }
            },
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

        if (showFAB) {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(contentPadding)
                    .offset(x = (-16).dp, y = (-16).dp),
                onClick = onFABClick,
                expanded = expandedFab,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_filter_alt_24),
                        contentDescription = stringResource(R.string.filters),
                    )
                },
                text = { Text(text = stringResource(R.string.filters)) },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    WallFlowTheme {
        Surface {
            val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
            val pagingItems = wallpapers.collectAsLazyPagingItems()
            HomeScreenContent(tags = persistentListOf(), wallpapers = pagingItems)
        }
    }
}
