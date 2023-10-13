package com.ammar.wallflow.data.db.entity.search

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.Filters
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import kotlinx.datetime.Instant

@Entity(
    tableName = "search_history",
    indices = [
        Index(
            value = ["query"],
            unique = true,
        ),
    ],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val query: String,
    val filters: String,
    @ColumnInfo(name = "last_updated_on") val lastUpdatedOn: Instant,
)

fun SearchHistoryEntity.toSearch() = when (
    val filters = json.decodeFromString<Filters>(this.filters)
) {
    is WallhavenFilters -> WallhavenSearch(
        query = query,
        filters = filters,
    )
    is RedditFilters -> RedditSearch(
        query = query,
        filters = filters,
    )
}
