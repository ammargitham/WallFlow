package com.ammar.wallflow.utils

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.dao.ViewedDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.search.toSavedSearch
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.data.repository.reddit.RedditRepository
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.extensions.format
import com.ammar.wallflow.json
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.backup.Backup
import com.ammar.wallflow.model.backup.BackupOptions
import com.ammar.wallflow.model.backup.BackupV1
import com.ammar.wallflow.model.backup.InvalidJsonException
import com.ammar.wallflow.model.backup.RedditBackupV1
import com.ammar.wallflow.model.backup.WallhavenBackupV1
import com.ammar.wallflow.model.search.Filters
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.safeJson
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

val backupFileName
    get() = "wallflow_backup_${Clock.System.now().format("yyyyMMddHHmmss")}.json"

suspend fun getBackupV1Json(
    options: BackupOptions,
    appPreferencesRepository: AppPreferencesRepository,
    favoriteDao: FavoriteDao,
    wallhavenWallpapersDao: WallhavenWallpapersDao,
    redditWallpapersDao: RedditWallpapersDao,
    savedSearchDao: SavedSearchDao,
    viewedDao: ViewedDao,
    lightDarkDao: LightDarkDao,
): String? {
    if (!options.atleastOneChosen) {
        return null
    }
    var appPreferences: AppPreferences? = null
    var favorites: List<FavoriteEntity>? = null
    var lightDarkEntities: List<LightDarkEntity>? = null
    var viewed: List<ViewedEntity>? = null
    var wallhavenBackupV1 = WallhavenBackupV1(
        wallpapers = null,
        savedSearches = null,
    )
    var redditBackupV1 = RedditBackupV1(
        savedSearches = null,
        wallpapers = null,
    )

    if (options.settings) {
        appPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
    }

    if (options.favorites) {
        favorites = favoriteDao.getAll()
        // get all favorited wallhaven wallpapers
        val wallhavenWallpaperIds = favorites
            .filter { it.source == Source.WALLHAVEN }
            .map { it.sourceId }
        if (wallhavenWallpaperIds.isNotEmpty()) {
            val wallpapers = wallhavenWallpapersDao.getAllByWallhavenIds(wallhavenWallpaperIds)
            wallhavenBackupV1 = wallhavenBackupV1.copy(
                wallpapers = wallpapers,
            )
        }

        // get all favorited reddit wallpapers
        val redditWallpaperIds = favorites
            .filter { it.source == Source.REDDIT }
            .map { it.sourceId }
        if (redditWallpaperIds.isNotEmpty()) {
            val redditWallpapers = redditWallpapersDao.getByRedditIds(redditWallpaperIds)
            redditBackupV1 = redditBackupV1.copy(
                wallpapers = redditWallpapers,
            )
        }
    }

    if (options.lightDark) {
        lightDarkEntities = lightDarkDao.getAll()
        // get all light dark wallhaven wallpapers
        val wallhavenWallpaperIds = lightDarkEntities
            .filter { it.source == Source.WALLHAVEN }
            .mapTo(HashSet()) { it.sourceId }
        if (wallhavenWallpaperIds.isNotEmpty()) {
            var existingIds = emptySet<String>()
            val existingWallpapers = wallhavenBackupV1.wallpapers ?: emptyList()
            if (existingWallpapers.isNotEmpty()) {
                existingIds = existingWallpapers.mapTo(HashSet()) { it.wallhavenId }
            }
            val wallpapers = wallhavenWallpapersDao.getAllByWallhavenIds(
                wallhavenWallpaperIds - existingIds,
            )
            wallhavenBackupV1 = wallhavenBackupV1.copy(
                wallpapers = existingWallpapers + wallpapers,
            )
        }

        // get all light dark reddit wallpapers
        val redditWallpaperIds = lightDarkEntities
            .filter { it.source == Source.REDDIT }
            .map { it.sourceId }
        if (redditWallpaperIds.isNotEmpty()) {
            var existingIds = emptySet<String>()
            val existingWallpapers = redditBackupV1.wallpapers ?: emptyList()
            if (existingWallpapers.isNotEmpty()) {
                existingIds = existingWallpapers.mapTo(HashSet()) { it.redditId }
            }
            val wallpapers = redditWallpapersDao.getByRedditIds(
                redditWallpaperIds - existingIds,
            )
            redditBackupV1 = redditBackupV1.copy(
                wallpapers = existingWallpapers + wallpapers,
            )
        }
    }

    if (options.viewed) {
        viewed = viewedDao.getAll()
    }

    if (options.savedSearches) {
        val savedSearches = savedSearchDao.getAll()
        val map = savedSearches.groupBy {
            when (json.decodeFromString<Filters>(it.filters)) {
                is WallhavenFilters -> OnlineSource.WALLHAVEN
                is RedditFilters -> OnlineSource.REDDIT
            }
        }
        wallhavenBackupV1 = wallhavenBackupV1.copy(
            savedSearches = map[OnlineSource.WALLHAVEN] ?: emptyList(),
        )
        redditBackupV1 = redditBackupV1.copy(
            savedSearches = map[OnlineSource.REDDIT] ?: emptyList(),
        )
    }

    val backupV1 = BackupV1(
        preferences = appPreferences,
        favorites = favorites,
        lightDark = lightDarkEntities,
        viewed = viewed,
        wallhaven = wallhavenBackupV1,
        reddit = redditBackupV1,
    )
    return json.encodeToString(backupV1)
}

fun readBackupJson(
    json: String,
): Backup {
    val jsonElement = Json.parseToJsonElement(json)
    if (jsonElement !is JsonObject) {
        throw InvalidJsonException()
    }
    val jsonObject = jsonElement.jsonObject
    val version = jsonObject["version"]
        ?.jsonPrimitive
        ?.intOrNull
        ?: throw InvalidJsonException()
    return when (version) {
        1 -> readBackupV1Json(jsonObject)
        else -> throw InvalidJsonException()
    }
}

fun readBackupV1Json(
    jsonObject: JsonObject,
) = try {
    val updatedJsonObject = migrateBackupJson(jsonObject)
    safeJson.decodeFromJsonElement<BackupV1>(updatedJsonObject)
} catch (e: Exception) {
    throw InvalidJsonException(e)
}

suspend fun restoreBackup(
    context: Context,
    backup: Backup,
    options: BackupOptions,
    appPreferencesRepository: AppPreferencesRepository,
    savedSearchRepository: SavedSearchRepository,
    wallhavenRepository: WallhavenRepository,
    redditRepository: RedditRepository,
    favoritesRepository: FavoritesRepository,
    wallhavenWallpapersDao: WallhavenWallpapersDao,
    redditWallpapersDao: RedditWallpapersDao,
    viewedRepository: ViewedRepository,
    lightDarkRepository: LightDarkRepository,
) {
    when (backup.version) {
        1 -> restoreBackupV1(
            context = context,
            backup = backup as BackupV1,
            options = options,
            appPreferencesRepository = appPreferencesRepository,
            savedSearchRepository = savedSearchRepository,
            wallhavenRepository = wallhavenRepository,
            redditRepository = redditRepository,
            favoritesRepository = favoritesRepository,
            wallhavenWallpapersDao = wallhavenWallpapersDao,
            redditWallpapersDao = redditWallpapersDao,
            viewedRepository = viewedRepository,
            lightDarkRepository = lightDarkRepository,
        )
        else -> throw InvalidJsonException("Invalid version!")
    }
}

suspend fun restoreBackupV1(
    context: Context,
    backup: BackupV1,
    options: BackupOptions,
    appPreferencesRepository: AppPreferencesRepository,
    savedSearchRepository: SavedSearchRepository,
    wallhavenRepository: WallhavenRepository,
    redditRepository: RedditRepository,
    favoritesRepository: FavoritesRepository,
    wallhavenWallpapersDao: WallhavenWallpapersDao,
    redditWallpapersDao: RedditWallpapersDao,
    viewedRepository: ViewedRepository,
    lightDarkRepository: LightDarkRepository,
) {
    if (!options.atleastOneChosen) {
        return
    }
    // restore saved searches first
    if (options.savedSearches) {
        val wallhavenSavedSearches = backup.wallhaven?.savedSearches
        if (wallhavenSavedSearches != null) {
            savedSearchRepository.upsertAll(
                wallhavenSavedSearches.map { it.toSavedSearch() },
            )
        }
        val redditSavedSearches = backup.reddit?.savedSearches
        if (redditSavedSearches != null) {
            savedSearchRepository.upsertAll(
                redditSavedSearches.map { it.toSavedSearch() },
            )
        }
    }
    val hasFavorites = options.favorites && backup.favorites?.isNotEmpty() == true
    val hasLightDark = options.lightDark && backup.lightDark?.isNotEmpty() == true
    if (hasFavorites || hasLightDark) {
        // restore sources first
        // 1. Wallhaven
        //    - Wallpapers
        val wallhavenWallpapers = backup.wallhaven?.wallpapers
        if (wallhavenWallpapers?.isNotEmpty() == true) {
            // restore wallpapers
            wallhavenRepository.insertWallpaperEntities(wallhavenWallpapers)
        }
        // 2. Reddit wallpapers
        val redditWallpapers = backup.reddit?.wallpapers
        if (redditWallpapers?.isNotEmpty() == true) {
            // restore wallpapers
            redditRepository.insertWallpaperEntities(redditWallpapers)
        }
    }

    val existingWallhavenWallpaperIds = wallhavenWallpapersDao.getAllWallhavenIds()
    val existingRedditWallpaperIds = redditWallpapersDao.getAllRedditIds()

    if (hasFavorites) {
        val favoritesToInsert = backup.favorites?.filter {
            when (it.source) {
                Source.WALLHAVEN -> it.sourceId in existingWallhavenWallpaperIds
                Source.REDDIT -> it.sourceId in existingRedditWallpaperIds
                Source.LOCAL -> try {
                    DocumentFileCompat.fromSingleUri(context, it.sourceId.toUri()) != null
                } catch (e: Exception) {
                    false
                }
            }
        }
        if (favoritesToInsert != null) {
            favoritesRepository.insertEntities(favoritesToInsert)
        }
    }

    if (hasLightDark) {
        val lightDarkToInsert = backup.lightDark?.filter {
            when (it.source) {
                Source.WALLHAVEN -> it.sourceId in existingWallhavenWallpaperIds
                Source.REDDIT -> it.sourceId in existingRedditWallpaperIds
                Source.LOCAL -> try {
                    DocumentFileCompat.fromSingleUri(context, it.sourceId.toUri()) != null
                } catch (e: Exception) {
                    false
                }
            }
        }
        if (lightDarkToInsert != null) {
            lightDarkRepository.insertEntities(lightDarkToInsert)
        }
    }

    if (options.viewed && backup.viewed?.isNotEmpty() == true) {
        viewedRepository.insertEntities(backup.viewed)
    }
    if (options.settings && backup.preferences != null) {
        // update saved search ids depending on whether they exist
        val prevAutoWallpaperPrefs = backup.preferences.autoWallpaperPreferences
        val updatedSavedSearchIds = prevAutoWallpaperPrefs.savedSearchIds.filter {
            savedSearchRepository.exists(it)
        }.toSet()
        val updatedLsSavedSearchIds = prevAutoWallpaperPrefs.lsSavedSearchIds.filter {
            savedSearchRepository.exists(it)
        }.toSet()
        val updatedPrefs = backup.preferences.copy(
            autoWallpaperPreferences = prevAutoWallpaperPrefs.copy(
                savedSearchIds = updatedSavedSearchIds,
                savedSearchEnabled = prevAutoWallpaperPrefs.savedSearchEnabled &&
                    updatedSavedSearchIds.isNotEmpty(),
                lsSavedSearchIds = updatedLsSavedSearchIds,
                lsSavedSearchEnabled = prevAutoWallpaperPrefs.lsSavedSearchEnabled &&
                    updatedLsSavedSearchIds.isNotEmpty(),
            ),
        )
        appPreferencesRepository.setPreferences(updatedPrefs)
    }
}

fun migrateBackupJson(currentJson: JsonObject) = try {
    val prefs = currentJson["preferences"]?.let {
        // migrate preferences
        if (it is JsonObject) {
            migratePrefs(it.jsonObject)
        } else {
            null
        }
    }
    JsonObject(
        currentJson.toMutableMap().apply {
            if (prefs != null) {
                put("preferences", prefs)
            }
        },
    )
} catch (e: Exception) {
    Log.e("migrateBackupJson", "Error migrating: ", e)
    currentJson
}

private fun migratePrefs(prefsJson: JsonObject): JsonObject {
    val version = prefsJson["version"]?.jsonPrimitive?.intOrNull ?: 1
    if (version >= 2) {
        // no migration needed
        return prefsJson
    }
    return when (version) {
        1 -> migratePrefs1To2(prefsJson)
        else -> throw IllegalArgumentException("Invalid app prefs version: $version")
    }
}

private fun migratePrefs1To2(prefsJson: JsonObject) = JsonObject(
    prefsJson.toMutableMap().apply root@{
        val homeSearchKey = "homeSearch"
        val homeSearchJson = this[homeSearchKey]
        // fix types for CategoryWallhavenRatio and SizeWallhavenRatio
        val mutableHomeSearchJson = homeSearchJson?.jsonObject?.toMutableMap()
        val ratios = homeSearchJson?.jsonObject
            ?.get("filters")?.jsonObject
            ?.get("ratios")?.jsonArray
        val mutableRatios = ratios?.toMutableList()
        ratios?.forEachIndexed { i, ele ->
            val ratio = ele.jsonObject
            val type = ratio["type"]?.jsonPrimitive?.content
            if (type != null) {
                val newType = when (type) {
                    "com.ammar.wallflow.model.Ratio.CategoryRatio" ->
                        "com.ammar.wallflow.model.search.WallhavenRatio.CategoryWallhavenRatio"
                    "com.ammar.wallflow.model.Ratio.SizeRatio" ->
                        "com.ammar.wallflow.model.search.WallhavenRatio.SizeWallhavenRatio"
                    else -> null
                }
                if (newType != null) {
                    val mutableRatio = ratio.toMutableMap()
                    mutableRatio["type"] = JsonPrimitive(newType)
                    mutableRatios?.set(i, JsonObject(mutableRatio))
                }
            }
        }
        if (mutableHomeSearchJson != null && mutableRatios != null) {
            val mutableFilters = mutableHomeSearchJson["filters"]?.jsonObject?.toMutableMap()
            mutableFilters?.set("ratios", JsonArray(mutableRatios))
            if (mutableFilters != null) {
                mutableHomeSearchJson["filters"] = JsonObject(mutableFilters)
            }
        }

        // convert key `homeSearch` to `homeWallhavenSearch`
        if (mutableHomeSearchJson != null) {
            put("homeWallhavenSearch", JsonObject(mutableHomeSearchJson))
        }
        remove(homeSearchKey)

        // convert autoWallpaperPreferences.savedSearchId to savedSearchIds
        val autoWallpaperPrefs = this["autoWallpaperPreferences"]?.jsonObject
        if (autoWallpaperPrefs != null) {
            val savedSearchIdKey = "savedSearchId"
            val savedSearchIdValue = autoWallpaperPrefs[savedSearchIdKey]
                ?.jsonPrimitive
                ?.longOrNull
            if (savedSearchIdValue != null) {
                val updatedAutoWallpaperPrefsJson = JsonObject(
                    autoWallpaperPrefs.toMutableMap().apply autoWall@{
                        this["savedSearchIds"] = JsonArray(
                            listOf(JsonPrimitive(savedSearchIdValue)),
                        )
                        this.remove(savedSearchIdKey)
                    },
                )
                this@root["autoWallpaperPreferences"] = updatedAutoWallpaperPrefsJson
            }
        }

        // set version to 2
        put("version", JsonPrimitive(2))
    },
)
