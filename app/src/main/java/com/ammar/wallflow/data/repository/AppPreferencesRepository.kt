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
import com.ammar.wallflow.data.preferences.LocalWallpapersPreferences
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
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import com.ammar.wallflow.model.serializers.constraintTypeMapSerializer
import com.ammar.wallflow.ui.screens.local.LocalSort
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

    suspend fun updateHomeWallhavenSearch(search: WallhavenSearch) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.HOME_WALLHAVEN_SEARCH_QUERY] = search.query
            it[PreferencesKeys.HOME_WALLHAVEN_FILTERS] = search.filters.toQueryString()
        }
    }

    suspend fun updateHomeRedditSearch(search: RedditSearch) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.HOME_REDDIT_SEARCH] = search.toQueryString()
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
                    it[PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ENABLED] = savedSearchEnabled
                    it[PreferencesKeys.AUTO_WALLPAPER_FAVORITES_ENABLED] = favoritesEnabled
                    it[PreferencesKeys.AUTO_WALLPAPER_LOCAL_ENABLED] = localEnabled
                    it[PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ID] = savedSearchId
                    it[PreferencesKeys.AUTO_WALLPAPER_USE_OBJECT_DETECTION] = useObjectDetection
                    it[PreferencesKeys.AUTO_WALLPAPER_FREQUENCY] = frequency.toString()
                    it[PreferencesKeys.AUTO_WALLPAPER_CONSTRAINTS] = Json.encodeToString(
                        constraintTypeMapSerializer,
                        constraints.toConstraintTypeMap(),
                    )
                    it[PreferencesKeys.AUTO_WALLPAPER_SHOW_NOTIFICATION] = showNotification
                    it[PreferencesKeys.AUTO_WALLPAPER_TARGETS] = targets.map {
                        it.name
                    }.toSet()
                    it[PreferencesKeys.AUTO_WALLPAPER_MARK_FAVORITE] = markFavorite
                    it[PreferencesKeys.AUTO_WALLPAPER_DOWNLOAD] = download
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
                    it[PreferencesKeys.SHOW_LOCAL_TAB] = showLocalTab
                }
            }
        }

    suspend fun updateAutoWallpaperWorkRequestId(id: UUID?) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.AUTO_WALLPAPER_WORK_REQUEST_ID] = id?.toString() ?: ""
        }
    }

    suspend fun updateTileAdded(added: Boolean) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.CHANGE_WALLPAPER_TILE_ADDED] = added
        }
    }

    suspend fun updateLocalWallpapersSort(sort: LocalSort) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.LOCAL_WALLPAPERS_SORT] = sort.name
        }
    }

    private fun mapAppPreferences(preferences: Preferences) = AppPreferences(
        wallhavenApiKey = preferences[PreferencesKeys.WALLHAVEN_API_KEY] ?: "",
        homeWallhavenSearch = WallhavenSearch(
            query = preferences[PreferencesKeys.HOME_WALLHAVEN_SEARCH_QUERY] ?: "",
            filters = preferences[PreferencesKeys.HOME_WALLHAVEN_FILTERS]?.let {
                WallhavenFilters.fromQueryString(it)
            } ?: WallhavenFilters(
                sorting = WallhavenSorting.TOPLIST,
                topRange = WallhavenTopRange.ONE_DAY,
            ),
        ),
        homeRedditSearch = RedditSearch.fromQueryString(
            preferences[PreferencesKeys.HOME_REDDIT_SEARCH],
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
            val enabled = objectsDetector.isEnabled &&
                get(PreferencesKeys.ENABLE_OBJECT_DETECTION) ?: false
            ObjectDetectionPreferences(
                enabled = enabled,
                delegate = delegate,
                modelId = get(PreferencesKeys.OBJECT_DETECTION_MODEL_ID) ?: 0,
            )
        },
        autoWallpaperPreferences = with(preferences) {
            val savedSearchId = get(PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ID) ?: 0
            val savedSearchEnabled =
                (get(PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ENABLED) ?: false) &&
                    savedSearchId > 0
            val favoritesEnabled = get(PreferencesKeys.AUTO_WALLPAPER_FAVORITES_ENABLED) ?: false
            val localEnabled = get(PreferencesKeys.AUTO_WALLPAPER_LOCAL_ENABLED) ?: false
            AutoWallpaperPreferences(
                enabled = when {
                    !savedSearchEnabled && !favoritesEnabled && !localEnabled -> false
                    else -> get(PreferencesKeys.ENABLE_AUTO_WALLPAPER) ?: false
                },
                savedSearchEnabled = savedSearchEnabled,
                favoritesEnabled = favoritesEnabled,
                localEnabled = localEnabled,
                savedSearchId = savedSearchId,
                useObjectDetection = get(PreferencesKeys.AUTO_WALLPAPER_USE_OBJECT_DETECTION)
                    ?: true,
                frequency = parseFrequency(get(PreferencesKeys.AUTO_WALLPAPER_FREQUENCY)),
                constraints = parseConstraints(get(PreferencesKeys.AUTO_WALLPAPER_CONSTRAINTS)),
                workRequestId = parseWorkRequestId(
                    get(PreferencesKeys.AUTO_WALLPAPER_WORK_REQUEST_ID),
                ),
                showNotification = get(PreferencesKeys.AUTO_WALLPAPER_SHOW_NOTIFICATION) ?: false,
                targets = get(PreferencesKeys.AUTO_WALLPAPER_TARGETS)?.map {
                    try {
                        WallpaperTarget.valueOf(it)
                    } catch (e: Exception) {
                        WallpaperTarget.HOME
                    }
                }?.toSortedSet() ?: setOf(
                    WallpaperTarget.HOME,
                    WallpaperTarget.LOCKSCREEN,
                ),
                markFavorite = get(PreferencesKeys.AUTO_WALLPAPER_MARK_FAVORITE) ?: false,
                download = get(PreferencesKeys.AUTO_WALLPAPER_DOWNLOAD) ?: false,
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
                    GridColType.valueOf(
                        preferences[PreferencesKeys.LAYOUT_GRID_COL_TYPE] ?: "",
                    )
                } catch (e: Exception) {
                    GridColType.ADAPTIVE
                },
                gridColCount = preferences[PreferencesKeys.LAYOUT_GRID_COL_COUNT] ?: 2,
                gridColMinWidthPct = preferences[PreferencesKeys.LAYOUT_GRID_COL_MIN_WIDTH_PCT]
                    ?: 40,
                roundedCorners = preferences[PreferencesKeys.LAYOUT_ROUNDED_CORNERS] ?: true,
            ),
            showLocalTab = preferences[PreferencesKeys.SHOW_LOCAL_TAB] ?: true,
        ),
        changeWallpaperTileAdded = preferences[PreferencesKeys.CHANGE_WALLPAPER_TILE_ADDED]
            ?: false,
        localWallpapersPreferences = LocalWallpapersPreferences(
            sort = try {
                LocalSort.valueOf(preferences[PreferencesKeys.LOCAL_WALLPAPERS_SORT] ?: "")
            } catch (e: Exception) {
                LocalSort.NO_SORT
            },
        ),
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

    suspend fun setPreferences(appPreferences: AppPreferences) {
        updateWallhavenApiKey(appPreferences.wallhavenApiKey)
        updateHomeWallhavenSearch(appPreferences.homeWallhavenSearch)
        updateBlurSketchy(appPreferences.blurSketchy)
        updateBlurNsfw(appPreferences.blurNsfw)
        updateObjectDetectionPrefs(appPreferences.objectDetectionPreferences)
        updateAutoWallpaperPrefs(appPreferences.autoWallpaperPreferences)
        updateLookAndFeelPreferences(appPreferences.lookAndFeelPreferences)
        updateLocalWallpapersSort(appPreferences.localWallpapersPreferences.sort)
    }
}
