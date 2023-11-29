package com.ammar.wallflow.ui.screens.local

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.db.entity.toViewed
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.extensions.accessibleFolders
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.utils.getRealPath
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LocalScreenViewModel @Inject constructor(
    private val application: Application,
    private val localWallpapersRepository: LocalWallpapersRepository,
    private val favoritesRepository: FavoritesRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    viewedRepository: ViewedRepository,
) : AndroidViewModel(application) {
    private val localUiState = MutableStateFlow(LocalScreenUiStatePartial())
    private val foldersFlow = localUiState
        .map { it.folders.getOrElse { persistentListOf() } }
        .distinctUntilChanged()
    private val appPreferencesFlow = appPreferencesRepository.appPreferencesFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    private val foldersUriFlow = foldersFlow.mapLatest {
        it.map { dir -> dir.uri }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpapers = combine(
        foldersUriFlow,
        appPreferencesFlow,
    ) { folders, appPrefs ->
        folders to appPrefs.localWallpapersPreferences.sort
    }.flatMapLatest {
        localWallpapersRepository.wallpapersPager(
            context = application,
            uris = it.first,
            sort = it.second,
        )
    }.cachedIn(viewModelScope)

    val uiState = combine(
        localUiState,
        favoritesRepository.observeAll(),
        appPreferencesFlow,
        viewedRepository.observeAll(),
    ) {
            local,
            favorites,
            appPreferences,
            viewedList,
        ->
        local.merge(
            LocalScreenUiState(
                favorites = favorites.map { it.toFavorite() }.toImmutableList(),
                viewedList = viewedList.map(ViewedEntity::toViewed).toImmutableList(),
                viewedWallpapersLook = appPreferences.viewedWallpapersPreferences.look,
                sort = appPreferences.localWallpapersPreferences.sort,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LocalScreenUiState(),
    )

    init {
        refreshFolders()
    }

    fun refreshFolders() = localUiState.update {
        it.copy(
            folders = partial(
                application.accessibleFolders
                    .map { p ->
                        LocalDirectory(
                            uri = p.uri,
                            path = getRealPath(
                                context = application,
                                uri = p.uri,
                            ) ?: p.uri.toString(),
                        )
                    }.toImmutableList(),
            ),
        )
    }

    fun showManageFoldersSheet(show: Boolean) = localUiState.update {
        it.copy(showManageFoldersSheet = partial(show))
    }

    fun setSelectedWallpaper(wallpaper: Wallpaper) = localUiState.update {
        it.copy(selectedWallpaper = partial(wallpaper))
    }

    fun toggleFavorite(wallpaper: Wallpaper) = viewModelScope.launch {
        favoritesRepository.toggleFavorite(
            sourceId = wallpaper.id,
            source = wallpaper.source,
        )
    }

    fun updateSort(sort: LocalSort) = viewModelScope.launch {
        appPreferencesRepository.updateLocalWallpapersSort(sort)
    }
}

@Stable
@Partialize
data class LocalScreenUiState(
    val showManageFoldersSheet: Boolean = false,
    val folders: ImmutableList<LocalDirectory> = persistentListOf(),
    val selectedWallpaper: Wallpaper? = null,
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val favorites: ImmutableList<Favorite> = persistentListOf(),
    val viewedList: ImmutableList<Viewed> = persistentListOf(),
    val viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    val sort: LocalSort = LocalSort.NO_SORT,
)

enum class LocalSort {
    NO_SORT,
    NAME,
    LAST_MODIFIED,
}
