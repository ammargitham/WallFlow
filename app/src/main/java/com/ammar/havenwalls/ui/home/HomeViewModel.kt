package com.ammar.havenwalls.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.havenwalls.data.common.Purity
import com.ammar.havenwalls.data.common.SearchQuery
import com.ammar.havenwalls.data.common.Sorting
import com.ammar.havenwalls.data.common.TopRange
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.WallHavenRepository
import com.ammar.havenwalls.data.repository.utils.Resource
import com.ammar.havenwalls.data.repository.utils.successOr
import com.ammar.havenwalls.model.Tag
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wallHavenRepository: WallHavenRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    private val initialUiState = HomeUiState()
    private val popularTags = wallHavenRepository.popularTags()
    private val localUiState = MutableStateFlow(HomeUiStatePartial())

    val wallpapers = appPreferencesRepository.appPreferencesFlow.flatMapLatest {
        wallHavenRepository.wallpapersPager(it.homeSearchQuery)
    }.cachedIn(viewModelScope)

    private val wallpapersLoadingFlow = MutableStateFlow(false)

    @OptIn(FlowPreview::class)
    private val debouncedWallpapersLoadingFlow = wallpapersLoadingFlow
        .debounce { if (it) 1000 else 0 }
        .distinctUntilChanged()

    val uiState = combine(
        popularTags,
        appPreferencesRepository.appPreferencesFlow,
        localUiState,
        debouncedWallpapersLoadingFlow,
    ) { tags, appPreferences, local, wallpapersLoading ->
        HomeUiState(
            tags = if (tags is Resource.Loading) List(3) {
                Tag(
                    id = it + 1L,
                    name = "Loading...", // no need for string resource as "Loading..." won't be visible
                    alias = emptyList(),
                    categoryId = 0,
                    category = "",
                    purity = Purity.SFW,
                    createdAt = Clock.System.now(),
                )
            } else tags.successOr(emptyList()),
            areTagsLoading = tags is Resource.Loading,
            query = appPreferences.homeSearchQuery,
            showFilters = local.showFilters.getOrElse { false },
            wallpapersLoading = wallpapersLoading,
            blurSketchy = appPreferences.blurSketchy,
            blurNsfw = appPreferences.blurNsfw,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialUiState,
    )

    fun refresh() {
        viewModelScope.launch {
            wallHavenRepository.refreshPopularTags()
        }
    }

    fun updateQuery(searchQuery: SearchQuery) {
        viewModelScope.launch {
            appPreferencesRepository.updateHomeSearchQuery(searchQuery)
        }
        localUiState.update { it.copy(showFilters = partial(false)) }
    }

    fun showFilters(show: Boolean) = localUiState.update {
        it.copy(showFilters = partial(show))
    }

    fun setWallpapersLoading(refreshing: Boolean) = wallpapersLoadingFlow.update { refreshing }
}

@Partialize
data class HomeUiState(
    val tags: List<Tag> = emptyList(),
    val areTagsLoading: Boolean = true,
    val query: SearchQuery = SearchQuery(
        sorting = Sorting.TOPLIST,
        topRange = TopRange.ONE_DAY,
    ),
    val showFilters: Boolean = false,
    val wallpapersLoading: Boolean = false,
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
)
