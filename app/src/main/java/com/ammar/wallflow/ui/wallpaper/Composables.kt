package com.ammar.wallflow.ui.wallpaper

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberPlainTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.Avatar
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Tag
import com.ammar.wallflow.model.Uploader
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
    applyWallpaperEnabled: Boolean = true,
    showFullScreenAction: Boolean = false,
    onInfoClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onShareLinkClick: () -> Unit = {},
    onShareImageClick: () -> Unit = {},
    onApplyWallpaperClick: () -> Unit = {},
    onFullScreenClick: () -> Unit = {},
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = BottomAppBarDefaults.containerColor.copy(alpha = 0.8f),
        actions = {
            if (showFullScreenAction) {
                FullScreenButton(onClick = onFullScreenClick)
            }
            InfoButton(onClick = onInfoClick)
            DownloadButton(
                downloadStatus = downloadStatus,
                onClick = onDownloadClick,
            )
            ShareButton(
                onLinkClick = onShareLinkClick,
                onImageClick = onShareImageClick,
            )
        },
        floatingActionButton = if (applyWallpaperEnabled) {
            {
                ApplyWallpaperFAB(onClick = onApplyWallpaperClick)
            }
        } else null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyWallpaperFAB(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val tooltipState = rememberPlainTooltipState()

    PlainTooltipBox(
        modifier = modifier,
        tooltip = { Text(text = stringResource(R.string.apply_wallpaper)) },
        tooltipState = tooltipState,
    ) {
        FloatingActionButton(
            modifier = Modifier.tooltipTrigger(),
            onClick = onClick,
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
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
    val tooltipState = rememberPlainTooltipState()

    PlainTooltipBox(
        modifier = modifier,
        tooltip = { Text(text = stringResource(R.string.full_screen)) },
        tooltipState = tooltipState,
    ) {
        IconButton(
            modifier = Modifier.tooltipTrigger(),
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_open_in_full_24),
                contentDescription = stringResource(R.string.full_screen)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val tooltipState = rememberPlainTooltipState()

    PlainTooltipBox(
        modifier = modifier,
        tooltip = { Text(text = stringResource(R.string.info)) },
        tooltipState = tooltipState,
    ) {
        IconButton(
            modifier = Modifier.tooltipTrigger(),
            onClick = onClick,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.info)
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
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadButton(
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus? = null,
    onClick: () -> Unit = {},
) {
    val tooltipState = rememberPlainTooltipState()
    val progress by animateFloatAsState(
        when (downloadStatus) {
            is DownloadStatus.Running -> downloadStatus.progress
            is DownloadStatus.Paused -> downloadStatus.progress
            else -> -1F
        }
    )
    val clickable = remember(downloadStatus) {
        (downloadStatus == null
                || downloadStatus is DownloadStatus.Failed
                || downloadStatus is DownloadStatus.Success)
    }
    val showProgress = remember(downloadStatus) {
        (downloadStatus is DownloadStatus.Running
                || downloadStatus is DownloadStatus.Paused
                || downloadStatus is DownloadStatus.Pending)
    }

    val defaultContainerColor = Color.Transparent
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val containerColor by animateColorAsState(
        when (downloadStatus) {
            is DownloadStatus.Failed -> errorContainerColor
            else -> defaultContainerColor
        }
    )

    val icon = remember(downloadStatus) {
        when (downloadStatus) {
            is DownloadStatus.Paused -> R.drawable.baseline_pause_24
            is DownloadStatus.Failed -> R.drawable.baseline_error_outline_24
            is DownloadStatus.Success -> R.drawable.outline_file_download_done_24
            else -> R.drawable.outline_file_download_24
        }
    }

    PlainTooltipBox(
        modifier = modifier,
        tooltip = { Text(text = stringResource(R.string.download)) },
        tooltipState = tooltipState,
    ) {
        IconButton(
            modifier = Modifier.tooltipTrigger(),
            onClick = if (clickable) onClick else {
                {}
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
                contentColor = contentColorFor(containerColor)
            ),
        ) {
            Crossfade(targetState = icon) {
                Icon(
                    painter = painterResource(it),
                    contentDescription = stringResource(R.string.download)
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
        )
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
private fun ShareButton(
    modifier: Modifier = Modifier,
    onLinkClick: () -> Unit = {},
    onImageClick: () -> Unit = {},
) {
    val tooltipState = rememberPlainTooltipState()
    var expanded by remember { mutableStateOf(false) }

    PlainTooltipBox(
        modifier = modifier.wrapContentSize(Alignment.TopStart),
        tooltip = { Text(text = stringResource(R.string.share)) },
        tooltipState = tooltipState,
    ) {
        IconButton(
            modifier = Modifier.tooltipTrigger(),
            onClick = { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.share),
            )
        }
        DropdownMenu(
            modifier = Modifier.widthIn(min = 150.dp),
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
                text = "Subtitle"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsRow(
    modifier: Modifier = Modifier,
    tags: List<Tag>,
    onTagClick: (tag: Tag) -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        tags.map {
            TagChip(
                tag = it,
                onClick = { onTagClick(it) },
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
                tags = listOf(
                    Tag(
                        id = 1,
                        name = "tag1",
                        alias = listOf("tag1"),
                        categoryId = 1,
                        category = "category1",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                    Tag(
                        id = 2,
                        name = "tag2",
                        alias = listOf("tag2"),
                        categoryId = 2,
                        category = "category2",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                    Tag(
                        id = 3,
                        name = "tag3",
                        alias = listOf("tag3"),
                        categoryId = 3,
                        category = "category3",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                    Tag(
                        id = 4,
                        name = "tag4",
                        alias = listOf("tag4"),
                        categoryId = 4,
                        category = "category4",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    ),
                )
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
            text = "Colors",
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
                )
            )
        }
    }
}

@Composable
fun UploaderRow(
    modifier: Modifier = Modifier,
    uploader: Uploader,
    onClick: () -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f),
            text = "Uploaded by",
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        Row(
            modifier = Modifier.weight(3f),
        ) {
            UploaderChip(
                uploader = uploader,
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
            UploaderRow(
                uploader = Uploader(
                    username = "test",
                    group = "",
                    avatar = Avatar(
                        large = "",
                        medium = "",
                        small = "",
                        tiny = "",
                    )
                )
            )
        }
    }
}
