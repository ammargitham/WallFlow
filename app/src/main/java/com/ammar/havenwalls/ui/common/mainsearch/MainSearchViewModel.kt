package com.ammar.havenwalls.ui.common.mainsearch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.havenwalls.data.db.entity.toSearch
import com.ammar.havenwalls.data.repository.SearchHistoryRepository
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.getSupportingText
import com.ammar.havenwalls.ui.common.Suggestion
import com.ammar.havenwalls.ui.common.UiStateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainSearchViewModel @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository,
    application: Application,
) : AndroidViewModel(application), UiStateViewModel<MainSearchUiState> {
    private val localUiState = MutableStateFlow(LocalMainSearchUiState())

    override val uiState = combine(
        searchHistoryRepository.getAll(),
        localUiState,
    ) { searchHistory, local ->
        val localQuery = local.search.query.lowercase()
        MainSearchUiState(
            active = local.active,
            showFilters = local.showFilters,
            search = local.search,
            suggestions = searchHistory
                .filter { localQuery.isBlank() || it.query.lowercase().contains(localQuery) }
                .map { s ->
                    val search = s.toSearch()
                    Suggestion(
                        value = search,
                        headline = s.query,
                        supportingText = search.getSupportingText(application),
                    )
                },
            showDeleteRequestConfirmation = local.showDeleteRequestConfirmation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainSearchUiState(),
    )

    fun setActive(active: Boolean) = localUiState.update {
        it.copy(active = active)
    }

    fun setShowFilters(showFilters: Boolean) = localUiState.update {
        it.copy(showFilters = showFilters)
    }

    fun onQueryChange(query: String) = localUiState.update {
        it.copy(search = it.search.copy(query = query))
    }

    fun setSearch(search: Search) = localUiState.update {
        it.copy(search = search)
    }

    fun onSearch(search: Search) {
        localUiState.update { it.copy(search = search) }
        viewModelScope.launch {
            delay(1000) // delay for better ux
            searchHistoryRepository.addSearch(search)
        }
    }

    fun setShowDeleteRequest(search: Search?) = localUiState.update {
        it.copy(showDeleteRequestConfirmation = search)
    }

    fun deleteSearch(search: Search) = viewModelScope.launch {
        searchHistoryRepository.deleteSearch(search)
        localUiState.update { it.copy(showDeleteRequestConfirmation = null) }
    }
}

data class MainSearchUiState(
    val active: Boolean = false,
    val showFilters: Boolean = false,
    val search: Search = Search(),
    val suggestions: List<Suggestion<Search>> = emptyList(),
    val showDeleteRequestConfirmation: Search? = null,
)

private data class LocalMainSearchUiState(
    val active: Boolean = false,
    val showFilters: Boolean = false,
    val search: Search = Search(),
    val showDeleteRequestConfirmation: Search? = null,
)
