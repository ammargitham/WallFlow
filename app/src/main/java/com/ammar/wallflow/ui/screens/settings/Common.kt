package com.ammar.wallflow.ui.screens.settings

import android.content.Context
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.services.ChangeWallpaperTileService
import com.ammar.wallflow.workers.AutoWallpaperWorker

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
