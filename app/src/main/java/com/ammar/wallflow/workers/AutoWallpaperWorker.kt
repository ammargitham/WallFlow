package com.ammar.wallflow.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.unit.toSize
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
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
import com.ammar.wallflow.MIME_TYPE_TFLITE_MODEL
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.search.toSavedSearch
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.data.db.entity.wallpaper.toWallpaper
import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.reddit.toWallpaperEntities
import com.ammar.wallflow.data.network.model.wallhaven.toWallhavenWallpaper
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.AutoWallpaperHistoryRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.displayManager
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getMLModelsDir
import com.ammar.wallflow.extensions.getMLModelsFileIfExists
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.getTempDir
import com.ammar.wallflow.extensions.getTempFileIfExists
import com.ammar.wallflow.extensions.isExtraDimActive
import com.ammar.wallflow.extensions.isInDefaultOrientation
import com.ammar.wallflow.extensions.isSystemInDarkTheme
import com.ammar.wallflow.extensions.notificationManager
import com.ammar.wallflow.extensions.parseMimeType
import com.ammar.wallflow.extensions.setWallpaper
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.AutoWallpaperHistory
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.services.ChangeWallpaperTileService
import com.ammar.wallflow.ui.common.permissions.checkNotificationPermission
import com.ammar.wallflow.ui.screens.crop.getCropRect
import com.ammar.wallflow.ui.screens.crop.getMaxCropSize
import com.ammar.wallflow.ui.screens.wallpaper.getWallpaperScreenPendingIntent
import com.ammar.wallflow.utils.ExifWriteType
import com.ammar.wallflow.utils.NotificationChannels
import com.ammar.wallflow.utils.NotificationIds.AUTO_WALLPAPER_HOME_SUCCESS_NOTIFICATION_ID
import com.ammar.wallflow.utils.NotificationIds.AUTO_WALLPAPER_LOCK_SUCCESS_NOTIFICATION_ID
import com.ammar.wallflow.utils.NotificationIds.AUTO_WALLPAPER_NOTIFICATION_ID
import com.ammar.wallflow.utils.NotificationIds.AUTO_WALLPAPER_SUCCESS_NOTIFICATION_ID
import com.ammar.wallflow.utils.decodeSampledBitmapFromUri
import com.ammar.wallflow.utils.getNextSourceChoice
import com.ammar.wallflow.utils.getPublicDownloadsDir
import com.ammar.wallflow.utils.objectdetection.detectObjects
import com.ammar.wallflow.utils.objectdetection.objectsDetector
import com.ammar.wallflow.utils.valueOf
import com.ammar.wallflow.utils.writeTagsToFile
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
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
    private val redditNetwork: RedditNetworkDataSource,
    private val favoritesRepository: FavoritesRepository,
    private val localWallpapersRepository: LocalWallpapersRepository,
    private val lightDarkRepository: LightDarkRepository,
) : CoroutineWorker(
    context,
    params,
) {
    private lateinit var appPreferences: AppPreferences
    private lateinit var autoWallpaperPreferences: AutoWallpaperPreferences
    private lateinit var currentTargets: Set<WallpaperTarget>
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
    private var prevPageNum: String? = null
    private var cachedWallhavenWallpapers = mutableListOf<Wallpaper>()
    private var badWallpapers = mutableListOf<Wallpaper>()
    private var badSourceChoices = mutableListOf<SourceChoice>()
    private val sourceChoices: Set<SourceChoice>
        get() = mutableSetOf<SourceChoice>().apply {
            if (autoWallpaperPreferences.lightDarkEnabled) {
                add(SourceChoice.LIGHT_DARK)
                // no other sources should be active
                // when light dark is enabled
                return@apply
            }
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
    private val lsSourceChoices: Set<SourceChoice>
        get() = mutableSetOf<SourceChoice>().apply {
            if (autoWallpaperPreferences.lsLightDarkEnabled) {
                add(SourceChoice.LIGHT_DARK)
                // no other sources should be active
                // when light dark is enabled
                return@apply
            }
            if (autoWallpaperPreferences.lsSavedSearchEnabled) {
                add(SourceChoice.SAVED_SEARCH)
            }
            if (autoWallpaperPreferences.lsFavoritesEnabled) {
                add(SourceChoice.FAVORITES)
            }
            if (autoWallpaperPreferences.lsLocalEnabled) {
                add(SourceChoice.LOCAL)
            }
        }.toSet()

    override suspend fun getForegroundInfo() = ForegroundInfo(
        AUTO_WALLPAPER_NOTIFICATION_ID,
        notificationBuilder.apply {
            setProgress(0, 0, true)
        }.build(),
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: AutoWallpaper work started")
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
        val targetStrArray = inputData.getStringArray(INPUT_TARGETS)
        currentTargets = targetStrArray
            ?.mapNotNull { valueOf<WallpaperTarget>(it) }
            ?.toSet()
            ?: WallpaperTarget.ALL
        if (currentTargets.isEmpty()) {
            currentTargets = WallpaperTarget.ALL
        }
        Log.d(TAG, "doWorkActual: enabled: ${autoWallpaperPreferences.enabled}")
        if (!autoWallpaperPreferences.enabled && !forced) {
            Log.d(TAG, "doWork: AutoWallpaper failed since it is disabled")
            // worker should not be running, stop it
            stop(
                context = context,
                appPreferencesRepository = appPreferencesRepository,
            )
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.DISABLED.name,
                ),
            )
        }
        // check if current targets are enabled in auto wall prefs
        val enabledTargets = autoWallpaperPreferences.targets
        val actualTargets = enabledTargets.intersect(currentTargets)
        if (actualTargets.isEmpty()) {
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.CURRENT_TARGETS_DISABLED.name,
                ),
            )
        }
        val sourceDisabled = if (currentTargets.size == 2) {
            !autoWallpaperPreferences.anySourceEnabled
        } else {
            when (currentTargets.first()) {
                WallpaperTarget.HOME -> !autoWallpaperPreferences.anyHomeScreenSourceEnabled
                WallpaperTarget.LOCKSCREEN -> !autoWallpaperPreferences.anyLockScreenSourceEnabled
            }
        }
        if (sourceDisabled) {
            Log.d(TAG, "doWork: AutoWallpaper failed since no source enabled")
            return Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.NO_SOURCES_ENABLED.name,
                ),
            )
        }
        // check if device is not in its default orientation
        if (!forced && !context.isInDefaultOrientation()) {
            Log.d(TAG, "doWork: AutoWallpaper failed since not in default orientation")
            Log.i(TAG, "Device is rotated. Auto wallpaper will retry in 15 minutes")
            return Result.retry()
        }
        return try {
            val (hWall, lWall) = setWallpaper()
            Result.success(
                workDataOf(
                    SUCCESS_NEXT_HOME_WALLPAPER_ID to hWall.id,
                    SUCCESS_NEXT_LOCK_WALLPAPER_ID to lWall.id,
                ),
            )
        } catch (e: NoWallpaperFoundError) {
            Log.e(TAG, "doWork: AutoWallpaper failed: no wallpaper found", e)
            Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.NO_WALLPAPER_FOUND.name,
                ),
            )
        } catch (e: SavedSearchNotFoundError) {
            Log.e(TAG, "doWork: AutoWallpaper failed: no saved search found", e)
            Result.failure(
                workDataOf(
                    FAILURE_REASON to FailureReason.SAVED_SEARCH_NOT_SET.name,
                ),
            )
        }
    }

    private suspend fun setWallpaper(): Pair<Wallpaper, Wallpaper> {
        val enabledTargets = autoWallpaperPreferences.targets
        val targets = enabledTargets.intersect(currentTargets)
        val setDifferentWallpapers = enabledTargets.size == 2 &&
            autoWallpaperPreferences.setDifferentWallpapers
        return if (setDifferentWallpapers) {
            val (
                homeWallpaper,
                homeUri,
                homeSourceChoice,
            ) = setWallpaperForTarget(WallpaperTarget.HOME)
            val (
                lockWallpaper,
                lockUri,
                lockSourceChoice,
            ) = setWallpaperForTarget(WallpaperTarget.LOCKSCREEN)
            if (autoWallpaperPreferences.showNotification) {
                showSuccessNotification(
                    wallpaper = homeWallpaper,
                    uri = homeUri,
                    targets = setOf(WallpaperTarget.HOME),
                )
                showSuccessNotification(
                    wallpaper = lockWallpaper,
                    uri = lockUri,
                    silent = true, // set second notification silent
                    targets = setOf(WallpaperTarget.LOCKSCREEN),
                )
            }
            appPreferencesRepository.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    prevHomeSource = homeSourceChoice,
                    prevLockScreenSource = lockSourceChoice,
                ),
            )
            homeWallpaper to lockWallpaper
        } else {
            val (nextWallpaper, uri, sourceChoice) = setWallpaperForTargets(targets)
            if (autoWallpaperPreferences.showNotification) {
                showSuccessNotification(
                    wallpaper = nextWallpaper,
                    uri = uri,
                    targets = targets,
                )
            }
            appPreferencesRepository.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    prevHomeSource = sourceChoice,
                    prevLockScreenSource = sourceChoice,
                ),
            )
            nextWallpaper to nextWallpaper
        }
    }

    private suspend fun setWallpaperForTarget(
        target: WallpaperTarget,
    ) = setWallpaperForTargets(setOf(target))

    private suspend fun setWallpaperForTargets(
        targets: Set<WallpaperTarget>,
    ): SetWallpaperSuccessResult {
        val result = setNextWallpaper(targets)
        val (nextWallpaper, uri, sourceChoice) = result
        if (nextWallpaper == null || uri == null || sourceChoice == null) {
            throw NoWallpaperFoundError()
        }
        if (autoWallpaperPreferences.markFavorite) {
            markFavorite(nextWallpaper)
        }
        if (autoWallpaperPreferences.download) {
            saveWallpaperToDownloads(
                wallpaper = nextWallpaper,
                uri = uri,
                writeTagsToExif = appPreferences.writeTagsToExif,
                tagsExifWriteType = appPreferences.tagsExifWriteType,
            )
        }
        return SetWallpaperSuccessResult(nextWallpaper, uri, sourceChoice)
    }

    private class SavedSearchNotFoundError : Error()
    private class NoWallpaperFoundError : Error()

    private suspend fun setNextWallpaper(
        targets: Set<WallpaperTarget>,
    ): SetWallpaperResult {
        val targetForSource = if (targets.size == 2) {
            // if setting to both targets, use the home screen sources
            WallpaperTarget.HOME
        } else {
            targets.first()
        }
        var sourceChoice = getNextSourceChoice(
            target = targetForSource,
            sourceChoices = sourceChoices,
            lsSourceChoices = lsSourceChoices,
            prevHomeSourceChoice = autoWallpaperPreferences.prevHomeSource,
            prevLsSourceChoice = autoWallpaperPreferences.prevLockScreenSource,
        ) ?: return SetWallpaperResult.EMPTY

        var result = SetWallpaperResult.EMPTY
        var tryNext = true
        while (tryNext) {
            val nextWallpaper = getNextWallpaper(
                sourceChoice = sourceChoice,
                target = targetForSource,
            ) ?: return SetWallpaperResult.EMPTY
            try {
                result = tryApplyWallpaper(
                    nextWallpaper = nextWallpaper,
                    targets = targets,
                    sourceChoice = sourceChoice,
                )
                // successful attempt. Break the loop.
                tryNext = false
            } catch (e: BadWallpaperError) {
                Log.e(TAG, "setNextWallpaper: Bad wallpaper:", e)
                badWallpapers.add(nextWallpaper)
                tryNext = hasMoreWallpapers(
                    sourceChoice = sourceChoice,
                    target = targetForSource,
                )
                if (!tryNext) {
                    badSourceChoices.add(sourceChoice)
                    // try the next source choice
                    sourceChoice = getNextSourceChoice(
                        target = targetForSource,
                        sourceChoices = sourceChoices,
                        lsSourceChoices = lsSourceChoices,
                        prevHomeSourceChoice = autoWallpaperPreferences.prevHomeSource,
                        prevLsSourceChoice = autoWallpaperPreferences.prevLockScreenSource,
                        excluding = badSourceChoices,
                    ) ?: return SetWallpaperResult.EMPTY
                    // restart loop with new choice
                    tryNext = true
                }
                Log.d(TAG, "setNextWallpaper: Trying again?: $tryNext")
            } catch (e: Exception) {
                Log.e(TAG, "setNextWallpaper: ", e)
                tryNext = false
            }
        }
        return result
    }

    private suspend fun tryApplyWallpaper(
        nextWallpaper: Wallpaper,
        targets: Set<WallpaperTarget>,
        sourceChoice: SourceChoice,
    ): SetWallpaperResult {
        Log.d(
            TAG,
            "tryApplyWallpaper: Trying to apply: " +
                "${nextWallpaper.source}: ${nextWallpaper.id}",
        )
        val (applied, file) = setWallpaper(
            nextWallpaper = nextWallpaper,
            targets = targets,
        )
        return if (applied) {
            autoWallpaperHistoryRepository.addHistory(
                AutoWallpaperHistory(
                    sourceId = nextWallpaper.id,
                    source = nextWallpaper.source,
                    sourceChoice = sourceChoice,
                    setOn = Clock.System.now(),
                    targets = targets,
                ),
            )
            SetWallpaperResult(nextWallpaper, file, sourceChoice)
        } else {
            Log.e(TAG, "tryApplyWallpaper: Apply failed")
            SetWallpaperResult.EMPTY
        }
    }

    data class SetWallpaperResult(
        val wallpaper: Wallpaper? = null,
        val uri: Uri? = null,
        val sourceChoice: SourceChoice? = null,
    ) {
        companion object {
            val EMPTY = SetWallpaperResult()
        }
    }

    data class SetWallpaperSuccessResult(
        val wallpaper: Wallpaper,
        val uri: Uri,
        val sourceChoice: SourceChoice,
    )

    private suspend fun getNextWallpaper(
        sourceChoice: SourceChoice,
        target: WallpaperTarget,
    ) = when (sourceChoice) {
        SourceChoice.LIGHT_DARK -> getNextLightDarkWallpaper(target)
        SourceChoice.SAVED_SEARCH -> getNextSavedSearchWallpaper(target)
        SourceChoice.FAVORITES -> getNextFavoriteWallpaper()
        SourceChoice.LOCAL -> getNextLocalWallpaper(target)
    }

    private suspend fun setWallpaper(
        nextWallpaper: Wallpaper,
        targets: Set<WallpaperTarget>,
    ): Pair<Boolean, Uri?> {
        val uri: Uri = when (nextWallpaper) {
            is DownloadableWallpaper -> {
                val wallpaperFile = safeDownloadWallpaper(nextWallpaper)
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
                wallpaperFile.uri
            }
            is LocalWallpaper -> nextWallpaper.data
            else -> return false to null
        }
        val rect = getCropRect(nextWallpaper, uri)
        val display = context.displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val applied = context.setWallpaper(display, uri, rect, targets)
        if (!applied) {
            return false to null
        }
        return true to uri
    }

    private suspend fun getCropRect(
        nextWallpaper: Wallpaper,
        uri: Uri,
    ): Rect {
        val imageSize = nextWallpaper.resolution.toSize()
        if (!autoWallpaperPreferences.crop) {
            return imageSize.toRect()
        }
        val screenResolution = context.getScreenResolution(true)
        val maxCropSize = getMaxCropSize(
            screenResolution = screenResolution,
            imageSize = imageSize,
        )
        val (detectionScale, detection) = try {
            getDetection(uri = uri)
        } catch (e: Exception) {
            Log.e(TAG, "setWallpaper: Error in object detection", e)
            1 to null
        }
        return getCropRect(
            maxCropSize = maxCropSize,
            detectionRect = detection?.detection?.boundingBox,
            detectedRectScale = detectionScale,
            imageSize = imageSize,
            cropScale = 1f,
        )
    }

    private suspend fun getDetection(uri: Uri) = if (
        !objectsDetector.isEnabled ||
        !appPreferences.autoWallpaperPreferences.useObjectDetection
    ) {
        1 to null
    } else {
        val modelFile = getObjectDetectionModel().uri.toFile()
        val (scale, detectionWithBitmaps) = detectObjects(
            context = context,
            uri = uri,
            model = modelFile,
            objectDetectionPreferences = appPreferences.objectDetectionPreferences,
        )
        scale to detectionWithBitmaps.firstOrNull()
    }

    private class BadWallpaperError : Error {
        constructor(cause: Throwable) : super(cause)
        constructor(cause: String) : super(cause)
    }

    private suspend fun safeDownloadWallpaper(
        wallpaper: DownloadableWallpaper,
    ): DocumentFileCompat {
        var downloadTries = 0
        while (true) {
            val wallpaperFile = try {
                downloadWallpaper(wallpaper)
            } catch (e: Exception) {
                throw BadWallpaperError(e)
            }
            if (wallpaper !is WallhavenWallpaper) {
                return wallpaperFile
            }
            // check if file size matches (only for wallhaven, as reddit does not return fileSize)
            if (wallpaperFile.length == wallpaper.fileSize) {
                // file was correctly downloaded
                return wallpaperFile
            }
            Log.w(TAG, "DownloadWallpaper: File length mismatch! Trying again...")
            // increment try count
            downloadTries++
            // max 3 tries
            if (downloadTries < 3) {
                // retry downloading the file
                continue
            }
            // delete the file and return
            wallpaperFile.delete()
            throw BadWallpaperError("Bad url: ${wallpaper.url}")
        }
    }

    private suspend fun downloadWallpaper(wallpaper: DownloadableWallpaper): DocumentFileCompat {
        val fileName = wallpaper.data.getFileNameFromUrl()
        val tempFile = context.getTempFileIfExists(fileName)
        if (tempFile != null) {
            return DocumentFileCompat.fromFile(context, tempFile)
        }
        return download(
            context = context,
            okHttpClient = okHttpClient,
            url = wallpaper.data,
            dirUri = context.getTempDir().toUri(),
            fileName = fileName,
            mimeType = wallpaper.mimeType ?: parseMimeType(wallpaper.data),
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

    private suspend fun getObjectDetectionModel(): DocumentFileCompat {
        val objectDetectionPreferences = appPreferences.objectDetectionPreferences
        val modelId = objectDetectionPreferences.modelId
        val objectDetectionModel = objectDetectionModelRepository.getById(modelId)?.toModel()
            ?: ObjectDetectionModel.DEFAULT
        val fileName = objectDetectionModel.fileName
        val file = context.getMLModelsFileIfExists(fileName)
        if (file != null) {
            return DocumentFileCompat.fromFile(context, file)
        }
        return download(
            context = context,
            okHttpClient = okHttpClient,
            url = objectDetectionModel.url,
            dirUri = context.getMLModelsDir().toUri(),
            fileName = objectDetectionModel.fileName,
            mimeType = MIME_TYPE_TFLITE_MODEL,
            progressCallback = { _, _ -> },
        )
    }

    private suspend fun getNextSavedSearchWallpaper(target: WallpaperTarget): Wallpaper? {
        val savedSearchIds = when (target) {
            WallpaperTarget.HOME -> autoWallpaperPreferences.savedSearchIds
            WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.lsSavedSearchIds
        }
        if (savedSearchIds.isEmpty()) {
            throw SavedSearchNotFoundError()
        }
        val savedSearchId = savedSearchIds.random()
        val savedSearch = savedSearchRepository.getById(savedSearchId)
        val search = savedSearch
            ?.toSavedSearch()
            ?.search
            ?: throw SavedSearchNotFoundError()
        // get a fresh wallpaper, ignoring the history initially
        return getNextSavedSearchWallpaper(search)
            ?: getNextSavedSearchWallpaper(search, false)
    }

    private suspend fun getNextSavedSearchWallpaper(
        search: Search,
        excludeHistory: Boolean = true,
    ): Wallpaper? {
        val historyIds = if (excludeHistory) {
            autoWallpaperHistoryRepository.getAllBySource(
                when (search) {
                    is RedditSearch -> Source.REDDIT
                    is WallhavenSearch -> Source.WALLHAVEN
                },
            ).map { it.sourceId }
        } else {
            emptyList()
        }
        var hasMore = true
        while (hasMore) {
            val wallpapers = if (excludeHistory) {
                val (tempWallpapers, nextPageNum) = loadWallpapers(
                    search = search,
                    page = prevPageNum,
                )
                prevPageNum = nextPageNum
                cachedWallhavenWallpapers += tempWallpapers
                hasMore = nextPageNum != null
                tempWallpapers
            } else {
                // not excluding history, means we already loaded all wallpapers previously
                // in such case, use cachedWallpapers
                hasMore = false
                cachedWallhavenWallpapers
            }

            // Loop until we find a wallpaper
            val wallpaper = wallpapers.firstOrNull {
                // exclude bad wallpapers always
                val isBad = badWallpapers.any { b ->
                    it.id == b.id && it.source == b.source
                }
                if (isBad) {
                    false
                } else {
                    if (excludeHistory) {
                        it.id !in historyIds
                    } else {
                        true
                    }
                }
            }
            if (wallpaper != null) {
                return wallpaper
            }
        }
        return null
    }

    private suspend fun getNextLightDarkWallpaper(target: WallpaperTarget): Wallpaper? {
        val typeFlags = getTypeFlagsForTarget(target)
        // try to get fresh first else one with oldest 'set_on' in history
        return lightDarkRepository.getFirstFreshByTypeFlags(
            context = context,
            typeFlags = typeFlags,
            excluding = badWallpapers,
        ) ?: lightDarkRepository.getByOldestSetOnAndTypeFlags(
            context = context,
            typeFlags = typeFlags,
            excluding = badWallpapers,
        )
    }

    private fun getTypeFlagsForTarget(target: WallpaperTarget) =
        if (!context.isSystemInDarkTheme()) {
            setOf(LightDarkType.LIGHT)
        } else {
            val extraDimActive = context.isExtraDimActive()
            val useDarkWithExtraDim = when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.useDarkWithExtraDim
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.lsUseDarkWithExtraDim
            }
            if (extraDimActive) {
                mutableSetOf(
                    LightDarkType.EXTRA_DIM,
                    LightDarkType.DARK or LightDarkType.EXTRA_DIM,
                ).also {
                    if (useDarkWithExtraDim) {
                        it + LightDarkType.DARK
                    }
                }
            } else {
                setOf(LightDarkType.DARK)
            }
        }

    private suspend fun getNextFavoriteWallpaper() = favoritesRepository.getFirstFresh(
        context = context,
        excluding = badWallpapers,
    ) ?: favoritesRepository.getByOldestSetOn(
        context = context,
        excluding = badWallpapers,
    )

    private suspend fun getNextLocalWallpaper(target: WallpaperTarget): Wallpaper? {
        val uris = when (target) {
            WallpaperTarget.HOME -> autoWallpaperPreferences.localDirs
            WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.lsLocalDirs
        }
        if (uris.isEmpty()) {
            return null
        }
        return localWallpapersRepository.getFirstFresh(
            context = context,
            uris = uris,
            excluding = badWallpapers,
        ) ?: localWallpapersRepository.getByOldestSetOn(
            context = context,
            excluding = badWallpapers,
        )
    }

    private suspend fun loadWallpapers(
        search: Search,
        page: String? = null,
    ): Pair<List<Wallpaper>, String?> {
        when (search) {
            is WallhavenSearch -> {
                val response = wallHavenNetwork.search(search, page?.toIntOrNull())
                val nextPageNumber = response.meta?.run {
                    if (current_page != last_page) current_page + 1 else null
                }
                return response.data.map {
                    it.toWallhavenWallpaper()
                } to nextPageNumber?.toString()
            }
            is RedditSearch -> {
                val response = redditNetwork.search(search, page)
                val after = response.data.after
                return response.data.children.flatMap {
                    it.data.toWallpaperEntities()
                }.map {
                    it.toWallpaper()
                } to after
            }
        }
    }

    private fun showSuccessNotification(
        wallpaper: Wallpaper,
        uri: Uri,
        silent: Boolean = false,
        targets: Set<WallpaperTarget>,
    ) {
        if (!context.checkNotificationPermission() || targets.isEmpty()) return
        val (bitmap, _) = decodeSampledBitmapFromUri(context, uri) ?: return
        val title = context.getString(
            when {
                targets.size == 2 -> R.string.new_wallpaper
                targets.first() == WallpaperTarget.HOME -> R.string.new_home_screen_wallpaper
                else -> R.string.new_lock_screen_wallpaper
            },
        )
        val notificationId = when {
            targets.size == 2 -> AUTO_WALLPAPER_SUCCESS_NOTIFICATION_ID
            targets.first() == WallpaperTarget.HOME -> AUTO_WALLPAPER_HOME_SUCCESS_NOTIFICATION_ID
            else -> AUTO_WALLPAPER_LOCK_SUCCESS_NOTIFICATION_ID
        }
        val notification = NotificationCompat.Builder(
            context,
            NotificationChannels.AUTO_WALLPAPER_CHANNEL_ID,
        ).apply {
            setContentTitle(title)
            setSmallIcon(R.drawable.outline_image_24)
            setSilent(silent)
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
            notificationId,
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
        writeTagsToExif: Boolean,
        tagsExifWriteType: ExifWriteType,
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
            val dirUri = appPreferences.downloadLocation
                ?: getPublicDownloadsDir().toUri()
            val destFile = createFile(
                context = context,
                dirUri = dirUri,
                fileName = fileName,
                mimeType = wallpaper.mimeType ?: parseMimeType(url),
            )
            copyFiles(context, uri, destFile.uri)
            if (writeTagsToExif &&
                wallpaper is WallhavenWallpaper &&
                wallpaper.tags != null
            ) {
                writeTagsToFile(
                    context = context,
                    file = destFile,
                    tags = wallpaper.tags.map { it.name },
                    exifWriteType = tagsExifWriteType,
                )
            }
            if (destFile.uri.scheme == "file") {
                scanFile(context, destFile.uri.toFile())
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveWallpaperToDownloads: ", e)
        }
    }

    private suspend fun hasMoreWallpapers(
        sourceChoice: SourceChoice,
        target: WallpaperTarget,
    ) = when (sourceChoice) {
        SourceChoice.LIGHT_DARK -> lightDarkHasMoreWallpapers(target)
        // saved search manages its own exclude history, see getNextSavedSearchWallpaper
        SourceChoice.SAVED_SEARCH -> true
        SourceChoice.FAVORITES -> favoritesHasMoreWallpapers()
        SourceChoice.LOCAL -> localHasMoreWallpapers(target)
    }

    private suspend fun lightDarkHasMoreWallpapers(
        target: WallpaperTarget,
    ) = lightDarkRepository.getCountForTypeFlagsAndExcludingWallpapers(
        typeFlags = getTypeFlagsForTarget(target),
        excluding = badWallpapers,
    ) > 0

    private suspend fun favoritesHasMoreWallpapers() =
        favoritesRepository.getCountExcludingWallpapers(
            excluding = badWallpapers,
        ) > 0

    private suspend fun localHasMoreWallpapers(
        target: WallpaperTarget,
    ) = localWallpapersRepository.getCountExcludingWallpapers(
        context = context,
        uris = when (target) {
            WallpaperTarget.HOME -> autoWallpaperPreferences.localDirs
            WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.lsLocalDirs
        },
        excluding = badWallpapers,
    ) > 0

    companion object {
        const val FAILURE_REASON = "failure_reason"
        const val SUCCESS_NEXT_HOME_WALLPAPER_ID = "success_home_wallpaper_id"
        const val SUCCESS_NEXT_LOCK_WALLPAPER_ID = "success_lock_wallpaper_id"
        private const val IMMEDIATE_WORK_NAME = "auto_wallpaper_immediate"
        internal const val PERIODIC_WORK_NAME = "auto_wallpaper_periodic"
        internal const val PERIODIC_LS_WORK_NAME = "auto_wallpaper_ls_periodic"
        internal const val INPUT_FORCE = "auto_wallpaper_force"
        internal const val INPUT_TARGETS = "auto_wallpaper_targets"

        enum class FailureReason {
            APP_PREFS_NULL,
            DISABLED,
            CURRENT_TARGETS_DISABLED,
            NO_SOURCES_ENABLED,
            SAVED_SEARCH_NOT_SET,
            NO_WALLPAPER_FOUND,
            CANCELLED,
        }

        suspend fun schedule(
            context: Context,
            autoWallpaperPreferences: AutoWallpaperPreferences,
            appPreferencesRepository: AppPreferencesRepository,
            enqueuePolicy: ExistingPeriodicWorkPolicy =
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
        ) {
            val useSameFreq = autoWallpaperPreferences.useSameFreq
            if (useSameFreq) {
                // stop lock screen worker
                stopLsWorker(context, appPreferencesRepository)
            }
            try {
                if (useSameFreq) {
                    val requestId = schedule(
                        context = context,
                        workName = PERIODIC_WORK_NAME,
                        constraints = autoWallpaperPreferences.constraints,
                        interval = autoWallpaperPreferences.frequency,
                        enqueuePolicy = enqueuePolicy,
                    )
                    appPreferencesRepository.updateAutoWallpaperWorkRequestId(requestId)
                    return
                }
                // not using same freq, so schedule different workers for hs and ls
                val enabledTargets = autoWallpaperPreferences.targets
                if (WallpaperTarget.HOME in enabledTargets) {
                    val requestId = schedule(
                        context = context,
                        workName = PERIODIC_WORK_NAME,
                        constraints = autoWallpaperPreferences.constraints,
                        interval = autoWallpaperPreferences.frequency,
                        enqueuePolicy = enqueuePolicy,
                        targets = setOf(WallpaperTarget.HOME),
                    )
                    appPreferencesRepository.updateAutoWallpaperWorkRequestId(requestId)
                } else {
                    // stop the default worker
                    stopWorker(context, appPreferencesRepository)
                }
                if (WallpaperTarget.LOCKSCREEN in enabledTargets) {
                    val requestId = schedule(
                        context = context,
                        workName = PERIODIC_LS_WORK_NAME,
                        constraints = autoWallpaperPreferences.constraints,
                        interval = autoWallpaperPreferences.lsFrequency,
                        enqueuePolicy = enqueuePolicy,
                        targets = setOf(WallpaperTarget.LOCKSCREEN),
                    )
                    appPreferencesRepository.updateAutoWallpaperLsWorkRequestId(requestId)
                } else {
                    // stop the lock screen worker
                    stopLsWorker(context, appPreferencesRepository)
                }
            } finally {
                appPreferencesRepository.updateAutoWallpaperBackoffUpdated(true)
            }
        }

        private fun schedule(
            context: Context,
            workName: String,
            constraints: Constraints,
            interval: DateTimePeriod,
            enqueuePolicy: ExistingPeriodicWorkPolicy =
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            targets: Set<WallpaperTarget> = WallpaperTarget.ALL,
        ): UUID {
            Log.i(TAG, "Scheduling auto wallpaper worker for targets: $targets")
            val minutes = interval.hours * 60L + interval.minutes
            val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
                minutes,
                TimeUnit.MINUTES,
            ).apply {
                // avoid immediate execution
                setInitialDelay(max(minutes, 15), TimeUnit.MINUTES)
                setConstraints(constraints)
                if (!constraints.requiresDeviceIdle()) {
                    // try to re-execute the worker when 'Retry' is returned
                    setBackoffCriteria(
                        backoffPolicy = BackoffPolicy.LINEAR,
                        backoffDelay = 15,
                        timeUnit = TimeUnit.MINUTES,
                    )
                }
                val wallpaperTargets = targets
                    .map { it.name }
                    .toTypedArray()
                setInputData(workDataOf(INPUT_TARGETS to wallpaperTargets))
            }.build()
            context.workManager.enqueueUniquePeriodicWork(
                workName,
                enqueuePolicy,
                request,
            )
            Log.i(TAG, "Auto wallpaper worker scheduled for targets: $targets")
            return request.id
        }

        suspend fun stop(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ) {
            stopWorker(context, appPreferencesRepository)
            stopLsWorker(context, appPreferencesRepository)
        }

        private suspend fun stopWorker(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ) {
            val scheduled = checkIfScheduled(context, appPreferencesRepository)
            if (!scheduled) {
                return
            }
            Log.i(TAG, "Stopping auto wallpaper worker...")
            context.workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            appPreferencesRepository.updateAutoWallpaperWorkRequestId(null)
            Log.i(TAG, "Auto wallpaper worker cancelled!")
        }

        private suspend fun stopLsWorker(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ) {
            val scheduled = checkIfLsScheduled(context, appPreferencesRepository)
            if (!scheduled) {
                return
            }
            Log.i(TAG, "Stopping auto wallpaper lock screen worker...")
            context.workManager.cancelUniqueWork(PERIODIC_LS_WORK_NAME)
            appPreferencesRepository.updateAutoWallpaperLsWorkRequestId(null)
            Log.i(TAG, "Auto wallpaper lock screen worker cancelled!")
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

        suspend fun checkIfAnyScheduled(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ) = checkIfScheduled(context, appPreferencesRepository) ||
            checkIfLsScheduled(context, appPreferencesRepository)

        private suspend fun checkIfScheduled(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ): Boolean {
            val requestId = appPreferencesRepository.getAutoWallpaperWorkRequestId()
                ?: return false
            return checkIfWorkerScheduled(context, requestId)
        }

        private suspend fun checkIfLsScheduled(
            context: Context,
            appPreferencesRepository: AppPreferencesRepository,
        ): Boolean {
            val requestId = appPreferencesRepository.getAutoWallpaperLsWorkRequestId()
                ?: return false
            return checkIfWorkerScheduled(context, requestId)
        }

        private suspend fun checkIfWorkerScheduled(
            context: Context,
            requestId: UUID,
        ): Boolean {
            var running = false
            try {
                val state = context.workManager.getWorkInfoByIdFlow(requestId).firstOrNull()?.state
                running = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
            } catch (e: Exception) {
                Log.e(TAG, "checkScheduled: requestId: $requestId ", e)
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
                WorkInfo.State.FAILED -> {
                    val failureReasonStr = info.outputData.getString(FAILURE_REASON) ?: ""
                    Status.Failed(
                        AutoWallpaperException(
                            message = failureReasonStr,
                            code = try {
                                FailureReason.valueOf(failureReasonStr)
                            } catch (e: Exception) {
                                FailureReason.CANCELLED
                            },
                        ),
                    )
                }
                WorkInfo.State.BLOCKED -> Status.Pending
                WorkInfo.State.CANCELLED -> Status.Failed(
                    AutoWallpaperException(
                        message = "Work cancelled",
                        code = FailureReason.CANCELLED,
                    ),
                )
            }
        }

        suspend fun checkIfNeedsUpdate(
            appPreferencesRepository: AppPreferencesRepository,
        ) = !appPreferencesRepository.getAutoWallBackoffUpdated()

        enum class SourceChoice {
            LIGHT_DARK,
            SAVED_SEARCH,
            FAVORITES,
            LOCAL,
        }

        class AutoWallpaperException(
            val code: FailureReason,
            message: String? = null,
        ) : Exception(message ?: "")

        sealed class Status {
            data object Running : Status()
            data object Pending : Status()
            data object Success : Status()
            data class Failed(val e: Throwable? = null) : Status()

            fun isSuccessOrFail() = this is Success || this is Failed
        }
    }
}
