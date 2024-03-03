package com.ammar.wallflow.utils.objectdetection

import android.graphics.Bitmap
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.extensions.getRegion
import com.ammar.wallflow.model.DetectionCategory
import com.ammar.wallflow.model.DetectionWithBitmap
import java.io.File
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectsDetectorImpl : ObjectsDetector {
    override val isEnabled: Boolean
        get() = true

    override fun detectObjects(
        objectDetectionPreferences: ObjectDetectionPreferences,
        model: File,
        bitmap: Bitmap,
    ): List<DetectionWithBitmap> {
        val objectDetectorOptions by lazy {
            val baseOptions = BaseOptions.builder().apply {
                when (objectDetectionPreferences.delegate) {
                    ObjectDetectionDelegate.NONE -> {}
                    ObjectDetectionDelegate.NNAPI -> useNnapi()
                    ObjectDetectionDelegate.GPU -> useGpu()
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
        val tensorImage = TensorImage.fromBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true))
        val detections = objectDetector.detect(tensorImage)
        val detectionWithBitmaps = detections.map {
            DetectionWithBitmap(
                detection = it.toDetection(),
                bitmap = bitmap.getRegion(it.boundingBox),
            )
        }
        return detectionWithBitmaps
    }

    override fun validateModelFile(model: File): Boolean {
        val objectDetectorOptions = ObjectDetector.ObjectDetectorOptions.builder().apply {
            setBaseOptions(BaseOptions.builder().build())
            setMaxResults(5)
        }.build()
        var objectDetector: ObjectDetector? = null
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                model,
                objectDetectorOptions,
            )
        } finally {
            objectDetector?.close()
        }
        return true
    }
}

fun Detection.toDetection() = com.ammar.wallflow.model.Detection(
    categories = categories?.map {
        DetectionCategory(
            index = it.index,
            label = it.label,
            displayName = it.displayName,
            score = it.score,
        )
    } ?: emptyList(),
    boundingBox = boundingBox,
)
