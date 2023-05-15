package com.ammar.havenwalls.ui.common.bottombar

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.ammar.havenwalls.R
import com.ammar.havenwalls.ui.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.spec.Direction

enum class BottomBarDestination(
    val direction: Direction,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    Home(HomeScreenDestination, Icons.Default.Home, R.string.home),
    // Search(SearchScreenDestination, Icons.Default.Search, R.string.explore),
}
