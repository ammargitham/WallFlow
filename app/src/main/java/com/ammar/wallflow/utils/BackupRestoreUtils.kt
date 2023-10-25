package com.ammar.wallflow.utils

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.search.toSavedSearch
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
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
): String? {
    if (!options.atleastOneChosen) {
        return null
    }
    var appPreferences: AppPreferences? = null
    var favorites: List<FavoriteEntity>? = null
    var wallhavenBackupV1 = WallhavenBackupV1(
        wallpapers = null,
        uploaders = null,
        tags = null,
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
            val wallpapersWithUploaderAndTags = wallhavenWallpapersDao
                .getAllWithUploaderAndTagsByWallhavenIds(wallhavenWallpaperIds)
            val wallpapers = mutableListOf<WallhavenWallpaperEntity>()
            val uploaders = mutableListOf<WallhavenUploaderEntity>()
            val tags = mutableSetOf<WallhavenTagEntity>()
            wallpapersWithUploaderAndTags.forEach {
                wallpapers.add(it.wallpaper)
                if (it.uploader != null) {
                    uploaders.add(it.uploader)
                }
                if (it.tags != null) {
                    tags.addAll(it.tags)
                }
            }
            wallhavenBackupV1 = wallhavenBackupV1.copy(
                wallpapers = wallpapers,
                uploaders = uploaders,
                tags = tags,
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
    uploadersDao: WallhavenUploadersDao,
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
            uploadersDao = uploadersDao,
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
    uploadersDao: WallhavenUploadersDao,
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
    if (options.favorites && backup.favorites?.isNotEmpty() == true) {
        // restore sources first
        // 1. Wallhaven
        //    - Tags
        //    - Uploaders
        //    - Wallpapers
        val wallhavenWallpapers = backup.wallhaven?.wallpapers
        if (wallhavenWallpapers?.isNotEmpty() == true) {
            // restore tags
            val tags = backup.wallhaven.tags
            if (tags != null) {
                wallhavenRepository.insertTagEntities(tags)
            }
            // restore uploaders
            val uploaders = backup.wallhaven.uploaders
            val uploaderIdUpdateMap = mutableMapOf<Long, Long>()
            if (uploaders != null) {
                val uploaderUsernameMap = uploaders.associate {
                    it.username to it.id
                }
                wallhavenRepository.insertUploaderEntities(uploaders)
                // we need new uploader db ids to update field in wallpaper entities
                val uploadersByUsernames = uploadersDao.getByUsernames(uploaderUsernameMap.keys)
                val newUsernameMap = uploadersByUsernames.associate {
                    it.username to it.id
                }
                uploaderUsernameMap.entries.forEach {
                    val newId = newUsernameMap[it.key] ?: return@forEach
                    val oldId = it.value
                    uploaderIdUpdateMap[oldId] = newId
                }
            }
            var updatedWallhavenWallpapers = wallhavenWallpapers
            // restore wallpaper uploaderIds
            if (uploaderIdUpdateMap.isNotEmpty()) {
                // uploader id map cannot be empty while inserting wallpapers
                updatedWallhavenWallpapers = wallhavenWallpapers.map {
                    it.copy(
                        uploaderId = uploaderIdUpdateMap[it.uploaderId],
                    )
                }
            }
            // restore wallpapers
            wallhavenRepository.insertWallpaperEntities(updatedWallhavenWallpapers)
        }
        // 2. Reddit wallpapers
        val redditWallpapers = backup.reddit?.wallpapers
        if (redditWallpapers?.isNotEmpty() == true) {
            // restore wallpapers
            redditRepository.insertWallpaperEntities(redditWallpapers)
        }
        val existingWallhavenWallpaperIds = wallhavenWallpapersDao.getAllWallhavenIds()
        val existingRedditWallpaperIds = redditWallpapersDao.getAllRedditIds()
        val favoritesToInsert = backup.favorites.filter {
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
        favoritesRepository.insertEntities(favoritesToInsert)
    }
    if (options.settings && backup.preferences != null) {
        // update saved search ids depending on whether they exist
        val prevAutoWallpaperPrefs = backup.preferences.autoWallpaperPreferences
        val updatedSavedSearchIds = prevAutoWallpaperPrefs.savedSearchIds.filter {
            savedSearchRepository.exists(it)
        }.toSet()
        val updatedPrefs = backup.preferences.copy(
            autoWallpaperPreferences = prevAutoWallpaperPrefs.copy(
                savedSearchIds = updatedSavedSearchIds,
                savedSearchEnabled = prevAutoWallpaperPrefs.savedSearchEnabled &&
                    updatedSavedSearchIds.isNotEmpty(),
            ),
        )
        appPreferencesRepository.setPreferences(updatedPrefs)
    }
}

fun migrateBackupJson(currentJson: JsonObject) = try {
    val prefs = currentJson["preferences"]?.let {
        // migrate preferences
        migratePrefs(it.jsonObject)
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
    if (version == 2) {
        // no migration needed
        return prefsJson
    }
    return when (version) {
        1 -> migratePrefs1To2(prefsJson)
        else -> throw IllegalArgumentException("Invalid app prefs version: $version")
    }
}

private fun migratePrefs1To2(prefsJson: JsonObject): JsonObject {
    return JsonObject(
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
}
