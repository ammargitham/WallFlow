package com.ammar.wallflow.ui.screens.more

internal enum class ActiveOption {
    SETTINGS,
    BACKUP_RESTORE,
    OSL,
}

internal enum class MoreRootDestination(
    val activeOption: ActiveOption,
) {
    Settings(ActiveOption.SETTINGS),
    BackupRestore(ActiveOption.BACKUP_RESTORE),
    OpenSourceLicenses(ActiveOption.OSL),
}
