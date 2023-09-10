package com.ammar.wallflow.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.ui.common.bottombar.BottomBarController

fun getStartBottomPadding(
    density: Density,
    bottomBarController: BottomBarController,
    bottomWindowInsets: WindowInsets,
    navigationBarsInsets: WindowInsets,
): Dp = with(density) {
    val bottomBarState by bottomBarController.state
    val bottomInsetsPadding = if (bottomBarState.isRail) {
        bottomWindowInsets.getBottom(density).toDp()
    } else {
        0.dp
    }
    val bottomNavPadding = if (bottomBarState.isRail || bottomBarState.visible) {
        0.dp
    } else {
        navigationBarsInsets.getBottom(density).toDp()
    }
    val bottomBarPadding = if (bottomBarState.isRail) {
        0.dp
    } else {
        bottomBarState.size.height.toDp()
    }
    return bottomInsetsPadding + bottomBarPadding + bottomNavPadding
}
