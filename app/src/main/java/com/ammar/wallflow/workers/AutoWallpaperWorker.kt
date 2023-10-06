package com.ammar.wallflow.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.compose.ui.unit.toSize
import androidx.core.app.NotificationCompat
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
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.data.db.entity.toSavedSearch
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.toWallhavenWallpaper
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.AutoWallpaperHistoryRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.accessibleFolders
import com.ammar.wallflow.extensions.displayManager
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getMLModelsDir
import com.ammar.wallflow.extensions.getMLModelsFileIfExists
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.getTempDir
import com.ammar.wallflow.extensions.getTempFileIfExists
import com.ammar.wallflow.extensions.getUriForFile
import com.ammar.wallflow.extensions.notificationManager
import com.ammar.wallflow.extensions.setWallpaper
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.AutoWallpaperHistory
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.model.search.WallhavenSearchQuery
import com.ammar.wallflow.model.search.toSearchQuery
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.services.ChangeWallpaperTileService
import com.ammar.wallflow.ui.common.permissions.checkNotificationPermission
import com.ammar.wallflow.ui.screens.crop.getCropRect
import com.ammar.wallflow.ui.screens.crop.getMaxCropSize
import com.ammar.wallflow.ui.screens.wallpaper.getWallpaperScreenPendingIntent
import com.ammar.wallflow.utils.NotificationChannels
import com.ammar.wallflow.utils.NotificationIds.AUTO_WALLPAPER_NOTIFICATION_ID
import com.ammar.wallflow.utils.NotificationIds.AUTO_WALLPAPER_SUCCESS_NOTIFICATION_ID
import com.ammar.wallflow.utils.decodeSampledBitmapFromUri
import com.ammar.wallflow.utils.getPublicDownloadsFile
import com.ammar.wallflow.utils.objectdetection.detectObjects
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
    private val wallHavenNetwork: WallhavenNetworkDataSource,
    private val favoritesRepository: FavoritesRepository,
    private val localWallpapersRepository: LocalWallpapersRepository,
) : CoroutineWorker(
    context,
    params,
) {
    private lateinit var appPreferences: AppPreferences
    private lateinit var autoWallpaperPreferences: AutoWallpaperPreferences
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
    private var cachedWallhavenWallpapers = mutableListOf<WallhavenWallpaper>()
    private val sourceChoices: Set<SourceChoice>
        get() {
            return mutableSetOf<SourceChoice>().apply {
                if (autoWallpaperPreferences.savedSearchEnabled) {
                    add(SourceChoice.SAVED_SEARCH)
                }
                if (autoWallpaperPreferences.favoritesEnabled) {
                    add(SourceChoice.FAVORITES)
                }
                if (autoWallpaperPreferences.localEnabled) {
                    add(SourceChoice.LOCAL)
                }
            }.toSet()
        }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        AUTO_WALLPAPER_NOTIFICATION_ID,
        notificationBuilder.apply {
            setProgress(0, 0, true)
        }.build(),
    )

    override suspend fun doWork(): Result {
        appPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            ?: return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.APP_PREFS_NULL.name,
                ),
            )
        autoWallpaperPreferences = appPreferences.autoWallpaperPreferences
        val tileAdded = appPreferences.changeWallpaperTileAdded
        try {
            if (tileAdded) {
                ChangeWallpaperTileService.requestListeningState(context)
            }
            return doWorkActual()
        } finally {
            if (tileAdded) {
                ChangeWallpaperTileService.requestListeningState(context)
            }
        }
    }

    private suspend fun doWorkActual(): Result {
        val forced = inputData.getBoolean(INPUT_FORCE, false)
        if (!autoWallpaperPreferences.enabled && !forced) {
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.DISABLED.name,
                ),
            )
        }
        if (!autoWallpaperPreferences.anySourceEnabled) {
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.NO_SOURCES_ENABLED.name,
                ),
            )
        }
        // check if saved search id is valid
        if (autoWallpaperPreferences.savedSearchEnabled) {
            val savedSearchId = autoWallpaperPreferences.savedSearchId
            savedSearchRepository.getById(savedSearchId) ?: return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.SAVED_SEARCH_NOT_SET.name,
                ),
            )
        }
        val (nextWallpaper, uri) = setNextWallpaper()
        if (nextWallpaper == null || uri == null) {
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.NO_WALLPAPER_FOUND.name,
                ),
            )
        }
        if (autoWallpaperPreferences.markFavorite) {
            markFavorite(nextWallpaper)
        }
        if (autoWallpaperPreferences.download) {
            saveWallpaperToDownloads(nextWallpaper, uri)
        }
        if (autoWallpaperPreferences.showNotification) {
            showSuccessNotification(nextWallpaper, uri)
        }
        return Result.success(
            workDataOf(
                SUCCESS_NEXT_WALLPAPER_ID to nextWallpaper.id,
            ),
        )
    }

    private suspend fun setNextWallpaper(): Pair<Wallpaper?, Uri?> {
        val sourceChoice = getNextSourceChoice()
        val nextWallpaper: Wallpaper = when (sourceChoice) {
            SourceChoice.SAVED_SEARCH -> {
                val savedSearchId = autoWallpaperPreferences.savedSearchId
                val savedSearch = savedSearchRepository.getById(savedSearchId)
                val savedSearchQuery = savedSearch?.toSavedSearch()?.search?.toSearchQuery()
                    ?: return null to null
                // get a fresh wallpaper, ignoring the history initially
                getNextSavedSearchWallpaper(savedSearchQuery)
                    ?: getNextSavedSearchWallpaper(savedSearchQuery, false)
                    ?: return null to null
            }
            SourceChoice.FAVORITES -> getNextFavoriteWallpaper()
                ?: return null to null
            SourceChoice.LOCAL -> getNextLocalWallpaper()
                ?: return null to null
        }
        return try {
            val (applied, file) = setWallpaper(nextWallpaper)
            if (applied) {
                autoWallpaperHistoryRepository.addOrUpdateHistory(
                    AutoWallpaperHistory(
                        sourceId = nextWallpaper.id,
                        source = nextWallpaper.source,
                        sourceChoice = sourceChoice,
                        setOn = Clock.System.now(),
                    ),
                )
                nextWallpaper to file
            } else {
                null to null
            }
        } catch (e: Exception) {
            Log.e(TAG, "setNextWallpaper: ", e)
            return null to null
        }
    }

    private fun getNextSourceChoice() = sourceChoices.random()

    private suspend fun setWallpaper(
        nextWallpaper: Wallpaper,
    ): Pair<Boolean, Uri?> {
        val uri: Uri = when (nextWallpaper) {
            is WallhavenWallpaper -> {
                val wallpaperFile = safeDownloadWallpaper(nextWallpaper) ?: return false to null
                try {
                    val notification = notificationBuilder.apply {
                        setContentText(context.getString(R.string.changing_wallpaper))
                        setProgress(100, 0, true)
                    }.build()
                    setForeground(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ForegroundInfo(
                                AUTO_WALLPAPER_NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
                            )
                        } else {
                            ForegroundInfo(
                                AUTO_WALLPAPER_NOTIFICATION_ID,
                                notification,
                            )
                        },
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "setWallpaper: ", e)
                }
                context.getUriForFile(wallpaperFile)
            }
            is LocalWallpaper -> nextWallpaper.data
            else -> return false to null
        }
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
            detectionRect = detection?.detection?.boundingBox,
            detectedRectScale = detectionScale,
            imageSize = nextWallpaper.resolution.toSize(),
            cropScale = 1f,
        )
        val display = context.displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val applied = context.setWallpaper(display, uri, rect, autoWallpaperPreferences.targets)
        if (!applied) {
            return false to null
        }
        return true to uri
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

    private suspend fun safeDownloadWallpaper(wallhavenWallpaper: WallhavenWallpaper): File? {
        var downloadTries = 0
        while (true) {
            val wallpaperFile = downloadWallpaper(wallhavenWallpaper)
            // check if file size matches
            if (wallpaperFile.length() == wallhavenWallpaper.fileSize) {
                // file was correctly downloaded
                return wallpaperFile
            }
            // increment try count
            downloadTries++
            // max 3 tries
            if (downloadTries < 3) {
                // retry downloading the file
                continue
            }
            // delete the file and return
            wallpaperFile.delete()
            // TODO skip this file next time
            return null
        }
    }

    private suspend fun downloadWallpaper(wallpaper: DownloadableWallpaper): File {
        val fileName = wallpaper.data.getFileNameFromUrl()
        return context.getTempFileIfExists(fileName) ?: download(
            okHttpClient = okHttpClient,
            url = wallpaper.data,
            dir = context.getTempDir().absolutePath,
            fileName = fileName,
            progressCallback = { total, downloaded ->
                try {
                    val notification = notificationBuilder.apply {
                        setContentText(context.getString(R.string.downloading_wallpaper))
                        setProgress(total.toInt(), downloaded.toInt(), downloaded <= -1)
                    }.build()
                    setForeground(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ForegroundInfo(
                                AUTO_WALLPAPER_NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                            )
                        } else {
                            ForegroundInfo(
                                AUTO_WALLPAPER_NOTIFICATION_ID,
                                notification,
                            )
                        },
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

    private suspend fun getNextSavedSearchWallpaper(
        searchQuery: WallhavenSearchQuery,
        excludeHistory: Boolean = true,
    ): WallhavenWallpaper? {
        val historyIds = if (excludeHistory) {
            autoWallpaperHistoryRepository.getAllBySource(Source.WALLHAVEN).map { it.sourceId }
        } else {
            emptyList()
        }

        var hasMore = true
        while (hasMore) {
            val wallpapers = if (excludeHistory) {
                val (wallpapers, nextPageNum) = loadWallpapers(
                    searchQuery = searchQuery,
                    pageNum = prevPageNum,
                )
                prevPageNum = nextPageNum
                cachedWallhavenWallpapers += wallpapers
                hasMore = nextPageNum != null
                wallpapers
            } else {
                // not excluding history, means we already loaded all wallpapers previously
                // in such case, use cachedWallpapers
                hasMore = false
                cachedWallhavenWallpapers
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

    private suspend fun getNextFavoriteWallpaper() = favoritesRepository.getRandom(
        context = context,
    )

    private suspend fun getNextLocalWallpaper() = localWallpapersRepository.getRandom(
        context = context,
        uris = context.accessibleFolders.map { it.uri },
    )

    private suspend fun loadWallpapers(
        searchQuery: WallhavenSearchQuery,
        pageNum: Int? = null,
    ): Pair<List<WallhavenWallpaper>, Int?> {
        val response = wallHavenNetwork.search(searchQuery, pageNum)
        val nextPageNumber = response.meta?.run {
            if (current_page != last_page) current_page + 1 else null
        }
        return response.data.map { it.toWallhavenWallpaper() } to nextPageNumber
    }

    private fun showSuccessNotification(
        wallpaper: Wallpaper,
        uri: Uri,
    ) {
        if (!context.checkNotificationPermission()) return
        val (bitmap, _) = decodeSampledBitmapFromUri(context, uri) ?: return
        val notification = NotificationCompat.Builder(
            context,
            NotificationChannels.AUTO_WALLPAPER_CHANNEL_ID,
        ).apply {
            setContentTitle(context.getString(R.string.new_wallpaper))
            setSmallIcon(R.drawable.outline_image_24)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setLargeIcon(bitmap)
            setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?),
            )
            setContentIntent(
                getWallpaperScreenPendingIntent(
                    context = context,
                    source = wallpaper.source,
                    wallpaperId = wallpaper.id,
                ),
            )
            setAutoCancel(true)
        }.build()
        context.notificationManager.notify(
            AUTO_WALLPAPER_SUCCESS_NOTIFICATION_ID,
            notification,
        )
    }

    private suspend fun markFavorite(wallpaper: Wallpaper) {
        try {
            favoritesRepository.addFavorite(
                sourceId = wallpaper.id,
                source = wallpaper.source,
            )
        } catch (e: Exception) {
            Log.e(TAG, "markFavorite: ", e)
        }
    }

    private fun saveWallpaperToDownloads(
        wallpaper: Wallpaper,
        uri: Uri,
    ) {
        if (wallpaper.source == Source.LOCAL) {
            // Don't save Local files
            Log.i(TAG, "Download skipped as it is a Local wallpaper")
            return
        }
        if (wallpaper !is DownloadableWallpaper) {
            // only downloadable wallpapers can be saved
            Log.i(TAG, "Download skipped as it is not a downloadable wallpaper")
            return
        }
        try {
            val url = wallpaper.data
            val fileName = url.getFileNameFromUrl()
            val dest = getPublicDownloadsFile(fileName)
            copyFiles(context, uri, dest)
            scanFile(context, dest)
        } catch (e: Exception) {
            Log.e(TAG, "saveWallpaperToDownloads: ", e)
        }
    }

    companion object {
        const val FAILURE_REASON = "failure_reason"
        const val SUCCESS_NEXT_WALLPAPER_ID = "success_wallpaper_id"
        private const val IMMEDIATE_WORK_NAME = "auto_wallpaper_immediate"
        internal const val PERIODIC_WORK_NAME = "auto_wallpaper_periodic"
        internal const val INPUT_FORCE = "auto_wallpaper_force"

        enum class FailureReason {
            APP_PREFS_NULL,
            DISABLED,
            NO_SOURCES_ENABLED,
            SAVED_SEARCH_NOT_SET,
            NO_WALLPAPER_FOUND,
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
            Log.i(TAG, "Auto wallpaper worker scheduled!")
        }

        suspend fun stop(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ) {
            Log.i(TAG, "Stopping auto wallpaper worker...")
            context.workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            appPreferencesRepository.updateAutoWallpaperWorkRequestId(null)
            Log.i(TAG, "Auto wallpaper worker cancelled!")
        }

        suspend fun triggerImmediate(
            context: Context,
            force: Boolean = false,
        ): UUID {
            val workInfosFlow = context.workManager.getWorkInfosForUniqueWorkFlow(
                IMMEDIATE_WORK_NAME,
            )
            val workInfos = workInfosFlow.firstOrNull()
            val workInfo = workInfos?.firstOrNull {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            }
            if (workInfo != null) {
                return workInfo.id
            }
            val request = OneTimeWorkRequestBuilder<AutoWallpaperWorker>().apply {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                setInputData(workDataOf(INPUT_FORCE to force))
            }.build()
            context.workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
            return request.id
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

        fun getProgress(
            context: Context,
            requestId: UUID,
        ) = context.workManager.getWorkInfoByIdFlow(requestId).map {
            val info = it ?: return@map Status.Failed(
                IllegalArgumentException("No work found for requestId $requestId"),
            )
            when (info.state) {
                WorkInfo.State.ENQUEUED -> Status.Pending
                WorkInfo.State.RUNNING -> Status.Running
                WorkInfo.State.SUCCEEDED -> Status.Success
                WorkInfo.State.FAILED -> Status.Failed(
                    AutoWallpaperException(info.outputData.getString(FAILURE_REASON)),
                )
                WorkInfo.State.BLOCKED -> Status.Pending
                WorkInfo.State.CANCELLED -> Status.Failed(AutoWallpaperException("Work cancelled"))
            }
        }

        enum class SourceChoice {
            SAVED_SEARCH,
            FAVORITES,
            LOCAL,
        }

        class AutoWallpaperException(message: String? = null) : Exception(message ?: "")

        sealed class Status {
            data object Running : Status()
            data object Pending : Status()
            data object Success : Status()
            data class Failed(val e: Throwable? = null) : Status()

            fun isSuccessOrFail() = this is Success || this is Failed
        }
    }
}
