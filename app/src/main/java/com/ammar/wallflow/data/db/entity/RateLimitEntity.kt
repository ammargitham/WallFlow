package com.ammar.wallflow.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.Source
import kotlinx.datetime.Instant

@Entity(
    tableName = "rate_limits",
    indices = [
        Index(
            value = ["source"],
            unique = true,
        ),
    ],
)
data class RateLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val source: Source,
    val limit: Int? = null,
    val remaining: Int? = null,
    val reset: Instant? = null,
)
