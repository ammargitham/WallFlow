package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.screens.settings.composables.delegateString
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun ObjectDetectionContent(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    delegate: ObjectDetectionDelegate = ObjectDetectionDelegate.GPU,
    model: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    isExpanded: Boolean = false,
    onEnabledChange: (enabled: Boolean) -> Unit = {},
    onDelegateClick: () -> Unit = {},
    onModelClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        val alpha = if (enabled) 1f else DISABLED_ALPHA

        item {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.object_detection_setting_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ),
                text = stringResource(R.string.object_detection_setting_warning),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable { onEnabledChange(!enabled) },
                isExpanded = isExpanded,
                isFirst = true,
                headlineContent = { Text(text = stringResource(R.string.enable_object_detection)) },
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
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = onDelegateClick,
                ),
                isExpanded = isExpanded,
                headlineContent = {
                    Text(
                        text = stringResource(R.string.tflite_delegate),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    )
                },
                supportingContent = {
                    Text(
                        text = delegateString(delegate),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                },
            )
        }
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClick = onModelClick,
                ),
                isExpanded = isExpanded,
                isLast = true,
                headlineContent = {
                    Text(
                        text = stringResource(R.string.tflite_model),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    )
                },
                supportingContent = {
                    Text(
                        text = model.name,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewObjectDetectionContent() {
    WallFlowTheme {
        Surface {
            ObjectDetectionContent()
        }
    }
}
