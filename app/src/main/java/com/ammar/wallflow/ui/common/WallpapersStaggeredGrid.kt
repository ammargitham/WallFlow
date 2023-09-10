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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ammar.wallflow.data.preferences.GridColType
import com.ammar.wallflow.data.preferences.GridType
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Purity
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
    onWallpaperClick: (wallpaper: Wallpaper) -> Unit = {},
    onWallpaperFavoriteClick: (wallpaper: Wallpaper) -> Unit = {},
) {
    val isRefreshing = wallpapers.loadState.refresh == LoadState.Loading
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    val gridWidthDp = gridSize.width.toDp()
    val layoutDirection = LocalLayoutDirection.current
    val adaptiveMinWidth = remember(
        gridColType,
        gridColMinWidthPct,
        gridWidthDp,
        contentPadding,
    ) {
        if (gridColType != GridColType.ADAPTIVE) {
            return@remember 0.dp
        }
        val horizontalPadding = contentPadding.let {
            it.calculateStartPadding(layoutDirection) + it.calculateEndPadding(layoutDirection)
        }
        val availWidth = gridWidthDp - horizontalPadding
        var wDp = availWidth * gridColMinWidthPct / 100
        if (wDp <= 0.dp) {
            wDp = 128.dp
        }
        return@remember wDp
    }

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
        if (isRefreshing) {
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
                    onClick = { onWallpaperClick(it) },
                    onFavoriteClick = { onWallpaperFavoriteClick(it) },
                )
            } ?: PlaceholderWallpaperCard()
        }
    }
}
