package com.ammar.havenwalls.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.times
import com.ammar.havenwalls.extensions.aspectRatio
import com.ammar.havenwalls.model.DetectionWithBitmap

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
    detectionWithBitmap: DetectionWithBitmap?,
    detectedRectScale: Int,
    imageSize: Size,
    cropScale: Float,
): Rect {
    val detectionRect = detectionWithBitmap?.detection?.boundingBox
    val center = if (detectionRect != null) {
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
