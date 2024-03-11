package com.ammar.wallflow.ui.screens.local

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.db.entity.toLightDark
import com.ammar.wallflow.data.db.entity.toViewed
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.Viewed
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.utils.getLocalDirs
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
import kotlinx.coroutines.flow.flatMapLatest
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
    lightDarkRepository: LightDarkRepository,
) : AndroidViewModel(application) {
    private val localUiState = MutableStateFlow(LocalScreenUiStatePartial())
    private val appPreferencesFlow = appPreferencesRepository.appPreferencesFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpapers = appPreferencesFlow.flatMapLatest {
        val sort = it.localWallpapersPreferences.sort
        val localDirs = getLocalDirs(application, it)
        localWallpapersRepository.wallpapersPager(
            context = application,
            uris = localDirs.map { d -> d.uri },
            sort = sort,
        )
    }.cachedIn(viewModelScope)

    val uiState = combine(
        localUiState,
        favoritesRepository.observeAll(),
        appPreferencesFlow,
        viewedRepository.observeAll(),
        lightDarkRepository.observeAll(),
    ) {
            local,
            favorites,
            appPreferences,
            viewedList,
            lightDarkList,
        ->
        local.merge(
            LocalScreenUiState(
                folders = getLocalDirs(application, appPreferences).toImmutableList(),
                favorites = favorites.map { it.toFavorite() }.toImmutableList(),
                viewedList = viewedList.map(ViewedEntity::toViewed).toImmutableList(),
                viewedWallpapersLook = appPreferences.viewedWallpapersPreferences.look,
                lightDarkList = lightDarkList.map(LightDarkEntity::toLightDark).toImmutableList(),
                sort = appPreferences.localWallpapersPreferences.sort,
                downloadLocation = appPreferences.downloadLocation,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LocalScreenUiState(),
    )

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

    fun addLocalDir(uri: Uri) = viewModelScope.launch {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val currentDirs = uiState.value.folders.map { it.uri }
        if (currentDirs.contains(uri)) {
            return@launch
        }
        appPreferencesRepository.updateLocalDirs((currentDirs + uri).toSet())
    }

    fun removeLocalDir(uri: Uri) = viewModelScope.launch {
        val currentDownloadLocation = uiState.value.downloadLocation
        // do not release permission if this uri is also our custom download dir
        if (uri != currentDownloadLocation) {
            application.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val currentDirs = uiState.value.folders.map { it.uri }
        appPreferencesRepository.updateLocalDirs((currentDirs - uri).toSet())
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
    val lightDarkList: ImmutableList<LightDark> = persistentListOf(),
    val sort: LocalSort = LocalSort.NO_SORT,
    val downloadLocation: Uri? = null,
)

enum class LocalSort {
    NO_SORT,
    NAME,
    LAST_MODIFIED,
}
