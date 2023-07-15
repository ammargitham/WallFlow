package com.ammar.wallflow.model

import androidx.compose.runtime.Stable
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import kotlinx.datetime.Instant

@Stable
data class AutoWallpaperHistory(
    val wallhavenId: String,
    val setOn: Instant,
)

fun AutoWallpaperHistory.toEntity(id: Long = 0) = AutoWallpaperHistoryEntity(
    id = id,
    wallhavenId = wallhavenId,
    setOn = setOn,
)
