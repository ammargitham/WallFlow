package com.ammar.havenwalls.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.havenwalls.data.common.SearchQuery
import com.ammar.havenwalls.model.Search
import kotlinx.datetime.Instant

@Entity(
    tableName = "search_history",
    indices = [
        Index(
            value = ["query"],
            unique = true,
        )
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val query: String,
    val filters: String,
    @ColumnInfo(name = "last_updated_on") val lastUpdatedOn: Instant,
)

fun SearchHistoryEntity.toSearch() = Search(
    query = query,
    filters = SearchQuery.fromQueryString(filters),
)
