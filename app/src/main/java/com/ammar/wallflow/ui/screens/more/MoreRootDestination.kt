package com.ammar.wallflow.ui.screens.more

import com.ammar.wallflow.ui.navigation.NavGraphs
import com.ammar.wallflow.ui.screens.NavGraph

internal enum class ActiveOption {
    SETTINGS,
    // OSL,
}

@Suppress("unused")
internal enum class MoreRootDestination(
    val graph: NavGraph,
    val activeOption: ActiveOption,
) {
    Settings(NavGraphs.settings, ActiveOption.SETTINGS),
    // OpenSourceLicenses(NavGraphs.openSourceLicenses, ActiveOption.OSL),
}
