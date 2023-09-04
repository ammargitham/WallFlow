package com.ammar.wallflow.model

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class WallhavenTag(
    val id: Long,
    val name: String,
    val alias: List<String>,
    val categoryId: Long,
    val category: String,
    val purity: Purity,
    val createdAt: Instant,
)
