package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun CrashReportsContent(
    modifier: Modifier = Modifier,
    acraEnabled: Boolean = false,
    isExpanded: Boolean = false,
    onAcraEnabledChange: (Boolean) -> Unit = {},
) {
    Column(modifier = modifier) {
        SettingsDetailListItem(
            modifier = Modifier.clickable { onAcraEnabledChange(!acraEnabled) },
            isExpanded = isExpanded,
            isFirst = true,
            isLast = true,
            headlineContent = { Text(text = stringResource(R.string.show_crash_report_dialog)) },
            trailingContent = {
                Switch(
                    modifier = Modifier.height(24.dp),
                    checked = acraEnabled,
                    onCheckedChange = onAcraEnabledChange,
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCrashReportsContent() {
    WallFlowTheme {
        Surface {
            CrashReportsContent()
        }
    }
}
