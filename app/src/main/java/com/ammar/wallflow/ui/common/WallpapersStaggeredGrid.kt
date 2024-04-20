package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ammar.wallflow.data.preferences.GridColType
import com.ammar.wallflow.data.preferences.GridType
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.extensions.Saver
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun WallpaperStaggeredGrid(
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    wallpapers: LazyPagingItems<Wallpaper>,
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
    emptyContent: (LazyStaggeredGridScope.() -> Unit)? = null,
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    selectedWallpaper: Wallpaper? = null,
    showSelection: Boolean = false,
    gridType: GridType = GridType.STAGGERED,
    gridColType: GridColType = GridColType.ADAPTIVE,
    gridColCount: Int = 2,
    gridColMinWidthPct: Int = 40,
    roundedCorners: Boolean = true,
    favorites: ImmutableList<Favorite> = persistentListOf(),
    viewedList: ImmutableList<Viewed> = persistentListOf(),
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
) {
    val isRefreshing = wallpapers.loadState.refresh == LoadState.Loading
    var gridSize by rememberSaveable(
        stateSaver = IntSize.Saver,
    ) { mutableStateOf(IntSize.Zero) }
    val gridWidthDp = gridSize.width.toDp()
    val layoutDirection = LocalLayoutDirection.current
    val adaptiveMinWidth = getAdaptiveMinWidth(
        gridColType,
        contentPadding,
        layoutDirection,
        gridWidthDp,
        gridColMinWidthPct,
    )

    LazyVerticalStaggeredGrid(
        modifier = modifier.onSizeChanged { gridSize = it },
        state = state,
        contentPadding = contentPadding,
        columns = when (gridColType) {
            GridColType.ADAPTIVE -> StaggeredGridCells.Adaptive(minSize = adaptiveMinWidth)
            GridColType.FIXED -> StaggeredGridCells.Fixed(count = gridColCount)
        },
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        header?.invoke(this)
        if (wallpapers.itemCount == 0 && !isRefreshing) {
            emptyContent?.invoke(this)
        }
        if (isRefreshing && wallpapers.itemCount == 0) {
            items(9) {
                PlaceholderWallpaperCard()
            }
            return@LazyVerticalStaggeredGrid
        }
        items(
            count = wallpapers.itemCount,
            key = wallpapers.itemKey { it.id },
            contentType = wallpapers.itemContentType { "wallpaper" },
        ) { index ->
            val wallpaper = wallpapers[index]
            wallpaper?.let {
                WallpaperCard(
                    modifier = Modifier.animateItem(),
                    wallpaper = it,
                    blur = when (it.purity) {
                        Purity.SFW -> false
                        Purity.SKETCHY -> blurSketchy
                        Purity.NSFW -> blurNsfw
                    },
                    isSelected = showSelection && selectedWallpaper?.id == it.id,
                    isFavorite = favorites.find { f ->
                        f.sourceId == it.id && f.source == it.source
                    } != null,
                    fixedHeight = gridType == GridType.FIXED_SIZE,
                    roundedCorners = roundedCorners,
                    isViewed = viewedList.find { v ->
                        v.sourceId == it.id && v.source == it.source
                    } != null,
                    viewedWallpapersLook = viewedWallpapersLook,
                    lightDarkTypeFlags = lightDarkList.find { v ->
                        v.sourceId == it.id && v.source == it.source
                    }?.typeFlags ?: LightDarkType.UNSPECIFIED,
                    onClick = { onWallpaperClick(it) },
                    onFavoriteClick = { onWallpaperFavoriteClick(it) },
                )
            } ?: PlaceholderWallpaperCard()
        }
    }
}

private fun getAdaptiveMinWidth(
    gridColType: GridColType,
    contentPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    gridWidthDp: Dp,
    gridColMinWidthPct: Int,
): Dp {
    if (gridColType != GridColType.ADAPTIVE) {
        return 0.dp
    }
    val horizontalPadding = contentPadding.let {
        it.calculateStartPadding(layoutDirection) + it.calculateEndPadding(layoutDirection)
    }
    val availWidth = gridWidthDp - horizontalPadding
    var wDp = availWidth * gridColMinWidthPct / 100
    if (wDp <= 0.dp) {
        wDp = 128.dp
    }
    return wDp
}
