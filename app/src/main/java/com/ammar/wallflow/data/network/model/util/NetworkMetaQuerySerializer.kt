package com.ammar.wallflow.data.network.model.util

import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.model.wallhaven.StringNetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.model.wallhaven.TagNetworkWallhavenMetaQuery
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

object NetworkMetaQuerySerializer : JsonContentPolymorphicSerializer<NetworkWallhavenMetaQuery>(
    NetworkWallhavenMetaQuery::class,
) {
    override fun selectDeserializer(element: JsonElement) = when (element) {
        is JsonPrimitive -> StringNetworkWallhavenMetaQuery.serializer()
        else -> TagNetworkWallhavenMetaQuery.serializer()
    }
}
