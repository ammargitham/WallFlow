package com.ammar.wallflow.data.network.model.wallhaven

import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.network.model.serializers.InstantSerializer
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class NetworkWallhavenTag(
    val id: Long,
    val name: String,
    val alias: String,
    val category_id: Long,
    val category: String,
    val purity: String,
    @Serializable(InstantSerializer::class)
    val created_at: Instant,
)

fun NetworkWallhavenTag.toEntity(id: Long = 0) = WallhavenTagEntity(
    id = id,
    wallhavenId = this.id,
    name = name,
    alias = alias,
    categoryId = category_id,
    category = category,
    purity = Purity.fromName(purity),
    createdAt = created_at,
)

fun NetworkWallhavenTag.toWallhavenTag() = WallhavenTag(
    id = id,
    name = name,
    alias = alias.split(",").map { it.trimAll() },
    categoryId = category_id,
    category = category,
    purity = Purity.fromName(purity),
    createdAt = created_at,
)
