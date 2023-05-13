package com.ammar.havenwalls.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.havenwalls.data.repository.WallHavenRepository
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.toSearchQuery
import com.ammar.havenwalls.ui.common.UiStateViewModel
import com.ammar.havenwalls.ui.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    wallHavenRepository: WallHavenRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UiStateViewModel<SearchUiState> {
    private val navArgs: SearchScreenNavArgs = savedStateHandle.navArgs()
    private val _uiState = MutableStateFlow(SearchUiState())
    override val uiState: StateFlow<SearchUiState> = _uiState
    private val searchFlow = MutableStateFlow(navArgs.search)

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpapers = searchFlow.flatMapLatest {
        wallHavenRepository.wallpapersPager(it.toSearchQuery())
    }.cachedIn(viewModelScope)

    fun search(search: Search) = searchFlow.update { search }
}

data class SearchUiState(
    val loading: Boolean = false,
)
