package com.ammar.wallflow.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.data.preferences.ViewedWallpapersPreferences
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperConstraints
import com.ammar.wallflow.data.preferences.defaultAutoWallpaperFreq
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.accessibleFolders
import com.ammar.wallflow.extensions.toConstraintTypeMap
import com.ammar.wallflow.extensions.toConstraints
import com.ammar.wallflow.extensions.toUriOrNull
import com.ammar.wallflow.json
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSorting
import com.ammar.wallflow.model.search.WallhavenTopRange
import com.ammar.wallflow.model.serializers.constraintTypeMapSerializer
import com.ammar.wallflow.ui.screens.local.LocalSort
import com.ammar.wallflow.utils.ExifWriteType
import com.ammar.wallflow.utils.objectdetection.objectsDetector
import com.ammar.wallflow.utils.valueOf
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.serialization.encodeToString

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val favoritesRepository: FavoritesRepository,
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
        .map(::mapAppPreferences)
        .flowOn(ioDispatcher)

    suspend fun updateWallhavenApiKey(wallhavenApiKey: String) = withContext(ioDispatcher) {
        dataStore.edit { it.updateWallhavenApikey(wallhavenApiKey) }
    }

    suspend fun updateHomeWallhavenSearch(search: WallhavenSearch) = withContext(ioDispatcher) {
        dataStore.edit { it.updateHomeWallhavenSearch(search) }
    }

    suspend fun updateHomeRedditSearch(search: RedditSearch) = withContext(ioDispatcher) {
        dataStore.edit { it.updateHomeRedditSearch(search) }
    }

    suspend fun updateBlurSketchy(blurSketchy: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { it.updateBlurSketchy(blurSketchy) }
    }

    suspend fun updateBlurNsfw(blurNsfw: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { it.updateBlurNsfw(blurNsfw) }
    }

    suspend fun updateWriteTagsToExif(writeTagsToExif: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { it.updateWriteTagsToExif(writeTagsToExif) }
    }

    suspend fun updateTagsWriteType(tagsExifWriteType: ExifWriteType) = withContext(ioDispatcher) {
        dataStore.edit { it.updateTagsWriteType(tagsExifWriteType) }
    }

    suspend fun updateObjectDetectionPrefs(
        objectDetectionPreferences: ObjectDetectionPreferences,
    ) = withContext(ioDispatcher) {
        dataStore.edit {
            it.updateObjectDetectionPrefs(objectDetectionPreferences)
        }
    }

    suspend fun updateAutoWallpaperPrefs(
        autoWallpaperPreferences: AutoWallpaperPreferences,
    ) = withContext(ioDispatcher) {
        dataStore.edit {
            it.updateAutoWallpaperPrefs(autoWallpaperPreferences)
        }
    }

    suspend fun updateLookAndFeelPreferences(
        lookAndFeelPreferences: LookAndFeelPreferences,
    ) = withContext(ioDispatcher) {
        dataStore.edit {
            it.updateLookAndFeelPreferences(lookAndFeelPreferences)
        }
    }

    private fun MutablePreferences.updateWallhavenApikey(wallhavenApiKey: String) {
        set(PreferencesKeys.WALLHAVEN_API_KEY, wallhavenApiKey)
    }

    private fun MutablePreferences.updateHomeWallhavenSearch(search: WallhavenSearch) {
        set(PreferencesKeys.HOME_WALLHAVEN_SEARCH, search.toJson())
    }

    private fun MutablePreferences.updateHomeRedditSearch(search: RedditSearch) {
        set(PreferencesKeys.HOME_REDDIT_SEARCH, json.encodeToString(search))
        updateHomeSources(
            mapOf(
                OnlineSource.WALLHAVEN to true,
                OnlineSource.REDDIT to true,
            ),
        )
    }

    private fun MutablePreferences.updateBlurSketchy(blurSketchy: Boolean) {
        set(PreferencesKeys.BLUR_SKETCHY, blurSketchy)
    }

    private fun MutablePreferences.updateBlurNsfw(blurNsfw: Boolean) {
        set(PreferencesKeys.BLUR_NSFW, blurNsfw)
    }

    private fun MutablePreferences.updateWriteTagsToExif(writeTagsToExif: Boolean) {
        set(PreferencesKeys.WRITE_TAGS_TO_EXIF, writeTagsToExif)
    }

    private fun MutablePreferences.updateTagsWriteType(exifWriteType: ExifWriteType) {
        set(PreferencesKeys.TAGS_WRITE_TYPE, exifWriteType.name)
    }

    private fun MutablePreferences.updateObjectDetectionPrefs(
        objectDetectionPreferences: ObjectDetectionPreferences,
    ) = with(objectDetectionPreferences) {
        set(PreferencesKeys.ENABLE_OBJECT_DETECTION, enabled)
        set(PreferencesKeys.OBJECT_DETECTION_DELEGATE, delegate.name)
        set(PreferencesKeys.OBJECT_DETECTION_MODEL_ID, modelId)
    }

    private fun MutablePreferences.updateAutoWallpaperPrefs(
        autoWallpaperPreferences: AutoWallpaperPreferences,
    ) = with(autoWallpaperPreferences) {
        set(PreferencesKeys.ENABLE_AUTO_WALLPAPER, enabled)
        set(PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ENABLED, savedSearchEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_LS_SAVED_SEARCH_ENABLED, lsSavedSearchEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_FAVORITES_ENABLED, favoritesEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_LS_FAVORITES_ENABLED, lsFavoritesEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_LOCAL_ENABLED, localEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_LS_LOCAL_ENABLED, lsLocalEnabled)
        set(
            PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ID,
            savedSearchIds.mapTo(HashSet()) { it.toString() },
        )
        set(
            PreferencesKeys.AUTO_WALLPAPER_LS_SAVED_SEARCH_ID,
            lsSavedSearchIds.mapTo(HashSet()) { it.toString() },
        )
        set(
            PreferencesKeys.AUTO_WALLPAPER_LOCAL_DIRS,
            localDirs.mapTo(HashSet()) { it.toString() },
        )
        set(
            PreferencesKeys.AUTO_WALLPAPER_LS_LOCAL_DIRS,
            lsLocalDirs.mapTo(HashSet()) { it.toString() },
        )
        set(PreferencesKeys.AUTO_WALLPAPER_USE_OBJECT_DETECTION, useObjectDetection)
        set(PreferencesKeys.AUTO_WALLPAPER_USE_SAME_FREQUENCY, useSameFreq)
        set(PreferencesKeys.AUTO_WALLPAPER_FREQUENCY, frequency.toString())
        set(PreferencesKeys.AUTO_WALLPAPER_LS_FREQUENCY, lsFrequency.toString())
        set(
            PreferencesKeys.AUTO_WALLPAPER_CONSTRAINTS,
            json.encodeToString(
                constraintTypeMapSerializer,
                constraints.toConstraintTypeMap(),
            ),
        )
        set(PreferencesKeys.AUTO_WALLPAPER_SHOW_NOTIFICATION, showNotification)
        set(
            PreferencesKeys.AUTO_WALLPAPER_TARGETS,
            targets.map { it.name }.toSet(),
        )
        set(PreferencesKeys.AUTO_WALLPAPER_MARK_FAVORITE, markFavorite)
        set(PreferencesKeys.AUTO_WALLPAPER_DOWNLOAD, download)
        set(PreferencesKeys.AUTO_WALLPAPER_SET_DIFFERENT_WALLPAPERS, setDifferentWallpapers)
        set(PreferencesKeys.AUTO_WALLPAPER_CROP, crop)
        set(PreferencesKeys.AUTO_WALLPAPER_LIGHT_DARK_ENABLED, lightDarkEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_LS_LIGHT_DARK_ENABLED, lsLightDarkEnabled)
        set(PreferencesKeys.AUTO_WALLPAPER_USE_DARK_WITH_EXTRA_DIM, useDarkWithExtraDim)
        set(PreferencesKeys.AUTO_WALLPAPER_LS_USE_DARK_WITH_EXTRA_DIM, lsUseDarkWithExtraDim)
        if (prevHomeSource != null) {
            set(PreferencesKeys.AUTO_WALLPAPER_PREV_HOME_SOURCE, prevHomeSource.name)
        }
        if (prevLockScreenSource != null) {
            set(PreferencesKeys.AUTO_WALLPAPER_PREV_LS_SOURCE, prevLockScreenSource.name)
        }
    }

    private fun MutablePreferences.updateLookAndFeelPreferences(
        lookAndFeelPreferences: LookAndFeelPreferences,
    ) = with(lookAndFeelPreferences) {
        set(PreferencesKeys.THEME, theme.name)
        set(PreferencesKeys.LAYOUT_GRID_TYPE, layoutPreferences.gridType.name)
        set(PreferencesKeys.LAYOUT_GRID_COL_TYPE, layoutPreferences.gridColType.name)
        set(PreferencesKeys.LAYOUT_GRID_COL_COUNT, layoutPreferences.gridColCount)
        set(
            PreferencesKeys.LAYOUT_GRID_COL_MIN_WIDTH_PCT,
            layoutPreferences.gridColMinWidthPct,
        )
        set(PreferencesKeys.LAYOUT_ROUNDED_CORNERS, layoutPreferences.roundedCorners)
        set(PreferencesKeys.SHOW_LOCAL_TAB, showLocalTab)
    }

    suspend fun updateAutoWallpaperWorkRequestId(id: UUID?) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.AUTO_WALLPAPER_WORK_REQUEST_ID] = id?.toString() ?: ""
        }
    }

    suspend fun updateAutoWallpaperLsWorkRequestId(id: UUID?) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.AUTO_WALLPAPER_LS_WORK_REQUEST_ID] = id?.toString() ?: ""
        }
    }

    suspend fun updateAutoWallpaperBackoffUpdated(updated: Boolean) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.AUTO_WALLPAPER_BACKOFF_UPDATED] = updated
        }
    }

    suspend fun updateTileAdded(added: Boolean) = withContext(ioDispatcher) {
        dataStore.edit {
            it[PreferencesKeys.CHANGE_WALLPAPER_TILE_ADDED] = added
        }
    }

    suspend fun updateLocalWallpapersSort(sort: LocalSort) = withContext(ioDispatcher) {
        dataStore.edit {
            it.updateLocalWallpapersSort(sort)
        }
    }

    private fun MutablePreferences.updateLocalWallpapersSort(sort: LocalSort) {
        set(PreferencesKeys.LOCAL_WALLPAPERS_SORT, sort.name)
    }

    suspend fun updateLocalDirs(dirs: Set<Uri>) = withContext(ioDispatcher) {
        dataStore.edit {
            it.updateLocalDirs(dirs)
        }
    }

    private fun MutablePreferences.updateLocalDirs(dirs: Set<Uri>) {
        set(PreferencesKeys.LOCAL_DIRS, dirs.mapTo(HashSet()) { it.toString() })
    }

    suspend fun updateHomeSources(
        sources: Map<OnlineSource, Boolean>,
    ) = withContext(ioDispatcher) {
        dataStore.edit { it.updateHomeSources(sources) }
    }

    private fun MutablePreferences.updateHomeSources(sources: Map<OnlineSource, Boolean>) {
        set(PreferencesKeys.HOME_SOURCES, json.encodeToString(sources))
    }

    suspend fun updateMainSearch(search: Search) = withContext(ioDispatcher) {
        dataStore.edit { it.updateMainSearch(search) }
    }

    private fun MutablePreferences.updateMainSearch(search: Search) {
        if (search is WallhavenSearch) {
            set(PreferencesKeys.MAIN_WALLHAVEN_SEARCH, json.encodeToString(search))
        } else if (search is RedditSearch) {
            set(PreferencesKeys.MAIN_REDDIT_SEARCH, json.encodeToString(search))
        }
    }

    suspend fun updateViewedWallpapersPreferences(
        viewedWallpapersPreferences: ViewedWallpapersPreferences,
    ) = withContext(ioDispatcher) {
        dataStore.edit { it.updateViewedWallpapersPreferences(viewedWallpapersPreferences) }
    }

    private fun MutablePreferences.updateViewedWallpapersPreferences(
        viewedWallpapersPreferences: ViewedWallpapersPreferences,
    ) {
        set(PreferencesKeys.VIEWED_WALLPAPERS_ENABLED, viewedWallpapersPreferences.enabled)
        set(PreferencesKeys.VIEWED_WALLPAPERS_LOOK, viewedWallpapersPreferences.look.name)
    }

    suspend fun updateDownloadLocation(uri: Uri?) = withContext(ioDispatcher) {
        dataStore.edit {
            if (uri == null) {
                it.remove(PreferencesKeys.DOWNLOAD_LOCATION)
            } else {
                it[PreferencesKeys.DOWNLOAD_LOCATION] = uri.toString()
            }
        }
    }

    suspend fun updateAcraEnabled(enable: Boolean) = withContext(ioDispatcher) {
        dataStore.edit {
            it.updateAcraEnabled(enable)
        }
    }

    private fun MutablePreferences.updateAcraEnabled(enable: Boolean) {
        set(PreferencesKeys.ENABLE_ACRA, enable)
    }

    private suspend fun mapAppPreferences(preferences: Preferences): AppPreferences {
        val homeRedditSearch = getHomeRedditSearch(preferences)
        return AppPreferences(
            version = preferences[PreferencesKeys.VERSION] ?: AppPreferences.CURRENT_VERSION,
            wallhavenApiKey = getWallhavenApiKey(preferences),
            homeWallhavenSearch = getHomeWallhavenSearch(preferences),
            homeRedditSearch = homeRedditSearch,
            homeSources = getHomeSources(preferences, homeRedditSearch),
            blurSketchy = preferences[PreferencesKeys.BLUR_SKETCHY] ?: false,
            blurNsfw = preferences[PreferencesKeys.BLUR_NSFW] ?: false,
            writeTagsToExif = preferences[PreferencesKeys.WRITE_TAGS_TO_EXIF] ?: false,
            tagsExifWriteType = getTagsWriteType(preferences),
            downloadLocation = preferences[PreferencesKeys.DOWNLOAD_LOCATION]?.toUriOrNull(),
            objectDetectionPreferences = getObjectDetectionPreferences(preferences),
            autoWallpaperPreferences = getAutoWallpaperPreferences(preferences),
            lookAndFeelPreferences = getLookAndFeelPreferences(preferences),
            changeWallpaperTileAdded = preferences[PreferencesKeys.CHANGE_WALLPAPER_TILE_ADDED]
                ?: false,
            localWallpapersPreferences = getLocalWallpapersPreferences(preferences),
            mainWallhavenSearch = getMainWallhavenSearch(preferences),
            mainRedditSearch = getMainRedditSearch(preferences),
            viewedWallpapersPreferences = getViewedWallpapersPreferences(preferences),
            acraEnabled = preferences[PreferencesKeys.ENABLE_ACRA] ?: true,
        )
    }

    private fun getViewedWallpapersPreferences(preferences: Preferences) =
        ViewedWallpapersPreferences(
            enabled = preferences[PreferencesKeys.VIEWED_WALLPAPERS_ENABLED] ?: false,
            look = try {
                ViewedWallpapersLook.valueOf(
                    preferences[PreferencesKeys.VIEWED_WALLPAPERS_LOOK]
                        ?: ViewedWallpapersLook.DIM_WITH_LABEL.name,
                )
            } catch (e: Exception) {
                ViewedWallpapersLook.DIM_WITH_LABEL
            },
        )

    private fun getTagsWriteType(preferences: Preferences): ExifWriteType {
        val tagsWriteTypeStr = preferences[PreferencesKeys.TAGS_WRITE_TYPE]
        return if (tagsWriteTypeStr != null) {
            try {
                ExifWriteType.valueOf(tagsWriteTypeStr)
            } catch (e: Exception) {
                ExifWriteType.APPEND
            }
        } else {
            ExifWriteType.APPEND
        }
    }

    private fun getHomeSources(
        preferences: Preferences,
        homeRedditSearch: RedditSearch?,
    ) = try {
        val sourcesStr = preferences[PreferencesKeys.HOME_SOURCES] ?: ""
        val sources = json.decodeFromString<Map<OnlineSource, Boolean>>(sourcesStr)
        sources.mapValues {
            if (it.key == OnlineSource.REDDIT) {
                it.value && homeRedditSearch != null
            } else {
                it.value
            }
        }
    } catch (e: Exception) {
        val mutableMap = mutableMapOf(
            OnlineSource.WALLHAVEN to true,
        )
        if (homeRedditSearch != null) {
            mutableMap[OnlineSource.REDDIT] = true
        }
        mutableMap
    }

    private fun getLocalWallpapersPreferences(
        preferences: Preferences,
    ): LocalWallpapersPreferences {
        val localDirStrings = preferences[PreferencesKeys.LOCAL_DIRS]
        val localDirs = localDirStrings
            ?.mapNotNullTo(HashSet(), String::toUriOrNull)
            ?: context.accessibleFolders.mapTo(HashSet()) { it.uri }
        return LocalWallpapersPreferences(
            sort = try {
                LocalSort.valueOf(preferences[PreferencesKeys.LOCAL_WALLPAPERS_SORT] ?: "")
            } catch (e: Exception) {
                LocalSort.NO_SORT
            },
            directories = localDirs,
        )
    }

    private fun getLookAndFeelPreferences(preferences: Preferences) = LookAndFeelPreferences(
        theme = try {
            Theme.valueOf(preferences[PreferencesKeys.THEME] ?: "")
        } catch (e: Exception) {
            Theme.SYSTEM
        },
        layoutPreferences = getLayoutPreferences(preferences),
        showLocalTab = preferences[PreferencesKeys.SHOW_LOCAL_TAB] ?: true,
    )

    private fun getLayoutPreferences(preferences: Preferences) = LayoutPreferences(
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
    )

    private suspend fun getAutoWallpaperPreferences(
        preferences: Preferences,
    ) = with(preferences) {
        val savedSearchIdStrings = get(PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ID)
            ?: emptySet()
        val savedSearchIds = savedSearchIdStrings
            .mapNotNull { it.toLongOrNull() }
            .filterTo(HashSet()) { it > 0 }
        val savedSearchEnabled =
            (get(PreferencesKeys.AUTO_WALLPAPER_SAVED_SEARCH_ENABLED) ?: false) &&
                savedSearchIds.isNotEmpty()

        val lsSavedSearchIdStrings = get(PreferencesKeys.AUTO_WALLPAPER_LS_SAVED_SEARCH_ID)
            ?: savedSearchIdStrings
        val lsSavedSearchIds = lsSavedSearchIdStrings
            .mapNotNull { it.toLongOrNull() }
            .filterTo(HashSet()) { it > 0 }
        val lsSavedSearchEnabled = (
            get(PreferencesKeys.AUTO_WALLPAPER_LS_SAVED_SEARCH_ENABLED)
                ?: savedSearchEnabled
            ) && lsSavedSearchIds.isNotEmpty()
        val hasFavorites = favoritesRepository.getCount() > 0
        val favoritesEnabled = hasFavorites &&
            get(PreferencesKeys.AUTO_WALLPAPER_FAVORITES_ENABLED) ?: false
        val lsFavoritesEnabled = hasFavorites &&
            get(PreferencesKeys.AUTO_WALLPAPER_LS_FAVORITES_ENABLED) ?: favoritesEnabled

        val localDirStrings = get(PreferencesKeys.AUTO_WALLPAPER_LOCAL_DIRS)
        val accessibleFolderUris = context.accessibleFolders.mapTo(HashSet()) { it.uri }
        val localDirs = localDirStrings
            ?.mapNotNullTo(HashSet()) {
                getUriIfAccessible(it, accessibleFolderUris)
            } ?: accessibleFolderUris
        val localEnabled = (get(PreferencesKeys.AUTO_WALLPAPER_LOCAL_ENABLED) ?: false) &&
            localDirs.isNotEmpty()

        val lsLocalDirStrings = get(PreferencesKeys.AUTO_WALLPAPER_LS_LOCAL_DIRS)
            ?: localDirs.map { it.toString() }
        val lsLocalDirs = lsLocalDirStrings.mapNotNullTo(HashSet()) {
            getUriIfAccessible(it, accessibleFolderUris)
        }
        val lsLocalEnabled = (
            get(PreferencesKeys.AUTO_WALLPAPER_LS_LOCAL_ENABLED)
                ?: localEnabled
            ) && lsLocalDirs.isNotEmpty()

        val lightDarkEnabled = get(PreferencesKeys.AUTO_WALLPAPER_LIGHT_DARK_ENABLED) ?: false
        val lsLightDarkEnabled = get(PreferencesKeys.AUTO_WALLPAPER_LS_LIGHT_DARK_ENABLED)
            ?: lightDarkEnabled

        val useDarkWithExtraDim = get(PreferencesKeys.AUTO_WALLPAPER_USE_DARK_WITH_EXTRA_DIM)
            ?: false
        val lsUseDarkWithExtraDim = get(PreferencesKeys.AUTO_WALLPAPER_LS_USE_DARK_WITH_EXTRA_DIM)
            ?: useDarkWithExtraDim

        val anyHomeScreenSourceEnabled = lightDarkEnabled || (
            savedSearchEnabled &&
                savedSearchIds.isNotEmpty() &&
                savedSearchIds.all { it > 0 }
            ) ||
            favoritesEnabled ||
            localEnabled
        val anyLockScreenSourceEnabled = lsLightDarkEnabled || (
            lsSavedSearchEnabled &&
                lsSavedSearchIds.isNotEmpty() &&
                lsSavedSearchIds.all { it > 0 }
            ) ||
            lsFavoritesEnabled ||
            lsLocalEnabled
        val anySourceEnabled = anyHomeScreenSourceEnabled || anyLockScreenSourceEnabled
        AutoWallpaperPreferences(
            enabled = when {
                !anySourceEnabled -> false
                else -> get(PreferencesKeys.ENABLE_AUTO_WALLPAPER) ?: false
            },
            savedSearchEnabled = savedSearchEnabled,
            lsSavedSearchEnabled = lsSavedSearchEnabled,
            favoritesEnabled = favoritesEnabled,
            lsFavoritesEnabled = lsFavoritesEnabled,
            localEnabled = localEnabled,
            lsLocalEnabled = lsLocalEnabled,
            savedSearchIds = savedSearchIds,
            lsSavedSearchIds = lsSavedSearchIds,
            localDirs = localDirs,
            lsLocalDirs = lsLocalDirs,
            useObjectDetection = get(PreferencesKeys.AUTO_WALLPAPER_USE_OBJECT_DETECTION)
                ?: true,
            useSameFreq = get(PreferencesKeys.AUTO_WALLPAPER_USE_SAME_FREQUENCY) ?: true,
            frequency = parseFrequency(get(PreferencesKeys.AUTO_WALLPAPER_FREQUENCY)),
            lsFrequency = parseFrequency(get(PreferencesKeys.AUTO_WALLPAPER_LS_FREQUENCY)),
            constraints = parseConstraints(get(PreferencesKeys.AUTO_WALLPAPER_CONSTRAINTS)),
            workRequestId = parseWorkRequestId(
                get(PreferencesKeys.AUTO_WALLPAPER_WORK_REQUEST_ID),
            ),
            lsWorkRequestId = parseWorkRequestId(
                get(PreferencesKeys.AUTO_WALLPAPER_LS_WORK_REQUEST_ID),
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
            setDifferentWallpapers = get(PreferencesKeys.AUTO_WALLPAPER_SET_DIFFERENT_WALLPAPERS)
                ?: false,
            crop = get(PreferencesKeys.AUTO_WALLPAPER_CROP) ?: true,
            lightDarkEnabled = lightDarkEnabled,
            lsLightDarkEnabled = lsLightDarkEnabled,
            useDarkWithExtraDim = useDarkWithExtraDim,
            lsUseDarkWithExtraDim = lsUseDarkWithExtraDim,
            backoffUpdated = get(PreferencesKeys.AUTO_WALLPAPER_BACKOFF_UPDATED) ?: false,
            prevHomeSource = valueOf<SourceChoice>(
                get(PreferencesKeys.AUTO_WALLPAPER_PREV_HOME_SOURCE),
            ),
            prevLockScreenSource = valueOf<SourceChoice>(
                get(PreferencesKeys.AUTO_WALLPAPER_PREV_LS_SOURCE),
            ),
        )
    }

    private fun getUriIfAccessible(
        uriString: String,
        accessibleFolderUris: Set<Uri>,
    ) = try {
        val uri = Uri.parse(uriString)
        if (uri in accessibleFolderUris) {
            uri
        } else {
            null
        }
    } catch (exception: Exception) {
        null
    }

    private fun getObjectDetectionPreferences(preferences: Preferences) = with(preferences) {
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
    }

    private fun getHomeRedditSearch(preferences: Preferences): RedditSearch? = try {
        json.decodeFromString(preferences[PreferencesKeys.HOME_REDDIT_SEARCH] ?: "")
    } catch (e: Exception) {
        null
    }

    private fun getHomeWallhavenSearch(preferences: Preferences) = try {
        WallhavenSearch.fromJson(preferences[PreferencesKeys.HOME_WALLHAVEN_SEARCH] ?: "")
    } catch (e: Exception) {
        WallhavenSearch(
            filters = WallhavenFilters(
                sorting = WallhavenSorting.TOPLIST,
                topRange = WallhavenTopRange.ONE_DAY,
            ),
        )
    }

    private fun getMainWallhavenSearch(preferences: Preferences): WallhavenSearch? = try {
        json.decodeFromString(preferences[PreferencesKeys.MAIN_WALLHAVEN_SEARCH] ?: "")
    } catch (e: Exception) {
        null
    }

    private fun getMainRedditSearch(preferences: Preferences): RedditSearch? = try {
        json.decodeFromString(preferences[PreferencesKeys.MAIN_REDDIT_SEARCH] ?: "")
    } catch (e: Exception) {
        null
    }

    private fun getWallhavenApiKey(preferences: Preferences) =
        preferences[PreferencesKeys.WALLHAVEN_API_KEY] ?: ""

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
            json.decodeFromString(
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

    suspend fun getWallHavenApiKey() = getWallhavenApiKey(dataStore.data.first())

    suspend fun getAutoWallpaperWorkRequestId() = getAutoWallpaperPreferences(
        dataStore.data.first(),
    ).workRequestId

    suspend fun getAutoWallpaperLsWorkRequestId() = getAutoWallpaperPreferences(
        dataStore.data.first(),
    ).lsWorkRequestId

    suspend fun getAutoWallBackoffUpdated() = getAutoWallpaperPreferences(
        dataStore.data.first(),
    ).backoffUpdated

    suspend fun setPreferences(appPreferences: AppPreferences) {
        dataStore.edit {
            it.apply {
                set(
                    PreferencesKeys.VERSION,
                    appPreferences.version ?: AppPreferences.CURRENT_VERSION,
                )
                updateWallhavenApikey(appPreferences.wallhavenApiKey)
                updateHomeWallhavenSearch(appPreferences.homeWallhavenSearch)
                appPreferences.homeRedditSearch?.run {
                    updateHomeRedditSearch(this@run)
                }
                updateHomeSources(appPreferences.homeSources)
                updateBlurSketchy(appPreferences.blurSketchy)
                updateBlurNsfw(appPreferences.blurNsfw)
                updateWriteTagsToExif(appPreferences.writeTagsToExif)
                updateTagsWriteType(appPreferences.tagsExifWriteType)
                updateObjectDetectionPrefs(appPreferences.objectDetectionPreferences)
                updateAutoWallpaperPrefs(appPreferences.autoWallpaperPreferences)
                updateLookAndFeelPreferences(appPreferences.lookAndFeelPreferences)
                updateLocalWallpapersSort(appPreferences.localWallpapersPreferences.sort)
                if (appPreferences.mainWallhavenSearch != null) {
                    updateMainSearch(appPreferences.mainWallhavenSearch)
                }
                if (appPreferences.mainRedditSearch != null) {
                    updateMainSearch(appPreferences.mainRedditSearch)
                }
                updateViewedWallpapersPreferences(appPreferences.viewedWallpapersPreferences)
                updateAcraEnabled(appPreferences.acraEnabled)
            }
        }
    }
}
