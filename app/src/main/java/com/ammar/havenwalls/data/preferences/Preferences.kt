package com.ammar.havenwalls.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
}
