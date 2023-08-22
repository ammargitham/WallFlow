package com.ammar.wallflow.model.serializers

import androidx.work.Constraints
import com.ammar.wallflow.extensions.toConstraintTypeMap
import com.ammar.wallflow.extensions.toConstraints
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ConstraintsSerializer : KSerializer<Constraints> {
    override fun serialize(encoder: Encoder, value: Constraints) = encoder.encodeSerializableValue(
        constraintTypeMapSerializer,
        value.toConstraintTypeMap(),
    )

    override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(
        constraintTypeMapSerializer,
    ).toConstraints()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "Constraints",
        kind = PrimitiveKind.STRING,
    )
}
