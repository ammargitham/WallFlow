package com.ammar.wallflow.ui.common.bottombar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ammar.wallflow.NavGraphs
import com.ammar.wallflow.R
import com.ramcosta.composedestinations.spec.Route

enum class BottomBarDestination(
    val graph: Route,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
) {
    Home(NavGraphs.home, R.drawable.baseline_home_24, R.string.home),
    Collections(NavGraphs.collections, R.drawable.baseline_collections_24, R.string.collections),
    Local(NavGraphs.local, R.drawable.baseline_folder_24, R.string.local),
    More(NavGraphs.more, R.drawable.baseline_more_horiz_24, R.string.more),
}
