package com.ammar.wallflow.model.search

import android.content.Context
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.quoteIfSpaced
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
@SerialName("WallhavenSearch")
data class WallhavenSearch(
    override val query: String = "",
    override val filters: WallhavenFilters = WallhavenFilters(),
    override val meta: WallhavenSearchMeta? = null,
) : Search() {
    fun toJson() = json.encodeToString(this)

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
        fun fromJson(string: String): WallhavenSearch = json.decodeFromString(string)
    }
}

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

@Suppress("DEPRECATION")
fun migrateWallhavenFiltersQSToWallhavenSearchJson(
    filtersStr: String,
) = json.encodeToString(
    WallhavenSearch(
        filters = WallhavenFilters.fromQueryString(filtersStr),
    ),
)

@Suppress("DEPRECATION")
fun migrateWallhavenFiltersQSToWallhavenFiltersJson(
    filtersStr: String,
) = json.encodeToString(
    WallhavenFilters.fromQueryString(filtersStr),
)
