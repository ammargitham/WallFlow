package com.ammar.wallflow.ui.common.mainsearch

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.MenuItem
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTagSearchMeta
import com.ammar.wallflow.model.search.WallhavenUploaderSearchMeta
import com.ammar.wallflow.ui.common.OverflowMenu
import com.ammar.wallflow.ui.common.SearchBar
import com.ammar.wallflow.ui.common.Suggestion
import com.ammar.wallflow.ui.common.TagChip
import com.ammar.wallflow.ui.common.UploaderChip
import com.ammar.wallflow.ui.common.searchedit.EditSearchContent
import com.ammar.wallflow.ui.screens.home.SearchBarFiltersToggle
import com.ammar.wallflow.ui.theme.WallFlowTheme

object MainSearchBar {
    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        useDocked: Boolean = false,
        visible: Boolean = true,
        active: Boolean = false,
        search: WallhavenSearch = Defaults.search,
        query: String = "",
        suggestions: List<Suggestion<WallhavenSearch>> = emptyList(),
        showFilters: Boolean = false,
        deleteSuggestion: WallhavenSearch? = null,
        overflowIcon: @Composable (() -> Unit)? = null,
        showNSFW: Boolean = false,
        showQuery: Boolean = true,
        onQueryChange: (String) -> Unit = {},
        onBackClick: (() -> Unit)? = null,
        onSearch: (query: String) -> Unit = {},
        onSuggestionClick: (suggestion: Suggestion<WallhavenSearch>) -> Unit = {},
        onSuggestionInsert: (suggestion: Suggestion<WallhavenSearch>) -> Unit = {},
        onSuggestionDeleteRequest: (suggestion: Suggestion<WallhavenSearch>) -> Unit = {},
        onActiveChange: (active: Boolean) -> Unit = {},
        onShowFiltersChange: (show: Boolean) -> Unit = {},
        onFiltersChange: (searchQuery: WallhavenFilters) -> Unit = {},
        onDeleteSuggestionConfirmClick: () -> Unit = {},
        onDeleteSuggestionDismissRequest: () -> Unit = {},
        onSaveAsClick: () -> Unit = {},
        onLoadClick: () -> Unit = {},
    ) {
        val placeholder: @Composable () -> Unit = remember {
            {
                Text(text = stringResource(R.string.search))
            }
        }

        AnimatedVisibility(
            modifier = modifier,
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SearchBar(
                active = active,
                useDocked = useDocked,
                placeholder = when {
                    active -> placeholder
                    else -> when (search.meta) {
                        is WallhavenTagSearchMeta, is WallhavenUploaderSearchMeta -> null
                        else -> placeholder
                    }
                },
                query = if (showQuery) query else "",
                suggestions = suggestions,
                extraLeadingContent = when {
                    active -> null
                    else -> when (search.meta) {
                        is WallhavenTagSearchMeta -> {
                            { TagChip(wallhavenTag = search.meta.wallhavenTag) }
                        }
                        is WallhavenUploaderSearchMeta -> {
                            { UploaderChip(wallhavenUploader = search.meta.wallhavenUploader) }
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
                    Crossfade(
                        targetState = active,
                        label = "trailingIcon",
                    ) {
                        if (it) {
                            Row {
                                SearchBarFiltersToggle(
                                    checked = showFilters,
                                    onCheckedChange = onShowFiltersChange,
                                )
                                ActiveOverflowIcon(
                                    query = query,
                                    onSaveAsClick = onSaveAsClick,
                                    onLoadClick = onLoadClick,
                                )
                            }
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
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            EditSearchContent(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .windowInsetsPadding(WindowInsets.ime)
                                    .padding(16.dp),
                                showQueryField = false,
                                search = search,
                                showNSFW = showNSFW,
                                onChange = { onFiltersChange(it.filters) },
                            )
                        }
                    }
                },
            )
        }

        deleteSuggestion?.run {
            AlertDialog(
                title = { Text(text = this.query) },
                text = { Text(text = stringResource(R.string.delete_suggestion_dialog_text)) },
                confirmButton = {
                    TextButton(onClick = onDeleteSuggestionConfirmClick) {
                        Text(text = stringResource(R.string.confirm))
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

    object Defaults {
        val search = WallhavenSearch(
            filters = WallhavenFilters(
                sorting = WallhavenSorting.RELEVANCE,
            ),
        )
    }
}

@Composable
fun ActiveOverflowIcon(
    modifier: Modifier = Modifier,
    query: String = "",
    onSaveAsClick: () -> Unit = {},
    onLoadClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val menuItems = remember(context, query.isNotBlank()) {
        listOf(
            MenuItem(
                text = context.getString(R.string.save_as),
                value = "save_as",
                onClick = onSaveAsClick,
                enabled = query.isNotBlank(),
            ),
            MenuItem(
                text = context.getString(R.string.load),
                value = "load",
                onClick = onLoadClick,
            ),
        )
    }

    OverflowMenu(
        modifier = modifier,
    ) { closeMenu ->
        menuItems.forEach {
            DropdownMenuItem(
                text = { Text(it.text) },
                enabled = it.enabled,
                onClick = {
                    it.onClick?.invoke()
                    closeMenu()
                },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMainSearchBar() {
    WallFlowTheme {
        Surface {
            MainSearchBar()
        }
    }
}
