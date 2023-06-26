package com.ammar.havenwalls.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.unit.toSize
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toRect
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.db.entity.toModel
import com.ammar.havenwalls.data.db.entity.toSavedSearch
import com.ammar.havenwalls.data.network.WallHavenNetworkDataSource
import com.ammar.havenwalls.data.network.model.asWallpaper
import com.ammar.havenwalls.data.preferences.AppPreferences
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.AutoWallpaperHistoryRepository
import com.ammar.havenwalls.data.repository.ObjectDetectionModelRepository
import com.ammar.havenwalls.data.repository.SavedSearchRepository
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.getFileNameFromUrl
import com.ammar.havenwalls.extensions.getMLModelsDir
import com.ammar.havenwalls.extensions.getMLModelsFileIfExists
import com.ammar.havenwalls.extensions.getScreenResolution
import com.ammar.havenwalls.extensions.getTempDir
import com.ammar.havenwalls.extensions.getTempFileIfExists
import com.ammar.havenwalls.extensions.getUriForFile
import com.ammar.havenwalls.extensions.notificationManager
import com.ammar.havenwalls.extensions.setWallpaper
import com.ammar.havenwalls.extensions.workManager
import com.ammar.havenwalls.model.AutoWallpaperHistory
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.model.Wallpaper
import com.ammar.havenwalls.model.toSearchQuery
import com.ammar.havenwalls.ui.common.permissions.checkNotificationPermission
import com.ammar.havenwalls.ui.crop.getCropRect
import com.ammar.havenwalls.ui.crop.getMaxCropSize
import com.ammar.havenwalls.ui.wallpaper.getWallpaperScreenPendingIntent
import com.ammar.havenwalls.utils.NotificationChannels
import com.ammar.havenwalls.utils.decodeSampledBitmapFromFile
import com.ammar.havenwalls.utils.detectObjects
import com.ammar.havenwalls.utils.getDecodeSampledBitmapOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import okhttp3.OkHttpClient

@HiltWorker
class AutoWallpaperWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val savedSearchRepository: SavedSearchRepository,
    private val autoWallpaperHistoryRepository: AutoWallpaperHistoryRepository,
    private val objectDetectionModelRepository: ObjectDetectionModelRepository,
    private val wallHavenNetwork: WallHavenNetworkDataSource,
) : CoroutineWorker(
    context,
    params,
) {
    private lateinit var appPreferences: AppPreferences
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(context, NotificationChannels.AUTO_WALLPAPER_CHANNEL_ID).apply {
            setContentTitle(context.getString(R.string.auto_wallpaper))
            setContentText(context.getString(R.string.running))
            setSmallIcon(R.drawable.outline_image_24)
            setOngoing(true)
            setSilent(true)
            priority = NotificationCompat.PRIORITY_LOW
        }
    }
    private var prevPageNum: Int? = null
    private var cachedWallpapers = mutableListOf<Wallpaper>()

    override suspend fun getForegroundInfo() = ForegroundInfo(
        NOTIFICATION_ID,
        notificationBuilder.apply {
            setProgress(0, 0, true)
        }.build()
    )

    override suspend fun doWork(): Result {
        // get auto wallpaper preferences
        appPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            ?: return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.APP_PREFS_NULL.name,
                )
            )
        val savedSearchId = appPreferences.autoWallpaperPreferences.savedSearchId
        if (!appPreferences.autoWallpaperPreferences.enabled) {
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.DISABLED.name,
                )
            )
        }
        val savedSearch = savedSearchRepository.getById(savedSearchId)?.toSavedSearch()
            ?: return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.SAVED_SEARCH_NOT_SET.name,
                )
            )
        val searchQuery = savedSearch.search.toSearchQuery()
        val (nextWallpaper, file) = setNextWallpaper(searchQuery)
        if (nextWallpaper == null || file == null) {
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.NO_WALLPAPER_FOUND.name,
                )
            )
        }
        if (appPreferences.autoWallpaperPreferences.showNotification) {
            showSuccessNotification(nextWallpaper, file)
        }
        return Result.success(
            workDataOf(
                SUCCESS_NEXT_WALLPAPER_ID to nextWallpaper.id,
            )
        )
    }

    private suspend fun setNextWallpaper(searchQuery: SearchQuery): Pair<Wallpaper?, File?> {
        // get a fresh wallpaper, ignoring the history initially
        val nextWallpaper = getNextWallpaper(searchQuery)
            ?: getNextWallpaper(searchQuery, false)
            ?: return null to null
        return try {
            val (applied, file) = setWallpaper(nextWallpaper)
            if (applied) {
                autoWallpaperHistoryRepository.addOrUpdateHistory(
                    AutoWallpaperHistory(
                        wallhavenId = nextWallpaper.id,
                        setOn = Clock.System.now(),
                    )
                )
                nextWallpaper to file
            } else null to null
        } catch (e: Exception) {
            Log.e(TAG, "setNextWallpaper: ", e)
            return null to null
        }
    }

    private suspend fun setWallpaper(nextWallpaper: Wallpaper): Pair<Boolean, File?> {
        val wallpaperFile = downloadWallpaper(nextWallpaper)
        try {
            setForeground(
                ForegroundInfo(
                    NOTIFICATION_ID,
                    notificationBuilder.apply {
                        setContentText(context.getString(R.string.changing_wallpaper))
                        setProgress(100, 0, true)
                    }.build(),
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "setWallpaper: ", e)
        }
        val uri = context.getUriForFile(wallpaperFile)
        val screenResolution = context.getScreenResolution(true)
        val maxCropSize = getMaxCropSize(
            screenResolution = screenResolution,
            imageSize = nextWallpaper.resolution.toSize(),
        )
        val (detectionScale, detection) = try {
            getDetection(uri = uri)
        } catch (e: Exception) {
            Log.e(TAG, "setWallpaper: Error in object detection", e)
            1 to null
        }
        val rect = getCropRect(
            maxCropSize = maxCropSize,
            detectionWithBitmap = detection,
            detectedRectScale = detectionScale,
            imageSize = nextWallpaper.resolution.toSize(),
            cropScale = 1f,
        )
        context.contentResolver.openInputStream(uri).use {
            if (it == null) return false to null
            val decoder = getBitmapRegionDecoder(it) ?: return false to null
            val (opts, _) = getDecodeSampledBitmapOptions(
                context = context,
                uri = uri,
                reqWidth = screenResolution.width,
                reqHeight = screenResolution.height,
            ) ?: return false to null
            val bitmap = decoder.decodeRegion(rect.toAndroidRectF().toRect(), opts)
                ?: return false to null
            context.setWallpaper(
                bitmap = bitmap,
            )
        }
        return true to wallpaperFile
    }

    private suspend fun getDetection(uri: Uri) =
        if (!appPreferences.autoWallpaperPreferences.useObjectDetection) {
            1 to null
        } else {
            val modelFile = getObjectDetectionModel()
            val (scale, detectionWithBitmaps) = detectObjects(
                context = context,
                uri = uri,
                model = modelFile,
                objectDetectionPreferences = appPreferences.objectDetectionPreferences,
            )
            scale to detectionWithBitmaps.firstOrNull()
        }

    private fun getBitmapRegionDecoder(`is`: InputStream) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(`is`)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(`is`, false)
        }

    private suspend fun downloadWallpaper(wallpaper: Wallpaper): File {
        val fileName = wallpaper.path.getFileNameFromUrl()
        return context.getTempFileIfExists(fileName) ?: download(
            okHttpClient = okHttpClient,
            url = wallpaper.path,
            dir = context.getTempDir().absolutePath,
            fileName = fileName,
            progressCallback = { total, downloaded ->
                try {
                    setForeground(
                        ForegroundInfo(
                            NOTIFICATION_ID,
                            notificationBuilder.apply {
                                setContentText(context.getString(R.string.downloading_wallpaper))
                                setProgress(total.toInt(), downloaded.toInt(), downloaded <= -1)
                            }.build(),
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting to foreground: ", e)
                }
            },
        )
    }

    private suspend fun getObjectDetectionModel(): File {
        val objectDetectionPreferences = appPreferences.objectDetectionPreferences
        val modelId = objectDetectionPreferences.modelId
        val objectDetectionModel = objectDetectionModelRepository.getById(modelId)?.toModel()
            ?: ObjectDetectionModel.DEFAULT
        val fileName = objectDetectionModel.fileName
        return context.getMLModelsFileIfExists(fileName) ?: download(
            okHttpClient = okHttpClient,
            url = objectDetectionModel.url,
            dir = context.getMLModelsDir().absolutePath,
            fileName = null,
            progressCallback = { _, _ -> },
        )
    }

    private suspend fun getNextWallpaper(
        searchQuery: SearchQuery,
        excludeHistory: Boolean = true,
    ): Wallpaper? {
        val historyIds = if (excludeHistory) {
            autoWallpaperHistoryRepository.getAll().map { it.wallhavenId }
        } else emptyList()

        var hasMore = true
        while (hasMore) {
            val wallpapers = if (excludeHistory) {
                val (wallpapers, nextPageNum) = loadWallpapers(
                    searchQuery = searchQuery,
                    pageNum = prevPageNum,
                )
                prevPageNum = nextPageNum
                cachedWallpapers += wallpapers
                hasMore = nextPageNum != null
                wallpapers
            } else {
                // not excluding history, means we already loaded all wallpapers previously
                // in such case, use cachedWallpapers
                hasMore = false
                cachedWallpapers
            }

            // Loop until we find a wallpaper
            val wallpaper = wallpapers.firstOrNull {
                if (excludeHistory) {
                    it.id !in historyIds
                } else {
                    true
                }
            }
            if (wallpaper != null) {
                return wallpaper
            }
        }
        return null
    }

    private suspend fun loadWallpapers(
        searchQuery: SearchQuery,
        pageNum: Int? = null,
    ): Pair<List<Wallpaper>, Int?> {
        val response = wallHavenNetwork.search(searchQuery, pageNum)
        val nextPageNumber = response.meta?.run {
            if (current_page != last_page) current_page + 1 else null
        }
        return response.data.map { it.asWallpaper() } to nextPageNumber
    }

    private fun showSuccessNotification(
        wallpaper: Wallpaper,
        file: File,
    ) {
        if (!context.checkNotificationPermission()) return
        val bitmap = decodeSampledBitmapFromFile(context, file.absolutePath)
        val notification = NotificationCompat.Builder(
            context,
            NotificationChannels.AUTO_WALLPAPER_CHANNEL_ID
        ).apply {
            setContentTitle(context.getString(R.string.new_wallpaper))
            setSmallIcon(R.drawable.outline_image_24)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setLargeIcon(bitmap)
            setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
            setContentIntent(
                getWallpaperScreenPendingIntent(
                    context,
                    wallpaper.id,
                )
            )
            setAutoCancel(true)
        }.build()
        context.notificationManager.notify(SUCCESS_NOTIFICATION_ID, notification)
    }

    companion object {
        const val FAILURE_REASON = "failure_reason"
        const val SUCCESS_NEXT_WALLPAPER_ID = "success_wallpaper_id"
        const val NOTIFICATION_ID = 123
        const val SUCCESS_NOTIFICATION_ID = 234
        private const val IMMEDIATE_WORK_NAME = "auto_wallpaper_immediate"
        internal const val PERIODIC_WORK_NAME = "auto_wallpaper_periodic"

        enum class FailureReason {
            APP_PREFS_NULL,
            DISABLED,
            SAVED_SEARCH_NOT_SET,
            NO_WALLPAPER_FOUND;
        }

        suspend fun schedule(
            context: Context,
            constraints: Constraints,
            interval: DateTimePeriod,
            appPreferencesRepository: AppPreferencesRepository,
        ) {
            Log.i(TAG, "Scheduling auto wallpaper worker...")
            val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
                interval.hours.toLong(),
                TimeUnit.HOURS,
            ).apply {
                setConstraints(constraints)
            }.build()
            context.workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
            appPreferencesRepository.updateAutoWallpaperWorkRequestId(request.id)
        }

        suspend fun stop(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ) {
            Log.i(TAG, "Stopping auto wallpaper worker...")
            context.workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            appPreferencesRepository.updateAutoWallpaperWorkRequestId(null)
        }

        fun triggerImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<AutoWallpaperWorker>().apply {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }.build()
            context.workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        suspend fun checkIfScheduled(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ): Boolean {
            var running = false
            try {
                val requestId = appPreferencesRepository.getAutoWallHavenWorkRequestId()
                    ?: return false
                val state = context.workManager.getWorkInfoByIdFlow(requestId).firstOrNull()?.state
                running = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
            } catch (e: Exception) {
                Log.e(TAG, "checkScheduled: ", e)
            }
            return running
        }
    }
}
