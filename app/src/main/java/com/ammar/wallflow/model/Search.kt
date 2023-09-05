package com.ammar.wallflow.model

import android.content.Context
import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.SearchHistoryEntity
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed class SearchMeta

@Serializable
data class Search(
    val query: String = "",
    val filters: SearchQuery = SearchQuery(),
    val meta: SearchMeta? = null,
)

@Serializable
data class TagSearchMeta(
    val wallhavenTag: WallhavenTag,
) : SearchMeta()

@Serializable
data class UploaderSearchMeta(
    val wallhavenUploader: WallhavenUploader,
) : SearchMeta()

fun Search.toSearchHistoryEntity(
    id: Long = 0,
    lastUpdatedOn: Instant,
) = SearchHistoryEntity(
    id = id,
    query = query.trimAll().lowercase(),
    filters = filters.toQueryString(),
    lastUpdatedOn = lastUpdatedOn,
)

fun Search.toSearchQuery(): SearchQuery {
    if (query.isBlank()) return filters
    val q = query.trimAll()
    if (q.startsWith("id:")) {
        val tagIdString = q.removePrefix("id:")
        val tagId = tagIdString.toLongOrNull() ?: return filters
        return filters.copy(
            tagId = tagId,
        )
    }
    if (q.startsWith("like:")) {
        val wallpaperId = q.removePrefix("like:")
        if (wallpaperId.isBlank()) return filters
        return filters.copy(
            wallpaperId = wallpaperId,
        )
    }
    if (q.startsWith("@")) {
        val username = q.removePrefix("@")
        if (username.isBlank()) return filters
        return filters.copy(
            username = username,
        )
    }
    return filters.copy(
        includedTags = filters.includedTags + q,
    )
}

fun Search.getSupportingText(
    context: Context,
) = mutableListOf<String>().apply {
    if (filters.includedTags.isNotEmpty()) {
        add(
            context.getString(
                R.string.included_tags_supp,
                filters.includedTags.joinToString(", ") { "#$it" },
            ),
        )
    }
    if (filters.excludedTags.isNotEmpty()) {
        add(
            context.getString(
                R.string.excluded_tags_supp,
                filters.excludedTags.joinToString(", ") { "#$it" },
            ),
        )
    }
    if (filters.categories != SearchQuery.defaultCategories) {
        add(
            context.getString(
                R.string.categories_supp,
                filters.categories.joinToString(", ") { it.value },
            ),
        )
    }
    if (filters.purity != SearchQuery.defaultPurities) {
        add(
            context.getString(
                R.string.purities_supp,
                filters.purity.joinToString(", ") { it.purityName },
            ),
        )
    }
    filters.username?.run {
        if (isNotBlank()) {
            add(context.getString(R.string.username_supp, "@$this"))
        }
    }
    filters.tagId?.run {
        add(context.getString(R.string.tag_id_supp, this.toString()))
    }
    filters.wallpaperId?.run {
        add(context.getString(R.string.wallpaper_id_supp, this))
    }
}.joinToString(", ").ifBlank { null }

val SearchSaver = Saver<Search, List<String>>(
    save = { listOf(it.query, it.filters.toQueryString()) },
    restore = {
        Search(
            query = it[0],
            filters = SearchQuery.fromQueryString(it[1]),
        )
    },
)
