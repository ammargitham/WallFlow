package com.ammar.wallflow.ui.screens.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.db.entity.wallhaven.toWallhavenSavedSearch
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.search.WallhavenSavedSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSearchQuery
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import com.ammar.wallflow.model.search.toSearchQuery
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.ui.screens.navArgs
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wallHavenRepository: WallhavenRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val savedSearchRepository: SavedSearchRepository,
    private val favoritesRepository: FavoritesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mainSearch = savedStateHandle.navArgs<HomeScreenNavArgs>().search
    private val popularTags = wallHavenRepository.popularTags()
    private val localUiState = MutableStateFlow(HomeUiStatePartial())

    private val homeSearchFlow = appPreferencesRepository.appPreferencesFlow.mapLatest {
        it.homeSearch
    }.distinctUntilChanged()

    val wallpapers = if (mainSearch != null) {
        wallHavenRepository.wallpapersPager(mainSearch.toSearchQuery())
    } else {
        homeSearchFlow.flatMapLatest {
            wallHavenRepository.wallpapersPager(it.toSearchQuery())
        }
    }.cachedIn(viewModelScope)

    val uiState = combine(
        popularTags,
        appPreferencesRepository.appPreferencesFlow,
        localUiState,
        savedSearchRepository.observeAll(),
        favoritesRepository.observeAll(),
    ) {
            tags,
            appPreferences,
            local,
            savedSearchEntities,
            favorites,
        ->
        local.merge(
            HomeUiState(
                wallhavenTags = (
                    if (tags is Resource.Loading) {
                        tempWallhavenTags
                    } else {
                        tags.successOr(
                            emptyList(),
                        )
                    }
                    ).toImmutableList(),
                areTagsLoading = tags is Resource.Loading,
                mainSearch = mainSearch,
                homeSearch = appPreferences.homeSearch,
                blurSketchy = appPreferences.blurSketchy,
                blurNsfw = appPreferences.blurNsfw,
                showNSFW = appPreferences.wallhavenApiKey.isNotBlank(),
                savedSearches = savedSearchEntities.map { entity ->
                    entity.toWallhavenSavedSearch()
                },
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

    fun updateHomeSearch(search: WallhavenSearch) {
        viewModelScope.launch {
            appPreferencesRepository.updateHomeSearch(search)
        }
    }

    fun showFilters(show: Boolean) = localUiState.update {
        it.copy(showFilters = partial(show))
    }

    fun setSelectedWallpaper(wallpaper: Wallpaper) = localUiState.update {
        it.copy(selectedWallpaper = partial(wallpaper))
    }

    fun showSaveSearchAsDialog(search: WallhavenSearch? = null) = localUiState.update {
        it.copy(saveSearchAsSearch = partial(search))
    }

    fun saveSearchAs(name: String, search: WallhavenSearch) = viewModelScope.launch {
        savedSearchRepository.upsert(
            WallhavenSavedSearch(
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
            source = wallpaper.source,
        )
    }
}

private val tempWallhavenTags = List(3) {
    WallhavenTag(
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
    val wallhavenTags: ImmutableList<WallhavenTag> = persistentListOf(),
    val areTagsLoading: Boolean = true,
    val mainSearch: WallhavenSearch? = null,
    val homeSearch: WallhavenSearch = WallhavenSearch(
        filters = WallhavenSearchQuery(
            sorting = WallhavenSorting.TOPLIST,
            topRange = WallhavenTopRange.ONE_DAY,
        ),
    ),
    val showFilters: Boolean = false,
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val showNSFW: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val saveSearchAsSearch: WallhavenSearch? = null,
    val showSavedSearchesDialog: Boolean = false,
    val savedSearches: List<WallhavenSavedSearch> = emptyList(),
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val favorites: ImmutableList<Favorite> = persistentListOf(),
) {
    val isHome = mainSearch == null
    val showSaveAsDialog = saveSearchAsSearch != null
}
