package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.model.AutoWallpaperHistory
import com.ammar.wallflow.model.toEntity
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
