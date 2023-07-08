package com.ammar.wallflow.data.network.model.util

import com.ammar.wallflow.data.network.model.NetworkMetaQuery
import com.ammar.wallflow.data.network.model.StringNetworkMetaQuery
import com.ammar.wallflow.data.network.model.TagNetworkMetaQuery
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

object NetworkMetaQuerySerializer : JsonContentPolymorphicSerializer<NetworkMetaQuery>(
    NetworkMetaQuery::class,
) {
    override fun selectDeserializer(element: JsonElement) = when (element) {
        is JsonPrimitive -> StringNetworkMetaQuery.serializer()
        else -> TagNetworkMetaQuery.serializer()
    }
}
