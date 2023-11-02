package com.ammar.wallflow.ui.screens.wallpaper

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class WallpaperViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(WallpaperUiState())
    val uiState: StateFlow<WallpaperUiState> = _uiState.asStateFlow()

    fun onWallpaperTap() = _uiState.update {
        it.copy(
            systemBarsVisible = !it.systemBarsVisible,
        )
    }

    // fun onWallpaperTransform() = _uiState.update {
    //     it.copy(
    //         systemBarsVisible = false,
    //     )
    // }
    fun onWallpaperTransform() {}
}

data class WallpaperUiState(
    val systemBarsVisible: Boolean = true,
)
