package com.ammar.wallflow.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.github.materiiapps.partial.Partialize
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val globalErrorsRepository: GlobalErrorsRepository,
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    private val localUiState = MutableStateFlow(MainUiStatePartial())

    val uiState = combine(
        localUiState,
        globalErrorsRepository.errors,
        appPreferencesRepository.appPreferencesFlow,
    ) { local, errors, appPreferences ->
        local.merge(
            MainUiState(
                globalErrors = errors,
                showLocalTab = appPreferences.lookAndFeelPreferences.showLocalTab,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(),
    )

    fun dismissGlobalError(
        error: GlobalErrorsRepository.GlobalError,
    ) = globalErrorsRepository.removeError(error)
}

@Partialize
data class MainUiState(
    val globalErrors: List<GlobalErrorsRepository.GlobalError> = emptyList(),
    val showLocalTab: Boolean = true,
)
