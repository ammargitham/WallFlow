package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.model.Source
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@Singleton
class LightDarkRepository @Inject constructor(
    private val lightDarkDao: LightDarkDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll() = lightDarkDao.observeAll()

    suspend fun upsert(
        sourceId: String,
        source: Source,
        typeFlags: Int,
    ) = withContext(ioDispatcher) {
        val existing = lightDarkDao.getBySourceIdAndSource(
            sourceId = sourceId,
            source = source,
        )
        val updatedOn = Clock.System.now()
        val updated = existing?.copy(
            updatedOn = updatedOn,
            typeFlags = typeFlags,
        ) ?: LightDarkEntity(
            id = 0,
            sourceId = sourceId,
            source = source,
            typeFlags = typeFlags,
            updatedOn = updatedOn,
        )
        lightDarkDao.upsert(updated)
    }

    fun observeIsFavorite(
        source: Source,
        sourceId: String,
    ) = lightDarkDao.observeTypeFlags(source = source, sourceId = sourceId)

    // suspend fun insertEntities(entities: Collection<ViewedEntity>) = withContext(ioDispatcher) {
    //     val existing = lightDarkDao.getAll()
    //     val existingMap = existing.associateBy { (it.source to it.sourceId) }
    //     val insert = entities.filter {
    //         // only take non-existing
    //         existingMap[(it.source to it.sourceId)] == null
    //     }.map {
    //         // reset id
    //         it.copy(id = 0)
    //     }
    //     lightDarkDao.insertAll(insert)
    // }
}
