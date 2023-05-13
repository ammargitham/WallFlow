package com.ammar.havenwalls.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.memory.MemoryCache
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.rememberLazyStaggeredGridState
import com.ammar.havenwalls.extensions.search
import com.ammar.havenwalls.extensions.toDp
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.Tag
import com.ammar.havenwalls.model.TagSearchMeta
import com.ammar.havenwalls.model.Wallpaper
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.model.wallpaper2
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.common.SearchBar
import com.ammar.havenwalls.ui.common.WallpaperFiltersModalBottomSheet
import com.ammar.havenwalls.ui.common.WallpaperStaggeredGrid
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBarState
import com.ammar.havenwalls.ui.common.topWindowInsets
import com.ammar.havenwalls.ui.destinations.WallpaperScreenDestination
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.flowOf

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
)
@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    val gridState = wallpapers.rememberLazyStaggeredGridState()
    val refreshing = wallpapers.loadState.refresh == LoadState.Loading
    val expandedFab by remember(gridState.firstVisibleItemIndex) {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 }
    }
    val refreshState = rememberPullRefreshState(
        // refreshing = uiState.wallpapersLoading,
        refreshing = false,
        onRefresh = {
            wallpapers.refresh()
            viewModel.refresh()
        },
    )
    val filtersBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemBarsController = LocalSystemBarsController.current
    val bottomPadding = bottomBarController.state.value.size.height.toDp()

    LaunchedEffect(refreshing) {
        viewModel.setWallpapersLoading(refreshing)
    }

    LaunchedEffect(Unit) {
        systemBarsController.reset()
        bottomBarController.update { it.copy(visible = true) }
        searchBarController.update {
            MainSearchBarState(
                visible = true,
                overflowIcon = { SearchBarOverflowMenu(navigator = navigator) },
                onSearch = { navigator.search(it) }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets)
            .padding(top = SearchBar.Defaults.height)
            .pullRefresh(state = refreshState)
    ) {
        HomeScreenContent(
            gridState = gridState,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                bottom = bottomPadding + 8.dp,
            ),
            tags = uiState.tags,
            isTagsLoading = uiState.areTagsLoading,
            wallpapers = wallpapers,
            blurSketchy = uiState.blurSketchy,
            blurNsfw = uiState.blurNsfw,
            onWallpaperClick = { cacheKey, wallpaper ->
                navigator.navigate(
                    WallpaperScreenDestination(
                        cacheKey = cacheKey,
                        wallpaperId = wallpaper.id,
                    )
                )
            },
            onTagClick = {
                navigator.search(
                    Search(
                        query = "id:${it.id}",
                        meta = TagSearchMeta(it),
                    )
                )
            }
        )

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            // refreshing = uiState.wallpapersLoading,
            refreshing = false,
            state = refreshState,
        )

        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-16).dp - bottomPadding),
            onClick = {
                viewModel.showFilters(true)
            },
            expanded = expandedFab,
            icon = {
                Icon(
                    painterResource(R.drawable.baseline_filter_alt_24),
                    contentDescription = "Filters",
                )
            },
            text = { Text(text = "Filters") },
        )
    }
    if (uiState.showFilters) {
        WallpaperFiltersModalBottomSheet(
            bottomSheetState = filtersBottomSheetState,
            searchQuery = uiState.query,
            title = "Home Filters",
            onSave = viewModel::updateQuery,
            onDismissRequest = { viewModel.showFilters(false) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeScreenContent(
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    tags: List<Tag> = emptyList(),
    isTagsLoading: Boolean = false,
    wallpapers: LazyPagingItems<Wallpaper>,
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    onWallpaperClick: (cacheKey: MemoryCache.Key?, wallpaper: Wallpaper) -> Unit = { _, _ -> },
    onTagClick: (tag: Tag) -> Unit = {},
) {
    WallpaperStaggeredGrid(
        modifier = modifier,
        state = gridState,
        contentPadding = contentPadding,
        wallpapers = wallpapers,
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
        onWallpaperClick = onWallpaperClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    HavenWallsTheme {
        val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
        val pagingItems = wallpapers.collectAsLazyPagingItems()
        HomeScreenContent(tags = emptyList(), wallpapers = pagingItems)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, widthDp = 480)
@Composable
private fun PortraitPreview() {
    HavenWallsTheme {
        val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
        val pagingItems = wallpapers.collectAsLazyPagingItems()
        HomeScreenContent(tags = emptyList(), wallpapers = pagingItems)
    }
}
