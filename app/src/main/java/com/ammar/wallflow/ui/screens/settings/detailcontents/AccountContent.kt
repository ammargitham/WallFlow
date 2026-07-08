package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun AccountContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onWallhavenApiKeyItemClick: () -> Unit = {},
    onRedditLoginItemClick: () -> Unit = {},
    onRedditLoginItemLongPressClick: () -> Unit = {},
    redditCookie: String = "",
) {
    val isLoggedIn = redditCookie.isNotBlank()
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current

    LaunchedEffect(interactionSource) {
        var isLongPress = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongPress = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongPress = true
                    onRedditLoginItemLongPressClick()
                }

                is PressInteraction.Release -> {
                    if (!isLongPress) {
                        onRedditLoginItemClick()
                    }
                }
            }
        }
    }

    Column(modifier = modifier) {
        WallhavenApiKeyItem(
            modifier = Modifier.clickable(
                onClick = onWallhavenApiKeyItemClick,
            ),
            isExpanded = isExpanded,
            isFirst = true,
        )
        Surface(
            modifier = Modifier,
            interactionSource = interactionSource,
            onClick = { },
            color = Color.Transparent,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = (if (isExpanded) 16 else 0).dp,
                topEnd = (if (isExpanded) 16 else 0).dp,
                bottomStart = (if (isExpanded) 16 else 0).dp,
                bottomEnd = (if (isExpanded) 16 else 0).dp,
            ),
        ) {
            RedditLoginItem(
                modifier = Modifier,
                isExpanded = isExpanded,
                isLast = true,
                isLoggedIn = isLoggedIn,
            )
        }
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
                        stringResource(R.string.reddit_logged_in_long_press)
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
        Surface {
            AccountContent()
        }
    }
}
