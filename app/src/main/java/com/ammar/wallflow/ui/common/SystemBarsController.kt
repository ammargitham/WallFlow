package com.ammar.wallflow.ui.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

abstract class SystemBarsController {
    abstract val state: State<SystemBarsState>
    abstract fun update(fn: (prevState: SystemBarsState) -> SystemBarsState)
    abstract fun reset()
}

data class SystemBarsState(
    val statusBarVisible: Boolean = true,
    val statusBarColor: Color = Color.Unspecified,
    val navigationBarVisible: Boolean = true,
    val navigationBarColor: Color = Color.Unspecified,
    val lightStatusBars: Boolean? = null,
    val lightNavigationBars: Boolean? = null,
)

class DefaultSystemBarsController(initialState: SystemBarsState) : SystemBarsController() {
    private var _state: MutableState<SystemBarsState> = mutableStateOf(initialState)
    override val state: State<SystemBarsState> = _state

    override fun update(fn: (prevState: SystemBarsState) -> SystemBarsState) {
        val newState = fn(state.value)
        _state.value = _state.value.copy(
            statusBarVisible = newState.statusBarVisible,
            statusBarColor = newState.statusBarColor,
            navigationBarVisible = newState.navigationBarVisible,
            navigationBarColor = newState.navigationBarColor,
            lightStatusBars = newState.lightStatusBars,
            lightNavigationBars = newState.lightNavigationBars,
        )
    }

    override fun reset() {
        _state.value = SystemBarsState()
    }
}

val LocalSystemBarsController: ProvidableCompositionLocal<SystemBarsController> =
    staticCompositionLocalOf {
        DefaultSystemBarsController(SystemBarsState())
    }
