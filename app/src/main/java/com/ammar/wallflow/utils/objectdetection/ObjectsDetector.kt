package com.ammar.wallflow.utils.objectdetection

import android.graphics.Bitmap
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.model.DetectionWithBitmap
import java.io.File

interface ObjectsDetector {
    val isEnabled: Boolean

    fun detectObjects(
        objectDetectionPreferences: ObjectDetectionPreferences,
        model: File,
        bitmap: Bitmap,
    ): List<DetectionWithBitmap>

    fun validateModelFile(
        model: File,
    ): Boolean
}
