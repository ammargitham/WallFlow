package com.ammar.havenwalls.model

import kotlinx.serialization.Serializable

@Serializable
data class Uploader(
    val username: String,
    val group: String,
    val avatar: Avatar,
)

@Serializable
data class Avatar(
    val large: String,
    val medium: String,
    val small: String,
    val tiny: String,
)
