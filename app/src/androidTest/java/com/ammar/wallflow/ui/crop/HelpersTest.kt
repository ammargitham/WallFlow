package com.ammar.wallflow.ui.crop

import android.graphics.RectF
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.times
import kotlin.test.assertEquals
import org.junit.Test

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
        val deviceSize = IntSize(1080, 2340)
        val imageSize = IntSize(2047, 1335)
        val maxCropSize = getMaxCropSize(
            screenResolution = deviceSize,
            imageSize = imageSize.toSize(),
        )
        val detectionScale = 1
        val detectionRect = RectF(1746f, 978f, 2040f, 1292f) * (1 / detectionScale)
        val cropRect = getCropRect(
            maxCropSize = maxCropSize,
            imageSize = imageSize.toSize(),
            cropScale = 1f,
            detectionRect = detectionRect,
            detectedRectScale = detectionScale,
        )
        val expected = Rect(1430.8462f, 0f, 2047f, 1335f)
        assertEquals(expected.left, cropRect.left)
        assertEquals(expected.right, cropRect.right)
        assertEquals(expected.top, cropRect.top)
        assertEquals(expected.bottom, cropRect.bottom)
    }
}
