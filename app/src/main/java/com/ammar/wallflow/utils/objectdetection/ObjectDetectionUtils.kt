package com.ammar.wallflow.utils.objectdetection

import android.content.Context
import android.net.Uri
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.model.DetectionWithBitmap
import com.ammar.wallflow.utils.decodeSampledBitmapFromUri
import java.io.File

val objectsDetector = ObjectsDetectorImpl()

fun detectObjects(
    context: Context,
    uri: Uri,
    model: File,
    objectDetectionPreferences: ObjectDetectionPreferences,
): Pair<Int, List<DetectionWithBitmap>> {
    val (bitmap, scale) = decodeSampledBitmapFromUri(context, uri) ?: return 1 to emptyList()
    val detections = objectsDetector.detectObjects(objectDetectionPreferences, model, bitmap)
    return scale to detections
}

fun validateModelFile(model: File) = objectsDetector.validateModelFile(model)
