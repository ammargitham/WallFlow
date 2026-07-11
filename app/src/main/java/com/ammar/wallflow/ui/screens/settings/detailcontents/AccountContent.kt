package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
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
internal fun AccountContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onWallhavenApiKeyItemClick: () -> Unit = {},
    onRedditLoginItemClick: () -> Unit = {},
    redditCookie: String = "",
) {
    val isLoggedIn = redditCookie.isNotBlank()

    Column(modifier = modifier) {
        WallhavenApiKeyItem(
            modifier = Modifier.clickable(onClick = onWallhavenApiKeyItemClick),
            isExpanded = isExpanded,
            isFirst = true,
        )
        RedditLoginItem(
            modifier = Modifier.clickable(onClick = onRedditLoginItemClick),
            isExpanded = isExpanded,
            isLast = true,
            isLoggedIn = isLoggedIn,
        )
    }
}

@Composable
private fun WallhavenApiKeyItem(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isFirst: Boolean = false,
) {
    SettingsDetailListItem(
        modifier = modifier,
        isExpanded = isExpanded,
        isFirst = isFirst,
        isLast = false,
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

@Composable
private fun RedditLoginItem(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isLoggedIn: Boolean = false,
) {
    SettingsDetailListItem(
        modifier = modifier,
        isExpanded = isExpanded,
        isFirst = isFirst,
        isLast = isLast,
        headlineContent = {
            Text(text = stringResource(R.string.reddit_login))
        },
        supportingContent = {
            Column {
                Text(text = stringResource(R.string.reddit_login_desc))
                Text(
                    text = if (isLoggedIn) {
                        stringResource(R.string.reddit_logged_in_status)
                    } else {
                        stringResource(R.string.reddit_not_logged_in)
                    },
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
        AccountContent()
    }
}
