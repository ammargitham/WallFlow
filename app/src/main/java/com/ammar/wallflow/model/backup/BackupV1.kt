package com.ammar.wallflow.model.backup

import android.net.Uri
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.TagEntity
import com.ammar.wallflow.data.db.entity.UploaderEntity
import com.ammar.wallflow.data.db.entity.WallpaperEntity
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
    val wallhaven: WallhavenBackupV1?,
) : Backup {
    override fun getRestoreSummary(file: Uri) = RestoreSummary(
        file = file,
        settings = preferences != null,
        favorites = favorites?.size,
        savedSearches = wallhaven?.savedSearches?.size,
        backup = this,
    )
}

@Serializable
data class WallhavenBackupV1(
    val savedSearches: List<SavedSearchEntity>?,
    val wallpapers: List<WallpaperEntity>?,
    val uploaders: List<UploaderEntity>?,
    val tags: Set<TagEntity>?,
)
