package com.ammar.wallflow.data.network.model

import com.ammar.wallflow.data.db.entity.ThumbsEntity
import com.ammar.wallflow.model.WallhavenThumbs
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWallhavenThumbs(
    val large: String,
    val original: String,
    val small: String,
)

fun NetworkWallhavenThumbs.toWallhavenThumbs() = WallhavenThumbs(
    large = large,
    original = original,
    small = small,
)

fun NetworkWallhavenThumbs.asThumbsEntity() = ThumbsEntity(
    large = large,
    original = original,
    small = small,
)
