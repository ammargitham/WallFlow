package com.ammar.wallflow.model

import androidx.compose.runtime.Stable
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
import kotlinx.datetime.Instant

@Stable
data class AutoWallpaperHistory(
    val sourceId: String,
    val source: Source,
    val sourceChoice: SourceChoice,
    val setOn: Instant,
    val targets: Set<WallpaperTarget>?,
)

fun AutoWallpaperHistory.toEntity(id: Long = 0) = AutoWallpaperHistoryEntity(
    id = id,
    sourceId = sourceId,
    source = source,
    sourceChoice = sourceChoice,
    setOn = setOn,
    targets = targets,
)
