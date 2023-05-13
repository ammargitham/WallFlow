package com.ammar.havenwalls.data.repository

import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.data.db.dao.SearchHistoryDao
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.toSearchHistoryEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun getAll() = searchHistoryDao.getAll().flowOn(ioDispatcher)

    suspend fun addSearch(search: Search) = withContext(ioDispatcher) {
        val lastUpdatedOn = Clock.System.now()
        val searchHistory = searchHistoryDao.getByQuery(search.query)?.copy(
            filters = search.filters.toQueryString(),
            lastUpdatedOn = lastUpdatedOn,
        ) ?: search.toSearchHistoryEntity(lastUpdatedOn = lastUpdatedOn)
        searchHistoryDao.upsert(searchHistory)
    }

    suspend fun deleteSearch(search: Search) = withContext(ioDispatcher) {
        searchHistoryDao.deleteByQuery(search.query)
    }
}
