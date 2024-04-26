package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    sheetState: AdaptiveBottomSheetState = rememberAdaptiveBottomSheetState(
        bottomSheetState = rememberModalBottomSheetState(),
        sideSheetState = rememberSideSheetState(),
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val systemState by LocalSystemController.current.state
    if (systemState.isExpanded) {
        ModalSideSheet(
            modifier = modifier.padding(
                top = 16.dp,
            ),
            state = sheetState.sideSheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onDismissRequest()
                }
            },
            content = content,
        )
    } else {
        ModalBottomSheet(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            sheetState = sheetState.bottomSheetState,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class AdaptiveBottomSheetState(
    private val isExpanded: Boolean,
    internal val bottomSheetState: SheetState,
    internal val sideSheetState: SideSheetState,
) {
    val isVisible
        get() = if (isExpanded) {
            sideSheetState.isVisible
        } else {
            bottomSheetState.isVisible
        }

    suspend fun show() {
        if (isExpanded) {
            sideSheetState.show()
        } else {
            bottomSheetState.show()
        }
    }

    suspend fun hide() {
        if (isExpanded) {
            sideSheetState.hide()
        } else {
            bottomSheetState.hide()
        }
    }

    suspend fun expand() {
        if (!isExpanded) {
            bottomSheetState.expand()
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun rememberAdaptiveBottomSheetState(
    bottomSheetState: SheetState = rememberModalBottomSheetState(),
    sideSheetState: SideSheetState = rememberSideSheetState(),
): AdaptiveBottomSheetState {
    val systemState by LocalSystemController.current.state
    return remember {
        AdaptiveBottomSheetState(
            isExpanded = systemState.isExpanded,
            bottomSheetState = bottomSheetState,
            sideSheetState = sideSheetState,
        )
    }
}
