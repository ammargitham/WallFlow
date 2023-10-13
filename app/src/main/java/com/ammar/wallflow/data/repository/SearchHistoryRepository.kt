package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.wallhaven.SearchHistoryDao
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.toSearchHistoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun getAll() = searchHistoryDao.getAll().flowOn(ioDispatcher)

    suspend fun addSearch(search: Search) = withContext(ioDispatcher) {
        val lastUpdatedOn = Clock.System.now()
        val searchHistory = searchHistoryDao.getByQuery(search.query)?.copy(
            filters = json.encodeToString(search.filters),
            lastUpdatedOn = lastUpdatedOn,
        ) ?: search.toSearchHistoryEntity(lastUpdatedOn = lastUpdatedOn)
        searchHistoryDao.upsert(searchHistory)
    }

    suspend fun deleteSearch(search: Search) = withContext(ioDispatcher) {
        searchHistoryDao.deleteByQuery(search.query)
    }
}
