package com.ammar.wallflow.model.backup

import android.net.Uri

data class RestoreSummary(
    val file: Uri?,
    val backup: Backup? = null,
    val settings: Boolean = false,
    val favorites: Int? = null,
    val lightDark: Int? = null,
    val viewed: Int? = null,
    val savedSearches: Int? = null,
) {
    fun getInitialRestoreOptions() = BackupOptions(
        file = file,
        settings = settings,
        favorites = favorites != null && favorites > 0,
        lightDark = lightDark != null && lightDark > 0,
        viewed = viewed != null && viewed > 0,
        savedSearches = savedSearches != null && savedSearches > 0,
    )
}
