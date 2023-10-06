package com.ammar.wallflow.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.search.WallhavenSavedSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSearchQuery
import kotlinx.serialization.Serializable

@Entity(
    tableName = "saved_searches",
    indices = [
        Index(
            value = ["name"],
            unique = true,
        ),
    ],
)
@Serializable
data class WallhavenSavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val query: String,
    val filters: String,
)

fun WallhavenSavedSearchEntity.toWallhavenSavedSearch() = WallhavenSavedSearch(
    id = id,
    name = name,
    search = WallhavenSearch(
        query = query,
        filters = WallhavenSearchQuery.fromQueryString(filters),
    ),
)
