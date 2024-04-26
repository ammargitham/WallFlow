package com.ammar.wallflow.ui.screens.wallpaper

import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.capitalise
import com.ammar.wallflow.extensions.openUrl
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.model.reddit.RedditWallpaper
import com.ammar.wallflow.model.reddit.redditWallpaper1
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper1
import com.ammar.wallflow.ui.common.AdaptiveBottomSheet
import com.ammar.wallflow.ui.common.rememberAdaptiveBottomSheetState
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.getRealPath
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperInfoBottomSheet(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    wallpaper: Wallpaper,
    onDismissRequest: () -> Unit = {},
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onUploaderClick: () -> Unit = {},
    onSourceClick: () -> Unit = {},
    onSourceLongClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val adaptiveBottomSheetState = rememberAdaptiveBottomSheetState(
        bottomSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
    )

    fun dismissSheet() {
        coroutineScope.launch { adaptiveBottomSheetState.hide() }.invokeOnCompletion {
            if (!adaptiveBottomSheetState.isVisible) {
                onDismissRequest()
            }
        }
    }

    AdaptiveBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = adaptiveBottomSheetState,
    ) {
        WallpaperInfoBottomSheetContent(
            modifier = contentModifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                )
                .heightIn(min = 100.dp),
            wallpaper = wallpaper,
            onTagClick = {
                onTagClick(it)
                dismissSheet()
            },
            onUploaderClick = {
                onUploaderClick()
                dismissSheet()
            },
            onSourceClick = {
                onSourceClick()
                dismissSheet()
            },
            onSourceLongClick = onSourceLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperInfoBottomSheetContent(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper,
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onUploaderClick: () -> Unit = {},
    onSourceClick: () -> Unit = {},
    onSourceLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sourceUrl = getSource(wallpaper)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (wallpaper is WallhavenWallpaper &&
            !wallpaper.tags.isNullOrEmpty()
        ) {
            TagsRow(
                modifier = Modifier.fillMaxWidth(),
                wallhavenTags = wallpaper.tags,
                onTagClick = onTagClick,
                onTagLongClick = {
                    clipboardManager.setText(AnnotatedString("#${it.name}"))
                    context.toast(context.getString(R.string.tag_copied))
                },
            )
        }

        if (wallpaper is WallhavenWallpaper &&
            wallpaper.colors.isNotEmpty()
        ) {
            ColorsRow(
                modifier = Modifier.fillMaxWidth(),
                colors = wallpaper.colors,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (wallpaper is LocalWallpaper) {
                PropertyRow(title = stringResource(R.string.location)) {
                    Box(
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(3f),
                    ) {
                        val realPath = getRealPath(context, wallpaper.data)
                        Text(
                            text = if (realPath != null) {
                                "$realPath/${wallpaper.name}"
                            } else {
                                wallpaper.data.toString()
                            },
                        )
                    }
                }
            }
            if (!sourceUrl.isNullOrEmpty()) {
                PropertyRow(title = stringResource(R.string.source)) {
                    Box(
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(3f),
                    ) {
                        Text(
                            modifier = Modifier.combinedClickable(
                                onClick = onSourceClick,
                                onLongClick = onSourceLongClick,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            text = sourceUrl,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (wallpaper is DownloadableWallpaper) {
                PropertyRow(title = stringResource(R.string.image_url)) {
                    Box(
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(3f),
                    ) {
                        Text(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    context.openUrl(wallpaper.data)
                                },
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(wallpaper.data))
                                    context.toast(context.getString(R.string.url_copied))
                                },
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            text = wallpaper.data,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // reddit post title
            if (wallpaper is RedditWallpaper) {
                PropertyRow(
                    title = stringResource(R.string.title),
                    text = wallpaper.postTitle,
                )
            }
            // uploader or author
            when (wallpaper) {
                is WallhavenWallpaper -> {
                    if (wallpaper.uploader != null) {
                        WallhavenUploaderRow(
                            wallhavenUploader = wallpaper.uploader,
                            onClick = onUploaderClick,
                        )
                    }
                }
                is RedditWallpaper -> {
                    if (wallpaper.author.isNotEmpty()) {
                        PropertyRow(
                            title = stringResource(R.string.user),
                            text = "/u/${wallpaper.author}",
                        )
                    }
                }
            }
            if (wallpaper is WallhavenWallpaper) {
                PropertyRow(title = stringResource(R.string.category)) {
                    Box(
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(3f),
                    ) {
                        Text(
                            text = wallpaper.category.capitalise(),
                        )
                    }
                }
            }
            PropertyRow(
                title = stringResource(R.string.resolution),
                text = wallpaper.resolution.toString(),
            )
            if (wallpaper.fileSize > 0) {
                PropertyRow(
                    title = stringResource(R.string.size),
                    text = Formatter.formatShortFileSize(context, wallpaper.fileSize),
                )
            }
            if (wallpaper is WallhavenWallpaper) {
                PropertyRow(
                    title = stringResource(R.string.views),
                    text = wallpaper.views.toString(),
                )
                PropertyRow(
                    title = stringResource(R.string.favorites),
                    text = wallpaper.favorites.toString(),
                )
            }
        }
    }
}

private fun getSource(wallpaper: Wallpaper) = when (wallpaper) {
    is WallhavenWallpaper -> wallpaper.wallhavenSource
    is RedditWallpaper -> wallpaper.postUrl
    else -> null
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWallhavenWallpaperInfoBottomSheetContent() {
    WallFlowTheme {
        Surface {
            WallpaperInfoBottomSheetContent(
                modifier = Modifier.padding(16.dp),
                wallpaper = wallhavenWallpaper1,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRedditWallpaperInfoBottomSheetContent() {
    WallFlowTheme {
        Surface {
            WallpaperInfoBottomSheetContent(
                modifier = Modifier.padding(16.dp),
                wallpaper = redditWallpaper1,
            )
        }
    }
}
