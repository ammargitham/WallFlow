package com.ammar.havenwalls.model.serializers

import com.ammar.havenwalls.model.ConstraintType
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer

val constraintTypeMapSerializer = MapSerializer<ConstraintType, Boolean>(
    keySerializer = serializer(),
    valueSerializer = serializer(),
)
