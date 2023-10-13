package com.ammar.wallflow.model.search

import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.data.db.entity.search.SearchHistoryEntity
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
) = SearchHistoryEntity(
    id = id,
    query = query.trimAll().lowercase(),
    filters = json.encodeToString(filters),
    lastUpdatedOn = lastUpdatedOn,
)

val SearchSaver = Saver<Search, String>(
    save = { json.encodeToString(it) },
    restore = { json.decodeFromString(it) },
)
