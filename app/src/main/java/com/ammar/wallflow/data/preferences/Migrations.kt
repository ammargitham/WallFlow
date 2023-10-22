package com.ammar.wallflow.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import kotlinx.serialization.encodeToString

/**
 * Use static strings for preference keys except PreferencesKeys.VERSION
 */

fun migrateAppPrefs1To2() = object : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[PreferencesKeys.VERSION]
        return version == null || version == 1
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutablePrefs = currentData.toMutablePreferences()
        // we convert => home_search_query and home_filters to home_wallhaven_search

        // get existing home_search_query and home_filters
        val homeSearchQueryPrefKey = stringPreferencesKey("home_search_query")
        val homeFiltersPrefKey = stringPreferencesKey("home_filters")
        val homeSearchQuery = mutablePrefs[homeSearchQueryPrefKey]
        val homeFilters = mutablePrefs[homeFiltersPrefKey]

        // create WallhavenSearch instance
        val wallhavenSearch = WallhavenSearch(
            query = homeSearchQuery ?: "",
            filters = homeFilters?.let {
                @Suppress("DEPRECATION")
                WallhavenFilters.fromQueryString(it)
            } ?: WallhavenFilters(
                sorting = WallhavenSorting.TOPLIST,
                topRange = WallhavenTopRange.ONE_DAY,
            ),
        )

        // remove prev keys
        if (mutablePrefs.contains(homeSearchQueryPrefKey)) {
            mutablePrefs.remove(homeSearchQueryPrefKey)
        }
        if (mutablePrefs.contains(homeFiltersPrefKey)) {
            mutablePrefs.remove(homeFiltersPrefKey)
        }

        // insert new "home_wallhaven_search"
        mutablePrefs[stringPreferencesKey("home_wallhaven_search")] = json.encodeToString(
            wallhavenSearch,
        )

        // convert auto_wallpaper_saved_search_id to set
        val savedSearchId = mutablePrefs[
            longPreferencesKey("auto_wallpaper_saved_search_id"),
        ]
        mutablePrefs[
            stringSetPreferencesKey("auto_wallpaper_saved_search_id"),
        ] = setOf(savedSearchId.toString())

        mutablePrefs[PreferencesKeys.VERSION] = 2
        return mutablePrefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}
