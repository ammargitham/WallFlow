package com.ammar.wallflow.ui.common.bottombar

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.destinations.FavoritesScreenDestination
import com.ammar.wallflow.ui.destinations.HomeScreenDestination
import com.ammar.wallflow.ui.destinations.TypedDestination

@Suppress("unused")
enum class BottomBarDestination(
    val direction: TypedDestination<*>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    Home(HomeScreenDestination, Icons.Default.Home, R.string.home),
    Favorites(FavoritesScreenDestination, Icons.Default.Favorite, R.string.favorites),
}
