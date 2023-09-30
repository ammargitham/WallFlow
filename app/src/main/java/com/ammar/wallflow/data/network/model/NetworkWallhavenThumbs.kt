package com.ammar.wallflow.data.network.model

import com.ammar.wallflow.data.db.entity.wallhaven.ThumbsEntity
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWallhavenThumbs(
    val large: String,
    val original: String,
    val small: String,
)

fun NetworkWallhavenThumbs.asThumbsEntity() = ThumbsEntity(
    large = large,
    original = original,
    small = small,
)
