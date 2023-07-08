package com.ammar.wallflow.data.network.model

import com.ammar.wallflow.data.db.entity.ThumbsEntity
import com.ammar.wallflow.model.Thumbs
import kotlinx.serialization.Serializable

@Serializable
data class NetworkThumbs(
    val large: String,
    val original: String,
    val small: String,
)

fun NetworkThumbs.asThumbs() = Thumbs(
    large = large,
    original = original,
    small = small,
)

fun NetworkThumbs.asThumbsEntity() = ThumbsEntity(
    large = large,
    original = original,
    small = small,
)
