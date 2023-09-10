package com.ammar.wallflow.ui.screens.favorites

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ammar.wallflow.data.db.entity.toFavorite
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Wallpaper
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    application: Application,
    private val favoritesRepository: FavoritesRepository,
    appPreferencesRepository: AppPreferencesRepository,
) : AndroidViewModel(
    application = application,
) {
    val favoriteWallpapers = favoritesRepository.favoriteWallpapersPager(
        context = application,
    ).cachedIn(viewModelScope)
    private val localUiState = MutableStateFlow(FavoritesUiStatePartial())

    val uiState = combine(
        localUiState,
        appPreferencesRepository.appPreferencesFlow,
        favoritesRepository.observeAll(),
    ) { local, appPreferences, favorites ->
        local.merge(
            FavoritesUiState(
                blurSketchy = appPreferences.blurSketchy,
                blurNsfw = appPreferences.blurNsfw,
                layoutPreferences = appPreferences.lookAndFeelPreferences.layoutPreferences,
                favorites = favorites.map { it.toFavorite() }.toImmutableList(),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState(),
    )

    fun setSelectedWallpaper(wallpaper: Wallpaper) = localUiState.update {
        it.copy(selectedWallpaper = partial(wallpaper))
    }

    fun toggleFavorite(wallpaper: Wallpaper) = viewModelScope.launch {
        favoritesRepository.toggleFavorite(
            sourceId = wallpaper.id,
            source = wallpaper.source,
        )
    }
}

@Stable
@Partialize
data class FavoritesUiState(
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val favorites: ImmutableList<Favorite> = persistentListOf(),
)
