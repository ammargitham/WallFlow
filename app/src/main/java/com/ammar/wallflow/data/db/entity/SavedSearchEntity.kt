package com.ammar.wallflow.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.WallhavenSavedSearch
import com.ammar.wallflow.model.WallhavenSearch
import com.ammar.wallflow.model.wallhaven.WallhavenSearchQuery
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
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val query: String,
    val filters: String,
)

fun SavedSearchEntity.toSavedSearch() = WallhavenSavedSearch(
    id = id,
    name = name,
    search = WallhavenSearch(
        query = query,
        filters = WallhavenSearchQuery.fromQueryString(filters),
    ),
)
