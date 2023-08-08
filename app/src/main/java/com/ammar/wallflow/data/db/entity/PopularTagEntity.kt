package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "popular_tags",
    indices = [
        Index(
            value = ["tag_id"],
            unique = true,
        ),
    ],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PopularTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

data class PopularTagWithDetails(
    @Embedded val popularTagEntity: PopularTagEntity,
    @Relation(
        parentColumn = "tag_id",
        entityColumn = "id",
    )
    val tagEntity: TagEntity,
)
