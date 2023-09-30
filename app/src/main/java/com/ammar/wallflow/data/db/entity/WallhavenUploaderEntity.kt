package com.ammar.wallflow.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.wallhaven.WallhavenAvatar
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.serialization.Serializable

@Entity(
    tableName = "wallhaven_uploaders",
    indices = [
        Index(
            value = ["username"],
            unique = true,
        ),
    ],
)
@Serializable
data class WallhavenUploaderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val username: String,
    val group: String,
    @Embedded("avatar_") val avatar: WallhavenAvatarEntity,
)

@Serializable
data class WallhavenAvatarEntity(
    val large: String,
    val medium: String,
    val small: String,
    val tiny: String,
)

fun WallhavenUploaderEntity.asUploader() = WallhavenUploader(
    username = username,
    group = group,
    avatar = avatar.asAvatar(),
)

fun WallhavenAvatarEntity.asAvatar() = WallhavenAvatar(
    large = large,
    medium = medium,
    small = small,
    tiny = tiny,
)
