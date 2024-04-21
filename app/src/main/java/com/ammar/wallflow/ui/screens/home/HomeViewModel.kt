package com.ammar.wallflow.ui.screens.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.search.toSavedSearch
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.db.entity.toLightDark
import com.ammar.wallflow.data.db.entity.toViewed
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.data.repository.reddit.RedditRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.navArgs
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wallhavenRepository: WallhavenRepository,
    private val redditRepository: RedditRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val savedSearchRepository: SavedSearchRepository,
    private val favoritesRepository: FavoritesRepository,
    viewedRepository: ViewedRepository,
    lightDarkRepository: LightDarkRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mainSearch = savedStateHandle.navArgs<HomeScreenNavArgs>().search
    private val popularTags = wallhavenRepository.popularTags()
    private val localUiState = MutableStateFlow(HomeUiStatePartial())
    private val homeSearchFlow = combine(
        localUiState,
        appPreferencesRepository.appPreferencesFlow,
    ) { local, appPreferences ->
        val source = getCorrectSelectedSource(
            local = local,
            homeSource = appPreferences.homeSources,
            homeRedditSearch = appPreferences.homeRedditSearch,
        )
        when (source) {
            OnlineSource.WALLHAVEN -> appPreferences.homeWallhavenSearch
            OnlineSource.REDDIT -> appPreferences.homeRedditSearch
        }
    }.filterNotNull().distinctUntilChanged()

    private fun getCorrectSelectedSource(
        local: HomeUiStatePartial,
        homeSource: Map<OnlineSource, Boolean>,
        homeRedditSearch: RedditSearch?,
    ): OnlineSource {
        var source = local.selectedSource.getOrElse { OnlineSource.WALLHAVEN }
        val sourceEnabled = homeSource[source] ?: false
        if (!sourceEnabled) {
            source = OnlineSource.entries.first { it != source }
        }
        if (source == OnlineSource.REDDIT && homeRedditSearch == null) {
            // fallback to wallhaven
            source = OnlineSource.WALLHAVEN
        }
        return source
    }

    val wallpapers = if (mainSearch != null) {
        when (mainSearch) {
            is WallhavenSearch -> wallhavenRepository.wallpapersPager(mainSearch)
            is RedditSearch -> redditRepository.wallpapersPager(mainSearch)
        }
    } else {
        homeSearchFlow.flatMapLatest {
            when (it) {
                is WallhavenSearch -> wallhavenRepository.wallpapersPager(it)
                is RedditSearch -> redditRepository.wallpapersPager(it)
            }
        }
    }.cachedIn(viewModelScope)

    init {
        if (mainSearch != null) {
            // save search to app prefs
            viewModelScope.launch {
                appPreferencesRepository.updateMainSearch(mainSearch)
            }
        }
        viewModelScope.launch {
            localUiState.update {
                val appPreferences = appPreferencesRepository.appPreferencesFlow.first()
                it.copy(
                    selectedSource = partial(
                        getCorrectSelectedSource(
                            local = it,
                            homeSource = appPreferences.homeSources,
                            homeRedditSearch = appPreferences.homeRedditSearch,
                        ),
                    ),
                )
            }
        }
    }

    val uiState = com.ammar.wallflow.utils.combine(
        popularTags,
        homeSearchFlow,
        appPreferencesRepository.appPreferencesFlow,
        localUiState,
        savedSearchRepository.observeAll(),
        favoritesRepository.observeAll(),
        viewedRepository.observeAll(),
        lightDarkRepository.observeAll(),
    ) {
            tags,
            homeSearch,
            appPreferences,
            local,
            savedSearchEntities,
            favorites,
            viewedList,
            lightDarkList,
        ->
        local.merge(
            HomeUiState(
                wallhaven = WallhavenState(
                    wallhavenTags = (
                        if (tags is Resource.Loading) {
                            tempWallhavenTags
                        } else {
                            tags.successOr(emptyList())
                        }
                        ).toImmutableList(),
                    areTagsLoading = tags is Resource.Loading,
                ),
                reddit = RedditState(
                    subreddits = appPreferences.homeRedditSearch?.filters?.subreddits
                        ?.toImmutableSet()
                        ?: persistentSetOf(),
                ),
                mainSearch = mainSearch,
                homeSearch = homeSearch,
                savedSearches = savedSearchEntities.map(
                    SavedSearchEntity::toSavedSearch,
                ),
                blurSketchy = appPreferences.blurSketchy,
                blurNsfw = appPreferences.blurNsfw,
                showNSFW = appPreferences.wallhavenApiKey.isNotBlank(),
                layoutPreferences = appPreferences.lookAndFeelPreferences.layoutPreferences,
                favorites = favorites.map(FavoriteEntity::toFavorite).toImmutableList(),
                viewedList = viewedList.map(ViewedEntity::toViewed).toImmutableList(),
                viewedWallpapersLook = appPreferences.viewedWallpapersPreferences.look,
                lightDarkList = lightDarkList.map(LightDarkEntity::toLightDark).toImmutableList(),
                sources = appPreferences.homeSources.toImmutableMap(),
                prevMainWallhavenSearch = appPreferences.mainWallhavenSearch,
                prevMainRedditSearch = appPreferences.mainRedditSearch,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun refresh() {
        viewModelScope.launch {
            wallhavenRepository.refreshPopularTags()
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

    fun showSavedSearches(
        show: Boolean = true,
        isFromSearchBar: Boolean = false,
    ) = localUiState.update {
        it.copy(
            showSavedSearchesDialog = partial(show),
            showSavedSearchesForSearchBar = partial(isFromSearchBar),
        )
    }

    fun toggleFavorite(wallpaper: Wallpaper) = viewModelScope.launch {
        favoritesRepository.toggleFavorite(
            sourceId = wallpaper.id,
            source = wallpaper.source,
        )
    }

    fun showManageSourcesDialog(show: Boolean) = localUiState.update {
        it.copy(
            manageSourcesState = partial(
                ManageSourcesState(
                    showDialog = show,
                    currentSources = if (show) {
                        uiState.value.sources
                    } else {
                        uiState.value.manageSourcesState.currentSources
                    },
                ),
            ),
        )
    }

    fun addSource(source: OnlineSource) {
        localUiState.update {
            when (source) {
                OnlineSource.WALLHAVEN -> it // will never be called
                OnlineSource.REDDIT -> it.copy(
                    showRedditInitDialog = partial(true),
                )
            }
        }
        showManageSourcesDialog(false)
    }

    fun showRedditInitDialog(show: Boolean) = localUiState.update {
        it.copy(showRedditInitDialog = partial(show))
    }

    fun updateRedditConfigAndCloseDialog(subreddits: Set<String>) = viewModelScope.launch {
        appPreferencesRepository.updateHomeRedditSearch(
            RedditSearch(
                query = "",
                filters = RedditFilters(
                    subreddits = subreddits,
                    includeNsfw = false,
                    sort = RedditSort.TOP,
                    timeRange = RedditTimeRange.WEEK,
                ),
            ),
        )
        localUiState.update {
            it.copy(showRedditInitDialog = partial(false))
        }
    }

    fun changeSource(source: OnlineSource) {
        if (uiState.value.selectedSource == source) {
            return
        }
        localUiState.update {
            it.copy(selectedSource = partial(source))
        }
    }

    fun updateManageSourcesDialogSources(
        sources: Map<OnlineSource, Boolean>,
    ) = localUiState.update {
        val existingSources = uiState.value.sources
        val updatedSources = sources.toPersistentMap()
        val saveEnabled = existingSources != updatedSources
        it.copy(
            manageSourcesState = partial(
                uiState.value.manageSourcesState.copy(
                    currentSources = updatedSources,
                    saveEnabled = saveEnabled,
                ),
            ),
        )
    }

    fun saveManageSources() = viewModelScope.launch {
        val manageSourcesState = uiState.value.manageSourcesState
        val currentSources = manageSourcesState.currentSources
        val allDisabled = currentSources.all { !it.value }
        if (allDisabled) {
            return@launch
        }
        appPreferencesRepository.updateHomeSources(currentSources)
        localUiState.update {
            it.copy(
                selectedSource = partial(
                    getCorrectSelectedSource(
                        local = it,
                        homeSource = currentSources,
                        homeRedditSearch = appPreferencesRepository.appPreferencesFlow
                            .first()
                            .homeRedditSearch,
                    ),
                ),
            )
        }
        showManageSourcesDialog(false)
    }

    suspend fun checkSavedSearchNameExists(name: String) = savedSearchRepository.exists(name)
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
    val sources: ImmutableMap<OnlineSource, Boolean> = persistentMapOf(),
    val mainSearch: Search? = null,
    val selectedSource: OnlineSource = OnlineSource.WALLHAVEN,
    val homeSearch: Search = WallhavenSearch(
        filters = WallhavenFilters(
            sorting = WallhavenSorting.TOPLIST,
            topRange = WallhavenTopRange.ONE_DAY,
        ),
    ),
    val saveSearchAsSearch: Search? = null,
    val savedSearches: List<SavedSearch> = emptyList(),
    val wallhaven: WallhavenState = WallhavenState(),
    val reddit: RedditState = RedditState(),
    val showFilters: Boolean = false,
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val showNSFW: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val showSavedSearchesDialog: Boolean = false,
    val showSavedSearchesForSearchBar: Boolean = false,
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val favorites: ImmutableList<Favorite> = persistentListOf(),
    val viewedList: ImmutableList<Viewed> = persistentListOf(),
    val viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    val lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    val showRedditInitDialog: Boolean = false,
    val manageSourcesState: ManageSourcesState = ManageSourcesState(),
    val prevMainWallhavenSearch: WallhavenSearch? = null,
    val prevMainRedditSearch: RedditSearch? = null,
) {
    val isHome = mainSearch == null
    val showSaveAsDialog = saveSearchAsSearch != null
}

data class WallhavenState(
    val wallhavenTags: ImmutableList<WallhavenTag> = persistentListOf(),
    val areTagsLoading: Boolean = true,
)

data class RedditState(
    val subreddits: ImmutableSet<String> = persistentSetOf(),
)

data class ManageSourcesState(
    val showDialog: Boolean = false,
    val currentSources: ImmutableMap<OnlineSource, Boolean> = persistentMapOf(),
    val saveEnabled: Boolean = false,
)
