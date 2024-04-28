package com.ammar.wallflow.ui.screens.settings

import android.content.Context
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.services.ChangeWallpaperTileService
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.FailureReason
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.DateTimePeriod

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
        val frequenciesEqual = areFrequenciesEqual(
            currentUseSameFreq = currentPrefs.useSameFreq,
            currentFreq = currentPrefs.frequency,
            currentLsFreq = currentPrefs.lsFrequency,
            newUseSameFreq = prefs.useSameFreq,
            newFreq = prefs.frequency,
            newLsFreq = prefs.lsFrequency,
        )
        if (
            currentPrefs.enabled &&
            frequenciesEqual &&
            currentPrefs.constraints == prefs.constraints
        ) {
            return
        }
        // schedule worker with updated preferences
        AutoWallpaperWorker.schedule(
            context = context,
            autoWallpaperPreferences = prefs,
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

private fun areFrequenciesEqual(
    currentUseSameFreq: Boolean,
    currentFreq: DateTimePeriod,
    currentLsFreq: DateTimePeriod,
    newUseSameFreq: Boolean,
    newFreq: DateTimePeriod,
    newLsFreq: DateTimePeriod,
): Boolean {
    if (currentUseSameFreq != newUseSameFreq) {
        return false
    }
    if (currentUseSameFreq) {
        return currentFreq == newFreq
    }
    return currentFreq == newFreq &&
        currentLsFreq == newLsFreq
}

internal fun getFailureReasonString(
    context: Context,
    reason: FailureReason,
): String {
    val reasonStr = when (reason) {
        FailureReason.APP_PREFS_NULL -> context.getString(R.string.app_prefs_null)
        FailureReason.DISABLED -> context.getString(R.string.auto_wallpaper_disabled)
        FailureReason.NO_SOURCES_ENABLED -> context.getString(R.string.no_sources_set)
        FailureReason.SAVED_SEARCH_NOT_SET -> context.getString(R.string.no_saved_searches)
        FailureReason.NO_WALLPAPER_FOUND -> context.getString(R.string.no_wallpaper_found)
        FailureReason.CANCELLED -> context.getString(R.string.auto_wallpaper_cancelled)
        FailureReason.CURRENT_TARGETS_DISABLED -> context.getString(
            R.string.current_target_disabled,
        )
    }
    return context.getString(R.string.wallpaper_not_changed_with_reason, reasonStr)
}

internal fun getSourcesSummary(
    context: Context,
    useSameSources: Boolean,
    lightDarkEnabled: Boolean,
    savedSearches: ImmutableList<SavedSearch>,
    savedSearchEnabled: Boolean,
    favoritesEnabled: Boolean,
    localEnabled: Boolean,
    lsLightDarkEnabled: Boolean,
    lsSavedSearchEnabled: Boolean,
    lsFavoritesEnabled: Boolean,
    lsLocalEnabled: Boolean,
) = mutableListOf<String>().apply {
    if (useSameSources) {
        if (lightDarkEnabled) {
            add(context.getString(R.string.light_dark))
            return@apply
        }
        if (savedSearchEnabled && savedSearches.size > 0) {
            val searchNames = if (savedSearches.size > 2) {
                context.resources.getQuantityString(
                    R.plurals.n_searches,
                    savedSearches.size,
                    savedSearches.size,
                )
            } else {
                savedSearches.joinToString(", ") { it.name }
            }
            add("${context.getString(R.string.saved_search)} ($searchNames)")
        }
        if (favoritesEnabled) {
            add(context.getString(R.string.favorites))
        }
        if (localEnabled) {
            add(context.getString(R.string.local))
        }
    } else {
        val homeCount = getSourcesCount(
            lightDarkEnabled,
            savedSearchEnabled,
            favoritesEnabled,
            localEnabled,
        )
        if (homeCount > 0) {
            add(
                context.resources.getQuantityString(
                    R.plurals.home_screen_sources,
                    homeCount,
                    homeCount,
                ),
            )
        }
        val lsCount = getSourcesCount(
            lsLightDarkEnabled,
            lsSavedSearchEnabled,
            lsFavoritesEnabled,
            lsLocalEnabled,
        )
        if (lsCount > 0) {
            add(
                context.resources.getQuantityString(
                    R.plurals.lock_screen_sources,
                    lsCount,
                    lsCount,
                ),
            )
        }
    }
}.joinToString(", ")

internal fun getSourcesCount(
    lightDarkEnabled: Boolean,
    savedSearchEnabled: Boolean,
    favoritesEnabled: Boolean,
    localEnabled: Boolean,
): Int {
    if (lightDarkEnabled) {
        return 1
    }
    var count = 0
    if (savedSearchEnabled) {
        count += 1
    }
    if (favoritesEnabled) {
        count += 1
    }
    if (localEnabled) {
        count += 1
    }
    return count
}
