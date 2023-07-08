package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "search_query",
    indices = [
        Index(
            value = ["query_string"],
            unique = true,
        )
    ]
)
data class SearchQueryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "query_string") val queryString: String,
    @ColumnInfo(name = "last_updated_on") val lastUpdatedOn: Instant,
)
