package com.ammar.wallflow.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalErrorType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WallhavenApiKeyViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val globalErrorsRepository: GlobalErrorsRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = appPreferencesRepository.appPreferencesFlow.mapLatest { appPreferences ->
        WallhavenApiKeyUiState(
            apiKey = appPreferences.wallhavenApiKey,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WallhavenApiKeyUiState(),
    )

    fun updateWallhavenApiKey(wallhavenApiKey: String) {
        viewModelScope.launch {
            appPreferencesRepository.updateWallhavenApiKey(wallhavenApiKey)
            globalErrorsRepository.removeErrorByType(GlobalErrorType.WALLHAVEN_UNAUTHORISED)
        }
    }
}

data class WallhavenApiKeyUiState(
    val apiKey: String = "",
)
