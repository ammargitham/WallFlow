package com.ammar.wallflow.model.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RedditSearch")
data class RedditSearch(
    override val query: String = "",
    override val filters: RedditFilters,
    override val meta: SearchMeta? = null,
) : Search()
