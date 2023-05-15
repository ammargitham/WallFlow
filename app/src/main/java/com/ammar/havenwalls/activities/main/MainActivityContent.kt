package com.ammar.havenwalls.activities.main

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.havenwalls.data.common.Purity
import com.ammar.havenwalls.data.common.SearchQuery
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.Tag
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.model.wallpaper2
import com.ammar.havenwalls.ui.common.SearchBar
import com.ammar.havenwalls.ui.common.Suggestion
import com.ammar.havenwalls.ui.common.bottombar.BottomBar
import com.ammar.havenwalls.ui.common.bottombar.NavRail
import com.ammar.havenwalls.ui.common.globalerrors.GlobalErrorsColumn
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBar
import com.ammar.havenwalls.ui.common.topWindowInsets
import com.ammar.havenwalls.ui.destinations.TypedDestination
import com.ammar.havenwalls.ui.home.HomeScreenContent
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ramcosta.composedestinations.spec.Direction
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

@Composable
fun MainActivityContent(
    modifier: Modifier = Modifier,
    currentDestination: TypedDestination<*>? = null,
    showBackButton: Boolean = false,
    useNavRail: Boolean = false,
    useDockedSearchBar: Boolean = false,
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
    onBottomBarItemClick: (destination: Direction) -> Unit = {},
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
                    useDocked = useDockedSearchBar,
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
                if (useNavRail) {
                    NavRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.TopStart)
                            .onSizeChanged(onBottomBarSizeChanged),
                        currentDestination = currentDestination,
                        onItemClick = onBottomBarItemClick,
                    )
                } else {
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
}

@Preview(
    showSystemUi = true,
    device = "id:pixel_6_pro"
)
@Preview(
    showSystemUi = true,
    device = "id:pixel_6_pro",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentPixel6Pro() {
    PreviewContent()
}

@Preview(
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Preview(
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentCustomDpi() {
    PreviewContent(
        useNavRail = true,
    )
}

@Preview(
    showSystemUi = true,
    device = "id:desktop_large"
)
@Preview(
    showSystemUi = true,
    device = "id:desktop_large",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentDesktop() {
    PreviewContent(
        useNavRail = true,
    )
}

@Preview(
    showSystemUi = true,
    device = "id:8in Foldable"
)
@Preview(
    showSystemUi = true,
    device = "id:8in Foldable",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentFoldable() {
    PreviewContent(
        useNavRail = true,
    )
}

@Preview(
    showSystemUi = true,
    device = "id:10.1in WXGA (Tablet)"
)
@Preview(
    showSystemUi = true,
    device = "id:10.1in WXGA (Tablet)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentTable() {
    PreviewContent(
        useNavRail = true,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PreviewContent(
    useNavRail: Boolean = false,
    isExpanded: Boolean = false,
) {
    val previewWallpaperFlow = flowOf(PagingData.from(listOf(wallpaper1, wallpaper2)))
    val previewTags = List(10) {
        Tag(
            id = it.toLong(),
            name = "tag_$it",
            alias = emptyList(),
            categoryId = it.toLong(),
            category = "category",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        )
    }

    HavenWallsTheme {
        Surface {
            MainActivityContent(
                useNavRail = useNavRail,
            ) {
                val pagingItems = previewWallpaperFlow.collectAsLazyPagingItems()
                HomeScreenContent(
                    modifier = Modifier
                        .windowInsetsPadding(topWindowInsets)
                        .padding(top = SearchBar.Defaults.height),
                    tags = previewTags,
                    wallpapers = pagingItems,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp,
                    ),
                )
            }
        }
    }
}
