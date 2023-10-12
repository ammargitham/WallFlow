package com.ammar.wallflow.model.search

import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchHistoryEntity
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
sealed class Search {
    abstract val query: String
    abstract val filters: Filters
    abstract val meta: SearchMeta?
}

fun Search.toSearchHistoryEntity(
    id: Long = 0,
    lastUpdatedOn: Instant,
) = WallhavenSearchHistoryEntity(
    id = id,
    query = query.trimAll().lowercase(),
    filters = json.encodeToString(filters),
    lastUpdatedOn = lastUpdatedOn,
)

val SearchSaver = Saver<Search, String>(
    save = { json.encodeToString(it) },
    restore = { json.decodeFromString(it) },
)
