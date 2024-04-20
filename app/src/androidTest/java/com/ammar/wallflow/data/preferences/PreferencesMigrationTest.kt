package com.ammar.wallflow.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesMigrationTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = this.backgroundScope,
        produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
    )

    private suspend fun DataStore<Preferences>.clear() = this.edit { it.clear() }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun shouldMigrate1To2() = runTest(testDispatcher) {
        val dataStore = dataStore()
        try {
            val homeSearchQueryPrefKey = stringPreferencesKey("home_search_query")
            val homeFiltersPrefKey = stringPreferencesKey("home_filters")
            val prevAutoWallpaperSavedSearchIdKey = longPreferencesKey(
                "auto_wallpaper_saved_search_id",
            )
            val preferences = dataStore.edit {
                it[homeSearchQueryPrefKey] = "test"
                it[homeFiltersPrefKey] =
                    "includedTags=&excludedTags=&username=&tagId=&wallpaperId=" +
                    "&categories=anime%2Cgeneral%2Cpeople&purity=sfw&sorting=toplist&order=desc" +
                    "&topRange=1d&atleast=&resolutions=&ratios=portrait&colors=&seed="
                it[prevAutoWallpaperSavedSearchIdKey] = 1
            }
            val migrateAppPrefs1To2 = migrateAppPrefs1To2()
            assertTrue(migrateAppPrefs1To2.shouldMigrate(preferences))
            val updatedPrefs = migrateAppPrefs1To2.migrate(preferences)
            assertFalse(updatedPrefs.contains(homeSearchQueryPrefKey))
            assertFalse(updatedPrefs.contains(homeFiltersPrefKey))
            assertTrue(updatedPrefs.contains(PreferencesKeys.VERSION))
            assertEquals(2, updatedPrefs[PreferencesKeys.VERSION])
            val homeWallhavenSearchPrefKey = stringPreferencesKey("home_wallhaven_search")
            assertTrue(updatedPrefs.contains(homeWallhavenSearchPrefKey))
            val homeWallhavenSearchStr = updatedPrefs[homeWallhavenSearchPrefKey]
            assertNotNull(homeWallhavenSearchStr)
            // language=json
            assertEquals(
                """
                    {
                      "query": "test",
                      "filters": {
                        "sorting": "TOPLIST",
                        "topRange": "ONE_DAY",
                        "ratios": [
                          {
                            "type": "com.ammar.wallflow.model.search.WallhavenRatio.CategoryWallhavenRatio",
                            "category": "PORTRAIT"
                          }
                        ]
                      }
                    }
                """.trimIndent().replace(
                    // language=regexp
                    "\\s(?=[\\s\":{}\\[\\]])".toRegex(),
                    "",
                ),
                homeWallhavenSearchStr,
            )
            val autoWallpaperSavedSearchIdKey = stringSetPreferencesKey(
                "auto_wallpaper_saved_search_id",
            )
            assertEquals(setOf("1"), updatedPrefs[autoWallpaperSavedSearchIdKey])
        } finally {
            dataStore.clear()
        }
    }

    companion object {
        private const val TEST_DATASTORE_NAME: String = "test_datastore"
    }
}
