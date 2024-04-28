package com.ammar.wallflow.ui.screens.more

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.BuildConfig
import com.ammar.wallflow.R
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
    val context = LocalContext.current
    Box(
        modifier = modifier,
    ) {
        val items = remember {
            listOf(
                MoreListItem.Content {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                    )
                },
                MoreListItem.Divider,
                MoreListItem.Clickable(
                    icon = R.drawable.baseline_settings_24,
                    label = context.getString(R.string.settings),
                    value = ActiveOption.SETTINGS.name,
                ),
                MoreListItem.Clickable(
                    icon = R.drawable.baseline_settings_backup_restore_24,
                    label = context.getString(R.string.backup_and_restore),
                    value = ActiveOption.BACKUP_RESTORE.name,
                ),
                MoreListItem.Divider,
                MoreListItem.Clickable(
                    label = context.getString(R.string.open_source_licenses),
                    value = ActiveOption.OSL.name,
                ),
                MoreListItem.Static(
                    label = context.getString(R.string.version),
                    supportingText = BuildConfig.VERSION_NAME,
                ),
            )
        }
        MoreList(
            isExpanded = isExpanded,
            isMedium = isMedium,
            items = items,
            onItemClick = {
                when (it.value) {
                    ActiveOption.SETTINGS.name -> onSettingsClick()
                    ActiveOption.BACKUP_RESTORE.name -> onBackupRestoreClick()
                    ActiveOption.OSL.name -> onOpenSourceLicensesClick()
                    else -> {}
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMoreScreenContent() {
    WallFlowTheme {
        Surface {
            MoreScreenContent()
        }
    }
}
