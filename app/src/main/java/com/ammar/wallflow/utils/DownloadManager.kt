package com.ammar.wallflow.utils

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getMLModelsDir
import com.ammar.wallflow.extensions.getTempDir
import com.ammar.wallflow.extensions.getTempFileIfExists
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.workers.DownloadWorker
import com.ammar.wallflow.workers.DownloadWorker.Companion.NotificationType
import com.ammar.wallflow.workers.DownloadWorker.Companion.OUTPUT_KEY_ERROR
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Singleton
class DownloadManager @Inject constructor(
    val appPreferencesRepository: AppPreferencesRepository,
) {
    suspend fun requestDownload(
        context: Context,
        wallpaper: DownloadableWallpaper,
        notificationType: NotificationType = NotificationType.VISIBLE_SUCCESS,
        downloadLocation: DownloadLocation = DownloadLocation.DOWNLOADS,
        tags: List<String>? = null,
        tagsExifWriteType: ExifWriteType = ExifWriteType.APPEND,
    ) = requestDownload(
        context = context,
        url = wallpaper.data,
        downloadLocation = downloadLocation,
        notificationType = notificationType,
        extraWorkerData = arrayOf(
            DownloadWorker.INPUT_KEY_WALLPAPER_ID to wallpaper.id,
            DownloadWorker.INPUT_KEY_WALLPAPER_SOURCE to wallpaper.source.name,
            DownloadWorker.INPUT_KEY_TAGS to tags?.toTypedArray(),
            DownloadWorker.INPUT_KEY_TAGS_WRITE_TYPE to tagsExifWriteType.name,
        ),
    )

    suspend fun requestDownload(
        context: Context,
        url: String,
        downloadLocation: DownloadLocation = DownloadLocation.DOWNLOADS,
        notificationType: NotificationType = NotificationType.VISIBLE_SUCCESS,
        notificationTitle: String? = null,
        fileName: String? = null,
        extraWorkerData: Array<out Pair<String, Any?>> = emptyArray(),
    ): String {
        val fName = when {
            fileName.isNullOrBlank() -> url.getFileNameFromUrl()
            else -> fileName.trimAll()
        }
        val dirUri = when (downloadLocation) {
            DownloadLocation.APP_TEMP -> context.getTempDir().toUri()
            DownloadLocation.DOWNLOADS -> getDownloadLocation()
            DownloadLocation.APP_ML_MODELS -> context.getMLModelsDir().toUri()
        }
        val scanFile = when (downloadLocation) {
            DownloadLocation.APP_TEMP -> false
            DownloadLocation.DOWNLOADS -> true
            DownloadLocation.APP_ML_MODELS -> false
        }
        val request = OneTimeWorkRequestBuilder<DownloadWorker>().apply {
            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            setInputData(
                workDataOf(
                    DownloadWorker.INPUT_KEY_URL to url,
                    DownloadWorker.INPUT_KEY_DESTINATION_DIR to dirUri.toString(),
                    DownloadWorker.INPUT_KEY_DESTINATION_FILE_NAME to fName,
                    DownloadWorker.INPUT_KEY_NOTIFICATION_TYPE to notificationType.type,
                    DownloadWorker.INPUT_KEY_NOTIFICATION_TITLE to notificationTitle,
                    DownloadWorker.INPUT_KEY_SCAN_FILE to scanFile,
                    *extraWorkerData,
                ),
            )
        }.build()
        context.workManager.enqueueUniqueWork(
            url,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return url
    }

    private suspend fun getDownloadLocation(): Uri {
        val appPreferences = appPreferencesRepository
            .appPreferencesFlow
            .firstOrNull()
            ?: return getPublicDownloadsDir().toUri()
        return appPreferences.downloadLocation
            ?: getPublicDownloadsDir().toUri()
    }

    fun getProgress(
        context: Context,
        workName: String,
    ) = context.workManager.getWorkInfosForUniqueWorkFlow(workName).map {
        val info = it.firstOrNull() ?: return@map DownloadStatus.Failed(
            IllegalArgumentException("No download request found with name $workName"),
        )
        when (info.state) {
            WorkInfo.State.ENQUEUED -> DownloadStatus.Pending
            WorkInfo.State.RUNNING -> DownloadStatus.Running(
                info.progress.getLong(DownloadWorker.PROGRESS_KEY_DOWNLOADED, 0),
                info.progress.getLong(DownloadWorker.PROGRESS_KEY_TOTAL, 0),
            )
            WorkInfo.State.SUCCEEDED -> DownloadStatus.Success(
                info.outputData.getString(DownloadWorker.OUTPUT_KEY_FILE_PATH),
            )
            WorkInfo.State.FAILED -> DownloadStatus.Failed(
                DownloadException(info.outputData.getString(OUTPUT_KEY_ERROR)),
            )
            WorkInfo.State.BLOCKED -> DownloadStatus.Pending
            WorkInfo.State.CANCELLED -> DownloadStatus.Cancelled
        }
    }

    suspend fun downloadWallpaperAsync(
        context: Application,
        wallpaper: DownloadableWallpaper,
        tags: List<String>? = null,
        tagsExifWriteType: ExifWriteType = ExifWriteType.APPEND,
        onLoadingChange: (loading: Boolean) -> Unit = {},
        onResult: (file: File?) -> Unit,
    ) {
        try {
            onLoadingChange(true)
            val fileName = wallpaper.data.getFileNameFromUrl()
            val fileIfExists = context.getTempFileIfExists(fileName)
            if (fileIfExists != null) {
                onLoadingChange(false)
                onResult(fileIfExists)
                return
            }
            val workName = requestDownload(
                context = context,
                wallpaper = wallpaper,
                downloadLocation = DownloadLocation.APP_TEMP,
                notificationType = NotificationType.SILENT,
                tags = tags,
                tagsExifWriteType = tagsExifWriteType,
            )
            getProgress(context, workName).collectLatest { state ->
                if (!state.isSuccessOrFail()) return@collectLatest
                onLoadingChange(false)
                onResult(
                    if (state is DownloadStatus.Failed) {
                        null
                    } else {
                        context.getTempFileIfExists(fileName)
                    },
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadWallpaperAsync: ", e)
            onLoadingChange(false)
            onResult(null)
        }
    }

    companion object {
        // fun getWorkName(wallpaper: Wallpaper) = getWorkName(wallpaper.path.getFileNameFromUrl())
        // fun getWorkName(fileName: String) = "download_$fileName"

        enum class DownloadLocation {
            DOWNLOADS,
            APP_TEMP,
            APP_ML_MODELS,
        }

        class DownloadException(message: String? = null) : Exception(message ?: "")
    }
}

sealed class DownloadStatus {
    data class Running(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : DownloadStatus() {
        val progress = getProgress(downloadedBytes, totalBytes)
    }

    data class Paused(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : DownloadStatus() {
        val progress = getProgress(downloadedBytes, totalBytes)
    }

    data class Success(val filePath: String? = null) : DownloadStatus()
    data class Failed(val e: Throwable? = null) : DownloadStatus()
    data object Pending : DownloadStatus()
    data object Cancelled : DownloadStatus()

    fun isSuccessOrFail() = this is Success || this is Failed

    companion object {
        fun getProgress(
            downloadedBytes: Long,
            totalBytes: Long,
        ) = if (totalBytes == 0L) 0F else ((downloadedBytes.toFloat()) / totalBytes)
    }
}
