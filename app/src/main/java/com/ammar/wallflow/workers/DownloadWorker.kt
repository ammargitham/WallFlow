package com.ammar.wallflow.workers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getShareChooserIntent
import com.ammar.wallflow.extensions.notificationManager
import com.ammar.wallflow.extensions.parseMimeType
import com.ammar.wallflow.extensions.toUriOrNull
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.services.DownloadSuccessActionsService
import com.ammar.wallflow.ui.common.permissions.checkNotificationPermission
import com.ammar.wallflow.ui.screens.wallpaper.getWallpaperScreenPendingIntent
import com.ammar.wallflow.utils.ExifWriteType
import com.ammar.wallflow.utils.NotificationChannels.DOWNLOADS_CHANNEL_ID
import com.ammar.wallflow.utils.decodeSampledBitmapFromUri
import com.ammar.wallflow.utils.writeTagsToFile
import com.lazygeniouz.dfc.file.DocumentFileCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(
    context,
    params,
) {
    private val progressNotificationId by lazy { id.hashCode().absoluteValue }
    private val successNotificationId by lazy { progressNotificationId + 100 }
    private val progressNotificationBuilder by lazy {
        NotificationCompat.Builder(context, DOWNLOADS_CHANNEL_ID).apply {
            setContentText(context.getString(R.string.downloading))
            setSmallIcon(R.drawable.baseline_download_24)
            setOngoing(true)
            setSilent(true)
            priority = NotificationCompat.PRIORITY_LOW
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.cancel),
                    context.workManager.createCancelPendingIntent(id),
                ).build(),
            )
        }
    }
    private val successNotificationBuilder by lazy {
        NotificationCompat.Builder(context, DOWNLOADS_CHANNEL_ID).apply {
            setContentTitle(context.getString(R.string.downloaded))
            setSmallIcon(R.drawable.baseline_download_24)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }
    }
    private val notificationType by lazy {
        NotificationType.entries.firstOrNull {
            it.type == inputData.getInt(INPUT_KEY_NOTIFICATION_TYPE, NotificationType.VISIBLE.type)
        } ?: NotificationType.VISIBLE
    }

    override suspend fun doWork() = withContext(ioDispatcher) {
        val url = inputData.getString(INPUT_KEY_URL)
        if (url.isNullOrBlank()) {
            return@withContext Result.failure(
                workDataOf(OUTPUT_KEY_ERROR to "Invalid url"),
            )
        }
        val destinationDir = inputData.getString(INPUT_KEY_DESTINATION_DIR)
        if (destinationDir.isNullOrBlank()) {
            return@withContext Result.failure(
                workDataOf(OUTPUT_KEY_ERROR to "Invalid destination dir"),
            )
        }
        val destinationDirUri = destinationDir.toUriOrNull()
            ?: return@withContext Result.failure(
                workDataOf(OUTPUT_KEY_ERROR to "Invalid destination directory"),
            )
        val fileName: String? = inputData.getString(INPUT_KEY_DESTINATION_FILE_NAME)
        if (fileName.isNullOrBlank()) {
            return@withContext Result.failure(
                workDataOf(OUTPUT_KEY_ERROR to "Invalid destination file name"),
            )
        }
        progressNotificationBuilder.setContentTitle(
            inputData.getString(INPUT_KEY_NOTIFICATION_TITLE) ?: url.getFileNameFromUrl(),
        )
        val wallpaperId = inputData.getString(INPUT_KEY_WALLPAPER_ID)
        try {
            val file: DocumentFileCompat = if (wallpaperId != null) {
                val source = Source.valueOf(inputData.getString(INPUT_KEY_WALLPAPER_SOURCE) ?: "")
                downloadWallpaper(
                    url = url,
                    dirUri = destinationDirUri,
                    fileName = fileName,
                    wallpaperId = wallpaperId,
                    source = source,
                )
            } else {
                download(
                    context = context,
                    okHttpClient = okHttpClient,
                    url = url,
                    dirUri = destinationDirUri,
                    fileName = fileName,
                    mimeType = parseMimeType(url),
                    progressCallback = this@DownloadWorker::notifyProgress,
                )
            }
            if (wallpaperId != null) {
                val tags = inputData.getStringArray(INPUT_KEY_TAGS) ?: emptyArray()
                if (tags.isNotEmpty()) {
                    // write tags to file
                    val writeTypeStr = inputData.getString(INPUT_KEY_TAGS_WRITE_TYPE)
                    val exifWriteType = if (writeTypeStr != null) {
                        try {
                            ExifWriteType.valueOf(writeTypeStr)
                        } catch (e: Exception) {
                            ExifWriteType.APPEND
                        }
                    } else {
                        ExifWriteType.APPEND
                    }
                    writeTagsToFile(
                        context = context,
                        file = file,
                        tags = tags.asList(),
                        exifWriteType = exifWriteType,
                    )
                }
            }
            Result.success(
                workDataOf(OUTPUT_KEY_FILE_PATH to file.uri.toString()),
            )
        } catch (e: Exception) {
            Log.e(TAG, "doWork: Error downloading $url", e)
            Result.failure(workDataOf(OUTPUT_KEY_ERROR to e.localizedMessage))
        }
    }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        progressNotificationId,
        progressNotificationBuilder.apply {
            val url = inputData.getString(INPUT_KEY_URL)
            setContentTitle(
                inputData.getString(INPUT_KEY_NOTIFICATION_TITLE)
                    ?: url?.getFileNameFromUrl()
                    ?: "",
            )
            setProgress(0, 0, true)
        }.build(),
    )

    private suspend fun downloadWallpaper(
        url: String,
        dirUri: Uri,
        fileName: String,
        wallpaperId: String,
        source: Source,
    ): DocumentFileCompat {
        var file = copyFromCache(
            url = url,
            dirUri = dirUri,
            fileName = fileName,
        )
        if (file == null) {
            file = download(
                context = context,
                okHttpClient = okHttpClient,
                url = url,
                dirUri = dirUri,
                fileName = fileName,
                mimeType = parseMimeType(url),
                progressCallback = this::notifyProgress,
            )
        }
        if (inputData.getBoolean(INPUT_KEY_SCAN_FILE, false)) {
            val uri = file.uri
            if (uri.scheme == "file") {
                scanFile(context, uri.toFile())
            }
        }
        try {
            notifyWallpaperDownloadSuccess(wallpaperId, file, source)
        } catch (e: Exception) {
            Log.e(TAG, "download: Error notifying success", e)
        }
        return file
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun copyFromCache(
        url: String,
        dirUri: Uri,
        fileName: String?,
    ): DocumentFileCompat? {
        val cacheFile = context.imageLoader.diskCache?.openSnapshot(url)?.use {
            it.data.toFile().toUri()
        } ?: return null
        val fileNameActual = fileName ?: url.getFileNameFromUrl()
        val file = createFile(
            context = context,
            dirUri = dirUri,
            fileName = fileNameActual,
            mimeType = parseMimeType(url),
        )
        copyFiles(context, cacheFile, file.uri)
        return file
    }

    private suspend fun notifyProgress(
        total: Long,
        downloaded: Long,
    ) {
        setProgress(
            workDataOf(
                PROGRESS_KEY_TOTAL to total,
                PROGRESS_KEY_DOWNLOADED to downloaded,
            ),
        )
        if (!shouldShowNotification()) return
        val notification = progressNotificationBuilder.setProgress(
            total.toInt(),
            downloaded.toInt(),
            downloaded <= -1,
        ).build()
        try {
            setForeground(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ForegroundInfo(
                        progressNotificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    ForegroundInfo(
                        progressNotificationId,
                        notification,
                    )
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting to foreground: ", e)
        }
    }

    @SuppressLint("MissingPermission") // permission check added in shouldShowNotification()
    private fun notifyWallpaperDownloadSuccess(
        wallpaperId: String,
        file: DocumentFileCompat,
        source: Source,
    ) {
        if (!shouldShowSuccessNotification()) return
        val (bitmap, _) = decodeSampledBitmapFromUri(context, file.uri) ?: return
        val notification = successNotificationBuilder.apply {
            setContentTitle(file.name)
            setLargeIcon(bitmap)
            setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?),
            )
            setContentIntent(
                getWallpaperScreenPendingIntent(
                    context,
                    source,
                    wallpaperId,
                ),
            )
            setAutoCancel(true)
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.share),
                    PendingIntent.getActivity(
                        context,
                        Random.nextInt(),
                        context.getShareChooserIntent(file, true),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).build(),
            )
            addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.delete),
                    DownloadSuccessActionsService.getDeletePendingIntent(
                        context,
                        file,
                        successNotificationId,
                    ),
                ).build(),
            )
        }.build()
        context.notificationManager.notify(successNotificationId, notification)
    }

    private fun shouldShowNotification() = !notificationType.isSilent() &&
        context.checkNotificationPermission()

    private fun shouldShowSuccessNotification() =
        shouldShowNotification() && notificationType == NotificationType.VISIBLE_SUCCESS

    companion object {
        const val INPUT_KEY_URL = "url"
        const val INPUT_KEY_DESTINATION_DIR = "destination_dir"
        const val INPUT_KEY_DESTINATION_FILE_NAME = "file_name"
        const val INPUT_KEY_NOTIFICATION_TYPE = "notification_type"
        const val INPUT_KEY_NOTIFICATION_TITLE = "notification_title"
        const val INPUT_KEY_WALLPAPER_ID = "wallpaper_id"
        const val INPUT_KEY_WALLPAPER_SOURCE = "wallpaper_source"
        const val INPUT_KEY_SCAN_FILE = "scan_file"
        const val INPUT_KEY_TAGS = "tags"
        const val INPUT_KEY_TAGS_WRITE_TYPE = "tags_write_type"
        const val OUTPUT_KEY_ERROR = "error"
        const val OUTPUT_KEY_FILE_PATH = "output_file_path"
        const val PROGRESS_KEY_TOTAL = "total"
        const val PROGRESS_KEY_DOWNLOADED = "downloaded"

        enum class NotificationType(val type: Int) {
            SILENT(1),
            VISIBLE(2),
            VISIBLE_SUCCESS(3),
            ;

            fun isSilent() = this == SILENT
        }
    }
}
