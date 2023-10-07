package com.ammar.wallflow.data.network.model.serializers

import kotlinx.serialization.json.JsonContentPolymorphicSerializer as PolymorphicSerializer
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.model.wallhaven.StringNetworkWallhavenMetaQuery
import com.ammar.wallflow.data.network.model.wallhaven.TagNetworkWallhavenMetaQuery
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

object NetworkWallhavenMetaQuerySerializer : PolymorphicSerializer<NetworkWallhavenMetaQuery>(
    NetworkWallhavenMetaQuery::class,
) {
    override fun selectDeserializer(element: JsonElement) = when (element) {
        is JsonPrimitive -> StringNetworkWallhavenMetaQuery.serializer()
        else -> TagNetworkWallhavenMetaQuery.serializer()
    }
}
