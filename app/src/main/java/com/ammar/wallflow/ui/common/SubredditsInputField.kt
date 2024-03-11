package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.SUBREDDIT_REGEX
import com.ammar.wallflow.ui.common.taginput.TagInputField
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun SubredditsInputField(
    modifier: Modifier = Modifier,
    subreddits: Set<String> = emptySet(),
    focusRequester: FocusRequester? = null,
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
    var isDirty by rememberSaveable {
        mutableStateOf(false)
    }
    val hasError = (isDirty && localSubreddits.isEmpty()) ||
        localSubreddits.any { !it.second }

    val regex = remember { "[ ,\\r\\n]+".toRegex() }

    LaunchedEffect(localSubreddits) {
        val updatedSubreddits = localSubreddits.mapTo(LinkedHashSet()) { it.first }
        if (subreddits == updatedSubreddits) {
            return@LaunchedEffect
        }
        isDirty = true
        onChange(
            updatedSubreddits,
            hasError,
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TagInputField(
            modifier = Modifier.let {
                if (focusRequester != null) {
                    it.focusRequester(focusRequester)
                } else {
                    it
                }
            },
            tags = localSubreddits,
            separatorRegex = regex,
            isError = hasError,
            maxLines = 3,
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
                if (localSubreddits.isEmpty()) {
                    stringResource(R.string.required_one_subreddit)
                } else {
                    stringResource(R.string.subreddit_input_error_support_text)
                }
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

private data class SubredditsInputFieldProps(
    val subreddits: Set<String> = emptySet(),
)

private class SubredditsInputFieldCPP : CollectionPreviewParameterProvider<
    SubredditsInputFieldProps,
    >(
    listOf(
        SubredditsInputFieldProps(),
        SubredditsInputFieldProps(
            subreddits = setOf("wallpapers", "test"),
        ),
        SubredditsInputFieldProps(
            subreddits = setOf("w"),
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSubredditsInputField(
    @PreviewParameter(SubredditsInputFieldCPP::class) props: SubredditsInputFieldProps,
) {
    WallFlowTheme {
        Surface {
            Column(modifier = Modifier.padding(8.dp)) {
                SubredditsInputField(
                    subreddits = props.subreddits,
                )
            }
        }
    }
}
