package com.ammar.wallflow.utils.objectdetection

import android.graphics.Bitmap
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.model.DetectionWithBitmap
import java.io.File

class ObjectsDetectorImpl : ObjectsDetector {
    override val isEnabled: Boolean
        get() = false

    override fun detectObjects(
        objectDetectionPreferences: ObjectDetectionPreferences,
        model: File,
        bitmap: Bitmap,
    ): List<DetectionWithBitmap> = emptyList()

    override fun validateModelFile(model: File): Boolean {
        throw IllegalStateException("Objection detection not available")
    }
}
