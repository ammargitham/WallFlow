package com.ammar.wallflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_query_remote_keys",
    indices = [
        Index(
            value = ["search_query_id"],
            unique = true,
        ),
    ],
    foreignKeys = [
        ForeignKey(
            SearchQueryEntity::class,
            parentColumns = ["id"],
            childColumns = ["search_query_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SearchQueryRemoteKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "search_query_id") val searchQueryId: Long,
    @ColumnInfo(name = "next_page_number") val nextPageNumber: Int?,
)
