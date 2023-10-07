package com.ammar.wallflow.data.network.model.wallhaven

import com.ammar.wallflow.data.network.model.serializers.NetworkMetaQuerySerializer
import kotlinx.serialization.Serializable

interface NetworkWallhavenMetaQuery

@Suppress("PropertyName")
@Serializable
data class NetworkWallhavenMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int,
    @Serializable(NetworkMetaQuerySerializer::class)
    val query: NetworkWallhavenMetaQuery,
    val seed: String? = null,
)

@JvmInline
@Serializable
value class StringNetworkWallhavenMetaQuery(val value: String) : NetworkWallhavenMetaQuery

@Serializable
data class TagNetworkWallhavenMetaQuery(
    val id: Long,
    val tag: String,
) : NetworkWallhavenMetaQuery
