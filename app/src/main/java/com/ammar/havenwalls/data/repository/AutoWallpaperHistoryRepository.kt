package com.ammar.havenwalls.data.repository

import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.havenwalls.data.db.entity.toModel
import com.ammar.havenwalls.model.AutoWallpaperHistory
import com.ammar.havenwalls.model.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AutoWallpaperHistoryRepository @Inject constructor(
    private val autoWallpaperHistoryDao: AutoWallpaperHistoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getAll() = withContext(ioDispatcher) {
        autoWallpaperHistoryDao.getAll().map { it.toModel() }
    }

    suspend fun addOrUpdateHistory(
        autoWallpaperHistory: AutoWallpaperHistory,
    ) = withContext(ioDispatcher) {
        val entity = autoWallpaperHistoryDao.getByWallhavenId(
            autoWallpaperHistory.wallhavenId
        )?.copy(
            setOn = autoWallpaperHistory.setOn,
        ) ?: autoWallpaperHistory.toEntity()
        autoWallpaperHistoryDao.upsert(entity)
    }
}
