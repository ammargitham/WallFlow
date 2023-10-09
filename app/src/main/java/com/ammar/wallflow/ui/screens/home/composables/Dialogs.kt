package com.ammar.wallflow.ui.screens.home.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.SUBREDDIT_REGEX
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.common.taginput.TagInputField
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageSourcesDialog(
    modifier: Modifier = Modifier,
    currentSources: ImmutableMap<OnlineSource, Boolean> = persistentMapOf(),
    onDismissRequest: () -> Unit = {},
    onAddSourceClick: (OnlineSource) -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.manage_sources)) },
            text = {
                ManageSourcesDialogContent(
                    currentSources = currentSources,
                    onAddSourceClick = onAddSourceClick,
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = false,
                        onClick = {
                            // onSaveClick(localSelectedDelegate)
                        },
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun ManageSourcesDialogContent(
    modifier: Modifier = Modifier,
    currentSources: ImmutableMap<OnlineSource, Boolean> = persistentMapOf(),
    onAddSourceClick: (OnlineSource) -> Unit = {},
) {
    val newSources = remember(currentSources) {
        OnlineSource.entries.toSet() - currentSources.keys
    }

    Column(
        modifier = modifier,
    ) {
        if (newSources.isNotEmpty()) {
            Header(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResource(R.string.new_sources),
            )
            newSources.forEach {
                ListItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onAddSourceClick(it) },
                    headlineContent = {
                        Text(text = getSourceLabel(it))
                    },
                    leadingContent = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.reddit),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        Header(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.current_sources),
        )
        currentSources.forEach {
            ListItem(
                modifier = Modifier
                    .clickable(enabled = false) {}
                    .padding(horizontal = 8.dp),
                headlineContent = {
                    Text(text = getSourceLabel(it.key))
                },
                leadingContent = {
                    Checkbox(
                        modifier = Modifier.size(24.dp),
                        enabled = false,
                        checked = it.value,
                        onCheckedChange = {},
                    )
                },
            )
        }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun getSourceLabel(
    source: OnlineSource,
) = when (source) {
    OnlineSource.WALLHAVEN -> stringResource(R.string.wallhaven_cc)
    OnlineSource.REDDIT -> stringResource(R.string.reddit)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewManageSourcesDialog() {
    WallFlowTheme {
        Surface {
            ManageSourcesDialog(
                currentSources = persistentMapOf(
                    OnlineSource.WALLHAVEN to true,
                ),
            )
        }
    }
}

@Composable
internal fun RedditInitDialog(
    modifier: Modifier = Modifier,
    subreddits: Set<String> = emptySet(),
    onSaveClick: (Set<String>) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localSubreddits by rememberSaveable(subreddits) {
        mutableStateOf(subreddits)
    }
    var hasError by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        modifier = modifier,
        icon = {
            Icon(
                painter = painterResource(R.drawable.reddit),
                contentDescription = null,
            )
        },
        title = {
            Text(text = stringResource(R.string.configure_reddit))
        },
        text = {
            RedditInitDialogContent(
                subreddits = localSubreddits,
                onChange = { s, e ->
                    localSubreddits = s
                    hasError = e
                },
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = localSubreddits.isNotEmpty() && !hasError,
                onClick = { onSaveClick(localSubreddits) },
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RedditInitDialogContent(
    modifier: Modifier = Modifier,
    subreddits: Set<String> = emptySet(),
    onChange: (
        subreddits: Set<String>,
        hasError: Boolean,
    ) -> Unit = { _, _ -> },
) {
    var localSubreddits by rememberSaveable(subreddits) {
        mutableStateOf<Set<Pair<String, Boolean>>>(
            subreddits.mapTo(LinkedHashSet()) {
                it to it.matches(SUBREDDIT_REGEX)
            },
        )
    }
    val focusRequester = remember { FocusRequester() }
    val hasError = localSubreddits.any { !it.second }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(localSubreddits) {
        onChange(
            localSubreddits.mapTo(LinkedHashSet()) { it.first },
            hasError,
        )
    }

    val regex = remember { "[ ,\\r\\n]+".toRegex() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TagInputField(
            modifier = Modifier.focusRequester(focusRequester),
            tags = localSubreddits,
            separatorRegex = regex,
            label = { Text(text = stringResource(R.string.subreddits)) },
            tagFromInputString = {
                val name = SUBREDDIT_REGEX.matchEntire(it)
                    ?.groupValues
                    ?.get(1)
                    ?: return@TagInputField it to false
                name to true
            },
            validateTag = { it.second },
            getTagString = { "r/${it.first}" },
            onAddTag = { localSubreddits += it },
            onRemoveTag = { localSubreddits -= it },
        )
        Text(
            text = if (hasError) {
                stringResource(R.string.subreddit_input_error_support_text)
            } else {
                stringResource(R.string.subreddit_input_support_text)
            },
            color = if (hasError) {
                MaterialTheme.colorScheme.error
            } else {
                Color.Unspecified
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewRedditInitDialog() {
    WallFlowTheme {
        Surface {
            RedditInitDialog(
                subreddits = setOf(
                    "wallpapers",
                    "test",
                ),
            )
        }
    }
}
