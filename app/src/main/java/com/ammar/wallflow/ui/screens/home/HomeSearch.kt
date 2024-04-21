package com.ammar.wallflow.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.common.Suggestion
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBar
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBar.Defaults

@Composable
fun HomeSearch(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    useDocked: Boolean = false,
    useFullWidth: Boolean = false,
    search: Search = Defaults.wallhavenSearch,
    query: String = "",
    suggestions: List<Suggestion<Search>> = emptyList(),
    showQuery: Boolean = true,
    onQueryChange: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onSearch: (Search) -> Unit = {},
    onSearchChange: (Search) -> Unit = {},
    onSearchDeleteRequest: (Search) -> Unit = {},
    onActiveChange: (Boolean) -> Unit = {},
    onSaveAsClick: () -> Unit = {},
    onLoadClick: () -> Unit = {},
) {
    MainSearchBar(
        modifier = modifier,
        active = active,
        useDocked = useDocked,
        useFullWidth = useFullWidth,
        search = search,
        query = query,
        suggestions = suggestions,
        showQuery = showQuery,
        onQueryChange = onQueryChange,
        onBackClick = onBackClick,
        onSearch = {
            if (it.isBlank()) {
                return@MainSearchBar
            }
            val localSearch = if (it.trimAll() == query) {
                // keep current search data if query hasn't changed
                // this allows to keep meta data if only filters were changed
                val filters = search.filters
                when (search) {
                    is RedditSearch -> search.copy(
                        filters = filters as RedditFilters,
                    )
                    is WallhavenSearch -> search.copy(
                        filters = filters as WallhavenFilters,
                    )
                }
            } else {
                WallhavenSearch(
                    query = it,
                    filters = search.filters as WallhavenFilters,
                )
            }
            onSearch(localSearch)
        },
        onSuggestionClick = { onSearch(it.value) },
        onSuggestionInsert = { onSearchChange(it.value) },
        onSuggestionDeleteRequest = { onSearchDeleteRequest(it.value) },
        onActiveChange = onActiveChange,
        onSaveAsClick = onSaveAsClick,
        onLoadClick = onLoadClick,
    )
}
