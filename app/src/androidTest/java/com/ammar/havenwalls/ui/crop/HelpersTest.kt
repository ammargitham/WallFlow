package com.ammar.havenwalls.ui.crop

import android.graphics.RectF
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.times
import org.junit.Test
import kotlin.test.assertEquals

class HelpersTest {
    @Test
    fun shouldCalculateCorrectCropRect() {
        val deviceSize = IntSize(500, 1000)
        val imageSize = IntSize(1280, 768)
        val maxCropSize = getMaxCropSize(
            screenResolution = deviceSize,
            imageSize = imageSize.toSize(),
        )
        val cropRect = getCropRect(
            maxCropSize = maxCropSize,
            imageSize = imageSize.toSize(),
            cropScale = 1f,
        )
        assertEquals(
            Rect(448f, 0f, 832f, 768f),
            cropRect,
        )
    }

    @Test
    fun shouldCalculateCorrectCropRectAroundDetection() {
        val deviceSize = IntSize(500, 1000)
        val imageSize = IntSize(1280, 768)
        val maxCropSize = getMaxCropSize(
            screenResolution = deviceSize,
            imageSize = imageSize.toSize(),
        )
        val detectionScale = 2
        val detectionRect = RectF(110f, 340f, 210f, 368f) * (1 / detectionScale)
        val cropRect = getCropRect(
            maxCropSize = maxCropSize,
            imageSize = imageSize.toSize(),
            cropScale = 1f,
            detectionRect = detectionRect,
            detectedRectScale = detectionScale,
        )
        assertEquals(
            Rect(0f, 0f, 384f, 768f),
            cropRect,
        )
    }
}
