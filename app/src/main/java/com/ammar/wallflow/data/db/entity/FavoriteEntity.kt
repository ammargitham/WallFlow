package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.Favorite
import com.ammar.wallflow.model.Source
import kotlinx.datetime.Instant

@Entity(
    tableName = "favorites",
    indices = [
        Index(
            value = ["source_id", "source"],
            unique = true,
        ),
    ],
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val source: Source,
    @ColumnInfo(name = "favorited_on") val favoritedOn: Instant,
)

fun FavoriteEntity.toFavorite() = Favorite(
    sourceId = sourceId,
    source = source,
)
