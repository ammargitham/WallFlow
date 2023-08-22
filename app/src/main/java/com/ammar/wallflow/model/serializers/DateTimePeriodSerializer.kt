package com.ammar.wallflow.model.serializers

import kotlinx.datetime.DateTimePeriod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateTimePeriodSerializer : KSerializer<DateTimePeriod> {
    override fun serialize(encoder: Encoder, value: DateTimePeriod) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = DateTimePeriod.parse(decoder.decodeString())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "DateTimePeriod",
        kind = PrimitiveKind.STRING,
    )
}
