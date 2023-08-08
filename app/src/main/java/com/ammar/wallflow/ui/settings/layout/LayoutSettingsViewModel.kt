package com.ammar.wallflow.ui.settings.layout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.github.materiiapps.partial.Partialize
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LayoutSettingsViewModel @Inject constructor(
    application: Application,
    private val appPreferencesRepository: AppPreferencesRepository,
) : AndroidViewModel(application) {
    private val localUiStateFlow = MutableStateFlow(LayoutSettingsUiStatePartial())

    val uiState = combine(
        appPreferencesRepository.appPreferencesFlow,
        localUiStateFlow,
    ) { appPreferences, localUiState ->
        localUiState.merge(
            LayoutSettingsUiState(
                appPreferences = appPreferences,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LayoutSettingsUiState(),
    )

    fun updatePreferences(preferences: LayoutPreferences) = viewModelScope.launch {
        appPreferencesRepository.updateLookAndFeelPreferences(
            uiState.value.appPreferences.lookAndFeelPreferences.copy(
                layoutPreferences = preferences,
            ),
        )
    }
}

@Partialize
data class LayoutSettingsUiState(
    val appPreferences: AppPreferences = AppPreferences(),
)
