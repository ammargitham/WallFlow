package com.ammar.havenwalls.ui.common

import kotlinx.coroutines.flow.StateFlow

interface UiStateViewModel<T> {
    val uiState: StateFlow<T>
}
