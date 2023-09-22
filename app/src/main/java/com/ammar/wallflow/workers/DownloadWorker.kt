package com.ammar.wallflow.workers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getShareChooserIntent
import com.ammar.wallflow.extensions.notificationManager
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.services.DownloadSuccessActionsService
import com.ammar.wallflow.ui.common.permissions.checkNotificationPermission
import com.ammar.wallflow.ui.screens.wallpaper.getWallpaperScreenPendingIntent
import com.ammar.wallflow.utils.NotificationChannels.DOWNLOADS_CHANNEL_ID
import com.ammar.wallflow.utils.decodeSampledBitmapFromFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
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
        val inferFileNameFromResponse = inputData.getBoolean(
            INPUT_KEY_FILE_NAME_FROM_RESPONSE,
            false,
        )
        var fileName: String? = null
        if (!inferFileNameFromResponse) {
            fileName = inputData.getString(INPUT_KEY_DESTINATION_FILE_NAME)
            if (fileName.isNullOrBlank()) {
                return@withContext Result.failure(
                    workDataOf(OUTPUT_KEY_ERROR to "Invalid destination file name"),
                )
            }
        }
        progressNotificationBuilder.setContentTitle(
            inputData.getString(INPUT_KEY_NOTIFICATION_TITLE) ?: url.getFileNameFromUrl(),
        )
        val wallpaperId = inputData.getString(INPUT_KEY_WALLPAPER_ID)
        try {
            val file = if (wallpaperId != null) {
                val source = Source.valueOf(inputData.getString(INPUT_KEY_WALLPAPER_SOURCE) ?: "")
                downloadWallpaper(
                    url = url,
                    dir = destinationDir,
                    fileName = fileName,
                    wallpaperId = wallpaperId,
                    source = source,
                )
            } else {
                download(
                    okHttpClient = okHttpClient,
                    url = url,
                    dir = destinationDir,
                    fileName = fileName,
                    progressCallback = this@DownloadWorker::notifyProgress,
                )
            }
            Result.success(
                workDataOf(OUTPUT_KEY_FILE_PATH to file.absolutePath),
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
        dir: String,
        fileName: String? = null,
        wallpaperId: String,
        source: Source,
    ): File {
        val file = download(
            okHttpClient = okHttpClient,
            url = url,
            dir = dir,
            fileName = fileName,
            progressCallback = this::notifyProgress,
        )
        if (inputData.getBoolean(INPUT_KEY_SCAN_FILE, false)) {
            scanFile(context, file)
        }
        try {
            notifyWallpaperDownloadSuccess(wallpaperId, file, source)
        } catch (e: Exception) {
            Log.e(TAG, "download: Error notifying success", e)
        }
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
        file: File,
        source: Source,
    ) {
        if (!shouldShowSuccessNotification()) return
        val bitmap = decodeSampledBitmapFromFile(context, file.absolutePath)
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
        const val INPUT_KEY_FILE_NAME_FROM_RESPONSE = "file_name_from_response"
        const val INPUT_KEY_NOTIFICATION_TYPE = "notification_type"
        const val INPUT_KEY_NOTIFICATION_TITLE = "notification_title"
        const val INPUT_KEY_WALLPAPER_ID = "wallpaper_id"
        const val INPUT_KEY_WALLPAPER_SOURCE = "wallpaper_source"
        const val INPUT_KEY_SCAN_FILE = "scan_file"
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
