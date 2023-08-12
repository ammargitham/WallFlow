package com.ammar.wallflow.ui.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.db.entity.toSavedSearch
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.WallHavenRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.SavedSearch
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.Sorting
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Tag
import com.ammar.wallflow.model.TopRange
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.toSearchQuery
import com.ammar.wallflow.ui.navargs.ktxserializable.DefaultKtxSerializableNavTypeSerializer
import com.ammar.wallflow.utils.combine
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(
    ExperimentalCoroutinesApi::class,
    FlowPreview::class,
    ExperimentalSerializationApi::class,
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wallHavenRepository: WallHavenRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val savedSearchRepository: SavedSearchRepository,
    private val favoritesRepository: FavoritesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val savedStateSearchFlow: Flow<Search?> = savedStateHandle.getStateFlow<ByteArray?>(
        "search",
        null,
    ).mapLatest {
        it?.run {
            DefaultKtxSerializableNavTypeSerializer(Search.serializer()).fromByteArray(it)
        }
    }
    private val popularTags = wallHavenRepository.popularTags()
    private val localUiState = MutableStateFlow(HomeUiStatePartial())
    private val searchFlow = combine(
        appPreferencesRepository.appPreferencesFlow,
        savedStateSearchFlow,
    ) { preferences, search -> search ?: preferences.homeSearch }
    private val wallpapersLoadingFlow = MutableStateFlow(false)
    private val debouncedWallpapersLoadingFlow = wallpapersLoadingFlow
        .debounce { if (it) 1000 else 0 }
        .distinctUntilChanged()

    val wallpapers = searchFlow.flatMapLatest {
        wallHavenRepository.wallpapersPager(it.toSearchQuery())
    }.cachedIn(viewModelScope)

    val uiState = combine(
        searchFlow,
        popularTags,
        appPreferencesRepository.appPreferencesFlow,
        localUiState,
        debouncedWallpapersLoadingFlow,
        savedSearchRepository.getAll(),
        favoritesRepository.observeAll(),
    ) {
            search,
            tags,
            appPreferences,
            local,
            wallpapersLoading,
            savedSearchEntities,
            favorites,
        ->
        local.merge(
            HomeUiState(
                tags = (
                    if (tags is Resource.Loading) {
                        tempTags
                    } else {
                        tags.successOr(
                            emptyList(),
                        )
                    }
                    ).toImmutableList(),
                areTagsLoading = tags is Resource.Loading,
                search = search,
                wallpapersLoading = wallpapersLoading,
                blurSketchy = appPreferences.blurSketchy,
                blurNsfw = appPreferences.blurNsfw,
                showNSFW = appPreferences.wallhavenApiKey.isNotBlank(),
                isHome = search == appPreferences.homeSearch,
                savedSearches = savedSearchEntities.map { entity -> entity.toSavedSearch() },
                layoutPreferences = appPreferences.lookAndFeelPreferences.layoutPreferences,
                favorites = favorites.map { it.toFavorite() }.toImmutableList(),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun refresh() {
        viewModelScope.launch {
            wallHavenRepository.refreshPopularTags()
        }
    }

    fun updateHomeSearch(search: Search) {
        viewModelScope.launch {
            appPreferencesRepository.updateHomeSearch(search)
        }
    }

    fun showFilters(show: Boolean) = localUiState.update {
        it.copy(showFilters = partial(show))
    }

    fun setWallpapersLoading(refreshing: Boolean) = wallpapersLoadingFlow.update { refreshing }

    fun setSelectedWallpaper(wallpaper: Wallpaper) = localUiState.update {
        it.copy(selectedWallpaper = partial(wallpaper))
    }

    fun showSaveSearchAsDialog(search: Search? = null) = localUiState.update {
        it.copy(saveSearchAsSearch = partial(search))
    }

    fun saveSearchAs(name: String, search: Search) = viewModelScope.launch {
        savedSearchRepository.addOrUpdateSavedSearch(
            SavedSearch(
                name = name,
                search = search,
            ),
        )
    }

    fun showSavedSearches(show: Boolean = true) = localUiState.update {
        it.copy(showSavedSearchesDialog = partial(show))
    }

    fun toggleFavorite(wallpaper: Wallpaper) = viewModelScope.launch {
        favoritesRepository.toggleFavorite(
            sourceId = wallpaper.id,
            source = Source.WALLHAVEN,
        )
    }
}

private val tempTags = List(3) {
    Tag(
        id = it + 1L,
        name = "Loading...", // no need for string resource as "Loading..." won't be visible
        alias = emptyList(),
        categoryId = 0,
        category = "",
        purity = Purity.SFW,
        createdAt = Clock.System.now(),
    )
}

@Stable
@Partialize
data class HomeUiState(
    val tags: ImmutableList<Tag> = persistentListOf(),
    val areTagsLoading: Boolean = true,
    val search: Search = Search(
        filters = SearchQuery(
            sorting = Sorting.TOPLIST,
            topRange = TopRange.ONE_DAY,
        ),
    ),
    val showFilters: Boolean = false,
    val wallpapersLoading: Boolean = false,
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val showNSFW: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val isHome: Boolean = false,
    val saveSearchAsSearch: Search? = null,
    val showSavedSearchesDialog: Boolean = false,
    val savedSearches: List<SavedSearch> = emptyList(),
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val favorites: ImmutableList<Favorite> = persistentListOf(),
)
