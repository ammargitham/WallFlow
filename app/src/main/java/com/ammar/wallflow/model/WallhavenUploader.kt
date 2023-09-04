package com.ammar.wallflow.model

import kotlinx.serialization.Serializable

@Serializable
data class WallhavenUploader(
    val username: String,
    val group: String,
    val avatar: WallhavenAvatar,
)

@Serializable
data class WallhavenAvatar(
    val large: String,
    val medium: String,
    val small: String,
    val tiny: String,
)
