package com.ammar.havenwalls.workers

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.db.database.AppDatabase
import com.ammar.havenwalls.data.db.entity.WallpaperEntity
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.getFileNameFromUrl
import com.ammar.havenwalls.extensions.getTempDir
import com.ammar.havenwalls.extensions.workManager
import com.ammar.havenwalls.utils.NotificationChannels
import com.ammar.havenwalls.utils.NotificationIds.CLEANUP_NOTIFICATION_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val clock: Clock,
    private val appDatabase: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(
    context,
    params,
) {
    private val remoteKeysDao by lazy { appDatabase.searchQueryRemoteKeysDao() }
    private val wallpapersDao by lazy { appDatabase.wallpapersDao() }
    private val searchQueryWallpapersDao by lazy { appDatabase.searchQueryWallpapersDao() }
    private val searchQueryDao by lazy { appDatabase.searchQueryDao() }
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(context, NotificationChannels.CLEANUP_CHANNEL_ID).apply {
            setContentTitle(context.getString(R.string.cache_clean_up))
            setContentText(context.getString(R.string.running))
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setOngoing(true)
            setSilent(true)
            priority = NotificationCompat.PRIORITY_LOW
        }
    }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        CLEANUP_NOTIFICATION_ID,
        notificationBuilder.apply {
            setProgress(0, 0, true)
        }.build()
    )

    override suspend fun doWork() = try {
        withContext(ioDispatcher) {
            appDatabase.withTransaction {
                val now = clock.now()
                val cutOff = now - 7.days
                cleanupOldSearchQueries(cutOff)
                deleteOldTempFiles(cutOff)
                Result.success()
            }
        }
    } catch (e: Exception) {
        Result.failure()
    }

    private suspend fun cleanupOldSearchQueries(cutOff: Instant) {
        val ids = searchQueryDao.getAllIdsOlderThan(cutOff)
        val deletedWallpapers = mutableSetOf<WallpaperEntity>()
        ids.forEach { deletedWallpapers.addAll(cleanupSearchQueryId(it)) }
        val deleteFileNamesFromTemp = deletedWallpapers.map { it.path.getFileNameFromUrl() }
        val tempDir = context.getTempDir()
        deleteFileNamesFromTemp.forEach { File(tempDir, it).delete() }
    }

    private suspend fun cleanupSearchQueryId(searchQueryId: Long): List<WallpaperEntity> {
        remoteKeysDao.deleteBySearchQueryId(searchQueryId)
        val wallpapersToDelete = wallpapersDao.getAllUniqueToSearchQueryId(searchQueryId)
        wallpapersDao.deleteAllUniqueToSearchQueryId(searchQueryId)
        searchQueryWallpapersDao.deleteBySearchQueryId(searchQueryId)
        searchQueryDao.deleteById(searchQueryId)
        return wallpapersToDelete
    }

    private fun deleteOldTempFiles(cutOff: Instant) {
        context.getTempDir().listFiles()?.forEach {
            if (it.lastModified() > cutOff.toEpochMilliseconds()) {
                return@forEach
            }
            it.delete()
        }
    }

    companion object {
        private const val WORK_NAME = "cleanup"

        suspend fun checkIfScheduled(context: Context) = try {
            val state = context.workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .firstOrNull()
                ?.firstOrNull<WorkInfo?>()
                ?.state
            state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
        } catch (e: Exception) {
            Log.e(TAG, "checkScheduled: ", e)
            false
        }

        fun schedule(context: Context) {
            Log.i(TAG, "Scheduling cleanup worker...")
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(
                24,
                TimeUnit.HOURS,
            ).apply {
                setConstraints(
                    Constraints.Builder().apply {
                        setRequiresCharging(true)
                    }.build()
                )
            }.build()
            context.workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }
    }
}
