package com.ammar.wallflow.data.db.entity.wallhaven

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Entity(
    tableName = "wallhaven_tags",
    indices = [
        Index(
            value = ["wallhaven_id"],
            unique = true,
        ),
    ],
)
@Serializable
data class WallhavenTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "wallhaven_id") val wallhavenId: Long,
    val name: String,
    val alias: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val category: String,
    val purity: Purity,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

fun WallhavenTagEntity.asTag() = WallhavenTag(
    id = wallhavenId,
    name = name,
    alias = alias.split(",").map { it.trimAll() },
    categoryId = categoryId,
    category = category,
    purity = purity,
    createdAt = createdAt,
)
