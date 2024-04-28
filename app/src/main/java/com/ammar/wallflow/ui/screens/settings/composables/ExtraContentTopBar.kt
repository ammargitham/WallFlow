package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType.AUTO_WALLPAPER_SOURCES
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType.LAYOUT
import com.ammar.wallflow.ui.screens.settings.SettingsExtraType.VIEW_WALLPAPERS_LOOK

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExtraContentTopBar(
    isExpanded: Boolean,
    selectedExtraType: SettingsExtraType?,
    onBackClick: () -> Unit = {},
) {
    TopBar(
        title = { ExtraTopBarTitle(selectedExtraType) },
        showBackButton = !isExpanded || selectedExtraType != null,
        backIcon = if (isExpanded) {
            { CloseIcon(onClick = onBackClick) }
        } else {
            null
        },
        onBackClick = onBackClick,
    )
}

@Composable
private fun ExtraTopBarTitle(selectedExtraType: SettingsExtraType?) {
    Text(
        text = selectedExtraType?.let {
            stringResource(
                when (it) {
                    LAYOUT -> R.string.layout
                    VIEW_WALLPAPERS_LOOK -> R.string.viewed_wallpapers_look
                    AUTO_WALLPAPER_SOURCES -> R.string.sources
                },
            )
        } ?: "",
        maxLines = 1,
    )
}

@Composable
private fun CloseIcon(
    onClick: () -> Unit = {},
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.baseline_close_24),
            contentDescription = stringResource(R.string.close),
        )
    }
}
