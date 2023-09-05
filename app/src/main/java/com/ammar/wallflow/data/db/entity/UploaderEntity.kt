package com.ammar.wallflow.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.wallhaven.WallhavenAvatar
import com.ammar.wallflow.model.wallhaven.WallhavenUploader

@Entity(
    tableName = "uploaders",
    indices = [
        Index(
            value = ["username"],
            unique = true,
        ),
    ],
)
data class UploaderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val username: String,
    val group: String,
    @Embedded("avatar_") val avatar: AvatarEntity,
)

data class AvatarEntity(
    val large: String,
    val medium: String,
    val small: String,
    val tiny: String,
)

fun UploaderEntity.asUploader() = WallhavenUploader(
    username = username,
    group = group,
    avatar = avatar.asAvatar(),
)

fun AvatarEntity.asAvatar() = WallhavenAvatar(
    large = large,
    medium = medium,
    small = small,
    tiny = tiny,
)
