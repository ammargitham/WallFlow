package com.ammar.wallflow.extensions

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.roundToInt

fun Bitmap.getRegion(rectF: RectF): Bitmap {
    var x = rectF.left.roundToInt().coerceAtLeast(0)
    var y = rectF.top.roundToInt().coerceAtLeast(0)
    var width = rectF.width().roundToInt().coerceIn(1, this.width)
    var height = rectF.height().roundToInt().coerceIn(1, this.height)
    if (x + width > this.width) {
        x = 0
        width = this.width
    }
    if (y + height > this.height) {
        y = 0
        height = this.height
    }
    return Bitmap.createBitmap(this, x, y, width, height)
}
