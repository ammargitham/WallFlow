package com.ammar.havenwalls.extensions

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt

fun Color.toHexString() = String.format("#%06X", 0xFFFFFF and this.toArgb())

val Color.Companion.DELETE
    get() = Color("#d9534f".toColorInt())

@Composable
fun Dp.toPx() = with(LocalDensity.current) { roundToPx() }

@Composable
fun Dp.toPxF() = with(LocalDensity.current) { toPx() }

fun Modifier.ignoreHorizontalParentPadding(horizontal: Dp) =
    this.layout { measurable, constraints ->
        val overriddenWidth = constraints.maxWidth + 2 * horizontal.roundToPx()
        val placeable = measurable.measure(constraints.copy(maxWidth = overriddenWidth))
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

fun Modifier.ignoreVerticalParentPadding(vertical: Dp, oneSide: Boolean = false) =
    this.layout { measurable, constraints ->
        val addingHeight = (if (oneSide) 1 else 2) * vertical.roundToPx()
        val overriddenHeight = constraints.maxHeight + addingHeight
        val newConstraints = constraints.copy(maxHeight = overriddenHeight)
        Log.d(
            TAG,
            "ignoreVerticalParentPadding: before: ${constraints.maxHeight}, adding: $addingHeight, after: ${newConstraints.maxHeight}"
        )
        val placeable = measurable.measure(newConstraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

val IntSize.aspectRatio
    get() = if (height != 0) width.toFloat() / height else 0F
