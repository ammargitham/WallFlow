package com.ammar.wallflow.model

import com.ammar.wallflow.EFFICIENT_DET_LITE_0_MODEL_FILE_NAME
import com.ammar.wallflow.EFFICIENT_DET_LITE_0_MODEL_NAME
import com.ammar.wallflow.EFFICIENT_DET_LITE_0_MODEL_URL
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity

data class ObjectDetectionModel(
    val name: String,
    val fileName: String,
    val url: String,
) {
    companion object {
        val DEFAULT = ObjectDetectionModel(
            name = EFFICIENT_DET_LITE_0_MODEL_NAME,
            fileName = EFFICIENT_DET_LITE_0_MODEL_FILE_NAME,
            url = EFFICIENT_DET_LITE_0_MODEL_URL,
        )
    }
}

fun ObjectDetectionModel.toEntity(id: Long = 0) = ObjectDetectionModelEntity(
    id = id,
    name = name,
    fileName = fileName,
    url = url,
)
