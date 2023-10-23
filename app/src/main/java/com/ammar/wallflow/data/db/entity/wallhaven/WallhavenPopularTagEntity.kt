package com.ammar.wallflow.data.db.entity.wallhaven

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "wallhaven_popular_tags",
    indices = [
        Index(
            value = ["tag_id"],
            unique = true,
        ),
    ],
    foreignKeys = [
        ForeignKey(
            entity = WallhavenTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WallhavenPopularTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

data class WallhavenPopularTagWithDetails(
    @Embedded val popularTagEntity: WallhavenPopularTagEntity,
    @Relation(
        parentColumn = "tag_id",
        entityColumn = "id",
    )
    val tagEntity: WallhavenTagEntity,
)
