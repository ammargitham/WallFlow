package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoWallpaperSetToDialog(
    modifier: Modifier = Modifier,
    selectedTargets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
    onSaveClick: (targets: Set<WallpaperTarget>) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSelectedTargets by remember(selectedTargets) {
        mutableStateOf(selectedTargets)
    }

    fun toggleTarget(target: WallpaperTarget) {
        localSelectedTargets = if (target !in localSelectedTargets) {
            localSelectedTargets + target
        } else {
            localSelectedTargets - target
        }
    }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.set_to)) },
            text = {
                Column {
                    WallpaperTargetItem(
                        target = WallpaperTarget.HOME,
                        localSelectedTargets = localSelectedTargets,
                        onClick = { toggleTarget(WallpaperTarget.HOME) },
                    )
                    WallpaperTargetItem(
                        target = WallpaperTarget.LOCKSCREEN,
                        localSelectedTargets = localSelectedTargets,
                        onClick = { toggleTarget(WallpaperTarget.LOCKSCREEN) },
                    )
                }
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = { onSaveClick(localSelectedTargets) }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun WallpaperTargetItem(
    target: WallpaperTarget,
    localSelectedTargets: Set<WallpaperTarget>,
    onClick: () -> Unit = {},
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        headlineContent = {
            Text(
                text = stringResource(
                    when (target) {
                        WallpaperTarget.HOME -> R.string.home_screen
                        WallpaperTarget.LOCKSCREEN -> R.string.lock_screen
                    },
                ),
            )
        },
        leadingContent = {
            Checkbox(
                modifier = Modifier.size(24.dp),
                enabled = localSelectedTargets.size > 1 || target !in localSelectedTargets,
                checked = target in localSelectedTargets,
                onCheckedChange = { onClick() },
            )
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAutoWallpaperSetToDialog() {
    WallFlowTheme {
        Surface {
            AutoWallpaperSetToDialog()
        }
    }
}
