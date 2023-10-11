package com.ammar.wallflow.model.search

import android.content.Context
import androidx.compose.runtime.saveable.Saver
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchHistoryEntity
import com.ammar.wallflow.extensions.quoteIfSpaced
import com.ammar.wallflow.extensions.trimAll
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WallhavenSearch(
    val query: String = "",
    val filters: WallhavenFilters = WallhavenFilters(),
    val meta: SearchMeta? = null,
) {
    fun toQueryString(
        backwardsCompat: Boolean = false,
    ): String {
        if (backwardsCompat) {
            return getQueryCombinedFilters().toQueryString()
        }
        return Json.encodeToString(this)
    }

    fun getApiQueryString() = with(getQueryCombinedFilters()) {
        ArrayList<String>().apply {
            val i = includedTags
                .filter { it.isNotBlank() }
                .joinToString(" ") { "+${it.quoteIfSpaced()}" }
            if (i.isNotBlank()) {
                add(i)
            }
            val e = excludedTags
                .filter { it.isNotBlank() }
                .joinToString(" ") { "-${it.quoteIfSpaced()}" }
            if (e.isNotBlank()) {
                add(e)
            }
            username?.run {
                if (this.isNotBlank()) {
                    this@apply.add("@$this")
                }
            }
            tagId?.run {
                this@apply.add("id:$this")
            }
            wallpaperId?.run {
                this@apply.add("like:$this")
            }
        }.joinToString(" ")
    }

    private fun getQueryCombinedFilters(): WallhavenFilters {
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

    companion object {
        fun fromQueryString(string: String) = try {
            Json.decodeFromString(string)
        } catch (e: Exception) {
            // for backwards compat, check if is a WallhavenFilters qs
            try {
                WallhavenSearch(
                    filters = WallhavenFilters.fromQueryString(string),
                )
            } catch (e: Exception) {
                // if not even WallhavenFilters qs return default
                WallhavenSearch()
            }
        }
    }
}

fun WallhavenSearch.toSearchHistoryEntity(
    id: Long = 0,
    lastUpdatedOn: Instant,
) = WallhavenSearchHistoryEntity(
    id = id,
    query = query.trimAll().lowercase(),
    filters = filters.toQueryString(),
    lastUpdatedOn = lastUpdatedOn,
)

fun WallhavenSearch.getSupportingText(
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
    if (filters.categories != WallhavenFilters.defaultCategories) {
        add(
            context.getString(
                R.string.categories_supp,
                filters.categories.joinToString(", ") { it.value },
            ),
        )
    }
    if (filters.purity != WallhavenFilters.defaultPurities) {
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

val WallhavenSearchSaver = Saver<WallhavenSearch, List<String>>(
    save = { listOf(it.query, it.filters.toQueryString()) },
    restore = {
        WallhavenSearch(
            query = it[0],
            filters = WallhavenFilters.fromQueryString(it[1]),
        )
    },
)

fun migrateWallhavenFiltersQSToWallhavenSearchJson(
    filtersStr: String,
) = Json.encodeToString(
    WallhavenSearch(
        filters = WallhavenFilters.fromQueryString(filtersStr),
    ),
)
