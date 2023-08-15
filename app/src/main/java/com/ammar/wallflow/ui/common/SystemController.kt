package com.ammar.wallflow.ui.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize

abstract class SystemController {
    abstract val state: State<SystemState>
    abstract fun update(fn: (prevState: SystemState) -> SystemState)
    abstract fun resetBarsState()
}

data class SystemState(
    val statusBarVisible: Boolean = true,
    val statusBarColor: Color = Color.Unspecified,
    val navigationBarVisible: Boolean = true,
    val navigationBarColor: Color = Color.Unspecified,
    val lightStatusBars: Boolean? = null,
    val lightNavigationBars: Boolean? = null,
    val isExpanded: Boolean = false,
    val size: IntSize = IntSize.Zero,
    val applyScaffoldPadding: Boolean = true,
)

private class DefaultSystemController(initialState: SystemState) : SystemController() {
    private var _state: MutableState<SystemState> = mutableStateOf(initialState)
    override val state: State<SystemState> = _state

    override fun update(fn: (prevState: SystemState) -> SystemState) {
        val newState = fn(state.value)
        _state.value = newState
    }

    override fun resetBarsState() {
        _state.value = SystemState(
            isExpanded = _state.value.isExpanded,
            size = _state.value.size,
            applyScaffoldPadding = _state.value.applyScaffoldPadding,
        )
    }
}

val LocalSystemController: ProvidableCompositionLocal<SystemController> =
    staticCompositionLocalOf {
        DefaultSystemController(SystemState())
    }
