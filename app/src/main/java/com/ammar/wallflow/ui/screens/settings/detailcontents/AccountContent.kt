package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun AccountContent(
    modifier: Modifier = Modifier,
    onWallhavenApiKeyItemClick: () -> Unit = {},
) {
    WallhavenApiKeyItem(
        modifier = modifier.clickable(
            onClick = onWallhavenApiKeyItemClick,
        ),
    )
}

@Composable
private fun WallhavenApiKeyItem(
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(text = stringResource(R.string.wallhaven_api_key))
        },
        supportingContent = {
            Column {
                Text(text = stringResource(R.string.wallhaven_api_key_desc))
                Text(
                    text = stringResource(R.string.wallhaven_api_key_desc_2),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAccountContent() {
    WallFlowTheme {
        Surface {
            AccountContent()
        }
    }
}
