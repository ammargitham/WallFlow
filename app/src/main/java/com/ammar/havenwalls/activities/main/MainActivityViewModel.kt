package com.ammar.havenwalls.activities.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.havenwalls.data.db.entity.toSearch
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalError
import com.ammar.havenwalls.data.repository.SearchHistoryRepository
import com.ammar.havenwalls.extensions.trimAll
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.getSupportingText
import com.ammar.havenwalls.ui.common.Suggestion
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

/**
 * This view model is shared across destinations to perform some top level actions
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val globalErrorsRepository: GlobalErrorsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val application: Application,
) : AndroidViewModel(application) {
    private val localUiState = MutableStateFlow(MainUiStatePartial())

    val uiState = combine(
        localUiState,
        searchHistoryRepository.getAll(),
        globalErrorsRepository.errors,
    ) { local, searchHistory, errors ->
        val localQuery = local.searchBarSearch.getOrNull()?.query?.trimAll()?.lowercase() ?: ""
        local.merge(
            MainUiState(
                searchBarSuggestions = searchHistory
                    .filter {
                        localQuery.isBlank()
                                || it.query.trimAll().lowercase().contains(localQuery)
                    }
                    .map { s ->
                        val search = s.toSearch()
                        Suggestion(
                            value = search,
                            headline = s.query,
                            supportingText = search.getSupportingText(application),
                        )
                    },
                globalErrors = errors,
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(),
    )

    // fun setTopAppBarVisible(visible: Boolean) = _uiState.update {
    //     it.copy(topAppBarVisible = visible)
    // }
    //
    // fun toggleTopAppBarVisibility() = _uiState.update {
    //     it.copy(topAppBarVisible = !it.topAppBarVisible)
    // }
    //
    // fun setTopAppBarGradientBg(enabled: Boolean) = _uiState.update {
    //     it.copy(topAppBarGradientBg = enabled)
    // }
    //
    // fun setTopAppBarTitleVisible(visible: Boolean) = _uiState.update {
    //     it.copy(topAppBarTitleVisible = visible)
    // }

    fun applyScaffoldPadding(apply: Boolean) = localUiState.update {
        it.copy(applyScaffoldPadding = partial(apply))
    }

    fun dismissGlobalError(error: GlobalError) = globalErrorsRepository.removeError(error)

    fun setSearchBarActive(active: Boolean) = localUiState.update {
        it.copy(searchBarActive = partial(active))
    }

    fun setSearchBarSearch(search: Search) = localUiState.update {
        it.copy(searchBarSearch = partial(search))
    }

    fun onSearch(search: Search) {
        localUiState.update { it.copy(searchBarSearch = partial(search)) }
        viewModelScope.launch {
            delay(1000) // delay for better ux
            searchHistoryRepository.addSearch(search)
        }
    }

    fun setShowSearchBarFilters(show: Boolean) = localUiState.update {
        it.copy(showSearchBarFilters = partial(show))
    }

    fun setShowSearchBarSuggestionDeleteRequest(search: Search?) = localUiState.update {
        it.copy(searchBarDeleteSuggestion = partial(search))
    }

    fun onSearchBarQueryChange(query: String) = localUiState.update {
        val currentSearch = it.searchBarSearch.getOrElse { Search() }
        it.copy(searchBarSearch = partial(currentSearch.copy(query = query)))
    }

    fun deleteSearch(search: Search) = viewModelScope.launch {
        searchHistoryRepository.deleteSearch(search)
        localUiState.update { it.copy(searchBarDeleteSuggestion = partial(null)) }
    }
}

@Partialize
data class MainUiState(
    // val topAppBarVisible: Boolean = true,
    // val topAppBarGradientBg: Boolean = false,
    // val topAppBarTitleVisible: Boolean = true,
    val applyScaffoldPadding: Boolean = true,
    val globalErrors: List<GlobalError> = emptyList(),
    val searchBarActive: Boolean = false,
    val searchBarSearch: Search = Search(),
    val searchBarSuggestions: List<Suggestion<Search>> = emptyList(),
    val showSearchBarFilters: Boolean = false,
    val searchBarDeleteSuggestion: Search? = null,
)
