package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun RestartDialog(
    reason: RestartReason,
    modifier: Modifier = Modifier,
    onRestartClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        title = {
            Text(text = stringResource(R.string.restart_required))
        },
        text = {
            Text(text = getRestartReasonText(reason))
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = onRestartClick) {
                Text(text = stringResource(R.string.restart))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClick) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

enum class RestartReason {
    ACRA_ENABLED,
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRestartDialog() {
    WallFlowTheme {
        Surface {
            RestartDialog(RestartReason.ACRA_ENABLED)
        }
    }
}
