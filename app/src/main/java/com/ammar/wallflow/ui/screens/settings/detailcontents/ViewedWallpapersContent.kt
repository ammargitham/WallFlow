package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.screens.settings.composables.viewedWallpapersLookString
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun ViewedWallpapersContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    selectedType: SettingsExtraType? = null,
    enabled: Boolean = false,
    look: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    onEnabledChange: (Boolean) -> Unit = {},
    onViewedWallpapersLookClick: () -> Unit = {},
    onClearClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onEnabledChange(!enabled) },
                isExpanded = isExpanded,
                isFirst = true,
                headlineContent = {
                    Text(text = stringResource(R.string.remember_viewed_wallpapers))
                },
                trailingContent = {
                    Switch(
                        modifier = Modifier.height(24.dp),
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(onClick = onViewedWallpapersLookClick),
                isExpanded = isExpanded,
                isLast = true,
                selected = selectedType == SettingsExtraType.VIEW_WALLPAPERS_LOOK,
                headlineContent = {
                    Text(text = stringResource(R.string.viewed_wallpapers_look))
                },
                supportingContent = { Text(text = viewedWallpapersLookString(look)) },
            )
        }
        item {
            // TODO: Change to normal text button
            Box(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            ) {
                OutlinedButton(onClick = onClearClick) {
                    Text(text = stringResource(R.string.clear))
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewViewedWallpapersContent() {
    WallFlowTheme {
        Surface {
            ViewedWallpapersContent(
                isExpanded = true,
                selectedType = SettingsExtraType.VIEW_WALLPAPERS_LOOK,
            )
        }
    }
}
