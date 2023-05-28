package com.ammar.havenwalls.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.havenwalls.model.SavedSearch
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.SearchQuery

@Entity(
    tableName = "saved_searches",
    indices = [
        Index(
            value = ["name"],
            unique = true,
        )
    ]
)
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val query: String,
    val filters: String,
)

fun SavedSearchEntity.toSavedSearch() = SavedSearch(
    id = id,
    name = name,
    search = Search(
        query = query,
        filters = SearchQuery.fromQueryString(filters),
    )
)