package com.ammar.wallflow.activities.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper1
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper2
import com.ammar.wallflow.ui.common.bottombar.BottomBar
import com.ammar.wallflow.ui.common.bottombar.NavRail
import com.ammar.wallflow.ui.common.globalerrors.GlobalErrorsColumn
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.screens.NavGraph
import com.ammar.wallflow.ui.screens.home.HomeScreenContent
import com.ammar.wallflow.ui.screens.home.composables.header
import com.ammar.wallflow.ui.screens.home.composables.wallhavenHeader
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

@OptIn(
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun MainActivityContent(
    currentDestination: NavDestination? = null,
    globalErrors: List<GlobalError> = emptyList(),
    bottomBarVisible: Boolean = true,
    showLocalTab: Boolean = true,
    searchBar: @Composable () -> Unit = {},
    onFixWallHavenApiKeyClick: () -> Unit = {},
    onDismissGlobalError: (error: GlobalError) -> Unit = {},
    onBottomBarItemClick: (destination: NavGraph) -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo(),
    )
    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            AnimatedVisibility(
                visible = bottomBarVisible,
                enter = if (navigationSuiteType == NavigationSuiteType.NavigationBar) {
                    slideInVertically(initialOffsetY = { it })
                } else {
                    slideInHorizontally(initialOffsetX = { -it })
                },
                exit = if (navigationSuiteType == NavigationSuiteType.NavigationBar) {
                    slideOutVertically(targetOffsetY = { it })
                } else {
                    slideOutHorizontally(targetOffsetX = { -it })
                },
            ) {
                if (navigationSuiteType == NavigationSuiteType.NavigationBar) {
                    BottomBar(
                        modifier = Modifier.fillMaxWidth(),
                        currentDestination = currentDestination,
                        showLocalTab = showLocalTab,
                        onItemClick = onBottomBarItemClick,
                    )
                } else {
                    NavRail(
                        modifier = Modifier.fillMaxHeight(),
                        currentDestination = currentDestination,
                        showLocalTab = showLocalTab,
                        onItemClick = onBottomBarItemClick,
                    )
                }
                // NavigationSuite(
                //     // layoutType = if (bottomBarVisible) {
                //     //     navigationSuiteType
                //     // } else {
                //     //     NavigationSuiteType.None
                //     // },
                //     layoutType = navigationSuiteType,
                //     colors = NavigationSuiteDefaults.colors(),
                //     content = {
                //         navSuiteItems(
                //             currentDestination = currentDestination,
                //             showLocalTab = showLocalTab,
                //             onItemClick = onBottomBarItemClick,
                //         )
                //     },
                // )
            }
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            content(PaddingValues(0.dp))
            searchBar()
            if (globalErrors.isNotEmpty()) {
                GlobalErrorsColumn(
                    modifier = Modifier.windowInsetsPadding(topWindowInsets),
                    globalErrors = globalErrors,
                    onFixWallHavenApiKeyClick = onFixWallHavenApiKeyClick,
                    onDismiss = onDismissGlobalError,
                )
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    device = "id:pixel_6_pro",
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
    device = "spec:width=800dp,height=1280dp,dpi=480",
)
@Preview(
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentCustomDpi() {
    PreviewContent()
}

@Preview(
    showSystemUi = true,
    device = "id:desktop_large",
)
@Preview(
    showSystemUi = true,
    device = "id:desktop_large",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentDesktop() {
    PreviewContent()
}

@Preview(
    showSystemUi = true,
    device = "id:8in Foldable",
)
@Preview(
    showSystemUi = true,
    device = "id:8in Foldable",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentFoldable() {
    PreviewContent()
}

@Preview(
    showSystemUi = true,
    device = "id:10.1in WXGA (Tablet)",
)
@Preview(
    showSystemUi = true,
    device = "id:10.1in WXGA (Tablet)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewMainActivityContentTable() {
    PreviewContent()
}

@Composable
private fun PreviewContent() {
    val previewWallpaperFlow = flowOf(
        PagingData.from(
            listOf<Wallpaper>(
                wallhavenWallpaper1,
                wallhavenWallpaper2,
            ),
        ),
    )
    val previewWallhavenTags = List(10) {
        WallhavenTag(
            id = it.toLong(),
            name = "tag_$it",
            alias = emptyList(),
            categoryId = it.toLong(),
            category = "category",
            purity = Purity.SFW,
            createdAt = Clock.System.now(),
        )
    }.toPersistentList()

    WallFlowTheme {
        Surface {
            MainActivityContent(
                globalErrors = listOf(
                    GlobalErrorsRepository.WallHavenUnauthorisedError(),
                ),
            ) {
                val pagingItems = previewWallpaperFlow.collectAsLazyPagingItems()
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {}
                }
                HomeScreenContent(
                    modifier = Modifier.windowInsetsPadding(topWindowInsets),
                    header = {
                        header(
                            sources = persistentListOf(OnlineSource.WALLHAVEN),
                            sourceHeader = {
                                wallhavenHeader(
                                    wallhavenTags = previewWallhavenTags,
                                )
                            },
                        )
                    },
                    wallpapers = pagingItems,
                    nestedScrollConnectionGetter = { nestedScrollConnection },
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
