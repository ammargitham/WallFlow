package com.ammar.havenwalls.workers

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.await
import com.ammar.havenwalls.extensions.getFileNameFromUrl
import com.ammar.havenwalls.extensions.getShareChooserIntent
import com.ammar.havenwalls.services.DownloadSuccessActionsService
import com.ammar.havenwalls.ui.wallpaper.getWallpaperScreenPendingIntent
import com.ammar.havenwalls.utils.ContentDisposition
import com.ammar.havenwalls.utils.NotificationChannels
import com.ammar.havenwalls.utils.NotificationChannels.DOWNLOADS_CHANNEL_ID
import com.ammar.havenwalls.utils.decodeSampledBitmapFromFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import kotlin.math.absoluteValue
import kotlin.random.Random


class DownloadWorker(
    private val context: Context,
    params: WorkerParameters,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(
    context,
    params,
) {
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
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
                    WorkManager.getInstance(context).createCancelPendingIntent(id),
                ).build()
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
        NotificationType.values().firstOrNull {
            it.type == inputData.getInt(INPUT_KEY_NOTIFICATION_TYPE, NotificationType.VISIBLE.type)
        } ?: NotificationType.VISIBLE
    }

    init {
        NotificationChannels.createDownloadChannel(context)
    }

    override suspend fun doWork() = withContext(coroutineDispatcher) {
        val url = inputData.getString(INPUT_KEY_URL)
        if (url.isNullOrBlank()) {
            return@withContext Result.failure(
                workDataOf(OUTPUT_KEY_ERROR to "Invalid url")
            )
        }
        val destinationDir = inputData.getString(INPUT_KEY_DESTINATION_DIR)
        if (destinationDir.isNullOrBlank()) {
            return@withContext Result.failure(
                workDataOf(OUTPUT_KEY_ERROR to "Invalid destination dir")
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
                    workDataOf(OUTPUT_KEY_ERROR to "Invalid destination file name")
                )
            }
        }
        progressNotificationBuilder.setContentTitle(
            inputData.getString(INPUT_KEY_NOTIFICATION_TITLE) ?: url.getFileNameFromUrl()
        )
        val wallpaperId = inputData.getString(INPUT_KEY_WALLPAPER_ID)
        try {
            val file = if (wallpaperId != null) {
                downloadWallpaper(
                    url = url,
                    dir = destinationDir,
                    fileName = fileName,
                    wallpaperId = wallpaperId
                )
            } else {
                download(
                    url = url,
                    dir = destinationDir,
                    fileName = fileName,
                )
            }
            Result.success(
                workDataOf(OUTPUT_KEY_FILE_PATH to file.absolutePath)
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
                    ?: ""
            )
            setProgress(0, 0, true)
        }.build()
    )

    private suspend fun downloadWallpaper(
        url: String,
        dir: String,
        fileName: String? = null,
        wallpaperId: String,
    ): File {
        val file = download(
            url = url,
            dir = dir,
            fileName = fileName,
        )
        try {
            notifyWallpaperDownloadSuccess(wallpaperId, file)
        } catch (e: Exception) {
            Log.e(TAG, "download: Error notifying success", e)
        }
        return file
    }

    private suspend fun download(
        url: String,
        dir: String,
        fileName: String? = null,
    ): File {
        notifyProgress(100, -1)
        val downloadRequest = Request.Builder().url(url).build()
        var file: File? = null
        try {
            okHttpClient.newCall(downloadRequest).await().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code: $response")
                }
                val tempFile = createFile(response, dir, fileName)
                (response.body ?: throw IOException("Response body is null")).use { body ->
                    val contentLength = body.contentLength()
                    val source = body.source()
                    tempFile.sink().buffer().use { sink ->
                        var totalBytesRead: Long = 0
                        val bufferSize = 8 * 1024
                        var bytesRead: Long
                        while (source.read(
                                sink.buffer,
                                bufferSize.toLong(),
                            ).also { bytesRead = it } != -1L
                        ) {
                            sink.emit()
                            totalBytesRead += bytesRead
                            notifyProgress(contentLength, totalBytesRead)
                        }
                        sink.flush()
                    }
                }
                file = tempFile
            }
        } catch (e: Exception) {
            // delete created file on any exception and rethrow the error
            file?.delete()
            throw e
        }
        return file ?: throw IOException("This will never be thrown")
    }

    private fun createFile(
        response: Response,
        dir: String,
        fileName: String?,
    ): File {
        var fName = when {
            fileName != null -> fileName
            else -> {
                val contentDispositionStr = response.header("Content-Disposition")
                val parseExceptionMsg = "Could not parse file name from response"
                if (contentDispositionStr.isNullOrEmpty()) {
                    throw IllegalArgumentException(parseExceptionMsg)
                }
                ContentDisposition.parse(contentDispositionStr).filename
                    ?: throw IllegalArgumentException(parseExceptionMsg)
            }
        }
        var tempFile = File(dir, fName)
        val extension = tempFile.extension
        val nameWoExt = tempFile.nameWithoutExtension
        var suffix = 0
        while (tempFile.exists()) {
            suffix++
            fName = "$nameWoExt-$suffix${if (extension.isNotEmpty()) ".$extension" else ""}"
            tempFile = File(dir, fName)
        }
        tempFile.parentFile?.mkdirs()
        tempFile.createNewFile()
        return tempFile
    }

    private suspend fun notifyProgress(
        total: Long,
        downloaded: Long,
    ) {
        setProgress(
            workDataOf(
                PROGRESS_KEY_TOTAL to total,
                PROGRESS_KEY_DOWNLOADED to downloaded,
            )
        )
        if (!shouldShowNotification()) return
        val notification = progressNotificationBuilder.setProgress(
            total.toInt(),
            downloaded.toInt(),
            downloaded <= -1
        ).build()
        try {
            setForeground(ForegroundInfo(progressNotificationId, notification))
        } catch (e: Exception) {
            Log.e(TAG, "Error setting to foreground: ", e)
        }
    }

    @SuppressLint("MissingPermission") // permission check added in shouldShowNotification()
    private fun notifyWallpaperDownloadSuccess(
        wallpaperId: String,
        file: File,
    ) {
        if (!shouldShowSuccessNotification()) return
        val bitmap = decodeSampledBitmapFromFile(context, file.absolutePath)
        val notification = successNotificationBuilder.apply {
            setContentTitle(file.name)
            setLargeIcon(bitmap)
            setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
            setContentIntent(
                getWallpaperScreenPendingIntent(
                    context,
                    wallpaperId,
                )
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
                ).build()
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
                ).build()
            )
        }.build()
        notificationManager.notify(successNotificationId, notification)
    }

    private fun shouldShowNotification() =
        !notificationType.isSilent() && hasNotificationPermission()

    private fun shouldShowSuccessNotification() =
        shouldShowNotification() && notificationType == NotificationType.VISIBLE_SUCCESS

    private fun hasNotificationPermission() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        else -> PackageManager.PERMISSION_GRANTED
    } == PackageManager.PERMISSION_GRANTED

    companion object {
        const val INPUT_KEY_URL = "url"
        const val INPUT_KEY_DESTINATION_DIR = "destination_dir"
        const val INPUT_KEY_DESTINATION_FILE_NAME = "file_name"
        const val INPUT_KEY_FILE_NAME_FROM_RESPONSE = "file_name_from_response"
        const val INPUT_KEY_NOTIFICATION_TYPE = "notification_type"
        const val INPUT_KEY_NOTIFICATION_TITLE = "notification_title"
        const val INPUT_KEY_WALLPAPER_ID = "wallpaper_id"
        const val OUTPUT_KEY_ERROR = "error"
        const val OUTPUT_KEY_FILE_PATH = "output_file_path"
        const val PROGRESS_KEY_TOTAL = "total"
        const val PROGRESS_KEY_DOWNLOADED = "downloaded"

        enum class NotificationType(val type: Int) {
            SILENT(1),
            VISIBLE(2),
            VISIBLE_SUCCESS(3);

            fun isSilent() = this == SILENT
        }
    }
}
