package com.ammar.wallflow.model

import com.ammar.wallflow.data.db.entity.FavoriteEntity
import kotlinx.datetime.Instant

data class Favorite(
    val sourceId: String,
    val source: Source,
)

fun Favorite.toEntity(
    id: Long = 0,
    favoritedOn: Instant,
) = FavoriteEntity(
    id = id,
    sourceId = sourceId,
    source = source,
    favoritedOn = favoritedOn,
)
