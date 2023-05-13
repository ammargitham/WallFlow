package com.ammar.havenwalls.ui.common.fab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

abstract class FABController {
    abstract fun update(newState: FABState = FABState())
}

data class FABState(
    val icon: @Composable () -> Unit = {},
    val text: @Composable () -> Unit = {},
    val expanded: Boolean = true,
    val onClick: () -> Unit = {},
    val visible: Boolean = false,
)

class DefaultFABController(initialState: FABState) : FABController() {
    private var _icon: MutableState<@Composable () -> Unit> = mutableStateOf(initialState.icon)
    val icon: State<@Composable () -> Unit> = _icon

    private var _text: MutableState<@Composable () -> Unit> = mutableStateOf(initialState.text)
    val text: State<@Composable () -> Unit> = _text

    private var _expanded: MutableState<Boolean> = mutableStateOf(initialState.expanded)
    val expanded: State<Boolean> = _expanded

    private var _onClick: MutableState<() -> Unit> = mutableStateOf(initialState.onClick)
    val onClick: State<() -> Unit> = _onClick

    private var _visible: MutableState<Boolean> = mutableStateOf(initialState.visible)
    val visible: State<Boolean> = _visible

    override fun update(newState: FABState) {
        _icon.value = newState.icon
        _text.value = newState.text
        _expanded.value = newState.expanded
        _onClick.value = newState.onClick
        _visible.value = newState.visible
    }
}

@Composable
fun rememberFABController(
    initialState: FABState = FABState(),
): DefaultFABController = remember { DefaultFABController(initialState) }
