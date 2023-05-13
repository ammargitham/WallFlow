package com.ammar.havenwalls.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.havenwalls.model.Avatar
import com.ammar.havenwalls.model.Uploader

@Entity(
    tableName = "uploaders",
    indices = [
        Index(
            value = ["username"],
            unique = true,
        )
    ]
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

fun UploaderEntity.asUploader() = Uploader(
    username = username,
    group = group,
    avatar = avatar.asAvatar(),
)

fun AvatarEntity.asAvatar() = Avatar(
    large = large,
    medium = medium,
    small = small,
    tiny = tiny,
)
