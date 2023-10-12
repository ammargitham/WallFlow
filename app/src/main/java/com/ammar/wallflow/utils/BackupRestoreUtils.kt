package com.ammar.wallflow.utils

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenSavedSearchDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.toWallhavenSavedSearch
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.extensions.format
import com.ammar.wallflow.json
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.backup.Backup
import com.ammar.wallflow.model.backup.BackupOptions
import com.ammar.wallflow.model.backup.BackupV1
import com.ammar.wallflow.model.backup.InvalidJsonException
import com.ammar.wallflow.model.backup.WallhavenBackupV1
import com.ammar.wallflow.safeJson
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val backupFileName
    get() = "wallflow_backup_${Clock.System.now().format("yyyyMMddHHmmss")}.json"

suspend fun getBackupV1Json(
    options: BackupOptions,
    appPreferencesRepository: AppPreferencesRepository,
    favoriteDao: FavoriteDao,
    wallpapersDao: WallhavenWallpapersDao,
    savedSearchDao: WallhavenSavedSearchDao,
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
            val wallpapersWithUploaderAndTags = wallpapersDao
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
    }

    if (options.savedSearches) {
        wallhavenBackupV1 = wallhavenBackupV1.copy(
            savedSearches = savedSearchDao.getAll(),
        )
    }

    val backupV1 = BackupV1(
        preferences = appPreferences,
        favorites = favorites,
        wallhaven = wallhavenBackupV1,
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
    favoritesRepository: FavoritesRepository,
    wallpapersDao: WallhavenWallpapersDao,
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
            favoritesRepository = favoritesRepository,
            wallpapersDao = wallpapersDao,
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
    favoritesRepository: FavoritesRepository,
    wallpapersDao: WallhavenWallpapersDao,
    uploadersDao: WallhavenUploadersDao,
) {
    if (!options.atleastOneChosen) {
        return
    }
    if (options.settings && backup.preferences != null) {
        appPreferencesRepository.setPreferences(backup.preferences)
    }
    // restore saved searches first
    if (options.savedSearches) {
        val savedSearches = backup.wallhaven?.savedSearches
        if (savedSearches != null) {
            savedSearchRepository.upsertAll(
                savedSearches.map { it.toWallhavenSavedSearch() },
            )
        }
    }
    if (options.favorites && backup.favorites?.isNotEmpty() == true) {
        // restore sources first
        // 1. First Wallhaven
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
            // restore wallpapers
            if (uploaderIdUpdateMap.isNotEmpty()) {
                // uploader id map cannot be empty while inserting wallpapers
                val updatedWallhavenWallpapers = wallhavenWallpapers.map {
                    it.copy(
                        uploaderId = uploaderIdUpdateMap[it.uploaderId],
                    )
                }
                wallhavenRepository.insertWallpaperEntities(updatedWallhavenWallpapers)
            }
        }
        val existingWallhavenWallpaperIds = wallpapersDao.getAllWallhavenIds()
        val favoritesToInsert = backup.favorites.filter {
            when (it.source) {
                Source.WALLHAVEN -> it.sourceId in existingWallhavenWallpaperIds
                Source.LOCAL -> try {
                    DocumentFileCompat.fromSingleUri(context, it.sourceId.toUri()) != null
                } catch (e: Exception) {
                    false
                }
            }
        }
        favoritesRepository.insertEntities(favoritesToInsert)
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
    // we need to convert key `homeSearch` to `homeWallhavenSearch`
    val homeSearchKey = "homeSearch"
    val homeSearchJson = prefsJson[homeSearchKey]
    return JsonObject(
        prefsJson.toMutableMap().apply {
            // insert new key
            if (homeSearchJson != null) {
                put("homeWallhavenSearch", homeSearchJson)
            }
            // set version to 2
            put("version", JsonPrimitive(2))
            // remove old key
            remove(homeSearchKey)
        },
    )
}
