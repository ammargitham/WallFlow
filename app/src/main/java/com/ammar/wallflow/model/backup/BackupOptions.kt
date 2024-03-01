package com.ammar.wallflow.model.backup

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.core.os.bundleOf
import com.ammar.wallflow.extensions.getParcelableCompat

data class BackupOptions(
    val settings: Boolean = false,
    val favorites: Boolean = false,
    val lightDark: Boolean = false,
    val savedSearches: Boolean = false,
    val viewed: Boolean = false,
    val file: Uri? = null,
) {
    val atleastOneChosen = settings ||
        favorites ||
        lightDark ||
        savedSearches ||
        viewed
}

val MutableBackupOptionsSaver = Saver<MutableState<BackupOptions>, Bundle>(
    save = {
        val value = it.value
        bundleOf(
            "settings" to value.settings,
            "favorites" to value.favorites,
            "light_dark" to value.lightDark,
            "saved_searches" to value.savedSearches,
            "viewed" to value.viewed,
            "file" to value.file,
        )
    },
    restore = {
        mutableStateOf(
            BackupOptions(
                settings = it.getBoolean("settings", false),
                favorites = it.getBoolean("favorites", false),
                lightDark = it.getBoolean("light_dark", false),
                savedSearches = it.getBoolean("saved_searches", false),
                viewed = it.getBoolean("viewed", false),
                file = it.getParcelableCompat("file", Uri::class.java),
            ),
        )
    },
)
