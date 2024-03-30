package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.model.AutoWallpaperHistory
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.toEntity
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.SourceChoice
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

    suspend fun getAllBySource(source: Source) = withContext(ioDispatcher) {
        autoWallpaperHistoryDao.getAllBySource(source).map { it.toModel() }
    }

    suspend fun getAllBySourceChoice(sourceChoice: SourceChoice) = withContext(ioDispatcher) {
        autoWallpaperHistoryDao.getAllBySourceChoice(sourceChoice).map { it.toModel() }
    }

    suspend fun getAllSourceIdsBySourceChoice(
        sourceChoice: SourceChoice,
    ) = withContext(ioDispatcher) {
        autoWallpaperHistoryDao.getAllSourceIdsBySourceChoice(sourceChoice)
    }

    suspend fun getOldestSetOnSourceIdBySourceChoice(
        sourceChoice: SourceChoice,
    ) = withContext(ioDispatcher) {
        autoWallpaperHistoryDao.getOldestSetOnSourceIdBySourceChoice(sourceChoice)
    }

    suspend fun addHistory(
        autoWallpaperHistory: AutoWallpaperHistory,
    ) = withContext(ioDispatcher) {
        autoWallpaperHistoryDao.upsert(autoWallpaperHistory.toEntity())
    }
}
