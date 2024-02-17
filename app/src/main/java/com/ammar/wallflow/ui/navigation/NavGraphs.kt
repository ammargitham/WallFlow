package com.ammar.wallflow.ui.navigation

import com.ammar.wallflow.ui.screens.NavGraph
import com.ammar.wallflow.ui.screens.destinations.BackupRestoreScreenDestination
import com.ammar.wallflow.ui.screens.destinations.CollectionsScreenDestination
import com.ammar.wallflow.ui.screens.destinations.HomeScreenDestination
import com.ammar.wallflow.ui.screens.destinations.LayoutSettingsScreenDestination
import com.ammar.wallflow.ui.screens.destinations.LocalScreenDestination
import com.ammar.wallflow.ui.screens.destinations.MoreScreenDestination
import com.ammar.wallflow.ui.screens.destinations.OSLibrariesScreenDestination
import com.ammar.wallflow.ui.screens.destinations.SettingsScreenDestination
import com.ammar.wallflow.ui.screens.destinations.WallhavenApiKeyDialogDestination
import com.ammar.wallflow.ui.screens.destinations.WallpaperScreenDestination

object NavGraphs {

    val home: NavGraph = NavGraph(
        route = "home",
        startRoute = HomeScreenDestination,
        destinations = listOf(
            HomeScreenDestination,
        ),
    )

    val collections: NavGraph = NavGraph(
        route = "collections",
        startRoute = CollectionsScreenDestination,
        destinations = listOf(
            CollectionsScreenDestination,
        ),
    )

    val local: NavGraph = NavGraph(
        route = "local",
        startRoute = LocalScreenDestination,
        destinations = listOf(
            LocalScreenDestination,
        ),
    )

    val settings: NavGraph = NavGraph(
        route = "settings",
        startRoute = SettingsScreenDestination,
        destinations = listOf(
            SettingsScreenDestination,
            WallhavenApiKeyDialogDestination,
            LayoutSettingsScreenDestination,
        ),
    )

    val backup_restore: NavGraph = NavGraph(
        route = "backup_restore",
        startRoute = BackupRestoreScreenDestination,
        destinations = listOf(
            BackupRestoreScreenDestination,
        ),
    )

    val openSourceLicenses: NavGraph = NavGraph(
        route = "open_source_licenses",
        startRoute = OSLibrariesScreenDestination,
        destinations = listOf(
            OSLibrariesScreenDestination,
        ),
    )

    val more: NavGraph = NavGraph(
        route = "more",
        startRoute = MoreScreenDestination,
        destinations = listOf(
            MoreScreenDestination,
        ),
        nestedNavGraphs = listOf(
            settings,
            backup_restore,
            openSourceLicenses,
        ),
    )

    val moreDetail: NavGraph = NavGraph(
        route = "more_detail",
        startRoute = settings,
        destinations = emptyList(),
        nestedNavGraphs = listOf(
            settings,
            backup_restore,
            openSourceLicenses,
        ),
    )

    val root: NavGraph = NavGraph(
        route = "root",
        startRoute = home,
        destinations = listOf(
            WallpaperScreenDestination,
        ),
        nestedNavGraphs = listOf(
            home,
            collections,
            local,
            more,
        ),
    )
}
