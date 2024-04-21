package com.ammar.wallflow.activities.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toPx
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
import com.ammar.wallflow.ui.screens.home.HomeScreenContent
import com.ammar.wallflow.ui.screens.home.composables.header
import com.ammar.wallflow.ui.screens.home.composables.wallhavenHeader
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ramcosta.composedestinations.spec.Route
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

@Composable
fun MainActivityContent(
    modifier: Modifier = Modifier,
    currentDestination: NavDestination? = null,
    useNavRail: Boolean = false,
    globalErrors: List<GlobalError> = emptyList(),
    bottomBarVisible: Boolean = true,
    bottomBarSize: IntSize = IntSize.Zero,
    showLocalTab: Boolean = true,
    searchBar: @Composable () -> Unit = {},
    onFixWallHavenApiKeyClick: () -> Unit = {},
    onDismissGlobalError: (error: GlobalError) -> Unit = {},
    onBottomBarSizeChanged: (size: IntSize) -> Unit = {},
    onBottomBarItemClick: (destination: Route) -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(left = 0),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        start = if (useNavRail && bottomBarVisible) {
                            bottomBarSize.width.toDp()
                        } else {
                            0.dp
                        },
                    ),
            ) {
                content(it)
            }
            if (globalErrors.isNotEmpty()) {
                GlobalErrorsColumn(
                    modifier = Modifier
                        .windowInsetsPadding(topWindowInsets)
                        .padding(
                            start = if (useNavRail) bottomBarSize.width.toDp() else 0.dp,
                        ),
                    globalErrors = globalErrors,
                    onFixWallHavenApiKeyClick = onFixWallHavenApiKeyClick,
                    onDismiss = onDismissGlobalError,
                )
            }
            if (useNavRail) {
                NavRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.TopStart)
                        .onSizeChanged(onBottomBarSizeChanged),
                    currentDestination = currentDestination,
                    showLocalTab = showLocalTab,
                    onItemClick = onBottomBarItemClick,
                )
            } else {
                BottomBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .onSizeChanged(onBottomBarSizeChanged),
                    currentDestination = currentDestination,
                    showLocalTab = showLocalTab,
                    onItemClick = onBottomBarItemClick,
                )
            }
            searchBar()
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
    PreviewContent(
        useNavRail = true,
    )
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
    PreviewContent(
        useNavRail = true,
    )
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
    PreviewContent(
        useNavRail = true,
    )
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
    PreviewContent(
        useNavRail = true,
    )
}

@Composable
private fun PreviewContent(
    useNavRail: Boolean = false,
) {
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
                useNavRail = useNavRail,
                globalErrors = listOf(
                    GlobalErrorsRepository.WallHavenUnauthorisedError(),
                ),
                bottomBarSize = if (useNavRail) {
                    IntSize(80.dp.toPx(), 120)
                } else {
                    IntSize.Zero
                },
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
