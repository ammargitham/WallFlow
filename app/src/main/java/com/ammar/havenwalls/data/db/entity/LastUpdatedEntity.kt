package com.ammar.havenwalls.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "last_updated")
data class LastUpdatedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val key: String,
    @ColumnInfo(name = "last_updated_on") val lastUpdatedOn: Instant,
)

enum class LastUpdatedCategory(val key: String) {
    POPULAR_TAGS("popular_tags"),
}
