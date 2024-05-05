package com.ammar.wallflow.ui.screens.local

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
fun RemoveDirectoryConfirmDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        title = {
            Text(text = stringResource(R.string.are_you_sure))
        },
        text = {
            Text(text = stringResource(R.string.remove_local_dir_confirm_text))
        },
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(R.string.remove))
            }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRemoveDirectoryConfirmDialog() {
    WallFlowTheme {
        Surface {
            RemoveDirectoryConfirmDialog()
        }
    }
}
