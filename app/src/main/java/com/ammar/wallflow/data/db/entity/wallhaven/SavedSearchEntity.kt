package com.ammar.wallflow.data.db.entity.wallhaven

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.Filters
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
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

fun SavedSearchEntity.toSavedSearch() = SavedSearch(
    id = id,
    name = name,
    search = when (val filters: Filters = json.decodeFromString(this.filters)) {
        is RedditFilters -> RedditSearch(
            query = query,
            filters = filters,
        )
        is WallhavenFilters -> WallhavenSearch(
            query = query,
            filters = filters,
        )
    },
)
