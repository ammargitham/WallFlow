package com.ammar.wallflow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val APP_PREFERENCES_NAME = "app_preferences"

val Context.dataStore by preferencesDataStore(name = APP_PREFERENCES_NAME)

object PreferencesKeys {
    val WALLHAVEN_API_KEY = stringPreferencesKey("wallhaven_api_key")
    val HOME_SEARCH_QUERY = stringPreferencesKey("home_search_query")
    val HOME_FILTERS = stringPreferencesKey("home_filters")
    val BLUR_SKETCHY = booleanPreferencesKey("blur_sketchy")
    val BLUR_NSFW = booleanPreferencesKey("blur_nsfw")
    val ENABLE_OBJECT_DETECTION = booleanPreferencesKey("enable_object_detection")
    val OBJECT_DETECTION_DELEGATE = stringPreferencesKey("object_detection_delegate")
    val OBJECT_DETECTION_MODEL_ID = longPreferencesKey("object_detection_model_id")
    val ENABLE_AUTO_WALLPAPER = booleanPreferencesKey("enable_auto_wallpaper")
    val AUTO_WALLPAPER_SAVED_SEARCH_ENABLED = booleanPreferencesKey(
        "auto_wallpaper_saved_search_enabled",
    )
    val AUTO_WALLPAPER_FAVORITES_ENABLED = booleanPreferencesKey(
        "auto_wallpaper_favorites_enabled",
    )
    val AUTO_WALLPAPER_LOCAL_ENABLED = booleanPreferencesKey(
        "auto_wallpaper_local_enabled",
    )
    val AUTO_WALLPAPER_SAVED_SEARCH_ID = longPreferencesKey("auto_wallpaper_saved_search_id")
    val AUTO_WALLPAPER_USE_OBJECT_DETECTION = booleanPreferencesKey(
        "auto_wallpaper_use_object_detection",
    )
    val AUTO_WALLPAPER_FREQUENCY = stringPreferencesKey("auto_wallpaper_frequency")
    val AUTO_WALLPAPER_CONSTRAINTS = stringPreferencesKey("auto_wallpaper_constraints")
    val AUTO_WALLPAPER_SHOW_NOTIFICATION = booleanPreferencesKey("auto_wallpaper_show_notification")
    val AUTO_WALLPAPER_WORK_REQUEST_ID = stringPreferencesKey("auto_wallpaper_work_request_id")
    val AUTO_WALLPAPER_TARGETS = stringSetPreferencesKey("auto_wallpaper_targets")
    val AUTO_WALLPAPER_MARK_FAVORITE = booleanPreferencesKey("auto_wallpaper_mark_favorite")
    val AUTO_WALLPAPER_DOWNLOAD = booleanPreferencesKey("auto_wallpaper_download")
    val THEME = stringPreferencesKey("theme")
    val LAYOUT_GRID_TYPE = stringPreferencesKey("layout_grid_type")
    val LAYOUT_GRID_COL_TYPE = stringPreferencesKey("layout_grid_col_type")
    val LAYOUT_GRID_COL_COUNT = intPreferencesKey("layout_grid_col_count")
    val LAYOUT_GRID_COL_MIN_WIDTH_PCT = intPreferencesKey("layout_grid_col_min_width_pct")
    val LAYOUT_ROUNDED_CORNERS = booleanPreferencesKey("layout_rounded_corners")
    val SHOW_LOCAL_TAB = booleanPreferencesKey("show_local_tab")
    val CHANGE_WALLPAPER_TILE_ADDED = booleanPreferencesKey("change_wallpaper_tile_added")
    val LOCAL_WALLPAPERS_SORT = stringPreferencesKey("local_wallpapers_sort")
}
