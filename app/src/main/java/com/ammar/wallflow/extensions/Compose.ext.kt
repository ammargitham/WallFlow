package com.ammar.wallflow.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.ammar.wallflow.R

fun Color.toHexString() = String.format("#%06X", 0xFFFFFF and this.toArgb())

val Color.Companion.DELETE
    get() = Color("#d9534f".toColorInt())

fun Color.blend(
    color: Color,
    ratio: Float = 0.2f,
) = Color(
    ColorUtils.blendARGB(
        toArgb(),
        color.toArgb(),
        ratio,
    ),
)

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

val IntSize.Companion.Saver
    get() = listSaver(
        save = {
            listOf(
                it.height,
                it.width,
            )
        },
        restore = {
            IntSize(
                height = it[0],
                width = it[1],
            )
        },
    )

fun <I, O> ManagedActivityResultLauncher<I, O>.safeLaunch(
    context: Context,
    input: I,
    options: ActivityOptionsCompat? = null,
) = try {
    launch(input, options)
} catch (e: ActivityNotFoundException) {
    context.toast(context.getString(R.string.target_activity_not_found))
}
