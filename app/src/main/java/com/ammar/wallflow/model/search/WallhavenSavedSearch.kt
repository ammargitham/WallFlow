package com.ammar.wallflow.model.search

import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSavedSearchEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class WallhavenSavedSearch(
    val id: Long = 0,
    val name: String = "",
    val search: WallhavenSearch = WallhavenSearch(),
)

fun WallhavenSavedSearch.toEntity(id: Long? = null) = WallhavenSavedSearchEntity(
    id = id ?: this.id,
    name = name,
    query = search.query,
    filters = Json.encodeToString(search.filters),
)

val WallhavenSavedSearchSaver = Saver<WallhavenSavedSearch, List<Any>>(
    save = {
        listOf(
            it.id,
            it.name,
            it.search.query,
            Json.encodeToString(it.search.filters),
        )
    },
    restore = {
        WallhavenSavedSearch(
            id = it[0] as Long,
            name = it[1] as String,
            search = WallhavenSearch(
                query = it[2] as String,
                filters = Json.decodeFromString(it[3] as String),
            ),
        )
    },
)
