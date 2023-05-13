package com.ammar.havenwalls.activities.main

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.havenwalls.data.common.SearchQuery
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.model.wallpaper2
import com.ammar.havenwalls.ui.common.SearchBar
import com.ammar.havenwalls.ui.common.Suggestion
import com.ammar.havenwalls.ui.common.bottombar.BottomBar
import com.ammar.havenwalls.ui.common.globalerrors.GlobalErrorsColumn
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBar
import com.ammar.havenwalls.ui.common.topWindowInsets
import com.ammar.havenwalls.ui.destinations.TypedDestination
import com.ammar.havenwalls.ui.home.HomeScreenContent
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import kotlinx.coroutines.flow.flowOf

@Composable
fun MainActivityContent(
    modifier: Modifier = Modifier,
    currentDestination: TypedDestination<*>? = null,
    showBackButton: Boolean = false,
    globalErrors: List<GlobalError> = emptyList(),
    searchBarVisible: Boolean = true,
    searchBarActive: Boolean = false,
    searchBarSearch: Search = Search(),
    searchBarQuery: String = "",
    searchBarSuggestions: List<Suggestion<Search>> = emptyList(),
    showSearchBarFilters: Boolean = false,
    searchBarDeleteSuggestion: Search? = null,
    searchBarOverflowIcon: @Composable (() -> Unit)? = null,
    onBackClick: () -> Unit = {},
    onFixWallHavenApiKeyClick: () -> Unit = {},
    onDismissGlobalError: (error: GlobalError) -> Unit = {},
    onBottomBarSizeChanged: (size: IntSize) -> Unit = {},
    onBottomBarItemClick: (destination: DirectionDestinationSpec) -> Unit = {},
    onSearchBarActiveChange: (active: Boolean) -> Unit = {},
    onSearchBarQueryChange: (String) -> Unit = {},
    onSearchBarSearch: (query: String) -> Unit = {},
    onSearchBarSuggestionClick: (suggestion: Suggestion<Search>) -> Unit = {},
    onSearchBarSuggestionInsert: (suggestion: Suggestion<Search>) -> Unit = {},
    onSearchBarSuggestionDeleteRequest: (suggestion: Suggestion<Search>) -> Unit = {},
    onSearchBarShowFiltersChange: (show: Boolean) -> Unit = {},
    onSearchBarFiltersChange: (searchQuery: SearchQuery) -> Unit = {},
    onDeleteSearchBarSuggestionConfirmClick: () -> Unit = {},
    onDeleteSearchBarSuggestionDismissRequest: () -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        GlobalErrorsColumn(
            globalErrors = globalErrors,
            onFixWallHavenApiKeyClick = onFixWallHavenApiKeyClick,
            onDismiss = onDismissGlobalError,
        )
        Scaffold(
            // modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            // topBar = {
            //     TopBar(
            //         navController = navController,
            //         scrollBehavior = scrollBehavior,
            //         visible = uiState.topAppBarVisible,
            //         gradientBg = uiState.topAppBarGradientBg,
            //         titleVisible = uiState.topAppBarTitleVisible,
            //     )
            // },
            // bottomBar = { BottomBar(navController = navController) },
            // floatingActionButton = {
            //     FloatingActionButton(
            //         expanded = fabController.expanded,
            //
            //         )
            // },
            // contentWindowInsets = WindowInsets.navigationBars,
            contentWindowInsets = WindowInsets(left = 0),
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                content(it)
                MainSearchBar(
                    modifier = Modifier.windowInsetsPadding(topWindowInsets),
                    visible = searchBarVisible,
                    active = searchBarActive,
                    search = searchBarSearch,
                    query = searchBarQuery,
                    suggestions = searchBarSuggestions,
                    showFilters = showSearchBarFilters,
                    deleteSuggestion = searchBarDeleteSuggestion,
                    overflowIcon = searchBarOverflowIcon,
                    onQueryChange = onSearchBarQueryChange,
                    onBackClick = if (showBackButton) onBackClick else null,
                    onSearch = onSearchBarSearch,
                    onSuggestionClick = onSearchBarSuggestionClick,
                    onSuggestionInsert = onSearchBarSuggestionInsert,
                    onSuggestionDeleteRequest = onSearchBarSuggestionDeleteRequest,
                    onActiveChange = onSearchBarActiveChange,
                    onShowFiltersChange = onSearchBarShowFiltersChange,
                    onFiltersChange = onSearchBarFiltersChange,
                    onDeleteSuggestionConfirmClick = onDeleteSearchBarSuggestionConfirmClick,
                    onDeleteSuggestionDismissRequest = onDeleteSearchBarSuggestionDismissRequest,
                )
                BottomBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .onSizeChanged(onBottomBarSizeChanged),
                    currentDestination = currentDestination,
                    onItemClick = onBottomBarItemClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMainActivityContent() {
    HavenWallsTheme {
        Surface {
            MainActivityContent {
                val wallpapers = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
                val pagingItems = wallpapers.collectAsLazyPagingItems()
                HomeScreenContent(
                    modifier = Modifier
                        .windowInsetsPadding(topWindowInsets)
                        .padding(top = SearchBar.Defaults.height),
                    tags = emptyList(),
                    wallpapers = pagingItems
                )
            }
        }
    }
}
