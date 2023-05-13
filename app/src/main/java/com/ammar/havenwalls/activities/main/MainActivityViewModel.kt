package com.ammar.havenwalls.activities.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This view model is shared across destinations to perform some top level actions
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val globalErrorsRepository: GlobalErrorsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            globalErrorsRepository.errors.collectLatest { errorList ->
                _uiState.update {
                    it.copy(globalErrors = errorList)
                }
            }
        }
    }

    // fun setTopAppBarVisible(visible: Boolean) = _uiState.update {
    //     it.copy(topAppBarVisible = visible)
    // }
    //
    // fun toggleTopAppBarVisibility() = _uiState.update {
    //     it.copy(topAppBarVisible = !it.topAppBarVisible)
    // }
    //
    // fun setTopAppBarGradientBg(enabled: Boolean) = _uiState.update {
    //     it.copy(topAppBarGradientBg = enabled)
    // }
    //
    // fun setTopAppBarTitleVisible(visible: Boolean) = _uiState.update {
    //     it.copy(topAppBarTitleVisible = visible)
    // }

    fun applyScaffoldPadding(apply: Boolean) = _uiState.update {
        it.copy(applyScaffoldPadding = apply)
    }

    fun dismissGlobalError(error: GlobalError) = globalErrorsRepository.removeError(error)
}

data class MainUiState(
    // val topAppBarVisible: Boolean = true,
    // val topAppBarGradientBg: Boolean = false,
    // val topAppBarTitleVisible: Boolean = true,
    val applyScaffoldPadding: Boolean = true,
    val globalErrors: List<GlobalError> = emptyList(),
)
