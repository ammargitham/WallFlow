package com.ammar.wallflow.ui.screens.more

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.BuildConfig
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun MoreScreenContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isMedium: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onOpenSourceLicensesClick: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(
                    fraction = if (isExpanded) {
                        0.75f
                    } else {
                        1f
                    },
                )
                .align(Alignment.TopCenter),
            contentPadding = if (!isExpanded && isMedium) {
                PaddingValues(24.dp)
            } else {
                PaddingValues(0.dp)
            },
        ) {
            item {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                )
            }
            if (!isExpanded && !isMedium) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = 8.dp,
                        ),
                    )
                }
            }
            item {
                SettingsDetailListItem(
                    modifier = Modifier.clickable(onClick = onSettingsClick),
                    headlineContent = {
                        Text(text = stringResource(R.string.settings))
                    },
                    isExpanded = isExpanded || isMedium,
                    isFirst = true,
                )
            }
            item {
                SettingsDetailListItem(
                    modifier = Modifier.clickable(onClick = onBackupRestoreClick),
                    headlineContent = {
                        Text(text = stringResource(R.string.backup_and_restore))
                    },
                    isExpanded = isExpanded || isMedium,
                    isLast = true,
                )
            }
            if (!isExpanded && !isMedium) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = 8.dp,
                        ),
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.requiredHeight(16.dp))
                }
            }
            item {
                SettingsDetailListItem(
                    modifier = Modifier.clickable(onClick = onOpenSourceLicensesClick),
                    headlineContent = {
                        Text(text = stringResource(R.string.open_source_licenses))
                    },
                    isExpanded = isExpanded || isMedium,
                    isFirst = true,
                )
            }
            item {
                SettingsDetailListItem(
                    headlineContent = {
                        Text(text = stringResource(R.string.version))
                    },
                    supportingContent = {
                        Text(text = BuildConfig.VERSION_NAME)
                    },
                    isExpanded = isExpanded || isMedium,
                    isLast = true,
                )
            }
        }
    }
}

private data class MoreListContainerProps(
    val isExpanded: Boolean,
    val isMedium: Boolean,
    val selectedItemValue: String? = null,
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMoreScreenContent() {
    val props = MoreListContainerProps(
        isExpanded = false,
        isMedium = false,
    )

    WallFlowTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MoreScreenContent(
                isExpanded = props.isExpanded,
            )
        }
    }
}

@Preview(device = Devices.FOLDABLE)
@Preview(device = Devices.FOLDABLE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMoreScreenContentFoldable() {
    val props = MoreListContainerProps(
        isExpanded = false,
        isMedium = true,
        selectedItemValue = ActiveOption.SETTINGS.name,
    )

    WallFlowTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MoreScreenContent(
                isExpanded = props.isExpanded,
                isMedium = props.isMedium,
            )
        }
    }
}

@Preview(device = Devices.TABLET)
@Preview(device = Devices.TABLET, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMoreScreenContentTablet() {
    val props = MoreListContainerProps(
        isExpanded = true,
        isMedium = false,
        selectedItemValue = ActiveOption.SETTINGS.name,
    )

    WallFlowTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MoreScreenContent(
                isExpanded = props.isExpanded,
            )
        }
    }
}
