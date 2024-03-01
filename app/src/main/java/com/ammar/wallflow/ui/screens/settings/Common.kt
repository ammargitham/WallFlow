package com.ammar.wallflow.ui.screens.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.services.ChangeWallpaperTileService
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.FailureReason

suspend fun updateAutoWallpaperPrefs(
    context: Context,
    appPreferencesRepository: AppPreferencesRepository,
    prevAppPreferences: AppPreferences,
    newAutoWallpaperPreferences: AutoWallpaperPreferences,
) {
    var prefs = newAutoWallpaperPreferences
    if (prefs.enabled && !prefs.anySourceEnabled) {
        prefs = prefs.copy(
            enabled = false,
            workRequestId = null,
        )
    }
    if (!prefs.enabled) {
        // reset work request id
        prefs = prefs.copy(
            workRequestId = null,
        )
    }
    appPreferencesRepository.updateAutoWallpaperPrefs(prefs)
    val tileAdded = prevAppPreferences.changeWallpaperTileAdded
    if (tileAdded) {
        ChangeWallpaperTileService.requestListeningState(context)
    }
    if (prefs.enabled) {
        // only reschedule if enabled or frequency or constraints change
        val currentPrefs = prevAppPreferences.autoWallpaperPreferences
        if (
            currentPrefs.enabled &&
            currentPrefs.frequency == prefs.frequency &&
            currentPrefs.constraints == prefs.constraints
        ) {
            return
        }
        // schedule worker with updated preferences
        AutoWallpaperWorker.schedule(
            context = context,
            constraints = prefs.constraints,
            interval = prefs.frequency,
            appPreferencesRepository = appPreferencesRepository,
        )
    } else {
        // stop the worker
        AutoWallpaperWorker.stop(
            context = context,
            appPreferencesRepository = appPreferencesRepository,
        )
    }
}

@Composable
internal fun getFailureReasonString(reason: FailureReason): String {
    val reasonStr = when (reason) {
        FailureReason.APP_PREFS_NULL -> stringResource(R.string.app_prefs_null)
        FailureReason.DISABLED -> stringResource(R.string.auto_wallpaper_disabled)
        FailureReason.NO_SOURCES_ENABLED -> stringResource(R.string.no_sources_set)
        FailureReason.SAVED_SEARCH_NOT_SET -> stringResource(R.string.no_saved_searches)
        FailureReason.NO_WALLPAPER_FOUND -> stringResource(R.string.no_wallpaper_found)
        FailureReason.CANCELLED -> stringResource(R.string.auto_wallpaper_cancelled)
    }
    return stringResource(R.string.wallpaper_not_changed_with_reason, reasonStr)
}
