package com.ammar.havenwalls.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.ammar.havenwalls.data.preferences.ObjectDetectionPreferences
import com.ammar.havenwalls.extensions.getRegion
import com.ammar.havenwalls.model.DetectionWithBitmap
import java.io.File
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.ComputeSettings
import org.tensorflow.lite.task.vision.detector.ObjectDetector

fun detectObjects(
    context: Context,
    uri: Uri,
    model: File,
    objectDetectionPreferences: ObjectDetectionPreferences,
): Pair<Int, List<DetectionWithBitmap>> {
    val objectDetectorOptions by lazy {
        val baseOptions = BaseOptions.builder().apply {
            when (objectDetectionPreferences.delegate) {
                ComputeSettings.Delegate.NONE -> {}
                ComputeSettings.Delegate.NNAPI -> useNnapi()
                ComputeSettings.Delegate.GPU -> useGpu()
            }
        }.build()
        ObjectDetector.ObjectDetectorOptions.builder().apply {
            setBaseOptions(baseOptions)
            setScoreThreshold(0.2f)
            setMaxResults(5)
        }.build()
    }
    val objectDetector = ObjectDetector.createFromFileAndOptions(
        model,
        objectDetectorOptions,
    )
    val (bitmap, scale) = decodeSampledBitmapFromUri(context, uri) ?: return 1 to emptyList()
    val tensorImage = TensorImage.fromBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true))
    val detections = objectDetector.detect(tensorImage)
    val detectionWithBitmaps = detections.map {
        DetectionWithBitmap(
            detection = it,
            bitmap = bitmap.getRegion(it.boundingBox),
        )
    }
    return scale to detectionWithBitmaps
}
