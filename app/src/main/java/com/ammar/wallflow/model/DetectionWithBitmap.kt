package com.ammar.wallflow.model

import android.graphics.Bitmap
import android.graphics.RectF

data class DetectionCategory(
    val index: Int,
    val label: String,
    val displayName: String,
    val score: Float,
)

data class Detection(
    val categories: List<DetectionCategory>,
    val boundingBox: RectF,
) {
    companion object {
        val EMPTY = Detection(
            categories = emptyList(),
            boundingBox = RectF(0f, 0f, 0f, 0f),
        )
    }
}

data class DetectionWithBitmap(
    val detection: Detection,
    val bitmap: Bitmap,
)
