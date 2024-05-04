package com.ammar.wallflow.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar as MaterialSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.model.OnlineSource

object SearchBar {
    @Composable
    operator fun <T> invoke(
        modifier: Modifier = Modifier,
        active: Boolean = false,
        useDocked: Boolean = false,
        useFullWidth: Boolean = !useDocked,
        query: String = "",
        placeholder: @Composable (() -> Unit)? = null,
        extraLeadingContent: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        extraContent: @Composable (() -> Unit)? = null,
        suggestions: List<Suggestion<T>> = emptyList(),
        enabled: Boolean = true,
        onQueryChange: (String) -> Unit = {},
        onSearch: (query: String) -> Unit = {},
        onSuggestionClick: (suggestion: Suggestion<T>) -> Unit = {},
        onSuggestionInsert: (suggestion: Suggestion<T>) -> Unit = {},
        onSuggestionDeleteRequest: (suggestion: Suggestion<T>) -> Unit = {},
        onActiveChange: (active: Boolean) -> Unit = {},
        onBackClick: (() -> Unit)? = null,
    ) {
        var localActive by rememberSaveable { mutableStateOf(active) }
        val density = LocalDensity.current
        val imePadding = WindowInsets.ime.getBottom(density).toDp()

        LaunchedEffect(active) {
            localActive = active
        }

        Box(
            modifier
                .semantics { isTraversalGroup = true }
                .zIndex(1f)
                .fillMaxWidth(),
        ) {
            SwitchableSearchBar(
                modifier = Modifier.searchBar(useFullWidth = useFullWidth),
                useDocked = useDocked,
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    localActive = false
                    onSearch(it)
                    onActiveChange(false)
                },
                active = localActive,
                onActiveChange = {
                    localActive = it
                    onActiveChange(it)
                },
                placeholder = placeholder,
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Crossfade(
                            targetState = localActive || onBackClick != null,
                            label = "leadingIcon",
                        ) {
                            if (it) {
                                IconButton(
                                    onClick = {
                                        if (localActive) {
                                            localActive = false
                                            onActiveChange(false)
                                            return@IconButton
                                        }
                                        onBackClick?.invoke()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                                return@Crossfade
                            }
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp),
                            ) {
                                Icon(
                                    modifier = Modifier.align(Alignment.Center),
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                )
                            }
                        }
                        extraLeadingContent?.invoke()
                    }
                },
                trailingIcon = trailingIcon,
                enabled = enabled,
            ) {
                Box {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            bottom = imePadding,
                        ),
                    ) {
                        items(suggestions) {
                            SuggestionItem(
                                suggestion = it,
                                onClick = {
                                    localActive = false
                                    onActiveChange(false)
                                    onSuggestionClick(it)
                                },
                                onDeleteRequest = { onSuggestionDeleteRequest(it) },
                                onInsertClick = { onSuggestionInsert(it) },
                            )
                        }
                    }
                    extraContent?.invoke()
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    private fun <T> SuggestionItem(
        suggestion: Suggestion<T>,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        onDeleteRequest: () -> Unit = {},
        onInsertClick: () -> Unit = {},
    ) {
        ListItem(
            modifier = modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteRequest,
            ),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(
                            when (suggestion.source) {
                                OnlineSource.WALLHAVEN -> R.drawable.wallhaven_logo_short
                                OnlineSource.REDDIT -> R.drawable.reddit
                            },
                        ),
                        contentDescription = null,
                    )
                    Text(suggestion.headline)
                }
            },
            supportingContent = suggestion.supportingText?.let { t ->
                {
                    Text(
                        text = t,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = suggestion.icon,
            trailingContent = {
                IconButton(onClick = onInsertClick) {
                    Icon(
                        painter = painterResource(
                            R.drawable.baseline_north_west_24,
                        ),
                        contentDescription = null,
                    )
                }
            },
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun BoxScope.SwitchableSearchBar(
        modifier: Modifier = Modifier,
        useDocked: Boolean,
        query: String,
        onQueryChange: (String) -> Unit,
        onSearch: (String) -> Unit,
        active: Boolean,
        onActiveChange: (Boolean) -> Unit,
        placeholder: @Composable (() -> Unit)?,
        leadingIcon: @Composable () -> Unit,
        trailingIcon: @Composable (() -> Unit)?,
        enabled: Boolean,
        content: @Composable (ColumnScope.() -> Unit),
    ) {
        if (useDocked) {
            DockedSearchBar(
                modifier = modifier
                    .windowInsetsPadding(topWindowInsets)
                    .align(Alignment.TopCenter)
                    .offset(y = 16.dp),
                inputField = {
                    SearchBarDefaults.InputField(
                        modifier = Modifier.fillMaxWidth(),
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                        expanded = active,
                        onExpandedChange = onActiveChange,
                        placeholder = placeholder,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        enabled = enabled,
                    )
                },
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
                expanded = active,
                onExpandedChange = onActiveChange,
                content = content,
            )
        } else {
            MaterialSearchBar(
                modifier = modifier.align(Alignment.TopCenter),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                        expanded = active,
                        onExpandedChange = onActiveChange,
                        placeholder = placeholder,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        enabled = enabled,
                    )
                },
                expanded = active,
                onExpandedChange = onActiveChange,
                content = content,
            )
        }
    }

    object Defaults {
        val height = 72.dp
    }
}

data class Suggestion<T>(
    val value: T,
    val source: OnlineSource,
    val headline: String,
    val supportingText: String? = null,
    val icon: (@Composable () -> Unit)? = {
        Icon(
            painter = painterResource(R.drawable.baseline_history_24),
            contentDescription = null,
        )
    },
)

internal fun Modifier.searchBar(
    useFullWidth: Boolean = false,
) = if (useFullWidth) {
    this.fillMaxWidth()
} else {
    this
}
