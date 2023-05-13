package com.ammar.havenwalls.data.network.model

import com.ammar.havenwalls.data.db.entity.AvatarEntity
import com.ammar.havenwalls.data.db.entity.UploaderEntity
import com.ammar.havenwalls.model.Avatar
import com.ammar.havenwalls.model.Uploader
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUploader(
    val username: String,
    val group: String,
    val avatar: Map<String, String>,
)

fun NetworkUploader.asUploaderEntity(id: Long = 0) = UploaderEntity(
    id = id,
    username = username,
    group = group,
    avatar = AvatarEntity(
        large = avatar["200px"] ?: "",
        medium = avatar["128px"] ?: "",
        small = avatar["32px"] ?: "",
        tiny = avatar["20px"] ?: "",
    )
)

fun NetworkUploader.asUploader() = Uploader(
    username = username,
    group = group,
    avatar = Avatar(
        large = avatar["200px"] ?: "",
        medium = avatar["128px"] ?: "",
        small = avatar["32px"] ?: "",
        tiny = avatar["20px"] ?: "",
    )
)
