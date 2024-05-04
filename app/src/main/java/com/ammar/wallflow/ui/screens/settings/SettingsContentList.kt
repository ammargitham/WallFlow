package com.ammar.wallflow.ui.screens.settings

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.settingsListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.objectdetection.objectsDetector

@Composable
fun SettingsContentList(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    selectedType: SettingsType = SettingsType.ACCOUNT,
    hasSetWallpaperPermission: Boolean = true,
    onItemClick: (SettingsType) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = (if (isExpanded) 16 else 0).dp,
            vertical = 8.dp,
        ),
    ) {
        mainSettingsListItem(
            type = SettingsType.ACCOUNT,
            labelRes = R.string.account,
            iconRes = R.drawable.baseline_person_24,
            isExpanded = isExpanded,
            selectedType = selectedType,
            onItemClick = onItemClick,
        )
        mainSettingsListItem(
            type = SettingsType.LOOK_AND_FEEL,
            labelRes = R.string.look_and_feel,
            iconRes = R.drawable.baseline_format_paint_24,
            isExpanded = isExpanded,
            selectedType = selectedType,
            onItemClick = onItemClick,
        )
        mainSettingsListItem(
            type = SettingsType.DOWNLOADS,
            labelRes = R.string.downloads,
            iconRes = R.drawable.baseline_download_24,
            isExpanded = isExpanded,
            selectedType = selectedType,
            onItemClick = onItemClick,
        )
        mainSettingsListItem(
            type = SettingsType.SAVED_SEARCHES,
            labelRes = R.string.saved_searches,
            iconRes = R.drawable.baseline_manage_search_24,
            isExpanded = isExpanded,
            selectedType = selectedType,
            onItemClick = onItemClick,
        )
        mainSettingsListItem(
            type = SettingsType.VIEWED_WALLPAPERS,
            labelRes = R.string.viewed_wallpapers,
            iconRes = R.drawable.baseline_visibility_24,
            isExpanded = isExpanded,
            selectedType = selectedType,
            onItemClick = onItemClick,
        )
        if (objectsDetector.isEnabled) {
            mainSettingsListItem(
                type = SettingsType.OBJECT_DETECTION,
                labelRes = R.string.object_detection,
                iconRes = R.drawable.tensorflow,
                isExpanded = isExpanded,
                selectedType = selectedType,
                onItemClick = onItemClick,
            )
        }
        if (hasSetWallpaperPermission) {
            mainSettingsListItem(
                type = SettingsType.AUTO_WALLPAPER,
                labelRes = R.string.auto_wallpaper,
                iconRes = R.drawable.image_sync_outline,
                isExpanded = isExpanded,
                selectedType = selectedType,
                onItemClick = onItemClick,
            )
        }
        mainSettingsListItem(
            type = SettingsType.CRASH_REPORTS,
            labelRes = R.string.crash_reports,
            iconRes = R.drawable.baseline_bug_report_24,
            isExpanded = isExpanded,
            selectedType = selectedType,
            onItemClick = onItemClick,
        )
    }
}

private fun LazyListScope.mainSettingsListItem(
    type: SettingsType,
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    isExpanded: Boolean,
    selectedType: SettingsType,
    onItemClick: (SettingsType) -> Unit,
    modifier: Modifier = Modifier,
) {
    settingsListItem(
        modifier = modifier,
        labelRes = labelRes,
        iconRes = iconRes,
        isExpanded = isExpanded,
        selected = selectedType == type,
        onClick = { onItemClick(type) },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSettingsContentList() {
    WallFlowTheme {
        Surface {
            SettingsContentList(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
