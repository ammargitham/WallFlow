package com.ammar.wallflow.model.serializers

import com.ammar.wallflow.model.ConstraintType
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer

val constraintTypeMapSerializer = MapSerializer<ConstraintType, Boolean>(
    keySerializer = serializer(),
    valueSerializer = serializer(),
)
