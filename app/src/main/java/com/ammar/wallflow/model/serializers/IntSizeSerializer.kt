package com.ammar.wallflow.model.serializers

import androidx.compose.ui.unit.IntSize
import com.ammar.wallflow.data.db.converters.Converters
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IntSizeSerializer : KSerializer<IntSize> {
    override fun serialize(encoder: Encoder, value: IntSize) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = Converters.fromIntSizeStr(decoder.decodeString())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "IntSize",
        kind = PrimitiveKind.STRING,
    )
}
