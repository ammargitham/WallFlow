package com.ammar.havenwalls.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalErrorType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
