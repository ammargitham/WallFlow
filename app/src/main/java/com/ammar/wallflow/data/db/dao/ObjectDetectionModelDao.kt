package com.ammar.wallflow.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectDetectionModelDao {
    @Query("SELECT * FROM object_detection_models")
    fun getAll(): Flow<List<ObjectDetectionModelEntity>>

    @Query("SELECT * FROM object_detection_models WHERE id=:id")
    suspend fun getById(id: Long): ObjectDetectionModelEntity?

    @Query("SELECT * FROM object_detection_models WHERE name=:name")
    suspend fun getByName(name: String): ObjectDetectionModelEntity?

    @Query("SELECT EXISTS(SELECT * FROM object_detection_models WHERE name=:name)")
    suspend fun nameExists(name: String): Boolean

    @Query(
        """
        SELECT EXISTS (
            SELECT *
            FROM object_detection_models
            WHERE
                id!=:id
                AND name=:name
        )
        """
    )
    suspend fun nameExistsExcludingId(id: Long, name: String): Boolean

    @Upsert
    suspend fun upsert(vararg objectDetectionModelEntity: ObjectDetectionModelEntity)

    @Query("DELETE FROM object_detection_models WHERE name=:name")
    suspend fun deleteByName(name: String)

    @Delete
    suspend fun delete(entity: ObjectDetectionModelEntity)
}
