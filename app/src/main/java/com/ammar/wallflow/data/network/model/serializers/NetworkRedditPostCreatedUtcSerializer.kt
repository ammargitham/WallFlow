package com.ammar.wallflow.data.network.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NetworkRedditPostCreatedUtcSerializer : KSerializer<Long> {
    override fun deserialize(decoder: Decoder) = decoder.decodeFloat().toLong()

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeFloat(value.toFloat())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "NetworkRedditPostCreatedUtcSerializer",
        kind = PrimitiveKind.FLOAT,
    )
}
