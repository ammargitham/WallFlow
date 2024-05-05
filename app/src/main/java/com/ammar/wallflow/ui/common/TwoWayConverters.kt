package com.ammar.wallflow.ui.common

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

fun getPaddingValuesConverter(layoutDirection: LayoutDirection) =
    TwoWayConverter(
        convertToVector = { paddingValues: PaddingValues ->
            AnimationVector4D(
                paddingValues.calculateTopPadding().value,
                paddingValues.calculateBottomPadding().value,
                paddingValues.calculateStartPadding(layoutDirection).value,
                paddingValues.calculateEndPadding(layoutDirection).value,
            )
        },
        convertFromVector = { vector: AnimationVector4D ->
            PaddingValues(
                top = vector.v1.coerceAtLeast(0f).dp,
                bottom = vector.v2.coerceAtLeast(0f).dp,
                start = vector.v3.coerceAtLeast(0f).dp,
                end = vector.v4.coerceAtLeast(0f).dp,
            )
        },
    )

fun getOffsetConverter() = TwoWayConverter(
    convertToVector = { offset: Offset -> AnimationVector2D(offset.x, offset.y) },
    convertFromVector = { vector: AnimationVector2D -> Offset(vector.v1, vector.v2) },
)
