package com.ammar.wallflow.model

import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.data.db.entity.SavedSearchEntity
import com.ammar.wallflow.model.wallhaven.WallhavenSearchQuery

data class SavedSearch(
    val id: Long = 0,
    val name: String = "",
    val search: WallhavenSearch = WallhavenSearch(),
)

fun SavedSearch.toEntity(id: Long? = null) = SavedSearchEntity(
    id = id ?: this.id,
    name = name,
    query = search.query,
    filters = search.filters.toQueryString(),
)

val SavedWallhavenSearchSaver = Saver<SavedSearch, List<Any>>(
    save = { listOf(it.id, it.name, it.search.query, it.search.filters.toQueryString()) },
    restore = {
        SavedSearch(
            id = it[0] as Long,
            name = it[1] as String,
            search = WallhavenSearch(
                query = it[2] as String,
                filters = WallhavenSearchQuery.fromQueryString(it[3] as String),
            ),
        )
    },
)
