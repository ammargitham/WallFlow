package com.ammar.wallflow.ui.common.searchedit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.ui.common.SubredditsInputField
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun EditRedditSearchContent(
    modifier: Modifier = Modifier,
    search: RedditSearch = RedditSearch(
        filters = RedditFilters(
            subreddits = emptySet(),
            includeNsfw = false,
            sort = RedditSort.RELEVANCE,
            timeRange = RedditTimeRange.ALL,
        ),
    ),
    showQueryField: Boolean = true,
    onChange: (RedditSearch) -> Unit = {},
    onErrorStateChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showQueryField) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.query)) },
                value = search.query,
                onValueChange = { onChange(search.copy(query = it)) },
            )
        }
        SubredditsInputField(
            subreddits = search.filters.subreddits,
            onChange = { subreddits, hasError ->
                onChange(
                    search.copy(
                        filters = search.filters.copy(
                            subreddits = subreddits,
                        ),
                    ),
                )
                onErrorStateChange(hasError || subreddits.isEmpty())
            },
        )
        PurityFilter(
            purities = if (search.filters.includeNsfw) {
                setOf(Purity.SFW, Purity.NSFW)
            } else {
                setOf(Purity.SFW)
            },
            showNSFW = true,
            showSketchy = false,
            canToggleSFW = false,
            onChange = {
                onChange(
                    search.copy(
                        filters = search.filters.copy(
                            includeNsfw = it.contains(Purity.NSFW),
                        ),
                    ),
                )
            },
        )
        RedditSortFilter(
            sort = search.filters.sort,
            onChange = {
                onChange(
                    search.copy(
                        filters = search.filters.copy(
                            sort = it,
                        ),
                    ),
                )
            },
        )
        RedditTimeRangeFilter(
            timeRange = search.filters.timeRange,
            onChange = {
                onChange(
                    search.copy(
                        filters = search.filters.copy(
                            timeRange = it,
                        ),
                    ),
                )
            },
        )
    }
}

@Preview(device = "spec:width=1080px,height=3500px,dpi=440")
@Preview(
    device = "spec:width=1080px,height=3500px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewEditRedditSearchContent() {
    WallFlowTheme {
        Surface {
            EditRedditSearchContent(
                modifier = Modifier.padding(16.dp),
                search = RedditSearch(
                    filters = RedditFilters(
                        subreddits = setOf("wallpapers", "test"),
                        includeNsfw = false,
                        sort = RedditSort.RELEVANCE,
                        timeRange = RedditTimeRange.ALL,
                    ),
                ),
            )
        }
    }
}
