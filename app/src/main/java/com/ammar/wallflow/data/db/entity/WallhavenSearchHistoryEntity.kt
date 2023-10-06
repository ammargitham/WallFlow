package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.search.WallhavenSearchQuery
import kotlinx.datetime.Instant

@Entity(
    tableName = "wallhaven_search_history",
    indices = [
        Index(
            value = ["query"],
            unique = true,
        ),
    ],
)
data class WallhavenSearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val query: String,
    val filters: String,
    @ColumnInfo(name = "last_updated_on") val lastUpdatedOn: Instant,
)

fun WallhavenSearchHistoryEntity.toWallhavenSearch() = WallhavenSearch(
    query = query,
    filters = WallhavenSearchQuery.fromQueryString(filters),
)