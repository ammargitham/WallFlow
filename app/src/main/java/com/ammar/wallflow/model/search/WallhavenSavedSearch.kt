package com.ammar.wallflow.model.search

import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.json
import kotlinx.serialization.encodeToString

data class SavedSearch(
    val id: Long = 0,
    val name: String = "",
    val search: Search = WallhavenSearch(),
)

fun SavedSearch.toEntity(id: Long? = null) = SavedSearchEntity(
    id = id ?: this.id,
    name = name,
    query = search.query,
    filters = json.encodeToString(search.filters),
)

val SavedSearchSaver = Saver<SavedSearch, List<Any>>(
    save = {
        listOf(
            it.id,
            it.name,
            it.search.query,
            json.encodeToString(it.search.filters),
        )
    },
    restore = {
        SavedSearch(
            id = it[0] as Long,
            name = it[1] as String,
            search = when (
                val filters = json.decodeFromString<Filters>(it[3] as String)
            ) {
                is WallhavenFilters -> WallhavenSearch(
                    query = it[2] as String,
                    filters = filters,
                )
                is RedditFilters -> RedditSearch(
                    query = it[2] as String,
                    filters = filters,
                )
            },
        )
    },
)
