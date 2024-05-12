package com.ammar.wallflow.ui.screens.backuprestore

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun BackupRestoreScreenContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onBackupClicked: () -> Unit = {},
    onRestoreClicked: () -> Unit = {},
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
            contentPadding = if (isExpanded) {
                PaddingValues(vertical = 24.dp)
            } else {
                PaddingValues(0.dp)
            },
        ) {
            item {
                SettingsDetailListItem(
                    modifier = Modifier.clickable(onClick = onBackupClicked),
                    headlineContent = {
                        Text(text = stringResource(R.string.backup))
                    },
                    isExpanded = isExpanded,
                    isFirst = true,
                )
            }
            item {
                SettingsDetailListItem(
                    modifier = Modifier.clickable(onClick = onRestoreClicked),
                    headlineContent = {
                        Text(text = stringResource(R.string.restore))
                    },
                    isExpanded = isExpanded,
                    isLast = true,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBackupRestoreScreenContent() {
    WallFlowTheme {
        Surface {
            BackupRestoreScreenContent()
        }
    }
}

@Preview(device = Devices.PIXEL_TABLET)
@Preview(device = Devices.PIXEL_TABLET, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBackupRestoreScreenContentTablet() {
    WallFlowTheme {
        Surface {
            BackupRestoreScreenContent(
                isExpanded = true,
            )
        }
    }
}
