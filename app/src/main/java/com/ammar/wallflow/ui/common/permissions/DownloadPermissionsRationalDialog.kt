package com.ammar.wallflow.ui.common.permissions

import android.Manifest
import android.annotation.SuppressLint
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
fun DownloadPermissionsRationalDialog(
    modifier: Modifier = Modifier,
    permissions: Collection<String> = emptyList(),
    onConfirmOrDismiss: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        text = {
            val rationales = permissions.map {
                when (it) {
                    Manifest.permission.POST_NOTIFICATIONS -> stringResource(R.string.notification_permission_rationale)
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> stringResource(R.string.write_storage_permission_rationale)
                    else -> stringResource(R.string.permission_is_required, it)
                }
            }.filter { it.isNotBlank() }
            Text(text = rationales.joinToString("\n\n"))
        },
        confirmButton = {
            TextButton(onClick = onConfirmOrDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        },
        onDismissRequest = onConfirmOrDismiss,
    )
}

@SuppressLint("InlinedApi")
@Preview(showSystemUi = true)
@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewNotificationPermissionRationalDialog() {
    WallFlowTheme {
        Surface {
            DownloadPermissionsRationalDialog(
                permissions = listOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            )
        }
    }
}
