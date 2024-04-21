package com.ammar.wallflow.ui.screens.home

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.db.entity.search.toSearch
import com.ammar.wallflow.data.repository.SearchHistoryRepository
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.getSupportingText
import com.ammar.wallflow.ui.common.Suggestion
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBar.Defaults
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
import com.github.materiiapps.partial.getOrNull
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchBarViewModel @Inject constructor(
    private val application: Application,
    private val searchHistoryRepository: SearchHistoryRepository,
) : AndroidViewModel(
    application = application,
) {
    private val localUiState = MutableStateFlow(SearchBarUiStatePartial())

    val uiState = combine(
        localUiState,
        searchHistoryRepository.getAll(),
    ) { local, searchHistory ->
        val localQuery = local.search.getOrNull()?.query?.trimAll()?.lowercase() ?: ""
        local.merge(
            SearchBarUiState(
                suggestions = searchHistory
                    .filter {
                        localQuery.isBlank() || it.query.trimAll().lowercase().contains(localQuery)
                    }
                    .map { it.toSearch() }
                    .filter {
                        when (local.source.getOrNull()) {
                            OnlineSource.WALLHAVEN -> it is WallhavenSearch
                            OnlineSource.REDDIT -> it is RedditSearch
                            null -> false
                        }
                    }
                    .map {
                        when (it) {
                            is WallhavenSearch -> Suggestion(
                                value = it,
                                source = OnlineSource.WALLHAVEN,
                                headline = it.query,
                                supportingText = it.getSupportingText(application),
                            )
                            is RedditSearch -> Suggestion(
                                value = it,
                                source = OnlineSource.REDDIT,
                                headline = it.query,
                                supportingText = null,
                            )
                        }
                    },

            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchBarUiState(),
    )

    fun setActive(active: Boolean) = localUiState.update {
        it.copy(active = partial(active))
    }

    fun setSource(source: OnlineSource) = localUiState.update {
        it.copy(source = partial(source))
    }

    fun setQuery(query: String) = localUiState.update {
        val updated = when (
            val currentSearch =
                it.search.getOrElse { Defaults.wallhavenSearch }
        ) {
            is RedditSearch -> currentSearch.copy(
                query = query,
            )
            is WallhavenSearch -> currentSearch.copy(
                query = query,
            )
        }
        it.copy(search = partial(updated))
    }

    fun setSearch(search: Search) = localUiState.update {
        it.copy(search = partial(search))
    }

    fun onSearch(search: Search) {
        localUiState.update { it.copy(search = partial(search)) }
        viewModelScope.launch {
            delay(1000) // delay for better ux
            searchHistoryRepository.addSearch(search)
        }
    }

    fun setSearchToDelete(search: Search?) = localUiState.update {
        it.copy(searchToDelete = partial(search))
    }

    fun onConfirmDeleteSearch() = viewModelScope.launch {
        val searchToDelete = localUiState.value.searchToDelete.getOrNull() ?: return@launch
        searchHistoryRepository.deleteSearch(searchToDelete)
        localUiState.update { it.copy(searchToDelete = partial(null)) }
    }

    fun onCancelDeleteSearch() = viewModelScope.launch {
        localUiState.update { it.copy(searchToDelete = partial(null)) }
    }
}

@Stable
@Partialize
data class SearchBarUiState(
    val source: OnlineSource = OnlineSource.WALLHAVEN,
    val search: Search = Defaults.wallhavenSearch,
    val active: Boolean = false,
    val suggestions: List<Suggestion<Search>> = emptyList(),
    val searchToDelete: Search? = null,
    val showSavedSearchesDialog: Boolean = false,
)
