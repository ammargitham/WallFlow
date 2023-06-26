package com.ammar.havenwalls.model

import com.ammar.havenwalls.data.db.entity.AutoWallpaperHistoryEntity
import kotlinx.datetime.Instant

data class AutoWallpaperHistory(
    val wallhavenId: String,
    val setOn: Instant,
)

fun AutoWallpaperHistory.toEntity(id: Long = 0) = AutoWallpaperHistoryEntity(
    id = id,
    wallhavenId = wallhavenId,
    setOn = setOn,
)
