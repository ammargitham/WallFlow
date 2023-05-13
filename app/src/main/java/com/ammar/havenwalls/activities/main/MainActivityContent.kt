package com.ammar.havenwalls.activities.main

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.model.wallpaper2
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
    onBackClick: () -> Unit = {},
    onFixWallHavenApiKeyClick: () -> Unit = {},
    onDismissGlobalError: (error: GlobalError) -> Unit = {},
    onBottomBarSizeChanged: (size: IntSize) -> Unit = {},
    onBottomBarItemClick: (destination: DirectionDestinationSpec) -> Unit = {},
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
                    showBackButton = showBackButton,
                    onBackClick = onBackClick,
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
                HomeScreenContent(tags = emptyList(), wallpapers = pagingItems)
            }
        }
    }
}
