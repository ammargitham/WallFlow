package com.ammar.wallflow.ui.screens.wallpaper

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenAvatar
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.ui.common.ProgressIndicator
import com.ammar.wallflow.ui.common.TagChip
import com.ammar.wallflow.ui.common.UploaderChip
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.DownloadStatus
import kotlinx.datetime.Clock

@Composable
fun WallpaperActions(
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus? = null,
    showApplyWallpaperAction: Boolean = true,
    showFullScreenAction: Boolean = false,
    showDownloadAction: Boolean = true,
    isFavorite: Boolean = false,
    lightDarkTypeFlags: Int = LightDarkType.UNSPECIFIED,
    onDownloadClick: () -> Unit = {},
    onApplyWallpaperClick: () -> Unit = {},
    onFullScreenClick: () -> Unit = {},
    onFavoriteToggle: (Boolean) -> Unit = {},
    onLightDarkTypeFlagsChange: (Int) -> Unit = {},
    onShowLightDarkInfoClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = BottomAppBarDefaults.containerColor.copy(alpha = 0.8f),
        actions = {
            if (showFullScreenAction) {
                FullScreenButton(onClick = onFullScreenClick)
            }
            InfoButton(onClick = onInfoClick)
            FavoriteButton(
                isFavorite = isFavorite,
                onToggle = onFavoriteToggle,
            )
            if (showDownloadAction) {
                DownloadButton(
                    downloadStatus = downloadStatus,
                    onClick = onDownloadClick,
                )
            }
            LightDarkButton(
                typeFlags = lightDarkTypeFlags,
                onFlagsChange = onLightDarkTypeFlagsChange,
                onShowLightDarkInfoClick = onShowLightDarkInfoClick,
            )
        },
        floatingActionButton = if (showApplyWallpaperAction) {
            {
                ApplyWallpaperFAB(onClick = onApplyWallpaperClick)
            }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyWallpaperFAB(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.apply_wallpaper))
            }
        },
    ) {
        FloatingActionButton(
            modifier = modifier,
            onClick = onClick,
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_wallpaper_24),
                contentDescription = stringResource(R.string.apply_wallpaper),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.full_screen))
            }
        },
    ) {
        IconButton(
            modifier = modifier,
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_open_in_full_24),
                contentDescription = stringResource(R.string.full_screen),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.info))
            }
        },
    ) {
        IconButton(
            onClick = onClick,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.info),
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWallpaperActions() {
    WallFlowTheme {
        Surface {
            WallpaperActions(
                downloadStatus = DownloadStatus.Running(
                    downloadedBytes = 50,
                    totalBytes = 100,
                ),
                showFullScreenAction = true,
                isFavorite = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadButton(
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus? = null,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val progress by animateFloatAsState(
        when (downloadStatus) {
            is DownloadStatus.Running -> downloadStatus.progress
            is DownloadStatus.Paused -> downloadStatus.progress
            else -> -1F
        },
        label = "progress",
    )
    val clickable = remember(downloadStatus) {
        (
            downloadStatus == null ||
                downloadStatus is DownloadStatus.Failed ||
                downloadStatus is DownloadStatus.Success
            )
    }
    val showProgress = remember(downloadStatus) {
        (
            downloadStatus is DownloadStatus.Running ||
                downloadStatus is DownloadStatus.Paused ||
                downloadStatus is DownloadStatus.Pending
            )
    }

    val defaultContainerColor = Color.Transparent
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val containerColor by animateColorAsState(
        when (downloadStatus) {
            is DownloadStatus.Failed -> errorContainerColor
            else -> defaultContainerColor
        },
        label = "containerColor",
    )

    val icon = remember(downloadStatus) {
        when (downloadStatus) {
            is DownloadStatus.Paused -> R.drawable.baseline_pause_24
            is DownloadStatus.Failed -> R.drawable.baseline_error_outline_24
            is DownloadStatus.Success -> R.drawable.outline_file_download_done_24
            else -> R.drawable.outline_file_download_24
        }
    }

    val iconContentDescription = stringResource(
        when (downloadStatus) {
            is DownloadStatus.Paused -> R.string.paused
            is DownloadStatus.Failed -> R.string.failed
            is DownloadStatus.Success -> R.string.success
            else -> R.string.download
        },
    )

    TooltipBox(
        modifier = modifier,
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.download))
            }
        },
    ) {
        IconButton(
            modifier = modifier.semantics {
                contentDescription = context.getString(R.string.download)
            },
            onClick = if (clickable) {
                onClick
            } else {
                {}
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
                contentColor = contentColorFor(containerColor),
            ),
        ) {
            Crossfade(
                targetState = icon,
                label = "iconCrossfade",
            ) {
                Icon(
                    painter = painterResource(it),
                    contentDescription = iconContentDescription,
                )
            }
        }
        if (showProgress) {
            ProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                progress = progress,
            )
        }
    }
}

private class DownloadStatusParameterProvider :
    CollectionPreviewParameterProvider<DownloadStatus?>(
        listOf(
            null,
            DownloadStatus.Pending,
            DownloadStatus.Paused(20, 50),
            DownloadStatus.Running(30, 50),
            DownloadStatus.Failed(),
            DownloadStatus.Success(),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDownloadButton(
    @PreviewParameter(DownloadStatusParameterProvider::class) downloadStatus: DownloadStatus?,
) {
    WallFlowTheme {
        Surface {
            DownloadButton(
                downloadStatus = downloadStatus,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareButton(
    modifier: Modifier = Modifier,
    showShareLinkAction: Boolean = true,
    onLinkClick: () -> Unit = {},
    onImageClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    TooltipBox(
        modifier = modifier.wrapContentSize(Alignment.TopStart),
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.share))
            }
        },
    ) {
        IconButton(
            modifier = modifier.wrapContentSize(Alignment.TopStart),
            onClick = {
                if (!showShareLinkAction) {
                    onImageClick()
                    return@IconButton
                }
                expanded = true
            },
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.share),
            )
        }
        DropdownMenu(
            modifier = Modifier
                .widthIn(min = 150.dp)
                .semantics {
                    contentDescription = context.getString(R.string.menu)
                },
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.link)) },
                onClick = {
                    expanded = false
                    onLinkClick()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_link_24),
                        contentDescription = null,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.image)) },
                onClick = {
                    expanded = false
                    onImageClick()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_image_24),
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoriteButton(
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(text = stringResource(R.string.favorite))
            }
        },
    ) {
        IconToggleButton(
            modifier = Modifier.testTag(
                if (isFavorite) {
                    "baseline_favorite_24"
                } else {
                    "outline_favorite_border_24"
                },
            ),
            checked = isFavorite,
            colors = IconButtonDefaults.iconToggleButtonColors(
                checkedContentColor = Color.Red,
            ),
            onCheckedChange = onToggle,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(
                    if (isFavorite) {
                        R.drawable.baseline_favorite_24
                    } else {
                        R.drawable.outline_favorite_border_24
                    },
                ),
                contentDescription = stringResource(R.string.favorite),
            )
        }
    }
}

@Composable
fun PropertyRow(
    modifier: Modifier = Modifier,
    title: String,
    text: String,
) {
    PropertyRow(modifier = modifier, title = title) {
        Text(
            modifier = Modifier
                .alignByBaseline()
                .weight(3f),
            text = text,
        )
    }
}

@Composable
fun PropertyRow(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .alignByBaseline()
                .weight(1f),
            text = title,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(16.dp))
        content()
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPropertyRow() {
    WallFlowTheme {
        Surface {
            PropertyRow(
                title = "Title",
                text = "Subtitle",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsRow(
    modifier: Modifier = Modifier,
    wallhavenTags: List<WallhavenTag>,
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onTagLongClick: (wallhavenTag: WallhavenTag) -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        wallhavenTags.map {
            TagChip(
                wallhavenTag = it,
                onClick = { onTagClick(it) },
                onLongClick = { onTagLongClick(it) },
            )
        }
    }
}

@Preview(widthDp = 200)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 200)
@Composable
private fun PreviewTagsRow() {
    WallFlowTheme {
        Surface {
            TagsRow(
                wallhavenTags = listOf(
                    WallhavenTag(
                        id = 1,
                        name = "tag1",
                        alias = listOf("tag1"),
                        categoryId = 1,
                        category = "category1",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                    WallhavenTag(
                        id = 2,
                        name = "tag2",
                        alias = listOf("tag2"),
                        categoryId = 2,
                        category = "category2",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                    WallhavenTag(
                        id = 3,
                        name = "tag3",
                        alias = listOf("tag3"),
                        categoryId = 3,
                        category = "category3",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                    WallhavenTag(
                        id = 4,
                        name = "tag4",
                        alias = listOf("tag4"),
                        categoryId = 4,
                        category = "category4",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                ),
            )
        }
    }
}

@Composable
fun ColorBox(
    modifier: Modifier = Modifier,
    color: Color,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color,
                shape = CircleShape,
            ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorsRow(
    modifier: Modifier = Modifier,
    colors: List<Color>,
) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .padding(top = 6.dp)
                .weight(1f),
            text = stringResource(R.string.colors),
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        FlowRow(
            modifier = modifier.weight(3f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.map { ColorBox(color = it) }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewColorsRow() {
    WallFlowTheme {
        Surface {
            ColorsRow(
                colors = listOf(
                    Color.Blue,
                    Color.Black,
                    Color.Cyan,
                ),
            )
        }
    }
}

@Composable
fun WallhavenUploaderRow(
    modifier: Modifier = Modifier,
    wallhavenUploader: WallhavenUploader,
    onClick: () -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f),
            text = stringResource(R.string.uploader),
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        Row(
            modifier = Modifier.weight(3f),
        ) {
            UploaderChip(
                wallhavenUploader = wallhavenUploader,
                onClick = onClick,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUploaderRow() {
    WallFlowTheme {
        Surface {
            WallhavenUploaderRow(
                wallhavenUploader = WallhavenUploader(
                    username = "test",
                    group = "",
                    avatar = WallhavenAvatar(
                        large = "",
                        medium = "",
                        small = "",
                        tiny = "",
                    ),
                ),
            )
        }
    }
}
