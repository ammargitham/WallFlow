package com.ammar.wallflow.model.backup

import android.net.Uri
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.preferences.AppPreferences
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class BackupV1(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault
    override val version: Int = 1,
    val preferences: AppPreferences?,
    val favorites: List<FavoriteEntity>?,
    val lightDark: List<LightDarkEntity>? = null,
    val viewed: List<ViewedEntity>? = null,
    val wallhaven: WallhavenBackupV1?,
    val reddit: RedditBackupV1? = null,
) : Backup {
    override fun getRestoreSummary(file: Uri) = RestoreSummary(
        file = file,
        settings = preferences != null,
        favorites = favorites?.size,
        lightDark = lightDark?.size,
        viewed = viewed?.size,
        savedSearches = (wallhaven?.savedSearches?.size ?: 0) + (reddit?.savedSearches?.size ?: 0),
        backup = this,
    )
}

@Serializable
data class WallhavenBackupV1(
    val savedSearches: List<SavedSearchEntity>?,
    val wallpapers: List<WallhavenWallpaperEntity>?,
)

@Serializable
data class RedditBackupV1(
    val savedSearches: List<SavedSearchEntity>?,
    val wallpapers: List<RedditWallpaperEntity>?,
)
