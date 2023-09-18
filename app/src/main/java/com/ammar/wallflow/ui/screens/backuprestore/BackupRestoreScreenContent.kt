package com.ammar.wallflow.ui.screens.backuprestore

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun BackupRestoreScreenContent(
    modifier: Modifier = Modifier,
    onBackupClicked: () -> Unit = {},
    onRestoreClicked: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            ListItem(
                modifier = Modifier.clickable(onClick = onBackupClicked),
                headlineContent = {
                    Text(text = stringResource(R.string.backup))
                },
            )
        }
        item {
            ListItem(
                modifier = Modifier.clickable(onClick = onRestoreClicked),
                headlineContent = {
                    Text(text = stringResource(R.string.restore))
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBackupRestoreScreenContent() {
    WallFlowTheme {
        Surface {
            BackupRestoreScreenContent()
        }
    }
}
