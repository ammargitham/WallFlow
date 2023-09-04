package com.ammar.wallflow.data.network.model

import com.ammar.wallflow.data.db.entity.AvatarEntity
import com.ammar.wallflow.data.db.entity.UploaderEntity
import com.ammar.wallflow.model.WallhavenAvatar
import com.ammar.wallflow.model.WallhavenUploader
import kotlinx.serialization.Serializable

@Serializable
data class NetworkWallhavenUploader(
    val username: String,
    val group: String,
    val avatar: Map<String, String>,
)

fun NetworkWallhavenUploader.asUploaderEntity(id: Long = 0) = UploaderEntity(
    id = id,
    username = username,
    group = group,
    avatar = AvatarEntity(
        large = avatar["200px"] ?: "",
        medium = avatar["128px"] ?: "",
        small = avatar["32px"] ?: "",
        tiny = avatar["20px"] ?: "",
    ),
)

fun NetworkWallhavenUploader.toWallhavenUploader() = WallhavenUploader(
    username = username,
    group = group,
    avatar = WallhavenAvatar(
        large = avatar["200px"] ?: "",
        medium = avatar["128px"] ?: "",
        small = avatar["32px"] ?: "",
        tiny = avatar["20px"] ?: "",
    ),
)
