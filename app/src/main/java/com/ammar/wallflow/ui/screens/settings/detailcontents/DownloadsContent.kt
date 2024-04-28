package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.ExifWriteType

@Composable
internal fun DownloadsContent(
    modifier: Modifier = Modifier,
    downloadLocation: String = "",
    writeTagsToExif: Boolean = false,
    tagsExifWriteType: ExifWriteType = ExifWriteType.APPEND,
    isExpanded: Boolean = false,
    onDownloadLocationClick: () -> Unit = {},
    onWriteTagsToExifCheckChange: (checked: Boolean) -> Unit = {},
    onTagsWriteTypeClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(onClick = onDownloadLocationClick),
                isExpanded = isExpanded,
                isFirst = true,
                headlineContent = { Text(text = stringResource(R.string.download_location)) },
                supportingContent = { Text(text = downloadLocation) },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onWriteTagsToExifCheckChange(!writeTagsToExif) },
                isExpanded = isExpanded,
                isLast = !writeTagsToExif,
                headlineContent = {
                    Text(text = stringResource(R.string.write_tags_to_exif))
                },
                supportingContent = {
                    Text(text = stringResource(R.string.write_tags_to_exif_desc))
                },
                trailingContent = {
                    Switch(
                        modifier = Modifier.height(24.dp),
                        checked = writeTagsToExif,
                        onCheckedChange = onWriteTagsToExifCheckChange,
                    )
                },
            )
        }
        item {
            AnimatedVisibility(
                modifier = Modifier.clipToBounds(),
                visible = writeTagsToExif,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                label = "EXIF write type",
            ) {
                SettingsDetailListItem(
                    modifier = Modifier.clickable(onClick = onTagsWriteTypeClick),
                    isExpanded = isExpanded,
                    isLast = true,
                    headlineContent = {
                        Text(text = stringResource(R.string.exif_write_type))
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(
                                when (tagsExifWriteType) {
                                    ExifWriteType.APPEND -> R.string.append
                                    ExifWriteType.OVERWRITE -> R.string.overwrite
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDownloadsContent() {
    WallFlowTheme {
        Surface {
            DownloadsContent()
        }
    }
}
