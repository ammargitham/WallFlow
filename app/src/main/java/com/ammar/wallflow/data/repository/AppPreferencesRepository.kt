package com.ammar.wallflow.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.preferences.GridColType
import com.ammar.wallflow.data.preferences.GridType
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.LookAndFeelPreferences
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.data.preferences.PreferencesKeys
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperConstraints
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperFreq
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.toConstraintTypeMap
import com.ammar.wallflow.extensions.toConstraints
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.Sorting
import com.ammar.wallflow.model.TopRange
import com.ammar.wallflow.model.serializers.constraintTypeMapSerializer
import com.ammar.wallflow.utils.objectdetection.objectsDetector
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimePeriod
import kotlinx.serialization.json.Json

@Singleton
class AppPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    val appPreferencesFlow: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { mapAppPreferences(it) }
        .flowOn(ioDispatcher)

    suspend fun updateWallhavenApiKey(wallhavenApiKey: String) = withContext(ioDispatcher) {
        dataStore.edit { it[PreferencesKeys.WALLHAVEN_API_KEY] = wallhavenApiKey }
    }

    suspend fun updateHomeSearch(search: Search) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.HOME_SEARCH_QUERY] = search.query
            it[PreferencesKeys.HOME_FILTERS] = search.filters.toQueryString()
        }
    }

    suspend fun updateBlurSketchy(blurSketchy: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { it[PreferencesKeys.BLUR_SKETCHY] = blurSketchy }
    }

    suspend fun updateBlurNsfw(blurNsfw: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { it[PreferencesKeys.BLUR_NSFW] = blurNsfw }
    }

    suspend fun updateObjectDetectionPrefs(objectDetectionPreferences: ObjectDetectionPreferences) =
        withContext(ioDispatcher) {
            dataStore.edit {
                with(objectDetectionPreferences) {
                    it[PreferencesKeys.ENABLE_OBJECT_DETECTION] = enabled
                    it[PreferencesKeys.OBJECT_DETECTION_DELEGATE] = delegate.name
                    it[PreferencesKeys.OBJECT_DETECTION_MODEL_ID] = modelId
                }
            }
        }

    suspend fun updateAutoWallpaperPrefs(autoWallpaperPreferences: AutoWallpaperPreferences) =
        withContext(ioDispatcher) {
            dataStore.edit {
                with(autoWallpaperPreferences) {
                    it[PreferencesKeys.ENABLE_AUTO_WALLPAPER] = enabled
                    it[PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ID] = savedSearchId
                    it[PreferencesKeys.AUTO_WALLPAPER_USE_OBJECT_DETECTION] = useObjectDetection
                    it[PreferencesKeys.AUTO_WALLPAPER_FREQUENCY] = frequency.toString()
                    it[PreferencesKeys.AUTO_WALLPAPER_CONSTRAINTS] = Json.encodeToString(
                        constraintTypeMapSerializer,
                        constraints.toConstraintTypeMap(),
                    )
                    it[PreferencesKeys.AUTO_WALLPAPER_SHOW_NOTIFICATION] = showNotification
                }
            }
        }

    suspend fun updateLookAndFeelPreferences(lookAndFeelPreferences: LookAndFeelPreferences) =
        withContext(ioDispatcher) {
            dataStore.edit {
                with(lookAndFeelPreferences) {
                    it[PreferencesKeys.THEME] = theme.name
                    it[PreferencesKeys.LAYOUT_GRID_TYPE] = layoutPreferences.gridType.name
                    it[PreferencesKeys.LAYOUT_GRID_COL_TYPE] = layoutPreferences.gridColType.name
                    it[PreferencesKeys.LAYOUT_GRID_COL_COUNT] = layoutPreferences.gridColCount
                    it[PreferencesKeys.LAYOUT_GRID_COL_MIN_WIDTH_PCT] =
                        layoutPreferences.gridColMinWidthPct
                    it[PreferencesKeys.LAYOUT_ROUNDED_CORNERS] = layoutPreferences.roundedCorners
                }
            }
        }

    suspend fun updateAutoWallpaperWorkRequestId(id: UUID?) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.AUTO_WALLPAPER_WORK_REQUEST_ID] = id?.toString() ?: ""
        }
    }

    private fun mapAppPreferences(preferences: Preferences) = AppPreferences(
        wallhavenApiKey = preferences[PreferencesKeys.WALLHAVEN_API_KEY] ?: "",
        homeSearch = Search(
            query = preferences[PreferencesKeys.HOME_SEARCH_QUERY] ?: "",
            filters = preferences[PreferencesKeys.HOME_FILTERS]?.let {
                SearchQuery.fromQueryString(it)
            } ?: SearchQuery(
                sorting = Sorting.TOPLIST,
                topRange = TopRange.ONE_DAY,
            ),
        ),
        blurSketchy = preferences[PreferencesKeys.BLUR_SKETCHY] ?: false,
        blurNsfw = preferences[PreferencesKeys.BLUR_NSFW] ?: false,
        objectDetectionPreferences = with(preferences) {
            val delegate = try {
                val deletePref = get(PreferencesKeys.OBJECT_DETECTION_DELEGATE)
                if (deletePref != null) {
                    ObjectDetectionDelegate.valueOf(deletePref)
                } else {
                    ObjectDetectionDelegate.GPU
                }
            } catch (e: Exception) {
                ObjectDetectionDelegate.GPU
            }
            val enabled = objectsDetector.isEnabled
                    && get(PreferencesKeys.ENABLE_OBJECT_DETECTION) ?: false
            ObjectDetectionPreferences(
                enabled = enabled,
                delegate = delegate,
                modelId = get(PreferencesKeys.OBJECT_DETECTION_MODEL_ID) ?: 0,
            )
        },
        autoWallpaperPreferences = with(preferences) {
            val savedSearchId = get(PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ID) ?: 0
            AutoWallpaperPreferences(
                enabled = when {
                    savedSearchId <= 0 -> false
                    else -> get(PreferencesKeys.ENABLE_AUTO_WALLPAPER) ?: false
                },
                savedSearchId = savedSearchId,
                useObjectDetection = get(PreferencesKeys.AUTO_WALLPAPER_USE_OBJECT_DETECTION)
                    ?: true,
                frequency = parseFrequency(get(PreferencesKeys.AUTO_WALLPAPER_FREQUENCY)),
                constraints = parseConstraints(get(PreferencesKeys.AUTO_WALLPAPER_CONSTRAINTS)),
                workRequestId = parseWorkRequestId(
                    get(PreferencesKeys.AUTO_WALLPAPER_WORK_REQUEST_ID)
                ),
                showNotification = get(PreferencesKeys.AUTO_WALLPAPER_SHOW_NOTIFICATION) ?: false,
            )
        },
        lookAndFeelPreferences = LookAndFeelPreferences(
            theme = try {
                Theme.valueOf(preferences[PreferencesKeys.THEME] ?: "")
            } catch (e: Exception) {
                Theme.SYSTEM
            },
            layoutPreferences = LayoutPreferences(
                gridType = try {
                    GridType.valueOf(preferences[PreferencesKeys.LAYOUT_GRID_TYPE] ?: "")
                } catch (e: Exception) {
                    GridType.STAGGERED
                },
                gridColType = try {
                    GridColType.valueOf(preferences[PreferencesKeys.LAYOUT_GRID_COL_TYPE] ?: "")
                } catch (e: Exception) {
                    GridColType.ADAPTIVE
                },
                gridColCount = preferences[PreferencesKeys.LAYOUT_GRID_COL_COUNT] ?: 2,
                gridColMinWidthPct = preferences[PreferencesKeys.LAYOUT_GRID_COL_MIN_WIDTH_PCT]
                    ?: 30,
                roundedCorners = preferences[PreferencesKeys.LAYOUT_ROUNDED_CORNERS] ?: true,
            )
        )
    )

    private fun parseFrequency(freqStr: String?) = try {
        if (freqStr != null) {
            DateTimePeriod.parse(freqStr)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing period string: ", e)
        null
    } ?: defaultAutoWallpaperFreq

    private fun parseConstraints(constraintsStr: String?) = try {
        if (constraintsStr != null) {
            Json.decodeFromString(
                constraintTypeMapSerializer,
                constraintsStr,
            ).toConstraints()
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing constraints string: ", e)
        null
    } ?: defaultAutoWallpaperConstraints

    private fun parseWorkRequestId(uuidStr: String?) = try {
        UUID.fromString(uuidStr)
    } catch (e: Exception) {
        null
    }

    suspend fun getWallHavenApiKey() = mapAppPreferences(dataStore.data.first()).wallhavenApiKey

    suspend fun getAutoWallHavenWorkRequestId() = mapAppPreferences(dataStore.data.first())
        .autoWallpaperPreferences
        .workRequestId
}
