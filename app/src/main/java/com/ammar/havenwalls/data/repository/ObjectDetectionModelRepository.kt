package com.ammar.havenwalls.data.repository

import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.data.db.dao.ObjectDetectionModelDao
import com.ammar.havenwalls.data.db.entity.ObjectDetectionModelEntity
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.model.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class ObjectDetectionModelRepository @Inject constructor(
    private val objectDetectionModelDao: ObjectDetectionModelDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun getAll() = objectDetectionModelDao.getAll().flowOn(ioDispatcher)

    suspend fun addOrUpdate(entity: ObjectDetectionModelEntity) = withContext(ioDispatcher) {
        objectDetectionModelDao.upsert(entity)
    }

    suspend fun addOrUpdateModel(objectDetectionModel: ObjectDetectionModel) =
        withContext(ioDispatcher) {
            val entity = objectDetectionModelDao.getByName(objectDetectionModel.name)?.copy(
                fileName = objectDetectionModel.fileName,
                url = objectDetectionModel.url,
            ) ?: objectDetectionModel.toEntity()
            objectDetectionModelDao.upsert(entity)
        }

    suspend fun delete(entity: ObjectDetectionModelEntity) = withContext(ioDispatcher) {
        objectDetectionModelDao.delete(entity)
    }

    suspend fun nameExists(name: String) = objectDetectionModelDao.nameExists(name)

    suspend fun nameExistsExcludingId(id: Long, name: String) =
        objectDetectionModelDao.nameExistsExcludingId(id, name)

    suspend fun getById(id: Long) = withContext(ioDispatcher) {
        objectDetectionModelDao.getById(id)
    }

    suspend fun getByName(name: String) = withContext(ioDispatcher) {
        objectDetectionModelDao.getByName(name)
    }
}
