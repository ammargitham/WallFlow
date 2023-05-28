package com.ammar.havenwalls.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.toDp
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import androidx.compose.material3.SearchBar as MaterialSearchBar

object SearchBar {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    operator fun <T> invoke(
        modifier: Modifier = Modifier,
        active: Boolean = false,
        useDocked: Boolean = false,
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
        val content: @Composable ColumnScope.() -> Unit = {
            Box {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = imePadding,
                    ),
                    // verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(suggestions) {
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    localActive = false
                                    onActiveChange(false)
                                    onSuggestionClick(it)
                                },
                                onLongClick = { onSuggestionDeleteRequest(it) },
                            ),
                            headlineContent = { Text(it.headline) },
                            supportingContent = it.supportingText?.let { t ->
                                {
                                    Text(
                                        text = t,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            leadingContent = it.icon,
                            trailingContent = {
                                IconButton(
                                    onClick = { onSuggestionInsert(it) },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_north_west_24),
                                        contentDescription = null,
                                    )
                                }
                            }
                        )
                    }
                }
                extraContent?.invoke()
            }
        }

        LaunchedEffect(active) {
            localActive = active
        }

        Box(
            modifier
                .semantics { isContainer = true }
                .zIndex(1f)
                .searchBarContainer(isDocked = useDocked),
        ) {
            SwitchableSearchBar(
                modifier = Modifier.searchBar(isDocked = useDocked),
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
                        Crossfade(localActive || onBackClick != null) {
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
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = null
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
                content = content
            )
        }
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
                modifier = modifier.align(Alignment.TopStart),
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                active = active,
                onActiveChange = onActiveChange,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                enabled = enabled,
                content = content,
            )
        } else {
            MaterialSearchBar(
                modifier = modifier.align(Alignment.TopCenter),
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                active = active,
                onActiveChange = onActiveChange,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                enabled = enabled,
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
    val headline: String,
    val supportingText: String? = null,
    val icon: (@Composable () -> Unit)? = {
        Icon(
            painter = painterResource(R.drawable.baseline_history_24),
            contentDescription = null,
        )
    },
)


fun Modifier.searchBarContainer(
    isDocked: Boolean = false,
) = composed {
    if (!isDocked) {
        return@composed this.fillMaxWidth()
    }
    val bottomBarController = LocalBottomBarController.current
    val state by bottomBarController.state
    this
        .fillMaxWidth(0.5f)
        .padding(
            start = state.size.width.toDp() + 8.dp,
            end = 8.dp,
        )
}

fun Modifier.searchBar(
    isDocked: Boolean = false,
) = if (!isDocked) {
    this
} else {
    this.fillMaxWidth()
}
