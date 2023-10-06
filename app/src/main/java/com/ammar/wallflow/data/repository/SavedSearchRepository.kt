package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenSavedSearchDao
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSavedSearchEntity
import com.ammar.wallflow.model.search.WallhavenSavedSearch
import com.ammar.wallflow.model.search.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class SavedSearchRepository @Inject constructor(
    private val savedSearchDao: WallhavenSavedSearchDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll() = savedSearchDao.observeAll().flowOn(ioDispatcher)

    suspend fun getById(id: Long) = withContext(ioDispatcher) {
        savedSearchDao.getById(id)
    }

    suspend fun upsert(savedSearch: WallhavenSavedSearch) = withContext(ioDispatcher) {
        val existing = if (savedSearch.id != 0L) {
            savedSearchDao.getById(savedSearch.id)
        } else {
            savedSearchDao.getByName(savedSearch.name)
        }
        val entity = updateExisting(existing, savedSearch)
        savedSearchDao.upsert(entity)
    }

    suspend fun upsertAll(
        savedSearches: Collection<WallhavenSavedSearch>,
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
        existing: WallhavenSavedSearchEntity?,
        savedSearch: WallhavenSavedSearch,
    ) = existing?.copy(
        name = savedSearch.name,
        query = savedSearch.search.query,
        filters = savedSearch.search.filters.toQueryString(),
    ) ?: savedSearch.toEntity(0)

    suspend fun delete(savedSearch: WallhavenSavedSearch) = withContext(ioDispatcher) {
        savedSearchDao.deleteByName(savedSearch.name)
    }
}
