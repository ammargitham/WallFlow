package com.ammar.havenwalls.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.model.Sorting
import com.ammar.havenwalls.model.TopRange
import com.ammar.havenwalls.data.preferences.AppPreferences
import com.ammar.havenwalls.data.preferences.ObjectDetectionPreferences
import com.ammar.havenwalls.data.preferences.PreferencesKeys
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.model.Search
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.core.ComputeSettings.Delegate

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
                if (deletePref != null) Delegate.valueOf(deletePref) else Delegate.GPU
            } catch (e: Exception) {
                Delegate.GPU
            }
            ObjectDetectionPreferences(
                enabled = get(PreferencesKeys.ENABLE_OBJECT_DETECTION) ?: true,
                delegate = delegate,
                modelId = get(PreferencesKeys.OBJECT_DETECTION_MODEL_ID) ?: 0,
            )
        }
    )

    suspend fun getWallHavenApiKey() = mapAppPreferences(dataStore.data.first()).wallhavenApiKey
}
