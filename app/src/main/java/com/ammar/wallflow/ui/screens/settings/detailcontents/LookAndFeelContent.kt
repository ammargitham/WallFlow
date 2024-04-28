package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun LookAndFeelContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    selectedType: SettingsExtraType? = null,
    blurSketchy: Boolean = false,
    blurNsfw: Boolean = false,
    showLocalTab: Boolean = true,
    onThemeClick: () -> Unit = {},
    onLayoutClick: () -> Unit = {},
    onBlurSketchyCheckChange: (checked: Boolean) -> Unit = {},
    onBlurNsfwCheckChange: (checked: Boolean) -> Unit = {},
    onShowLocalTabChange: (Boolean) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(onClick = onThemeClick),
                isExpanded = isExpanded,
                isFirst = true,
                headlineContent = { Text(text = stringResource(R.string.theme)) },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(onClick = onLayoutClick),
                isExpanded = isExpanded,
                selected = selectedType == SettingsExtraType.LAYOUT,
                headlineContent = { Text(text = stringResource(R.string.layout)) },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onBlurSketchyCheckChange(!blurSketchy) },
                isExpanded = isExpanded,
                headlineContent = { Text(text = stringResource(R.string.blur_sketchy_wallpapers)) },
                trailingContent = {
                    Switch(
                        modifier = Modifier.height(24.dp),
                        checked = blurSketchy,
                        onCheckedChange = onBlurSketchyCheckChange,
                    )
                },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onBlurNsfwCheckChange(!blurNsfw) },
                isExpanded = isExpanded,
                headlineContent = { Text(text = stringResource(R.string.blur_nsfw_wallpapers)) },
                trailingContent = {
                    Switch(
                        modifier = Modifier.height(24.dp),
                        checked = blurNsfw,
                        onCheckedChange = onBlurNsfwCheckChange,
                    )
                },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onShowLocalTabChange(!showLocalTab) },
                isExpanded = isExpanded,
                isLast = true,
                headlineContent = { Text(text = stringResource(R.string.show_local_tab)) },
                trailingContent = {
                    Switch(
                        modifier = Modifier.height(24.dp),
                        checked = showLocalTab,
                        onCheckedChange = onShowLocalTabChange,
                    )
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLookAndFeelContent() {
    WallFlowTheme {
        Surface {
            LookAndFeelContent()
        }
    }
}
