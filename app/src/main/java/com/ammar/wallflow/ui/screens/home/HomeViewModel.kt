package com.ammar.wallflow.ui.screens.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.db.entity.wallhaven.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.wallhaven.toSavedSearch
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.ui.screens.navArgs
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
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
        it.homeWallhavenSearch
    }.distinctUntilChanged()

    val wallpapers = if (mainSearch != null) {
        when (mainSearch) {
            is WallhavenSearch -> wallHavenRepository.wallpapersPager(mainSearch)
            is RedditSearch -> TODO()
        }
    } else {
        homeSearchFlow.flatMapLatest {
            wallHavenRepository.wallpapersPager(it)
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
                wallhaven = WallhavenState(
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
                ),
                mainSearch = mainSearch,
                homeSearch = appPreferences.homeWallhavenSearch,
                savedSearches = savedSearchEntities.map(
                    SavedSearchEntity::toSavedSearch,
                ),
                blurSketchy = appPreferences.blurSketchy,
                blurNsfw = appPreferences.blurNsfw,
                showNSFW = appPreferences.wallhavenApiKey.isNotBlank(),
                layoutPreferences = appPreferences.lookAndFeelPreferences.layoutPreferences,
                favorites = favorites.map(FavoriteEntity::toFavorite).toImmutableList(),
                sources = persistentMapOf(
                    OnlineSource.WALLHAVEN to true,
                ),
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
            when (search) {
                is WallhavenSearch -> appPreferencesRepository.updateHomeWallhavenSearch(search)
                is RedditSearch -> appPreferencesRepository.updateHomeRedditSearch(search)
            }
        }
    }

    fun showFilters(show: Boolean) = localUiState.update {
        it.copy(showFilters = partial(show))
    }

    fun setSelectedWallpaper(wallpaper: Wallpaper) = localUiState.update {
        it.copy(selectedWallpaper = partial(wallpaper))
    }

    fun showSaveSearchAsDialog(search: Search? = null) = localUiState.update {
        it.copy(
            saveSearchAsSearch = partial(search),
        )
    }

    fun saveSearchAs(name: String, search: Search) = viewModelScope.launch {
        savedSearchRepository.upsert(
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
            source = wallpaper.source,
        )
    }

    fun showManageSourcesDialog(show: Boolean) = localUiState.update {
        it.copy(showManageSourcesDialog = partial(show))
    }

    fun addSource(source: OnlineSource) = localUiState.update {
        when (source) {
            OnlineSource.WALLHAVEN -> it // will never be called
            OnlineSource.REDDIT -> it.copy(
                showManageSourcesDialog = partial(false),
                showRedditInitDialog = partial(true),
            )
        }
    }

    fun showRedditInitDialog(show: Boolean) = localUiState.update {
        it.copy(showRedditInitDialog = partial(show))
    }

    fun updateRedditConfigAndCloseDialog(subreddits: Set<String>) = localUiState.update {
        it.copy(showRedditInitDialog = partial(false))
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
    val selectedSource: OnlineSource = OnlineSource.WALLHAVEN,
    val sources: ImmutableMap<OnlineSource, Boolean> = persistentMapOf(
        OnlineSource.WALLHAVEN to true,
    ),
    val mainSearch: Search? = null,
    val homeSearch: Search = WallhavenSearch(
        filters = WallhavenFilters(
            sorting = WallhavenSorting.TOPLIST,
            topRange = WallhavenTopRange.ONE_DAY,
        ),
    ),
    val saveSearchAsSearch: Search? = null,
    val savedSearches: List<SavedSearch> = emptyList(),
    val wallhaven: WallhavenState = WallhavenState(),
    val showFilters: Boolean = false,
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val showNSFW: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val showSavedSearchesDialog: Boolean = false,
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val favorites: ImmutableList<Favorite> = persistentListOf(),
    val showManageSourcesDialog: Boolean = false,
    val showRedditInitDialog: Boolean = false,
) {
    val isHome = when (selectedSource) {
        OnlineSource.WALLHAVEN -> mainSearch == null
        OnlineSource.REDDIT -> TODO()
    }
    val showSaveAsDialog = when (selectedSource) {
        OnlineSource.WALLHAVEN -> saveSearchAsSearch != null
        OnlineSource.REDDIT -> TODO()
    }
}

data class WallhavenState(
    val wallhavenTags: ImmutableList<WallhavenTag> = persistentListOf(),
    val areTagsLoading: Boolean = true,
)
