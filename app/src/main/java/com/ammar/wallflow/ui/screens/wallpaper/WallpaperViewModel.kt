package com.ammar.wallflow.ui.screens.wallpaper

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    private val localUiState = MutableStateFlow(WallpaperUiStatePartial())

    val uiState = combine(
        localUiState,
        appPreferencesRepository.appPreferencesFlow,
    ) { local, appPreferences ->
        local.merge(
            WallpaperUiState(
                prevMainWallhavenSearch = appPreferences.mainWallhavenSearch,
                prevMainRedditSearch = appPreferences.mainRedditSearch,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WallpaperUiState(),
    )

    fun onWallpaperTap() = localUiState.update {
        it.copy(
            systemBarsVisible = partial(!it.systemBarsVisible.getOrElse { true }),
        )
    }

    // fun onWallpaperTransform() = _uiState.update {
    //     it.copy(
    //         systemBarsVisible = false,
    //     )
    // }
    fun onWallpaperTransform() {}
}

@Stable
@Partialize
data class WallpaperUiState(
    val systemBarsVisible: Boolean = true,
    val prevMainWallhavenSearch: WallhavenSearch? = null,
    val prevMainRedditSearch: RedditSearch? = null,
)
