package com.ammar.wallflow.ui.screens.more

import com.ammar.wallflow.ui.navigation.NavGraphs
import com.ammar.wallflow.ui.screens.NavGraph

internal enum class ActiveOption {
    SETTINGS,
    BACKUP_RESTORE,
    OSL,
}

@Suppress("unused")
internal enum class MoreRootDestination(
    val graph: NavGraph,
    val activeOption: ActiveOption,
) {
    Settings(NavGraphs.settings, ActiveOption.SETTINGS),
    BackupRestore(NavGraphs.backup_restore, ActiveOption.BACKUP_RESTORE),
    OpenSourceLicenses(NavGraphs.openSourceLicenses, ActiveOption.OSL),
}
