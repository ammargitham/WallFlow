package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.R
import com.ammar.wallflow.model.DeviceOrientation
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.screens.settings.composables.getOrientationString
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun DeviceSettingsContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    currentOrientation: DeviceOrientation = DeviceOrientation.Vertical,
    defaultOrientation: DeviceOrientation = DeviceOrientation.Vertical,
    onDefaultOrientationClick: () -> Unit = {},
) {
    LazyColumn {
        item {
            SettingsDetailListItem(
                modifier = modifier.clickable(onClick = onDefaultOrientationClick),
                isExpanded = isExpanded,
                isFirst = true,
                isLast = true,
                headlineContent = { Text(text = stringResource(R.string.default_orientation)) },
                supportingContent = {
                    Text(
                        text = getOrientationString(
                            orientation = defaultOrientation,
                            current = currentOrientation,
                        ),
                    )
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDeviceContent() {
    WallFlowTheme {
        Surface {
            DeviceSettingsContent()
        }
    }
}
