package com.ammar.havenwalls.model.serializers

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.ammar.havenwalls.extensions.toHexString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorSerializer : KSerializer<Color> {
    override fun serialize(encoder: Encoder, value: Color) =
        encoder.encodeString(value.toHexString())

    override fun deserialize(decoder: Decoder) =
        Color(decoder.decodeString().toColorInt())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "Color",
        kind = PrimitiveKind.STRING
    )
}
