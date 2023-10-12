package com.ammar.wallflow.model.search

import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSavedSearchEntity
import com.ammar.wallflow.json
import kotlinx.serialization.encodeToString

data class WallhavenSavedSearch(
    val id: Long = 0,
    val name: String = "",
    val search: Search = WallhavenSearch(),
)

fun WallhavenSavedSearch.toEntity(id: Long? = null) = WallhavenSavedSearchEntity(
    id = id ?: this.id,
    name = name,
    query = search.query,
    filters = json.encodeToString(search.filters),
)

val WallhavenSavedSearchSaver = Saver<WallhavenSavedSearch, List<Any>>(
    save = {
        listOf(
            it.id,
            it.name,
            it.search.query,
            json.encodeToString(it.search.filters),
        )
    },
    restore = {
        WallhavenSavedSearch(
            id = it[0] as Long,
            name = it[1] as String,
            search = WallhavenSearch(
                query = it[2] as String,
                filters = json.decodeFromString(it[3] as String),
            ),
        )
    },
)
