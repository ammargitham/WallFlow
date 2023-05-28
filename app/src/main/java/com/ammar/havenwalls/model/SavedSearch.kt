package com.ammar.havenwalls.model

import androidx.compose.runtime.saveable.Saver
import com.ammar.havenwalls.data.db.entity.SavedSearchEntity

data class SavedSearch(
    val id: Long = 0,
    val name: String = "",
    val search: Search = Search(),
)

fun SavedSearch.toEntity(id: Long? = null) = SavedSearchEntity(
    id = id ?: this.id,
    name = name,
    query = search.query,
    filters = search.filters.toQueryString(),
)

val SavedSearchSaver = Saver<SavedSearch, List<Any>>(
    save = { listOf(it.id, it.name, it.search.query, it.search.filters.toQueryString()) },
    restore = {
        SavedSearch(
            id = it[0] as Long,
            name = it[1] as String,
            search = Search(
                query = it[2] as String,
                filters = SearchQuery.fromQueryString(it[3] as String),
            )
        )
    }
)
