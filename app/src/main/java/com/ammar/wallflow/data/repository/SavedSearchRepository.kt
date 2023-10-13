package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@Singleton
class SavedSearchRepository @Inject constructor(
    private val savedSearchDao: SavedSearchDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll() = savedSearchDao.observeAll().flowOn(ioDispatcher)

    suspend fun getById(id: Long) = withContext(ioDispatcher) {
        savedSearchDao.getById(id)
    }

    suspend fun upsert(savedSearch: SavedSearch) = withContext(ioDispatcher) {
        val existing = if (savedSearch.id != 0L) {
            savedSearchDao.getById(savedSearch.id)
        } else {
            savedSearchDao.getByName(savedSearch.name)
        }
        val entity = updateExisting(existing, savedSearch)
        savedSearchDao.upsert(entity)
    }

    suspend fun upsertAll(
        savedSearches: Collection<SavedSearch>,
    ) = withContext(ioDispatcher) {
        val allNames = savedSearches.map { it.name }
        val existingEntities = savedSearchDao.getAllByNames(allNames)
        val existingMap = existingEntities.associateBy { it.name }
        val entities = savedSearches.map {
            updateExisting(existingMap[it.name], it)
        }
        savedSearchDao.upsert(entities)
    }

    private fun updateExisting(
        existing: SavedSearchEntity?,
        savedSearch: SavedSearch,
    ) = existing?.copy(
        name = savedSearch.name,
        query = savedSearch.search.query,
        filters = json.encodeToString(savedSearch.search.filters),
    ) ?: savedSearch.toEntity(0)

    suspend fun delete(savedSearch: SavedSearch) = withContext(ioDispatcher) {
        savedSearchDao.deleteByName(savedSearch.name)
    }
}
