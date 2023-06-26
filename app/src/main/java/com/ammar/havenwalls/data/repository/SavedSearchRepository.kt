package com.ammar.havenwalls.data.repository

import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.data.db.dao.SavedSearchDao
import com.ammar.havenwalls.model.SavedSearch
import com.ammar.havenwalls.model.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class SavedSearchRepository @Inject constructor(
    private val savedSearchDao: SavedSearchDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun getAll() = savedSearchDao.getAll().flowOn(ioDispatcher)

    suspend fun getById(id: Long) = withContext(ioDispatcher) {
        savedSearchDao.getById(id)
    }

    suspend fun addOrUpdateSavedSearch(savedSearch: SavedSearch) = withContext(ioDispatcher) {
        val entity = if (savedSearch.id != 0L) {
            savedSearchDao.getById(savedSearch.id)
        } else {
            savedSearchDao.getByName(savedSearch.name)
        }?.copy(
            name = savedSearch.name,
            query = savedSearch.search.query,
            filters = savedSearch.search.filters.toQueryString(),
        ) ?: savedSearch.toEntity(0)
        savedSearchDao.upsert(entity)
    }

    suspend fun deleteSavedSearch(savedSearch: SavedSearch) = withContext(ioDispatcher) {
        savedSearchDao.deleteByName(savedSearch.name)
    }
}
