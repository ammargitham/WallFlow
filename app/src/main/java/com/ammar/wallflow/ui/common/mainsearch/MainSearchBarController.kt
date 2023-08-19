package com.ammar.wallflow.ui.common.mainsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.ammar.wallflow.model.Search

abstract class MainSearchBarController {
    abstract val state: State<MainSearchBarState>
    abstract fun update(fn: (prevState: MainSearchBarState) -> MainSearchBarState)
}

data class MainSearchBarState(
    val visible: Boolean = true,
    val search: Search = MainSearchBar.Defaults.search,
    val showQuery: Boolean = true,
    val overflowIcon: @Composable (() -> Unit)? = null,
    val onActiveChange: (active: Boolean) -> Unit = {},
)

class DefaultMainSearchBarController(initialState: MainSearchBarState) : MainSearchBarController() {
    private var _state: MutableState<MainSearchBarState> = mutableStateOf(initialState)
    override val state: State<MainSearchBarState> = _state

    override fun update(fn: (prevState: MainSearchBarState) -> MainSearchBarState) {
        val newState = fn(state.value)
        _state.value = newState
    }
}

val LocalMainSearchBarController: ProvidableCompositionLocal<MainSearchBarController> =
    staticCompositionLocalOf {
        DefaultMainSearchBarController(MainSearchBarState())
    }
