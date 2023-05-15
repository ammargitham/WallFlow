package com.ammar.havenwalls.ui.common.mainsearch

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.common.SearchQuery
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.TagSearchMeta
import com.ammar.havenwalls.model.UploaderSearchMeta
import com.ammar.havenwalls.ui.common.SearchBar
import com.ammar.havenwalls.ui.common.Suggestion
import com.ammar.havenwalls.ui.common.TagChip
import com.ammar.havenwalls.ui.common.UploaderChip
import com.ammar.havenwalls.ui.common.WallpaperFiltersDialogContent
import com.ammar.havenwalls.ui.home.SearchBarFiltersToggle
import com.ammar.havenwalls.ui.theme.HavenWallsTheme

@Composable
fun MainSearchBar(
    modifier: Modifier = Modifier,
    useDocked: Boolean = false,
    visible: Boolean = true,
    active: Boolean = false,
    search: Search = Search(),
    query: String = "",
    suggestions: List<Suggestion<Search>> = emptyList(),
    showFilters: Boolean = false,
    deleteSuggestion: Search? = null,
    overflowIcon: @Composable (() -> Unit)? = null,
    onQueryChange: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onSearch: (query: String) -> Unit = {},
    onSuggestionClick: (suggestion: Suggestion<Search>) -> Unit = {},
    onSuggestionInsert: (suggestion: Suggestion<Search>) -> Unit = {},
    onSuggestionDeleteRequest: (suggestion: Suggestion<Search>) -> Unit = {},
    onActiveChange: (active: Boolean) -> Unit = {},
    onShowFiltersChange: (show: Boolean) -> Unit = {},
    onFiltersChange: (searchQuery: SearchQuery) -> Unit = {},
    onDeleteSuggestionConfirmClick: () -> Unit = {},
    onDeleteSuggestionDismissRequest: () -> Unit = {},
) {
    val placeholder: @Composable () -> Unit = remember { { Text(text = "Search") } }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        SearchBar(
            useDocked = useDocked,
            placeholder = when {
                active -> placeholder
                else -> when (search.meta) {
                    is TagSearchMeta, is UploaderSearchMeta -> null
                    else -> placeholder
                }
            },
            query = query,
            suggestions = suggestions,
            extraLeadingContent = when {
                active -> null
                else -> when (search.meta) {
                    is TagSearchMeta -> {
                        { TagChip(tag = search.meta.tag) }
                    }
                    is UploaderSearchMeta -> {
                        { UploaderChip(uploader = search.meta.uploader) }
                    }
                    else -> null
                }
            },
            onQueryChange = onQueryChange,
            onBackClick = onBackClick,
            onSearch = onSearch,
            onSuggestionClick = onSuggestionClick,
            onSuggestionInsert = onSuggestionInsert,
            onSuggestionDeleteRequest = onSuggestionDeleteRequest,
            onActiveChange = onActiveChange,
            trailingIcon = {
                Crossfade(active) {
                    if (it) {
                        SearchBarFiltersToggle(
                            checked = showFilters,
                            onCheckedChange = onShowFiltersChange,
                        )
                        return@Crossfade
                    }
                    overflowIcon?.invoke()
                }
            },
            extraContent = {
                AnimatedVisibility(
                    modifier = Modifier.clipToBounds(),
                    visible = showFilters,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        WallpaperFiltersDialogContent(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .windowInsetsPadding(WindowInsets.ime)
                                .padding(16.dp),
                            searchQuery = search.filters,
                            onChange = onFiltersChange,
                        )
                    }
                }
            },
        )
    }

    deleteSuggestion?.run {
        AlertDialog(
            title = { Text(text = this.query) },
            text = { Text(text = "Remove from history?") },
            confirmButton = {
                TextButton(onClick = onDeleteSuggestionConfirmClick) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteSuggestionDismissRequest) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            onDismissRequest = onDeleteSuggestionDismissRequest,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMainSearchBar() {
    HavenWallsTheme {
        Surface {
            MainSearchBar()
        }
    }
}
