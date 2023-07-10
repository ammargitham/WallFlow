package com.ammar.wallflow.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

val IntSize.aspectRatio
    get() = if (height != 0) width.toFloat() / height else 0F

fun Rect.constrainOffset(bounds: Rect): Rect {
    var (x, y) = topLeft
    if (right > bounds.right) x += bounds.right - right
    if (bottom > bounds.bottom) y += bounds.bottom - bottom
    if (x < bounds.left) x += bounds.left - x
    if (y < bounds.top) y += bounds.top - y
    return Rect(Offset(x, y), size)
}
