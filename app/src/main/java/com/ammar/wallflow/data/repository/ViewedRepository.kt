package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.ViewedDao
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.model.Source
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@Singleton
class ViewedRepository @Inject constructor(
    private val viewedDao: ViewedDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll() = viewedDao.observeAll()

    suspend fun upsert(
        sourceId: String,
        source: Source,
    ) = withContext(ioDispatcher) {
        val existing = viewedDao.getBySourceIdAndSource(
            sourceId = sourceId,
            source = source,
        )
        val lastViewedOn = Clock.System.now()
        val updated = existing?.copy(
            lastViewedOn = lastViewedOn,
        ) ?: ViewedEntity(
            id = 0,
            sourceId = sourceId,
            source = source,
            lastViewedOn = lastViewedOn,
        )
        viewedDao.upsert(updated)
    }

    suspend fun deleteAll() = viewedDao.deleteAll()

    suspend fun insertEntities(entities: Collection<ViewedEntity>) = withContext(ioDispatcher) {
        val existing = viewedDao.getAll()
        val existingMap = existing.associateBy { (it.source to it.sourceId) }
        val insert = entities.filter {
            // only take non-existing
            existingMap[(it.source to it.sourceId)] == null
        }.map {
            // reset id
            it.copy(id = 0)
        }
        viewedDao.insertAll(insert)
    }
}
