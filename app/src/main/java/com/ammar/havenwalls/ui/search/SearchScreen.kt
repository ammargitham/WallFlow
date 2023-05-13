package com.ammar.havenwalls.ui.search

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.memory.MemoryCache
import com.ammar.havenwalls.extensions.produceState
import com.ammar.havenwalls.model.Wallpaper
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.model.wallpaper2
import com.ammar.havenwalls.ui.common.SearchBar
import com.ammar.havenwalls.ui.common.WallpaperStaggeredGrid
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBarState
import com.ammar.havenwalls.ui.common.topWindowInsets
import com.ammar.havenwalls.ui.destinations.WallpaperScreenDestination
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    navArgsDelegate = SearchScreenNavArgs::class,
)
@Composable
fun SearchScreen(
    navigator: DestinationsNavigator,
    navArgs: SearchScreenNavArgs,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val search = navArgs.search
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uiState = lifecycle.produceState(
        viewModel = viewModel,
        initialValue = SearchUiState()
    )
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        searchBarController.update {
            MainSearchBarState(
                visible = true,
                search = search,
                onSearch = { viewModel.search(it) }
            )
        }
        bottomBarController.update { it.copy(visible = false) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets)
            .padding(top = SearchBar.Defaults.height),
        // .pullRefresh(
        //     state = refreshState,
        //     enabled = !refreshing, // we show shimmer when refreshing
        // )
    ) {
        SearchScreenContent(
            wallpapers = wallpapers,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                bottom = 8.dp,
            ),
            onWallpaperClick = { cacheKey, wallpaper ->
                navigator.navigate(
                    WallpaperScreenDestination(
                        cacheKey = cacheKey,
                        wallpaperId = wallpaper.id,
                    )
                )
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchScreenContent(
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    wallpapers: LazyPagingItems<Wallpaper>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onWallpaperClick: (cacheKey: MemoryCache.Key?, wallpaper: Wallpaper) -> Unit = { _, _ -> },
) {
    WallpaperStaggeredGrid(
        modifier = modifier,
        state = gridState,
        contentPadding = contentPadding,
        wallpapers = wallpapers,
        onWallpaperClick = onWallpaperClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSearchScreenContent() {
    val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
    val pagingItems = wallpapers.collectAsLazyPagingItems()
    HavenWallsTheme {
        Surface {
            SearchScreenContent(wallpapers = pagingItems)
        }
    }
}
