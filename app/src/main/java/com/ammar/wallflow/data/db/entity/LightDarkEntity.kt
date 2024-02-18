package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.LightDark
import com.ammar.wallflow.model.Source
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Entity(
    tableName = "light_dark",
    indices = [
        Index(
            value = ["source_id", "source"],
            unique = true,
        ),
    ],
)
@Serializable
data class LightDarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val source: Source,
    val typeFlags: Int,
    @ColumnInfo(name = "updated_on") val updatedOn: Instant,
)

fun LightDarkEntity.toLightDark() = LightDark(
    sourceId = sourceId,
    source = source,
    typeFlags = typeFlags,
)
