package com.ammar.wallflow.data.network.model

import com.ammar.wallflow.data.network.model.util.NetworkMetaQuerySerializer
import kotlinx.serialization.Serializable

interface NetworkMetaQuery

@Serializable
data class NetworkMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int,
    @Serializable(NetworkMetaQuerySerializer::class)
    val query: NetworkMetaQuery,
    val seed: String? = null,
)

@JvmInline
@Serializable
value class StringNetworkMetaQuery(val value: String) : NetworkMetaQuery

@Serializable
data class TagNetworkMetaQuery(
    val id: Long,
    val tag: String,
) : NetworkMetaQuery
