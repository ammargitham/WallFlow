package com.ammar.havenwalls.ui.common.bottombar

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntSize

abstract class BottomBarController {
    abstract val state: State<BottomBarState>
    abstract fun update(fn: (prevState: BottomBarState) -> BottomBarState)
}

data class BottomBarState(
    val visible: Boolean = true,
    val size: IntSize = IntSize.Zero,
    val isRail: Boolean = false,
)

class DefaultBottomBarController(initialState: BottomBarState) : BottomBarController() {
    private var _state: MutableState<BottomBarState> = mutableStateOf(initialState)
    override val state: State<BottomBarState> = _state

    override fun update(fn: (prevState: BottomBarState) -> BottomBarState) {
        val newState = fn(state.value)
        _state.value = _state.value.copy(
            visible = newState.visible,
            size = newState.size,
            isRail = newState.isRail,
        )
    }
}

val LocalBottomBarController: ProvidableCompositionLocal<BottomBarController> =
    staticCompositionLocalOf {
        DefaultBottomBarController(BottomBarState())
    }
