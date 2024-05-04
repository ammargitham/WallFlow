package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.screens.settings.SettingsType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailContentTopBar(
    isExpanded: Boolean,
    selectedType: SettingsType,
    onBackClick: () -> Unit,
) {
    TopBar(
        title = { DetailTopBarTitle(selectedType) },
        showBackButton = !isExpanded,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        onBackClick = onBackClick,
    )
}

@Composable
private fun DetailTopBarTitle(selectedType: SettingsType) {
    Text(
        text = stringResource(
            when (selectedType) {
                SettingsType.ACCOUNT -> R.string.account
                SettingsType.LOOK_AND_FEEL -> R.string.look_and_feel
                SettingsType.DOWNLOADS -> R.string.downloads
                SettingsType.SAVED_SEARCHES -> R.string.saved_searches
                SettingsType.VIEWED_WALLPAPERS -> R.string.viewed_wallpapers
                SettingsType.OBJECT_DETECTION -> R.string.object_detection
                SettingsType.AUTO_WALLPAPER -> R.string.auto_wallpaper
                SettingsType.CRASH_REPORTS -> R.string.crash_reports
            },
        ),
        maxLines = 1,
    )
}
