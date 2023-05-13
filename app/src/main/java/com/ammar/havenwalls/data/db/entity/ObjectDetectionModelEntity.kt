package com.ammar.havenwalls.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ammar.havenwalls.model.ObjectDetectionModel

@Entity(
    tableName = "object_detection_models",
    indices = [
        Index(
            value = ["name"],
            unique = true,
        )
    ]
)
data class ObjectDetectionModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    val url: String,
)

fun ObjectDetectionModelEntity.toModel() = ObjectDetectionModel(
    name = name,
    fileName = fileName,
    url = url,
)
