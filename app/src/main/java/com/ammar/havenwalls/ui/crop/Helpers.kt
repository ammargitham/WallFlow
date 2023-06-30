package com.ammar.havenwalls.ui.crop

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.times
import com.ammar.havenwalls.extensions.aspectRatio

fun getMaxCropSize(
    screenResolution: IntSize,
    imageSize: Size,
): Size {
    val imageAspectRatio = imageSize.width / imageSize.height
    val deviceAspectRatio = screenResolution.aspectRatio
    return when {
        imageAspectRatio == deviceAspectRatio -> Size(
            imageSize.width,
            imageSize.height,
        )
        imageAspectRatio < deviceAspectRatio -> Size(
            imageSize.width,
            imageSize.width / deviceAspectRatio,
        )
        else -> Size(
            imageSize.height * deviceAspectRatio,
            imageSize.height,
        )
    }
}

fun getCropRect(
    maxCropSize: Size,
    detectionRect: RectF? = null,
    detectedRectScale: Int? = null,
    imageSize: Size,
    cropScale: Float,
): Rect {
    val center = if (detectionRect != null && detectedRectScale != null) {
        val rectF = detectionRect * detectedRectScale * cropScale
        Offset(rectF.centerX(), rectF.centerY())
    } else {
        imageSize.center
    }
    val left = (center.x - (maxCropSize.width / 2)).coerceAtLeast(0f)
    val top = (center.y - (maxCropSize.height / 2)).coerceAtLeast(0f)
    return Rect(
        offset = Offset(left, top),
        size = maxCropSize,
    )
}
